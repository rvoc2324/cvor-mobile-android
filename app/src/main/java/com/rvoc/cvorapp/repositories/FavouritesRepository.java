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

    public void removeFavourite(String filePath) {
        new Thread(() -> favouritesDao.removeFavourite(filePath)).start();
    }

    public LiveData<List<FavouritesModel>> getAllFavourites() {
        return favouritesDao.getAllFavourites();
    }

    public boolean isFileFavourite(String filePath) {
        return favouritesDao.getFavouriteByPath(filePath) != null;
    }
}
