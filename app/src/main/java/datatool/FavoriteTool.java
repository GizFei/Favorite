package datatool;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

public class FavoriteTool {
    // todo 【集】App 收藏项简单描述
    // todo 【集】App 富文本加载进度条
    // todo 原文链接
    // todo 分享
    // todo 处理文章中的链接
    // todo 数据导出
    // todo 丰富自定义内容
    // todo 批量删除
    // todo 收藏夹批量导入
    // todo 启动页
    // todo 内容可修改（*应用分享的）

    // 与本应用相关的工具
    private static final String TAG = "FavoriteTool";

    /**
     * 获得应用存储图片的路径，结尾带“/”
     * @return 路径
     */
    private static String getImagePath(){
        return Environment.getExternalStorageDirectory() + "/FavoriteApp/ItemImages/";
    }

    public static String saveItemImage(String uuid, Bitmap bitmap){
        Log.d(TAG, "saveItemImage: " + Environment.getExternalStorageState());
        if(Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)){
            // 有权限
            String imageDir = getImagePath();
            File file = new File(imageDir);
            Log.d(TAG, "saveItemImage: path----" + imageDir);
            if(!file.exists()){
                if(!file.mkdirs()){
                    Log.d(TAG, "saveItemImage: make dir fail");
                    return null;
                }
            }
            File imgFile = new File(imageDir + uuid + ".jpg");
            try {
                FileOutputStream outputStream = new FileOutputStream(imgFile);
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
                outputStream.flush();
                outputStream.close();
                Log.d(TAG, "saveItemImage: path: " + imgFile.getPath());
                Log.d(TAG, "saveItemImage: absolute path: " + imgFile.getAbsolutePath());
                return imgFile.getAbsolutePath();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        Log.d(TAG, "saveItemImage: Failed");
        return null;
    }

    public static Bitmap getItemImage(String imgPath){
        return BitmapFactory.decodeFile(imgPath);
    }

    public static void deleteItemImage(String imgPath){
        if(imgPath == null){
            Log.d(TAG, "deleteItemImage: null image path");
            return;
        }
        File file = new File(imgPath);
        if(file.exists()) {
            if(file.delete()){
                Log.d(TAG, "deleteItemImage: delete Successfully");
            }
        }
    }

    public static void setArchivePreferences(Context context, String archiveSet){
        SharedPreferences preferences = context.getSharedPreferences("Archives", Context.MODE_PRIVATE);
        preferences.edit().putString("archives", archiveSet).apply();
    }
    public static String getArchivePreferences(Context context){
        SharedPreferences preferences = context.getSharedPreferences("Archives", Context.MODE_PRIVATE);
        return preferences.getString("archives", null);
    }

    public static String getClipboardText(Context context){
        SharedPreferences preferences = context.getSharedPreferences("Clipboard", Context.MODE_PRIVATE);
        return preferences.getString("prevText", null);
    }
    public static void setClipboardText(Context context, String text){
        SharedPreferences preferences = context.getSharedPreferences("Clipboard", Context.MODE_PRIVATE);
        preferences.edit().putString("prevText", text).apply();
    }
}
