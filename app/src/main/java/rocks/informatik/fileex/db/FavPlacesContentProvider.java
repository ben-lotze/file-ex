package rocks.informatik.fileex.db;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import timber.log.Timber;

public class FavPlacesContentProvider extends ContentProvider {

    public static final int FAV_PLACES_LOCAL = 100;
    public static final int FAV_PLACE_LOCAL_WITH_ID = 101;
    private static final UriMatcher sUriMatcher = buildUriMatcher();


    private FavPlacesDbHelper mDbHelper;


    @Override
    public boolean onCreate() {
        Context context = getContext();
        mDbHelper = new FavPlacesDbHelper(context);
        return false;
    }

    public static UriMatcher buildUriMatcher() {
        UriMatcher uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        uriMatcher.addURI(FavPlacesContract.AUTHORITY,
                FavPlacesContract.PATH_FAVORITE_PLACES_LOCAL, FAV_PLACES_LOCAL);
        uriMatcher.addURI(FavPlacesContract.AUTHORITY,
                FavPlacesContract.PATH_FAVORITE_PLACES_LOCAL + "/#", FAV_PLACE_LOCAL_WITH_ID);

        return uriMatcher;
    }

    @Nullable
    @Override
    public Cursor query(@NonNull Uri uri, @Nullable String[] projection, @Nullable String selection,
                        @Nullable String[] selectionArgs, @Nullable String sortOrder) {
        final SQLiteDatabase db = mDbHelper.getReadableDatabase();
        int matchId = sUriMatcher.match(uri);
        Cursor retCursor = null;
        switch (matchId) {
            case FAV_PLACES_LOCAL:
                retCursor = db.query(FavPlacesContract.FavPlaceLocalEntry.TABLE_NAME,
                        projection,
                        selection,
                        selectionArgs,
                        null,
                        null,
                        sortOrder);
                break;
            case FAV_PLACE_LOCAL_WITH_ID:
                String favId = uri.getLastPathSegment();
                retCursor = db.query(FavPlacesContract.FavPlaceLocalEntry.TABLE_NAME,
                        projection,
                        "_id=" + favId,
                        null,
                        null,
                        null,
                        sortOrder);
                break;
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
        // set notification URI on cursor
        retCursor.setNotificationUri(getContext().getContentResolver(), uri);
        return retCursor;
    }


    @Nullable
    @Override
    public Uri insert(@NonNull Uri uri, @Nullable ContentValues values) {
        Uri returnUri;
        final SQLiteDatabase db = mDbHelper.getWritableDatabase();

        int matchId = sUriMatcher.match(uri);
        switch (matchId) {
            case FAV_PLACES_LOCAL:
                long id = db.insert(FavPlacesContract.FavPlaceLocalEntry.TABLE_NAME, null, values);
                if (id > 0) {
                    returnUri = ContentUris.withAppendedId(FavPlacesContract.FavPlaceLocalEntry.CONTENT_URI, id);
                } else {
                    throw new android.database.SQLException("Failed to insert row into " + uri);
                }
                break;
            default:
                throw new UnsupportedOperationException("Unknown uri on insert: " + uri);
        }

        // Notify the resolver if the uri has been changed, and return the newly inserted URI
        getContext().getContentResolver().notifyChange(uri, null);
        return returnUri;
    }

    @Override
    public int delete(@NonNull Uri uri, @Nullable String selection, @Nullable String[] selectionArgs) {
        // Get access to the database and write URI matching code to recognize a single item
        final SQLiteDatabase db = mDbHelper.getWritableDatabase();

        int match = sUriMatcher.match(uri);
        // Keep track of the number of deleted tasks
        int numberOfEntriesDeleted = 0; // starts as 0


        switch (match) {
            case FAV_PLACE_LOCAL_WITH_ID:
                // Get the task ID from the URI path
                String id = uri.getPathSegments().get(1);
                // Use selections/selectionArgs to filter for this ID
                if (id != null) {
                    numberOfEntriesDeleted = db.delete(FavPlacesContract.FavPlaceLocalEntry.TABLE_NAME,
                            FavPlacesContract.FavPlaceLocalEntry._ID + "=?", new String[]{id});
                }
                break;
            default:
                Timber.d("delete failed, URI unknown or maybe tried to delete all entries at once " +
                                "(not allowed currently, may change or not)");
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }

        // Notify the resolver of a change and return the number of items deleted
        if (numberOfEntriesDeleted != 0) {
            // A task was deleted, set notification
            Timber.d("delete successful");
            getContext().getContentResolver().notifyChange(uri, null);
        } else {
            Timber.d("delete failed");
        }

        // Return the number of tasks deleted
        return numberOfEntriesDeleted;
    }

    @Override
    public int update(@NonNull Uri uri, @Nullable ContentValues values, @Nullable String selection, @Nullable String[] selectionArgs) {
        final SQLiteDatabase db = mDbHelper.getWritableDatabase();

        int matchId = sUriMatcher.match(uri);
        switch (matchId) {
            case FAV_PLACE_LOCAL_WITH_ID:
                String id = uri.getPathSegments().get(1);
                int rowsUpdated = db.update(FavPlacesContract.FavPlaceLocalEntry.TABLE_NAME,
                        values,
                        FavPlacesContract.FavPlaceLocalEntry._ID + "=" + id,
                        null
                        );
                Timber.d(rowsUpdated + " rows UPDATED");
                return rowsUpdated;
            default:
                // all other cases (no id specified in URI) throw Exception
                throw new UnsupportedOperationException("updating fav places not implemented yet");
        }

    }


    @Nullable
    @Override
    public String getType(@NonNull Uri uri) {
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
