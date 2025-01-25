package com.rvoc.cvorapp.di;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;

import com.rvoc.cvorapp.utils.PDFBoxInitialiser;

import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.components.SingletonComponent;

import javax.inject.Singleton;

@Module
@InstallIn(SingletonComponent.class)
public class AppModule {

    @Provides
    @Singleton
    public Context provideContext(Application application) {
        return application.getApplicationContext();
    }

    @Provides
    @Singleton
    public SharedPreferences provideSharedPreferences(Context context) {
        return context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE);
    }

    @Provides
    @Singleton
    public PDFBoxInitialiser providePDFBoxInitialiser(Context context) {
        return new PDFBoxInitialiser(context);
    }
}
