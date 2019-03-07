package datatool;

import java.util.Date;
import java.util.UUID;

import utility.CommonUtil;

public class FavoriteItem {
    // 通用项
    private UUID mUUID;         // 唯一ID
    private String mTitle;      // 标题
    private Date mDate;         // 收藏时间
    private String mSource;     // 来源（哪个App）

    // 特有项
    private String mUrl;        // 网页链接
    private String mContent;    // 原创项的内容
    private String mImagePath;  // 图片路径

    private boolean mIsStarred;  // 是否星标收藏
    private String mArchive;    // 归档的收藏夹

    public FavoriteItem(){
        mUUID = UUID.randomUUID();
        mDate = new Date();
        mIsStarred = false;
    }

    public FavoriteItem(String uuid){
        mUUID = UUID.fromString(uuid);
    }

    public String getTitle() {
        return mTitle;
    }

    public void setTitle(String title) {
        mTitle = title;
    }

    public void setDate(Date date) {
        mDate = date;
    }

    public Date getDate() {
        return mDate;
    }

    public String getSource() {
        return mSource;
    }

    public void setSource(String source) {
        mSource = source;
    }

    public UUID getUUID() {
        return mUUID;
    }

    public String getUrl() {
        return mUrl;
    }

    public void setUrl(String url) {
        mUrl = url;
    }

    public String getContent() {
        return mContent;
    }

    public void setContent(String content) {
        mContent = content;
    }

    public String getImagePath() {
        return mImagePath;
    }

    public void setImagePath(String imagePath) {
        mImagePath = imagePath;
    }

    public boolean isStarred() {
        return mIsStarred;
    }

    public void setStarred(boolean starred) {
        mIsStarred = starred;
    }

    public String getArchive() {
        return mArchive;
    }

    public void setArchive(String archive) {
        mArchive = archive;
    }
}
