package rocks.informatik.fileex.db;

import android.Manifest;
import android.content.ContentValues;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.support.v4.content.ContextCompat;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;

import rocks.informatik.fileex.R;
import rocks.informatik.fileex.data.FavoritePlace;
import rocks.informatik.fileex.data.FavoritePlaceLocal;
import rocks.informatik.fileex.tools.FileHelpers;
import timber.log.Timber;

public class FavoritesManager {

    public void updateLocalFavorite(Context context, FavoritePlaceLocal place) {
        ContentValues values = new ContentValues();
        values.put(FavPlacesContract.FavPlaceLocalEntry.COLUMN_NAME, place.getName());
        values.put(FavPlacesContract.FavPlaceLocalEntry.COLUMN_PATH, place.getPathStr());
        values.put(FavPlacesContract.FavPlaceLocalEntry.COLUMN_ICON_RES_ID, place.getDrawableResId());
        // build correct URI (including id to update)
        Uri uri = FavPlacesContract.FavPlaceLocalEntry.CONTENT_URI.buildUpon()
                .appendPath(String.valueOf(place.getId())).build();
        context.getContentResolver().update(uri, values,
                null, null);
    }


    public static int deleteLocalFavorite(Context context, int id) {
        Uri uri = FavPlacesContract.FavPlaceLocalEntry.CONTENT_URI.buildUpon()
                .appendPath(String.valueOf(id)).build();
        int rowsDeleted = context.getContentResolver().delete(uri, null, null);
        Timber.d(rowsDeleted + " favs were deleted");
        return rowsDeleted;
    }

    // may be needed later
//    public void deleteLocalFavorite(FavoritePlaceLocal place) {
//        // build correct URI (including id to update)
//        Uri uri = FavPlacesContract.FavPlaceLocalEntry.CONTENT_URI.buildUpon()
//                .appendPath(String.valueOf(place.getId())).build();
//
//    }


    public static List<FavoritePlace> getLocalFavoritesFromDb(final Context context) {

        AsyncTask<Void, Void, List<FavoritePlace>> task = new AsyncTask<Void, Void, List<FavoritePlace>>() {
            @Override
            protected List<FavoritePlace> doInBackground(Void... voids) {
                List<FavoritePlace> places = new ArrayList<>();
                Uri uri = FavPlacesContract.FavPlaceLocalEntry.CONTENT_URI;
                Cursor cursor = context.getContentResolver().query(uri, null, null, null, null);
                if (cursor != null && cursor.getCount() > 0) {
                    while (cursor.moveToNext()) {
                        int id = cursor.getInt(cursor.getColumnIndex(FavPlacesContract.FavPlaceLocalEntry._ID));
                        String name = cursor.getString(cursor.getColumnIndex(FavPlacesContract.FavPlaceLocalEntry.COLUMN_NAME));
                        String path = cursor.getString(cursor.getColumnIndex(FavPlacesContract.FavPlaceLocalEntry.COLUMN_PATH));
                        int iconResId = cursor.getInt(cursor.getColumnIndex(FavPlacesContract.FavPlaceLocalEntry.COLUMN_ICON_RES_ID));
                        FavoritePlace fav = new FavoritePlaceLocal(id, name, path, iconResId);
                        places.add(fav);
                    }
                }
                return places;
            }
        }.execute();

        // initialize empty list, do not return null in case of errors
        List<FavoritePlace> places = new ArrayList<>();
        try {
            places = task.get();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
        return places;
    }


}
