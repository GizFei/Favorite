package viewtool;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.giz.favorite.R;
import com.giz.favorite.RichContentActivity;

import java.util.List;
import java.util.UUID;

import database.FavoriteItemLib;
import datatool.FavoriteItem;
import datatool.FavoriteTool;
import datatool.SourceApp;
import utility.CommonUtil;
import utility.RoundedImageView;

public class FavoriteItemAdapter extends RecyclerView.Adapter<FavoriteItemAdapter.FavoriteItemHolder> {
    private static final String TAG = "FavoriteItemAdapter";

    public static final int TYPE_DATE = 1;
    public static final int TYPE_ITEM = 2;

    private Context mContext;
    private LayoutInflater mInflater;
    private List<Pair<Integer, FavoriteItem>> mFavoriteItemList;
    private OnItemSelectedListener mSelectedListener;

    public FavoriteItemAdapter(Context context, List<Pair<Integer, FavoriteItem>> itemList){
        mContext = context;
        mInflater = LayoutInflater.from(mContext);
        mFavoriteItemList = itemList;
    }

    @NonNull
    @Override
    public FavoriteItemHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int type) {
        return new FavoriteItemHolder(mInflater.inflate(
                type == TYPE_DATE ? R.layout.item_favorite_date : R.layout.item_favorite
                , viewGroup, false));
    }

    @Override
    public void onBindViewHolder(@NonNull FavoriteItemHolder favoriteItemHolder, int pos) {
//        favoriteItemHolder.setIsRecyclable(false);

        favoriteItemHolder.bind(mFavoriteItemList.get(pos).second, mFavoriteItemList.get(pos).first);
    }

    @Override
    public int getItemViewType(int position) {
        return mFavoriteItemList.get(position).first;
    }

    @Override
    public int getItemCount() {
        return mFavoriteItemList.size();
    }

    @Override
    public void onViewRecycled(@NonNull FavoriteItemHolder holder) {
        if(holder.itemView.getTranslationX() > 0f)
            holder.itemView.setTranslationX(0);
        super.onViewRecycled(holder);
    }

    public void setFavoriteItemList(List<Pair<Integer, FavoriteItem>> favoriteItemList){
        mFavoriteItemList = favoriteItemList;
        notifyDataSetChanged();
    }

    public void removeFavoriteItem(int pos){
        mFavoriteItemList.remove(pos);
        notifyItemRemoved(pos);
    }

    public void addFavoriteItem(int pos, Pair<Integer, FavoriteItem> pair){
        mFavoriteItemList.add(pos, pair);
        notifyItemInserted(pos);
    }

    public void updateFavoriteItem(int pos, FavoriteItem item){
        mFavoriteItemList.set(pos, new Pair<>(TYPE_ITEM, item));
        notifyItemChanged(pos);
    }

    /**
     * 当ViewHolder长按后，进入选中状态时，执行的外部事件
     */
    public void setOnItemSelectedListener(OnItemSelectedListener listener){
        mSelectedListener = listener;
    }

    public interface OnItemSelectedListener{
        void onSelected(FavoriteItem item, int pos);
    }

    public class FavoriteItemHolder extends RecyclerView.ViewHolder{

        private FavoriteItemHolder(View view){
            super(view);
        }

        private void bind(final FavoriteItem item, int type){
            if(type == TYPE_DATE){
                ((TextView)itemView.findViewById(R.id.item_favorite_date_tv))
                        .setText(CommonUtil.isToday(item.getDate()) ? mContext.getResources().getString(R.string.item_today) : CommonUtil.getDateDescription(item.getDate()));
            }else{
                ((TextView)itemView.findViewById(R.id.item_favorite_title)).setText(item.getTitle());
                ((TextView)itemView.findViewById(R.id.item_favorite_date))
                        .setText(CommonUtil.getDateBriefDescription(item.getDate()));
                ((TextView)itemView.findViewById(R.id.item_favorite_source)).setText(SourceApp.getSourceText(item.getSource()));

                TextView archiveTv = itemView.findViewById(R.id.item_favorite_archive);
                if(item.getArchive() != null){
                    archiveTv.setVisibility(View.VISIBLE);
                    archiveTv.setText(item.getArchive());
                }
                else
                    archiveTv.setVisibility(View.GONE);

                Drawable drawable = CommonUtil.getAppIcon(mContext, SourceApp.getAppPackageName(item.getSource()));
                ((RoundedImageView)itemView.findViewById(R.id.item_favorite_icon))
                        .setImageDrawable(drawable == null ? mContext.getResources().getDrawable(R.mipmap.ic_launcher, mContext.getTheme()) : drawable);
                Bitmap itemImg = FavoriteTool.getItemImage(item.getImagePath());
                if(itemImg != null){
                    ((ImageView)itemView.findViewById(R.id.item_favorite_image)).setImageBitmap(itemImg);
                    itemView.findViewById(R.id.item_favorite_image).setVisibility(View.VISIBLE);
                }
                else
                    itemView.findViewById(R.id.item_favorite_image).setVisibility(View.GONE);
                // 是否星标
                itemView.findViewById(R.id.item_favorite_star).setVisibility(item.isStarred() ? View.VISIBLE : View.GONE);
            }

            if(type == TYPE_ITEM){
                itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent intent = RichContentActivity.newIntent(mContext, item.getUUID().toString());
                        mContext.startActivity(intent);
                    }
                });

                itemView.setOnLongClickListener(new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View v) {
                        if(mSelectedListener != null){
                            Log.d(TAG, "onLongClick: layout position:" + getLayoutPosition());
                            mSelectedListener.onSelected(item, getAdapterPosition());
                        }
                        return true;
                    }
                });
            }
//            itemView.setEnabled(false); 取消点击事件，clickable不起作用
        }
    }
}
