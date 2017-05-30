package com.lingyi.vangogh;

import android.graphics.Rect;
import android.support.v7.widget.RecyclerView;
import android.util.SparseArray;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by lingyi on 2017/5/24.
 * Copyright © 1994-2017 lingyi™ Inc. All rights reserved.
 */
public class FlowGridLayoutManager extends RecyclerView.LayoutManager {

    private int verticalScrollOffset = 0;

    //保存所有的Item的上下左右的偏移量信息
    private SparseArray<Rect> allItemFrames = new SparseArray<>();

    private int mNumcolumnsPerRow = 3;//每行分成几等分

    private int mLastVisibleRowIndex = 0; //最后一个可见item的position
    private int mFirstVisibleRowIndex = 0;//第一个可见item的position
    private SpanSizeLookUp spansizeLookUp;

    private Map<Integer,ViewTypeSize> mViewTypeSizes = new HashMap<>(); //记录每种viewtype  分别得大小
    private Map<Integer,Map<Integer,Integer>> positionMapViewTypes = new HashMap<>(); //记录每个position对应的他当前位置不同的viewtype分别有多少个


    public FlowGridLayoutManager(int columnSpan){
        this.mNumcolumnsPerRow = columnSpan;
    }

    @Override
    public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
        //如果没有item，直接返回
        if (getItemCount() <= 0) return;
        // 跳过preLayout，preLayout主要用于支持动画
        if (state.isPreLayout()) {
            return;
        }

        if(spansizeLookUp == null){
            spansizeLookUp = new DefualtSpanSizeLookUp(mNumcolumnsPerRow,getItemCount());
            spansizeLookUp.caculateRowItemInfo();
        }
        //在布局之前，将所有的子View先Detach掉，放入到Scrap缓存中
        detachAndScrapAttachedViews(recycler);

