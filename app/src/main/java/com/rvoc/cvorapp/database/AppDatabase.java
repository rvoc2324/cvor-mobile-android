package com.rvoc.cvorapp.database;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;

import com.rvoc.cvorapp.database.dao.FavouritesDao;
import com.rvoc.cvorapp.database.dao.ShareHistoryDao;
import com.rvoc.cvorapp.models.FavouritesModel;
import com.rvoc.cvorapp.models.ShareHistory;
import com.rvoc.cvorapp.utils.DateConverter;

// Annotate the class as a Room database
@Database(entities = {ShareHistory.class, FavouritesModel.class}, version = 2, exportSchema = false)
@TypeConverters({DateConverter.class})
public abstract class AppDatabase extends RoomDatabase {

    // Define DAOs
    public abstract ShareHistoryDao shareHistoryDao();
    public abstract FavouritesDao favouritesDao();

}
