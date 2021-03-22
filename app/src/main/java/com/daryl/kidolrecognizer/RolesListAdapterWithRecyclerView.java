package com.daryl.kidolrecognizer;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class RolesListAdapterWithRecyclerView extends RecyclerView.Adapter<RolesListAdapterWithRecyclerView.RoleViewHolder> {

    private final List<Role> roles;
    private final Context context;
    private final int layoutResId;

    public RolesListAdapterWithRecyclerView(List<Role> roles, Context context, int layoutResId) {
        this.roles = roles;
        this.context = context;
        this.layoutResId = layoutResId;
    }

    @NonNull
    @Override
    public RolesListAdapterWithRecyclerView.RoleViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(layoutResId, parent, false);
        return new RoleViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RolesListAdapterWithRecyclerView.RoleViewHolder holder, int position) {
        final Role role = roles.get(position);
        holder.roleTV.setText(role.getRoleName());
    }

    @Override
    public int getItemCount() {
        return roles.size();
    }

    static class RoleViewHolder extends RecyclerView.ViewHolder {

        TextView roleTV;

        public RoleViewHolder(@NonNull View itemView) {
            super(itemView);
            roleTV = itemView.findViewById(R.id.role_text_view);
        }
    }

}
