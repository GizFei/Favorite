package utility;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Environment;
import android.text.format.DateFormat;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.WindowManager;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class CommonUtil {
    /* 常用工具类 */

    /**
     * dp转px
     * @param dp dp值
     * @param context 上下文
     * @return px像素值
     */
    public static float dp2px(Context context, float dp){
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp,
                context.getResources().getDisplayMetrics());
    }

    private static String formDateString(Date date){
        return DateFormat.format("yyyyMMdd", date).toString();
    }

    /**
     * 构建日期描述文本，用于Date型的ViewHolder中的展示
     * @param date 日期
     * @return 日期描述文本
     */
    public static String getDateDescription(Date date){
        Calendar calendar = Calendar.getInstance(Locale.getDefault());
        calendar.setTime(date);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);
        return String.format(Locale.CHINA, "%d月%d日", month + 1, day);
    }
    // 简易，用于Item型的ViewHolder中展示
    public static String getDateBriefDescription(Date date){
        if(isToday(date)){
            // 获取时间差描述
            Date now = new Date();
            Calendar calendar = Calendar.getInstance(Locale.getDefault());
            calendar.setTime(date);
            Calendar nowCal = Calendar.getInstance(Locale.getDefault());
            nowCal.setTime(now);
            int hourDiff = nowCal.get(Calendar.HOUR_OF_DAY) - calendar.get(Calendar.HOUR_OF_DAY);
            if(hourDiff > 0){
                return hourDiff + "小时";
            }
            int minDiff = nowCal.get(Calendar.MINUTE) - calendar.get(Calendar.MINUTE);
            if(minDiff > 0){
                return minDiff + "分钟";
            }
            return "刚刚";
        }else
            return DateFormat.format("MM-dd", date).toString();
    }
    // 详细，用于富文本中展示
    public static String getDateDetailDescription(Date date){
        return DateFormat.format("MM-dd hh:mm", date).toString();
    }
    public static boolean isToday(Date date){
        Calendar calendar = Calendar.getInstance(Locale.CHINA);
        calendar.setTime(new Date());
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        long startTime = calendar.getTimeInMillis();
        long endTime = startTime + 60 * 60 * 24 * 1000;
        return date.getTime() >= startTime && date.getTime() < endTime;
    }
    public static boolean theSameDay(Date date1, Date date2) {
        return formDateString(date1).equals(formDateString(date2));
    }


    public static Bitmap resizeBitmap(Bitmap bitmap, int width, int height){
        float factorW = (float)width / bitmap.getWidth();
        float factorH = (float)height / bitmap.getHeight();

        Matrix matrix = new Matrix();
        matrix.postScale(factorW, factorH);

        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
    }

    /**
     * 获得应用的图标
     * @param context 上下文
     * @param packageName 应用包名
     * @return 图标Drawable
     */
    public static Drawable getAppIcon(Context context, String packageName){
        try {
            if(packageName != null){
                PackageManager packageManager = context.getPackageManager();
                ApplicationInfo info = packageManager.getApplicationInfo(packageName, 0);

                return info.loadIcon(packageManager);
            }
        } catch (PackageManager.NameNotFoundException e) {
            Log.d("CommonUtil", "getAppIcon: Name Not Found");
        }
        return null;
    }

    public static Bitmap drawable2Bitmap(Drawable drawable){
        if(drawable == null)
            return null;

        BitmapDrawable bitmapDrawable = (BitmapDrawable)drawable;
        return bitmapDrawable.getBitmap();
    }

    public static Drawable bitmap2Drawable(Bitmap bitmap){
        if(bitmap == null)
            return null;

        return new BitmapDrawable(bitmap);
    }

    public static void logAllContent(String tag, String msg) {  //信息太长,分段打印
        //因为String的length是字符数量不是字节数量所以为了防止中文字符过多，
        //  把4*1024的MAX字节打印长度改为2001字符数
        int max_str_length = 2001 - tag.length();
        //大于4000时
        while (msg.length() > max_str_length) {
            Log.d(tag, msg.substring(0, max_str_length));
            msg = msg.substring(max_str_length);
        }
        //剩余部分
        Log.d(tag, msg);
    }

    /**
     * 抓取网页源码
     * @param connection 连接
     * @return 源码
     */
    public static String fetchHtml(HttpURLConnection connection) throws Exception{
        InputStream inputStream = connection.getInputStream();
        ByteArrayOutputStream result = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int length;
        while ((length = inputStream.read(buffer)) != -1) {
            result.write(buffer, 0, length);
        }
        result.close();
        return result.toString();
    }

    /**
     * 检查网络是否可用
     * @param context 上下文
     * @return 布尔值
     */
    public static boolean isNetworkAvailable(Context context){
        ConnectivityManager cm = (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if(cm == null)
            return false;
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isConnected();
    }

    public static int getScreenWidth(Context context){
        WindowManager windowManager = (WindowManager)context.getSystemService(Context.WINDOW_SERVICE);
        if(windowManager != null){
            DisplayMetrics metrics = new DisplayMetrics();
            windowManager.getDefaultDisplay().getMetrics(metrics);
            return metrics.widthPixels;
        }
        return 400;
    }
    public static int getScreenHeight(Context context){
        WindowManager windowManager = (WindowManager)context.getSystemService(Context.WINDOW_SERVICE);
        if(windowManager != null){
            DisplayMetrics metrics = new DisplayMetrics();
            windowManager.getDefaultDisplay().getMetrics(metrics);
            return metrics.heightPixels;
        }
        return 600;
    }

}
