package com.powyin.nestscroll;
import android.app.Activity;
import android.os.Bundle;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import com.powyin.nestscroll.refresh.SwipeControlStyle_Horizontal;
import com.powyin.scroll.widget.SwipeNest;

/**
 * Created by powyin on 2016/7/27.
 */
public class SimpleSwipeNest extends Activity implements View.OnClickListener{
    SwipeNest swipeNest;
    RecyclerView mRecyclerView;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.simple_swipe_nest);
        findView();
        init();
    }


    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.click_me_to_set_swipe_control:
                swipeNest.setSwipeControl(new SwipeControlStyle_Horizontal(this));
                break;
            case R.id.click_me_to_stop_head:
                swipeNest.finishRefresh();
                break;
        }
    }

    private void findView(){
        findViewById(R.id.click_me_to_set_swipe_control).setOnClickListener(this);
        findViewById(R.id.click_me_to_stop_head).setOnClickListener(this);
        mRecyclerView = (RecyclerView)findViewById(R.id.my_recycle);
        swipeNest = (SwipeNest)findViewById(R.id.nest_combine);
    }

    private void init(){
        initRecycle();
        swipeNest.setOnRefreshListener(new SwipeNest.OnRefreshListener() {
            @Override
            public void onRefresh() {
                System.out.println("------------------------------------------------onRefresh----------------------->>>>>>>>");
            }

        });
    }

    private void initRecycle(){
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mRecyclerView.setAdapter(new RecyclerAdapter(this));
        mRecyclerView.setItemAnimator(new DefaultItemAnimator());
    }

}