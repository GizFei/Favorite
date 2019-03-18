package datatool;

import android.util.Log;

import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ParseShareContent {
    private static final String TAG = "ParseShareContent";
    /**
     * 解析分享内容的来源
     * @param shareContent 分享内容
     * @return 来源App
     */
    public static String getSourceApp(String shareContent){
        if(shareContent.contains("zhihu.com")){
            // 来源为知乎
            return SourceApp.APP_ZHIHU;
        }else if(shareContent.contains("hupu.com")){
            // 来源为虎扑
            return SourceApp.APP_HUPU;
        }else if(shareContent.contains("qdaily.com")){
            // 来源为好奇心日报
            return SourceApp.APP_QDAILY;
        }else if(shareContent.contains("douban.com")){
            // 豆瓣
            return SourceApp.APP_DOUBAN;
        }else if(shareContent.contains("weibo.cn")){
            // 微博
            return SourceApp.APP_WEIBO;
        }else if(shareContent.contains("weixin.qq.com")){
            // 微信
            return SourceApp.APP_WECHAT;
        }
        return SourceApp.APP_SELF;
    }

    /**
     * 从分享内容构造收藏项
     * @param shareContent 分享内容
     * @return 收藏项[FavoriteItem]
     */
    public static FavoriteItem formFavoriteItem(String shareContent){
        String source = getSourceApp(shareContent);
//        Log.d(TAG, "formFavoriteItem: source:" + source);

        switch (source){
            case SourceApp.APP_ZHIHU:
                return parseZhiHuContent(shareContent);
            case SourceApp.APP_HUPU:
                return parseHuPuContent(shareContent);
            case SourceApp.APP_QDAILY:
                return parseQDailyContent(shareContent);
            case SourceApp.APP_DOUBAN:
                return parseDouBanContent(shareContent);
            case SourceApp.APP_WEIBO:
                return parseWeiBoContent(shareContent);
            case SourceApp.APP_WECHAT:
                return parseWeChatContent(shareContent);
            default:
                // 未知App来源，视为原创
                return parseSelfContent(shareContent);
        }
    }

    private static FavoriteItem parseSelfContent(String shareContent) {
        // 当作纯文本处理
        FavoriteItem item = new FavoriteItem();
        item.setSource(SourceApp.APP_SELF);
        item.setContent(shareContent);

        return item;
    }

    private static FavoriteItem parseZhiHuContent(String shareContent){
        FavoriteItem favoriteItem = new FavoriteItem();
        favoriteItem.setSource(SourceApp.APP_ZHIHU);

        // 知乎 https://www.zhihu.com/question/268998966/answer/600981522?utm_source=com.miui.notes&utm_medium=social&utm_oi=732317165998329856
        // 收藏“问题回答”
        Pattern pattern = Pattern.compile("【(.*)】(.*)：(?:\\.{3}\\s|…\\s)(.*)（(.*)）");
        Matcher matcher = pattern.matcher(shareContent);
        if(matcher.find()){
            int count = matcher.groupCount();
            if(count == 4){
                favoriteItem.setTitle(matcher.group(1));
                // 网页链接
                favoriteItem.setUrl(matcher.group(3));
                return favoriteItem;
            }
        }
        // 收藏专栏文章 https://zhuanlan.zhihu.com/p/57816934
        Pattern pattern1 = Pattern.compile("(.*)（分享自知乎网）(.*)");
        Matcher matcher1 = pattern1.matcher(shareContent);
        if(matcher1.find()){
            if(matcher1.groupCount() == 2){
                favoriteItem.setTitle(matcher1.group(1));
                // 网页链接
                favoriteItem.setUrl(matcher1.group(2));
                return favoriteItem;
            }
        }

        // 是网址
        if(isUrl(shareContent)){
            favoriteItem.setUrl(shareContent);
            return favoriteItem;
        }
        return null;
    }

    private static FavoriteItem parseHuPuContent(String shareContent){
        // 虎扑
        FavoriteItem favoriteItem = new FavoriteItem();
        favoriteItem.setSource(SourceApp.APP_HUPU);

        // 只有网址
        if(isUrl(shareContent)){
            favoriteItem.setUrl(shareContent);
            return favoriteItem;
        }
        // 分享内容
        Pattern pattern = Pattern.compile("(.*)(http|https)(://.*)");
        Matcher matcher = pattern.matcher(shareContent);

        if(matcher.find()){
            if(matcher.groupCount() == 3){
                favoriteItem.setTitle(matcher.group(1));
                // 网页链接
                favoriteItem.setUrl(matcher.group(2) + matcher.group(3));
                return favoriteItem;
            }
        }

        return null;
    }

    private static FavoriteItem parseQDailyContent(String shareContent){
        // 好奇心日报 https://www.qdaily.com/articles/61586.html?share_from=app
        Pattern pattern = Pattern.compile("(.*)_好奇心日报\\s+(http|https)(://.*)");
        Matcher matcher = pattern.matcher(shareContent);

        if(matcher.find()){
            if(matcher.groupCount() == 3){
                FavoriteItem favoriteItem = new FavoriteItem();
                favoriteItem.setTitle(matcher.group(1));
                favoriteItem.setSource(SourceApp.APP_QDAILY);
                favoriteItem.setDate(new Date());
                // 网页链接
                favoriteItem.setUrl(matcher.group(2) + matcher.group(3));
                return favoriteItem;
            }
        }
        return null;
    }

    private static FavoriteItem parseDouBanContent(String shareContent) {
        FavoriteItem item = new FavoriteItem();
        item.setSource(SourceApp.APP_DOUBAN);
        if(shareContent.contains("豆瓣日记")){
            // 日记
            // [我要做征友界的泥石流 本人性别女，明年30，母胎单身。 很多人说我长得像周冬雨，
            // 但实际上并不像。 至今没能恋爱的原因是：既  | 豆瓣日记 https://www.douban.com/doubanapp/dispatch?uri=/note/700173726/&dt_platform=other&dt_dapp=1]
            Pattern pattern = Pattern.compile("(.*?)\\s+(.*)豆瓣日记\\s*(.*)");
            Matcher matcher = pattern.matcher(shareContent);
            if(matcher.find()){
                if(matcher.groupCount() == 3){
                    item.setTitle("[日记] " + matcher.group(1));
                    item.setUrl(matcher.group(3));
                    return item;
                }
            }
        }else if(shareContent.contains("豆瓣评分")){
            // 分享电影/图片等评分项目
            // [《一吻定情》豆瓣评分:5.3(21947人评分)  https://www.douban.com/doubanapp/dispatch/movie/30263995?dt_platform=other&dt_dapp=1]
            Pattern pattern = Pattern.compile("(《.*》)豆瓣评分(?:.*)\\s+(.*)");
            Matcher matcher = pattern.matcher(shareContent);
            if(matcher.find()){
                if(matcher.groupCount() == 2){
                    String title = getDouBanCatalog(matcher.group(2)) + matcher.group(1);
                    item.setTitle(title);
                    item.setUrl(matcher.group(2));
                    return item;
                }
            }
        }else {
            // 广播
            // [沥青博士 ,  https://www.douban.com/doubanapp/dispatch?uri=/status/2246396150/&dt_platform=other&dt_dapp=1]
            String[] text = shareContent.split(",");
            if(text.length == 2){
                String author = text[0].trim();
                String url = text[1].trim();
                item.setTitle("「" + author + "」的广播");
                item.setUrl(url);
                return item;
            }else
                return null;
        }
        return null;
    }

    private static FavoriteItem parseWeiBoContent(String shareContent){
        // 微博 https://m.weibo.cn/2286908003/4346029334055046
        FavoriteItem item = new FavoriteItem();
        item.setSource(SourceApp.APP_WEIBO);
        item.setUrl(shareContent);

        return item;
    }

    private static FavoriteItem parseWeChatContent(String shareContent) {
        // 微信 https://mp.weixin.qq.com/s/lfyeyvQqQydLZ4IQU6Wf-Q
        FavoriteItem item = new FavoriteItem();
        item.setSource(SourceApp.APP_WECHAT);
        item.setUrl(shareContent);

        return item;
    }

    private static String getDouBanCatalog(String url){
        StringBuilder builder = new StringBuilder();
        builder.append("[");
        if(url.contains("game")){
            builder.append("游戏");
        }else if(url.contains("book")){
            builder.append("书籍");
        }else if(url.contains("movie")){
            builder.append("电影|电视|综艺");
        }else if(url.contains("music")){
            builder.append("音乐");
        }else {
            builder.append("其他");
        }
        builder.append("] ");
        return builder.toString();
    }

    private static boolean isUrl(String text){
        return text.matches("(https?|ftp|file)://[-A-Za-z0-9+&@#/%?=~_|!:,.;]+[-A-Za-z0-9+&@#/%=~_|]");
    }
}
