package com.rvoc.cvorapp.di;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;

import com.rvoc.cvorapp.adapters.FileActionListener;
import com.rvoc.cvorapp.adapters.FileListAdapter;
import com.rvoc.cvorapp.utils.PDFBoxInitialiser;
import com.rvoc.cvorapp.viewmodels.CoreViewModel;

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
    /*
    // Provide FileActionListener
    @Provides
    @Singleton
    public FileActionListener provideFileActionListener(CoreViewModel coreViewModel) {
        return new FileActionListener(coreViewModel);
    }

    // Provide FileListAdapter, injected with the OnFileActionListener
    @Provides
    @Singleton
    public FileListAdapter provideFileListAdapter(FileActionListener fileActionListener) {
        return new FileListAdapter(fileActionListener);
    }

    // Provide DragAndDropAdapter
    @Provides
    @Singleton
    public DragAndDropAdapter provideDragAndDropAdapter(DragAndDropAdapter.OnItemMoveListener listener) {
        return new DragAndDropAdapter(listener);
    }*/
}
