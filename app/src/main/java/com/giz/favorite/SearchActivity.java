package com.giz.favorite;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;
import android.transition.Slide;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.Collections;
import java.util.List;

import database.FavoriteItemLib;
import datatool.FavoriteItem;
import datatool.FavoriteTool;
import datatool.SourceApp;
import utility.CommonAdapter;
import utility.CommonUtil;
import utility.CommonViewHolder;
import utility.RoundedImageView;

public class SearchActivity extends AppCompatActivity {

    private RecyclerView mRecyclerView;
    private EditText mEditText;
    private ImageView mBackIcon;

    private CommonAdapter<FavoriteItem> mCommonAdapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);
        Slide slide = new Slide(Gravity.END);
        slide.setDuration(400);
        getWindow().setEnterTransition(slide);

        // 设置状态栏为白色，状态栏内容为深色
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
            getWindow().setStatusBarColor(Color.WHITE);
        }

        mRecyclerView = findViewById(R.id.search_rv);
        mEditText = findViewById(R.id.search_editText);
        mBackIcon = findViewById(R.id.search_back);

        // 初始化列表
        initRecyclerView();
        initEvents();
    }

    private void initRecyclerView() {
        mCommonAdapter = new CommonAdapter<FavoriteItem>(this, getFavoriteItemList(), R.layout.item_favorite) {
            @Override
            public void bindData(CommonViewHolder viewHolder, final FavoriteItem data, int pos) {
                View itemView = viewHolder.itemView;
                ((TextView)itemView.findViewById(R.id.item_favorite_title)).setText(data.getTitle());
                ((TextView)itemView.findViewById(R.id.item_favorite_date))
                        .setText(CommonUtil.getDateBriefDescription(data.getDate()));
                ((TextView)itemView.findViewById(R.id.item_favorite_source)).setText(SourceApp.getSourceText(data.getSource()));

                TextView archiveTv = itemView.findViewById(R.id.item_favorite_archive);
                if(data.getArchive() != null){
                    archiveTv.setVisibility(View.VISIBLE);
                    archiveTv.setText(data.getArchive());
                }
                else
                    archiveTv.setVisibility(View.GONE);

                Drawable drawable = CommonUtil.getAppIcon(SearchActivity.this, SourceApp.getAppPackageName(data.getSource()));
                ((RoundedImageView)itemView.findViewById(R.id.item_favorite_icon))
                        .setImageDrawable(drawable == null ? SearchActivity.this.getResources().getDrawable(R.mipmap.ic_launcher, SearchActivity.this.getTheme()) : drawable);
                Bitmap itemImg = FavoriteTool.getItemImage(data.getImagePath());
                if(itemImg != null){
                    ((ImageView)itemView.findViewById(R.id.item_favorite_image)).setImageBitmap(itemImg);
                    itemView.findViewById(R.id.item_favorite_image).setVisibility(View.VISIBLE);
                }
                else
                    itemView.findViewById(R.id.item_favorite_image).setVisibility(View.GONE);
                // 是否星标
                itemView.findViewById(R.id.item_favorite_star).setVisibility(data.isStarred() ? View.VISIBLE : View.GONE);

                itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent intent = RichContentActivity.newIntent(SearchActivity.this, data.getUUID().toString());
                        SearchActivity.this.startActivity(intent);
                    }
                });
            }
        };
        mRecyclerView.setAdapter(mCommonAdapter);
        mRecyclerView.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));
    }

    private void initEvents() {
        mBackIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

        mEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String text = s.toString();
                mCommonAdapter.updateData(FavoriteItemLib.get(SearchActivity.this).queryFavoriteItemByText(text));
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
    }

    List<FavoriteItem> getFavoriteItemList(){
        List<FavoriteItem> itemList = FavoriteItemLib.get(this).getFavoriteItemList();
        Collections.reverse(itemList);

        return itemList;
    }
}
