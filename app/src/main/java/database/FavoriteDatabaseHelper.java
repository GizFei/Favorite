package database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class FavoriteDatabaseHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "FavoriteTable";
    private static final int VERSION = 1;

    public FavoriteDatabaseHelper(Context context){
        super(context, DATABASE_NAME, null, VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String sql = "create table " + FavoriteDatabaseSchema.TABLE_NAME + "("
                + FavoriteDatabaseSchema.Cols.UUID + ","
                + FavoriteDatabaseSchema.Cols.TITLE + ","
                + FavoriteDatabaseSchema.Cols.SOURCE + ","
                + FavoriteDatabaseSchema.Cols.DATE + " INTEGER,"
                + FavoriteDatabaseSchema.Cols.URL + ","
                + FavoriteDatabaseSchema.Cols.CONTENT + ","
                + FavoriteDatabaseSchema.Cols.IMG_PATH + ","
                + FavoriteDatabaseSchema.Cols.IS_STARRED + ","
                + FavoriteDatabaseSchema.Cols.ARCHIVE + ")";
        db.execSQL(sql);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) { }
}
