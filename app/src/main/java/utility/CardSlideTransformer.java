package utility;

import android.support.annotation.NonNull;
import android.support.v4.view.ViewPager;
import android.view.View;

public class CardSlideTransformer implements ViewPager.PageTransformer {
    /**
     * ViewPager卡片叠放滑动过渡效果
     */
    @Override
    public void transformPage(@NonNull View view, float v) {
        view.setAlpha(1f);
        if(v <= 0.0f){
            view.setTranslationX(0);
        }else{
            view.setAlpha(1f);
            view.setPivotX(view.getWidth() / 2f);
            view.setPivotY(view.getHeight() / 2f);
            view.setScaleX((float)Math.pow(0.9, v));
            view.setScaleY((float)Math.pow(0.9, v));

            view.setTranslationX(-view.getWidth() * v + (view.getWidth() / 2f) * (float)(1 - Math.pow(0.9, v)) + 30f * v);
        }
    }
}
