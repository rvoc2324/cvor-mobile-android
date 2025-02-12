package com.rvoc.cvorapp.repositories;

import androidx.lifecycle.LiveData;

import com.rvoc.cvorapp.database.dao.FavouritesDao;
import com.rvoc.cvorapp.models.FavouritesModel;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class FavouritesRepository {

    private final FavouritesDao favouritesDao;

    @Inject
    public FavouritesRepository(FavouritesDao favouritesDao) {
        this.favouritesDao = favouritesDao;
    }

    public void addFavourite(FavouritesModel favourite) {
        new Thread(() -> favouritesDao.addFavourite(favourite)).start();
    }

    public void removeFavourite(String fileUri) {
        new Thread(() -> favouritesDao.removeFavourite(fileUri)).start();
    }

    public LiveData<List<FavouritesModel>> getAllFavourites() {
        return favouritesDao.getAllFavourites();
    }

    public boolean isFileFavourite(String fileUri) {
        return favouritesDao.getFavouriteByUri(fileUri) != null;
    }
}
