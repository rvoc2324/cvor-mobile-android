package com.rvoc.cvorapp.database.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.rvoc.cvorapp.models.FavouritesModel;

import java.util.List;

@Dao
public interface FavouritesDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void addFavourite(FavouritesModel favourite);

    @Query("DELETE FROM favourites WHERE fileUri = :fileUri")
    void removeFavourite(String fileUri);

    @Query("SELECT * FROM favourites ORDER BY addedTimestamp DESC")
    LiveData<List<FavouritesModel>> getAllFavourites();

    @Query("SELECT * FROM favourites WHERE fileUri = :fileUri LIMIT 1")
    FavouritesModel getFavouriteByUri(String fileUri);

}
