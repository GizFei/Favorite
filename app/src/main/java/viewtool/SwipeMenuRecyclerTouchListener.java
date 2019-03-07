package viewtool;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.content.Context;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Vibrator;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.animation.DecelerateInterpolator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SwipeMenuRecyclerTouchListener implements RecyclerView.OnItemTouchListener {
    // 从左向滑打开菜单

    private static final String TAG = "RecyclerTouchListener";
    private final Handler mHandler = new Handler();

    private Activity mActivity;             // 对应的活动
    private RecyclerView mRecyclerView;     // 对应的列表

    private int mTouchSlop;     // 不是滑动手势的最大距离
    private int mMinFlingVel;   // 最小的滑动速度
    private int mMaxFlingVel;   // 最大的滑动速度
    private int ANIMATION_STANDARD = 300;   // 标准动画时长
    private int ANIMATION_CLOSE = 150;      // 关闭动画时长

    List<Integer> mUnSwipeableRows;         // 不可滑动的行
    // 背景BG为菜单，FG为内容视图
    private int mBgWidth = 1;   // 防止出现背景长为0的情况，1表示没有背景

    // 滑动过程监视量
    private float mTouchedX;
    private float mTouchedY;
    private boolean mIsFgSwiping;
    private int mSwipingSlop;
    private VelocityTracker mVelocityTracker;
    // 当前触碰的视图
    private int mTouchedPosition;   // 触碰视图在列表中的位置
    private View mTouchedView;      // 触碰视图
    // 菜单显示相关量
    private boolean mBgVisible;         // 是否有背景可见
    private int mBgVisiblePosition;     // 可见背景的位置
    private View mBgVisibleView;        // 可见背景视图
    private boolean mIsRecyclerViewScrolling;   // 列表是否正在滑动
    // 前景与背景
    private int mFgViewID;      // 前景（内容）视图ID
    private View mFgView;       // 前景视图
    private int mBgViewID;      // 背景（菜单）视图ID
    private View mBgView;       // 背景视图
    // 事件监听器
    private OnRowClickListener mRowClickListener;
    private OnRowLongClickListener mRowLongClickListener;
    private OnSwipeOptionsClickListener mBgClickListener;
    // 控制量
    private boolean mClickable = false;     // 是否可点击
    private boolean mLongClickable = false; // 是否可长按
    private boolean mLongClickPerformed = false;    // 是否处理长按了
    private boolean mSwipable = false;      // 是否可滑动
    private int LONG_CLICK_DELAY = 800;
    private boolean mLongClickVibrate;      // 是否长按振动
    private boolean mPaused;                // 是否暂停监听手势
    private boolean mFgPartialViewClicked;  // 背景菜单打开时，触碰点是否在前景部分
    // 视图们
    private List<Integer> mOptionViews;     // 侧滑菜单中的选项视图ID
    // 长按事件
    private Runnable mLongPressed;

    private OnSwipeListenerPublic mSwipeListenerPublic;

    public SwipeMenuRecyclerTouchListener(Activity activity, RecyclerView recyclerView){
        mActivity = activity;
        mRecyclerView = recyclerView;
        // 获取列表配置
        ViewConfiguration vc = ViewConfiguration.get(recyclerView.getContext());
        mTouchSlop = vc.getScaledTouchSlop();
        mMinFlingVel = vc.getScaledMinimumFlingVelocity() * 16;
        mMaxFlingVel = vc.getScaledMaximumFlingVelocity();
        // 初始化
        mBgVisible = false;
        mBgVisiblePosition = RecyclerView.NO_POSITION;
        mBgVisibleView = null;
        mIsRecyclerViewScrolling = false;
        mOptionViews = new ArrayList<>();
        mUnSwipeableRows = new ArrayList<>();
        // 列表滑动监听事件
        mRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                // 列表滑动时不监听手势
                setEnabled(newState != RecyclerView.SCROLL_STATE_DRAGGING);
                // 非静止则为滑动中
                mIsRecyclerViewScrolling = newState != RecyclerView.SCROLL_STATE_IDLE;
            }

            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
//                super.onScrolled(recyclerView, dx, dy);
            }
        });
        mLongPressed = new Runnable() {
            @Override
            public void run() {
                if(!mLongClickable){
                    return;
                }
                mLongClickPerformed = true;
                if(!mBgVisible                      // 没有可见背景
                        && mTouchedPosition >= 0    // 有触碰视图
                        && !mIsRecyclerViewScrolling){
                    if(mLongClickVibrate){
                        // 振动效果
                        Vibrator vibrator = (Vibrator)mActivity.getSystemService(Context.VIBRATOR_SERVICE);
                        if(vibrator != null)
                            vibrator.vibrate(100);
                    }
                    mRowLongClickListener.onRowLongClicked(mTouchedPosition);
                }
            }
        };
    }

    @Override
    public boolean onInterceptTouchEvent(@NonNull RecyclerView recyclerView, @NonNull MotionEvent motionEvent) {
        return handleTouchEvent(motionEvent);
    }

    @Override
    public void onTouchEvent(@NonNull RecyclerView recyclerView, @NonNull MotionEvent motionEvent) {
        handleTouchEvent(motionEvent);
    }

    @Override
    public void onRequestDisallowInterceptTouchEvent(boolean b) {

    }

    public SwipeMenuRecyclerTouchListener setClickable(OnRowClickListener listener){
        mClickable = true;
        mRowClickListener = listener;
        return this;
    }

    public SwipeMenuRecyclerTouchListener setClickable(boolean clickable){
        mClickable = clickable;
        return this;
    }

    public SwipeMenuRecyclerTouchListener setLongClickable(boolean vibrate, OnRowLongClickListener listener){
        mLongClickable = true;
        mRowLongClickListener = listener;
        mLongClickVibrate = vibrate;
        return this;
    }

    public SwipeMenuRecyclerTouchListener setLongClickable(boolean longClickable){
        mLongClickable = longClickable;
        return this;
    }

    public SwipeMenuRecyclerTouchListener setSwipeable(int foregroundID, int backgroundID,
                                                       OnSwipeOptionsClickListener listener){
        this.mSwipable = true;
        if(mFgViewID != 0 && mFgViewID != foregroundID)
            throw new IllegalArgumentException("ForegroundID does not match previously set ID");
        mFgViewID = foregroundID;
        mBgViewID = backgroundID;
        this.mBgClickListener = listener;

        return this;
    }

    public SwipeMenuRecyclerTouchListener setSwipeable(boolean swipeable){
        this.mSwipable = swipeable;
        if(!swipeable)
            invalidateSwipeOptions();

        return this;
    }

    public SwipeMenuRecyclerTouchListener setSwipeOptionViews(Integer... viewIDs){
        mOptionViews = new ArrayList<>(Arrays.asList(viewIDs));
        return this;
    }

    public SwipeMenuRecyclerTouchListener setUnSwipeableRows(Integer... rows) {
        mUnSwipeableRows = Arrays.asList(rows);
        return this;
    }

    private boolean handleTouchEvent(MotionEvent event){
        if(mSwipable && mBgWidth < 2){
            // 未打开时测量菜单宽度
            if(mActivity.findViewById(mBgViewID) != null){
                mBgWidth = mActivity.findViewById(mBgViewID).getWidth();
            }
        }

        switch (event.getActionMasked()){
            case MotionEvent.ACTION_DOWN:
            {
                // 按下
                if(mPaused)
                    break;
                Rect rect = new Rect();
                int childCount = mRecyclerView.getChildCount();

                int[] recyclerViewCoord = new int[2];     // 列表的绝对坐标
                mRecyclerView.getLocationOnScreen(recyclerViewCoord);
                // 点击位置相对于列表视图左上角的坐标
                int x = (int)event.getRawX() - recyclerViewCoord[0];
                int y = (int)event.getRawY() - recyclerViewCoord[1];

                View child;
                for(int i = 0; i < childCount; i++){
                    child = mRecyclerView.getChildAt(i);
                    child.getHitRect(rect);
                    if(rect.contains(x, y)){
                        mTouchedView = child;
                        break;
                    }
                }
                // 找到触碰的视图
                if(mTouchedView != null){
                    // 记录按下的位置和视图在列表中的位置
                    mTouchedX = event.getRawX();
                    mTouchedY = event.getRawY();
                    mTouchedPosition = mRecyclerView.getChildAdapterPosition(mTouchedView);

                    if(mLongClickable){
                        mLongClickPerformed = false;
                        // 800毫秒后启动事件
                        mHandler.postDelayed(mLongPressed, LONG_CLICK_DELAY);
                    }
                    if(mSwipable){
                        // android.view.VelocityTracker主要用跟踪触摸屏事件（flinging事件和其他gestures手势事件）
                        // 的速率。
                        // 用addMovement(MotionEvent)函数将Motion event加入到VelocityTracker类实例中.
                        // 你可以使用getXVelocity() 或getXVelocity()获得横向和竖向的速率到速率时，
                        // 但是使用它们之前请先调用computeCurrentVelocity(int)来初始化速率的单位 。
                        mVelocityTracker = VelocityTracker.obtain();
                        mVelocityTracker.addMovement(event);
                        // 获取视图
                        mFgView = mTouchedView.findViewById(mFgViewID);
                        mBgView = mTouchedView.findViewById(mBgViewID);
                        mBgView.setMinimumHeight(mFgView.getHeight());
                        if(mBgVisible && mFgView != null){
                            // 有可见背景，并且找到前景，则取消长按事件
                            mHandler.removeCallbacks(mLongPressed);
                            x = (int)event.getRawX();
                            y = (int)event.getRawY();
                            mFgView.getGlobalVisibleRect(rect);
                            mFgPartialViewClicked = rect.contains(x, y);
                        }else{
                            mFgPartialViewClicked = false;
                        }
                    }
                }
                mRecyclerView.getHitRect(rect);
                if(mSwipable && mBgVisible && mTouchedPosition != mBgVisiblePosition){
                    // 触碰可见背景外的某一项
                    mHandler.removeCallbacks(mLongPressed);
                    closeVisibleBG();
                }
                break;
            }
            case MotionEvent.ACTION_CANCEL:
            {
                // 就是触摸某个控件，但是又不是在这个控件的区域上抬起（移动到别的地方了）
                mHandler.removeCallbacks(mLongPressed);
                if(mLongClickPerformed)
                    break;
                if(mVelocityTracker == null)
                    break;
                if(mSwipable){
                    if(mTouchedView != null && mIsFgSwiping){
                        animateFG(mTouchedView, AnimationType.CLOSE, ANIMATION_STANDARD);
                    }
                    mVelocityTracker.recycle();
                    mVelocityTracker = null;
                    mIsFgSwiping = false;
                    mBgView = null;
                }
                // 清空状态
                mTouchedX = 0;
                mTouchedY = 0;
                mTouchedView = null;
                mTouchedPosition = RecyclerView.NO_POSITION;
                break;
            }
            case MotionEvent.ACTION_UP:
            {
                // 手指抬起
                mHandler.removeCallbacks(mLongPressed);
                if(mLongClickPerformed)
                    break;
                if(mVelocityTracker == null && mSwipable)
                    break;
                if(mTouchedPosition < 0)
                    break;
                // 相关检测量
                boolean swipedLeft = false;      // 往左滑
                boolean swipedRight = false;     // 往右滑
                boolean swipedLeftProper = false;       // 滑动距离大于背景一半时，确实往左滑
                boolean swipedRightProper = false;      // 滑动距离大于背景一半时，确实往右滑
                float finalDelta = event.getRawX() - mTouchedX;     // 水平滑动量
                if(mIsFgSwiping){
                    // 前景正在滑动
                    swipedLeft = finalDelta < 0;
                    swipedRight = finalDelta > 0;
                }
                // 以下确定真实滑动情况
                if(Math.abs(finalDelta) > mBgWidth / 2 && mIsFgSwiping){
                    // 滑动距离超过背景一半，这表示菜单应该显示或隐藏
                    swipedLeftProper = finalDelta < 0;
                    swipedRightProper = finalDelta > 0;
                }else if(mSwipable){
                    // 从滑动速度看
                    mVelocityTracker.addMovement(event);    // 加入移动事件
                    mVelocityTracker.computeCurrentVelocity(1000);  // //设置units的值为1000，意思为一秒时间内运动了多少个像素
                    float velocityX = mVelocityTracker.getXVelocity();
                    float absVelocityX = Math.abs(velocityX);
                    float absVelocityY = Math.abs(mVelocityTracker.getYVelocity());
                    if(absVelocityX >= mMinFlingVel && absVelocityX <= mMaxFlingVel
                            && absVelocityY < absVelocityX && mIsFgSwiping){
                        // 在范围内，且水平速度大于垂直方向，将滑动看成水平方向
                        swipedLeftProper = (velocityX < 0) == (finalDelta < 0);
                        swipedRightProper = (velocityX > 0) == (finalDelta > 0);
                    }
                }

                if(mSwipable && !swipedLeft && swipedRightProper
                        && mTouchedPosition != RecyclerView.NO_POSITION
                        && !mUnSwipeableRows.contains(mTouchedPosition)){
                    //  !bgVisible
                    // 向右滑，展开菜单
                    animateFG(mTouchedView, AnimationType.OPEN, ANIMATION_STANDARD);
                    // 更新状态
                    mBgVisible = true;
                    mBgVisibleView = mTouchedView;
                    mBgVisiblePosition = mTouchedPosition;
                }else if(mSwipable && !swipedRight && swipedLeftProper
                        && mTouchedPosition != RecyclerView.NO_POSITION
                        && !mUnSwipeableRows.contains(mTouchedPosition)
                        && mBgVisible){
                    // 向左滑，关闭菜单
                    animateFG(mTouchedView, AnimationType.CLOSE, ANIMATION_STANDARD);
                    // 清空状态
                    clearBgVisibleState();
                }else if(mSwipable && swipedRight && !mBgVisible){
                    // 向右滑动距离不够，取消打开菜单
                    animateFG(mTouchedView, AnimationType.CLOSE, ANIMATION_STANDARD);
                    clearBgVisibleState();
                }else if(mSwipable && swipedLeft && mBgVisible){
                    // 向左滑动距离不够，取消关闭菜单
                    animateFG(mTouchedView, AnimationType.OPEN, ANIMATION_STANDARD);
                    mBgVisible = true;
                    mBgVisibleView = mTouchedView;
                    mBgVisiblePosition = mTouchedPosition;
                }else if(mSwipable && swipedRight && mBgVisible){
                    // 菜单已打开时向右滑
                    animateFG(mTouchedView, AnimationType.OPEN, ANIMATION_STANDARD);
                    mBgVisible = true;
                    mBgVisibleView = mTouchedView;
                    mBgVisiblePosition = mTouchedPosition;
                }else if(!swipedLeft && !swipedRight){
                    // 没有滑动，只是点击
                    if(mSwipable && mFgPartialViewClicked){
                        // 菜单打开时，点击前景，则关闭菜单
                        animateFG(mTouchedView, AnimationType.CLOSE, ANIMATION_STANDARD);
                        clearBgVisibleState();
                    }else if(mClickable && !mBgVisible && mTouchedPosition >= 0
                            && !mIsRecyclerViewScrolling){
                        // 背景菜单未打开，处理列表项点击事件
                        mRowClickListener.onRowClicked(mTouchedPosition);
                    }else if(mSwipable && mBgVisible && !mFgPartialViewClicked){
                        // 点击背景的某项菜单
                        final int optionPosition = mTouchedPosition;
                        final int optionID = getOptionsViewID(event);
                        if(optionID >= 0 && mTouchedPosition >= 0){
                            closeVisibleBG(new OnSwipeListener() {
                                @Override
                                public void onSwipeOptionsClosed() {
                                    mBgClickListener.onSwipeOptionsClicked(optionID, optionPosition);
                                    if(mSwipeListenerPublic != null)
                                        mSwipeListenerPublic.onSwipeOptionsClose();
                                }
                                @Override
                                public void onSwipeOptionsOpened() {
                                    if(mSwipeListenerPublic != null)
                                        mSwipeListenerPublic.onSwipeOptionsOpened();
                                }
                            });
                        }
                    }
                }

                if(mSwipable){
                    mVelocityTracker.recycle();
                    mVelocityTracker = null;
                }
                // 清空状态
                mTouchedX = 0;
                mTouchedY = 0;
                mTouchedView = null;
                mTouchedPosition = RecyclerView.NO_POSITION;
                mIsFgSwiping = false;
                mBgView = null;
                break;
            }
            case MotionEvent.ACTION_MOVE:
            {
                // 移动手势
                if(mLongClickPerformed)
                    break;
                if(mVelocityTracker == null || mPaused || !mSwipable)
                    break;
                mVelocityTracker.addMovement(event);
                // 移动距离
                float deltaX = event.getRawX() - mTouchedX;
                float deltaY = event.getRawY() - mTouchedY;

                if(!mIsFgSwiping && Math.abs(deltaX) > mTouchSlop
                        && Math.abs(deltaX) / 2 > Math.abs(deltaY)){
                    // 水平距离大于垂直距离的两倍，则视为滑动
                    mHandler.removeCallbacks(mLongPressed);
                    mIsFgSwiping = true;
                    mSwipingSlop = (deltaX > 0 ? mTouchSlop : -mTouchSlop);     // 滑动阈值
                }

                if(mSwipable && mIsFgSwiping && !mUnSwipeableRows.contains(mTouchedPosition)){
                    if(mBgView == null){
                        mBgView = mTouchedView.findViewById(mBgViewID);
                        mBgView.setVisibility(View.VISIBLE);
                    }
                    Log.d(TAG, "handleTouchEvent: " + deltaX);
                    Log.d(TAG, "handleTouchEvent: touch slop: " + mSwipingSlop);
                    if(deltaX > mTouchSlop && !mBgVisible){
                        // 没有完全滑出
                        float translateAmount = deltaX - mTouchSlop;
                        float alphaAmount = Math.abs(translateAmount) / mBgWidth;
                        mFgView.setTranslationX(Math.abs(translateAmount) > mBgWidth ? mBgWidth : translateAmount);
                        mBgView.setAlpha(alphaAmount > 1f ? 1f : alphaAmount);
                        if(mFgView.getTranslationX() < 0)
                            mFgView.setTranslationX(0);
                    }else if(deltaX < 0 && mBgVisible){
                        float translateAmount = deltaX - mSwipingSlop + mBgWidth;
                        float alphaAmount = Math.abs(translateAmount) / mBgWidth;
                        mFgView.setTranslationX(translateAmount < 0 ? 0 : translateAmount);
                        mBgView.setAlpha(alphaAmount < 0 ? 0 : alphaAmount);
                        if(mFgView.getTranslationX() < 0)
                            mFgView.setTranslationX(0);
                    }
                    return true;
                }else if(mSwipable && mIsFgSwiping && mUnSwipeableRows.contains(mTouchedPosition)){
                    // 轻轻滑动一小段距离，示意这行不能滑动
                    if (deltaX > mTouchSlop && !mBgVisible) {
                        float translateAmount = deltaX - mSwipingSlop;
                        if (mBgView == null)
                            mBgView = mTouchedView.findViewById(mBgViewID);

                        if (mBgView != null)
                            mBgView.setVisibility(View.GONE);

                        // swipe fg till width of bg. If swiped further, nothing happens (stalls at width of bg)
                        mFgView.setTranslationX(translateAmount / 6);
                        if (mFgView.getTranslationX() < 0) mFgView.setTranslationX(0);
                    }
                    return true;
                }
                break;
            }
        }

        return false;
    }

    // 获得侧滑菜单点击项
    private int getOptionsViewID(MotionEvent motionEvent){
        for(int i = 0; i < mOptionViews.size(); i++){
            if(mTouchedView != null){
                Rect rect = new Rect();
                int x = (int)motionEvent.getRawX();
                int y = (int)motionEvent.getRawY();
                mTouchedView.findViewById(mOptionViews.get(i)).getGlobalVisibleRect(rect);
                if(rect.contains(x, y)){
                    return mOptionViews.get(i);
                }
            }
        }

        return -1;
    }

    // 清空可见背景状态
    private void clearBgVisibleState(){
        mBgVisible = false;
        mBgVisibleView = null;
        mBgVisiblePosition = RecyclerView.NO_POSITION;
    }

    // 关闭可见背景
    private void closeVisibleBG(){
        if(mBgVisibleView == null){
            Log.e(TAG, "No rows found for which background options are visible");
            return;
        }
        View fgView = mBgVisibleView.findViewById(mFgViewID);
        final ObjectAnimator translateAnimator = ObjectAnimator.ofFloat(fgView, View.TRANSLATION_X, 0);
        translateAnimator.setDuration(ANIMATION_CLOSE);
        translateAnimator.start();
        // 清空状态
        clearBgVisibleState();
    }

    private void closeVisibleBG(final OnSwipeListener listener){
        if(mBgVisibleView == null){
            Log.e(TAG, "No rows found for which background options are visible");
            return;
        }
        final ObjectAnimator translateAnimator = ObjectAnimator.ofFloat(mFgView, View.TRANSLATION_X, 0f);
        translateAnimator.setDuration(ANIMATION_CLOSE);
        translateAnimator.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) { }

            @Override
            public void onAnimationEnd(Animator animation) {
                Log.d(TAG, "onAnimationEnd: onSwipeOptionsClose()");
                if(listener != null)
                    listener.onSwipeOptionsClosed();
                translateAnimator.removeAllListeners();
            }

            @Override
            public void onAnimationCancel(Animator animation) { }

            @Override
            public void onAnimationRepeat(Animator animation) { }
        });
        translateAnimator.start();
        // 清空状态
        clearBgVisibleState();
    }

    private void animateFG(View downView, AnimationType animationType, int duration){
        AnimatorSet animatorSet = new AnimatorSet();
        if(animationType == AnimationType.OPEN){
            ObjectAnimator translateAnimator = ObjectAnimator.ofFloat(mFgView, View.TRANSLATION_X,
                    mBgWidth);
            ObjectAnimator alphaAnimator = ObjectAnimator.ofFloat(mBgView, View.ALPHA, 1.0f);
            animatorSet.playTogether(translateAnimator, alphaAnimator);
            animatorSet.setDuration(duration);
            animatorSet.setInterpolator(new DecelerateInterpolator(1.5f));
            if(mSwipeListenerPublic != null)
                mSwipeListenerPublic.onSwipeOptionsOpened();
        }else{
            ObjectAnimator translateAnimator = ObjectAnimator.ofFloat(mFgView, View.TRANSLATION_X,
                    0);
            ObjectAnimator alphaAnimator = ObjectAnimator.ofFloat(mBgView, View.ALPHA, 0.0f);
            animatorSet.playTogether(translateAnimator, alphaAnimator);
            animatorSet.setDuration(duration);
            animatorSet.setInterpolator(new DecelerateInterpolator(1.5f));
        }
        animatorSet.start();
    }

    // 设置是否监听手势
    public void setEnabled(boolean enabled){
        mPaused = !enabled;
    }

    private void invalidateSwipeOptions(){
        // 相当于没有背景，即取消菜单点击事件
        mBgWidth = 1;
    }

    private enum AnimationType{
        OPEN, CLOSE
    }

    // 列表项点击事件
    public interface OnRowClickListener{
        void onRowClicked(int position);
    }

    //列表项长按事件
    public interface OnRowLongClickListener{
        void onRowLongClicked(int position);
    }

    // 侧滑菜单选项点击事件
    public interface OnSwipeOptionsClickListener{
        void onSwipeOptionsClicked(int viewID, int position);
    }

    private interface OnSwipeListener{
        void onSwipeOptionsClosed();
        void onSwipeOptionsOpened();
    }

    public interface OnSwipeListenerPublic{
        void onSwipeOptionsClose();
        void onSwipeOptionsOpened();
    }

    public SwipeMenuRecyclerTouchListener setOnSwipeListener(OnSwipeListenerPublic listener){
        mSwipeListenerPublic = listener;
        return this;
    }
}
