package datatool;

public class SourceApp {
    // 来源的App
    public static final String APP_ZHIHU = "SourceApp.ZhiHu";      // 知乎
    public static final String APP_HUPU = "SourceApp.HuPu";        // 虎扑
    public static final String APP_QDAILY = "SourceApp.QDaily";    // 好奇心日报
    public static final String APP_DOUBAN = "SourceApp.DouBan";    // 豆瓣
    public static final String APP_WEIBO = "SourceApp.WeiBo";        // 微博
    public static final String APP_WECHAT = "SourceApp.WeChat";      // 微信
    public static final String APP_SELF = "SourceApp.Self";        // 如果没有符合上述App的，则全部视为原创

    /**
     * 把来源转换为文字描述用于显示
     * @param source 来源
     * @return 描述性文字
     */
    public static String getSourceText(String source){
        switch (source){
            case APP_ZHIHU:
                return "知乎";
            case APP_HUPU:
                return "虎扑";
            case APP_SELF:
                return "原创";
            case APP_QDAILY:
                return "好奇心日报";
            case APP_DOUBAN:
                return "豆瓣";
            case APP_WEIBO:
                return "微博";
            case APP_WECHAT:
                return "微信";
        }
        return "原创";
    }

    public static String getAppPackageName(String source){
        switch (source){
            case APP_ZHIHU:
                return "com.zhihu.android";
            case APP_HUPU:
                return "com.hupu.games";
            case APP_SELF:
                return "com.giz.favorite";
            case APP_QDAILY:
                return "com.qdaily.ui";
            case APP_DOUBAN:
                return "com.douban.frodo";
            case APP_WEIBO:
                return "com.sina.weibo";
            case APP_WECHAT:
                return "com.tencent.mm";
        }
        return "com.giz.favorite";
    }
}
