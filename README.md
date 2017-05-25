## 一款强大的recycleview布局管理器

### 像gridlayoumanager的瀑布流布局，gridlayout只支持合并列，不支持合并行，这个做到了瀑布流和gridlayout布局双重特点，任一行列合并单元格，使用简单。

##使用姿势

```
 FlowGridLayoutManager layoutManager = new FlowGridLayoutManager(3, defaultWidth, defaultWidth);

``` 

### 第一个参数3指定纵向总共几列，第二三个参数传入一个默认单元格的默认宽高，


```
        layoutManager.setSpanSizeLookUp(new FlowGridLayoutManager.SpanSizeLookUp() {
            @Override
            int ItemCount() {
                return items.size(); //所有的item个数
            }

            @Override
            public FlowGridLayoutManager.RowItem getRowSpanItem(int position) {
//                FlowGridLayoutManager.RowItem item = new FlowGridLayoutManager.RowItem();
//                item.setRowSpan(1);
//                item.setColumnSpan(3);
//                item.setIndex(1);
                return items.get(position); //返回对应位置item  item的格式是 告诉recycleview 这个item占几行几列
            }

            @Override
            public int getRowSpancing() {
                return defaultRowSpacing;//默认行间距
            }

            @Override
            public int getColumnSpacing() {
                return defaultColumnSpacing;//默认列间距
            }

            @Override
            public int getDefaultViewType() {
                return DEFAULT_VIEW_TYPE; //返回一个宫格item的默认viewtype  只有这个viewtype的item之间才会有间隔
            }

        });
        
         recyclerView.setLayoutManager(layoutManager);
```


### 然后在创建一个adapter


```
    class MyAdapter extends RecyclerView.Adapter<MyViewHolder> {

        @Override
        public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            if (viewType == DEFAULT_VIEW_TYPE) {
                View v = LayoutInflater.from(MainActivity.this).inflate(R.layout.recycler_view_item, parent, false);
                MyViewHolder viewHolder = new MyViewHolder(v);
                return viewHolder;
            }
            View v = LayoutInflater.from(MainActivity.this).inflate(R.layout.recycler_view_item_title, parent, false);
            MyViewHolder viewHolder = new MyViewHolder(v);
            return viewHolder;
        }

        @Override
        public void onBindViewHolder(MyViewHolder holder, int position) {
            FlowGridLayoutManager.RowItem item = items.get(position);
            holder.bindData(position, item,holder.getItemViewType());
        }

        @Override
        public int getItemViewType(int i) {
            if (i == 0 || i == 4 || i == 22) { //position 为这些的item 是另外一种type 但是这个type之间是没有行列间距的  可以没有其他type
                return 0x02;
            }
            return DEFAULT_VIEW_TYPE;
        }

        @Override
        public int getItemCount() {
            return items.size();
        }
    }

```


### 最后的效果如下


![](http://chuantu.biz/t5/94/1495722766x2890171414.gif)

