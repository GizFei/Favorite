package com.giz.favorite;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BlurMaskFilter;
import android.graphics.Color;
import android.graphics.MaskFilter;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.circularreveal.CircularRevealFrameLayout;
import android.support.design.widget.TextInputEditText;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.Html;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextWatcher;
import android.text.method.LinkMovementMethod;
import android.text.style.AbsoluteSizeSpan;
import android.text.style.ClickableSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.ImageSpan;
import android.text.style.MaskFilterSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.StrikethroughSpan;
import android.text.style.StyleSpan;
import android.text.style.UnderlineSpan;
import android.transition.Fade;
import android.transition.Slide;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.select.Elements;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import database.FavoriteItemLib;
import datatool.FavoriteItem;
import datatool.FavoriteTool;
import datatool.SourceApp;
import utility.CommonAdapter;
import utility.CommonUtil;
import utility.CommonViewHolder;
import viewtool.CustomToast;

public class SelfContentActivity extends AppCompatActivity {

    private static final String TAG = "SelfContentActivity";
    private static final String EXTRA_DX = "extra_dx";
    private static final String EXTRA_DY = "extra_dy";
    private static final String EXTRA_START_RADIUS = "extra_sr";
    private static final String EXTRA_MODE = "extra_mode";
    private static final String EXTRA_UUID = "extra_uuid";
    private static final int REQUEST_IMAGE = 1;
    public static final int MODE_CREATE = 1;
    public static final int MODE_EDIT = 2;

    private CircularRevealFrameLayout mFrameLayout;
    private ImageView mBackIcon;
    private TextInputEditText mTitleEt;
    private EditText mContentEt;
    private Button mOkBtn;
    private Button mCancelBtn;
    private View mMaskView;

    private ImageView mBigAction;
    private ImageView mBoldAction;
    private ImageView mItalicAction;
    private ImageView mUnderlineAction;
    private ImageView mStrikeThroughAction;
    private ImageView mTextColorAction;
    private ImageView mImageAction;

    private int mDx;
    private int mDy;
    private int mSr;
    private int mMode;

    private FavoriteItem mFavoriteItem;

    private int originalContentEtHeight = 0;

//    private List<String> mFormatStringList = new ArrayList<>();
    private boolean mIsBigFont = false; // 大字体
    private boolean mIsItalic = false;  // 斜体
    private boolean mIsBold = false;    // 粗体
    private boolean mIsUnderline = false;    // 下划线
    private boolean mIsStrikeThrough = false;   // 删除线
    private int mTextColor = Color.BLACK;       // 字体颜色

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
        mMaskView = findViewById(R.id.self_content_mask);

