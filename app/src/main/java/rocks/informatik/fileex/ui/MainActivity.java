package rocks.informatik.fileex.ui;

import android.Manifest;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.Loader;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;

import com.google.android.gms.ads.MobileAds;
import com.google.firebase.analytics.FirebaseAnalytics;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import rocks.informatik.fileex.BuildConfig;
import rocks.informatik.fileex.R;
import rocks.informatik.fileex.adapters.FavPlacesLocalAdapter;
import rocks.informatik.fileex.data.FavoritePlace;
import rocks.informatik.fileex.db.FavoritesManager;
import rocks.informatik.fileex.tools.FileHelpers;
import timber.log.Timber;

public class MainActivity extends AppCompatActivity implements
        FileBrowserFragment.OnFragmentInteractionListener,
        FavPlacesLocalAdapter.ListItemClickListener {

    private ActionBarDrawerToggle toggle;
    private static final String TAG = MainActivity.class.getSimpleName();

    @BindView(R.id.toolbar)
    Toolbar toolbar;
    @BindView(R.id.drawer_layout)
    DrawerLayout drawer;
    @BindView(R.id.rv_nav_places_local_phone_storage)
    RecyclerView rvLocalPhoneStorages;
    @BindView(R.id.rv_nav_places_local_favorites)
    RecyclerView rvLocalFavoritePlaces;

    private FavPlacesLocalAdapter adapterLocalPhoneStorages;
    private FavPlacesLocalAdapter adapterLocalFavoritePlaces;

    private FragmentBackButtonListener fragmentForBackButtonPress;

    private FirebaseAnalytics mFirebaseAnalytics;

    // loader only used for data from SQLite (local favorites), phone storage created on demand in AsyncTask (not from database)
    public static final int LOADER_ID_LOCAL_FAVORITES = 0;
    private static final int PERMISSION_REQUEST_CODE_READ_WRITE_STORAGE = 666;


    private String pathToOpen;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        setSupportActionBar(toolbar);


        // activate timber logging
        if (BuildConfig.DEBUG) {
            Timber.plant(new Timber.DebugTree());
        }
        Timber.d("onCreate() called");


        toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        // remaining work can be continued while permission popup is visible
        handlePermissions();


        ImageButton btnEditLocalPlaces = findViewById(R.id.btn_local_add);
        btnEditLocalPlaces.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, PlacesManagerLocalFavoritesActivity.class);
                startActivity(intent);
            }
        });


        // always using same instance of Firebase
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(getApplicationContext());
        MobileAds.initialize(this, getString(R.string.banner_ad_app_id));

        // local favorites (stored in database, fetched via loader from ContentProvider)
        // no permissions necessary
        LinearLayoutManager layoutManagerLocalFavs = new LinearLayoutManager(this);
        rvLocalFavoritePlaces.setLayoutManager(layoutManagerLocalFavs);
        adapterLocalFavoritePlaces = new FavPlacesLocalAdapter(
                this, FavPlacesLocalAdapter.TAG_LOCAL_FAVORITES);
        rvLocalFavoritePlaces.setAdapter(adapterLocalFavoritePlaces);
        getSupportLoaderManager().initLoader(LOADER_ID_LOCAL_FAVORITES, null, loaderListenerLocalFavorites);


        Intent intent = getIntent();
        if (intent != null) {
            pathToOpen = intent.getStringExtra(AppWidget.INTENT_EXTRA_PATH_TO_OPEN);
            Timber.d("intent!=null, MainActivity started from outer world/space/widget, path: '" + pathToOpen + "'");
        }


    }


    public void handlePermissions() {

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            Timber.d("version code > M, must handle permissions on runtime");
            if ((ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
                    || (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)) {
                Timber.d("permission denied to read storage, will ask user");

                Snackbar.make(this.findViewById(android.R.id.content),
                        getString(R.string.info_app_restart_after_permission_grant),
                        Snackbar.LENGTH_INDEFINITE)
                        .show();

                String[] permissions = {Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE};
                ActivityCompat.requestPermissions(this, permissions, PERMISSION_REQUEST_CODE_READ_WRITE_STORAGE);
            } else {
                Timber.d("permission to read storage OK");
                initOnAllPermissionsGranted(pathToOpen);
            }
        }
        // TODO Test legacy device (without runtime permission handling)
        else {
            Timber.d("permission to read storage OK");
            initOnAllPermissionsGranted(pathToOpen);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST_CODE_READ_WRITE_STORAGE:
                if (grantResults != null && grantResults.length > 0) {

                    boolean allPermissionsGranted = true;   // assume all were granted
                    for (int result : grantResults) {
                        if (result != PackageManager.PERMISSION_GRANTED) {
                            allPermissionsGranted = false;  // one failure: ask again for permission
                        }
                    }

                    if (allPermissionsGranted) {
                        Timber.d("onRequestPermissionsResult(): permissions indeed granted");

                        // TODO: solve weird permission problem, reason unclear
                        // directly using granted permissions during first app runtime does not work for some unknown reason, see notes below
//                        initOnAllPermissionsGranted(pathToOpen);

                        /*
                        Today's Dirty Hack: restart app! solves weird problem, that files/folders can not be read
                        immediately after permissions were granted, only app restart makes them readable.

                        long version:
                        - during first app start: permissions are requested via popup, but even after the user granted permissions: during
                        first app runtime all methods on file objects do not work correctly: canRead() returns false, listFiles() returns null, ...
                        - The system nonetheless claims that permissions have been granted
                        - restarting the app "solves" the problem: from second app start on everything is fine until the end of time :-)
                        - recreating the app transparently for the user makes this problem more or less "invisible" until reason is found
                        and eliminated
                         */
                        Timber.d("no read access after granting permissions, restarting app");
                        // recreate()-method does not always work as necessary (seems to depend on circumstances/device?)
                        // instead: using pending intent with timer to restart MainActivity and then exit app
                        // as found here: https://stackoverflow.com/a/17166729
                        Intent intentRestart = new Intent(this, MainActivity.class);
                        int pendingIntentId = 1337;
                        PendingIntent pendingIntent = PendingIntent.getActivity(this,
                                pendingIntentId, intentRestart, PendingIntent.FLAG_CANCEL_CURRENT);
                        AlarmManager alarmManager = (AlarmManager) this.getSystemService(Context.ALARM_SERVICE);
                        alarmManager.set(AlarmManager.RTC, System.currentTimeMillis() + 100, pendingIntent);
                        System.exit(0);


                    } else {
                        // permissions were not granted, i.e. user clicked no... advice him to change his mind
                        Snackbar.make(this.findViewById(android.R.id.content),
                                getString(R.string.info_after_permission_dismissal),
                                Snackbar.LENGTH_INDEFINITE)
                                .setAction(getString(R.string.ok),
                                        new View.OnClickListener() {
                                            @Override
                                            public void onClick(View v) {
                                                handlePermissions();
                                            }
                                        }).show();
                    }
                }
                break;
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }


    public void initOnAllPermissionsGranted(String pathToOpen) {
        Timber.d("initOnAllPermissionsGranted() called");

        // phone storage places need permissions too (checking if folder contents exist)
        // local phone storage places (on demand, not from database)
        initPhonePlaces();
        initFileManagerFragment(pathToOpen);
    }


    @Override
    protected void onResume() {
        super.onResume();
        getSupportLoaderManager().restartLoader(LOADER_ID_LOCAL_FAVORITES, null, loaderListenerLocalFavorites);
    }

    private void initPhonePlaces() {
        // TODO: longterm: each fragment needs own NavPanel (tablets, landscape)

        // phone storage (not user customizable, not from database, created on demand)
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        rvLocalPhoneStorages.setLayoutManager(layoutManager);
        adapterLocalPhoneStorages = new FavPlacesLocalAdapter(this, FavPlacesLocalAdapter.TAG_PHONE_STORAGES);
        rvLocalPhoneStorages.setAdapter(adapterLocalPhoneStorages);
        List<FavoritePlace> placesPhoneStorages = FileHelpers.getLocalPhoneFolders(this);
        if (placesPhoneStorages != null) {
            adapterLocalPhoneStorages.swapFavorites(placesPhoneStorages);
        }


    }


    /**
     * @param path i.e. when opening from widget (may be null, will then revert to default folder)
     */
    private void initFileManagerFragment(String path) {
        Timber.d("SAVE_STATE: MainActivity: initFileManagerFragment(): path");
        FragmentManager fm = getSupportFragmentManager();

        // this is a bit hacky, but it works until multiple fragments will be used
        List<Fragment> fragments = fm.getFragments();
        if (fragments != null && fragments.size() > 0 && fragments.get(0) instanceof FileBrowserFragment) {
            Timber.d("SAVE_STATE: MainActivity: fragments already exists");
            fragmentForBackButtonPress = (FileBrowserFragment) fragments.get(0);
            return;
        }

//        FileBrowserFragment fragmentFileBrowser = new FileBrowserFragment();
        FileBrowserFragment fragmentFileBrowser = FileBrowserFragment.newInstance(path);
        fm.beginTransaction()
                .replace(R.id.fragment_main_activity_single_column, fragmentFileBrowser)
                .commit();
        fragmentForBackButtonPress = fragmentFileBrowser;
//        fragmentFileBrowser.swapFolder(path);
    }


    @Override
    public void onBackPressed() {
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {

            // only handle back if active browser fragment did not (for moving up)
            if (!fragmentForBackButtonPress.backButtonWasHandledInFragment()) {
                super.onBackPressed();
//                moveTaskToBack(true);
            }


        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // no menu yet, gets necessary when sorting and view modes get introduced later
//        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }


    @Override
    public void onFragmentInteraction(Uri uri) {

    }

    @Override
    public void onListItemClicked(int clickedItemIndex, String tag) {

        // init analytics event
        Bundle bundle = new Bundle();
        bundle.putString(FirebaseAnalytics.Param.ITEM_ID, "Sidebar");
        bundle.putString(FirebaseAnalytics.Param.CONTENT_TYPE, "navigation");

        // need to check which adapter has to be asked for path in clicked item
        FavoritePlace fav = null;
        switch (tag) {
            case FavPlacesLocalAdapter.TAG_PHONE_STORAGES:
                fav = adapterLocalPhoneStorages.getFavoriteAtPosition(clickedItemIndex);
                bundle.putString(FirebaseAnalytics.Param.ITEM_NAME, "phone storage");
                break;
            case FavPlacesLocalAdapter.TAG_LOCAL_FAVORITES:
                fav = adapterLocalFavoritePlaces.getFavoriteAtPosition(clickedItemIndex);
                bundle.putString(FirebaseAnalytics.Param.ITEM_NAME, "local favorite");
                break;
            default:
                Timber.d("BEWARE, unhandled case for FavPlacesLocalAdapter.ListItemClickListener!");
        }

        // finish Analytics action and send
        mFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundle);

        drawer.closeDrawer(GravityCompat.START);

        String path = fav.getPathStr();
        FileBrowserFragment fragment = (FileBrowserFragment) fragmentForBackButtonPress;
        fragment.swapFolder(path);
    }


    private LoaderManager.LoaderCallbacks<List<FavoritePlace>> loaderListenerLocalFavorites =
            new LoaderManager.LoaderCallbacks<List<FavoritePlace>>() {

                @NonNull
                @Override
                public Loader<List<FavoritePlace>> onCreateLoader(int id, @Nullable Bundle args) {

                    return new AsyncTaskLoader<List<FavoritePlace>>(MainActivity.this) {

                        @Override
                        protected void onStartLoading() {

                            Timber.d("Loader onStartLoading() called");
                            forceLoad();
                        }

                        @Nullable
                        @Override
                        public List<FavoritePlace> loadInBackground() {
                            Timber.d("Loader loadInBackground() called");
                            List<FavoritePlace> placesLocalFavorites = FavoritesManager
                                    .getLocalFavoritesFromDb(MainActivity.this);
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
                    Timber.d("Loader onLoadFinished() called");
                    adapterLocalFavoritePlaces.swapFavorites(placesLocalFavorites);
                }

                @Override
                public void onLoaderReset(@NonNull Loader<List<FavoritePlace>> loader) {
                    Timber.d("Loader: onLoaderReset() called");
                }
            };


}
