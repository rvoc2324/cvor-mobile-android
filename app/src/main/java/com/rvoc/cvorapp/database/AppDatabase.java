package com.rvoc.cvorapp.database;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

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

    // Migration from version 1 to 2
    public static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            // Create the new Favourites table
            database.execSQL("CREATE TABLE IF NOT EXISTS favourites (fileUri TEXT PRIMARY KEY NOT NULL, thumbnailPath TEXT)");
        }
    };

}
