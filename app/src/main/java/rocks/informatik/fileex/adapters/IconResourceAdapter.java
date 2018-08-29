package rocks.informatik.fileex.adapters;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import butterknife.BindView;
import butterknife.ButterKnife;
import rocks.informatik.fileex.R;

public class IconResourceAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private Context context;
    private int[] icons = {
            R.drawable.ic_android_black_24dp,
            R.drawable.ic_cake_black_24dp,
            R.drawable.ic_camera_roll_black_24dp,
            R.drawable.ic_downloads_black_24dp,
            R.drawable.ic_folders_black_24dp,
            R.drawable.ic_menu_camera,
            R.drawable.ic_menu_gallery,
            R.drawable.ic_phone_android_black_24dp,
            R.drawable.ic_sd_card_black_24dp,
            R.drawable.ic_file_upload_black_24dp
    };

    private IconGridClickListener onClickListener;

    public IconResourceAdapter(IconGridClickListener onClickListener) {
        this.onClickListener = onClickListener;
    }

    public interface IconGridClickListener {
        void onGridItemClicked(int clickedItemInde);
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        context = parent.getContext();

//        RecyclerView.LayoutManager layoutManager = new GridLayoutManager(this, 2);
        LayoutInflater inflater = LayoutInflater.from(context);
        View view = view = inflater.inflate(R.layout.rv_item_grid_image, parent, false);
        RecyclerView.ViewHolder holder = new ViewHolderIcon(view);
        return holder;
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        // for now only one type, so casting is safe
        ViewHolderIcon iconHolder = (ViewHolderIcon) holder;
        iconHolder.bind(position);
    }

    @Override
    public int getItemCount() {
        return icons.length;
    }

    public int getIconResourceAtIndex(int index) {
        if (index < icons.length) {
            return icons[index];
        }
        // provide default icon in case of errors
        return R.drawable.ic_folders_black_24dp;
    }


    public class ViewHolderIcon extends RecyclerView.ViewHolder implements View.OnClickListener {

        @BindView(R.id.iv_rv_grid_image)
        ImageView ivFolderIcon;

        public ViewHolderIcon(View itemView) {
            super(itemView);
            itemView.setOnClickListener(this);
            ButterKnife.bind(this, itemView);
        }

        public void bind(int position) {
            int iconResId = getIconResourceAtIndex(position);
            ivFolderIcon.setImageResource(iconResId);
        }

        @Override
        public void onClick(View v) {
            int clickedItemPosition = getAdapterPosition();
            onClickListener.onGridItemClicked(clickedItemPosition);
        }
    }
}