        mFirstVisibleRowIndex = 0;
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
            this.spansizeLookUp.setNumColumnCount(this.mNumcolumnsPerRow);
            this.spansizeLookUp.caculateRowItemInfo();
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
        for(int i = maxPos ; i >= mFirstVisibleRowIndex ; i--){
            Rect frame = allItemFrames.get(i);
            if(frame == null)continue;
            if(frame.bottom - verticalScrollOffset - dy < getPaddingTop()){
//                  mFirstVisibleRowIndex = i+1; //这里当前view没显示在屏幕上 并不代表下一个view不显示在屏幕上，因为这是不规则的、
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
                 *   | ————————————————|
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
    private int  fillDown(RecyclerView.Recycler recycler, RecyclerView.State state,int dy){
        int topOffset = getPaddingTop();
        //回收越界子View
        if (getChildCount() > 0) {//滑动时进来的 需要回收当前屏幕，上越界的View
            for (int i = getChildCount() - 1; i >= 0; i--) {
                View child = getChildAt(i);
                if (getDecoratedBottom(child) - dy < topOffset) {
                    removeAndRecycleView(child, recycler);
                    mFirstVisibleRowIndex++;
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
            Rect frame = allItemFrames.get(i);
            if (frame == null) {
                frame = new Rect();
                ItemPositionInfo posInfo = spansizeLookUp.getItemPositionInfo(i);
                RowItem item = spansizeLookUp.getRowSpanItem(i);
                int viewType = getItemViewType(child);
                Map<Integer,Integer> viewtypeCount = new HashMap<>();
                if(i > 0){//主要是用来记录当前position下的每个viewtype的数量
                    if(!positionMapViewTypes.containsKey(i)){
                        viewtypeCount = positionMapViewTypes.get(i-1);
                        Map<Integer,Integer> copy = new HashMap<>();
                        copy.putAll(viewtypeCount);
                        if(copy.containsKey(viewType)){
                            copy.put(viewType,copy.get(viewType)+1);
                        }else{
                            copy.put(viewType,1);
                        }
                        positionMapViewTypes.put(i,copy);
                    }
                }else{
                    viewtypeCount.put(viewType,1);
                    positionMapViewTypes.clear();
                    positionMapViewTypes.put(i,viewtypeCount);
                }
                if(!mViewTypeSizes.containsKey(viewType)){
                    int defaultWidth = width;
                    int defaultHeight = height;
                    if(viewType == spansizeLookUp.getDefaultViewType()){
                        defaultWidth = (width - spansizeLookUp.getColumnSpacing()*(item.getColumnSpan()-1))/ item.getColumnSpan();
                        defaultHeight = (height - spansizeLookUp.getRowSpancing() * (item.getColumnSpan()-1)) / item.getRowSpan();
                    }
                    ViewTypeSize  viewTypeSize = new ViewTypeSize(defaultWidth,defaultHeight);
                    item.setDefaultHeight(defaultHeight);
                    item.setDefaultWidth(defaultWidth);
                    mViewTypeSizes.put(viewType,viewTypeSize);
                }
                int leftOffset = caculateLeftOffset(posInfo.getmCurrentColumnIndex());
                int top = caculateTopOffset(i,viewType,posInfo.getRowIndex() - item.getRowSpan());
                frame.set(leftOffset,top, leftOffset+width, top + height);
                // 将当前的Item的Rect边界数据保存
                allItemFrames.put(i, frame);
            }
            topOffset = frame.top - verticalScrollOffset;//判断这个view的头部是否已经露出屏幕下边缘，如果没有  那当前这个view不用展示
            if(topOffset -dy > getHeight() -getPaddingBottom()){
                mLastVisibleRowIndex = i -1;
                allItemFrames.remove(i);
                spansizeLookUp.rollbackRowIndex(i);
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
        private List<Integer> mColumnPointRowIndex = new ArrayList<>();//记录当前最下面的item的行号
        private Map<Integer,ItemPositionInfo> mRowItemInfos = new HashMap<>();
        private int mNumcolumnsPerRow = 3;
        public abstract int ItemCount();
        public abstract  RowItem getRowSpanItem(int position);
        public abstract int getRowSpancing();
        public abstract int getColumnSpacing();
        public abstract int getDefaultViewType ();
        public SpanSizeLookUp(){
        }
        private void setNumColumnCount(int numcolumnCount){
            this.mNumcolumnsPerRow = numcolumnCount;
            mColumnPointRowIndex = new ArrayList<>();
            for(int i = 0 ; i < mNumcolumnsPerRow ; i++){
                mColumnPointRowIndex.add(0);
            }
        }

        /**
         * 计算并且保存每个item应该对应的行下标和列下标，当往下滑动的时候 逆序布局直接使用保存好的位置信息
         */
        private void caculateRowItemInfo(){
            int currentColumnIndex = 0;
            if (mRowItemInfos.size() > 0){
                return;
            }
            for(int i = 0 ; i < ItemCount(); i ++){
                RowItem item = getRowSpanItem(i);

                currentColumnIndex = calculateCurrentColumnIndex();

                if((mNumcolumnsPerRow - currentColumnIndex) < item.getColumnSpan()){
                    currentColumnIndex = 0;
                }
                int rowIndex = mColumnPointRowIndex.get(currentColumnIndex);
                for(int j = currentColumnIndex ; j < (currentColumnIndex+item.getColumnSpan()) ; j ++){
                    if(mColumnPointRowIndex.get(j) > rowIndex){
                        rowIndex ++;
                        j = 0;
                    }
                }
                for(int k = currentColumnIndex ;k < currentColumnIndex+item.getColumnSpan() ; k++ ){
                    ItemPositionInfo itemPositionInfo = new ItemPositionInfo(i,rowIndex+item.getRowSpan(),item,currentColumnIndex);
                    mColumnPointRowIndex.set(k,rowIndex+item.getRowSpan());
                    mRowItemInfos.put(i,itemPositionInfo);
                }
            }
        }

        /**
         * 按照瀑布流排列 计算当前item应该从哪一列开始排列
         * @return
         */
        private int calculateCurrentColumnIndex(){
            int min = 0 ;
            for(int i = 0 ; i < mColumnPointRowIndex.size() ; i++ ){
                if(mColumnPointRowIndex.get(i) < mColumnPointRowIndex.get(min)){
                    min = i ;
                    break;
                }
            }
            return min;
        }

        /**
         * 回滚当前position 的行下标
         * @param position position
         */
        public void rollbackRowIndex(int position){
            ItemPositionInfo posInfo = mRowItemInfos.get(position);
            if(posInfo == null)return;
            for(int i = posInfo.getmCurrentColumnIndex(); i < posInfo.getmCurrentColumnIndex()+posInfo.getItem().getColumnSpan();i++){
                mColumnPointRowIndex.set(i,posInfo.getRowIndex() - posInfo.getItem().getRowSpan());
            }
        }

        /**
         * 返回当前position的 行位置信息
         * @param position
         * @return
         */
        public ItemPositionInfo getItemPositionInfo(int position){
            if(position >= mRowItemInfos.size()){
                return new ItemPositionInfo(0,0,new RowItem(),0);
            }

            return mRowItemInfos.get(position);
        }

    }

    public static class RowItem<T extends Object>{
        private int columnSpan;//所占的列的宽度
        private int RowSpan;//所占的行的宽度
        private int index;//item index
        private int viewType = 0; //viewTye  以后做支持多类型扩展
        private int columnSpacing;
        private int rowSpacing;
        private T itemData;
        private int defaultWidth;
        private int defaultHeight;

        public int getDefaultWidth() {
            return defaultWidth;
        }

        public void setDefaultWidth(int defaultWidth) {
            this.defaultWidth = defaultWidth;
        }

        public int getDefaultHeight() {
            return defaultHeight;
        }

        public void setDefaultHeight(int defaultHeight) {
            this.defaultHeight = defaultHeight;
        }

        public int getColumnSpacing() {
            return columnSpacing;
        }

        public void setColumnSpacing(int columnSpacing) {
            this.columnSpacing = columnSpacing;
        }

        public int getRowSpacing() {
            return rowSpacing;
        }

        public void setRowSpacing(int rowSpacing) {
            this.rowSpacing = rowSpacing;
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

        public int getViewType() {
            return viewType;
        }

        public void setViewType(int viewType) {
            this.viewType = viewType;
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
        private int itemCount = 0;
        public DefualtSpanSizeLookUp(int numcolumnCount,int itemCount) {
            super();
            this.itemCount = itemCount;
            super.setNumColumnCount(numcolumnCount);
        }

        @Override
        public int ItemCount() {
            return itemCount;
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
        public int getDefaultViewType() {
            return 0;
        }


    }

    public static class ViewTypeSize{
        private int mDefaultWidth;
        private int mDefaulTHeight;

        public int getmDefaultWidth() {
            return mDefaultWidth;
        }

        public void setmDefaultWidth(int mDefaultWidth) {
            this.mDefaultWidth = mDefaultWidth;
        }

        public int getmDefaulTHeight() {
            return mDefaulTHeight;
        }

        public void setmDefaulTHeight(int mDefaulTHeight) {
            this.mDefaulTHeight = mDefaulTHeight;
        }
        public ViewTypeSize(int mDefaultWidth, int mDefaulTHeight) {
            this.mDefaultWidth = mDefaultWidth;
            this.mDefaulTHeight = mDefaulTHeight;
        }
    }

    /**
     * 计算每个position对应的item的top偏移量
     * @param position 对应的item 下标
     * @param viewType  当前要计算的viewtype
     * @param currentRowIndex 当前起始行下标
     * @return
     */
    private int caculateTopOffset(int position ,int viewType,int currentRowIndex){
        int topOffset = getPaddingTop();
        int notDefaultViewTypeCount = 0 ;
        if(currentRowIndex == 0)return topOffset;
        Map<Integer,Integer> typeCount = positionMapViewTypes.get(position);
        for(Integer key:typeCount.keySet()){  //这段逻辑真的死恶心  不是宫格的view 不能让他有行列间隔，因为为了避免可能 titleview的间隔小于行列间隔
            if(key != spansizeLookUp.getDefaultViewType()){
                ViewTypeSize size = mViewTypeSizes.get(key);
                int count = typeCount.get(key);
                if(key == viewType){
                    count --;
                }
                notDefaultViewTypeCount += count;
                topOffset+= count*size.getmDefaulTHeight();
            }
        }
        int spacingCount = (currentRowIndex+1) - notDefaultViewTypeCount*2 - (viewType == spansizeLookUp.getDefaultViewType()?0:1);
        if(spacingCount < 0){
            spacingCount = 0;
        }
        int defaultHeight = 0;
        if(mViewTypeSizes.containsKey(spansizeLookUp.getDefaultViewType())){
            defaultHeight = mViewTypeSizes.get(spansizeLookUp.getDefaultViewType()).getmDefaulTHeight();
        }
        topOffset += spacingCount*spansizeLookUp.getRowSpancing()+defaultHeight*(currentRowIndex - notDefaultViewTypeCount);
        return topOffset;
    }

    private int caculateLeftOffset(int currentColumnIndex){
        int leftOffset = getPaddingLeft();
        int defaultWidth = 0;
        if(mViewTypeSizes.containsKey(spansizeLookUp.getDefaultViewType())){
            defaultWidth = mViewTypeSizes.get(spansizeLookUp.getDefaultViewType()).getmDefaultWidth();
        }
        leftOffset += (currentColumnIndex * (spansizeLookUp.getColumnSpacing() + defaultWidth));
        return leftOffset;
    }
}
