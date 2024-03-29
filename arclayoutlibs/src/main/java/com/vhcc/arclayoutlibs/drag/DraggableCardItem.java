package com.vhcc.arclayoutlibs.drag;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.Point;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.facebook.rebound.SimpleSpringListener;
import com.facebook.rebound.Spring;
import com.facebook.rebound.SpringConfig;
import com.facebook.rebound.SpringSystem;
import com.vhcc.arclayoutlibs.ArcLayout;
import com.vhcc.arclayoutlibs.R;
import com.vhcc.arclayoutlibs.Utils;

/**
 * Created by xmuSistone on 2016/5/23.
 */
public class DraggableCardItem extends FrameLayout {

    private static final Utils mLog = new Utils(true);
    private final String TAG = getClass().getSimpleName() + "@" + Integer.toHexString(hashCode());


    public static final int STATUS_LEFT_TOP = 0;
    public static final int STATUS_RIGHT_TOP = 1;
    public static final int STATUS_RIGHT_MIDDLE = 2;
    public static final int STATUS_RIGHT_BOTTOM = 3;
    public static final int STATUS_MIDDLE_BOTTOM = 4;
    public static final int STATUS_LEFT_BOTTOM = 5;

    public static final int SCALE_LEVEL_1 = 1; // 最大状态，缩放比例是100%
    public static final int SCALE_LEVEL_2 = 2; // 中间状态，缩放比例scaleRate
    public static final int SCALE_LEVEL_3 = 3; // 最小状态，缩放比例是smallerRate

    // Scale rate
    private float scaleRate = 0.5f;
    private float smallerRate = scaleRate * 0.8f;

    // View
    private ImageView imageView;
    private View maskView;
    private ArcLayout parentView;

    private int status;

    private Spring springX, springY;
    private ObjectAnimator scaleAnimator;
    private boolean hasSetCurrentSpringValue = false;

    private SpringConfig springConfigCommon = SpringConfig.fromOrigamiTensionAndFriction(140, 7);
    private int moveDstX = Integer.MIN_VALUE, moveDstY = Integer.MIN_VALUE;
    private OnClickListener dialogListener;

    private String imagePath;
    private View addView;



    public DraggableCardItem(Context context) {
        this(context, null);
    }

    public DraggableCardItem(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public DraggableCardItem(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        inflate(context, R.layout.drag_item, this);
        imageView = findViewById(R.id.drag_item_imageview);
        maskView = findViewById(R.id.drag_item_mask_view);
        addView = findViewById(R.id.add_view);

        dialogListener = new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (v.getId() == R.id.pick_image) {
                    // 从相册选择图片
//                    pickImage();
                } else {
                    // 删除
                    imagePath = null;
                    imageView.setImageBitmap(null);
                    addView.setVisibility(View.VISIBLE);
//                    parentView.onDeleteImage(DraggableCardItemNew.this);
                }
            }
        };

        getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                if (!hasSetCurrentSpringValue) {
                    adjustImageView();
                    hasSetCurrentSpringValue = true;
                }
            }
        });

        maskView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isDraggable()) {
//                    pickImage();
                } else {
                    CustDialog dialog = new CustDialog(getContext());
                    dialog.setClickListener(dialogListener);
                    dialog.show();
                }
            }
        });

        initSpring();
    }

