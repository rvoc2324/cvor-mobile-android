package com.rvoc.cvorapp.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupMenu;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.rvoc.cvorapp.R;
import com.rvoc.cvorapp.adapters.FavouritesActionListener;
import com.rvoc.cvorapp.models.FavouritesModel;
import com.rvoc.cvorapp.databinding.ItemAddFavouriteBinding;
import com.rvoc.cvorapp.databinding.ItemFavouriteBinding;

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
            ItemAddFavouriteBinding binding = ItemAddFavouriteBinding.inflate(inflater, parent, false);
            return new AddFavouriteViewHolder(binding);
        } else {
            ItemFavouriteBinding binding = ItemFavouriteBinding.inflate(inflater, parent, false);
            return new FavouriteViewHolder(binding);
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
        private final ItemAddFavouriteBinding binding;

        AddFavouriteViewHolder(@NonNull ItemAddFavouriteBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind() {
            binding.getRoot().setOnClickListener(v -> listener.onAddFavouriteClicked());
        }
    }

    /**
     * ViewHolder for favourite items.
     */
    class FavouriteViewHolder extends RecyclerView.ViewHolder {
        private final ItemFavouriteBinding binding;

        FavouriteViewHolder(@NonNull ItemFavouriteBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(FavouritesModel favourite) {
            binding.fileNameTextView.setText(favourite.getFileName());

            Glide.with(context)
                    .load(favourite.getThumbnailPath())
                    .placeholder(R.drawable.ic_image)
                    .into(binding.fileTypeImageView);

            binding.getRoot().setOnClickListener(v -> listener.onFavouriteClicked(favourite));

            binding.getRoot().setOnLongClickListener(v -> {
                showPopupMenu(v, favourite.getFileUri());
                return true;
            });
        }

        private void showPopupMenu(View view, String fileUri) {
            PopupMenu popup = new PopupMenu(view.getContext(), view);
            popup.inflate(R.menu.favourites_menu);

            popup.setOnMenuItemClickListener(item -> {
                int itemId = item.getItemId();

                if (itemId == R.id.option_watermark) {
                    listener.onFavouriteLongPressed("directWatermark", fileUri);
                    return true;
                } else if (itemId == R.id.option_share) {
                    listener.onFavouriteLongPressed("directShare", fileUri);
                    return true;
                } else if (itemId == R.id.option_remove) {
                    listener.onFavouriteLongPressed("remove", fileUri);
                    return true;
                }
                return false;
            });

            popup.show();
        }
    }
}
