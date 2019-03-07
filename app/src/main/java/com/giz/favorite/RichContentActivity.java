package com.giz.favorite;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LevelListDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.IdRes;
import android.support.annotation.MainThread;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.Html;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.text.method.ScrollingMovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.ImageSpan;
import android.util.Log;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebIconDatabase;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.toolbox.ImageRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;
import org.xml.sax.XMLReader;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import database.FavoriteItemLib;
import datatool.FavoriteItem;
import datatool.FavoriteTool;
import datatool.HttpSingleTon;
import datatool.SourceApp;
import utility.CommonUtil;
import utility.RoundedImageView;
import viewtool.CustomToast;

public class RichContentActivity extends AppCompatActivity {
    // 展示富文本信息的活动
    private static final String TAG = "RichContentActivity";
    private static final String EXTRA_UUID = "extra_uuid";

    private TextView mTextView;
    private TextView mUsernameTv;
    private RoundedImageView mAvatarImg;
    private LinearLayout mUserWrapper;

    private FavoriteItem mFavoriteItem;
    private Html.ImageGetter mImageGetter;
    Html.TagHandler mTagHandler;

    public static Intent newIntent(Context context, String uuid){
        Intent intent = new Intent(context, RichContentActivity.class);
        intent.putExtra(EXTRA_UUID, uuid);

        return intent;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rich_content);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);

        mTextView = findViewById(R.id.rich_content_tv);
        mUsernameTv = findViewById(R.id.rich_content_username);
        mAvatarImg = findViewById(R.id.rich_content_user_avatar);
        mUserWrapper = findViewById(R.id.rich_content_user_wrapper);
