package com.powyin.scroll.adapter;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.Context;
import android.database.DataSetObservable;
import android.database.DataSetObserver;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.support.annotation.NonNull;
import android.text.TextPaint;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListAdapter;
import android.widget.TextView;

import com.powyin.scroll.R;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by powyin on 2016/6/14.
 */
public class MultipleListAdapter<T> implements ListAdapter, AdapterDelegate<T> {

    // 0 空白页面；
    // 1 错误页面；
    // 2 加载更多；

    @SuppressWarnings("unchecked")
    @SafeVarargs
    public static <T, N extends T> MultipleListAdapter<N> getByViewHolder(Activity activity,
                                                                          Class<? extends PowViewHolder<? extends T>>... arrClass) {
        return new MultipleListAdapter(activity, arrClass);
    }

    @SuppressWarnings("unchecked")
    @SafeVarargs
    public static <T, N extends T> MultipleListAdapter<N> getByViewHolder(Activity activity, int loadMoreViewCount,
                                                                          Class<? extends PowViewHolder<? extends T>>... arrClass) {
        return new MultipleListAdapter(activity, loadMoreViewCount, arrClass);
    }


    private PowViewHolder[] mHolderInstances;                                                                          // viewHolder 类实现实例
    private Class<? extends PowViewHolder>[] mHolderClasses;                                                           // viewHolder class类
    private Class[] mHolderGenericDataClass;                                                                           // viewHolder 携带泛型
    private Activity mActivity;
    private List<T> mDataList = new ArrayList<>();
    private boolean mShowError = true;                                                                                 // 是否展示错误信息

    private final int mFixedLoadMoreCount;
    private int mSpaceCount;
    private int mLoadMoreCount;

    private final DataSetObservable mDataSetObservable = new DataSetObservable();


    // 上拉加载实现
    private boolean mLoadEnableShow = false;                                                                              // 是否展示加载更多
    private LoadedStatus mLoadStatus = LoadedStatus.CONTINUE;
    private LoadMorePowViewHolder loadMorePowViewHolder;
    private String mLoadCompleteInfo = "我是有底线的";
    OnLoadMoreListener mOnLoadMoreListener;                                                                       // 显示更多监听

    @SuppressWarnings("unchecked")
    @SafeVarargs
    public MultipleListAdapter(Activity activity, Class<? extends PowViewHolder<? extends T>>... viewHolderClass) {
        this(activity, 1, viewHolderClass);
    }

    @SuppressWarnings("unchecked")
    @SafeVarargs
    public MultipleListAdapter(Activity activity, int loadMoreViewCount, Class<? extends PowViewHolder<? extends T>>... viewHolderClass) {

        Class<? extends PowViewHolder>[] arrClass = new Class[viewHolderClass.length];
        System.arraycopy(viewHolderClass, 0, arrClass, 0, viewHolderClass.length);

        this.mActivity = activity;
        this.mFixedLoadMoreCount = loadMoreViewCount;
        this.mHolderClasses = arrClass;
        this.mHolderInstances = new PowViewHolder[arrClass.length];
        this.mHolderGenericDataClass = new Class[arrClass.length];

        for (int i = 0; i < arrClass.length; i++) {
            Type genericType;                                                                                          // class类(泛型修饰信息)
            Class typeClass = mHolderClasses[i];                                                                       // class类
            do {
                genericType = typeClass.getGenericSuperclass();
                typeClass = typeClass.getSuperclass();
            } while (typeClass != PowViewHolder.class && typeClass != Object.class);

            if (typeClass != PowViewHolder.class || genericType == PowViewHolder.class) {
                throw new RuntimeException("参数类必须继承泛型ViewHolder");
            }
            ParameterizedType paramType = (ParameterizedType) genericType;
            Type genericClass = paramType.getActualTypeArguments()[0];
            mHolderGenericDataClass[i] = (Class) genericClass;                                                         //赋值 泛型类型(泛型类持有)
            try {
                mHolderInstances[i] = mHolderClasses[i].getConstructor(Activity.class, ViewGroup.class).newInstance(mActivity, null);         //赋值 holder实例
            } catch (Exception e) {
                e.printStackTrace();
                throw new RuntimeException("参数类必须实现（Activity）单一参数的构造方法  或者  " + e.getMessage());
            }
        }

    }

    //----------------------------------------------------adapterImp----------------------------------------------------//

