package rocks.informatik.fileex.adapters;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;

import com.bumptech.glide.Glide;

import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.audio.AudioHeader;
import org.jaudiotagger.audio.exceptions.CannotReadException;
import org.jaudiotagger.audio.exceptions.InvalidAudioFrameException;
import org.jaudiotagger.audio.exceptions.ReadOnlyFileException;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.Tag;
import org.jaudiotagger.tag.TagException;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Stack;

import butterknife.BindView;
import butterknife.ButterKnife;
import rocks.informatik.fileex.R;
import rocks.informatik.fileex.tools.FileHelpers;
import rocks.informatik.fileex.tools.MusicTagHelpers;

public class FileBrowserAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {


    /**
     * TODO later: implement filters, only show files/folders containing files that are supported
     * - recursively scan folders to only show folders containing music at some deeper subfolder?
     * */

    private String activeFolderStr;
    private List<File> activeFolderContents;
    private Stack<String> browsingHistory;

    private Context context;

    private static final int VIEW_TYPE_FILE_DEFAULT = R.layout.rv_item_file_default;
    private static final int VIEW_TYPE_FILE_AUDIO = R.layout.rv_item_song_add_info_with_album_art;

    private static final int RV_ITEM_TAG_FILENAME = 1;  // TODO: put tags into resources?

    private AdapterCallback callback;



    public FileBrowserAdapter(AdapterCallback callback) {
        Log.d("FileBrowserAdapter", "SAVE_STATE: new FileBrowserAdapter (default)");
        this.activeFolderStr = Environment.getExternalStorageDirectory().getAbsolutePath();
        this.browsingHistory = new Stack<>();
        this.callback = callback;
        swapActiveFolder(activeFolderStr, false);
    }

    public FileBrowserAdapter(String activeFolderStr, Stack<String> browsingHistory, AdapterCallback callback) {
        this.activeFolderStr = activeFolderStr;
        this.browsingHistory = browsingHistory;
        this.callback = callback;
        swapActiveFolder(activeFolderStr, false);
    }
    public FileBrowserAdapter(String activeFolderStr, List<String> browsingHistoryParam, AdapterCallback callback) {
        Log.d("FileBrowserAdapter", "SAVE_STATE: new FileBrowserAdapter (with params)");
        this.activeFolderStr = activeFolderStr;
        this.browsingHistory = new Stack<>();
        this.callback = callback;
        browsingHistory.addAll(browsingHistoryParam);
        swapActiveFolder(activeFolderStr, false);
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
//        Log.d("Browser", "onCreateViewHolder()");
        this.context = parent.getContext();
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(context,
                LinearLayoutManager.VERTICAL, false);
        LayoutInflater inflater = LayoutInflater.from(context);

        RecyclerView.ViewHolder holder = null;
        if (VIEW_TYPE_FILE_DEFAULT == viewType) {
            View view = inflater.inflate(R.layout.rv_item_file_default, parent, false);
            holder = new ViewHolderFile(view);
        } else if (VIEW_TYPE_FILE_AUDIO == viewType) {
            View view = inflater.inflate(R.layout.rv_item_song_add_info_with_album_art, parent, false);
            holder = new ViewHolderMusic(view);
        }

        return holder;
    }


