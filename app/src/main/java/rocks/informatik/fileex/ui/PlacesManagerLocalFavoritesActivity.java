package rocks.informatik.fileex.ui;

import android.app.ActivityOptions;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.View;
import android.widget.ImageView;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import rocks.informatik.fileex.R;
import rocks.informatik.fileex.adapters.FavPlacesLocalAdapter;
import rocks.informatik.fileex.data.FavoritePlace;
import rocks.informatik.fileex.db.FavoritesManager;
import rocks.informatik.fileex.tools.AdmobHelper;
import timber.log.Timber;

public class PlacesManagerLocalFavoritesActivity extends AppCompatActivity
        implements FavPlacesLocalAdapter.ListItemClickListener {

    // TODO: type seems redundant -> if fav is inside intent: update, otherwise new
    public static final String INTENT_EXTRA_ACTION_TYPE = "action-type";
    public static final int ACTION_TYPE_UPDATE_FAV = 0;
    public static final int ACTION_TYPE_NEW_FAV = 1;

    @BindView(R.id.rv_places_manager_local_favorites)
    RecyclerView rvLocalFavoritePlaces;
    @BindView(R.id.ad_view_places_manager_local_bottom)
    AdView mAdViewBottom;
    @BindView(R.id.fab_add_new_local_favorite)
    FloatingActionButton fabAddNewFavorite;


    private LinearLayoutManager layoutManager;

    private FavPlacesLocalAdapter adapterLocalFavoritePlaces;

    public static final int LOADER_ID_LOCAL_FAVORITES = 1;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_places_manager_local_favorites);
        ButterKnife.bind(this);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle(R.string.activity_title_fav_places_manager);

        AdRequest adRequest = AdmobHelper.createAdRequest(true);
        mAdViewBottom.loadAd(adRequest);

        // local favorites
        adapterLocalFavoritePlaces = new FavPlacesLocalAdapter(this, null);
        getSupportLoaderManager().initLoader(LOADER_ID_LOCAL_FAVORITES, null, loaderListenerLocalFavorites);
        layoutManager = new LinearLayoutManager(this);
        rvLocalFavoritePlaces.setLayoutManager(layoutManager);
        rvLocalFavoritePlaces.setAdapter(adapterLocalFavoritePlaces);


        // add swipe to delete
        new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {

            @Override
            public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
                return false;
            }

            // Called when a user swipes left or right on a ViewHolder
            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int swipeDir) {
                int id = (int) viewHolder.itemView.getTag(R.id.tag_local_favorite_id_in_DB);
                Timber.d("id to be deleted: " + id);
                int rowsDeleted = FavoritesManager.deleteLocalFavorite(PlacesManagerLocalFavoritesActivity.this, id);
                if (rowsDeleted > 0) {
                    getSupportLoaderManager().restartLoader(LOADER_ID_LOCAL_FAVORITES, null, loaderListenerLocalFavorites);
                }
            }
        }).attachToRecyclerView(rvLocalFavoritePlaces);

    }


    @Override
    protected void onResume() {
        super.onResume();
        getSupportLoaderManager().restartLoader(LOADER_ID_LOCAL_FAVORITES, null, loaderListenerLocalFavorites);
    }

    @Override
    public void onListItemClicked(int clickedItemIndex, String tag) {

        Intent intent = new Intent(this, EditLocalFavoritePlaceActivity.class);
        intent.putExtra(INTENT_EXTRA_ACTION_TYPE,
                ACTION_TYPE_UPDATE_FAV);

        FavoritePlace placeToEdit = adapterLocalFavoritePlaces.getFavoriteAtPosition(clickedItemIndex);
        intent.putExtra(EditLocalFavoritePlaceActivity.INTENT_EXTRA_FAVORITE_TO_EDIT, placeToEdit);

        // shared element transition from icon in recycler to edit-activity's pre-selected icon
        RecyclerView.ViewHolder holder = rvLocalFavoritePlaces.findViewHolderForAdapterPosition(clickedItemIndex);
        ImageView iconViewInRecycler = holder.itemView.findViewById(R.id.iv_rv_item_favority_entry_icon);
        // shared view may be null if recycled!
        if (iconViewInRecycler != null
                && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            ActivityOptions options = ActivityOptions
                    .makeSceneTransitionAnimation(this, iconViewInRecycler, "robot");
            startActivity(intent, options.toBundle());
        } else {
            // no transition if shared view not found
            startActivity(intent);
        }


    }

    public void onFabNewLocalFavoriteClicked(View view) {
        Intent intent = new Intent(this, EditLocalFavoritePlaceActivity.class);
        intent.putExtra(INTENT_EXTRA_ACTION_TYPE,
                ACTION_TYPE_NEW_FAV);
        startActivity(intent);
    }


    private LoaderManager.LoaderCallbacks<List<FavoritePlace>> loaderListenerLocalFavorites =
            new LoaderManager.LoaderCallbacks<List<FavoritePlace>>() {

                @NonNull
                @Override
                public Loader<List<FavoritePlace>> onCreateLoader(int id, @Nullable Bundle args) {

                    return new AsyncTaskLoader<List<FavoritePlace>>(PlacesManagerLocalFavoritesActivity.this) {

                        @Override
                        protected void onStartLoading() {
                            forceLoad();
                        }

                        @Nullable
                        @Override
                        public List<FavoritePlace> loadInBackground() {
                            List<FavoritePlace> placesLocalFavorites = FavoritesManager
                                    .getLocalFavoritesFromDb(PlacesManagerLocalFavoritesActivity.this);
                            return placesLocalFavorites;
                        }

                        @Override
                        public void deliverResult(List<FavoritePlace> places) {
                            super.deliverResult(places);
                        }

                    };

                }

                @Override
                public void onLoadFinished(@NonNull Loader<List<FavoritePlace>> loader,
                                           List<FavoritePlace> placesLocalFavorites) {
                    adapterLocalFavoritePlaces.swapFavorites(placesLocalFavorites);
                }

                @Override
                public void onLoaderReset(@NonNull Loader<List<FavoritePlace>> loader) {
                    Timber.d( "Loader: onLoaderReset() called");
                }
            };


}
