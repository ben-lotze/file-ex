package rocks.informatik.fileex.db;

import android.Manifest;
import android.content.ContentValues;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;

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


    public static List<FavoritePlace> getLocalPhoneFolders(final Context context) {

        // TODO: choose names/icons depending on which folders were found




        AsyncTask<Void, Void, List<FavoritePlace>> task = new AsyncTask<Void, Void, List<FavoritePlace>>() {
            @Override
            protected List<FavoritePlace> doInBackground(Void... voids) {

                List<FavoritePlace> places = new ArrayList<>();

                String rootPath = "/";
                String rootName = context.getString(R.string.fav_name_root);
                FavoritePlace placeRoot = new FavoritePlace(-1, rootName, rootPath, R.drawable.ic_folders_black_24dp);
                long rootFreeSpace = FileHelpers.getFreeSpaceForPath(rootPath);
                placeRoot.setFreeSpace(rootFreeSpace);
                long rootTotalSize = FileHelpers.getTotalSizeForPath(rootPath);
                placeRoot.setTotalSize(rootTotalSize);
                places.add(placeRoot);


                String pathExternal = Environment.getExternalStorageDirectory().getAbsolutePath();
                Timber.d("path external: " + pathExternal);
                String externalName = context.getString(R.string.fav_name_external_storage);
                FavoritePlace placeExternal = new FavoritePlace(-1, externalName, pathExternal,
                        R.drawable.ic_phone_android_black_24dp);
                long externalFreeSpace = FileHelpers.getFreeSpaceForPath(pathExternal);
                placeExternal.setFreeSpace(externalFreeSpace);
                long externalTotalSize = FileHelpers.getTotalSizeForPath(pathExternal);
                placeExternal.setTotalSize(externalTotalSize);
                places.add(placeExternal);


                /* more paths that may be worth checking:
                 * /sdcard
                 * /mnt/sdcard
                 * /extSdCard
                 */


                if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE)
                        != PackageManager.PERMISSION_GRANTED) {
                    Timber.d("FavManager: PERMISSION DENIED (right before checking contents)");
                } else {
                    Timber.d("FavManager: PERMISSION OK (right before checking contents)");
                }

                // TODO: check if there is more than one (if yes: add their respective name to the item)
                File folderStorage = new File("/storage/");
                File[] subfoldersInStorage = folderStorage.listFiles();
                if (subfoldersInStorage != null && subfoldersInStorage.length > 0) {
                    int subfolderCount = subfoldersInStorage.length;
                    for (File subfolder : subfoldersInStorage) {

                        Timber.d("checking storage subfolder: " + subfolder.getAbsolutePath());

                        // try to read contents -> if not possible: skip current folder
                        File[] contents = subfolder.listFiles();
                        if (contents == null) {
                            Timber.d("-> contents: NULL");
                            continue;
                        }
                        Timber.d("-> contents: " + Arrays.asList(contents));

                        String currentPath = subfolder.getAbsolutePath();
                        String nameSdCard = context.getString((R.string.fav_name_sd_card));
                        String currentFolderName = subfolder.getName();
                        // TODO: length is >1 in most cases, folders are just not all that interesting...
                        // count folders that CAN be handled
                        // or store in DB and offer possibility to edit names
                        if (subfolderCount > 1) {
                            nameSdCard = String.format("%s (%s)", nameSdCard, currentFolderName);
                        }

                        FavoritePlace place = new FavoritePlace(-1, nameSdCard, currentPath, R.drawable.ic_sd_card_black_24dp);
                        long currentPlaceFreeSpace = FileHelpers.getFreeSpaceForPath(currentPath);
                        place.setFreeSpace(currentPlaceFreeSpace);
                        long currentPlaceTotalSize = FileHelpers.getTotalSizeForPath(currentPath);
                        place.setTotalSize(currentPlaceTotalSize);

                        places.add(place);
                    }
                }

                return places;
            }
        }.execute();


        try {
            List<FavoritePlace> places = task.get();
            Timber.d("found phone places:");
            if (places != null) {
                for (FavoritePlace place : places) {
                    if (place == null) {
                        continue;
                    }
                    Timber.d("path: " + place.getPathStr());
                }
            }

            return places;

        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }


//




        return new ArrayList<>();

    }
}
