package utility;

import android.app.Dialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomSheetBehavior;
import android.support.design.widget.BottomSheetDialog;
import android.support.design.widget.BottomSheetDialogFragment;
import android.view.View;

public abstract class ExpandedBottomSheetFragment extends BottomSheetDialogFragment {
    /**
     * 初始全展开底页片段
     * 继承该类，实现getLayoutRes方法传入布局
     * R.style.BottomSheetDialog配置如下
     * <style name="BottomSheet" parent="Widget.Design.BottomSheet.Modal">
     *     <item name="android:background">@drawable/bg_bottom_sheet</item>
     * </style>
     * <style name="BottomSheetDialog" parent="Theme.Design.Light.BottomSheetDialog">
     *     <item name="android:windowIsFloating">false</item>
     *     <item name="bottomSheetStyle">@style/BottomSheet</item>
     *     <item name="android:statusBarColor">@color/transparent</item>
     * </style>
     * 如果要实现圆角效果，圆角背景配置如下，并重写getTheme方法
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
     */

    private BottomSheetBehavior mBehavior;

    /**
     * 获取布局资源ID
     * @return 资源ID
     */
    public abstract int getLayoutRes();

    // 如果要设置圆角，重写该方法
//    @Override
//    public int getTheme() {
//        return R.style.BottomSheetDialog;
//    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        BottomSheetDialog dialog = (BottomSheetDialog)super.onCreateDialog(savedInstanceState);
        View v = View.inflate(getContext(), getLayoutRes(), null);

        dialog.setContentView(v);
        mBehavior = BottomSheetBehavior.from((View)v.getParent());
        mBehavior.setSkipCollapsed(true); // 跳过中间折叠状态
        return dialog;
    }

    @Override
    public void onStart() {
        super.onStart();
        // 默认全展开。如果布局的高度小于屏幕高度，则不会全部铺开
        mBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
    }

    /**
     * 获取行为
     * @return BottomSheetBehavior
     */
    public BottomSheetBehavior getBehavior(){
        return mBehavior;
    }

    /**
     * 设置STATE_COLLAPSED状态下底页的高度
     * @param peekHeight 高度（px）
     */
    public void setPeekHeight(int peekHeight){
        mBehavior.setPeekHeight(peekHeight);
    }
}
