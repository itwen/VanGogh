package com.lingyi.vangogh;

/**
 * Created by lingyi on 2017/5/24.
 * Copyright © 1994-2017 lingyi™ Inc. All rights reserved.
 */

public class ItemPositionInfo {
    private int pos;
    private int rowIndex;
    private FlowGridLayoutManager.RowItem item;
    private int mCurrentColumnIndex = 0;
    private int topOffset;
    private int leftOffset;

    public int getTopOffset() {
        return topOffset;
    }

    public void setTopOffset(int topOffset) {
        this.topOffset = topOffset;
    }

    public int getLeftOffset() {
        return leftOffset;
    }

    public void setLeftOffset(int leftOffset) {
        this.leftOffset = leftOffset;
    }

    public int getmCurrentColumnIndex() {
        return mCurrentColumnIndex;
    }

    public void setmCurrentColumnIndex(int mCurrentColumnIndex) {
        this.mCurrentColumnIndex = mCurrentColumnIndex;
    }

    public FlowGridLayoutManager.RowItem getItem() {
        if (item == null) {
            item = new FlowGridLayoutManager.RowItem();
        }
        return item;
    }

    public void setItem(FlowGridLayoutManager.RowItem item) {
        this.item = item;
    }

    public int getPos() {
        return pos;
    }

    public void setPos(int pos) {
        this.pos = pos;
    }

    public int getRowIndex() {
        return rowIndex;
    }

    public void setRowIndex(int rowIndex) {
        this.rowIndex = rowIndex;
    }

    public ItemPositionInfo(int pos, int rowIndex, FlowGridLayoutManager.RowItem item, int mCurrentColumnIndex) {
        this.pos = pos;
        this.rowIndex = rowIndex;
        this.item = item;
        this.mCurrentColumnIndex = mCurrentColumnIndex;
    }

    @Override
    public String toString() {
        return "ItemPositionInfo{" +
                "pos=" + pos +
                ", rowIndex=" + rowIndex +
                ", item=" + item +
                ", mCurrentColumnIndex=" + mCurrentColumnIndex +
                '}';
    }
}
