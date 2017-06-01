package com.lingyi.vangogh;

import android.graphics.Rect;
import android.support.v7.widget.RecyclerView;
import android.util.SparseArray;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by lingyi on 2017/5/24.
 * Copyright © 1994-2017 lingyi™ Inc. All rights reserved.
 */
public class FlowGridLayoutManager extends RecyclerView.LayoutManager {

    private int verticalScrollOffset = 0;
    //保存所有的Item的上下左右的偏移量信息
    private SparseArray<Object> allItemFrames = new SparseArray<>();

    private int mNumcolumnsPerRow = 3;//每行分成几等分

    private int mLastVisibleRowIndex = 0; //最后一个可见item的position
    private int mFirstVisibleRowIndex = 0;//第一个可见item的position
    private SpanSizeLookUp spansizeLookUp;

    public FlowGridLayoutManager(int columnSpan){
        this.mNumcolumnsPerRow = columnSpan;
        mFirstVisibleRowIndex = 0;
    }

    @Override
    public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
        //如果没有item，直接返回
        if (getItemCount() <= 0) return;
        // 跳过preLayout，preLayout主要用于支持动画
        if (state.isPreLayout()) {
            return;
        }
        if(this.spansizeLookUp == null){
            this.spansizeLookUp = new DefualtSpanSizeLookUp();
        }

        if(spansizeLookUp.mNumcolumnsPerRow == 0){
            this.spansizeLookUp.setNumColumnCount(this.mNumcolumnsPerRow,getPaddingTop(),getPaddingLeft());
        }
        //在布局之前，将所有的子View先Detach掉，放入到Scrap缓存中
        detachAndScrapAttachedViews(recycler);

        mLastVisibleRowIndex = getItemCount();

