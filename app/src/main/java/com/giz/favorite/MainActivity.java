package com.giz.favorite;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.ClipboardManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.animation.AnimationUtils;
import android.support.design.bottomappbar.BottomAppBar;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.graphics.drawable.Animatable2Compat;
import android.support.graphics.drawable.AnimatedVectorDrawableCompat;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.CardView;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearSmoothScroller;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.method.ScrollingMovementMethod;
import android.transition.Explode;
import android.transition.Fade;
import android.util.Log;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.BounceInterpolator;
import android.view.animation.LinearInterpolator;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import database.FavoriteItemLib;
import datatool.FavoriteItem;
import datatool.FavoriteTool;
import datatool.SourceApp;
import utility.CommonAdapter;
import utility.CommonUtil;
import utility.CommonViewHolder;
import utility.RoundedBottomSheetDialog;
import viewtool.CustomToast;
import viewtool.FavoriteItemAdapter;

import static datatool.ArchiveTool.ARCHIVE_NAME_ALL;
import static datatool.ArchiveTool.ARCHIVE_NAME_SELF;
import static datatool.ArchiveTool.ARCHIVE_NAME_STAR;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private static final int WRITE_STORAGE_REQUEST_CODE = 1;

    public static final int REQUEST_SIDE_ACTIVITY = 2;
    public static final int REQUEST_DATE_PICKER_FRAGMENT = 3;

    private AppBarLayout mAppBarLayout;
    private CoordinatorLayout mCoordinatorLayout;
    private BottomAppBar mBottomAppBar;
    private RecyclerView mRecyclerView;
    private SwipeRefreshLayout mRefreshLayout;
    private FloatingActionButton mMainFab;
    private View mMaskView; // 遮罩
    private TextView mArchiveTitleTv;
    private CardView mDialogCard;
    private View mClipMaskView;
    private TextView mClipContentTv;
    private FloatingActionButton mBackToTopFab;

    private FavoriteItemAdapter mItemAdapter;

    private boolean inSelectedState = false;
    private boolean performRvLayoutAnim = true;     // 是否进行布局动画
    private boolean shouldSlideDownBab = false;     // 是否隐藏底部工具栏

    private String mArchiveTitle = ARCHIVE_NAME_ALL;

    private Toolbar.OnMenuItemClickListener mBabOptionMenuListener;     // 收藏项操作菜单
    private Toolbar.OnMenuItemClickListener mBabBasicMenuListener;      // 底部工具栏原始菜单
    private FavoriteItem mSelectedFavoriteItem;
    private int mSelectedPos;
    private List<String> mArchiveTitleList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // 设置状态栏为白色，状态栏内容为深色
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
            getWindow().setStatusBarColor(Color.WHITE);
        }

        // 绑定视图
        initViews();
        // 动态申请权限
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED)
            requestStoragePermission();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 加载数据
        mRefreshLayout.setRefreshing(true);
        updateRecyclerview(mArchiveTitle);

        // 检查剪贴板
        checkClipboard(false);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main_toolbar, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case R.id.main_date_search:
                // date fragment
                DatePickerFragment fragment = DatePickerFragment.newInstance(new Date());
                fragment.show(getSupportFragmentManager(), "DatePicker");
                return true;
            case R.id.main_day_pager:
                Intent intent = new Intent(MainActivity.this, DayPagerActivity.class);
                startActivity(intent, ActivityOptionsCompat.makeSceneTransitionAnimation(MainActivity.this).toBundle());
                return true;
        }
        return false;
    }

    @Override
    public void onBackPressed() {
        if(inSelectedState){
            // 取消选中状态
            backToOriginRecyclerView();
            backToOriginBottomAppBar();
            inSelectedState = false;
        }else
            super.onBackPressed();
    }

    private void initViews(){
        initBabListener();

        mAppBarLayout = findViewById(R.id.main_appbar);
        setSupportActionBar((Toolbar)findViewById(R.id.main_toolbar));
        mCoordinatorLayout = findViewById(R.id.main_CL);

        mBottomAppBar = findViewById(R.id.main_bottomAppBar);
        mBottomAppBar.replaceMenu(R.menu.menu_bottom_app_bar);
        mMainFab = findViewById(R.id.main_fab);
        mBackToTopFab = findViewById(R.id.main_back_to_top);

        mRecyclerView = findViewById(R.id.main_recyclerView);
        DividerItemDecoration decoration = new DividerItemDecoration(this, DividerItemDecoration.VERTICAL);
        mRecyclerView.addItemDecoration(decoration);

        mMaskView = findViewById(R.id.main_mask);
        mArchiveTitleTv = findViewById(R.id.main_archive_title);
        mDialogCard = findViewById(R.id.clip_cardView);
        mClipContentTv = mDialogCard.findViewById(R.id.clip_content);
        mClipMaskView = findViewById(R.id.clip_mask);
        mClipMaskView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 阻止向下传递
            }
        });
        mClipContentTv.setMovementMethod(ScrollingMovementMethod.getInstance());

        mRefreshLayout = findViewById(R.id.main_srl);
        mRefreshLayout.setColorSchemeResources(R.color.colorPrimaryLight);

        // 事件绑定
        mRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                updateRecyclerview(mArchiveTitle);   // 刷新列表
            }
        });
        mBottomAppBar.setOnMenuItemClickListener(mBabBasicMenuListener);
        mBottomAppBar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openSideActivity();
            }
        });
        mMainFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                newFavoriteItem();
            }
        });
        // 消费事件，阻止点击
        mMaskView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return true;
            }
        });
        // 监听列表滑动，决定是否显示回到顶部按钮
        mRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                if(mRecyclerView.computeVerticalScrollOffset() > 1080){
                    mBackToTopFab.show();
                }else{
                    mBackToTopFab.hide();
                }
            }
        });
        final RecyclerView.SmoothScroller smoothScroller = new LinearSmoothScroller(this){
            @Override
            protected int getVerticalSnapPreference() {
                return LinearSmoothScroller.SNAP_TO_START;
            }
        };
        mBackToTopFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                smoothScroller.setTargetPosition(0);
                mRecyclerView.getLayoutManager().startSmoothScroll(smoothScroller);
            }
        });
        // 标题下拉菜单
        mArchiveTitleTv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showTitleDropdownMenu();
            }
        });
    }

    private void showTitleDropdownMenu() {
        final PopupWindow popupWindow = new PopupWindow(this);
        final ImageView dropDownIcon = findViewById(R.id.main_drop_down_icon);
        dropDownIcon.animate().rotation(180).setDuration(225);

        final View view = getLayoutInflater().inflate(R.layout.popup_main_title, null);
        RecyclerView recyclerView = view.findViewById(R.id.popup_title_rv);
        CommonAdapter<String> adapter = new CommonAdapter<String>(this, getPureArchiveTitleList(), R.layout.item_popup_title) {
            @Override
            public void bindData(CommonViewHolder viewHolder, final String data, int pos) {
                viewHolder.setText(R.id.item_archive_title, data);
                ImageView icon = viewHolder.getView(R.id.item_archive_icon);
                switch (data) {
                    case ARCHIVE_NAME_ALL:
                        icon.setImageResource(R.drawable.ic_archive_folder_all);
                        break;
                    case ARCHIVE_NAME_STAR:
                        icon.setImageResource(R.drawable.ic_archive_folder_star);
                        break;
                    case ARCHIVE_NAME_SELF:
                        icon.setImageResource(R.drawable.ic_archive_folder_self);
                        break;
                    default:
                        icon.setImageResource(R.drawable.ic_archive_folder_normal);
                        break;
                }
                if(data.equals(mArchiveTitle)){
                    viewHolder.itemView.setBackgroundColor(0xFFEEEEEE);
                }else{
                    viewHolder.itemView.setBackgroundResource(R.color.white);
                }

                viewHolder.itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mArchiveTitle = data;
                        popupWindow.dismiss();
                    }
                });
            }
        };
        recyclerView.setAdapter(adapter);

        popupWindow.setContentView(view);
        popupWindow.setElevation(CommonUtil.dp2px(this, 16));
        popupWindow.setBackgroundDrawable(getResources().getDrawable(R.drawable.bg_popup_main));
        popupWindow.setHeight((int)CommonUtil.dp2px(this, 360));
        popupWindow.setWidth((int)CommonUtil.dp2px(this, 300));
        popupWindow.showAsDropDown(mArchiveTitleTv);
        mClipMaskView.setVisibility(View.VISIBLE);
        mClipMaskView.setAlpha(0f);

        popupWindow.setOnDismissListener(new PopupWindow.OnDismissListener() {
            @Override
            public void onDismiss() {
                dropDownIcon.animate().rotation(0).setDuration(175);
                mClipMaskView.setVisibility(View.GONE);
                updateRecyclerview(mArchiveTitle);
                mClipMaskView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) { }
                });
            }
        });
        mClipMaskView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                popupWindow.dismiss();
            }
        });
    }

    private void openSideActivity() {
        Intent intent = new Intent(MainActivity.this, SideActivity.class);
        startActivityForResult(intent, REQUEST_SIDE_ACTIVITY, ActivityOptionsCompat.makeSceneTransitionAnimation(MainActivity.this).toBundle());
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d(TAG, "onActivityResult: " + resultCode);
        if(resultCode == 2){
            if(requestCode == REQUEST_SIDE_ACTIVITY){
                if(data != null && data.hasExtra(SideActivity.EXTRA_ARCHIVE_TITLE)){
                    String at = data.getStringExtra(SideActivity.EXTRA_ARCHIVE_TITLE);
                    if(!at.equals(mArchiveTitle)){
                        mArchiveTitle = at;
                        mArchiveTitleTv.setText(mArchiveTitle);
                        performRvLayoutAnim = true;
                        updateRecyclerview(mArchiveTitle);

                        Log.d(TAG, "onActivityResult: " + mArchiveTitle);
                    }
                }
            }
        }else if(resultCode == RESULT_OK){
            Log.d(TAG, "onActivityResult: ok");
            if(requestCode == REQUEST_DATE_PICKER_FRAGMENT && data != null){
                Date date = (Date)data.getSerializableExtra(DatePickerFragment.EXTRA_DATE);

                mItemAdapter.setFavoriteItemList(getFavoriteItemPairListByDate(date));
                Log.d(TAG, "onActivityResult: " + date);
            }
        }
    }

    private void updateRecyclerview(@Nullable String archiveTitle){
        if(mItemAdapter == null){
            mItemAdapter = new FavoriteItemAdapter(this, getFavoriteItemPairList(archiveTitle));
            mRecyclerView.setAdapter(mItemAdapter);
            mItemAdapter.setOnItemSelectedListener(new FavoriteItemAdapter.OnItemSelectedListener() {
                @Override
                public void onSelected(FavoriteItem item, int pos) {
                    onItemSelectedEvent(item, pos);
                }
            });
            performRvLayoutAnim = false;
        }else {
            mItemAdapter.setFavoriteItemList(getFavoriteItemPairList(archiveTitle));
            if(performRvLayoutAnim)
                mRecyclerView.startLayoutAnimation();
            performRvLayoutAnim = false;
        }
        mRefreshLayout.setRefreshing(false);
        findViewById(R.id.main_bmni).setVisibility(mItemAdapter.getItemCount() == 0 ? View.VISIBLE : View.GONE);
    }

    /**
     * 弹出新建收藏项的底页
     */
    private void newFavoriteItem(){
        Intent intent = SelfContentActivity.newIntent(MainActivity.this, mMainFab.getLeft() + mMainFab.getWidth() / 2,
                mMainFab.getTop() + mMainFab.getHeight() / 2, mMainFab.getSize(), SelfContentActivity.MODE_CREATE, null);
        startActivity(intent, ActivityOptionsCompat.makeSceneTransitionAnimation(MainActivity.this).toBundle());
    }

    // 初始化底部工具栏监听器
    private void initBabListener(){
        mBabBasicMenuListener = new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem menuItem) {
                switch (menuItem.getItemId()){
                    case R.id.bab_search:
                        Intent intent = new Intent(MainActivity.this, SearchActivity.class);
                        startActivity(intent, ActivityOptionsCompat.makeSceneTransitionAnimation(MainActivity.this).toBundle());
                        return true;
                    case R.id.bab_clipboard:
                        checkClipboard(true);
                        return true;
                    case R.id.bab_about:
                        Intent intent1 = new Intent(MainActivity.this, AboutActivity.class);
                        startActivity(intent1, ActivityOptionsCompat.makeSceneTransitionAnimation(MainActivity.this).toBundle());
                        return true;
                }
                return false;
            }
        };

        mBabOptionMenuListener = new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem menuItem) {
                switch (menuItem.getItemId()){
                    case R.id.option_archive:
                    {
                        // 归档
                        archivingItem();
                        return true;
                    }
                    case R.id.option_delete:
                    {
                        // 删除
                        new AlertDialog.Builder(MainActivity.this)
                                .setTitle("确认删除吗？")
                                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        deleteFavoriteItem(mSelectedFavoriteItem, mSelectedPos);
                                    }
                                }).setNegativeButton(android.R.string.cancel, null).show();
                        return true;
                    }
                    case R.id.option_star:
                    {
                        // 星标
                        mSelectedFavoriteItem.setStarred(!mSelectedFavoriteItem.isStarred());
                        FavoriteItemLib.get(MainActivity.this).updateFavoriteItem(mSelectedFavoriteItem);
                        CustomToast customToast = CustomToast.make(MainActivity.this, mSelectedFavoriteItem.isStarred() ? "加星成功" : " 取消星标");
                        customToast.setMargin(0, 0.1f);
                        customToast.show();
                        AnimatedVectorDrawableCompat drawableCompat = AnimatedVectorDrawableCompat.create(MainActivity.this,
                                mSelectedFavoriteItem.isStarred() ? R.drawable.avd_star : R.drawable.avd_star_reverse);
                        mBottomAppBar.getMenu().getItem(0).setIcon(drawableCompat);
                        if(drawableCompat != null){
                            drawableCompat.registerAnimationCallback(new Animatable2Compat.AnimationCallback() {
                                @Override
                                public void onAnimationEnd(Drawable drawable) {
                                    super.onAnimationEnd(drawable);
                                    backToOriginRecyclerView();
                                    backToOriginBottomAppBar();
                                }
                            });
                            drawableCompat.start();
                        }
                        for(int i = 0; i < mRecyclerView.getChildCount(); i++){
                            View view = mRecyclerView.getChildAt(i);
                            if(mRecyclerView.getChildAdapterPosition(view) == mSelectedPos){
                                view.findViewById(R.id.item_favorite_star).setVisibility(mSelectedFavoriteItem.isStarred() ? View.VISIBLE : View.GONE);
                                break;
                            }
                        }
                        return true;
                    }
                }
                return false;
            }
        };
    }

    /**
     * 收藏项选中事件
     * 判断是否升起BottomAppBar
     */
    private void onItemSelectedEvent(final FavoriteItem item, final int pos){
        inSelectedState = true;
        mSelectedFavoriteItem = item;
        mSelectedPos = pos;
        mBackToTopFab.hide();
        if(mBottomAppBar.getTranslationY() > 0){
            // 弹出底部栏
            shouldSlideDownBab = true;
            Log.d(TAG, "onItemSelectedEvent: pop out the bottom app bar");
            mBottomAppBar.clearAnimation();
            mBottomAppBar.animate().translationY(0).setInterpolator(AnimationUtils.LINEAR_OUT_SLOW_IN_INTERPOLATOR).setDuration(225L);
            mMainFab.clearAnimation();
            mMainFab.animate().translationY(getFabTranslateY()).setInterpolator(AnimationUtils.LINEAR_OUT_SLOW_IN_INTERPOLATOR)
                    .setDuration(225L).setListener(new Animator.AnimatorListener() {
                @Override
                public void onAnimationStart(Animator animation) { }
                @Override
                public void onAnimationEnd(Animator animation) {
                    onItemSelectedEventDetail();
                    mMainFab.animate().setListener(null);   //取消这个监听动画
                }
                @Override
                public void onAnimationCancel(Animator animation) { }
                @Override
                public void onAnimationRepeat(Animator animation) { }
            });
        }else {
            onItemSelectedEventDetail();
        }
    }

    /**
     * 收藏项选中实际执行事件
     */
    private void onItemSelectedEventDetail(){
        // 底部工具栏菜单
        mMainFab.hide(new FloatingActionButton.OnVisibilityChangedListener() {
            @Override
            public void onHidden(FloatingActionButton fab) {
                super.onHidden(fab);
                mBottomAppBar.setFabAlignmentMode(BottomAppBar.FAB_ALIGNMENT_MODE_END); // 更改附着位置
                mBottomAppBar.replaceMenu(R.menu.menu_bab_item_option); // 更换菜单
                // 设置是否收藏了
                if(mSelectedFavoriteItem.isStarred())
                    mBottomAppBar.getMenu().getItem(0).setIcon(R.drawable.ic_star_yellow);
                mBottomAppBar.setOnMenuItemClickListener(mBabOptionMenuListener);

                mMainFab.setImageResource(R.drawable.ic_back_white);    // 更换主Fab图标
                // 更改事件
                mMainFab.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        backToOriginRecyclerView();
                        backToOriginBottomAppBar();
                    }
                });
                mMainFab.show();    // 显示
            }
        });
        mBottomAppBar.setNavigationIcon(null);                  // 取消导航图标

        // 列表动作，突出显示当前选中项
        int childCount = mRecyclerView.getChildCount();     // 布局上可视的视图
        // 禁止列表滑动和刷新
        mAppBarLayout.setEnabled(false);
        mMaskView.setVisibility(View.VISIBLE);
        Log.d(TAG, "onItemSelectedEventDetail: child count:" + childCount);
        Log.d(TAG, "onItemSelectedEventDetail: select pos:" + mSelectedPos);
        for(int i = 0; i < childCount; i++){
            View view = mRecyclerView.getChildAt(i);
            view.setEnabled(false);
            int adapterPos = mRecyclerView.getChildAdapterPosition(view);   // 获取该视图在适配器中的实际位置
            if(adapterPos == mSelectedPos){
                view.animate().translationZ(CommonUtil.dp2px(this, 12)).setDuration(225L);
            }else {
                view.animate().alpha(0.4f).setDuration(225L);
            }
        }
    }

    /**
     * 回到初始的底部工具栏
     */
    private void backToOriginBottomAppBar(){
        // 显示回到顶部按钮
        if(mRecyclerView.computeVerticalScrollOffset() > 1080){
            mBackToTopFab.show();
        }

        mMainFab.hide(new FloatingActionButton.OnVisibilityChangedListener() {
            @Override
            public void onHidden(FloatingActionButton fab) {
                super.onHidden(fab);
                mBottomAppBar.setFabAlignmentMode(BottomAppBar.FAB_ALIGNMENT_MODE_CENTER); // 更改附着位置
                mBottomAppBar.replaceMenu(R.menu.menu_bottom_app_bar); // 更换菜单

                mMainFab.setImageResource(R.drawable.ic_add_white);    // 更换主Fab图标
                // 更改事件
                mMainFab.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        newFavoriteItem();
                    }
                });
                mBottomAppBar.setNavigationIcon(R.drawable.ic_menu_white);                  // 取消导航图标
                mMainFab.show();    // 显示
            }
        });
        mBottomAppBar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openSideActivity();
            }
        });
        mBottomAppBar.setOnMenuItemClickListener(mBabBasicMenuListener);

        if(shouldSlideDownBab){
            slideDownBottomAppBar();
            shouldSlideDownBab = false;
        }
    }

    private void backToOriginRecyclerView(){
        inSelectedState = false;
        // 列表动作，恢复原始状态
        mAppBarLayout.setEnabled(true);
        mMaskView.setVisibility(View.GONE);
        int childCount = mRecyclerView.getChildCount();
        Log.d(TAG, "backToOrigin: recovery child count" + childCount);
        for(int i = 0; i < childCount; i++){
            View view = mRecyclerView.getChildAt(i);
            view.setEnabled(true);
            if(!mRecyclerView.isLayoutRequested())
                view.animate().alpha(1.0f).translationZ(0).setDuration(225L);
            else {
                // 删除后重新请求布局，动画无效
                view.setAlpha(1.0f);
                view.setTranslationZ(0);
            }
        }
    }

    /**
     * 从数据库获取收藏项，加入日期信息构成用于列表视图的列表
     * @return 对列表
     */
    private List<Pair<Integer, FavoriteItem>> getFavoriteItemPairList(String archiveTitle){
        List<Pair<Integer, FavoriteItem>> pairList = new ArrayList<>();
        List<FavoriteItem> itemList;
        if(archiveTitle == null || archiveTitle.equals(ARCHIVE_NAME_ALL))
            itemList = FavoriteItemLib.get(this).getFavoriteItemList();
        else if(archiveTitle.equals(ARCHIVE_NAME_SELF))
            itemList = FavoriteItemLib.get(this).getFavoriteItemListBySource(SourceApp.APP_SELF);
        else if(archiveTitle.equals(ARCHIVE_NAME_STAR))
            itemList = FavoriteItemLib.get(this).getFavoriteItemListOfStarred();
        else
            itemList = FavoriteItemLib.get(this).getFavoriteItemListByArchive(archiveTitle);

        Collections.reverse(itemList);  // 倒序，使最新的在上面
        if(itemList.size() > 0){
            long latestDate = itemList.get(0).getDate().getTime();
            FavoriteItem firstItem = new FavoriteItem();
            firstItem.setDate(itemList.get(0).getDate());
            Pair<Integer, FavoriteItem> firstPair = new Pair<>(FavoriteItemAdapter.TYPE_DATE, firstItem);
            pairList.add(firstPair);
            for (int i = 0; i < itemList.size(); i++){
                FavoriteItem item = itemList.get(i);
                if(!CommonUtil.theSameDay(new Date(latestDate), item.getDate())){
                    // 增加日期项
                    latestDate = item.getDate().getTime();
                    FavoriteItem dateItem = new FavoriteItem();
                    dateItem.setDate(item.getDate());
                    Pair<Integer, FavoriteItem> datePair = new Pair<>(FavoriteItemAdapter.TYPE_DATE, dateItem);
                    pairList.add(datePair);
                }
                // 此项
                pairList.add(new Pair<>(FavoriteItemAdapter.TYPE_ITEM, item));
            }
        }

        return pairList;
    }

    /**
     * 返回通过日期查询的结果
     * @param date 日期
     * @return 列表
     */
    private List<Pair<Integer, FavoriteItem>> getFavoriteItemPairListByDate(Date date){
        List<Pair<Integer, FavoriteItem>> pairList = new ArrayList<>();
        List<FavoriteItem> itemList = FavoriteItemLib.get(this).getFavoriteItemListByDate(date);

        Collections.reverse(itemList);  // 倒序，使最新的在上面
        if(itemList.size() > 0){
            FavoriteItem firstItem = new FavoriteItem();
            firstItem.setDate(itemList.get(0).getDate());
            Pair<Integer, FavoriteItem> firstPair = new Pair<>(FavoriteItemAdapter.TYPE_DATE, firstItem);
            pairList.add(firstPair);
            for (int i = 0; i < itemList.size(); i++){
                FavoriteItem item = itemList.get(i);
                // 此项
                pairList.add(new Pair<>(FavoriteItemAdapter.TYPE_ITEM, item));
            }
        }

        return pairList;
    }

    /**
     * 删除收藏项操作
     */
    private void deleteFavoriteItem(final FavoriteItem item, final int pos){
        // 动画
        RecyclerView.ViewHolder viewHolder = mRecyclerView.findViewHolderForAdapterPosition(pos);
        if(viewHolder != null){
            viewHolder.itemView.animate().translationX(viewHolder.itemView.getWidth())
                    .setDuration(225L).setInterpolator(AnimationUtils.FAST_OUT_LINEAR_IN_INTERPOLATOR)
                    .setListener(new Animator.AnimatorListener() {
                        @Override
                        public void onAnimationStart(Animator animation) { }

                        @Override
                        public void onAnimationEnd(Animator animation) {
                            Log.d(TAG, "onAnimationEnd: ");
                            mItemAdapter.removeFavoriteItem(pos);
                            backToOriginRecyclerView();
                            mAppBarLayout.setEnabled(true);
                            mMaskView.setVisibility(View.VISIBLE);
//                            viewHolder.itemView.animate().setListener(null);
                        }

                        @Override
                        public void onAnimationCancel(Animator animation) { }

                        @Override
                        public void onAnimationRepeat(Animator animation) { }
                    });
        }

        // 显示Snackbar
        final int itemCount = mItemAdapter.getItemCount();
        Snackbar snackbar = Snackbar.make(mCoordinatorLayout, R.string.del_snackbar_done, Snackbar.LENGTH_SHORT)
                .setAction(R.string.del_snackbar_undo, new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        // 撤消
                        mItemAdapter.addFavoriteItem(pos, new Pair<>(FavoriteItemAdapter.TYPE_ITEM, item));
                        Toast.makeText(MainActivity.this, R.string.del_snackbar_undo_success, Toast.LENGTH_SHORT).show();
                    }
                });
        snackbar.addCallback(new Snackbar.Callback(){
            @Override
            public void onShown(Snackbar sb) {
                super.onShown(sb);
                mMainFab.setEnabled(false);
            }

            @Override
            public void onDismissed(Snackbar transientBottomBar, int event) {
                super.onDismissed(transientBottomBar, event);
                // 没有撤消，则从数据库中删除
                FavoriteTool.deleteItemImage(item.getImagePath());
                if(itemCount != mItemAdapter.getItemCount())
                    FavoriteItemLib.get(MainActivity.this).removeFavoriteItem(item.getUUID().toString());
                mMainFab.setEnabled(true);

                mAppBarLayout.setEnabled(false);
                mMaskView.setVisibility(View.GONE);
                backToOriginBottomAppBar();
            }
        });
        // 与底部工具栏同高
        CoordinatorLayout.LayoutParams params = (CoordinatorLayout.LayoutParams)snackbar.getView().getLayoutParams();
        params.height = mBottomAppBar.getHeight();
        snackbar.getView().setLayoutParams(params);

        snackbar.show();
    }

    /**
     * 归档收藏项
     */
    private void archivingItem(){
        View view = getLayoutInflater().inflate(R.layout.bottom_sheet_item_archiving, null);
        final RoundedBottomSheetDialog bottomSheetDialog = new RoundedBottomSheetDialog(this, view,
                R.style.BottomSheetDialog);
        bottomSheetDialog.getBehavior().setSkipCollapsed(true);

        RecyclerView recyclerView = view.findViewById(R.id.item_archiving_rv);
        CommonAdapter<Archive> commonAdapter = new CommonAdapter<Archive>(this, getArchiveList(), R.layout.item_archive) {
            @Override
            public void bindData(CommonViewHolder viewHolder, final Archive data, int pos) {
                viewHolder.setText(R.id.item_archive_title, data.title);
                viewHolder.setText(R.id.item_archive_count, String.valueOf(data.count));
                ImageView icon = viewHolder.getView(R.id.item_archive_icon);
                if(data.title.equals(mSelectedFavoriteItem.getArchive()))
                    icon.setImageResource(R.drawable.ic_archive_folder_normal);
                else
                    icon.setImageResource(R.drawable.ic_archive_folder_gray);

                viewHolder.itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if(data.title.equals(mSelectedFavoriteItem.getArchive())){
                            String text = "已存在于[" + data.title + "]收藏夹";
                            CustomToast.make(MainActivity.this, text).show();
                        }else{
                            mSelectedFavoriteItem.setArchive(data.title);
                            FavoriteItemLib.get(MainActivity.this).updateFavoriteItem(mSelectedFavoriteItem);
                            // 更新布局
                            mItemAdapter.updateFavoriteItem(mSelectedPos, mSelectedFavoriteItem);
                            String text = "成功收藏到[" + data.title + "]收藏夹";
                            CustomToast.make(MainActivity.this, text).show();

                            bottomSheetDialog.dismiss();
                        }
                    }
                });
            }
        };
        recyclerView.setAdapter(commonAdapter);
        // 新建收藏夹
        view.findViewById(R.id.item_archiving_new).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showNewArchiveDialog(bottomSheetDialog);
            }
        });

        bottomSheetDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                backToOriginRecyclerView();
                backToOriginBottomAppBar();
            }
        });
        bottomSheetDialog.show();
    }
    private List<Archive> getArchiveList(){
        String archiveStr = FavoriteTool.getArchivePreferences(this);
//        String archiveStr = getArchiveString();
        mArchiveTitleList = new ArrayList<>();
        if(archiveStr != null && !archiveStr.equals(""))
            mArchiveTitleList.addAll(Arrays.asList(archiveStr.split(",")));

        Map<String, Archive> archiveMap = new HashMap<>();

        for(String a : mArchiveTitleList){
            Archive archive = new Archive();
            archive.title = a;
            archive.count = 0;
            archiveMap.put(a, archive);
        }

        List<FavoriteItem> itemList = FavoriteItemLib.get(this).getFavoriteItemList();
        List<Archive> archiveList = new ArrayList<>();

        for(FavoriteItem item : itemList){
            String itemArchive = item.getArchive();
            Archive archive = archiveMap.get(itemArchive);
            if(archive != null){
                archive.count += 1;
            }
        }
        for(String archiveTitle : mArchiveTitleList){
            archiveList.add(archiveMap.get(archiveTitle));
        }

        return archiveList;
    }
    private List<String> getPureArchiveTitleList(){
        String archiveStr = FavoriteTool.getArchivePreferences(this);
        List<String> list = new ArrayList<>();
        list.addAll(Arrays.asList(ARCHIVE_NAME_ALL, ARCHIVE_NAME_STAR, ARCHIVE_NAME_SELF));
        if(archiveStr != null && !archiveStr.equals(""))
            list.addAll(Arrays.asList(archiveStr.split(",")));

        return list;
    }

    public void showNewArchiveDialog(final RoundedBottomSheetDialog archivingDialog){
        archivingDialog.hide();

        View sheetView = LayoutInflater.from(this).inflate(R.layout.bottom_sheet_new_archive, null);
        final EditText titleEt = sheetView.findViewById(R.id.new_archive_title);

        final RoundedBottomSheetDialog bottomSheetDialog = new RoundedBottomSheetDialog(this, sheetView, R.style.BottomSheetDialog);
        bottomSheetDialog.setActionViewIDs(new RoundedBottomSheetDialog.OnActionViewListener() {
            @Override
            public void onClick(int viewID) {
                switch (viewID){
                    case R.id.new_archive_ok_btn:
                        if(titleEt != null){
                            String archiveTitle = titleEt.getText().toString();
                            if(archiveTitle.equals(""))
                                CustomToast.make(MainActivity.this, "名称不能为空").show();
                            else if(archiveTitle.equals("") || archiveTitle.equals(ARCHIVE_NAME_ALL) || archiveTitle.equals(ARCHIVE_NAME_STAR)
                                    || archiveTitle.equals(ARCHIVE_NAME_SELF)){
                                CustomToast.make(MainActivity.this, "不能使用内置名称").show();
                                titleEt.setText("");
                            }else if(mArchiveTitleList.contains(archiveTitle)){
                                CustomToast.make(MainActivity.this, "该收藏夹名已存在").show();
                            }else{
                                newArchive(archiveTitle);
                                mSelectedFavoriteItem.setArchive(archiveTitle);
                                FavoriteItemLib.get(MainActivity.this).updateFavoriteItem(mSelectedFavoriteItem);
                                // 更新布局
                                mItemAdapter.updateFavoriteItem(mSelectedPos, mSelectedFavoriteItem);
                                String text = "成功收藏到[" + archiveTitle + "]收藏夹";
                                CustomToast.make(MainActivity.this, text).show();

                                archivingDialog.dismiss();
                                bottomSheetDialog.dismiss();
                            }
                        }
                        break;
                    case R.id.new_archive_cancel_btn:
                        bottomSheetDialog.dismiss();
                        archivingDialog.show();
                        break;
                }
            }
        }, R.id.new_archive_ok_btn, R.id.new_archive_cancel_btn);

        bottomSheetDialog.show();
    }
    private void newArchive(String archive){
        String archiveStr = FavoriteTool.getArchivePreferences(this);
//        String archiveStr = getArchiveString();
        if(archiveStr == null || archiveStr.equals(""))
            archiveStr = archive;
        else
            archiveStr += "," + archive;
        FavoriteTool.setArchivePreferences(this, archiveStr);
//        saveArchiveString(archiveStr);
    }

    /**
     * 申请存储权限
     */
    private void requestStoragePermission(){
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED){
            if(ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE)){
                new AlertDialog.Builder(this)
                        .setTitle("需要获取读写文件的权限")
                        .setPositiveButton("修改权限", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                ActivityCompat.requestPermissions(MainActivity.this,
                                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, WRITE_STORAGE_REQUEST_CODE);
                            }
                        }).setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {
                        Toast.makeText(MainActivity.this, "没有相关权限，收藏失败", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                }).show();
            }else {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        WRITE_STORAGE_REQUEST_CODE);
            }
        }
    }

    /**
     * 检查剪贴板
     * @param ignorePrev 是否忽略之前的结果
     */
    private void checkClipboard(boolean ignorePrev){
        ClipboardManager manager = (ClipboardManager)getSystemService(CLIPBOARD_SERVICE);

        if(manager != null){
            if(manager.hasPrimaryClip() && manager.getPrimaryClip().getItemCount() > 0){
                final String text = manager.getPrimaryClip().getItemAt(0).getText().toString();
                    Log.d(TAG, "onPrimaryClipChanged: text:" + text);
                    // 进入CollectionActivity，相关操作
                if(!text.equals(FavoriteTool.getClipboardText(this)) || ignorePrev){
                    FavoriteTool.setClipboardText(this, text);

                    mClipContentTv.setText(text);
                    mDialogCard.findViewById(R.id.clip_ok_btn).setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            mClipMaskView.animate().alpha(0);
                            mDialogCard.animate().alpha(0).scaleX(0).scaleY(0).setInterpolator(new LinearInterpolator())
                                    .setListener(new AnimatorListenerAdapter() {
                                @Override
                                public void onAnimationEnd(Animator animation) {
                                    mDialogCard.setVisibility(View.GONE);
                                    mClipMaskView.setVisibility(View.GONE);
                                    mClipContentTv.scrollTo(0, 0);

                                    Intent intent = CollectionActivity.newIntent(MainActivity.this, text);
                                    startActivity(intent, ActivityOptionsCompat.makeSceneTransitionAnimation(MainActivity.this).toBundle());
                                }
                            });
                        }
                    });
                    mDialogCard.findViewById(R.id.clip_cancel_btn).setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            mClipMaskView.animate().alpha(0);
                            mDialogCard.animate().alpha(0).scaleX(0).scaleY(0).setInterpolator(new LinearInterpolator()).setListener(new AnimatorListenerAdapter() {
                                @Override
                                public void onAnimationEnd(Animator animation) {
                                    mDialogCard.setVisibility(View.GONE);
                                    mClipMaskView.setVisibility(View.GONE);
                                    mClipContentTv.scrollTo(0, 0);
                                }
                            });
                        }
                    });

                    mDialogCard.setVisibility(View.VISIBLE);
                    mDialogCard.animate().alpha(1).scaleX(1).scaleY(1).setInterpolator(new BounceInterpolator()).setListener(null);
                    mClipMaskView.setVisibility(View.VISIBLE);
                    mClipMaskView.animate().alpha(1);
                }
            }
        }else{
            Log.d(TAG, "registerClipEvent: null manager.");
        }
    }

    private void printFileInfo(){
        // 内部文件存储路径：/data/user/0/com.giz.favorite/files
        Log.d(TAG, "printFileInfo: internal file dir: " + getFilesDir());
        // 本应用外部文件存储路径：/storage/emulated/0/Android/data/com.giz.favorite/files
        Log.d(TAG, "onCreate: External Files Dir: " + getExternalFilesDir(null));
        // 外部存储目录
        Log.d(TAG, "printFileInfo: External Storage Dir: " + Environment.getExternalStorageDirectory());
    }

    private float getFabTranslateY(){
        Rect fabContentRect = new Rect();
        mMainFab.getContentRect(fabContentRect);
        float fabHeight = (float)fabContentRect.height();
        if (fabHeight == 0.0F) {
            fabHeight = (float)mMainFab.getMeasuredHeight();
        }

        float fabBottomShadow = (float)(mMainFab.getHeight() - fabContentRect.bottom);
        float fabVerticalShadowPadding = (float)(mMainFab.getHeight() - fabContentRect.height());
        float attached = -mBottomAppBar.getCradleVerticalOffset() + fabHeight / 2.0F + fabBottomShadow;
        float detached = fabVerticalShadowPadding - (float)mMainFab.getPaddingBottom();
        return (float)(-mBottomAppBar.getMeasuredHeight()) + attached;
    }

    private void slideDownBottomAppBar(){
        mBottomAppBar.animate().translationY(mBottomAppBar.getHeight()).setInterpolator(AnimationUtils.FAST_OUT_LINEAR_IN_INTERPOLATOR).setDuration(175L);

        Rect fabContentRect = new Rect();
        mMainFab.getContentRect(fabContentRect);
        float fabShadowPadding = (float)(mMainFab.getMeasuredHeight() - fabContentRect.height());
        mMainFab.clearAnimation();
        mMainFab.animate().translationY((float)(-mMainFab.getPaddingBottom()) + fabShadowPadding).setInterpolator(AnimationUtils.FAST_OUT_LINEAR_IN_INTERPOLATOR).setDuration(175L);
    }

    private class Archive{
        String title;
        int count;
    }
}
