package com.rvoc.cvorapp.adapters;

import androidx.annotation.Nullable;

import com.rvoc.cvorapp.models.FavouritesModel;

public interface FavouritesActionListener {
    void onFavouriteClicked(FavouritesModel favourite);

    void onFavouriteLongPressed(String actionType, String filePath, @Nullable String thumbnailPath);

    void onAddFavouriteClicked();
}
