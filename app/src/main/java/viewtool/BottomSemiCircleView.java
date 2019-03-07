package viewtool;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Shader;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.support.v7.widget.AppCompatImageView;
import android.util.AttributeSet;

import com.giz.favorite.R;

import utility.CommonUtil;

public class BottomSemiCircleView extends AppCompatImageView {
    private static final String TAG = "BottomSemiCircleView";

    private Context mContext;
    private Paint mPaint;

    public BottomSemiCircleView(Context context) {
        this(context, null);
    }

    public BottomSemiCircleView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public BottomSemiCircleView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mContext = context;
        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.DITHER_FLAG);  // 平滑
    }

    @Override
    protected void onDraw(Canvas canvas) {
//        super.onDraw(canvas);
        Drawable drawable;
        int rectHeight = (int) CommonUtil.dp2px(mContext, 16);  // 底部矩形高度
        if(getBackground() != null && getDrawable() == null){
            // 只设置了背景
            drawable = getBackground();
        }else if(getBackground() == null && getDrawable() != null){
            // 只设置了资源src
            drawable = getDrawable();
        }else if(getBackground() != null && getDrawable() != null){
            // 都设置了，则取资源
            drawable = getDrawable();
        }else{
            super.onDraw(canvas);
            return;
        }

        if(drawable instanceof ColorDrawable){
            // 是颜色
            mPaint.setColor(getResources().getColor(R.color.colorPrimary));
        }else {
            // 是资源
            BitmapShader mBitmapShader = new BitmapShader(drawable2Bitmap(getDrawable()), Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);
            mPaint.setShader(mBitmapShader);
        }
        mPaint.setShadowLayer(5, 0, -8, 0x66000000);    // 阴影层

        if(getHeight() < rectHeight){
            // 小于矩形高度，只画弧
            canvas.drawArc(0, 0, getWidth(), getHeight() * 2, 180, 180,
                    true, mPaint);
        }else {
            int arcHeight = (getHeight() - rectHeight) * 2;
            // 下方的矩形
            canvas.drawRect(0, getHeight() - rectHeight, getWidth(), getHeight(), mPaint);
            // 上方的弧形
            canvas.drawArc(0, 0, getWidth(), arcHeight, 180, 180, true, mPaint);
        }

    }

    /**
     * drawable转换成bitmap
     */
    private Bitmap drawable2Bitmap(Drawable drawable) {
        if (drawable == null) {
            return null;
        }
        Bitmap bitmap = Bitmap.createBitmap(getWidth(), getHeight(), Bitmap.Config.RGB_565);
        Canvas canvas = new Canvas(bitmap);
        //根据传递的scaletype获取matrix对象，设置给bitmap
        Matrix matrix = getImageMatrix();
        if (matrix != null) {
            canvas.concat(matrix);
        }
        drawable.draw(canvas);
        return bitmap;
    }
}
