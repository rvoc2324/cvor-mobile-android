package com.rvoc.cvorapp.di;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;

import com.rvoc.cvorapp.adapters.DragAndDropAdapter;
import com.rvoc.cvorapp.adapters.FileActionListener;
import com.rvoc.cvorapp.adapters.FileListAdapter;
import com.rvoc.cvorapp.adapters.PreviewAdapter;
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

    // Provide OnFileActionListener
    @Provides
    @Singleton
    public FileListAdapter.OnFileActionListener provideFileActionListener() {
        return new FileActionListener();  // Provide the concrete implementation here
    }

    // Provide FileListAdapter
    @Provides
    @Singleton
    public FileListAdapter provideFileListAdapter(FileListAdapter.OnFileActionListener listener) {
        return new FileListAdapter(listener);
    }

    // Provide DragAndDropAdapter
    @Provides
    @Singleton
    public DragAndDropAdapter provideDragAndDropAdapter(DragAndDropAdapter.OnItemMoveListener listener) {
        return new DragAndDropAdapter(listener);
    }
}
