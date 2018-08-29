package rocks.informatik.fileex.db;

import android.net.Uri;
import android.provider.BaseColumns;

public class FavPlacesContract {

    // to know which ContentProvider to access
    public static final String AUTHORITY = "rocks.informatik.fileex";

    // The base content URI = "content://" + <authority>
    public static final Uri BASE_CONTENT_URI = Uri.parse("content://" + AUTHORITY);

    // paths for accessing data in this contract
    public static final String PATH_FAVORITE_PLACES_LOCAL = "fav_places_local";

    // may need more than one table for different remote types
    // (may need fundamental different data? check out different remote types)
    // could also be solved with additional tables to hold extra data (with foreign key relation)
//    public static final String PATH_FAVORITE_PLACES_REMOTE = "fav_places_local";

    public static final class FavPlaceLocalEntry implements BaseColumns {

        public static final Uri CONTENT_URI =
                BASE_CONTENT_URI.buildUpon().appendPath(PATH_FAVORITE_PLACES_LOCAL).build();

        public static final String TABLE_NAME = "fav_places_local";

        // plus "_ID" column from BaseColumns interface
        public static final String COLUMN_NAME = "name";
        public static final String COLUMN_PATH = "path";
        public static final String COLUMN_ICON_RES_ID = "icon_res_id";

    }

}
