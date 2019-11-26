package com.vhcc.arclayoutlibs.drag;

import android.view.View;

import androidx.annotation.NonNull;
import androidx.customview.widget.ViewDragHelper;

/**
 * Created by IChen.Chu on 2019/11/26
 */
public class DragHelperCallback extends ViewDragHelper.Callback {

    @Override
    public boolean tryCaptureView(@NonNull View child, int pointerId) {
        return false;
    }

    @Override
    public void onViewPositionChanged(@NonNull View changedView, int left, int top, int dx, int dy) {
        super.onViewPositionChanged(changedView, left, top, dx, dy);
    }

    @Override
    public void onViewReleased(@NonNull View releasedChild, float xvel, float yvel) {
        super.onViewReleased(releasedChild, xvel, yvel);
    }

    @Override
    public int clampViewPositionHorizontal(@NonNull View child, int left, int dx) {
        return super.clampViewPositionHorizontal(child, left, dx);
    }

    @Override
    public int clampViewPositionVertical(@NonNull View child, int top, int dy) {
        return super.clampViewPositionVertical(child, top, dy);
    }
}
