package com.rvoc.cvorapp.utils;

import androidx.recyclerview.widget.DiffUtil;
import java.util.List;

public class DiffCallBack<T> extends DiffUtil.Callback {

    private final List<T> oldList;
    private final List<T> newList;
    private final DiffUtilComparer<T> comparer;

    public interface DiffUtilComparer<T> {
        boolean areItemsTheSame(T oldItem, T newItem);
        boolean areContentsTheSame(T oldItem, T newItem);
    }

    public DiffCallBack(List<T> oldList, List<T> newList, DiffUtilComparer<T> comparer) {
        this.oldList = oldList;
        this.newList = newList;
        this.comparer = comparer;
    }

    @Override
    public int getOldListSize() {
        return oldList.size();
    }

    @Override
    public int getNewListSize() {
        return newList.size();
    }

    @Override
    public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
        return comparer.areItemsTheSame(oldList.get(oldItemPosition), newList.get(newItemPosition));
    }

    @Override
    public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
        return comparer.areContentsTheSame(oldList.get(oldItemPosition), newList.get(newItemPosition));
    }
}
