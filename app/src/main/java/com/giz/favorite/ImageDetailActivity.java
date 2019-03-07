package com.giz.favorite;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.transition.Fade;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageRequest;
import com.android.volley.toolbox.Volley;

import viewtool.PinchImageView;

public class ImageDetailActivity extends AppCompatActivity {
    private static final String TAG = "ImageDetailActivity";
    private static final String EXTRA_IMG = "imgUrl";

    private PinchImageView mPinchImageView;
    private RequestQueue mRequestQueue;

    public static Intent newIntent(Context context, String url){
        Intent intent = new Intent(context, ImageDetailActivity.class);
        intent.putExtra(EXTRA_IMG, url);

        return intent;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activty_image_detail);
        getWindow().setEnterTransition(new Fade());
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_FULLSCREEN);

        mPinchImageView = findViewById(R.id.img_detail_img);
        findViewById(R.id.img_detail_close).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

        if(getIntent() != null){
            mRequestQueue = Volley.newRequestQueue(this);
            postponeEnterTransition();
            String url = getIntent().getStringExtra(EXTRA_IMG);
            Log.d(TAG, "onCreate: url" + url);

            ImageRequest imageRequest = new ImageRequest(url, new Response.Listener<Bitmap>() {
                @Override
                public void onResponse(Bitmap response) {
                    mPinchImageView.setImageBitmap(response);
                    startPostponedEnterTransition();
                }
            }, 0, 0, ImageView.ScaleType.CENTER_CROP, Bitmap.Config.RGB_565,
                    new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            // 获取图片错误
                        }
                    });
            mRequestQueue.add(imageRequest);
        }
    }
}
