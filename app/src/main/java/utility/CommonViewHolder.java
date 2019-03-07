package utility;

import android.support.annotation.IdRes;
import android.support.v7.widget.RecyclerView;
import android.util.SparseArray;
import android.view.View;
import android.widget.TextView;

public class CommonViewHolder extends RecyclerView.ViewHolder {
    /**
     * 通过视图托管类，与CommonAdapter配合使用
     */
    private SparseArray<View> mViews;

    public CommonViewHolder(View v){
        super(v);
        mViews = new SparseArray<>();
    }

    /**
     * 获取视图
     * @param viewId 视图ID
     * @param <V> 视图转换类型
     * @return 视图
     */
    public <V extends View> V getView(int viewId){
        View view = mViews.get(viewId);
        if(view == null){
            view = itemView.findViewById(viewId);
            mViews.put(viewId, view);
        }
        return (V) view;
    }

    /**
     * 设置文字，这只是一个绑定数据方法的示例
     * @param viewId 视图ID
     * @param text 内容
     */
    public void setText(@IdRes int viewId, String text){
        ((TextView)getView(viewId)).setText(text);
    }
}
