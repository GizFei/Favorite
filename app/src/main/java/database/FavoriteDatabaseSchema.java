package database;

public class FavoriteDatabaseSchema {
    public static final String TABLE_NAME = "FavoriteTable";

    public static final class Cols{
        public static final String UUID = "uuid";
        public static final String SOURCE = "source";
        public static final String DATE = "date";
        public static final String TITLE = "title";
        public static final String URL = "url";
        public static final String CONTENT = "content";
        public static final String IMG_PATH = "imgPath";

        public static final String IS_STARRED = "isStarred";
        public static final String ARCHIVE = "archive";
    }
}
