package com.rvoc.cvorapp.repositories;

import android.content.Context;

import com.rvoc.cvorapp.database.AppDatabase;
import com.rvoc.cvorapp.database.dao.ShareHistoryDao;
import com.rvoc.cvorapp.models.ShareHistory;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class ShareHistoryRepository {

    private final ShareHistoryDao shareHistoryDao;

    @Inject
    public ShareHistoryRepository(ShareHistoryDao shareHistoryDao) {
        this.shareHistoryDao = shareHistoryDao;
    }

    public void insertShareHistory(ShareHistory shareHistory) {
        new Thread(() -> shareHistoryDao.insert(shareHistory)).start();
    }

    public void insertAllShareHistory(List<ShareHistory> shareHistories) {
        new Thread(() -> shareHistoryDao.insertAll(shareHistories)).start();
    }

    public List<ShareHistory> getAllShareHistory() {
        return shareHistoryDao.getAllShareHistory();
    }

    public List<ShareHistory> getShareHistoryByFileName(String fileName) {
        return shareHistoryDao.getShareHistoryByFileName(fileName);
    }

    public List<ShareHistory> getShareHistoryByShareMedium(String shareMedium) {
        return shareHistoryDao.getShareHistoryByShareMedium(shareMedium);
    }

    public List<ShareHistory> getShareHistoryByDateRange(long startDate, long endDate) {
        return shareHistoryDao.getShareHistoryByDateRange(startDate, endDate);
    }

    public ShareHistory getLatestShareHistory() {
        return shareHistoryDao.getLatestShareHistory();
    }

    public void deleteShareHistory(ShareHistory shareHistory) {
        new Thread(() -> shareHistoryDao.delete(shareHistory)).start();
    }

    public void deleteAllShareHistory() {
        new Thread(shareHistoryDao::deleteAll).start();
    }
}
