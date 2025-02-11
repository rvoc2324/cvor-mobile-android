package com.rvoc.cvorapp.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.rvoc.cvorapp.R;
import com.rvoc.cvorapp.adapters.FavouritesActionListener;
import com.rvoc.cvorapp.models.FavouritesModel;

import java.util.ArrayList;
import java.util.List;

public class FavouritesAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_ADD = 0;
    private static final int TYPE_FAVOURITE = 1;

    private final Context context;
    private final FavouritesActionListener listener;
    private final List<FavouritesModel> favourites = new ArrayList<>();

    public FavouritesAdapter(Context context, FavouritesActionListener listener) {
        this.context = context;
        this.listener = listener;
    }

    public void setFavourites(List<FavouritesModel> newFavourites) {
        favourites.clear();
        favourites.addAll(newFavourites);
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        return position == 0 ? TYPE_ADD : TYPE_FAVOURITE;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == TYPE_ADD) {
            View view = inflater.inflate(R.layout.item_add_favourite, parent, false);
            return new AddFavouriteViewHolder(view);
        } else {
            View view = inflater.inflate(R.layout.item_favourite, parent, false);
            return new FavouriteViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof AddFavouriteViewHolder) {
            ((AddFavouriteViewHolder) holder).bind();
        } else {
            ((FavouriteViewHolder) holder).bind(favourites.get(position - 1)); // Adjust for "+" position
        }
    }

    @Override
    public int getItemCount() {
        return favourites.size() + 1; // +1 for "+"
    }

    /**
     * ViewHolder for "+" button.
     */
    class AddFavouriteViewHolder extends RecyclerView.ViewHolder {
        AddFavouriteViewHolder(@NonNull View itemView) {
            super(itemView);
        }

        void bind() {
            itemView.setOnClickListener(v -> listener.onAddFavouriteClicked());
        }
    }

    /**
     * ViewHolder for favourite items.
     */
    class FavouriteViewHolder extends RecyclerView.ViewHolder {
        private final ImageView thumbnail;
        private final TextView fileName;

        FavouriteViewHolder(@NonNull View itemView) {
            super(itemView);
            thumbnail = itemView.findViewById(R.id.file_type_image_view);
            fileName = itemView.findViewById(R.id.file_name_text_view);
        }

        void bind(FavouritesModel favourite) {
            fileName.setText(favourite.getFileName());

            Glide.with(context)
                    .load(favourite.getThumbnailPath())
                    .placeholder(R.drawable.ic_image)
                    .into(thumbnail);

            itemView.setOnClickListener(v -> listener.onFavouriteClicked(favourite));

            itemView.setOnLongClickListener(v -> {
                showPopupMenu(v, favourite.getFilePath());
                return true;
            });
        }

        private void showPopupMenu(View view, String filePath) {
            PopupMenu popup = new PopupMenu(view.getContext(), view);
            popup.inflate(R.menu.favourites_menu);

            popup.setOnMenuItemClickListener(item -> {
                int itemId = item.getItemId();

                if (itemId == R.id.option_watermark) {
                    listener.onFavouriteLongPressed("addwatermark", filePath);
                    return true;
                } else if (itemId == R.id.option_share) {
                    listener.onFavouriteLongPressed("share", filePath);
                    return true;
                } else if (itemId == R.id.option_remove) {
                    listener.onFavouriteLongPressed("remove", filePath);
                    return true;
                }

                return false;
            });

            popup.show();
        }

    }
}
