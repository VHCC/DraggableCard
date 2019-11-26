/**
 * Copyright (C) 2015 ogaclejapan
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.vhcc.arclayoutlibs;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Point;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;

import com.vhcc.arclayoutlibs.drag.DraggableCardItemNew;

import java.util.ArrayList;
import java.util.List;
import java.util.WeakHashMap;

import androidx.core.view.GestureDetectorCompat;
import androidx.customview.widget.ViewDragHelper;

import static android.os.Build.VERSION_CODES.JELLY_BEAN_MR1;

public class ArcLayout extends ViewGroup {

    private static final Utils mLog = new Utils(true);
    private final String TAG = getClass().getSimpleName() + "@" + Integer.toHexString(hashCode());


    private static final float DEFAULT_CHILD_ANGLE = 0f;
    private static final int DEFAULT_CHILD_ORIGIN = ArcOrigin.CENTER;
    private static final int DEFAULT_ORIGIN = ArcOrigin.CENTER;
    private static final int DEFAULT_COLOR = Color.TRANSPARENT;
    private static final int DEFAULT_RADIUS = 144;
    private static final int DEFAULT_AXIS_RADIUS = -1; //default: radius / 2
    private static final boolean DEFAULT_FREE_ANGLE = false;
    private static final boolean DEFAULT_REVERSE_ANGLE = false;

    private final WeakHashMap<View, Float> childAngleHolder = new WeakHashMap<>();

    private List<View> childViewList = new ArrayList<>();

    private Arc arc = Arc.CENTER;
    private ArcDrawable arcDrawable;
    private int axisRadius;
    private Point size = new Point();
    private boolean isFreeAngle = DEFAULT_FREE_ANGLE;
    private boolean isReverseAngle = DEFAULT_REVERSE_ANGLE;

    public ArcLayout(Context context) {
        this(context, null);
    }

    public ArcLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
        if (Utils.ENABLE_GLOBAL_LOG) {
            mLog.d(TAG, " * ArcLayout 2");
        }
    }

    private ViewDragHelper mDragHelper;
    private GestureDetectorCompat moveDetector;
    /**
     * itemView需要移动重心，此为对应的Handler
     */
    private Handler anchorHandler;

    /**
     * 正在拖拽的view
     */
    private DraggableCardItemNew draggingView;

    public ArcLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        if (Utils.ENABLE_GLOBAL_LOG) {
            mLog.d(TAG, " * ArcLayout 3");
        }
        init(context, attrs, defStyleAttr, 0);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public ArcLayout(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        if (Utils.ENABLE_GLOBAL_LOG) {
            mLog.d(TAG, " * ArcLayout 4");
        }
        init(context, attrs, defStyleAttr, defStyleRes);
    }

    // ************* Init *****************
    protected void init(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        if (Utils.ENABLE_GLOBAL_LOG) {
            mLog.d(TAG, " * init");
        }

        setWillNotDraw(false);

        final TypedArray a = context.getTheme().obtainStyledAttributes(
                attrs, R.styleable.arc_ArcLayout, defStyleAttr, defStyleRes);
        int arcOrigin = a.getInt(
                R.styleable.arc_ArcLayout_arc_origin, DEFAULT_ORIGIN);
        int arcColor = a.getColor(
                R.styleable.arc_ArcLayout_arc_color, DEFAULT_COLOR);
        int arcRadius = a.getDimensionPixelSize(
                R.styleable.arc_ArcLayout_arc_radius, DEFAULT_RADIUS);
        int arcAxisRadius = a.getDimensionPixelSize(
                R.styleable.arc_ArcLayout_arc_axisRadius, DEFAULT_AXIS_RADIUS);
        boolean isArcFreeAngle = a.getBoolean(
                R.styleable.arc_ArcLayout_arc_freeAngle, DEFAULT_FREE_ANGLE);
        boolean isArcReverseAngle = a.getBoolean(
                R.styleable.arc_ArcLayout_arc_reverseAngle, DEFAULT_REVERSE_ANGLE);
        a.recycle();

        if (Build.VERSION.SDK_INT >= JELLY_BEAN_MR1) {
            arcOrigin = ArcOrigin.getAbsoluteOrigin(arcOrigin, getLayoutDirection());
        }

        arc = Arc.of(arcOrigin);
        arcDrawable = new ArcDrawable(arc, arcRadius, arcColor);
        axisRadius = arcAxisRadius;
        isFreeAngle = isArcFreeAngle;
        isReverseAngle = isArcReverseAngle;

        if (Utils.ENABLE_GLOBAL_LOG) {
            mLog.d(TAG, "axisRadius= %d, isFreeAngle= %b, isReverseAngle= %b" , axisRadius, isFreeAngle, isReverseAngle);
        }

        mDragHelper = ViewDragHelper.create(this, 10f, new DragHelperCallback());
        moveDetector = new GestureDetectorCompat(context, new MoveDetector());
        // 不能处理长按事件，否则违背最初设计的初衷
        moveDetector.setIsLongpressEnabled(false);

        // 滑动的距离阈值由系统提供
        ViewConfiguration configuration = ViewConfiguration.get(getContext());
        mTouchSlop = configuration.getScaledTouchSlop();

        anchorHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                if (draggingView != null) {
                    // 开始移动重心的动画
                    draggingView.startAnchorAnimation();
                }
            }
        };
    }





    // ************************** Drag ******************************

    /**
     * ACTION_DOWN按下后超过这个时间，就直接touch拦截，不会调用底层view的onClick事件
     */
    private static final int INTERCEPT_TIME_SLOP = 200;

    /**
     * 按下的时候，itemView的重心移动，此为对应线程
     */
    private Thread moveAnchorThread;

    /**
     * 按下的时间
     */
    private long downTime = 0;

    /**
     * 按下时的坐标位置
     */
    private int downX, downY;

    /**
     * 保存最初状态时每个itemView的坐标位置
     */
    private List<Point> originViewPositionList = new ArrayList<>();


    @Override
    protected void onFinishInflate() {
        if (Utils.ENABLE_GLOBAL_LOG) {
            mLog.d(TAG, "onFinishInflate");
        }
        super.onFinishInflate();

//        for (int index = 0; index < 3; index++) {
//            // 渲染结束之后，朝viewGroup中添加子View
//            DraggableCardItemNew itemView = new DraggableCardItemNew(getContext());
//            itemView.setStatus(allCards[index]);
//            itemView.setParentView(this);
//            //  原始位置点，由此初始化，一定与子View的status绑定
//            originViewPositionList.add(new Point());
//            addView(itemView);
//        }
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (Utils.ENABLE_GLOBAL_LOG) {
//            mLog.d(TAG, "dispatchTouchEvent");
        }
        if (ev.getAction() == MotionEvent.ACTION_DOWN) {
            // 手指按下的时候，需要把某些view bringToFront，否则的话，tryCapture将不按预期工作
            downX = (int) ev.getX();
            downY = (int) ev.getY();
            downTime = System.currentTimeMillis();
            bringToFrontWhenTouchDown(downX, downY);
        } else if (ev.getAction() == MotionEvent.ACTION_UP) {
            if (draggingView != null) {
                draggingView.onDragRelease();
            }
            draggingView = null;

            if (null != moveAnchorThread) {
                moveAnchorThread.interrupt();
                moveAnchorThread = null;
            }
        }
        return super.dispatchTouchEvent(ev);
    }

    /**
     * 按下时根据触点的位置，将某个view bring到前台
     */
    private void bringToFrontWhenTouchDown(final int downX, final int downY) {
        if (Utils.ENABLE_GLOBAL_LOG) {
            mLog.d(TAG, " - bringToFrontWhenTouchDown, x= %d, y= %d", downX, downY);
        }
        int statusIndex = getStatusByDownPoint(downX, downY);
        final DraggableCardItemNew itemView = getItemViewByStatus(statusIndex);
        if (indexOfChild(itemView) != getChildCount() - 1) {
            bringChildToFront(itemView);
        }
        if (!itemView.isDraggable()) {
            getParent().requestDisallowInterceptTouchEvent(false);
            return;
        } else {
            getParent().requestDisallowInterceptTouchEvent(true);
        }

        itemView.saveAnchorInfo(downX, downY);
        moveAnchorThread = new Thread() {
            @Override
            public void run() {
                try {
                    sleep(INTERCEPT_TIME_SLOP);
                } catch (InterruptedException e) {
                }

                Message msg = anchorHandler.obtainMessage();
                msg.sendToTarget();
            }
        };
        moveAnchorThread.start();
    }

    private int getStatusByDownPoint(int downX, int downY) {
        if (Utils.ENABLE_GLOBAL_LOG) {
            mLog.d(TAG, " - getStatusByDownPoint, x= %d, y= %d", downX, downY);
        }
        int everyWidth = getMeasuredWidth() / 3;
        if (downX < everyWidth) {
            if (downY < everyWidth * 2) {
                return DraggableCardItemNew.STATUS_LEFT_TOP;
            } else {
                return DraggableCardItemNew.STATUS_LEFT_BOTTOM;
            }
        } else if (downX < everyWidth * 2) {
            if (downY < everyWidth * 2) {
                return DraggableCardItemNew.STATUS_LEFT_TOP;
            } else {
                return DraggableCardItemNew.STATUS_MIDDLE_BOTTOM;
            }
        } else {
            if (downY < everyWidth) {
                return DraggableCardItemNew.STATUS_RIGHT_TOP;
            } else if (downY < everyWidth * 2) {
                return DraggableCardItemNew.STATUS_RIGHT_MIDDLE;
            } else {
                return DraggableCardItemNew.STATUS_RIGHT_BOTTOM;
            }
        }
    }

    /**
     * 根据status获取itemView
     */
    private DraggableCardItemNew getItemViewByStatus(int status) {
        if (Utils.ENABLE_GLOBAL_LOG) {
            mLog.d(TAG, " - getItemViewByStatus= %d, ChildCount= %d", status, getChildCount());
        }
        int num = getChildCount();
        for (int i = 0; i < num; i++) {
            DraggableCardItemNew itemView = (DraggableCardItemNew) getChildAt(i);
            if (Utils.ENABLE_GLOBAL_LOG) {
                mLog.d(TAG, " - itemView.getStatus= %d ", itemView.getStatus());
            }


//            DraggableCardItemNew itemView = (DraggableCardItemNew) childViewList.get(i);
//            if (itemView.getStatus() == status) {
                return itemView;
//            }
        }
        return null;
    }

    /**
     * 这是viewdraghelper拖拽效果的主要逻辑
     */
    private class DragHelperCallback extends ViewDragHelper.Callback {

        private final String TAG = getClass().getSimpleName() + "@" + Integer.toHexString(hashCode());

        @Override
        public void onViewPositionChanged(View changedView, int left, int top, int dx, int dy) {
            if (Utils.ENABLE_GLOBAL_LOG) {
                mLog.d(TAG, "changedView= " + changedView.getClass().getSimpleName()
                        + "@" + changedView.hashCode());

                mLog.d(TAG, " - onViewPositionChanged, left= %d, top= %d, dx= %d, dy= %d",
                left, top, dx, dy);
            }
            // draggingView拖动的时候，如果与其它子view交换位置，其他子view位置改变，也会进入这个回调
            // 所以此处加了一层判断，剔除不关心的回调，以优化性能
            if (changedView == draggingView) {
                DraggableCardItemNew changedItemView = (DraggableCardItemNew) changedView;
//                switchPositionIfNeeded(changedItemView);
            }
        }

        @Override
        public boolean tryCaptureView(View child, int pointerId) {
            if (Utils.ENABLE_GLOBAL_LOG) {
                mLog.d(TAG, " - tryCaptureView, pointerId= %d", pointerId);
            }
            // 按下的时候，缩放到最小的级别
            draggingView = (DraggableCardItemNew) child;
            return draggingView.isDraggable();
        }

        @Override
        public void onViewReleased(View releasedChild, float xvel, float yvel) {
            if (Utils.ENABLE_GLOBAL_LOG) {
                mLog.d(TAG, " - onViewReleased, xvel= %f, yvel= %f",
                        xvel, yvel);
            }
            DraggableCardItemNew itemView = (DraggableCardItemNew) releasedChild;
            itemView.onDragRelease();
        }

        @Override
        public int clampViewPositionHorizontal(View child, int left, int dx) {
            if (Utils.ENABLE_GLOBAL_LOG) {
//                mLog.d(TAG, " - clampViewPositionHorizontal, left= %d, dx= %d",
//                        left, dx);
            }
            DraggableCardItemNew itemView = (DraggableCardItemNew) child;
            return itemView.computeDraggingX(dx);
        }

        @Override
        public int clampViewPositionVertical(View child, int top, int dy) {
            if (Utils.ENABLE_GLOBAL_LOG) {
//                mLog.d(TAG, " - clampViewPositionVertical, top= %f, dy= %f",
//                        top, dy);
            }
            DraggableCardItemNew itemView = (DraggableCardItemNew) child;
            return itemView.computeDraggingY(dy);
        }
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (Utils.ENABLE_GLOBAL_LOG) {
            mLog.d(TAG, " * onInterceptTouchEvent");
        }
        if (downTime > 0 && System.currentTimeMillis() - downTime > INTERCEPT_TIME_SLOP) {
            return true;
        }
        boolean shouldIntercept = mDragHelper.shouldInterceptTouchEvent(ev);
        int action = ev.getActionMasked();
        if (action == MotionEvent.ACTION_DOWN) {
            mDragHelper.processTouchEvent(ev);
        }

        boolean moveFlag = moveDetector.onTouchEvent(ev);
        if (moveFlag) {
            if (null != moveAnchorThread) {
                moveAnchorThread.interrupt();
                moveAnchorThread = null;
            }

            if (null != draggingView && draggingView.isDraggable()) {
                draggingView.startAnchorAnimation();
            }
        }
        return shouldIntercept && moveFlag;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (Utils.ENABLE_GLOBAL_LOG) {
//            mLog.d(TAG, " - onTouchEvent");
        }
        try {
            // 该行代码可能会抛异常，正式发布时请将这行代码加上try catch
            mDragHelper.processTouchEvent(event);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return true;
    }

    /**
     * 判定为滑动的阈值，单位是像素
     */
    private int mTouchSlop = 5;

    class MoveDetector extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float dx, float dy) {
            // 拖动了，touch不往下传递
            if (Utils.ENABLE_GLOBAL_LOG) {
                mLog.d(TAG, "Math.abs(dy) + Math.abs(dx)= %f, mTouchSlop= %b", Math.abs(dy) + Math.abs(dx), mTouchSlop);
            }
            return Math.abs(dy) + Math.abs(dx) > mTouchSlop;
        }
    }

    public Point getOriginViewPos(int status) {
        if (Utils.ENABLE_GLOBAL_LOG) {
            mLog.d(TAG, "getOriginViewPos, status= %d", status);
        }
        return originViewPositionList.get(status);
    }




    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (Utils.ENABLE_GLOBAL_LOG) {
            mLog.d(TAG, "onMeasure: w=%s, h=%s",
                    MeasureSpec.toString(widthMeasureSpec),
                    MeasureSpec.toString(heightMeasureSpec));
        }
        measureChildren(widthMeasureSpec, widthMeasureSpec);
