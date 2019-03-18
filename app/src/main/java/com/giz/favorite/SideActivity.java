package com.giz.favorite;

import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.transition.Slide;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import database.FavoriteItemLib;
import datatool.Archive;
import datatool.ArchiveTool;
import datatool.FavoriteItem;
import datatool.FavoriteTool;
import datatool.SourceApp;
import utility.CommonAdapter;
import utility.CommonViewHolder;
import utility.RoundedBottomSheetDialog;
import viewtool.SwipeMenuRecyclerTouchListener;
import viewtool.CustomToast;
import viewtool.SwipeBackLayout;

import static datatool.ArchiveTool.ARCHIVE_NAME_ALL;
import static datatool.ArchiveTool.ARCHIVE_NAME_SELF;
import static datatool.ArchiveTool.ARCHIVE_NAME_STAR;

public class SideActivity extends AppCompatActivity {
    private static final String TAG = "SideActivity";

    public static final String EXTRA_ARCHIVE_TITLE = "archiveTitle";

    private RecyclerView mRecyclerView;
    CommonAdapter<Archive> mArchiveAdapter;
    SwipeBackLayout mSwipeBackLayout;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_side);

        setupTransition();
        // 设置状态栏为白色，状态栏内容为深色
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
            getWindow().setStatusBarColor(Color.WHITE);
        }

        // 初始化布局
        initView();
    }

    private void setupTransition() {
        Slide slide = new Slide(Gravity.START);
        slide.setDuration(400);
        getWindow().setEnterTransition(slide);
    }

    private void initView() {
        // 滑动退出布局
        mSwipeBackLayout = findViewById(R.id.side_swipeBackL);
        mSwipeBackLayout.setDirection(SwipeBackLayout.SWIPE_LEFT);
        mSwipeBackLayout.setOnSwipeListener(new SwipeBackLayout.OnSwipeListener() {
            @Override
            public void onSwipeStart() {
                onBackPressed();
            }

            @Override
            public void onSwipeFinish() {
            }
        });

        // 关闭按钮
        findViewById(R.id.side_close_btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

        // 列表
        mRecyclerView = findViewById(R.id.side_rv);
        SwipeMenuRecyclerTouchListener touchListener = new SwipeMenuRecyclerTouchListener(this, mRecyclerView);
        touchListener.setSwipeable(R.id.rowFG, R.id.rowBG, new SwipeMenuRecyclerTouchListener.OnSwipeOptionsClickListener() {
            @Override
            public void onSwipeOptionsClicked(int viewID, int position) {
                switch (viewID){
                    case R.id.item_archive_rename:
                        // 重命名
                        showRenameArchiveDialog(position);
                        break;
                    case R.id.item_archive_delete:
                        // 删除
                        ArchiveTool.getInstance().removeArchive(SideActivity.this, mArchiveAdapter.getItem(position).title);
                        mArchiveAdapter.deleteItem(position);
                        CustomToast.make(SideActivity.this, "删除成功").show();
                        break;
                }
            }
        }).setSwipeOptionViews(R.id.item_archive_rename, R.id.item_archive_delete)
                .setUnSwipeableRows(0, 1, 2).setLongClickable(false)
        .setOnSwipeListener(new SwipeMenuRecyclerTouchListener.OnSwipeListenerPublic() {
            @Override
            public void onSwipeOptionsClose() {
                mSwipeBackLayout.setInterceptAction(true);
            }

            @Override
            public void onSwipeOptionsOpened() {
                mSwipeBackLayout.setInterceptAction(false);
            }
        }).setClickable(new SwipeMenuRecyclerTouchListener.OnRowClickListener() {
            @Override
            public void onRowClicked(int position) {
                backToMainActivity(mArchiveAdapter.getItem(position).title);
            }
        });
        mRecyclerView.addOnItemTouchListener(touchListener);
        initRecyclerView();
    }

    private void initRecyclerView(){
        mArchiveAdapter = new CommonAdapter<Archive>(this, ArchiveTool.getInstance().getAllArchiveList(this), R.layout.item_archive) {
            @Override
            public void bindData(CommonViewHolder viewHolder, final Archive data, int pos) {
                viewHolder.setText(R.id.item_archive_title, data.title);
                viewHolder.setText(R.id.item_archive_count, String.valueOf(data.count));
                ImageView icon = viewHolder.getView(R.id.item_archive_icon);
                switch (data.title) {
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
                        if(data.count > 0)
                            icon.setImageResource(R.drawable.ic_archive_folder_normal);
                        else
                            icon.setImageResource(R.drawable.ic_archive_folder_gray);
                        break;
                }
                viewHolder.itemView.setClickable(true);
            }
        };

        mRecyclerView.setAdapter(mArchiveAdapter);
    }

    /**
     * 返回结果给主活动
     */
    private void backToMainActivity(String title){
        Intent intent = new Intent();
        intent.putExtra(EXTRA_ARCHIVE_TITLE, title);
        setResult(MainActivity.REQUEST_SIDE_ACTIVITY, intent);
        onBackPressed();
    }

    public void showNewArchiveDialog(View view){
        ArchiveTool.getInstance().showNewArchiveDialog(this, new ArchiveTool.OnNewArchiveDialogListener() {
            @Override
            public void onShown() {
                
            }

            @Override
            public void onArchived(String archiveTitle) {
                Archive archive = new Archive();
                archive.title = archiveTitle;
                archive.count = 0;
                mArchiveAdapter.addItem(archive);
            }

            @Override
            public void onCancelled() {

            }
        });
    }

    private void showRenameArchiveDialog(final int pos) {
        final String oldArchive = mArchiveAdapter.getItem(pos).title;

        View sheetView = LayoutInflater.from(this).inflate(R.layout.bottom_sheet_new_archive, null);
        final EditText titleEt = sheetView.findViewById(R.id.new_archive_title);
        if(titleEt != null)
            titleEt.setText(oldArchive);

        final List<String> archiveTitleList = ArchiveTool.getInstance().getCustomArchiveTitleList(this);
        final RoundedBottomSheetDialog bottomSheetDialog = new RoundedBottomSheetDialog(this, sheetView, R.style.BottomSheetDialog);
        bottomSheetDialog.setActionViewIDs(new RoundedBottomSheetDialog.OnActionViewListener() {
            @Override
            public void onClick(int viewID) {
                switch (viewID){
                    case R.id.new_archive_ok_btn:
                        if(titleEt != null){
                            String archiveTitle = titleEt.getText().toString();
                            if(archiveTitle.equals(""))
                                CustomToast.make(SideActivity.this, "名称不能为空").show();
                            else if(archiveTitle.equals("") || archiveTitle.equals(ARCHIVE_NAME_ALL) || archiveTitle.equals(ARCHIVE_NAME_STAR)
                                    || archiveTitle.equals(ARCHIVE_NAME_SELF)){
                                CustomToast.make(SideActivity.this, "不能使用内置名称").show();
                                titleEt.setText("");
                            }else if(archiveTitleList.contains(archiveTitle)){
                                CustomToast.make(SideActivity.this, "该收藏夹名已存在").show();
                            }else{
                                ArchiveTool.getInstance().renameArchive(SideActivity.this, archiveTitle, oldArchive);

                                Archive archive = mArchiveAdapter.getItem(pos);
                                archive.title = archiveTitle;
                                mArchiveAdapter.updateItem(pos, archive);
                                bottomSheetDialog.dismiss();
                            }
                        }
                        break;
                    case R.id.new_archive_cancel_btn:
                        bottomSheetDialog.dismiss();
                        break;
                }
            }
        }, R.id.new_archive_ok_btn, R.id.new_archive_cancel_btn);
        bottomSheetDialog.show();
    }
}