//    private void pickImage() {
//        MainActivity mainActivity = (MainActivity) getContext();
//        mainActivity.pickImage(status, isDraggable());
//    }

    /**
     * 初始化Spring相关
     */
    private void initSpring() {
        SpringSystem mSpringSystem = SpringSystem.create();
        springX = mSpringSystem.createSpring();
        springY = mSpringSystem.createSpring();

        springX.addListener(new SimpleSpringListener() {
            @Override
            public void onSpringUpdate(Spring spring) {
                int xPos = (int) spring.getCurrentValue();
                setScreenX(xPos);
            }
        });

        springY.addListener(new SimpleSpringListener() {
            @Override
            public void onSpringUpdate(Spring spring) {
                int yPos = (int) spring.getCurrentValue();
                setScreenY(yPos);
            }
        });

        springX.setSpringConfig(springConfigCommon);
        springY.setSpringConfig(springConfigCommon);
    }

    /**
     * 调整ImageView的宽度和高度各为FrameLayout的一半
     */
    private void adjustImageView() {
        if (Utils.ENABLE_GLOBAL_LOG) {
            mLog.d(TAG, " * adjustImageView, status= %d", status);
            mLog.d(TAG, " * view= " + this.getClass().getSimpleName()
                    + "@" + hashCode());
        }

//        if (status != STATUS_LEFT_TOP) {
            imageView.setScaleX(scaleRate);
            imageView.setScaleY(scaleRate);

            maskView.setScaleX(scaleRate);
            maskView.setScaleY(scaleRate);
//        }

        setCurrentSpringPos(getLeft(), getTop());
    }

    public void setScaleRate(float scaleRate) {
        if (Utils.ENABLE_GLOBAL_LOG) {
            mLog.d(TAG, " * setScaleRate");
        }
        this.scaleRate = scaleRate;
        this.smallerRate = scaleRate * 0.9f;
    }

    /**
     * 从一个状态切换到另一个状态
     */
    public void switchPosition(int toStatus) {
        if (this.status == toStatus) {
            throw new RuntimeException("程序错乱");
        }

        if (toStatus == STATUS_LEFT_TOP) {
            scaleSize(SCALE_LEVEL_1);
        } else if (this.status == STATUS_LEFT_TOP) {
            scaleSize(SCALE_LEVEL_2);
        }

        this.status = toStatus;
        Point point = parentView.getOriginViewPos(status);
        this.moveDstX = point.x;
        this.moveDstY = point.y;
        animTo(moveDstX, moveDstY);
    }

    public void animTo(int xPos, int yPos) {
        springX.setEndValue(xPos);
        springY.setEndValue(yPos);
    }

    /**
     * 设置缩放大小
     */
    public void scaleSize(int scaleLevel) {
        if (Utils.ENABLE_GLOBAL_LOG) {
            mLog.d(TAG, " -- scaleSize, scaleLevel= %d", scaleLevel);
        }
        float rate = scaleRate;

        switch(scaleLevel) {
            case SCALE_LEVEL_1:
                rate = 1.0f;
                break;
            case SCALE_LEVEL_2:
                rate = scaleRate;
                break;
            case SCALE_LEVEL_3:
                rate = smallerRate;
                break;
        }


        if (scaleAnimator != null && scaleAnimator.isRunning()) {
            scaleAnimator.cancel();
        }

        scaleAnimator = ObjectAnimator
                .ofFloat(this, "custScale", imageView.getScaleX(), rate)
                .setDuration(200);
        scaleAnimator.setInterpolator(new DecelerateInterpolator());
        scaleAnimator.start();
    }

    public void saveAnchorInfo(int downX, int downY) {
        int halfSide = getMeasuredWidth() / 2;
        moveDstX = downX - halfSide;
        moveDstY = downY - halfSide;
    }

    /**
     * 真正开始动画
     */
    public void startAnchorAnimation() {
        if (moveDstX == Integer.MIN_VALUE || moveDstX == Integer.MIN_VALUE) {
            return;
        }
        if (Utils.ENABLE_GLOBAL_LOG) {
            mLog.d(TAG, " -- startAnchorAnimation");
        }

        springX.setOvershootClampingEnabled(true);
        springY.setOvershootClampingEnabled(true);
        animTo(moveDstX, moveDstY);
        scaleSize(DraggableCardItem.SCALE_LEVEL_1);
    }

    public void setScreenX(int screenX) {
        this.offsetLeftAndRight(screenX - getLeft());
    }

    public void setScreenY(int screenY) {
        this.offsetTopAndBottom(screenY - getTop());
    }

    public int computeDraggingX(int dx) {
        this.moveDstX += dx;
        return this.moveDstX;
    }

    public int computeDraggingY(int dy) {
        this.moveDstY += dy;
        return this.moveDstY;
    }

    /**
     * 设置当前spring位置
     */
    private void setCurrentSpringPos(int xPos, int yPos) {
        springX.setCurrentValue(xPos);
        springY.setCurrentValue(yPos);
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public int getStatus() {
        return status;
    }

    public void setParentView(ArcLayout parentView) {
        if (Utils.ENABLE_GLOBAL_LOG) {
            mLog.d(TAG, " * setParentView, view= " + this.getClass().getSimpleName()
                    + "@" + hashCode());
        }

        this.parentView = parentView;
    }

    public void onDragRelease() {
        if (Utils.ENABLE_GLOBAL_LOG) {
            mLog.d(TAG, " -- onDragRelease");
        }
        scaleSize(DraggableCardItem.SCALE_LEVEL_2);
//        if (status == DraggableCardItemNew.STATUS_LEFT_TOP) {
//            scaleSize(DraggableCardItemNew.SCALE_LEVEL_1);
//        } else {
//            scaleSize(DraggableCardItemNew.SCALE_LEVEL_2);
//        }

        springX.setOvershootClampingEnabled(false);
        springY.setOvershootClampingEnabled(false);
        springX.setSpringConfig(springConfigCommon);
        springY.setSpringConfig(springConfigCommon);

        Point point = parentView.getOriginViewPos(status);
        setCurrentSpringPos(getLeft(), getTop());
        this.moveDstX = point.x;
        this.moveDstY = point.y;
        animTo(moveDstX, moveDstY);
        if (Utils.ENABLE_GLOBAL_LOG) {
            mLog.d(TAG, " -- onDragRelease, point.x= %d, point.y= %d",
                    point.x, point.y);
        }
    }

    public void fillImageView(String imagePath) {
        this.imagePath = imagePath;
        addView.setVisibility(View.GONE);
        Glide.with(imageView).load(imagePath).into(imageView);
    }

    // 以下两个get、set方法是为自定义的属性动画CustScale服务，不能删
    public void setCustScale(float scale) {
        imageView.setScaleX(scale);
        imageView.setScaleY(scale);

        maskView.setScaleX(scale);
        maskView.setScaleY(scale);
    }

    public float getCustScale() {
        return imageView.getScaleX();
    }

    public void updateEndSpringX(int dx) {
        springX.setEndValue(springX.getEndValue() + dx);
    }

    public void updateEndSpringY(int dy) {
        springY.setEndValue(springY.getEndValue() + dy);
    }

    public boolean isDraggable() {
        return true;
//        return imagePath != null;
    }


}