    @Override
    public boolean areAllItemsEnabled() {
        return true;
    }

    @Override
    public boolean isEnabled(int position) {
        return true;
    }


    @Override
    public int getCount() {
        return mDataList.size();
    }

    @Override
    public Object getItem(int position) {
        return mDataList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return mDataList.get(position).hashCode();
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

    @SuppressWarnings("unchecked")
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        int typeIndex = getItemViewType(position);
        if (convertView == null) {

            if (typeIndex == mHolderClasses.length + 2) {
                if (loadMorePowViewHolder == null) {
                    loadMorePowViewHolder = new LoadMorePowViewHolder();
                }
                convertView = loadMorePowViewHolder.getNewInstance();
                convertView.setTag(loadMorePowViewHolder);
                loadMorePowViewHolder.ensureAnimation(false);
            } else if (typeIndex == mHolderClasses.length + 1) {
                IncludeTypeEmpty empty = new IncludeTypeEmpty(parent);
                convertView = empty.mainItem;
                empty.mainItem.setTag(empty);

            } else if (typeIndex == mHolderClasses.length) {
                IncludeTypeError error = new IncludeTypeError(parent);
                convertView = error.mainItem;
                error.mainItem.setTag(error);
            } else {
                try {
                    PowViewHolder holder = mHolderClasses[typeIndex].getConstructor(Activity.class, ViewGroup.class).newInstance(mActivity, parent);
                    convertView = holder.mViewHolder.itemView;
                    convertView.setTag(holder);
                } catch (Exception e) {
                    e.printStackTrace();
                    throw new RuntimeException(e.getMessage());
                }
            }
        }

        Object object = convertView.getTag();
        if (typeIndex == mHolderClasses.length + 2) {
            LoadMorePowViewHolder holder = (LoadMorePowViewHolder) object;

            // todo 刷新
        } else if (typeIndex == mHolderClasses.length + 1) {
            IncludeTypeEmpty holder = (IncludeTypeEmpty) object;

        } else if (typeIndex == mHolderClasses.length) {
            IncludeTypeError holder = (IncludeTypeError) object;

        } else {
            PowViewHolder holder = (PowViewHolder) convertView.getTag();
            T itemData = mDataList.get(position);
            holder.mData = itemData;

            if (holder.mRegisterMainItemClickStatus == 0) {
                holder.registerAutoItemClick();
            }

            holder.loadData(this, itemData, position);
            return holder.mViewHolder.itemView;
        }
        return convertView;
    }

