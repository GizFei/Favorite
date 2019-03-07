package viewtool;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.ViewConfiguration;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.Scroller;

public class SwipeBackLayout extends FrameLayout {
    private static final String TAG = "SwipeBackLayout";

    public static final int SWIPE_LEFT = 1;
    public static final int SWIPE_RIGHT = 2;

    private Context mContext;
    private Scroller mScroller;
    private int mTouchSlop;

    private int mDirection = SWIPE_LEFT;

    private int mScreenWidth;

    private int mLastDownX = 0;
    private int mLastDownY = 0;
    private boolean isClose = false;

    private boolean interceptAction = true;

    private OnSwipeListener mOnSwipeListener;

    public SwipeBackLayout(@NonNull Context context) {
        this(context, null);
    }

    public SwipeBackLayout(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        mContext = context;

        initView();
    }

    private void initView(){
        // 获得滚动阈值
        mTouchSlop = ViewConfiguration.get(mContext).getScaledTouchSlop();
        mScroller = new Scroller(mContext);

        // 获取屏幕宽高
        WindowManager wm = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics metrics = new DisplayMetrics();
        wm.getDefaultDisplay().getMetrics(metrics);
        mScreenWidth = metrics.widthPixels;

        setClickable(true);
        setBackgroundColor(Color.argb(225, 0, 0, 0));
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        // 拦截事件
        if(!isEnabled())
            return false;
        switch (ev.getAction()){
            case MotionEvent.ACTION_DOWN:
                mLastDownX = (int)ev.getX();
                mLastDownY = (int)ev.getY();
                break;
            case MotionEvent.ACTION_MOVE:
                float eventX = ev.getX();
                float eventY = ev.getY();
                float originXDiff = eventX - mLastDownX;
                float xDiff = Math.abs(eventX - mLastDownX);
                float yDiff = Math.abs(eventY - mLastDownY);
                if(xDiff > mTouchSlop && xDiff > yDiff && originXDiff < 0 && interceptAction){
                    // 视作水平向左滑动
                    return true;
                }
                break;
        }
        // 不拦截，向下传递
        return false;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()){
            case MotionEvent.ACTION_MOVE:
                int mCurrentX = (int)event.getX();
                int mDeltaX = mCurrentX - mLastDownX;
                invalidate();
                if(mDirection == SWIPE_LEFT && mDeltaX < 0){
                    // 左滑
                    scrollTo(-mDeltaX, 0);
                }else if(mDirection == SWIPE_RIGHT && mDeltaX > 0){
                    // 右滑
                    scrollTo(-mDeltaX, 0);
                }
                break;
            case MotionEvent.ACTION_UP:
                mCurrentX = (int)event.getX();
                mDeltaX = mCurrentX - mLastDownX;
                if(mDirection == SWIPE_RIGHT){
                    if(Math.abs(mDeltaX) > mScreenWidth / 3 && mDeltaX > 0){
                        // 滑动距离大于屏幕的一半，滑动至退出
                        startScrollToFinish(getScrollX(), 0, -mScreenWidth - getScrollX(), 0, 1000);
                    }else{
                        startScroll(getScrollX(), 0, -getScrollX(), 0, 1000);
                    }
                }else if(mDirection == SWIPE_LEFT){
                    if(Math.abs(mDeltaX) > mScreenWidth / 3 && mDeltaX < 0){
                        startScrollToFinish(getScrollX(), 0, mScreenWidth - getScrollX(),0, 1000);
                    }else{
                        startScroll(getScrollX(), 0, -getScrollX(), 0, 1000);
                    }
                }
                break;
        }
        return super.onTouchEvent(event);
    }

    private void startScroll(int startX, int startY, int dx, int dy, int duration){
        mScroller.startScroll(startX, startY, dx, dy, duration);    // 传入参数
        invalidate();       // 重绘，调用draw里面的computeScroll方法
    }

    private void startScrollToFinish(int startX, int startY, int dx, int dy, int duration){
        startScroll(startX, startY, dx, dy, duration);
        isClose = true;
        invalidate();
        if(mOnSwipeListener != null)
            mOnSwipeListener.onSwipeStart();
    }

    /**
     * computeScroll在View的draw方法里面是空实现，所以这里需要自己去实现，以下是较为标准的写法
     * 可以看到的是在computeScroll方法里面通过scrollTo方法来实现View的滑动，紧接着调用了postInvalidate重绘
     * 会再次进入此方法，如此循环从而实现滑动的效果
     */
    @Override
    public void computeScroll() {
        if(mScroller.computeScrollOffset()){
            scrollTo(mScroller.getCurrX(), mScroller.getCurrY());
            postInvalidate();
        }else{
            if(isClose && mDirection == SWIPE_LEFT && getScrollX() == mScreenWidth){
                Log.d(TAG, "computeScroll: close");
                if(mOnSwipeListener != null)
                    mOnSwipeListener.onSwipeFinish();
            }else if(isClose && mDirection == SWIPE_RIGHT && getScrollX() == -mScreenWidth){
                Log.d(TAG, "computeScroll: close");
                if(mOnSwipeListener != null)
                    mOnSwipeListener.onSwipeFinish();
            }
        }
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        drawShadow();
        super.dispatchDraw(canvas);
    }

    /**
     * 通过改变画笔的alpha值来改变阴影的深浅
     * RectF为限定画笔绘制的矩形区域，注意这里需要根据滑动值来调整区域，
     * 例如往右边滑动，其阴影左边界要相应向左偏移
     */
    private void drawShadow(){
        int alpha = 0;
        if(mDirection == SWIPE_LEFT){
            alpha = (int)(225 * (1 - (float)getScrollX() / mScreenWidth));
        }else if(mDirection == SWIPE_RIGHT){
            alpha = (int)(225 * (1 + (float)getScrollX() / mScreenWidth));
        }
        setBackgroundColor(Color.argb(alpha, 0, 0, 0));
    }

    public void setDirection(int direction) {
        mDirection = direction;
    }

    public interface OnSwipeListener{
        void onSwipeStart();
        void onSwipeFinish();
    }

    public void setOnSwipeListener(OnSwipeListener onSwipeListener) {
        mOnSwipeListener = onSwipeListener;
    }

    public void setInterceptAction(boolean interceptAction) {
        this.interceptAction = interceptAction;
    }
}