        mBigAction = findViewById(R.id.sc_action_big);
        mBoldAction = findViewById(R.id.sc_action_bold);
        mItalicAction = findViewById(R.id.sc_action_italic);
        mUnderlineAction = findViewById(R.id.sc_action_underline);
        mStrikeThroughAction = findViewById(R.id.sc_action_strikethrough);
        mTextColorAction = findViewById(R.id.sc_action_text_color);
        mImageAction = findViewById(R.id.sc_action_image);

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
        if(mMode == MODE_EDIT) {
            // 编辑模式
            mTitleEt.setText(mFavoriteItem.getTitle());
            mContentEt.setText(Html.fromHtml(mFavoriteItem.getContent()));
        }else {
            // 创建模式
            String s = getIntent().getStringExtra(EXTRA_UUID);
            if(s != null)
                mContentEt.setText(s);
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
                String content = processContent();
                if(mMode == MODE_CREATE){
                    // 添加收藏项
                    FavoriteItem newItem = new FavoriteItem();
                    newItem.setSource(SourceApp.APP_SELF);  // 原创
                    newItem.setTitle(mTitleEt.getText().toString());
                    newItem.setContent(content);
                    FavoriteItemLib.get(SelfContentActivity.this).addFavoriteItem(newItem);

                    myOnBackPressed();
                }else{
                    // 更新收藏项
                    mFavoriteItem.setTitle(mTitleEt.getText().toString());
                    mFavoriteItem.setContent(content);
                    FavoriteItemLib.get(SelfContentActivity.this).updateFavoriteItem(mFavoriteItem);

                    setResult(RESULT_OK);
                    myOnBackPressed();
                }
            }
        });

        mContentEt.addTextChangedListener(new TextWatcher() {
            int mStart = 0;
            int mBefore = 0;
            int mCount = 0;
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
//                Log.d(TAG, "onTextChanged: " + start + " " + before + " " + count);
                mStart = start;
                mCount = count;
                mBefore = before;
            }

            @Override
            public void afterTextChanged(Editable s) {
                if(mIsBigFont){
                    s.setSpan(new AbsoluteSizeSpan((int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 24, getResources().getDisplayMetrics()))
                            , mStart, mStart + mCount, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
                if(mIsBold){
                    s.setSpan(new StyleSpan(Typeface.BOLD), mStart, mStart + mCount, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
                if(mIsItalic){
                    s.setSpan(new StyleSpan(Typeface.ITALIC), mStart, mStart + mCount, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
                if(mIsStrikeThrough){
                    s.setSpan(new StrikethroughSpan(), mStart, mStart + mCount, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
                if(mIsUnderline){
                    s.setSpan(new UnderlineSpan(), mStart, mStart + mCount, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
                s.setSpan(new ForegroundColorSpan(mTextColor), mStart, mStart + mCount, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        });
        mContentEt.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if(hasFocus){
                    findViewById(R.id.self_content_btns).setVisibility(View.GONE);
                    findViewById(R.id.self_content_actions).setVisibility(View.VISIBLE);
                }else{
                    findViewById(R.id.self_content_btns).setVisibility(View.VISIBLE);
                    findViewById(R.id.self_content_actions).setVisibility(View.GONE);
                }
            }
        });

        mBigAction.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mBigAction.isSelected()){
                    mIsBigFont = false;
                    mBigAction.setImageResource(R.drawable.ic_format_size_black);
                    mBigAction.setSelected(false);

                    AbsoluteSizeSpan[] sizeSpans = mContentEt.getText().getSpans(mContentEt.getSelectionStart(), mContentEt.getSelectionEnd(), AbsoluteSizeSpan.class);
                    for(AbsoluteSizeSpan sizeSpan : sizeSpans)
                        mContentEt.getText().removeSpan(sizeSpan);
                }else {
                    mIsBigFont = true;
                    mBigAction.setImageResource(R.drawable.ic_format_size_white);
                    mBigAction.setSelected(true);
                    // 对选中内容操作
                    mContentEt.getText().setSpan(new AbsoluteSizeSpan((int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 24, getResources().getDisplayMetrics()))
                            , mContentEt.getSelectionStart(), mContentEt.getSelectionEnd(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
            }
        });
        mBoldAction.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mBoldAction.isSelected()){
                    mIsBold = false;
                    mBoldAction.setImageResource(R.drawable.ic_format_bold_black);
                    mBoldAction.setSelected(false);

                    StyleSpan[] styleSpans = mContentEt.getText().getSpans(mContentEt.getSelectionStart(),
                            mContentEt.getSelectionEnd(), StyleSpan.class);
                    for(StyleSpan styleSpan : styleSpans){
                        if(styleSpan.getStyle() == Typeface.BOLD)
                            mContentEt.getText().removeSpan(styleSpan);
                    }
                }else {
                    mIsBold = true;
                    mBoldAction.setImageResource(R.drawable.ic_format_bold_white);
                    mBoldAction.setSelected(true);

                    mContentEt.getText().setSpan(new StyleSpan(Typeface.BOLD), mContentEt.getSelectionStart(),
                            mContentEt.getSelectionEnd(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
            }
        });
        mItalicAction.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mItalicAction.isSelected()){
                    mIsItalic = false;
                    mItalicAction.setImageResource(R.drawable.ic_format_italic_black);
                    mItalicAction.setSelected(false);

                    StyleSpan[] styleSpans = mContentEt.getText().getSpans(mContentEt.getSelectionStart(),
                            mContentEt.getSelectionEnd(), StyleSpan.class);
                    for(StyleSpan styleSpan : styleSpans){
                        if(styleSpan.getStyle() == Typeface.ITALIC)
                            mContentEt.getText().removeSpan(styleSpan);
                    }
                }else {
                    mIsItalic = true;
                    mItalicAction.setImageResource(R.drawable.ic_format_italic_white);
                    mItalicAction.setSelected(true);

                    mContentEt.getText().setSpan(new StyleSpan(Typeface.ITALIC), mContentEt.getSelectionStart(),
                            mContentEt.getSelectionEnd(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
            }
        });
        mUnderlineAction.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mUnderlineAction.isSelected()){
                    mIsUnderline = false;
                    mUnderlineAction.setImageResource(R.drawable.ic_format_underlined_black);
                    mUnderlineAction.setSelected(false);

                    UnderlineSpan[] styleSpans = mContentEt.getText().getSpans(mContentEt.getSelectionStart(),
                            mContentEt.getSelectionEnd(), UnderlineSpan.class);
                    for(UnderlineSpan styleSpan : styleSpans){
                        mContentEt.getText().removeSpan(styleSpan);
                    }
                }else {
                    mIsUnderline = true;
                    mUnderlineAction.setImageResource(R.drawable.ic_format_underlined_white);
                    mUnderlineAction.setSelected(true);

                    mContentEt.getText().setSpan(new UnderlineSpan(), mContentEt.getSelectionStart(),
                            mContentEt.getSelectionEnd(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
            }
        });
        mStrikeThroughAction.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mStrikeThroughAction.isSelected()){
                    mIsStrikeThrough = false;
                    mStrikeThroughAction.setImageResource(R.drawable.ic_format_strikethrough_black);
                    mStrikeThroughAction.setSelected(false);

                    StrikethroughSpan[] styleSpans = mContentEt.getText().getSpans(mContentEt.getSelectionStart(),
                            mContentEt.getSelectionEnd(), StrikethroughSpan.class);
                    for(StrikethroughSpan styleSpan : styleSpans){
                        mContentEt.getText().removeSpan(styleSpan);
                    }
                }else {
                    mIsStrikeThrough = true;
                    mStrikeThroughAction.setImageResource(R.drawable.ic_format_strikethrough_white);
                    mStrikeThroughAction.setSelected(true);

                    mContentEt.getText().setSpan(new StrikethroughSpan(), mContentEt.getSelectionStart(),
                            mContentEt.getSelectionEnd(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
            }
        });
        mTextColorAction.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                chooseTextColor();
            }
        });
        mImageAction.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                insertImage();
            }
        });

        mContentEt.post(new Runnable() {
            @Override
            public void run() {
                originalContentEtHeight = mContentEt.getHeight();
                showLocalImage();
            }
        });
        mContentEt.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
                if(mContentEt.getHeight() == originalContentEtHeight){
                    // 收起软键盘
                    findViewById(R.id.self_content_btns).setVisibility(View.VISIBLE);
                    findViewById(R.id.self_content_actions).setVisibility(View.GONE);
                }else if(mContentEt.hasFocus()){
                    // 有焦点弹起
                    findViewById(R.id.self_content_btns).setVisibility(View.GONE);
                    findViewById(R.id.self_content_actions).setVisibility(View.VISIBLE);
                }
            }
        });
    }

    private void chooseTextColor() {
        // 选择颜色
        List<Integer> colorList = Arrays.asList(Color.BLACK, Color.RED, Color.BLUE, Color.YELLOW, Color.GREEN, Color.CYAN, Color.MAGENTA);
        final PopupWindow popupWindow = new PopupWindow(this);

        View view = getLayoutInflater().inflate(R.layout.popup_text_color, null);
        RecyclerView recyclerView = view.findViewById(R.id.popup_text_color_rv);
        CommonAdapter<Integer> colorAdapter = new CommonAdapter<Integer>(this, colorList, R.layout.item_popup_text_color) {
            @Override
            public void bindData(CommonViewHolder viewHolder, final Integer data, int pos) {
                ((CardView)viewHolder.itemView).setCardBackgroundColor(data);
                viewHolder.itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mTextColor = data;
                        popupWindow.dismiss();
                    }
                });
            }
        };
        recyclerView.setAdapter(colorAdapter);

        popupWindow.setContentView(view);
        popupWindow.setElevation(CommonUtil.dp2px(this, 8));
        popupWindow.setBackgroundDrawable(getResources().getDrawable(R.drawable.bg_popup_main));
        popupWindow.showAsDropDown(mTextColorAction,0, -(int)CommonUtil.dp2px(this, 80), Gravity.END);

        mMaskView.setVisibility(View.VISIBLE);
        popupWindow.setOnDismissListener(new PopupWindow.OnDismissListener() {
            @Override
            public void onDismiss() {
                mMaskView.setVisibility(View.GONE);
                mTextColorAction.setBackgroundTintList(ColorStateList.valueOf(mTextColor));
                mMaskView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) { }
                });

                ForegroundColorSpan[] foregroundColorSpans = mContentEt.getText().getSpans(mContentEt.getSelectionStart(),
                        mContentEt.getSelectionEnd(), ForegroundColorSpan.class);
                for(ForegroundColorSpan colorSpan : foregroundColorSpans)
                    mContentEt.getText().removeSpan(colorSpan);
                mContentEt.getText().setSpan(new ForegroundColorSpan(mTextColor), mContentEt.getSelectionStart(),
                        mContentEt.getSelectionEnd(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        });
        mMaskView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                popupWindow.dismiss();
            }
        });
    }

    private void insertImage() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setData(MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, REQUEST_IMAGE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(resultCode == RESULT_OK){
            if(requestCode == REQUEST_IMAGE){
                if(data != null && data.getData() != null){
                    int pos = mContentEt.getSelectionStart();
                    try {
                        Editable editable = mContentEt.getText();

                        String path;
                        String realPath = getPath(data.getData());
                        int len;
                        if(pos == 0){
                            path = "<img src=\"" + realPath + "\" />\n";
                            len = path.length() - 1;
                            editable.append(path);
                        }else{
                            path = "\n<img src=\"" + realPath + "\" />\n";
                            len = path.length() - 2;
                            editable.insert(pos, path);
                            pos += 1;
                        }
//                        Log.d(TAG, "onActivityResult: " + path);

                        Bitmap bitmap = BitmapFactory.decodeStream(getContentResolver().openInputStream(data.getData()));
                        int degree = FavoriteTool.getRotateDegree(realPath);
                        bitmap = CommonUtil.rotateBitmap(bitmap, degree);

                        int width = mContentEt.getWidth() - mContentEt.getPaddingLeft() - mContentEt.getPaddingRight();
                        float factor = (float)width / bitmap.getWidth();
                        Drawable drawable = new BitmapDrawable(getResources(), bitmap);
                        drawable.setBounds(0, 8, width, (int)(bitmap.getHeight() * factor));

                        editable.setSpan(new ImageSpan(drawable, ImageSpan.ALIGN_BASELINE), pos, pos + len, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
//                        Log.d(TAG, "onActivityResult: " + editable.toString());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
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

    private String getPath(Uri uri){
        Cursor cursor = managedQuery(uri, new String[]{MediaStore.Images.Media.DATA}, null, null, null);
        int col = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
        cursor.moveToFirst();
        return cursor.getString(col);
    }

    private String processContent(){
        Editable editable = mContentEt.getText();
        ImageSpan[] imageSpans = editable.getSpans(0, editable.length(), ImageSpan.class);
        for(ImageSpan span : imageSpans){
            editable.removeSpan(span);
        }
//        Log.d(TAG, "processContent: " + editable.toString());

        String string = Html.toHtml(editable);

        Document document = Jsoup.parse(string);
        Elements spans = document.select("span");
        for(Element span : spans){
            if(span.attr("style").contains("font-size")){
                span.html("<big>" + span.html() + "</big");
            }
        }
        Log.d(TAG, "processContent: " + document.toString());

        return document.toString();
    }

    private void showLocalImage() {
        Editable editable = mContentEt.getText();
        Pattern pattern = Pattern.compile("(<img src=\"(.*)\" />)");
        Matcher matcher = pattern.matcher(editable.toString());
        while(matcher.find()) {
            Log.d(TAG, "onCreate: " + matcher.group(2));
            String path = matcher.group(2);

            int degree = FavoriteTool.getRotateDegree(path);
            Bitmap bitmap = CommonUtil.rotateBitmap(BitmapFactory.decodeFile(path), degree);
            int width = mContentEt.getWidth() - mContentEt.getPaddingLeft() - mContentEt.getPaddingRight();
            float factor = (float)width / bitmap.getWidth();
            Drawable drawable = new BitmapDrawable(getResources(), bitmap);
            drawable.setBounds(0, 0, width, (int)(factor * bitmap.getHeight()));

            editable.setSpan(new ImageSpan(drawable, ImageSpan.ALIGN_BASELINE), matcher.start(), matcher.end(),
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
    }
}