    private void ensureConfig() {
        if (!mLoadEnableShow) {
            mSpaceCount = 0;
            mLoadMoreCount = 0;
        } else {
            int dataSize = mDataList.size();
            mSpaceCount = dataSize % mFixedLoadMoreCount == 0 ? 0 : mFixedLoadMoreCount - dataSize % mFixedLoadMoreCount;
            mLoadMoreCount = mFixedLoadMoreCount;
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public int getItemViewType(int position) {
        if (position >= mDataList.size()) {

            ensureConfig();

            if (position >= mDataList.size() + mSpaceCount) {
                return mHolderInstances.length + 2;
            } else {
                return mHolderInstances.length + 1;
            }
        }

        for (int i = 0; i < mHolderInstances.length; i++) {                        //返回能载入次数据的ViewHolderClass下标
            T itemData = mDataList.get(position);
            if (itemData != null && mHolderGenericDataClass[i].isAssignableFrom(itemData.getClass()) && mHolderInstances[i].acceptData(itemData)) {
                return i;
            }
        }

        return mHolderInstances.length;                                              //错误页面数据
    }

    @Override
    public int getViewTypeCount() {
        return mHolderClasses.length + 3;
    }

    @Override
    public boolean isEmpty() {
        return false;
    }


    public void registerDataSetObserver(DataSetObserver observer) {
        mDataSetObservable.registerObserver(observer);
    }

    public void unregisterDataSetObserver(DataSetObserver observer) {
        mDataSetObservable.unregisterObserver(observer);
    }

    public void notifyDataSetChanged() {
        mDataSetObservable.notifyChanged();
    }

    public void notifyDataSetInvalidated() {
        mDataSetObservable.notifyInvalidated();
    }

    @Override
    public void setOnItemClickListener(OnItemClickListener clickListener) {

    }

    //---------------------------------------------------------------AdapterDelegate------------------------------------------------------------//


    @Override
    public void setHeadView(View view) {

    }

    @Override
    public void setFootView(View view) {

    }

    @Override
    public void removeHeadView() {

    }

    @Override
    public void removeFootView() {

    }

    @Override
    public void enableEmptyView(boolean show) {

    }

    @Override
    public void setEmptyView(View view) {

    }

    @Override
    public void setOnItemLongClickListener(OnItemLongClickListener<T> clickListener) {

    }

    // 获取数据列表
    @NonNull
    @Override
    public List<T> getDataList() {
        return mDataList;
    }

    // 获取数据数量
    @Override
    public int getDataCount() {
        return mDataList.size();
    }

    // 载入数据
    @Override
    public void loadData(List<T> dataList) {
        mDataList.clear();
        mDataList.addAll(dataList);
        notifyDataSetChanged();
    }

    // 添加数据
    @Override
    public void addData(int position, T data) {
        mDataList.add(position, data);
        notifyDataSetChanged();
    }

    // 添加数据
    @Override
    public void addData(int position, List<T> dataList) {
        mDataList.addAll(position, dataList);
        notifyDataSetChanged();
    }

    // 移除数据
    @Override
    public T removeData(int position) {
        T ret = mDataList.remove(position);
        notifyDataSetChanged();
        return ret;
    }

    @Override
    public void addDataAtLast(List<T> dataList) {
        addDataAtLast(dataList,null,0);
    }

    // 加入尾部数据     delayTime 延迟加入 让上拉加载显示时间加长
    @Override
    public void addDataAtLast(final List<T> dataList, final LoadedStatus status, int delayTime) {
        if (delayTime <= 0) {
            mDataList.addAll(mDataList.size(), dataList);
            notifyDataSetChanged();
            setLoadMoreStatus(status);
        } else {
            mActivity.getWindow().getDecorView().postDelayed(new Runnable() {
                @Override
                public void run() {
                    mDataList.addAll(mDataList.size(), dataList);
                    notifyDataSetChanged();
                    setLoadMoreStatus(status);
                }
            }, delayTime);
        }
    }

    // 删除数据
    @Override
    public void removeData(T data) {
        if (mDataList.contains(data)) {
            mDataList.remove(data);
            notifyDataSetChanged();
        }
    }

    // 清空数据
    @Override
    public void clearData() {
        if (mDataList.size() != 0) {
            mDataList.clear();
            notifyDataSetChanged();
        }
    }



    // 设置是否显示加载更多
    @Override
    public void enableLoadMore(boolean show) {
        if (this.mLoadEnableShow != show) {
            this.mLoadEnableShow = show;
            notifyDataSetChanged();
        }
    }

    @Override
    public void setLoadMoreStatus(LoadedStatus status) {
        if (status == null) return;

        switch (status) {
            case CONTINUE:
                mLoadStatus = LoadedStatus.CONTINUE;
                break;
            case NO_MORE:
                mLoadStatus = LoadedStatus.NO_MORE;
        }

        if (loadMorePowViewHolder != null) {
            loadMorePowViewHolder.ensureAnimation(false);
            for (View view : loadMorePowViewHolder.viewHolders) {
                view.invalidate();
            }
        }
    }

    // 设置显示更多监听
    @Override
    public void setOnLoadMoreListener(OnLoadMoreListener loadMoreListener) {
        this.mOnLoadMoreListener = loadMoreListener;
    }

    // 0 空白页面
    private class IncludeTypeEmpty {
        View mainItem;

        IncludeTypeEmpty(ViewGroup viewGroup) {
            mainItem = mActivity.getLayoutInflater().inflate(R.layout.powyin_scroll_multiple_adapter_empty, viewGroup, false);
        }
    }

    // 1 不合法信息展示类
    private class IncludeTypeError {
        View mainItem;
        TextView errorInfo;

        IncludeTypeError(ViewGroup viewGroup) {
            mainItem = mActivity.getLayoutInflater().inflate(R.layout.powyin_scroll_multiple_adapter_err, viewGroup, false);
            errorInfo = (TextView) mainItem.findViewById(R.id.powyin_scroll_err_text);
        }
    }

    // 2 加载更多iew
    private class LoadMorePowViewHolder {

        boolean mAttached = false;

        List<View> viewHolders = new ArrayList<>();

        ValueAnimator animator;
        Paint circlePaint;
        TextPaint textPaint;
        int canvasWei;
        int canvasHei;

        float canvasTextX;
        float canvasTextY;

        int ballCount = 10;
        float divide;

        LoadMorePowViewHolder() {
            circlePaint = new Paint();
            circlePaint.setColor(0x99000000);
            circlePaint.setStrokeWidth(4);
            textPaint = new TextPaint();
            textPaint.setColor(0x99000000);

            final float fontScale = mActivity.getResources().getDisplayMetrics().scaledDensity;
            int target = (int) (13 * fontScale + 0.5f);

            textPaint.setTextSize(target);
            textPaint.setAntiAlias(true);
            textPaint.setStrokeWidth(1);
        }


        View getNewInstance() {
            View holder = new LoadProgressBar(mActivity);
            viewHolders.add(holder);
            return holder;
        }


        private void ensureAnimation(boolean forceReStart) {

            if (!mAttached || mLoadStatus == LoadedStatus.NO_MORE) {
                if (animator != null) {
                    animator.cancel();
                    animator = null;
                }
                return;
            }

            if (forceReStart) {
                if (animator != null) {
                    animator.cancel();
                    animator = null;
                }
            } else {
                if (animator != null && animator.isRunning()) {
                    return;
                }
            }

            animator = ValueAnimator.ofFloat(0, 1);
            animator.setDuration(2000);
            animator.setRepeatCount(5);

            animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator valueAnimator) {
                    divide = 8 * ((System.currentTimeMillis() % 3000) - 1500) / 3000f;
                    for (View view : viewHolders) {
                        view.invalidate();
                    }
                }
            });

            animator.addListener(new Animator.AnimatorListener() {
                @Override
                public void onAnimationStart(Animator animation) {
                }

                @Override
                public void onAnimationEnd(Animator animation) {
                    if (animation == animator) {
                        ensureAnimation(true);
                    }
                }

                @Override
                public void onAnimationCancel(Animator animation) {
                }

                @Override
                public void onAnimationRepeat(Animator animation) {
                }
            });

            animator.start();
        }

