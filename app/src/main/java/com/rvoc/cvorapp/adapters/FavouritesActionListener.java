package com.rvoc.cvorapp.adapters;

import com.rvoc.cvorapp.models.FavouritesModel;

public interface FavouritesActionListener {
    void onFavouriteClicked(FavouritesModel favourite);

    void onFavouriteLongPressed(String actionType, String filePath);

    void onAddFavouriteClicked();
}
