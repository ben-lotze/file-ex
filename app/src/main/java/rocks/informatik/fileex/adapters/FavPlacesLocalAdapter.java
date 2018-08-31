package rocks.informatik.fileex.adapters;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.bumptech.glide.Glide;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import rocks.informatik.fileex.R;
import rocks.informatik.fileex.data.FavoritePlace;
import rocks.informatik.fileex.tools.FileHelpers;
import timber.log.Timber;

/**
 * queries all favorite places stored in the database
 */
public class FavPlacesLocalAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    public static final String TAG = FavPlacesLocalAdapter.class.getSimpleName();

    // when a Recycler was clicked in the DrawerLayout: need to know from which adapter to fetch the clickedItemPosition
    // (there are multiple RecyclerViews, each with its own FavPlacesLocalAdapter)
    private String tagAdapterInstance;
    public static final String TAG_PHONE_STORAGES = "TAG_PHONE_STORAGES";
    public static final String TAG_LOCAL_FAVORITES = "TAG_LOCAL_FAVORITES";

    public String getAdapterTag() {
        return tagAdapterInstance;
    }


    private static final int VIEW_TYPE_PHONE_STORAGE = R.layout.rv_item_drawer_phone_storage;
    private static final int VIEW_TYPE_LOCAL_FAVORITE = R.layout.rv_item_drawer_local_favorite;


    private List<FavoritePlace> favPlaces;
    private Context context;

    private final ListItemClickListener onClickListener;

    public interface ListItemClickListener {
        void onListItemClicked(int clickedItemIndex, String tag);
    }


    public FavPlacesLocalAdapter(ListItemClickListener onClickListener, String tag) {
        this.onClickListener = onClickListener;
        this.tagAdapterInstance = tag;
    }


    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        context = parent.getContext();

//        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(context,
//                LinearLayoutManager.VERTICAL, false);
        LayoutInflater inflater = LayoutInflater.from(context);

        RecyclerView.ViewHolder holder = null;
        if (viewType == VIEW_TYPE_PHONE_STORAGE) {
            View view =  view = inflater.inflate(VIEW_TYPE_PHONE_STORAGE, parent, false);
            holder = new ViewHolderPhoneStorage(view);
        } else if (viewType == VIEW_TYPE_LOCAL_FAVORITE) {
            View view = inflater.inflate(VIEW_TYPE_LOCAL_FAVORITE, parent, false);
            holder = new ViewHolderLocalFavorite(view);
        }

        return holder;
    }

    @Override
    public int getItemViewType(int position) {
        // chooses type depending on type of recycler in sidebar

        // default fallback, also valid for FavoriteManager (since that does not get the tag to handle phone storage)
        int chosenType = VIEW_TYPE_LOCAL_FAVORITE;
        if (TAG_PHONE_STORAGES.equals(tagAdapterInstance)) {
            chosenType = VIEW_TYPE_PHONE_STORAGE;
        }
        return chosenType;
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        int viewType = getItemViewType(position);
        if (viewType == VIEW_TYPE_PHONE_STORAGE) {
            ViewHolderPhoneStorage holderPhoneStorage = (ViewHolderPhoneStorage) holder;
            holderPhoneStorage.bind(position);
        } else if (viewType == VIEW_TYPE_LOCAL_FAVORITE) {
            ViewHolderLocalFavorite holderLocalFavorite = (ViewHolderLocalFavorite) holder;
            holderLocalFavorite.bind(position);
        }
    }


    public void swapFavorites(List<FavoritePlace> places) {
        this.favPlaces = places;
        Timber.d("swapPlaces: listing favorites");
        for (FavoritePlace place : favPlaces) {
            Timber.d(place.toString());
        }

        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        if (favPlaces == null) {
            return 0;
        }
        return favPlaces.size();
    }

    public FavoritePlace getFavoriteAtPosition(int position) {
        if (favPlaces != null || favPlaces.size() < position) {
            return favPlaces.get(position);
        }
        return null;
    }


    public class ViewHolderPhoneStorage extends RecyclerView.ViewHolder implements View.OnClickListener {

        @BindView(R.id.iv_rv_item_favority_entry_icon)
        ImageView ivIcon;
        @BindView(R.id.tv_rv_item_favority_entry_name)
        TextView tvName;
        @BindView(R.id.tv_fav_free_space)
        TextView tvFreeSpace;
        @BindView(R.id.fav_free_space_progress_bar)
        ProgressBar progressBar;

        public ViewHolderPhoneStorage(View itemView) {
            super(itemView);
            itemView.setOnClickListener(this);
            ButterKnife.bind(this, itemView);
        }

        public void bind(int position) {
            FavoritePlace favPlace = favPlaces.get(position);

            String name = favPlace.getName();
            tvName.setText(name);
            int drawableResId = favPlace.getDrawableResId();
            Glide.with(context).load(drawableResId).into(ivIcon);

            long freeSpace = favPlace.getFreeSpace();
            long totalSpace = favPlace.getTotalSize();
            // using string with 2 arguments: free (1st) and used space (2nd)
            // (different languages may have different needs where those info needs to be placed)
            String spaceInfo = String.format(context.getString(R.string.free_of),
                    FileHelpers.formatSize(freeSpace), FileHelpers.formatSize(totalSpace));
            tvFreeSpace.setText(spaceInfo);

            if (totalSpace > 0) {
                long percentageUsed = FileHelpers.calculatePercentageUsed(freeSpace, totalSpace);
                progressBar.setProgress((int) percentageUsed);

            } else {
                progressBar.setVisibility(View.INVISIBLE);
            }


            // set path as tag in holder (for click handler)
            String path = favPlace.getPathStr();
            this.itemView.setTag(R.id.tv_rv_item_favority_entry_name, path);
            int id  = favPlace.getId();
            this.itemView.setTag(R.id.tag_local_favorite_id_in_DB, id);
        }


        @Override
        public void onClick(View v) {
            int clickedPosition = getAdapterPosition();
            Timber.d("fav item clicked, notifying listener, index=" + clickedPosition
                    + ", path=" + v.getTag(R.id.tv_rv_item_favority_entry_name));
            onClickListener.onListItemClicked(clickedPosition, getAdapterTag());
        }
    }


    public class ViewHolderLocalFavorite extends RecyclerView.ViewHolder implements View.OnClickListener {

        @BindView(R.id.iv_rv_item_favority_entry_icon)
        ImageView ivIcon;
        @BindView(R.id.tv_rv_item_favority_entry_name)
        TextView tvName;


        public ViewHolderLocalFavorite(View itemView) {
            super(itemView);
            itemView.setOnClickListener(this);
            ButterKnife.bind(this, itemView);
        }

        public void bind(int position) {
            FavoritePlace favPlace = favPlaces.get(position);

            String name = favPlace.getName();
            tvName.setText(name);

            int drawableResId = favPlace.getDrawableResId();
            Glide.with(context).load(drawableResId).into(ivIcon);

            // set path as tag in holder (for click handler)
            String path = favPlace.getPathStr();
            this.itemView.setTag(R.id.tv_rv_item_favority_entry_name, path);
            int id  = favPlace.getId();
            this.itemView.setTag(R.id.tag_local_favorite_id_in_DB, id);
        }


        @Override
        public void onClick(View v) {
            int clickedPosition = getAdapterPosition();
            Timber.d("fav item clicked, notifying listener, index=" + clickedPosition
                    + ", path=" + v.getTag(R.id.tv_rv_item_favority_entry_name));
            onClickListener.onListItemClicked(clickedPosition, getAdapterTag());
        }
    }
}
