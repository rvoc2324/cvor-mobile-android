package com.rvoc.cvorapp.services;

import android.content.Context;
import android.net.Uri;

import androidx.lifecycle.LiveData;

import com.rvoc.cvorapp.models.FavouritesModel;
import com.rvoc.cvorapp.repositories.FavouritesRepository;
import com.rvoc.cvorapp.utils.FileUtils;

import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.hilt.android.qualifiers.ApplicationContext;

@Singleton
public class FavouritesService {

    private final Context context;
    private final FavouritesRepository favouritesRepository;
    private final Executor executor = Executors.newSingleThreadExecutor(); // Background execution

    @Inject
    public FavouritesService(FavouritesRepository favouritesRepository, @ApplicationContext Context context) {
        this.favouritesRepository = favouritesRepository;
        this.context = context;
    }

    /**
     * Get all favourite items as LiveData to observe changes in real-time.
     */
    public LiveData<List<FavouritesModel>> getFavourites() {
        return favouritesRepository.getAllFavourites();
    }

    /**
     * Adds a file to favourites.
     * Ensures no duplicate entries.
     */
    public void addToFavourites(String fileUri, String thumbnailPath) {
        executor.execute(() -> {
            if (!favouritesRepository.isFileFavourite(fileUri)) {
                long timestamp = System.currentTimeMillis(); // Capture current timestamp
                FavouritesModel favourite = new FavouritesModel(
                        fileUri,
                        extractFileName(fileUri),
                        // extractFileType(fileUri),
                        thumbnailPath,
                        timestamp
                );
                favouritesRepository.addFavourite(favourite);
            }
        });
    }

    /**
     * Removes a file from favourites.
     */
    public void removeFromFavourites(String fileUri) {
        executor.execute(() -> favouritesRepository.removeFavourite(fileUri));
    }

    private String extractFileName(String fileUri) {
        Uri uri = Uri.parse(fileUri);
        return FileUtils.getFileNameFromUri(context, uri);
    }

    private String extractFileType(String fileUri) {
        return fileUri.endsWith(".pdf") ? "PDF" : "Image"; // Extend for other formats
    }

}
