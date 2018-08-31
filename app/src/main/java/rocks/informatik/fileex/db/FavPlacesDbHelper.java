package rocks.informatik.fileex.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.os.Environment;

import rocks.informatik.fileex.R;
import timber.log.Timber;

public class FavPlacesDbHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "favPlaces.db";
    // increment after each change of the database schema
    private static final int VERSION = 8;

    private Context mContext;

    public FavPlacesDbHelper(Context context) {
        super(context, DATABASE_NAME, null, VERSION);
        this.mContext = context;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        final String CREATE_TABLE = "CREATE TABLE " + FavPlacesContract.FavPlaceLocalEntry.TABLE_NAME + " ("
                + FavPlacesContract.FavPlaceLocalEntry._ID + " INTEGER PRIMARY KEY, "
                + FavPlacesContract.FavPlaceLocalEntry.COLUMN_NAME + " TEXT NOT NULL, "
                + FavPlacesContract.FavPlaceLocalEntry.COLUMN_PATH + " TEXT NOT NULL, "
                + FavPlacesContract.FavPlaceLocalEntry.COLUMN_ICON_RES_ID + " INTEGER NOT NULL"
                +");";
        db.execSQL(CREATE_TABLE);


        // ---- create default entries ----
        // defaults can be modified by user, but are recreated on every DB creation
        Uri uri = FavPlacesContract.FavPlaceLocalEntry.CONTENT_URI;
        // downloads
        String downloadPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath();
        ContentValues valuesDownload = new ContentValues();
        valuesDownload.put(FavPlacesContract.FavPlaceLocalEntry.COLUMN_NAME, mContext.getString(R.string.fav_name_downloads));
        valuesDownload.put(FavPlacesContract.FavPlaceLocalEntry.COLUMN_PATH, downloadPath);
        valuesDownload.put(FavPlacesContract.FavPlaceLocalEntry.COLUMN_ICON_RES_ID, R.drawable.ic_downloads_black_24dp);
        db.insert(FavPlacesContract.FavPlaceLocalEntry.TABLE_NAME, null, valuesDownload);
        //DCIM
        String dcimPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).getAbsolutePath();
        ContentValues valuesDcim = new ContentValues();
        valuesDcim.put(FavPlacesContract.FavPlaceLocalEntry.COLUMN_NAME, mContext.getString(R.string.fav_name_dcim_camera));
        valuesDcim.put(FavPlacesContract.FavPlaceLocalEntry.COLUMN_PATH, dcimPath);
        valuesDcim.put(FavPlacesContract.FavPlaceLocalEntry.COLUMN_ICON_RES_ID, R.drawable.ic_menu_camera);
        db.insert(FavPlacesContract.FavPlaceLocalEntry.TABLE_NAME, null, valuesDcim);

        Timber.d("tables created, inserted defaults, version="+VERSION);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // TODO: stop destroying users data before Playstore release
        db.execSQL("DROP TABLE IF EXISTS " + FavPlacesContract.FavPlaceLocalEntry.TABLE_NAME);
        onCreate(db);
    }
}
