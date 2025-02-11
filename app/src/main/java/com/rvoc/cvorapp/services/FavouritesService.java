package com.rvoc.cvorapp.services;

import androidx.lifecycle.LiveData;

import com.rvoc.cvorapp.models.FavouritesModel;
import com.rvoc.cvorapp.repositories.FavouritesRepository;

import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class FavouritesService {

    private final FavouritesRepository favouritesRepository;
    private final Executor executor = Executors.newSingleThreadExecutor(); // Background execution

    @Inject
    public FavouritesService(FavouritesRepository favouritesRepository) {
        this.favouritesRepository = favouritesRepository;
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
    public void addToFavourites(String filePath, String thumbnailPath) {
        executor.execute(() -> {
            if (!favouritesRepository.isFileFavourite(filePath)) {
                long timestamp = System.currentTimeMillis(); // Capture current timestamp
                FavouritesModel favourite = new FavouritesModel(
                        filePath,
                        extractFileName(filePath),
                        extractFileType(filePath),
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
    public void removeFromFavourites(String filePath) {
        executor.execute(() -> favouritesRepository.removeFavourite(filePath));
    }

    private String extractFileName(String filePath) {
        return filePath.substring(filePath.lastIndexOf('/') + 1);
    }

    private String extractFileType(String filePath) {
        return filePath.endsWith(".pdf") ? "PDF" : "Image"; // Extend for other formats
    }

}
