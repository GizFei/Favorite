package com.giz.favorite;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.circularreveal.CircularRevealFrameLayout;
import android.support.design.widget.TextInputEditText;
import android.support.v7.app.AppCompatActivity;
import android.transition.Fade;
import android.transition.Slide;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;

import database.FavoriteItemLib;
import datatool.FavoriteItem;
import datatool.SourceApp;

public class SelfContentActivity extends AppCompatActivity {

    private static final String EXTRA_DX = "extra_dx";
    private static final String EXTRA_DY = "extra_dy";
    private static final String EXTRA_START_RADIUS = "extra_sr";
    private static final String EXTRA_MODE = "extra_mode";
    private static final String EXTRA_UUID = "extra_uuid";
    public static final int MODE_CREATE = 1;
    public static final int MODE_EDIT = 2;

    private CircularRevealFrameLayout mFrameLayout;
    private ImageView mBackIcon;
    private TextInputEditText mTitleEt;
    private EditText mContentEt;
    private Button mOkBtn;
    private Button mCancelBtn;

    private int mDx;
    private int mDy;
    private int mSr;
    private int mMode;

    private FavoriteItem mFavoriteItem;

    public static Intent newIntent(Context context, int dx, int dy, int startRadius, int mode, @Nullable String uuid){
        Intent intent = new Intent(context, SelfContentActivity.class);
        intent.putExtra(EXTRA_DX, dx);
        intent.putExtra(EXTRA_DY, dy);
        intent.putExtra(EXTRA_START_RADIUS, startRadius);
        intent.putExtra(EXTRA_MODE, mode);
        intent.putExtra(EXTRA_UUID, uuid);

        return intent;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_self_content);
        Fade fade = new Fade();
        fade.setDuration(50);
        getWindow().setEnterTransition(fade);

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
            getWindow().setStatusBarColor(Color.WHITE);
        }

        mFrameLayout = findViewById(R.id.self_content_rootView);
        mTitleEt = findViewById(R.id.self_content_title);
        mContentEt = findViewById(R.id.self_content_content);
        mBackIcon = findViewById(R.id.self_content_back);
        mOkBtn = findViewById(R.id.self_content_ok_btn);
        mCancelBtn = findViewById(R.id.self_content_cancel_btn);

        mMode = getIntent().getIntExtra(EXTRA_MODE, MODE_CREATE);
        if(mMode == MODE_EDIT){
            String uuid = getIntent().getStringExtra(EXTRA_UUID);
            mFavoriteItem = FavoriteItemLib.get(this).findFavoriteItemById(uuid);
        }
        initViews();
    }

    private void initViews() {
        mFrameLayout.post(new Runnable() {
            @Override
            public void run() {
                mDx = getIntent().getIntExtra(EXTRA_DX, mFrameLayout.getWidth() / 2);
                mDy = getIntent().getIntExtra(EXTRA_DY, mFrameLayout.getBottom());
                mSr = getIntent().getIntExtra(EXTRA_START_RADIUS, 0);

                int endRadius = (int)Math.hypot(mFrameLayout.getWidth(), mFrameLayout.getHeight());
                Animator animator = ViewAnimationUtils.createCircularReveal(mFrameLayout, mDx, mDy, mSr, endRadius);
                animator.setDuration(560);
                animator.start();
            }
        });
        if(mMode == MODE_EDIT){
            // 编辑模式
            mTitleEt.setText(mFavoriteItem.getTitle());
            mContentEt.setText(mFavoriteItem.getContent());
        }
        mBackIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                myOnBackPressed();
            }
        });
        mCancelBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                myOnBackPressed();
            }
        });
        mOkBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mMode == MODE_CREATE){
                    // 添加收藏项
                    FavoriteItem newItem = new FavoriteItem();
                    newItem.setSource(SourceApp.APP_SELF);  // 原创
                    newItem.setTitle(mTitleEt.getText().toString());
                    newItem.setContent(mContentEt.getText().toString());
                    FavoriteItemLib.get(SelfContentActivity.this).addFavoriteItem(newItem);

                    myOnBackPressed();
                }else{
                    // 更新收藏项
                    mFavoriteItem.setTitle(mTitleEt.getText().toString());
                    mFavoriteItem.setContent(mContentEt.getText().toString());
                    FavoriteItemLib.get(SelfContentActivity.this).updateFavoriteItem(mFavoriteItem);

                    setResult(RESULT_OK);
                    myOnBackPressed();
                }
            }
        });
    }

    private void myOnBackPressed(){
        int endRadius = (int)Math.hypot(mFrameLayout.getWidth(), mFrameLayout.getHeight());
        Animator animator = ViewAnimationUtils.createCircularReveal(mFrameLayout, mDx, mDy, endRadius, mSr);
        animator.setDuration(480);
        animator.start();
        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                mFrameLayout.setAlpha(0);
                SelfContentActivity.super.onBackPressed();
            }
        });
    }

    @Override
    public void onBackPressed() {
        myOnBackPressed();
    }
}
