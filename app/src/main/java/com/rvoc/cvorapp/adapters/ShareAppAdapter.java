package com.rvoc.cvorapp.adapters;

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.rvoc.cvorapp.databinding.ItemShareAppBinding;

import java.util.ArrayList;
import java.util.List;

public class ShareAppAdapter extends RecyclerView.Adapter<ShareAppAdapter.ViewHolder> {

    private final Context context;
    private final List<ResolveInfo> appList;
    private final PackageManager packageManager;
    private final OnAppSelectedListener listener;

    public interface OnAppSelectedListener {
        void onAppSelected(ResolveInfo appInfo);
    }

    public ShareAppAdapter(Context context, List<ResolveInfo> appList, OnAppSelectedListener listener) {
        this.context = context;
        this.packageManager = context.getPackageManager();
        this.listener = listener;
        this.appList = prioritizeApps(appList);
    }

    private List<ResolveInfo> prioritizeApps(List<ResolveInfo> apps) {
        List<ResolveInfo> prioritized = new ArrayList<>(apps);
        prioritized.sort((o1, o2) -> getPriority(o1) - getPriority(o2));
        return prioritized;
    }

    private int getPriority(ResolveInfo app) {
        String packageName = app.activityInfo.packageName;
        if (packageName.contains("whatsapp")) return 0;
        if (packageName.contains("gmail") || packageName.contains("email")) return 1;
        if (packageName.contains("telegram")) return 2;
        return 3; // Default priority for other apps
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemShareAppBinding binding = ItemShareAppBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ResolveInfo appInfo = appList.get(position);
        holder.binding.appName.setText(appInfo.loadLabel(packageManager));
        holder.binding.appIcon.setImageDrawable(appInfo.loadIcon(packageManager));

        holder.binding.getRoot().setOnClickListener(v -> listener.onAppSelected(appInfo));
    }

    @Override
    public int getItemCount() {
        return appList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemShareAppBinding binding;

        public ViewHolder(ItemShareAppBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
