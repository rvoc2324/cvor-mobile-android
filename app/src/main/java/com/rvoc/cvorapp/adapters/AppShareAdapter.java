import android.content.pm.ResolveInfo;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.rvoc.cvorapp.R;

import java.util.List;

public class ShareAppsAdapter extends RecyclerView.Adapter<ShareAppsAdapter.AppViewHolder> {

    private List<ResolveInfo> appList;
    private OnAppSelectedListener listener;

    public interface OnAppSelectedListener {
        void onAppSelected(ResolveInfo app);
    }

    public ShareAppsAdapter(List<ResolveInfo> appList, OnAppSelectedListener listener) {
        this.appList = appList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public AppViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_share_app, parent, false);
        return new AppViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AppViewHolder holder, int position) {
        ResolveInfo app = appList.get(position);
        holder.bind(app);
    }

    @Override
    public int getItemCount() {
        return appList.size();
    }

    public class AppViewHolder extends RecyclerView.ViewHolder {
        private TextView appName;

        public AppViewHolder(View itemView) {
            super(itemView);
            appName = itemView.findViewById(R.id.appName);
        }

        public void bind(ResolveInfo app) {
            appName.setText(app.loadLabel(requireContext().getPackageManager()));

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onAppSelected(app);
                }
            });
        }
    }
}