//        int maxWidth = MeasureSpec.getSize(widthMeasureSpec);
//        int width = resolveSizeAndState(maxWidth, widthMeasureSpec, 0);
//        setMeasuredDimension(width, width);
        size.x = Utils.computeMeasureSize(widthMeasureSpec, arcDrawable.getIntrinsicWidth());
        size.y = Utils.computeMeasureSize(heightMeasureSpec, arcDrawable.getIntrinsicHeight());

        setMeasuredDimension(size.x, size.y);

        if (Utils.ENABLE_GLOBAL_LOG) {
            mLog.d(TAG, " - setMeasuredDimension: w=%d, h=%d", size.x, size.y);
        }
    }

    private boolean isChildInit = false;

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        if (Utils.ENABLE_GLOBAL_LOG) {
            mLog.d(TAG, " * onLayout: l=%d, t=%d, r=%d, b=%d", l, t, r, b);
        }

        if (isInEditMode()) {
            return;
        }
        arcDrawable.setBounds(0, 0, r - l, b - t);

        final Point o = arc.computeOrigin(0, 0, size.x, size.y);
        final int radius = (axisRadius == DEFAULT_AXIS_RADIUS)
                ? arcDrawable.getRadius() / 2
                : axisRadius;
        final float perDegrees = arc.computePerDegrees(getChildCountWithoutGone());

        int arcIndex = 0;

        if (Utils.ENABLE_GLOBAL_LOG) {
            mLog.d(TAG, "ChildCount= %d", getChildCount());
        }

        if (!isChildInit) {
            for (int i = 0, size = getChildCount(); i < size; i++) {
                final View child = getChildAt(i);
                childViewList.add(child);
                ((DraggableCardItemNew) child).setParentView(this);
                ((DraggableCardItemNew) child).setStatus(i);
            }
            isChildInit = true;
        }

        for (int i = 0; i < childViewList.size(); i++) {
            final View child = childViewList.get(i);
            if (child.getVisibility() == View.GONE) {
                continue;
            }

            final LayoutParams lp = (LayoutParams) child.getLayoutParams();
            float childAngle;
            if (isFreeAngle) {
                childAngle = arc.startAngle + lp.angle;
            } else if (isReverseAngle) {
                childAngle = arc.computeReverseDegrees(arcIndex++, perDegrees);
            } else {
                childAngle = arc.computeDegrees(arcIndex++, perDegrees);
            }

            final int x = o.x + Arc.x(radius, childAngle);
            final int y = o.y + Arc.y(radius, childAngle);

            if (Utils.ENABLE_GLOBAL_LOG) {
                mLog.d(TAG, "i= %d", i);
            }

            childMeasureBy(child, x, y);
            childLayoutBy(child, x, y);

            if (Utils.ENABLE_GLOBAL_LOG) {
                mLog.d(TAG, " - Child= " + child.getClass().getSimpleName() + "@" + child.hashCode());
            }
//                childAngleHolder.put(child, childAngle);
        }
        firstLayout = false;


    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (Utils.ENABLE_GLOBAL_LOG) {
            mLog.d(TAG, " * onDraw, isInEditMode() " + isInEditMode());
        }
        if (isInEditMode()) {
            return;
        }

        super.onDraw(canvas);
        arcDrawable.draw(canvas);
    }

    public int getArcColor() {
        return arcDrawable.getColor();
    }

    public void setArcColor(int color) {
        arcDrawable.setColor(color);
        invalidate();
    }

    public Arc getArc() {
        return arc;
    }

    public void setArc(Arc arc) {
        this.arc = arc;
        arcDrawable.setArc(arc);
        requestLayout();
    }

    public int getRadius() {
        return arcDrawable.getRadius();
    }

    public void setRadius(int radius) {
        arcDrawable.setRadius(radius);
        requestLayout();
    }

    public int getAxisRadius() {
        return axisRadius;
    }

    public void setAxisRadius(int radius) {
        axisRadius = radius;
        requestLayout();
    }

    public boolean isFreeAngle() {
        return isFreeAngle;
    }

    public void setFreeAngle(boolean b) {
        isFreeAngle = b;
        requestLayout();
    }

    public boolean isReverseAngle() {
        return isReverseAngle;
    }

    public void setReverseAngle(boolean b) {
        isReverseAngle = b;
        requestLayout();
    }

    public Point getOrigin() {
        return arc.computeOrigin(getLeft(), getTop(), getRight(), getBottom());
    }

    public float getChildAngleAt(int index) {
        return getChildAngleAt(getChildAt(index));
    }

    public float getChildAngleAt(View v) {
        return (childAngleHolder.containsKey(v)) ? childAngleHolder.get(v) : 0f;
    }

    public int getChildCountWithoutGone() {
        int childCount = 0;
        for (int i = 0, len = getChildCount(); i < len; i++) {
            if (getChildAt(i).getVisibility() != View.GONE) {
                childCount++;
            }
        }
        return childCount;
    }

    protected void childMeasureBy(View child, int x, int y) {
        if (Utils.ENABLE_GLOBAL_LOG) {
            mLog.d(TAG, " - childMeasureBy: x=%d, y=%d", x, y);
        }

        final LayoutParams lp = (LayoutParams) child.getLayoutParams();
        int origin = lp.origin;
        if (Build.VERSION.SDK_INT >= JELLY_BEAN_MR1) {
            origin = ArcOrigin.getAbsoluteOrigin(origin, getLayoutDirection());
        }

        int widthSize;
        int widthMode;

        switch (lp.width) {
            case LayoutParams.MATCH_PARENT:
                widthSize = Utils.computeWidth(origin, size.x, x);
                widthMode = MeasureSpec.EXACTLY;
                break;
            case LayoutParams.WRAP_CONTENT:
                widthSize = Utils.computeWidth(origin, size.x, x);
                widthMode = MeasureSpec.AT_MOST;
                break;
            default:
                widthSize = lp.width;
                widthMode = MeasureSpec.EXACTLY;
        }

        int heightSize;
        int heightMode;

        switch (lp.height) {
            case LayoutParams.MATCH_PARENT:
                heightSize = Utils.computeHeight(origin, size.y, y);
                heightMode = MeasureSpec.EXACTLY;
                break;
            case LayoutParams.WRAP_CONTENT:
                heightSize = Utils.computeHeight(origin, size.y, y);
                heightMode = MeasureSpec.AT_MOST;
                break;
            default:
                heightSize = lp.height;
                heightMode = MeasureSpec.EXACTLY;
        }

        child.measure(
                MeasureSpec.makeMeasureSpec(widthSize, widthMode),
                MeasureSpec.makeMeasureSpec(heightSize, heightMode)
        );

    }

    private boolean firstLayout = true;

    protected void childLayoutBy(View child, int x, int y) {
        if (Utils.ENABLE_GLOBAL_LOG) {
            mLog.d(TAG, " - childLayoutBy: x=%d, y=%d", x, y);
        }

        final LayoutParams lp = (LayoutParams) child.getLayoutParams();
        int origin = lp.origin;
        if (Build.VERSION.SDK_INT >= JELLY_BEAN_MR1) {
            origin = ArcOrigin.getAbsoluteOrigin(origin, getLayoutDirection());
        }

        final int width = child.getMeasuredWidth();
        final int height = child.getMeasuredHeight();

        int left;
        switch (origin & ArcOrigin.HORIZONTAL_MASK) {
            case ArcOrigin.LEFT:
                left = x;
                break;
            case ArcOrigin.RIGHT:
                left = x - width;
                break;
            default:
                left = x - (width / 2);
        }

        int top;
        switch (origin & ArcOrigin.VERTICAL_MASK) {
            case ArcOrigin.TOP:
                top = y;
                break;
            case ArcOrigin.BOTTOM:
                top = y - height;
                break;
            default:
                top = y - (height / 2);
        }

        child.layout(left, top, left + width, top + height);
        if (firstLayout) {
            if (Utils.ENABLE_GLOBAL_LOG) {
                mLog.d(TAG, "firstLayout= " + firstLayout);
            }
            originViewPositionList.add(new Point(left, top));
            child.requestLayout();
        }

        if (Utils.ENABLE_GLOBAL_LOG) {
            mLog.d(TAG, " --- l=%d, t=%d, r=%d, b=%d", left, top, left + width, top + height);
        }
    }

    @Override
    protected ViewGroup.LayoutParams generateLayoutParams(ViewGroup.LayoutParams p) {
        if (Utils.ENABLE_GLOBAL_LOG) {
            mLog.d(TAG, " * generateLayoutParams 1");
        }
        return new LayoutParams(p);
    }

    @Override
    public ViewGroup.LayoutParams generateLayoutParams(AttributeSet attrs) {
        if (Utils.ENABLE_GLOBAL_LOG) {
            mLog.d(TAG, " * generateLayoutParams 2");
        }
        return new LayoutParams(getContext(), attrs);
    }

    @Override
    protected ViewGroup.LayoutParams generateDefaultLayoutParams() {
        if (Utils.ENABLE_GLOBAL_LOG) {
            mLog.d(TAG, " * generateDefaultLayoutParams");
        }
        return new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
    }

    public static class LayoutParams extends MarginLayoutParams {

        private final String TAG = getClass().getSimpleName() + "@" + Integer.toHexString(hashCode());

        public int origin = DEFAULT_CHILD_ORIGIN;
        public float angle = DEFAULT_CHILD_ANGLE;

        public LayoutParams(Context c, AttributeSet attrs) {
            super(c, attrs);
            TypedArray a = c.getTheme()
                    .obtainStyledAttributes(attrs, R.styleable.arc_ArcLayout_Layout, 0, 0);
            origin = a.getInt(R.styleable.arc_ArcLayout_Layout_arc_origin, DEFAULT_CHILD_ORIGIN);
            angle = a.getFloat(R.styleable.arc_ArcLayout_Layout_arc_angle, DEFAULT_CHILD_ANGLE);
            a.recycle();
        }

        public LayoutParams(int width, int height) {
            super(width, height);
            if (Utils.ENABLE_GLOBAL_LOG) {
                mLog.d(TAG, " * LayoutParams 1 ");
            }
        }

        public LayoutParams(ViewGroup.LayoutParams source) {
            super(source);
            if (Utils.ENABLE_GLOBAL_LOG) {
                mLog.d(TAG, " * LayoutParams 2 ");
            }
        }

        public LayoutParams(MarginLayoutParams source) {
            super(source);
            if (Utils.ENABLE_GLOBAL_LOG) {
                mLog.d(TAG, " * LayoutParams 3 ");
            }
        }

    }
}
