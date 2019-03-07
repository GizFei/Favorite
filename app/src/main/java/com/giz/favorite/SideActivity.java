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
import datatool.FavoriteItem;
import datatool.FavoriteTool;
import datatool.SourceApp;
import utility.CommonAdapter;
import utility.CommonViewHolder;
import utility.RoundedBottomSheetDialog;
import viewtool.SwipeMenuRecyclerTouchListener;
import viewtool.CustomToast;
import viewtool.SwipeBackLayout;

public class SideActivity extends AppCompatActivity {
    private static final String TAG = "SideActivity";
    private static final String ARCHIVE_FILE = "archives.txt";

    public static final String ARCHIVE_NAME_ALL = "所有收藏";
    public static final String ARCHIVE_NAME_STAR = "星标收藏";
    public static final String ARCHIVE_NAME_SELF = "我的原创";
    public static final String EXTRA_ARCHIVE_TITLE = "archiveTitle";

    private RecyclerView mRecyclerView;
    CommonAdapter<Archive> mArchiveAdapter;
    SwipeBackLayout mSwipeBackLayout;

    List<String> mArchiveTitleList = new ArrayList<>();

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
            public void onSwipeStart() { }

            @Override
            public void onSwipeFinish() {
                onBackPressed();
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
                        deleteArchive(mArchiveAdapter.getItem(position).title);
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
        mArchiveAdapter = new CommonAdapter<Archive>(this, getArchiveList(), R.layout.item_archive) {
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

    private void updateRecyclerView(){
        mArchiveAdapter.updateData(getArchiveList());
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

    private List<Archive> getArchiveList(){
        String archiveStr = FavoriteTool.getArchivePreferences(this);
//        String archiveStr = getArchiveString();
        mArchiveTitleList = new ArrayList<>();
        mArchiveTitleList.add(ARCHIVE_NAME_ALL);
        mArchiveTitleList.add(ARCHIVE_NAME_STAR);
        mArchiveTitleList.add(ARCHIVE_NAME_SELF);
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

        archiveMap.get(ARCHIVE_NAME_ALL).count = itemList.size();
        for(FavoriteItem item : itemList){
            String itemArchive = item.getArchive();
            Archive archive = archiveMap.get(itemArchive);
            if(archive != null){
                archive.count += 1;
            }
            if(item.isStarred()){
                archiveMap.get(ARCHIVE_NAME_STAR).count += 1;
            }
            if(item.getSource().equals(SourceApp.APP_SELF)){
                archiveMap.get(ARCHIVE_NAME_SELF).count += 1;
            }
        }
        for(String archiveTitle : mArchiveTitleList){
            archiveList.add(archiveMap.get(archiveTitle));
        }

        return archiveList;
    }

    public void showNewArchiveDialog(View view){
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
                                CustomToast.make(SideActivity.this, "名称不能为空").show();
                            else if(archiveTitle.equals("") || archiveTitle.equals(ARCHIVE_NAME_ALL) || archiveTitle.equals(ARCHIVE_NAME_STAR)
                                    || archiveTitle.equals(ARCHIVE_NAME_SELF)){
                                CustomToast.make(SideActivity.this, "不能使用内置名称").show();
                                titleEt.setText("");
                            }else if(mArchiveTitleList.contains(archiveTitle)){
                                CustomToast.make(SideActivity.this, "该收藏夹名已存在").show();
                            }else{
                                newArchive(archiveTitle);

                                Archive archive = new Archive();
                                archive.title = archiveTitle;
                                archive.count = 0;
                                mArchiveAdapter.addItem(archive);
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

    private void showRenameArchiveDialog(final int pos) {
        final String oldArchive = mArchiveAdapter.getItem(pos).title;

        View sheetView = LayoutInflater.from(this).inflate(R.layout.bottom_sheet_new_archive, null);
        final EditText titleEt = sheetView.findViewById(R.id.new_archive_title);
        if(titleEt != null)
            titleEt.setText(oldArchive);

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
                            }else if(mArchiveTitleList.contains(archiveTitle)){
                                CustomToast.make(SideActivity.this, "该收藏夹名已存在").show();
                            }else{
                                renameArchive(archiveTitle, oldArchive);

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

    // 收藏夹相关操作
    private void renameArchive(String newArchive, String oldArchive){
        String archiveStr = FavoriteTool.getArchivePreferences(SideActivity.this);
        archiveStr = archiveStr.replace(oldArchive, newArchive);
        FavoriteTool.setArchivePreferences(SideActivity.this, archiveStr);
        FavoriteItemLib.get(this).patchUpdateFavoriteItemByArchive(newArchive, oldArchive);
//        String archiveStr = getArchiveString();
//        archiveStr = archiveStr.replace(oldArchive, newArchive);
//        saveArchiveString(archiveStr);
//        FavoriteItemLib.get(this).patchUpdateFavoriteItemByArchive(newArchive, oldArchive);
    }

    private void deleteArchive(String archive){
        String archiveStr = FavoriteTool.getArchivePreferences(SideActivity.this);
//        String archiveStr = getArchiveString();

        Log.d(TAG, "onSwipeOptionsClicked: [" + archiveStr + "]");
        if(archiveStr.contains(archive + ","))
            archiveStr = archiveStr.replace(archive + ",", "");
        else
            archiveStr = archiveStr.replace(archive, "");
        Log.d(TAG, "onSwipeOptionsClicked: [" + archiveStr + "]");
        FavoriteTool.setArchivePreferences(SideActivity.this, archiveStr);
//        saveArchiveString(archiveStr);
        FavoriteItemLib.get(SideActivity.this).patchUpdateFavoriteItemByArchive(null, archive);
    }

    private void newArchive(String archive){
        String archiveStr = FavoriteTool.getArchivePreferences(SideActivity.this);
//        String archiveStr = getArchiveString();
        if(archiveStr == null || archiveStr.equals(""))
            archiveStr = archive;
        else
            archiveStr += "," + archive;
        FavoriteTool.setArchivePreferences(SideActivity.this, archiveStr);
//        saveArchiveString(archiveStr);
    }

    private void saveArchiveString(String archiveString){
        try {
            FileOutputStream outputStream = openFileOutput(ARCHIVE_FILE, MODE_PRIVATE);
            outputStream.write(archiveString.getBytes());
            outputStream.close();
        }catch (FileNotFoundException e) {
            Log.d(TAG, "saveArchiveString: file not found");
        }catch (IOException e2){
            Log.d(TAG, "saveArchiveString: io exception");
        }
    }
    private String getArchiveString(){
        try {
            FileInputStream inputStream = openFileInput(ARCHIVE_FILE);
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            byte[] bytes = new byte[1024];
            int length = 0;
            while ((length = inputStream.read(bytes)) != -1){
                outputStream.write(bytes, 0, length);
            }
            inputStream.close();
            return outputStream.toString();
        } catch (IOException e) {
            e.printStackTrace();
            return "";
        }
    }

    private class Archive{
        String title;
        int count;
    }
}
