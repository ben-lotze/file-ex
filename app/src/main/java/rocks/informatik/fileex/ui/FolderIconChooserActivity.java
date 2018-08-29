package rocks.informatik.fileex.ui;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;

import butterknife.BindView;
import butterknife.ButterKnife;
import rocks.informatik.fileex.R;
import rocks.informatik.fileex.adapters.IconResourceAdapter;
import timber.log.Timber;

public class FolderIconChooserActivity extends AppCompatActivity implements IconResourceAdapter.IconGridClickListener {

    private static final String TAG = FolderIconChooserActivity.class.getSimpleName();

    @BindView(R.id.rv_icon_chooser)
    RecyclerView rvIcons;

    private IconResourceAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_folder_icon_chooser);
        ButterKnife.bind(this);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle(R.string.activity_title_choose_fav_icon);

        RecyclerView.LayoutManager layoutManager = new GridLayoutManager(this, 3);
        rvIcons.setLayoutManager(layoutManager);
        adapter = new IconResourceAdapter(this);
        rvIcons.setAdapter(adapter);
    }


    @Override
    public void onGridItemClicked(int clickedItemIndex) {
        // create result for caller
        Timber.d("grid icon clicked");
        int iconResId = adapter.getIconResourceAtIndex(clickedItemIndex);

        Intent intentResult = new Intent();
        intentResult.putExtra(EditLocalFavoritePlaceActivity.INTENT_EXTRA_ICON_RES_ID_RESULT, iconResId);
        setResult(RESULT_OK, intentResult);
        finish();
    }
}
