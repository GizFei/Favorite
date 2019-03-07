package utility;

import android.content.Context;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import java.util.List;

public abstract class CommonAdapter<T> extends RecyclerView.Adapter<CommonViewHolder> {
    /**
     * 通过列表适配器，与CommonViewHolder配合使用
     * 实现抽象方法bindData来绑定数据
     */
    private List<T> mData;
    private int mLayout;
    private LayoutInflater mInflater;
    private Context mContext;

    /**
     * T是数据类型
     * @param data 数据列表
     * @param holderLayout 布局资源
     */
    public CommonAdapter(Context context, List<T> data, @LayoutRes int holderLayout){
        mData = data;
        mLayout = holderLayout;
        mInflater = LayoutInflater.from(context);
        mContext = context;
    }

    @NonNull
    @Override
    public CommonViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        return new CommonViewHolder(mInflater.inflate(mLayout, viewGroup, false));
    }

    @Override
    public void onBindViewHolder(@NonNull CommonViewHolder commonViewHolder, int i) {
        // 使用抽象方法绑定数据
        bindData(commonViewHolder, mData.get(i), i);
    }

    @Override
    public int getItemCount() {
        return mData.size();
    }

    /**
     * 更新数据
     * @param newData 新数据
     */
    public void updateData(List<T> newData){
        mData = newData;
        notifyDataSetChanged();
    }

    /**
     * 更新某项
     * @param pos 位置
     * @param newItem 新项
     */
    public void updateItem(int pos, T newItem){
        if(pos > mData.size() || pos < 0)
            return;
        mData.set(pos, newItem);
        notifyItemChanged(pos);
    }

    /**
     * 添加新项
     * @param newItem 新项
     */
    public void addItem(T newItem){
        mData.add(newItem);
        notifyItemInserted(mData.size() - 1);
    }

    /**
     * 在某位置添加新项
     * @param pos 位置
     * @param newItem 新项
     */
    public void addItem(int pos, T newItem){
        mData.add(pos, newItem);
        notifyItemInserted(pos);
    }

    /**
     * 删除某项
     * @param pos 位置
     */
    public void deleteItem(int pos){
        mData.remove(pos);
        notifyItemRemoved(pos);
    }

    /**
     * 用于绑定数据的抽象方法
     * @param viewHolder 视图托管类
     * @param data 数据
     * @param pos 位置
     * 绑定示例：
     * ((TextView)viewHolder.getView(R.id.viewId).setText(data);
     */
    public abstract void bindData(CommonViewHolder viewHolder, T data, int pos);

    /**
     * 获取某位置的数据
     * @param pos 位置
     * @return item
     */
    public T getItem(int pos){
        return mData.get(pos);
    }
}
