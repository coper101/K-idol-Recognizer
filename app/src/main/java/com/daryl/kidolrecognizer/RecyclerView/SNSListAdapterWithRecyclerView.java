package com.daryl.kidolrecognizer.RecyclerView;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.daryl.kidolrecognizer.Data.SNS;
import com.daryl.kidolrecognizer.R;
import com.google.android.material.card.MaterialCardView;

import java.util.List;

// Reference:
// Click An Item | Youtube: Coding In Flow
// Understanding Recycler View | Youtube: Code Tutor
public class SNSListAdapterWithRecyclerView extends
        RecyclerView.Adapter<SNSListAdapterWithRecyclerView.SNSViewHolder> {

    private final List<SNS> snsList;
    private final Context context;
    private final int layoutResId;
    private OnItemClickListener myListener;

    public SNSListAdapterWithRecyclerView(List<SNS> snsList, Context context, int layoutResId) {
        this.snsList = snsList;
        this.context = context;
        this.layoutResId = layoutResId;
    }

    public interface OnItemClickListener {
        // Method to be override by the Activity when listener is set
        void onItemClick(int position);
    }

    // Implement the View.onClickListener in the Activity
    public void setOnItemClickListener(OnItemClickListener onItemClickListener) {
        myListener = onItemClickListener;
    }

    @NonNull
    @Override
    public SNSViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(layoutResId, parent, false);
        return new SNSViewHolder(view, myListener);
    }

    @Override
    public void onBindViewHolder(@NonNull SNSViewHolder holder, int position) {
        final SNS sns = snsList.get(position);
        holder.usernameTV.setText(sns.getUsername());
        holder.platformTV.setText(sns.getPlatform());
        // Add Left Margin to First Item View
        if (position == 0) {
            RecyclerView.LayoutParams params = (RecyclerView.LayoutParams) holder.itemView.getLayoutParams();
            float density = context.getResources().getDisplayMetrics().density;
            float px = 25 * density;
            params.leftMargin = (int) px;
            holder.itemView.setLayoutParams(params);
        }
    }

    @Override
    public int getItemCount() {
        return snsList.size();
    }


    // Class Holder
    public class SNSViewHolder extends RecyclerView.ViewHolder {

        TextView usernameTV, platformTV;
        MaterialCardView usernameCV;

        public SNSViewHolder(@NonNull View itemView, final OnItemClickListener listener) {
            super(itemView);
            usernameTV = itemView.findViewById(R.id.username_text_view);
            platformTV = itemView.findViewById(R.id.platform_text_view);
            usernameCV = itemView.findViewById(R.id.username_card_view);
            usernameCV.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (listener != null) {
                        int position = getAdapterPosition();
                        if (position != RecyclerView.NO_POSITION)
                            listener.onItemClick(position);
                    }
                }
            });
        }
    }

}