//        mTextView.setMovementMethod(ScrollingMovementMethod.getInstance());     // 使能够滑动
        mTextView.setMovementMethod(LinkMovementMethod.getInstance());          // 使能够响应点击事件

        String uuid = getIntent().getStringExtra(EXTRA_UUID);
        mFavoriteItem = FavoriteItemLib.get(this).findFavoriteItemById(uuid);

        initHtmlGetter();

        if(mFavoriteItem != null){
            setBasicInfo();         // 填充基本信息

            switch (mFavoriteItem.getSource()){
                case SourceApp.APP_ZHIHU:
                case SourceApp.APP_HUPU:
                case SourceApp.APP_QDAILY:
                case SourceApp.APP_DOUBAN:
                case SourceApp.APP_WEIBO:
                case SourceApp.APP_WECHAT:
                    if(CommonUtil.isNetworkAvailable(this)){
                        // 网络可用
                        findViewById(R.id.rich_content_tip_no).setVisibility(View.GONE);
                        new NetTask().execute(mFavoriteItem.getUrl());
                    }else{
                        // 网络不可用
                        findViewById(R.id.rich_content_tip_no).setVisibility(View.VISIBLE);
                        if(mFavoriteItem.getContent() != null){
                            mTextView.setText(Html.fromHtml(mFavoriteItem.getContent(), mImageGetter, mTagHandler));
                        }else{
                            mTextView.setText("离线保存的内容为空");
                        }
                    }
                    break;
                case SourceApp.APP_SELF:
                    mTextView.setText(mFavoriteItem.getContent());
                    break;
            }
        }else{
            CustomToast.make(this, "收藏项内容为空").show();
        }
    }

    /**
     * 初始化富文本处理工具
     */
    private void initHtmlGetter() {
        mImageGetter = new Html.ImageGetter() {
            @Override
            public Drawable getDrawable(String source) {
                LevelListDrawable levelListDrawable = new LevelListDrawable();
                Drawable drawable = getResources().getDrawable(R.drawable.skeleton);
                levelListDrawable.addLevel(0, 0, drawable);
                int width = mTextView.getWidth() - mTextView.getPaddingRight() - mTextView.getPaddingLeft();
                levelListDrawable.setBounds(0, 0, width, drawable.getIntrinsicHeight());

                new ImageTask().execute(source, levelListDrawable);
                return levelListDrawable;
            }
        };

        // 处理图片链接
        mTagHandler = new Html.TagHandler() {
            @Override
            public void handleTag(boolean opening, String tag, Editable output, XMLReader xmlReader) {
//                Log.d(TAG, "handleTag: tag--" + tag);
                if(tag.toLowerCase().equals("img")){
//                    Log.d(TAG, "handleTag: img: " + output);
                    int length = output.length();   // 长度
                    // 获取图片地址
                    ImageSpan[] images = output.getSpans(length - 1, length, ImageSpan.class);
                    final String imgURL = images[0].getSource();
                    output.setSpan(new ClickableSpan() {
                        @Override
                        public void onClick(@NonNull View widget) {
                            Intent intent = ImageDetailActivity.newIntent(RichContentActivity.this, imgURL);
                            startActivity(intent, ActivityOptionsCompat.makeSceneTransitionAnimation(RichContentActivity.this).toBundle());
                        }
                    }, length - 1, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
            }
        };
    }

    /**
     * 填充基本信息
     */
    private void setBasicInfo() {
        setText(R.id.rich_content_title, mFavoriteItem.getTitle());
        setText(R.id.rich_content_source, SourceApp.getSourceText(mFavoriteItem.getSource()));
        setText(R.id.rich_content_date, CommonUtil.getDateDetailDescription(mFavoriteItem.getDate()));
        Drawable icon = CommonUtil.getAppIcon(this, SourceApp.getAppPackageName(mFavoriteItem.getSource()));
        if(icon != null)
            ((RoundedImageView)findViewById(R.id.rich_content_icon)).setImageDrawable(icon);
        else
            ((RoundedImageView)findViewById(R.id.rich_content_icon)).setImageResource(R.mipmap.ic_launcher);
    }

    private class NetTask extends AsyncTask<String, Void, String>{

        @Override
        protected String doInBackground(String... strings) {
            try {
                HttpURLConnection connection = getAvailableConnection(strings[0]);
                int requestCode = connection.getResponseCode();
                if(requestCode == HttpURLConnection.HTTP_OK){
                    return CommonUtil.fetchHtml(connection);
                }
            } catch (Exception e) {
                Log.d(TAG, "doInBackground: fail to get url");
            }
            return null;
        }

        @Override
        protected void onPostExecute(String s) {
            if(s == null){
                Log.d(TAG, "onPostExecute: null html");
                mTextView.setText("没有抓取到内容(+_+)?");
                return;
            }
            dispatchHTML(s);
        }
    }

    /**
     * 分配网页内容
     * @param html 网页内容
     */
    private void dispatchHTML(String html){
        switch (mFavoriteItem.getSource()){
            case SourceApp.APP_ZHIHU:
                showZhiHuRichContent(html);
                break;
            case SourceApp.APP_HUPU:
                showHuPuRichContent(html);
                break;
            case SourceApp.APP_QDAILY:
                showQDailyRichContent(html);
                break;
            case SourceApp.APP_DOUBAN:
                showDouBanRichContent(html);
                break;
            case SourceApp.APP_WEIBO:
                showWeiBoRichContent(html);
                break;
            case SourceApp.APP_WECHAT:
                showWeChatRichContent(html);
                break;
        }
    }

    // 处理可能的重定向，获取最后的连接
    private HttpURLConnection getAvailableConnection(String theUrl) throws Exception{
        URL url = new URL(theUrl);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.connect();   // 连接
        int requestCode = connection.getResponseCode();
        if(requestCode == HttpURLConnection.HTTP_MOVED_TEMP
                || requestCode == HttpURLConnection.HTTP_MOVED_PERM){
            Log.d(TAG, "getAvailableConnection: new location: " + connection.getHeaderField("Location"));
            return getAvailableConnection(connection.getHeaderField("Location"));
        }
        return connection;
    }

    private void showZhiHuRichContent(String html){
        // https://www.zhihu.com/question/275144118/answer/594727214?utm_source=qq&utm_medium=social&utm_oi=732317165998329856
        Document document = Jsoup.parse(html);
        // 用户头像和名称
        String avatar = mFavoriteItem.getUrl().contains("zhuanlan") ? ".Post-Author .Avatar" : ".ContentItem-meta .Avatar";
        String username = mFavoriteItem.getUrl().contains("zhuanlan") ? ".Post-Author .AuthorInfo-name" : ".ContentItem-meta .AuthorInfo-name";
        showUserInfo(document, username, avatar);

        // 富文本内容
        String clsName = mFavoriteItem.getUrl().contains("zhuanlan") ? ".Post-RichText" : ".RichContent-inner";
        Element richContent = document.selectFirst(clsName);
        richContent.select("p.ztext-empty-paragraph").remove();

        mTextView.setText(Html.fromHtml(richContent.html(), mImageGetter, mTagHandler));
        saveOfflineContent(richContent.outerHtml());
    }

    private void showHuPuRichContent(String html){
        // https://bbs.hupu.com/25689490.html?share_from=kqapp 会重定向
        Document document = Jsoup.parse(html);
        // 用户头像和名称
        Log.d(TAG, "showHuPuRichContent: " + html);
        if (mFavoriteItem.getUrl().contains("bbs.hupu.com")) {
            showUserInfo(document, ".detail-author .author-name", ".detail-author img");
        }else{
            mUsernameTv.setText("虎扑新闻");
            mAvatarImg.setImageDrawable(CommonUtil.getAppIcon(this, SourceApp.getAppPackageName(mFavoriteItem.getSource())));
            mUserWrapper.setVisibility(View.VISIBLE);
        }
        // 富文本内容
        String clsName = mFavoriteItem.getUrl().contains("bbs.hupu.com") ? ".article-content" : ".artical-content";
        Element article = document.selectFirst(clsName);

        mTextView.setText(Html.fromHtml(article.outerHtml(), mImageGetter, mTagHandler));
        saveOfflineContent(article.outerHtml());
    }

    private void showQDailyRichContent(String html){
        // http://m.qdaily.com/mobile/articles/61586.html?share_from=app
        Document document = Jsoup.parse(html);
        Element article = document.selectFirst(".article-detail-bd");
        document.select("style").remove();

        // 用户头像和名称
        showUserInfo(document, ".author span.name", ".author .avatar img");

        // 把图片的data-src替换为src
        mTextView.setText(Html.fromHtml(article.html().replace("data-src", "src"), mImageGetter, mTagHandler));
        saveOfflineContent(article.outerHtml());
    }

    private void showDouBanRichContent(String html){
        Document document = Jsoup.parse(html);

        document.select("a.view-large").remove();   // 删除“查看原图”小链接
        document.select(".text-more").remove();     // 删除“更多图片”文本
        if(mFavoriteItem.getUrl().contains("note")){
            // 日记
            // https://www.douban.com/note/708214858/?dt_dapp=1&dt_platform=other
            showUserInfo(document, ".note-author", ".note_author_avatar");

            Element article = document.selectFirst("#link-report");
            mTextView.setText(Html.fromHtml(article.outerHtml(), mImageGetter, mTagHandler));
            saveOfflineContent(article.outerHtml());
        }else if(mFavoriteItem.getUrl().contains("status")){
            // 广播
            // https://www.douban.com/people/1155157/status/2384892255?dt_dapp=1&dt_platform=other
            showUserInfo(document, ".hd .lnk-people", ".hd .usr-pic img");

            Element topic = document.selectFirst(".topic-say");
            if(topic != null)
                topic.html("#话题：" + topic.html());
            Element article = document.selectFirst(".status-saying");
            mTextView.setText(Html.fromHtml(article.outerHtml(), mImageGetter, mTagHandler));
            saveOfflineContent(article.outerHtml());
        }else {
            // 其他，电影，书籍之类的分享
            // https://www.douban.com/music/subject/30467939/
            Element article = document.selectFirst(".subjectwrap .subject");
            Element summary = document.selectFirst("#link-report");
            if(summary != null){
                summary.select("link").remove();
                summary.select("script").remove();
                summary.select("style").remove();
                summary.appendTo(article);
            }
            mTextView.setText(Html.fromHtml(article.outerHtml(), mImageGetter, mTagHandler));
            saveOfflineContent(article.outerHtml());
        }
    }

    private void showWeiBoRichContent(String html){
        // 微博比较特殊
        int idx1 = html.indexOf("render_data");
        int idx2 = html.lastIndexOf("[0] || {}");
        String renderData = html.substring(idx1 + 14, idx2);
        try {
            JSONArray jsonArray = new JSONArray(renderData);
            // 标题
            String text = jsonArray.getJSONObject(0).getJSONObject("status").getString("text");
            JSONArray pics = jsonArray.getJSONObject(0).getJSONObject("status").getJSONArray("pics");
            StringBuilder picStr = new StringBuilder();
            for(int i = 0; i < pics.length(); i++){
                picStr.append(formImgTag(pics.getJSONObject(i).getJSONObject("large").getString("url")));
            }
            text += picStr.toString();

            // 用户
            String username = jsonArray.getJSONObject(0).getJSONObject("status").getJSONObject("user").getString("screen_name");
            String avatarUrl = jsonArray.getJSONObject(0).getJSONObject("status").getJSONObject("user").getString("profile_image_url");
            showUserInfo(username, avatarUrl);

            // 去除无关要素
            Document doc = Jsoup.parse(text);
            doc.select(".url-icon").remove();
            doc.select(".surl-text").remove();
            mTextView.setText(Html.fromHtml(doc.outerHtml(), mImageGetter, mTagHandler));
            saveOfflineContent(doc.outerHtml());
            Log.d(TAG, "showWeiBoRichContent: text: [" + text + "]");
        } catch (JSONException e) {
            Log.d(TAG, "showWeiBoRichContent: null json array");

            mTextView.setText("没有抓取到内容");
        }
    }

    private String formImgTag(String url){
        return String.format("<img src=\"%s\" />", url);
    }

    private void showWeChatRichContent(String html) {
        Document document = Jsoup.parse(html);
        Element article = document.selectFirst(".rich_media_content");

        // 微信的图片资源是data-src，替换为src
        if(article != null){
            mTextView.setText(Html.fromHtml(article.html().replace("data-src", "src"), mImageGetter, mTagHandler));
            saveOfflineContent(article.outerHtml());
        }
        else
            mTextView.setText("抓取内容失败");
    }

    /**
     * 展示用户信息
     * @param document 文档
     * @param usernameSelector 包含用户名的元素的类名
     * @param avatarSelector 包含头像的img元素的类名
     */
    private void showUserInfo(Document document, String usernameSelector, String avatarSelector) {
        Element username = document.selectFirst(usernameSelector);
        Element avatar = document.selectFirst(avatarSelector);
        if(username != null && avatar != null){
            Log.d(TAG, "showUserInfo: username: " + username.outerHtml());
            Log.d(TAG, "showUserInfo: avatar: " + avatar.outerHtml());

            mUsernameTv.setText(username.text());
            HttpSingleTon.getInstance(this).addImageRequest(avatar.attr("src"), new Response.Listener<Bitmap>() {
                @Override
                public void onResponse(Bitmap response) {
                    mAvatarImg.setImageBitmap(response);
                }
            }, 0, 0);

            mUserWrapper.setVisibility(View.VISIBLE);
        }
    }

    /**
     * 展示用户信息
     * @param username 用户名
     * @param avatarUrl 头像地址
     */
    private void showUserInfo(String username, String avatarUrl){
        mUsernameTv.setText(username);
        HttpSingleTon.getInstance(this).addImageRequest(avatarUrl, new Response.Listener<Bitmap>() {
            @Override
            public void onResponse(Bitmap response) {
                mAvatarImg.setImageBitmap(response);
            }
        }, 0, 0);

        mUserWrapper.setVisibility(View.VISIBLE);
    }

    private class ImageTask extends AsyncTask<Object, Void, Bitmap>{

        private LevelListDrawable mDrawable;

        @Override
        protected Bitmap doInBackground(Object... objects) {
            String source = (String)objects[0];
            mDrawable = (LevelListDrawable) objects[1];
            try {
                Log.d(TAG, "doInBackground: " + source);
                InputStream inputStream = new URL(source).openStream();

                return BitmapFactory.decodeStream(inputStream);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            if(bitmap != null){
                int width = mTextView.getWidth() - mTextView.getPaddingLeft() - mTextView.getPaddingRight();
                float factor = (float)width / bitmap.getWidth();

//                if(bitmap.getWidth() < bitmap.getHeight()){
//                    // 宽小于高，居中显示
//                    CenterBitmapDrawable centerBitmapDrawable = new CenterBitmapDrawable(bitmap);
//                    mDrawable.addLevel(1, 1, centerBitmapDrawable);
//                    Log.d(TAG, "onPostExecute: " + width + " " + bitmap.getWidth());
//                    int left = Math.abs(width - bitmap.getWidth()) / 2;
//                    mDrawable.setBounds(left, 0, bitmap.getWidth(), bitmap.getHeight());
//                    mDrawable.setLevel(1);
//                }else{
                    // 宽大于高，宽占满


                BitmapDrawable bitmapDrawable = new BitmapDrawable(bitmap);
                mDrawable.addLevel(1, 1, bitmapDrawable);
                // 图片太小，不调整大小
                if(factor <= 2f)
                    bitmap = Bitmap.createScaledBitmap(bitmap, width, (int)(bitmap.getHeight() * factor), true);
                mDrawable.setBounds(0, 0, bitmap.getWidth(), bitmap.getHeight());
                mDrawable.setLevel(1);
//                }

                CharSequence t = mTextView.getText();
                mTextView.setText(t);
            }
        }
    }

    private class CenterBitmapDrawable extends BitmapDrawable{

        // 居中绘制图片
        private CenterBitmapDrawable(Bitmap bitmap){
            super(bitmap);
        }
        @Override
        public void draw(Canvas canvas) {
            Bitmap bitmap = getBitmap();
            if(bitmap != null){
                int leftOffset = (canvas.getClipBounds().width() - bitmap.getWidth()) / 2;
                Paint paint = getPaint();
                paint.setColor(Color.BLUE);
                canvas.drawRect(0, 0, canvas.getClipBounds().width(), canvas.getClipBounds().height(), paint);
                canvas.drawBitmap(bitmap, leftOffset, 0, getPaint());
            }
        }
    }

    /**
     * 保存内容，用于没有网络时显示
     * @param content 内容
     */
    private void saveOfflineContent(String content){
        if(mFavoriteItem.getContent() == null){
            // 保存网页内容
            mFavoriteItem.setContent(content);
            FavoriteItemLib.get(RichContentActivity.this).updateFavoriteItem(mFavoriteItem);
        }
    }

    private void setText(@IdRes int tvId, String text){
        ((TextView)findViewById(tvId)).setText(text);
    }
}