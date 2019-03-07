package com.giz.favorite;

import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.transition.Explode;
import android.transition.Slide;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import datatool.SourceApp;
import utility.CommonAdapter;
import utility.CommonViewHolder;
import utility.RoundedBottomSheetDialog;

public class AboutActivity extends AppCompatActivity {

    private RecyclerView mRecyclerView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);
        getWindow().setEnterTransition(new Slide(Gravity.BOTTOM));

        // 设置状态栏为白色，状态栏内容为深色
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
            getWindow().setStatusBarColor(Color.WHITE);
        }

        mRecyclerView = findViewById(R.id.about_rv);
        initRecyclerView();

        findViewById(R.id.about_back).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });
    }

    private void initRecyclerView() {
        CommonAdapter<String> commonAdapter = new CommonAdapter<String>(this, getSupportedAppList(), R.layout.item_about_source) {
            @Override
            public void bindData(CommonViewHolder viewHolder, final String source, int pos) {
                viewHolder.setText(R.id.item_as_name, SourceApp.getSourceText(source));

                ImageView appIcon = viewHolder.itemView.findViewById(R.id.item_as_icon);
                appIcon.setImageResource(getAppIcon(source));

                viewHolder.itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        showTrickBottomSheet(source);
                    }
                });
            }
        };
        mRecyclerView.setAdapter(commonAdapter);
    }

    private List<String> getSupportedAppList(){
        String[] list = new String[]{
                SourceApp.APP_WECHAT,
                SourceApp.APP_ZHIHU,
                SourceApp.APP_DOUBAN,
                SourceApp.APP_WEIBO,
                SourceApp.APP_QDAILY,
                SourceApp.APP_HUPU
        };
        return Arrays.asList(list);
    }

    private void showTrickBottomSheet(String source){
        View view = getLayoutInflater().inflate(R.layout.bottom_sheet_about_trick, null);
        ImageView trickImg = view.findViewById(R.id.bsat_img);
        trickImg.setImageBitmap(getTrickImage(source));

        RoundedBottomSheetDialog bottomSheetDialog = new RoundedBottomSheetDialog(this, view,
                R.style.BottomSheetDialog);
        bottomSheetDialog.show();
    }

    private int getAppIcon(String source){
        switch (source){
            case SourceApp.APP_WECHAT:
                return R.drawable.appicon_wechat;
            case SourceApp.APP_ZHIHU:
                return R.drawable.appicon_zhihu;
            case SourceApp.APP_DOUBAN:
                return R.drawable.appicon_douban;
            case SourceApp.APP_WEIBO:
                return R.drawable.appicon_weibo;
            case SourceApp.APP_QDAILY:
                return R.drawable.appicon_qdaily;
            case SourceApp.APP_HUPU:
                return R.drawable.appicon_hupu;
        }
        return R.mipmap.ic_launcher;
    }

    private Bitmap getTrickImage(String source){
        String FOLDER = "trick_image/";
        AssetManager manager = getAssets();
        try {
            return BitmapFactory.decodeStream(manager.open(FOLDER + getTrickImageName(source)));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private String getTrickImageName(String source) {
        switch (source){
            case SourceApp.APP_WECHAT:
                return "about_wechat.png";
            case SourceApp.APP_ZHIHU:
                return "about_zhihu.png";
            case SourceApp.APP_DOUBAN:
                return "about_douban.png";
            case SourceApp.APP_WEIBO:
                return "about_weibo.png";
            case SourceApp.APP_QDAILY:
                return "about_qdaily.png";
            case SourceApp.APP_HUPU:
                return "about_hupu.png";
        }
        return null;
    }
}