        fill(recycler,state,0);//填充可见区域的item
    }

    /**
     *
     * @return 返回TRUE带便支持纵向滑动
     */
    @Override
    public boolean canScrollVertically() {
        return true;
    }

    public void setSpanSizeLookUp(SpanSizeLookUp spansize){
        if(spansize != null){
            this.spansizeLookUp = spansize;
        }
    }
    @Override
    public int scrollVerticallyBy(int dy, RecyclerView.Recycler recycler, RecyclerView.State state) {
        //位移0、没有子View 当然不移动
        if (dy == 0 || getChildCount() == 0) {
            return 0;
        }
        int realOffset = dy;//实际滑动的距离， 可能会在边界处被修复

        //边界修复代码
        if (verticalScrollOffset + realOffset < 0) {//上边界
            realOffset = -verticalScrollOffset;
        } else if (realOffset > 0) {//下边界
            //利用最后一个子View比较修正
            View lastChild = getChildAt(getChildCount() - 1);
            if (getPosition(lastChild) == getItemCount() - 1) {
                int gap = getHeight() - getPaddingBottom() - getDecoratedBottom(lastChild);
                if (gap > 0) {
                    realOffset = -gap;
                } else if (gap == 0) {
                    realOffset = 0;
                } else {
                    realOffset = Math.min(realOffset, -gap);
                }
            }
        }
        realOffset = fill(recycler, state, realOffset);//先填充，再位移。因为在滑动的过程中会有新的item要显示出来，也会有旧的item需要回收

        verticalScrollOffset += realOffset;//累加实际滑动距离

        offsetChildrenVertical(-realOffset);//滑动

        return realOffset;
    }

    private int fill(RecyclerView.Recycler recycler, RecyclerView.State state,int dy){
        int realOffset = 0;
        if(dy >= 0){//初始化或者向上滑动
            realOffset = fillDown(recycler,state,dy);
        }else{
            realOffset = fillUp(recycler,state,dy);
        }
        return realOffset;
    }

    /**
     * 从下往上逆序填充  手机从上往下滑动的时候
     * @param recycler
     * @param state
     * @param dy 滑动距离
     * @return 实际位移距离
     */
    private int fillUp(RecyclerView.Recycler recycler, RecyclerView.State state,int dy){
        //回收越界子View
        if (getChildCount() > 0) {//滑动时进来的 回收当前屏幕，下越界的View
            for (int i = getChildCount() - 1; i >= 0; i--) {
                View child = getChildAt(i);
                if (getDecoratedTop(child) - dy > getHeight() - getPaddingBottom()) {
                    removeAndRecycleView(child, recycler);
                    mLastVisibleRowIndex--;
                    continue;
                }
            }
        }
        int maxPos = getItemCount() - 1;
        mFirstVisibleRowIndex = 0;
        if(getChildCount() > 0){
            View firstView = getChildAt(0);
            maxPos = getPosition(firstView) + mNumcolumnsPerRow;//避免不规则不出现 所以从第一个child开始再往后加一列
        }
        for(int i = maxPos ; i >= 0 ; i--){
            Rect frame = (Rect) allItemFrames.get(i);
            if(frame == null)continue;
            if(frame.bottom - verticalScrollOffset - dy < getPaddingTop()){
                 // mFirstVisibleRowIndex = i+1; //这里当前view没显示在屏幕上 并不代表下一个view不显示在屏幕上，因为这是不规则的、
                /**
                 *    —————————————————
                 *   |         |       |
                 *   |         |       |
                 *   |         |    1  |
                 *   |         |       |
                 *   |     0   |—————— |
                 *   |         |       |
                 *   |         |     2 |
                 *   |         |       |
                 *   |——————— ———————— |
                 *   |
                 *   |    3
                 *   |
                 *
                 *     例如 现在从上往下滑动 3此时显示在屏幕上 0 ，1，2  在屏幕上边缘外，继续滑动
                 *     这时候计算到position 2  应该出现在屏幕上了，把2添加进去，但是继续判断 1 此时1的bottom在屏幕外不应显示出来，如果现在是一个规则的布局
                 *     1以下的view按理都不可能显示在屏幕中了  但是这是一个不规则的布局，虽然1没有显示在屏幕上，但是0已经出现了，position 0 应该显示出来。。。。
                 *
                 */
//                 break;
            }else{
                mFirstVisibleRowIndex = i;
                boolean hasAdd = false;
                for(int childIndex = 0 ; childIndex < getChildCount() ; childIndex++){
                    View child = getChildAt(childIndex);
                    int pos = getPosition(child);
                    if(pos == i){
                        hasAdd = true;
                        break;
                    }
                }
                if(!hasAdd){
                    View scrap = recycler.getViewForPosition(i);
                    addView(scrap,0);
                    measureChild(scrap,0,0);
                    layoutDecorated(scrap,
                            frame.left,
                            frame.top - verticalScrollOffset,
                            frame.right,
                            frame.bottom - verticalScrollOffset);
                }
            }
        }
        return dy;
    }

    /**
     * 从上往下填充
     * @param recycler
     * @param state
     * @param dy 滑动距离
     * @return  实际位移距离
     */
    private int  fillDown(RecyclerView.Recycler recycler, RecyclerView.State state,int dy){
        int topOffset = getPaddingTop();
        //回收越界子View
        if (getChildCount() > 0) {//滑动时进来的 需要回收当前屏幕，上越界的View
            for (int i = getChildCount() - 1; i >= 0; i--) {
                View child = getChildAt(i);
                if (getDecoratedBottom(child) - dy < topOffset) {
                    removeAndRecycleView(child, recycler);
                    mFirstVisibleRowIndex = getPosition(getChildAt(0));
                    continue;
                }
            }
        }
        int minPos = mFirstVisibleRowIndex;
        mLastVisibleRowIndex = getItemCount() -1;
        if(getChildCount() > 0){
            View lastchild = getChildAt(getChildCount() - 1);
            minPos = getPosition(lastchild) + 1;//从最后一个View+1开始吧
        }
        for (int i=minPos; i <= mLastVisibleRowIndex ; i++){
            View child = recycler.getViewForPosition(i);
            addView(child);
            measureChild(child,0,0);
            int width = getDecoratedMeasuredWidth(child);
            int height = getDecoratedMeasuredHeight(child);
            Rect frame = (Rect) allItemFrames.get(i);
            PointOffset po;
            if (frame == null) {
                frame = new Rect();
                RowItem item = spansizeLookUp.getRowSpanItem(i);
                po = spansizeLookUp.calculateLeftTopCoordinate(i,width,height);
                frame.left = po.leftOffset - width;
                frame.top = po.topOffset - height;
                frame.right = po.leftOffset;
                frame.bottom = po.topOffset;
                // 将当前的Item的Rect边界数据保存
                allItemFrames.put(i, frame);
            }
            topOffset = frame.top - verticalScrollOffset;//判断这个view的头部是否已经露出屏幕下边缘，如果没有  那当前这个view不用展示
            if(topOffset -dy > getHeight() -getPaddingBottom()){
                mLastVisibleRowIndex = i -1;
                removeAndRecycleView(child, recycler);
            }else{
                layoutDecorated(child,
                        frame.left,
                        frame.top - verticalScrollOffset,
                        frame.right,
                        frame.bottom - verticalScrollOffset);
            }
            //添加完后，判断是否已经没有更多的ItemView，并且此时屏幕仍有空白，则需要修正dy
            View lastChild = getChildAt(getChildCount() - 1);
            if (getPosition(lastChild) == getItemCount() - 1) {
                int gap = getHeight() - getPaddingBottom() - getDecoratedBottom(lastChild);
                if (gap > 0) {
                    dy -= gap;
                }
            }
        }
        return dy;
    }
    private int getVerticalSpace() {
        return getHeight() - getPaddingBottom() - getPaddingTop();
    }

    private int getHorizontalSpace() {
        return getWidth() - getPaddingRight() - getPaddingLeft();
    }

    @Override
    public RecyclerView.LayoutParams generateDefaultLayoutParams() {
        return new RecyclerView.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    }


    public abstract static class SpanSizeLookUp{
        private int mNumcolumnsPerRow = 0;
        private int mPaddingTop;
        private int mPaddingLeft;
        private List<PointOffset> mColumnRowOffset = new ArrayList<>();
        private int mLastNotSpacingRowIndex = -100;

        public abstract  RowItem getRowSpanItem(int position);

        public abstract  int getRowSpancing();
        public abstract  int getColumnSpacing();
        public abstract boolean shouldViewTypeHaveSpacing(int viewType);
        public SpanSizeLookUp(){
        }
        private void setNumColumnCount(int numcolumnCount,int paddingTop,int paddingLeft){
            this.mPaddingLeft = paddingLeft;
            this.mPaddingTop = paddingTop;
            this.mNumcolumnsPerRow = numcolumnCount;
            for(int i = 0 ; i < mNumcolumnsPerRow ; i++){
                PointOffset po = new PointOffset();
                po.rowIndex = 0;
                po.topOffset = mPaddingTop;
                po.leftOffset = mPaddingLeft;
                mColumnRowOffset.add(po);
            }

        }
        /**
         * 按照瀑布流排列 计算当前item的位置矩形
         * @return
         */
        private PointOffset calculateLeftTopCoordinate(int position, int width, int height){
            int currentColumnIndex = 0;
            for(int i = 0 ; i < mColumnRowOffset.size() ; i++ ){
                if(mColumnRowOffset.get(i).rowIndex < mColumnRowOffset.get(currentColumnIndex).rowIndex){
                    currentColumnIndex = i ;
                    break;
                }
            }

            RowItem item = getRowSpanItem(position);
            if((mNumcolumnsPerRow - currentColumnIndex) < item.getColumnSpan()){
                currentColumnIndex = 0;
            }

            int rowIndex = mColumnRowOffset.get(currentColumnIndex).rowIndex;
            int start = currentColumnIndex;
            int end = currentColumnIndex+item.getColumnSpan();
            int currentColumTopOffset = 0;
            int currentLeftOffset = 0;
            for(int j = start ; j < end ; j ++){
                if(mColumnRowOffset.get(j).rowIndex > rowIndex){
                    currentColumTopOffset = 0;
                    int offset = j+1+(end -start);
                    if(offset < mNumcolumnsPerRow){
                        start = j+1;
                        end = offset;
                        j = start;
                    }else{
                        rowIndex ++;
                        start = 0 ;
                        end = start + item.getColumnSpan();
                        j = 0;
                    }
                }
                if(mColumnRowOffset.get(j).topOffset > currentColumTopOffset){
                    currentColumTopOffset = mColumnRowOffset.get(j).topOffset;
                }

            }
            if(start == 0){
                currentLeftOffset = mPaddingLeft;
            }else{
                currentLeftOffset = mColumnRowOffset.get(start -1).leftOffset;
            }
            PointOffset po = new PointOffset();
            po.rowIndex = rowIndex+item.getRowSpan();
            po.topOffset = currentColumTopOffset + (rowIndex != 0?getRowSpancing():0)+height;
            po.leftOffset = currentLeftOffset + (start != 0?getColumnSpacing():0) + width;
            po.start = start;
            po.end = end;

            if((rowIndex -1) == mLastNotSpacingRowIndex){
                po.topOffset -= getRowSpancing();
            }
            if(!shouldViewTypeHaveSpacing(item.getViewType())){
                mLastNotSpacingRowIndex = rowIndex;
                po.topOffset -= getRowSpancing();
            }
            for(int k = start ;k < end ; k++ ){
                mColumnRowOffset.set(k,po);
            }
            return po;
        }
    }

    public static class RowItem<T extends Object>{
        private int columnSpan;//所占的列的宽度
        private int RowSpan;//所占的行的宽度
        private int index;//item index
        private T itemData;
        private int viewType;

        public int getViewType() {
            return viewType;
        }

        public void setViewType(int viewType) {
            this.viewType = viewType;
        }

        public T getItemData() {
            if(itemData == null){
                itemData = (T) new Object();
            }
            return itemData;
        }

        public void setItemData(T itemData) {
            this.itemData = itemData;
        }

        public int getColumnSpan() {
            return columnSpan;
        }

        public void setColumnSpan(int columnSpan) {
            this.columnSpan = columnSpan;
        }

        public int getRowSpan() {
            return RowSpan;
        }

        public void setRowSpan(int rowSpan) {
            RowSpan = rowSpan;
        }

        public int getIndex() {
            return index;
        }

        public void setIndex(int index) {
            this.index = index;
        }
    }

    /**
     * 默认的SpanSizeLookUp的实现，每个item占用一行一列
     */
    public static final class DefualtSpanSizeLookUp extends SpanSizeLookUp{

        public DefualtSpanSizeLookUp() {
            super();
        }
        @Override
        public RowItem getRowSpanItem(int position) {
            RowItem item = new RowItem();
            item.setRowSpan(1);
            item.setColumnSpan(1);
            item.setIndex(position);
            return item;
        }

        @Override
        public int getRowSpancing() {
            return 0;
        }

        @Override
        public int getColumnSpacing() {
            return 0;
        }

        @Override
        public boolean shouldViewTypeHaveSpacing(int viewType) {
            return true;
        }

    }
    public static class PointOffset{
        int rowIndex;
        int topOffset;
        int leftOffset;
        int start;
        int end;
    }

}
