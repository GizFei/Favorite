package com.giz.favorite;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.CoordinatorLayout;
import android.support.graphics.drawable.Animatable2Compat;
import android.support.graphics.drawable.AnimatedVectorDrawableCompat;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.animation.FastOutSlowInInterpolator;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.transition.Fade;
import android.transition.Slide;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.File;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import database.FavoriteItemLib;
import datatool.FavoriteItem;
import datatool.FavoriteTool;
import datatool.ParseShareContent;
import datatool.SourceApp;
import utility.CommonUtil;
import viewtool.CustomToast;

public class CollectionActivity extends AppCompatActivity {
    // 透明Activity，完成第三方应用的收藏
    private static final String TAG = "CollectionActivity";
    private static final int WRITE_STORAGE_REQUEST_CODE = 1;
    private static final String EXTRA_CLIPTEXT = "clipText";

    private FavoriteItem mFavoriteItem;

    private ImageView mProgressView;
    private TextView mTipTextView;

    private boolean needToFetchTitle = false;

    public static Intent newIntent(Context context, String text){
        Intent intent = new Intent(context, CollectionActivity.class);
        intent.putExtra(EXTRA_CLIPTEXT, text);

        return intent;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_collection);

        getWindow().setEnterTransition(new Fade());
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);

        mProgressView = findViewById(R.id.collection_progress);
        mTipTextView = findViewById(R.id.collection_tv);

        if(ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED)
            requestStoragePermission();
        else {
            collectItem();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 布局动画
        findViewById(R.id.collection_ll).animate().translationY(0).setDuration(400);
        Animation progressAnim = AnimationUtils.loadAnimation(this, R.anim.progress_rotate_forever);
        progressAnim.setInterpolator(new FastOutSlowInInterpolator());
        mProgressView.startAnimation(progressAnim);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(requestCode == WRITE_STORAGE_REQUEST_CODE){
            if(grantResults[0] != PackageManager.PERMISSION_GRANTED){
                CustomToast.make(this, "权限申请失败，收藏失败").show();
                onBackPressed();
            }else {
                collectItem();
            }
        }
    }

    private class NetTask extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... strings) {
            try {
                Log.d(TAG, "doInBackground: url: [" + strings[0].trim() + "]");
                HttpURLConnection connection = getAvailableConnection(strings[0].trim());
                int requestCode = connection.getResponseCode();
                Log.d(TAG, "doInBackground: requestCode:" + requestCode);
                if(requestCode == HttpURLConnection.HTTP_OK){
                    String result = CommonUtil.fetchHtml(connection);
                    if(mFavoriteItem.getSource().equals(SourceApp.APP_DOUBAN)){
                        // 豆瓣特殊处理
                        Pattern pattern = Pattern.compile("(?:.*)h5url : '(.*)'.replace(?:.*)");
                        Matcher matcher = pattern.matcher(result.toString());
                        if(matcher.find()){
                            // 新的Url
                            String newUrl = matcher.group(1).replace("&amp;", "&")
                                    .replace("m.douban.com", "www.douban.com");
                            Log.d(TAG, "doInBackground: douban new url: " + newUrl);
                            HttpURLConnection newConnection = getAvailableConnection(newUrl);
                            mFavoriteItem.setUrl(newUrl);
                            if(newConnection.getResponseCode() == HttpURLConnection.HTTP_OK){
                                return CommonUtil.fetchHtml(newConnection);
                            }
                        }
                    }else
                        return result;
                } else {
                    Log.d(TAG, "doInBackground: http fail");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(String s) {
            if(s == null){
                Log.d(TAG, "onPostExecute: null html");
                onBackPressed();
                return;
            }
//            CommonUtil.logAllContent(TAG, s);
            Document document = Jsoup.parse(s);
            switch (mFavoriteItem.getSource()){
                case SourceApp.APP_ZHIHU:
                    fetchZhiHuItemImage(document);
                    break;
                case SourceApp.APP_HUPU:
                    fetchHuPuItemImage(document);
                    break;
                case SourceApp.APP_QDAILY:
                    fetchQDailyItemImage(document);
                    break;
                case SourceApp.APP_DOUBAN:
                    fetchDouBanItemImage(document);
                    break;
                case SourceApp.APP_WEIBO:
                    fetchWeiBoItemImage(document);
                    break;
                case SourceApp.APP_WECHAT:
                    fetchWeChatItemImage(document);
                    break;
                default:
                    Log.d(TAG, "onPostExecute: null source");
                    onBackPressed();
                    break;
            }
        }
    }

    /**
     * 处理重定向的问题，获取正确的连接
     * @param theUrl 网址
     * @return 连接
     * @throws Exception 异常
     */
    private HttpURLConnection getAvailableConnection(String theUrl) throws Exception{
        URL url = new URL(theUrl);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.connect();   // 连接
        int requestCode = connection.getResponseCode();
        if(requestCode == HttpURLConnection.HTTP_MOVED_TEMP
                || requestCode == HttpURLConnection.HTTP_MOVED_PERM){
            Log.d(TAG, "getAvailableConnection: location: " + connection.getHeaderField("Location"));
            return getAvailableConnection(connection.getHeaderField("Location"));
        }
        return connection;
    }

    /**
     * 收藏项的主函数
     */
    private void collectItem(){
        Intent intent = getIntent();
        if(intent != null){
            logIntent(intent);
            if(intent.getAction() != null && intent.getAction().equals(Intent.ACTION_SEND)
                    && intent.getClipData() != null){
                // 来源于“更多”的分享
                String shareContent = intent.getClipData().getItemAt(0).getText().toString();

                mFavoriteItem = ParseShareContent.formFavoriteItem(shareContent);
                if(mFavoriteItem != null){
                    switch (mFavoriteItem.getSource()){
                        case SourceApp.APP_ZHIHU:
                        case SourceApp.APP_HUPU:
                        case SourceApp.APP_QDAILY:
                        case SourceApp.APP_DOUBAN:
                            new NetTask().execute(mFavoriteItem.getUrl());
                            break;
                        default:
                            // 未知来源，当作原创
                            queryTitle(mFavoriteItem.getContent());
                            break;
                    }
                } else{
                    CustomToast.make(this, "未知来源").show();
                    onBackPressed();
                }
            }else {
                // 来源于Process_text
                if(intent.getAction() != null && intent.getAction().equals(Intent.ACTION_PROCESS_TEXT)){
                    if(Build.VERSION.SDK_INT > Build.VERSION_CODES.M){
                        String text = intent.getStringExtra(Intent.EXTRA_PROCESS_TEXT);
                        Log.d(TAG, "collectItem: from process text:" + text);

                        queryTitle(text);
                    }
                }else if(intent.hasExtra(EXTRA_CLIPTEXT)){
                    // 来自剪贴板的文本
                    String text = intent.getStringExtra(EXTRA_CLIPTEXT);
                    Log.d(TAG, "collectItem: from clipboard." + text);

                    queryTitle(text);
                }
            }
        }else {
            Toast.makeText(this, "收藏失败", Toast.LENGTH_SHORT).show();
            onBackPressed();
        }
    }

    private void logIntent(Intent intent){
        String action = intent.getAction();
        String type = intent.getType();
//        textView.setText(intent.toString() + "\\n\\n" + "ClipData: [" + intent.getClipData() + "]");
        Log.d(TAG, "onCreate: " + intent.toString());
        Log.d(TAG, "onCreate: " + action + type);
        Log.d(TAG, "onCreate:  Package" + intent.getPackage());
        Log.d(TAG, "onCreate:  Scheme" + intent.getScheme());
        if(intent.getComponent() != null)
            Log.d(TAG, "onCreate:  component" + intent.getComponent().flattenToShortString());
    }

    private void fetchZhiHuItemImage(Document document){
        // 第一张图片
        if(needToFetchTitle){
            // 抓取标题
            Element element = document.selectFirst(mFavoriteItem.getUrl().contains("zhuanlan") ? ".Post-Title" : ".QuestionHeader-title");
            if(element != null)
                mFavoriteItem.setTitle(element.text());
            else
                mFavoriteItem.setTitle(mFavoriteItem.getUrl().contains("zhuanlan") ? "来自知乎的文章" : "来自知乎的回答");
        }
        // 判断是否为专栏
        if(mFavoriteItem.getUrl().contains("zhuanlan")){
            Element img = document.selectFirst(".TitleImage");
            if(img != null){
                fetchImageAndStore(img.attr("src"));
                return;
            }
        }
        String clsName = mFavoriteItem.getUrl().contains("zhuanlan") ? ".Post-RichText" : ".RichContent-inner";
        fetchImageAndStore(document, clsName);
    }

    private void fetchHuPuItemImage(Document document){
        // 前面是论坛，后面是新闻
        if(needToFetchTitle){
            // 先抓取标题
            Element element = document.selectFirst(".artical-title h1");
            if(element != null)
                mFavoriteItem.setTitle(element.text());
            else
                mFavoriteItem.setTitle("来自虎扑的文章");
        }
        String clsName = mFavoriteItem.getUrl().contains("bbs.hupu.com") ? ".article-content" : ".artical-content";
        fetchImageAndStore(document, clsName);
    }

    private void fetchQDailyItemImage(Document document){
        // 抓取标题
        if(needToFetchTitle){
            Element element = document.selectFirst(".category-title .title");
            if(element != null)
                mFavoriteItem.setTitle(element.text());
            else
                mFavoriteItem.setTitle("来自好奇心日报的文章");
        }
        String clsName = ".com-article-detail .banner";
        fetchImageAndStore(document, clsName);
    }

    private void fetchDouBanItemImage(Document document) {
        if(mFavoriteItem.getUrl().contains("note")){
            // 日记
            String clsName = "#link-report";
            fetchImageAndStore(document, clsName);
        }else if(mFavoriteItem.getUrl().contains("status")){
            // 广播
            String clsName = ".status-saying";
            fetchImageAndStore(document, clsName);
        }else {
            // 其他，电影，书籍之类的分享
            fetchImageAndStore(document, ".subjectwrap");
        }
    }

    private void fetchWeiBoItemImage(Document document){
        // 微博
        String html = document.outerHtml();
        int idx1 = html.indexOf("render_data");
        int idx2 = html.lastIndexOf("[0] || {}");
//        Log.d(TAG, "fetchWeiBoItemImage: " + idx1 + " " + idx2);
        String renderData = html.substring(idx1 + 14, idx2);
        try {
            JSONArray jsonArray = new JSONArray(renderData);
            if(needToFetchTitle){
                // 标题
                String title = jsonArray.getJSONObject(0).getJSONObject("status").getString("status_title");
                mFavoriteItem.setTitle(title);

                Log.d(TAG, "fetchWeiBoItemImage: title: [" + title + "]");
            }
            String imgUrl = jsonArray.getJSONObject(0).getJSONObject("status").getJSONArray("pics")
                    .getJSONObject(0).getString("url");
            fetchImageAndStore(imgUrl);

            Log.d(TAG, "fetchWeiBoItemImage: img: [" + imgUrl + "]");
        } catch (JSONException e) {
            Log.d(TAG, "fetchWeiBoItemImage: null json array");

            if(needToFetchTitle)
                mFavoriteItem.setTitle("微博正文");
            storeItem();
        }
    }

    private void fetchWeChatItemImage(Document document) {
        // 微信
        if(needToFetchTitle){
            // 先抓取标题
            Element element = document.selectFirst(".rich_media_title");
            if(element != null){
                mFavoriteItem.setTitle(element.text());
            }
            else
                mFavoriteItem.setTitle("来自微信的文章");
        }
        fetchImageAndStore(document, ".rich_media_content");
    }

    /**
     * 抓取图像并保存
     * @param document 文档元素
     * @param clsName 包围img的父元素的类名
     */
    private void fetchImageAndStore(Document document, String clsName){
        Element div = document.selectFirst(clsName);
        if(div != null){
//            Log.d(TAG, "fetchImageAndStore: div" + div.outerHtml());
            Elements imgElms = div.select("img");
            if(!imgElms.isEmpty()){
                boolean ifFetch = false;
                for(Element element : imgElms){
                    String imgUrl = element.attr("src");
                    if(mFavoriteItem.getSource().equals(SourceApp.APP_WECHAT)){
                        // 微信比较特殊
                        imgUrl = element.attr("data-src");
                    }
                    if(isUrl(imgUrl)) {
                        Log.d(TAG, "fetchItemImage: imgUrl:" + imgUrl);
                        Log.d(TAG, "fetchItemImage: imgElm:" + element.outerHtml());
                        requestImage(imgUrl);
                        ifFetch = true;
                        break;
                    }
                }
                if(!ifFetch)
                    storeItem();
            }
            else
                storeItem();
        }else {
            Log.d(TAG, "fetchImageAndStore: null div");
            showFailInfo();
        }
    }

    /**
     * 抓取图片并保存
     * @param url 网址
     */
    private void fetchImageAndStore(String url){
        requestImage(url);
    }

    private void queryTitle(String content){
        // 来源为“原创”，提示输入标题
        final EditText editText = new EditText(this);

        if(content.matches("(https?|ftp|file)://[-A-Za-z0-9+&@#/%?=~_|!:,.;]+[-A-Za-z0-9+&@#/%=~_|]")){
            // 来源是网址
            Log.d(TAG, "onPrimaryClipChanged: 是网址。");
            mFavoriteItem = ParseShareContent.formFavoriteItem(content);
            // todo 抓取标题
            if(mFavoriteItem != null){
                needToFetchTitle = true;
                new NetTask().execute(content);
            }
            else{
                showFailInfo();
            }
        }else{
            // 人工输入标题
            mFavoriteItem = new FavoriteItem();
            mFavoriteItem.setSource(SourceApp.APP_SELF);
            mFavoriteItem.setContent(content);

            new AlertDialog.Builder(this)
                    .setTitle("添加标题")
                    .setView(editText)
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            mFavoriteItem.setTitle(editText.getText().toString());
                        }
                    }).setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    mFavoriteItem.setTitle(CommonUtil.getDateDetailDescription(mFavoriteItem.getDate()) + " 的收藏");
                }
            }).setOnDismissListener(new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialog) {
                    FavoriteItemLib.get(CollectionActivity.this).addFavoriteItem(mFavoriteItem);
                    successToCollect();
                }
            }).show();
        }
    }

    /**
     * 保存收藏项到数据库中
     */
    private void storeItem(){
        Log.d(TAG, "storeItem: ");
        FavoriteItemLib.get(CollectionActivity.this).addFavoriteItem(mFavoriteItem);
        successToCollect();
    }

    /**
     * 收藏成功后，播放动画并结束活动
     */
    private void successToCollect(){
        Log.d(TAG, "successToCollect: ");
        mProgressView.clearAnimation();
        AnimatedVectorDrawableCompat drawableCompat = AnimatedVectorDrawableCompat.create(this, R.drawable.avd_collection_progress);
        mProgressView.setImageDrawable(drawableCompat);
        mTipTextView.setText("收藏成功");
        if(drawableCompat != null){
            drawableCompat.registerAnimationCallback(new Animatable2Compat.AnimationCallback() {
                @Override
                public void onAnimationEnd(Drawable drawable) {
                    super.onAnimationEnd(drawable);
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            Log.d(TAG, "run: ");
                            onBackPressed();
                        }
                    }, 800);
                }
            });
            drawableCompat.start();
        }else {
            Log.d(TAG, "successToCollect: no drawable");
            onBackPressed();
        }
    }

    /**
     * 显示失败信息并结束活动
     */
    private void showFailInfo(){
        mProgressView.clearAnimation();
        AnimatedVectorDrawableCompat drawableCompat = AnimatedVectorDrawableCompat.create(this, R.drawable.avd_collection_fail);
        mProgressView.setImageDrawable(drawableCompat);
        mTipTextView.setText("收藏失败");
        if(drawableCompat != null){
            drawableCompat.registerAnimationCallback(new Animatable2Compat.AnimationCallback() {
                @Override
                public void onAnimationEnd(Drawable drawable) {
                    super.onAnimationEnd(drawable);
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            Log.d(TAG, "run: ");
                            onBackPressed();
                        }
                    }, 800);
                }
            });
            drawableCompat.start();
        }else {
            Log.d(TAG, "showFailInfo: ");
            onBackPressed();
        }
    }

    private void requestImage(String imgUrl){
        RequestQueue queue = Volley.newRequestQueue(this);
        ImageRequest imageRequest = new ImageRequest(imgUrl, new Response.Listener<Bitmap>() {
            @Override
            public void onResponse(Bitmap response) {
                String imgPath = FavoriteTool.saveItemImage(mFavoriteItem.getUUID().toString(), response);
                Log.d(TAG, "onResponse: save image to: " + imgPath);
                if(imgPath != null){
                    mFavoriteItem.setImagePath(imgPath);
                    tellSystemGallery(imgPath);
                }
                storeItem();
            }
        }, 0, 0, ImageView.ScaleType.CENTER_CROP, Bitmap.Config.RGB_565,
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.d(TAG, "onErrorResponse: fail to get image" + error.getMessage());
                        storeItem();
                    }
                });
        queue.add(imageRequest);
    }

    // 通知系统添加了图片
    private void tellSystemGallery(String imgPath) {
        Intent intent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        Uri uri = Uri.fromFile(new File(imgPath));
        intent.setData(uri);
        this.sendBroadcast(intent);
    }

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
                                ActivityCompat.requestPermissions(CollectionActivity.this,
                                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, WRITE_STORAGE_REQUEST_CODE);
                            }
                        }).setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {
                        Toast.makeText(CollectionActivity.this, "没有相关权限，收藏失败", Toast.LENGTH_SHORT).show();
                        onBackPressed();
                    }
                }).show();
            }else {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        WRITE_STORAGE_REQUEST_CODE);
            }
        }
    }

    private boolean isUrl(String text){
        return text.matches("(https?|ftp|file)://[-A-Za-z0-9+&@#/%?=~_|!:,.;]+[-A-Za-z0-9+&@#/%=~_|]");
    }
}
