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

    private CircularRevealFrameLayout mFrameLayout;
    private ImageView mBackIcon;
    private TextInputEditText mTitleEt;
    private EditText mContentEt;
    private Button mOkBtn;

    private int mDx;
    private int mDy;
    private int mSr;

    public static Intent newIntent(Context context, int dx, int dy, int startRadius){
        Intent intent = new Intent(context, SelfContentActivity.class);
        intent.putExtra(EXTRA_DX, dx);
        intent.putExtra(EXTRA_DY, dy);
        intent.putExtra(EXTRA_START_RADIUS, startRadius);

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
                animator.setDuration(800);
                animator.start();
            }
        });
        mBackIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                myOnBackPressed();
            }
        });
        mOkBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 添加收藏项
                FavoriteItem newItem = new FavoriteItem();
                newItem.setSource(SourceApp.APP_SELF);  // 原创
                newItem.setTitle(mTitleEt.getText().toString());
                newItem.setContent(mContentEt.getText().toString());
                FavoriteItemLib.get(SelfContentActivity.this).addFavoriteItem(newItem);

                myOnBackPressed();
            }
        });
    }

    private void myOnBackPressed(){
        int endRadius = (int)Math.hypot(mFrameLayout.getWidth(), mFrameLayout.getHeight());
        Animator animator = ViewAnimationUtils.createCircularReveal(mFrameLayout, mDx, mDy, endRadius, mSr);
        animator.setDuration(600);
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
