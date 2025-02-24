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
@Database(entities = {ShareHistory.class, FavouritesModel.class}, version = 4, exportSchema = false)
@TypeConverters({DateConverter.class})
public abstract class AppDatabase extends RoomDatabase {

    // Define DAOs
    public abstract ShareHistoryDao shareHistoryDao();
    public abstract FavouritesDao favouritesDao();

    // Migration from version 3 to 4
    public static final Migration MIGRATION_3_4 = new Migration(3, 4) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            // Create new Share History table
            database.execSQL("CREATE TABLE IF NOT EXISTS ShareHistory_new (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "fileName TEXT NOT NULL, " +
                    "sharedDate INTEGER NOT NULL, " +
                    "shareMedium TEXT NOT NULL, " +
                    "sharedWith TEXT NOT NULL, " +
                    "purpose TEXT NOT NULL, " +
                    "filePath TEXT)"); // New column added

            // Step 2: Copy existing data from old ShareHistory
            database.execSQL("INSERT INTO ShareHistory_new (id, fileName, sharedDate, shareMedium, sharedWith, purpose) " +
                    "SELECT id, fileName, sharedDate, shareMedium, sharedWith, purpose FROM ShareHistory");

            // Step 3: Drop old ShareHistory table
            database.execSQL("DROP TABLE ShareHistory");

            // Step 4: Rename new table to original name
            database.execSQL("ALTER TABLE ShareHistory_new RENAME TO ShareHistory");
        }
    };
}
