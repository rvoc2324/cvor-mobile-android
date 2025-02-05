package com.rvoc.cvorapp.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.rvoc.cvorapp.models.ShareHistory;
import com.rvoc.cvorapp.repositories.ShareHistoryRepository;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.inject.Inject;

public class ShareHistoryViewModel extends ViewModel {
    private final ShareHistoryRepository repository;
    private final MutableLiveData<List<ShareHistory>> shareHistoryLiveData = new MutableLiveData<>();
    private final MutableLiveData<List<ShareHistory>> filteredShareHistoryLiveData = new MutableLiveData<>();
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    @Inject
    public ShareHistoryViewModel(ShareHistoryRepository repository) {
        this.repository = repository;
        loadShareHistory();
    }

    public void loadShareHistory() {
        executorService.execute(() -> {
            List<ShareHistory> historyList = repository.getAllShareHistory();
            shareHistoryLiveData.postValue(historyList);
            filteredShareHistoryLiveData.postValue(historyList); // Initially, filtered list is the same
        });
    }

    public LiveData<List<ShareHistory>> getFilteredShareHistory() {
        return filteredShareHistoryLiveData;
    }

    public void filterShareHistory(String query, Date fromDate, Date toDate) {
        executorService.execute(() -> {
            List<ShareHistory> allHistory = shareHistoryLiveData.getValue();
            if (allHistory == null) return;

            List<ShareHistory> filteredList = new ArrayList<>();
            for (ShareHistory entry : allHistory) {
                boolean matchesQuery = query == null || query.isEmpty() ||
                        entry.getFileName().toLowerCase().contains(query.toLowerCase()) ||
                        entry.getSharedWith().toLowerCase().contains(query.toLowerCase()) ||
                        entry.getShareMedium().toLowerCase().contains(query.toLowerCase()) ||
                        entry.getPurpose().toLowerCase().contains(query.toLowerCase());

                boolean matchesDateRange = (fromDate == null || !entry.getSharedDate().before(fromDate)) &&
                        (toDate == null || !entry.getSharedDate().after(toDate));

                if (matchesQuery && matchesDateRange) {
                    filteredList.add(entry);
                }
            }
            filteredShareHistoryLiveData.postValue(filteredList);
        });
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        executorService.shutdown();
    }
}
