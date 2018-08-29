package rocks.informatik.fileex.ui;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.Button;

import butterknife.BindView;
import butterknife.ButterKnife;
import rocks.informatik.fileex.R;
import rocks.informatik.fileex.adapters.FileBrowserAdapter;

public class SelectFolderActivity extends AppCompatActivity implements FileBrowserAdapter.AdapterCallback {

    @BindView(R.id.rv_folder_selector)
    RecyclerView rvFolderBrowser;
    @BindView(R.id.btn_select_folder)
    Button btnSelectFolder;

    // re-using same adapter as for normal file browsing
    private FileBrowserAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_folder);
        ButterKnife.bind(this);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle(R.string.activity_title_choose_fav_folder);

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        rvFolderBrowser.setLayoutManager(layoutManager);

        adapter = new FileBrowserAdapter(this);
        rvFolderBrowser.setAdapter(adapter);


    }

    public void onClickFolderSelected(View view) {

        String folder = adapter.getActiveFolderPathStr();

        Intent intentResult = new Intent();
        intentResult.putExtra(EditLocalFavoritePlaceActivity.INTENT_EXTRA_FOLDER_PATH_RESULT, folder);
        setResult(RESULT_OK, intentResult);
        finish();
    }

    @Override
    public void changeTitle(String title) {
        setTitle(title);
    }

    @Override
    public void onBackPressed() {
        boolean onBackHandledByAdapter = adapter.onBackPressedDirectoryUpWasSuccessful();
        if(!onBackHandledByAdapter) {
            super.onBackPressed();
        }
    }
}
