package com.lingyi.vangogh;

import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private RecyclerView recyclerView;
    private List<FlowGridLayoutManager.RowItem> items;
    private int defaultWidth = 0;
    private int defaultColumnSpacing = 12;
    private int defaultRowSpacing = 24;
    private final static int DEFAULT_VIEW_TYPE = 0x01;
    private TextView mChildCount;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        recyclerView = (RecyclerView) findViewById(R.id.recyclerView);
        mChildCount = (TextView) findViewById(R.id.show_child_count);
        items = new ArrayList<>();
        initData();
        defaultWidth = ((getScreenWidth() - defaultColumnSpacing * (3 - 1) - recyclerView.getPaddingLeft() - recyclerView.getPaddingRight()) / 3);
        recyclerView.setPadding(24, 24, 24, 0);
        FlowGridLayoutManager layoutManager = new FlowGridLayoutManager(3, defaultWidth, defaultWidth);
        layoutManager.setSpanSizeLookUp(new FlowGridLayoutManager.SpanSizeLookUp() {
            @Override
            int ItemCount() {
                return items.size();
            }

            @Override
            public FlowGridLayoutManager.RowItem getRowSpanItem(int position) {
                return items.get(position);
            }

            @Override
            public int getRowSpancing() {
                return defaultRowSpacing;
            }

            @Override
            public int getColumnSpacing() {
                return defaultColumnSpacing;
            }

            @Override
            public int getDefaultViewType() {
                return DEFAULT_VIEW_TYPE;
            }

        });
        MyAdapter adapter = new MyAdapter();
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(adapter);

        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                mChildCount.setText("recyclerView child count:"+recyclerView.getChildCount());
            }
        });
    }

    //初始化数据
    private void initData() {
        FlowGridLayoutManager.RowItem item = new FlowGridLayoutManager.RowItem();
        item.setRowSpan(1);
        item.setColumnSpan(3);
        item.setIndex(1);
        items.add(item);

        item = new FlowGridLayoutManager.RowItem();
        item.setRowSpan(1);
        item.setColumnSpan(1);
        item.setIndex(1);
        items.add(item);

        item = new FlowGridLayoutManager.RowItem();
        item.setRowSpan(2);
        item.setIndex(2);
        item.setColumnSpan(2);
        items.add(item);

        item = new FlowGridLayoutManager.RowItem();
        item.setRowSpan(1);
        item.setIndex(3);
        item.setColumnSpan(1);
        items.add(item);

        item = new FlowGridLayoutManager.RowItem();
        item.setRowSpan(1);
        item.setIndex(4);
        item.setColumnSpan(3);
        items.add(item);


        item = new FlowGridLayoutManager.RowItem();
        item.setRowSpan(2);
        item.setIndex(5);
        item.setColumnSpan(2);
        items.add(item);

        item = new FlowGridLayoutManager.RowItem();
        item.setRowSpan(1);
        item.setIndex(6);
        item.setColumnSpan(1);
        items.add(item);

        item = new FlowGridLayoutManager.RowItem();
        item.setRowSpan(1);
        item.setIndex(7);
        item.setColumnSpan(1);
        items.add(item);


        item.setRowSpan(1);
        item.setColumnSpan(1);
        item.setIndex(8);
        items.add(item);

        item = new FlowGridLayoutManager.RowItem();
        item.setRowSpan(2);
        item.setIndex(9);
        item.setColumnSpan(2);
        items.add(item);

        item = new FlowGridLayoutManager.RowItem();
        item.setRowSpan(1);
        item.setIndex(10);
        item.setColumnSpan(1);
        items.add(item);

        item = new FlowGridLayoutManager.RowItem();
        item.setRowSpan(2);
        item.setIndex(11);
        item.setColumnSpan(2);
        items.add(item);

        item = new FlowGridLayoutManager.RowItem();
        item.setRowSpan(1);
        item.setIndex(12);
        item.setColumnSpan(1);
        items.add(item);

        item = new FlowGridLayoutManager.RowItem();
        item.setRowSpan(1);
        item.setIndex(13);
        item.setColumnSpan(1);
        items.add(item);

        item = new FlowGridLayoutManager.RowItem();
        item.setRowSpan(2);
        item.setIndex(14);
        item.setColumnSpan(1);
        items.add(item);

        item = new FlowGridLayoutManager.RowItem();
        item.setRowSpan(2);
        item.setIndex(15);
        item.setColumnSpan(1);
        items.add(item);

        item = new FlowGridLayoutManager.RowItem();
        item.setRowSpan(2);
        item.setIndex(16);
        item.setColumnSpan(1);
        items.add(item);

        item = new FlowGridLayoutManager.RowItem();
        item.setRowSpan(4);
        item.setIndex(17);
        item.setColumnSpan(2);
        items.add(item);
        for (int i = 18; i < 900 ; i++) {
            if(i == 22){
                item = new FlowGridLayoutManager.RowItem();
                item.setRowSpan(1);
                item.setIndex(i);
                item.setColumnSpan(3);
                items.add(item);
                continue;
            }
            item = new FlowGridLayoutManager.RowItem();
            item.setRowSpan(1);
            item.setIndex(i);
            item.setColumnSpan(1);
            items.add(item);
        }
    }

    //自定义Adapter
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
            holder.setStr(position, item,holder.getItemViewType());
        }

        @Override
        public int getItemViewType(int i) {
            if (i == 0 || i == 4 || i == 22) {
                return 0x02;
            }
            return DEFAULT_VIEW_TYPE;
        }

        @Override
        public int getItemCount() {
            return items.size();
        }
    }

    //自定义Holder
    public class MyViewHolder extends RecyclerView.ViewHolder {
        private TextView strTv;

        public MyViewHolder(View itemView) {
            super(itemView);
            strTv = (TextView) itemView.findViewById(R.id.str);
        }

        public void setStr(final int str,FlowGridLayoutManager.RowItem item, int viewType) {
            if (viewType == DEFAULT_VIEW_TYPE) {
                strTv.setText(str + "");
                int num = (int) (Math.random() * 16777216);

                StringBuilder sb = new StringBuilder(Integer.toHexString(num));
                while (sb.length() < 6){
                    sb.append("0");
                }
                Log.i("lingyilog", "setStr: color:"+sb);
                itemView.setBackgroundColor(Color.parseColor("#"+sb));
                RecyclerView.LayoutParams params = (RecyclerView.LayoutParams) itemView.getLayoutParams();
                params.width = defaultWidth * item.getColumnSpan() + (item.getColumnSpan() - 1) * defaultColumnSpacing;
                params.height = defaultWidth * item.getRowSpan() + (item.getRowSpan() - 1) * defaultRowSpacing;
                itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Log.i("lingyilog", "onClick: position:" + str);
                        Toast.makeText(MainActivity.this, "点击了第几个item：" + str, Toast.LENGTH_SHORT).show();
                    }
                });
            }else{
                RecyclerView.LayoutParams params = (RecyclerView.LayoutParams) itemView.getLayoutParams();
                params.width = defaultWidth * 3 + (3 - 1) * defaultColumnSpacing;
                params.height = 180;
                itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Toast.makeText(MainActivity.this, "我是title：" + str, Toast.LENGTH_SHORT).show();
                    }
                });
            }
        }

    }

    public int getScreenWidth() {
        return getResources().getDisplayMetrics().widthPixels;
    }
}
