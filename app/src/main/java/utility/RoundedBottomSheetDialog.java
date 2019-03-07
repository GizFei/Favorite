package utility;

import android.content.Context;
import android.support.annotation.LayoutRes;
import android.support.annotation.StyleRes;
import android.support.design.widget.BottomSheetBehavior;
import android.support.design.widget.BottomSheetDialog;
import android.view.View;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class RoundedBottomSheetDialog extends BottomSheetDialog {

    private BottomSheetBehavior mBehavior;

    /**
     * 圆角底页框
     * 圆角样式设置如下
     * <shape xmlns:android="http://schemas.android.com/apk/res/android"
     *     android:shape="rectangle">
     *     <corners
     *         android:topLeftRadius="16dp"
     *         android:topRightRadius="16dp">
     *     </corners>
     *     <solid android:color="@android:color/white"/>
     *     <padding android:top="8dp"
     *         android:left="8dp"
     *         android:right="8dp" />
     * </shape>
     * <-- 底部弹出控件样式 -->
     * <style name="BottomSheet" parent="Widget.Design.BottomSheet.Modal">
     *     <item name="android:background">@drawable/bg_bottom_sheet</item>
     * </style>
     * <style name="BottomSheetDialog" parent="Theme.Design.Light.BottomSheetDialog">
     *     <item name="android:windowIsFloating">false</item>
     *     <item name="bottomSheetStyle">@style/BottomSheet</item>
     *     <item name="android:statusBarColor">@color/transparent</item>
     * </style>
     * @param context 上下文
     * @param layout 布局资源
     * @param style 圆角样式
     */
    public RoundedBottomSheetDialog(Context context, @LayoutRes int layout, @StyleRes int style){
        super(context, style);
        View v = getLayoutInflater().inflate(layout, null);
        this.setContentView(v);
        mBehavior = BottomSheetBehavior.from((View)v.getParent());
    }

    /**
     * 构造函数2
     * @param context 上下文
     * @param v 内部视图
     * @param style 圆角样式
     */
    public RoundedBottomSheetDialog(Context context, View v, @StyleRes int style){
        super(context, style);
        this.setContentView(v);
        mBehavior = BottomSheetBehavior.from((View)v.getParent());
    }

    /**
     * 获得底页的行为
     * @return BottomSheetBehavior
     */
    public BottomSheetBehavior getBehavior(){
        return mBehavior;
    }

    /**
     * 设置活动项视图的事件
     * @param listener 事件监听器
     * @param viewIDs 活动项ID
     */
    public void setActionViewIDs(final OnActionViewListener listener, Integer... viewIDs){
        List<Integer> actionViewIDs = new ArrayList<>(Arrays.asList(viewIDs));
        for(final Integer viewID : actionViewIDs){
            View view = findViewById(viewID);
            if(view != null) {
                view.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        listener.onClick(viewID);
                    }
                });
            }
        }
    }

    public interface OnActionViewListener{
        void onClick(int viewID);
    }
}
