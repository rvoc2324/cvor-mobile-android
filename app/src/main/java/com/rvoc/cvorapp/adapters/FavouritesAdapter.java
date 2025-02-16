package com.rvoc.cvorapp.adapters;

import android.content.Context;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import android.widget.PopupWindow;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.rvoc.cvorapp.R;

import com.rvoc.cvorapp.models.FavouritesModel;
import com.rvoc.cvorapp.databinding.ItemAddFavouriteBinding;
import com.rvoc.cvorapp.databinding.ItemFavouriteBinding;
import com.rvoc.cvorapp.utils.DiffCallBack;

import java.util.ArrayList;
import java.util.List;

public class FavouritesAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final String TAG = "Favourites Adapter";
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
        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(
                new DiffCallBack<>(favourites, newFavourites, new DiffCallBack.DiffUtilComparer<>() {
                    @Override
                    public boolean areItemsTheSame(FavouritesModel oldItem, FavouritesModel newItem) {
                        return oldItem.getFilePath().equals(newItem.getFilePath());
                    }

                    @Override
                    public boolean areContentsTheSame(FavouritesModel oldItem, FavouritesModel newItem) {
                        return oldItem.equals(newItem);
                    }
                })
        );
        favourites.clear();
        favourites.addAll(newFavourites);
        diffResult.dispatchUpdatesTo(this);
    }

    @Override
    public int getItemViewType(int position) {
        return position == 0 ? TYPE_ADD : TYPE_FAVOURITE;
    }

    /*
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
    }*/

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        ItemFavouriteBinding binding = ItemFavouriteBinding.inflate(inflater, parent, false);
        return new FavouriteViewHolder(binding);

    }

    /* @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof AddFavouriteViewHolder) {
            ((AddFavouriteViewHolder) holder).bind();
        } else {
            ((FavouriteViewHolder) holder).bind(favourites.get(position - 1)); // Adjust for "+" position
        }
    }*/

    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ((FavouriteViewHolder) holder).bind(favourites.get(position));
    }

    @Override
    public int getItemCount() {
        // return favourites.size() + 1; // +1 for "+"
        return favourites.size();
    }

    /*
     * ViewHolder for "+" button.

    class AddFavouriteViewHolder extends RecyclerView.ViewHolder {
        private final ItemAddFavouriteBinding binding;

        AddFavouriteViewHolder(@NonNull ItemAddFavouriteBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind() {
            binding.getRoot().setOnClickListener(v -> listener.onAddFavouriteClicked());
        }
    }*/

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
            binding.fileNameTextView.setSelected(false);
            binding.fileNameTextView.setSelected(true);

            Glide.with(context)
                    .load(favourite.getThumbnailPath())
                    .placeholder(R.drawable.ic_image)
                    .into(binding.fileTypeImageView);

            binding.getRoot().setOnClickListener(v -> listener.onFavouriteClicked(favourite));

            binding.getRoot().setOnLongClickListener(v -> {
                showPopupMenu(v, favourite.getFilePath(), favourite.getThumbnailPath());
                return true;
            });
        }

        private void showPopupMenu(View view, String filePath, String thumbnailPath) {
            // Inflate the custom menu layout
            LayoutInflater inflater = LayoutInflater.from(view.getContext());
            View popupView = inflater.inflate(R.layout.custom_popup_menu, new FrameLayout(context), false);


            // Get screen width in pixels
            final PopupWindow popupWindow = getPopupWindow(view, popupView);

            // Measure the height of the popup
            popupView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
            int popupHeight = popupView.getMeasuredHeight();

            // Calculate available space below and above the anchor view
            int[] location = new int[2];
            view.getLocationOnScreen(location);
            int anchorBottomY = location[1] + view.getHeight(); // Bottom Y-coordinate of the anchor
            int screenHeight = view.getResources().getDisplayMetrics().heightPixels;
            int spaceBelow = screenHeight - anchorBottomY; // Space below the view
            int spaceAbove = location[1]; // Space above the view

            // Determine whether to show the popup above or below the anchor view
            if (spaceBelow >= popupHeight) {
                // Enough space below, show as usual
                popupWindow.showAsDropDown(view);
            } else if (spaceAbove >= popupHeight) {
                // Not enough space below, show above the view
                popupWindow.showAsDropDown(view, 0, -view.getHeight() - popupHeight);
            } else {
                // Limited space both above and below, show as best as possible
                popupWindow.showAsDropDown(view);
            }

            // Set click listeners for each option
            popupView.findViewById(R.id.option_watermark).setOnClickListener(v -> {
                listener.onFavouriteLongPressed("directWatermark", filePath, null);
                popupWindow.dismiss();
            });

            popupView.findViewById(R.id.option_share).setOnClickListener(v -> {
                listener.onFavouriteLongPressed("directShare", filePath, null);
                popupWindow.dismiss();
            });

            popupView.findViewById(R.id.option_change_file_name).setOnClickListener(v -> {
                listener.onFavouriteLongPressed("changeFileName", filePath, null);
                popupWindow.dismiss();
            });

            popupView.findViewById(R.id.option_remove).setOnClickListener(v -> {
                listener.onFavouriteLongPressed("remove", filePath, thumbnailPath);
                popupWindow.dismiss();
            });
        }

    }

    @NonNull
    private static PopupWindow getPopupWindow(View view, View popupView) {
        DisplayMetrics displayMetrics = view.getContext().getResources().getDisplayMetrics();
        int screenWidth = displayMetrics.widthPixels;

        // Set popup width as 50% of the screen width
        int popupWidth = (int) (screenWidth * 0.5);

        // Create PopupWindow
        final PopupWindow popupWindow = new PopupWindow(popupView,
                popupWidth,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                true);
        popupWindow.setElevation(8); // Add shadow for better UI effect
        return popupWindow;
    }
}
