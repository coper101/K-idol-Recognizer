package com.daryl.kidolrecognizer.RecyclerView;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.daryl.kidolrecognizer.Data.Idol;
import com.daryl.kidolrecognizer.R;
import com.google.android.material.card.MaterialCardView;

import java.util.List;

public class FaveIdolListAdapterWithRecyclerView
        extends RecyclerView.Adapter<FaveIdolListAdapterWithRecyclerView.IdolViewHolder> {

    private final List<Idol> idolList;
    private final Context context;
    private final int layoutResId;
    private OnItemCheckedChangeListener myListener;

    public FaveIdolListAdapterWithRecyclerView(List<Idol> idolList, Context context, int layoutResId) {
        this.idolList = idolList;
        this.context = context;
        this.layoutResId = layoutResId;
    }

    public interface OnItemCheckedChangeListener {
        void onCheckedChange(int position, boolean isChecked);
    }

    public void setOnItemCheckedChangeListener(OnItemCheckedChangeListener onItemCheckedChangeListener) {
        myListener = onItemCheckedChangeListener;
    }

    @NonNull
    @Override
    public IdolViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(layoutResId, parent, false);
        return new IdolViewHolder(view, myListener);
    }

    @Override
    public void onBindViewHolder(@NonNull IdolViewHolder holder, int position) {
        final Idol idol = idolList.get(position);
        holder.idolStageName.setText(idol.getStageName());
        holder.idolGroupName.setText(idol.getGroup());
        holder.idolFaveButton.setChecked(true);
    }

    @Override
    public int getItemCount() {
        return idolList.size();
    }

    // Holder
    static class IdolViewHolder extends RecyclerView.ViewHolder {

        ImageView idolFaceIV;
        MaterialCardView idolFaceCard;
        TextView idolStageName, idolGroupName;
        CheckBox idolFaveButton;

        public IdolViewHolder(@NonNull View itemView, final OnItemCheckedChangeListener listener) {
            super(itemView);
            idolFaceIV = itemView.findViewById(R.id.idol_face_image_view);
            idolFaceCard = itemView.findViewById(R.id.idol_face_card_view);
            idolStageName = itemView.findViewById(R.id.idol_stage_name_text_view);
            idolGroupName = itemView.findViewById(R.id.idol_group_name_text_view);
            idolFaveButton = itemView.findViewById(R.id.idol_favorite_button);
            idolFaveButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    if (listener != null) {
                        int position = getAdapterPosition();
                        if (position != RecyclerView.NO_POSITION)
                            listener.onCheckedChange(position, isChecked);
                    }
                }
            });
        }
    }

}
