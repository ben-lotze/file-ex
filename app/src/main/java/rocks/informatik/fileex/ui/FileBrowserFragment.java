package rocks.informatik.fileex.ui;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.firebase.analytics.FirebaseAnalytics;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import butterknife.BindView;
import butterknife.ButterKnife;
import rocks.informatik.fileex.R;
import rocks.informatik.fileex.adapters.FileBrowserAdapter;
import rocks.informatik.fileex.tools.AdmobHelper;
import timber.log.Timber;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link FileBrowserFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link FileBrowserFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class FileBrowserFragment extends Fragment
        implements FragmentBackButtonListener, FileBrowserAdapter.AdapterCallback {

    @BindView(R.id.rv_file_browser) RecyclerView rvFileBrowser;
    @BindView(R.id.ad_view_file_browser_bottom) AdView mAdViewBottom;


    private FirebaseAnalytics mFirebaseAnalytics;

    private FileBrowserAdapter adapter;
    private static final String ARG_PATH_TO_OPEN = "pathToOpen";
    private String mPathToOpen;

    private OnFragmentInteractionListener mListener;

    public FileBrowserFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param pathToOpen Parameter 1.
     * @return A new instance of fragment FileBrowserFragment.
     */

    public static FileBrowserFragment newInstance(String pathToOpen) {
        Timber.d("SAVE_STATE: creating new instance of FileBrowserFragment");
        FileBrowserFragment fragment = new FileBrowserFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PATH_TO_OPEN, pathToOpen);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
            String activeFolder = savedInstanceState.getString(KEY_ACTIVE_FOLDER);
            mPathToOpen = activeFolder;
            List<String> historyList = savedInstanceState.getStringArrayList(KEY_BROWSING_HISTORY);
            adapter = new FileBrowserAdapter(activeFolder, historyList, this);
            Timber.d("SAVE_STATE: onCreate(): reinstated folder and history: " + activeFolder);

        }

        if (getArguments() != null) {
            mPathToOpen = getArguments().getString(ARG_PATH_TO_OPEN);
        }

//        AnalyticsApplication application = (AnalyticsApplication) getActivity().getApplication();
//        mTracker = application.getDefaultTracker();

        mFirebaseAnalytics = FirebaseAnalytics.getInstance(getContext().getApplicationContext());

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        Timber.d("SAVE_STATE: onCreateView(): will now call initFileBrowser()");

        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_file_browser, container, false);
        ButterKnife.bind(this, view);

        initFileBrowser();

        AdRequest adRequest = AdmobHelper.createAdRequest(true);
        mAdViewBottom.loadAd(adRequest);

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
//        mTracker.setScreenName(trackerName);
//        mTracker.send(new HitBuilders.ScreenViewBuilder().build());

        Bundle bundle = new Bundle();
        bundle.putString(FirebaseAnalytics.Param.ITEM_ID, "ScreenName");
        bundle.putString(FirebaseAnalytics.Param.ITEM_NAME, this.getClass().getSimpleName());
        mFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.VIEW_ITEM_LIST, bundle);
    }

    // TODO: Rename method, update argument and hook method into UI event
    public void onButtonPressed(Uri uri) {
        if (mListener != null) {
            mListener.onFragmentInteraction(uri);
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnGoogleDriveFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }


    public void swapFolder(String folder) {

        adapter.swapActiveFolder(folder, true);

        String folderName = adapter.getActiveFolderName();
        getActivity().setTitle(folderName);
        Timber.d( "toolbar title changed to '" + folderName + "'");
    }

    @Override
    public void changeTitle(String title) {
        getActivity().setTitle(title);
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name

        void onFragmentInteraction(Uri uri);
    }


    private void initFileBrowser() {
        LinearLayoutManager layoutManager = new LinearLayoutManager(this.getContext());
        rvFileBrowser.setLayoutManager(layoutManager);

        if (adapter == null) {
            adapter = new FileBrowserAdapter(this);
            Timber.d( "created new FileBrowserAdapter with defaults");
        }

        if (mPathToOpen != null) {
            // TODO: what to do with backstack inside fragment?
            adapter.swapActiveFolder(mPathToOpen, false);
        }
        rvFileBrowser.setAdapter(adapter);

    }


    @Override
    public boolean backButtonWasHandledInFragment() {
        if (adapter.onBackPressedDirectoryUpWasSuccessful()) {
            return true;
        }
        return false;
    }


    public static final String KEY_ACTIVE_FOLDER = "KEY_ACTIVE_FOLDER";
    public static final String KEY_BROWSING_HISTORY = "KEY_BROWSING_HISTORY";
    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);

        String activeFolderStr = adapter.getActiveFolderPathStr();
        outState.putString(KEY_ACTIVE_FOLDER, activeFolderStr);
        Stack<String> history = adapter.getBrowsingHistory();
        ArrayList<String> historyList = new ArrayList<>(history);
        outState.putStringArrayList(KEY_BROWSING_HISTORY, historyList);
    }
}