        private float getSplit(float value) {
            int positive = value >= 0 ? 1 : -1;                                 //保存符号 判断正负
            value = Math.abs(value);
            if (value <= 1) return value * positive;
            return (float) Math.pow(value, 2) * positive;
        }

        class LoadProgressBar extends View {              //刷新视图
            int mIndex;

            public LoadProgressBar(Context context) {
                super(context);
            }

            @Override
            protected void onAttachedToWindow() {
                super.onAttachedToWindow();
                mAttached = true;
                ensureAnimation(false);
            }

            @Override
            protected void onDetachedFromWindow() {
                super.onDetachedFromWindow();
                mAttached = false;
                ensureAnimation(false);
            }

            @Override
            protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
                super.onMeasure(widthMeasureSpec, heightMeasureSpec);

                float scale = getContext().getResources().getDisplayMetrics().density;
                int target = (int) (40 * scale + 0.5f);

                setMeasuredDimension(getMeasuredWidth(), target);
            }


            @Override
            protected void onDraw(Canvas canvas) {
                super.onDraw(canvas);
                if (mLoadStatus == LoadedStatus.NO_MORE) {
                    int diff = mIndex * getWidth();
                    canvas.drawText(mLoadCompleteInfo, canvasTextX - diff, canvasTextY, textPaint);
                    canvas.drawLine(20 - diff, canvasHei / 2, canvasTextX - 20 - diff, canvasHei / 2, textPaint);
                    canvas.drawLine(canvasWei - canvasTextX + 20 - diff, canvasHei / 2, canvasWei - 20 - diff, canvasHei / 2, textPaint);

                } else {
                    for (int i = 0; i < ballCount; i++) {
                        float wei = 4 * (1f * i / ballCount - 0.5f) + divide;
                        wei = canvasWei / 2 + getSplit(wei) * canvasWei * 0.08f;
                        wei -= mIndex * getWidth();                               // good
                        canvas.drawCircle(wei, canvasHei / 2, 8, circlePaint);
                    }
                }
            }

            @Override
            protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
                super.onLayout(changed, left, top, right, bottom);
                canvasHei = getHeight();
                canvasWei = getWidth() * mLoadMoreCount;

                canvasTextX = canvasWei / 2 - textPaint.measureText(mLoadCompleteInfo) / 2;
                canvasTextY = canvasHei / 2 + textPaint.getTextSize() / 2.55f;

                ensureAnimation(false);
            }
        }

    }


}