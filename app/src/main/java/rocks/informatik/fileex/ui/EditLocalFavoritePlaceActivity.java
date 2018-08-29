package rocks.informatik.fileex.ui;

import android.content.ContentValues;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.analytics.FirebaseAnalytics;

import java.io.File;

import butterknife.BindView;
import butterknife.ButterKnife;
import rocks.informatik.fileex.R;
import rocks.informatik.fileex.data.FavoritePlace;
import rocks.informatik.fileex.db.FavPlacesContract;
import timber.log.Timber;

public class EditLocalFavoritePlaceActivity extends AppCompatActivity {

    @BindView(R.id.btn_edit_fav_place_local_choose_icon)
    ImageView ivBtnChooseIcon;
    @BindView(R.id.btn_edit_fav_place_local_choose_folder)
    Button btnChooseFolder;
    @BindView(R.id.btn_edit_fav_place_local_cancel)
    Button btnCancel;
    @BindView(R.id.btn_edit_fav_place_local_ok)
    Button btnOk;

    @BindView(R.id.edittext_edit_fav_place_local_name)
    EditText editName;
    @BindView(R.id.tv_fav_place_local_folder_path_info)
    TextView tvFolderInfo;


    private static final int REQUEST_CODE_SELECT_FOLDER = 0;
    private static final int REQUEST_CODE_SELECT_ICON = 1;

    public static final String INTENT_EXTRA_ICON_RES_ID_RESULT = "INTENT_EXTRA_ICON_RES_ID_RESULT";
    public static final String INTENT_EXTRA_FOLDER_PATH_RESULT = "INTENT_EXTRA_FOLDER_PATH_RESULT";
    public static final String INTENT_EXTRA_FAVORITE_TO_EDIT = "INTENT_EXTRA_FAVORITE_TO_EDIT";

    private FirebaseAnalytics mFirebaseAnalytics;

    private int placeIdToUpdate;
    private int iconResId = R.drawable.ic_folders_black_24dp;  // default

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_local_favorite_place);
        ButterKnife.bind(this);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // always using same instance of firebase
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(getApplicationContext());

        Intent intent = getIntent();
        FavoritePlace placeToEdit = intent.getParcelableExtra(INTENT_EXTRA_FAVORITE_TO_EDIT);
        if (placeToEdit != null) {
            placeIdToUpdate = placeToEdit.getId();
            getSupportActionBar().setTitle(R.string.activity_title_edit_favorite);

            iconResId = placeToEdit.getDrawableResId();
            if (iconResId <= 0) {
                iconResId = R.drawable.ic_folders_black_24dp;
            }


            editName.setText(placeToEdit.getName());
            tvFolderInfo.setHint(placeToEdit.getPathStr());

        } else {
            getSupportActionBar().setTitle(R.string.activity_title_new_favorite);
        }

        ivBtnChooseIcon.setImageResource(iconResId);
    }

    public void onSelectIconClicked(View view) {
        // start activity to choose pre-defined icons
        Intent intent = new Intent(this, FolderIconChooserActivity.class);
        startActivityForResult(intent, REQUEST_CODE_SELECT_ICON);
    }

    public void onSelectFolderClicked(View view) {
        // start activity to choose a folder
        Intent intent = new Intent(this, SelectFolderActivity.class);
        startActivityForResult(intent, REQUEST_CODE_SELECT_FOLDER);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
//        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_SELECT_FOLDER && resultCode == RESULT_OK) {
            String path = data.getStringExtra(INTENT_EXTRA_FOLDER_PATH_RESULT);
            Timber.d("received result: " + path);
            tvFolderInfo.setHint(path);

            // also pre-define name
            int index = TextUtils.lastIndexOf(path, '/');
            if (index < path.length()) {
                String namePredefined = path.substring(index + 1);
                editName.setText(namePredefined);
            }

        } else if (requestCode == REQUEST_CODE_SELECT_ICON && resultCode == RESULT_OK) {
            int iconResId = data.getIntExtra(INTENT_EXTRA_ICON_RES_ID_RESULT, R.drawable.ic_folders_black_24dp);
            ivBtnChooseIcon.setImageResource(iconResId);
            this.iconResId = iconResId; // !!!!
        }
    }

    public void onCancelClicked(View view) {

        // TODO: if already values changed/entered -> create popup, ask user to really cancel?

        // return to parent activity (PlacesManager)
        onBackPressed();
    }

    @Override
    public void onBackPressed() {

        // log the fact that the user did not finish to add/edit a favorite
        Bundle bundle = new Bundle();
//        bundle.putString(FirebaseAnalytics.Param.ITEM_ID, "");
        bundle.putString(FirebaseAnalytics.Param.CONTENT_TYPE, "FavoritePlaceEditor");
        // log if editing or creating new favorite was aborted
        if (placeIdToUpdate != 0) {
            bundle.putString(FirebaseAnalytics.Param.ITEM_NAME, "UPDATE local favorite aborted");
        } else {
            bundle.putString(FirebaseAnalytics.Param.ITEM_NAME, "NEW local favorite aborted");
        }
        mFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundle);

        super.onBackPressed();
    }

    public void onOkClicked(View view) {

        // check input before sending update/insert
        boolean folderOk = false;
        String pathChosen = tvFolderInfo.getHint().toString();
        if (pathChosen != null) {
            File file = new File(pathChosen);
            if (file.exists() && file.isDirectory()) {
                folderOk = true;
            }
        }

        String name = editName.getText().toString();
        boolean nameOk = !TextUtils.isEmpty(name);
        if (!folderOk || TextUtils.isEmpty(name)) {
            // "If data does not exist or is in the wrong format, the app logs this fact and does not crash." :-)
            Timber.d(String.format("Input not ok: folderOk=%b, name='%s'", folderOk, name));
            // create popup to fix inputs
            Toast.makeText(this, "Please check input:"
                            + (folderOk ? "folder ok" : "folder invalid")
                            + (nameOk ? ", name ok" : ", name invalid")
                    , Toast.LENGTH_LONG).show();
            return;
        }

        Timber.d("user input OK!");

        // create content values
        ContentValues values = new ContentValues();
        values.put(FavPlacesContract.FavPlaceLocalEntry.COLUMN_NAME, name);
        values.put(FavPlacesContract.FavPlaceLocalEntry.COLUMN_PATH, pathChosen);
        values.put(FavPlacesContract.FavPlaceLocalEntry.COLUMN_ICON_RES_ID, iconResId);

        Uri uri = FavPlacesContract.FavPlaceLocalEntry.CONTENT_URI;

        // need to know if update/insert case
        Intent intent = getIntent();
        int actionType = intent.getIntExtra(PlacesManagerLocalFavoritesActivity.INTENT_EXTRA_ACTION_TYPE, 0);
        if (PlacesManagerLocalFavoritesActivity.ACTION_TYPE_NEW_FAV == actionType) {
            // keep uri as is
            getContentResolver().insert(uri, values);
        } else if (PlacesManagerLocalFavoritesActivity.ACTION_TYPE_UPDATE_FAV == actionType) {
            // append entry _ID to update
            uri = uri.buildUpon().appendEncodedPath(String.valueOf(placeIdToUpdate)).build();
            getContentResolver().update(uri, values, null, null);
        }

        finish();
    }
}