    @Override
    public int getItemViewType(int position) {
        File file = activeFolderContents.get(position);
//        String fileName = file.getName();
        String extension = FileHelpers.getExtensionString(file);
        int chosenType = VIEW_TYPE_FILE_DEFAULT;
        if (FileHelpers.getAudioFileExtensions().contains(extension)) {
            chosenType = VIEW_TYPE_FILE_AUDIO;
        }
        return chosenType;
    }


    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        int viewType = getItemViewType(position);
        if (VIEW_TYPE_FILE_DEFAULT == viewType) {
            ViewHolderFile holderDefault = (ViewHolderFile) holder;
            holderDefault.bind(holderDefault, position);
        } else if (VIEW_TYPE_FILE_AUDIO == viewType) {
            ViewHolderMusic holderMusic = (ViewHolderMusic) holder;
            holderMusic.bind(holderMusic, position);
        }
    }


    @Override
    public int getItemCount() {
        if (activeFolderContents == null) {
            return 0;
        }
        return activeFolderContents.size();
    }


    public String getActiveFolderName() {
        int indexLastSlash = TextUtils.lastIndexOf(activeFolderStr, '/');
        String folderName = activeFolderStr.substring(indexLastSlash+1);
        return folderName;
    }

    public String getActiveFolderPathStr() {
        return activeFolderStr;
    }

    public Stack<String> getBrowsingHistory() {
        return browsingHistory;
    }

    public void swapActiveFolder(String folderStr, boolean addLastFolderToHistory) {
        // fetching folder contents internally uses AsyncTask
        List<File> filesNewFolder = FileHelpers.getFolderContents(folderStr);
        // FIRST handle history, otherwise current folder is lost (for history)
        if (addLastFolderToHistory) {
            addFolderToHistory(this.activeFolderStr);
        }
        this.activeFolderStr = folderStr;
        this.activeFolderContents = filesNewFolder;
        notifyDataSetChanged();
    }


    public boolean onBackPressedDirectoryUpWasSuccessful() {
        Log.d("Browser", "onBackPressedDirectoryUpWasSuccessful() called");
        if (!historyHasPreviouslyVisitedFolder()) {
            Log.d("Browser", "folder history is empty");
            return false;
        }
        String lastFolder = popLastVisitedFolderFromHistory();
        swapActiveFolder(lastFolder, false);
        return true;
    }


    class ViewHolderFile extends RecyclerView.ViewHolder
            implements View.OnClickListener {

        @BindView(R.id.tv_rv_item_filename) TextView tvFilename;
        @BindView(R.id.iv_folder_icon) ImageView ivFolderIcon;
        // TODO later: implement more detailed view
//        @BindView(R.id.tv_rv_item_file_last_modified) TextView tvLastModified;
//        @BindView(R.id.tv_rv_item_file_size) TextView tvFileSize;

        public ViewHolderFile(View itemView) {
            super(itemView);
//            Log.d("Browser", "ViewHolderFile constructor");
            itemView.setOnClickListener(this);
            ButterKnife.bind(this, itemView);
        }


        @Override
        public void onClick(View v) {
//            Log.d("Browser", "ViewHolderMusic onClick");
            String clickedItemStr = (String) v.getTag(R.id.tv_rv_item_filename);
            handleItemClick(clickedItemStr);
        }


        public void bind(RecyclerView.ViewHolder holder, int position) {
            File file = activeFolderContents.get(position);
            String fileName = file.getName();
            itemView.setTag(R.id.tv_rv_item_filename, fileName);
            tvFilename.setText(fileName);

            // thumbnail
            String extension = FileHelpers.getExtensionString(file);
            if (file.isDirectory()) {
                Glide.with(context).load(R.drawable.ic_folders_black_24dp).into(ivFolderIcon);
            }
            else if (file.isFile()) {
                if ("jpg".equalsIgnoreCase(extension) || "png".equalsIgnoreCase(extension) || "webp".equalsIgnoreCase(extension) ) {

                    Uri imgUri = Uri.fromFile(file);
                    Glide.with(context).load(imgUri).
                            thumbnail(0.1f).
                            into(ivFolderIcon);
                }
                // fallback
                else {
                    Glide.with(context).load(R.drawable.ic_file_default_fallback_black_24dp).into(ivFolderIcon);
                }
            }


        }
    }


    class ViewHolderMusic extends RecyclerView.ViewHolder
            implements View.OnClickListener {

        @BindView(R.id.iv_rv_album_art)
        ImageView ivArtwork;
        @BindView(R.id.tv_track_number)
        TextView tvTrackNumber;
        @BindView(R.id.tv_title)
        TextView tvSongTitle;
        @BindView(R.id.tv_additional_song_info)
        TextView tvAdditionalInfo;
        @BindView(R.id.tv_length)
        TextView tvLength;
        @BindView(R.id.rating_bar)
        RatingBar ratingBar;


        public ViewHolderMusic(View itemView) {
            super(itemView);
            itemView.setOnClickListener(this);
            ButterKnife.bind(this, itemView);
        }


        @Override
        public void onClick(View v) {
            String clickedItemStr = (String) v.getTag(R.id.tv_rv_item_filename);
            handleItemClick(clickedItemStr);
        }


        public void bind(RecyclerView.ViewHolder holderMusic, int position) {

            File clickedFile = activeFolderContents.get(position);
            String fileName = clickedFile.getName();
            // store filename in holder for ClickHandler
            holderMusic.itemView.setTag(R.id.tv_rv_item_filename, fileName);

            if (activeFolderContents != null && position < activeFolderContents.size()) {
                try {
                    AudioFile f = AudioFileIO.read(clickedFile);
                    Tag tag = f.getTag();
                    AudioHeader header = f.getAudioHeader();

                    Bitmap bitmap = MusicTagHelpers.getArtworkBitmapFromTag(tag);
                    if (bitmap != null) {
                        Glide.with(context).load(bitmap).into(ivArtwork);
                    }
                    // else: keep default background color in ImageView for now


                    String trackNumber = tag.getFirst(FieldKey.TRACK);
                    tvTrackNumber.setText(trackNumber);

                    String title = tag.getFirst(FieldKey.TITLE);
                    tvSongTitle.setText(title);

                    String album = tag.getFirst(FieldKey.ALBUM);
                    String year = tag.getFirst(FieldKey.YEAR);
                    String addInfo = String.format("on %s (%s)", album, year);
                    tvAdditionalInfo.setText(addInfo);

                    String runtimeStr = MusicTagHelpers.getRuntimeStr(header);
                    tvLength.setText(runtimeStr);

                    float rating = MusicTagHelpers.getRatingStars(tag);
                    ratingBar.setRating(rating);

                    // TODO some tags can have multiple entries! (i.e. genres)
                    /* JAudiotagger seems to have no support for this (most players supporting this will
                     * divide separate entries by semicolon, though there is no common standard
                     * -> write helper method that divides separate tag entries
                     */
//                    List<String> genres = tag.getAll(FieldKey.GENRE);
//                    Log.d("Browser", fileName + ": genreCount="+genres.size());
//                    String genresCombinedStr = TextUtils.join(" ### ", genres);   // for testing
                } catch (CannotReadException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (TagException e) {
                    e.printStackTrace();
                } catch (ReadOnlyFileException e) {
                    e.printStackTrace();
                } catch (InvalidAudioFrameException e) {
                    e.printStackTrace();
                }
            }

        }

    }


    private boolean historyHasPreviouslyVisitedFolder() {
        if (browsingHistory == null || browsingHistory.size() == 0) {
            return false;
        }
        return true;
    }

    /**
     * handled internally, caller should not bother with this
     */
    private void addFolderToHistory(String folderStr) {
        if (browsingHistory == null) {
            browsingHistory = new Stack<>();
        }
        browsingHistory.push(folderStr);
    }

    private String popLastVisitedFolderFromHistory() {
        if (browsingHistory == null || browsingHistory.size() == 0) {
            return null;
        }
        return browsingHistory.pop();
    }


    // TODO: later -> maybe handle click for position, instead of filename (hidden as tag in view holder)
    public void handleItemClick(String fileName) {
        File clickedFile = new File(activeFolderStr, fileName);
        if (clickedFile.isDirectory()) {
            String clickedFileStr = clickedFile.toString();
            FileBrowserAdapter.this.swapActiveFolder(clickedFileStr, true);

        } else {
            FileHelpers.startFileInAssociatedApp(context, clickedFile);
        }

        String folderName = getActiveFolderName();
        callback.changeTitle(folderName);
    }


    /** used to change title in Activity's toolbar from adapter which doesn't have the correct context to do so */
    public interface AdapterCallback {
        void changeTitle(String s);
    }


}
