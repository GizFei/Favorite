package database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;

import datatool.FavoriteItem;

public class FavoriteItemLib {

    private static FavoriteItemLib sFavoriteItemLib;
    private SQLiteDatabase mDatabase;

    /**
     * 获得实例的静态方法
     * @param context 上下文
     * @return 数据库实例
     */
    public static FavoriteItemLib get(Context context){
        if(sFavoriteItemLib == null){
            sFavoriteItemLib = new FavoriteItemLib(context);
        }
        return sFavoriteItemLib;
    }

    private FavoriteItemLib(Context context){
        mDatabase = new FavoriteDatabaseHelper(context).getWritableDatabase();
    }

    public List<FavoriteItem> getFavoriteItemList(){
        return queryFavoriteItem(null, null);
    }

    public List<FavoriteItem> getFavoriteItemListByArchive(String archiveTitle){
        return queryFavoriteItem(FavoriteDatabaseSchema.Cols.ARCHIVE + "=?", new String[]{archiveTitle});
    }

    public List<FavoriteItem> getFavoriteItemListBySource(String source){
        return queryFavoriteItem(FavoriteDatabaseSchema.Cols.SOURCE + "=?", new String[]{source});
    }

    public List<FavoriteItem> getFavoriteItemListByDate(Date date){
        Calendar calendar = Calendar.getInstance(Locale.getDefault());
        calendar.setTime(date);
        long startDate = new GregorianCalendar(calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).getTime().getTime();
        long endDate = startDate + 3600000 * 24;
        return queryFavoriteItem(FavoriteDatabaseSchema.Cols.DATE + ">=? and " + FavoriteDatabaseSchema.Cols.DATE + "<?",
                new String[]{String.valueOf(startDate), String.valueOf(endDate)});
    }

    public List<FavoriteItem> getFavoriteItemListOfStarred(){
//        return queryFavoriteItem(FavoriteDatabaseSchema.Cols.IS_STARRED + "=?", new String[]{"1"});
        Cursor cursor = mDatabase.query(FavoriteDatabaseSchema.TABLE_NAME,
                null,
                null,
                null,
                null,
                null,
                null);
        List<FavoriteItem> itemList = new ArrayList<>();
        cursor.moveToFirst();
        while (!cursor.isAfterLast()){
            FavoriteItem item = getFavoriteItemByCursor(cursor);
            if(item.isStarred())
                itemList.add(item);
            cursor.moveToNext();
        }
        cursor.close();

        return itemList;
    }

    // 增
    public void addFavoriteItem(FavoriteItem item){
        mDatabase.insert(FavoriteDatabaseSchema.TABLE_NAME, null,
                getContentValues(item));
    }

    // 删
    public void removeFavoriteItem(String uuid){
        mDatabase.delete(FavoriteDatabaseSchema.TABLE_NAME, FavoriteDatabaseSchema.Cols.UUID + "=?",
                new String[]{uuid});
    }

    // 改
    public void updateFavoriteItem(FavoriteItem item){
        mDatabase.update(FavoriteDatabaseSchema.TABLE_NAME, getContentValues(item),
                FavoriteDatabaseSchema.Cols.UUID + "=?",
                new String[]{item.getUUID().toString()});
    }

    // 批量更改收藏夹名称
    public void patchUpdateFavoriteItemByArchive(String newArchive, String oldArchive){
        ContentValues values = new ContentValues();
        values.put(FavoriteDatabaseSchema.Cols.ARCHIVE, newArchive);
        mDatabase.update(FavoriteDatabaseSchema.TABLE_NAME, values,
                FavoriteDatabaseSchema.Cols.ARCHIVE + "=?",
                new String[]{oldArchive});
    }

    // 查
    public FavoriteItem findFavoriteItemById(String uuid){
        List<FavoriteItem> itemList = queryFavoriteItem(FavoriteDatabaseSchema.Cols.UUID + "=?",
                new String[]{uuid});
        if(itemList.size() == 1)
            return itemList.get(0);
        else
            return null;
    }

    public List<FavoriteItem> queryFavoriteItemByText(String text){
        String selection = String.format("%s like '%%%s%%' or %s like '%%%s%%'", FavoriteDatabaseSchema.Cols.TITLE,
                text, FavoriteDatabaseSchema.Cols.SOURCE, text);

        return queryFavoriteItem(selection, null);
    }

    /**
     * 根据查询数据库
     * @param selection 条件
     * @param selectionArgs 条件内容
     * @return 收藏项列表
     */
    private List<FavoriteItem> queryFavoriteItem(String selection, String[] selectionArgs){
        Cursor cursor = mDatabase.query(FavoriteDatabaseSchema.TABLE_NAME,
                null,
                selection,
                selectionArgs,
                null,
                null,
                null);
        List<FavoriteItem> itemList = new ArrayList<>();
        cursor.moveToFirst();
        while (!cursor.isAfterLast()){
            itemList.add(getFavoriteItemByCursor(cursor));
            cursor.moveToNext();
        }
        cursor.close();

        return itemList;
    }

    /**
     * 根据指针构建收藏项
     * @param cursor 指针
     * @return 收藏项
     */
    private FavoriteItem getFavoriteItemByCursor(Cursor cursor){
        String uuid = cursor.getString(cursor.getColumnIndex(FavoriteDatabaseSchema.Cols.UUID));
        FavoriteItem item = new FavoriteItem(uuid);

        item.setTitle(cursor.getString(cursor.getColumnIndex(FavoriteDatabaseSchema.Cols.TITLE)));
        item.setDate(new Date(cursor.getLong(cursor.getColumnIndex(FavoriteDatabaseSchema.Cols.DATE))));
        item.setSource(cursor.getString(cursor.getColumnIndex(FavoriteDatabaseSchema.Cols.SOURCE)));

        item.setUrl(cursor.getString(cursor.getColumnIndex(FavoriteDatabaseSchema.Cols.URL)));
        item.setContent(cursor.getString(cursor.getColumnIndex(FavoriteDatabaseSchema.Cols.CONTENT)));
        item.setImagePath(cursor.getString(cursor.getColumnIndex(FavoriteDatabaseSchema.Cols.IMG_PATH)));

        // Boolean在SQLite中是1，0
        item.setStarred(cursor.getInt(cursor.getColumnIndex(FavoriteDatabaseSchema.Cols.IS_STARRED)) == 1);
        item.setArchive(cursor.getString(cursor.getColumnIndex(FavoriteDatabaseSchema.Cols.ARCHIVE)));

        return item;
    }

    private ContentValues getContentValues(FavoriteItem item){
        ContentValues values = new ContentValues();

        values.put(FavoriteDatabaseSchema.Cols.UUID, item.getUUID().toString());
        values.put(FavoriteDatabaseSchema.Cols.TITLE, item.getTitle());
        values.put(FavoriteDatabaseSchema.Cols.DATE, item.getDate().getTime());
        values.put(FavoriteDatabaseSchema.Cols.SOURCE, item.getSource());

        values.put(FavoriteDatabaseSchema.Cols.URL, item.getUrl());
        values.put(FavoriteDatabaseSchema.Cols.CONTENT, item.getContent());
        values.put(FavoriteDatabaseSchema.Cols.IMG_PATH, item.getImagePath());

        values.put(FavoriteDatabaseSchema.Cols.IS_STARRED, item.isStarred());
        values.put(FavoriteDatabaseSchema.Cols.ARCHIVE, item.getArchive());

        return values;
    }
}
