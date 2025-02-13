package com.rvoc.cvorapp.di;

import android.content.Context;

import androidx.room.Room;

import com.rvoc.cvorapp.database.AppDatabase;
import com.rvoc.cvorapp.database.dao.FavouritesDao;
import com.rvoc.cvorapp.database.dao.ShareHistoryDao;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.components.SingletonComponent;
import dagger.hilt.android.qualifiers.ApplicationContext;

@Module
@InstallIn(SingletonComponent.class)
public class DatabaseModule {

    @Provides
    @Singleton
    public static AppDatabase provideDatabase(@ApplicationContext Context context) {
        return Room.databaseBuilder(context, AppDatabase.class, "app_database")
                .addMigrations(AppDatabase.MIGRATION_2_3) // Handles version upgrades
                .build();
    }

    @Provides
    public static ShareHistoryDao provideShareHistoryDao(AppDatabase database) {
        return database.shareHistoryDao();
    }

    @Provides
    public static FavouritesDao provideFavouritesDao(AppDatabase database) {
        return database.favouritesDao();
    }
}
