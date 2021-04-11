package com.daryl.kidolrecognizer.RecyclerView;

import android.content.Context;
import android.os.Build;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.daryl.kidolrecognizer.Data.Idol;
import com.daryl.kidolrecognizer.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

import java.util.List;

public class IdolListAdapterWithRecyclerView
        extends RecyclerView.Adapter<IdolListAdapterWithRecyclerView.IdolViewHolder> {

    private final List<Idol> idolList;
    private final Context context;
    private final int layoutResId;
    private OnItemCheckedChangeListener myListener;
    private OnItemClickedListener myClickedListener;

    public IdolListAdapterWithRecyclerView(List<Idol> idolList, Context context, int layoutResId) {
        this.idolList = idolList;
        this.context = context;
        this.layoutResId = layoutResId;
    }

    // Track Changes in State of Checkbox Button
    public interface OnItemCheckedChangeListener {
        void onCheckedChange(int position, boolean isChecked, CheckBox favoriteBtn);
    }

    public void setOnItemCheckedChangeListener(OnItemCheckedChangeListener onItemCheckedChangeListener) {
        myListener = onItemCheckedChangeListener;
    }

    // Track Item View is Clicked
    public void setOnItemClickedListener(OnItemClickedListener onItemClickedListener) {
        myClickedListener = onItemClickedListener;
    }

    public interface OnItemClickedListener {
        void onItemClicked(int position);
    }


    @NonNull
    @Override
    public IdolViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(layoutResId, parent, false);
        return new IdolViewHolder(view, myListener, myClickedListener);
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void onBindViewHolder(@NonNull IdolViewHolder holder, int position) {
        final Idol idol = idolList.get(position);
        holder.idolStageName.setText(idol.getStageName());
        holder.idolGroupName.setText(idol.getGroup());
        holder.idolFaveButton.setChecked(idol.isFavorite());

        RecyclerView.LayoutParams params = (RecyclerView.LayoutParams) holder.itemView.getLayoutParams();
        // even element - add margin of 14 to left and 6.5 to right
        if (position % 2 == 0) {
            params.leftMargin = toPx(14);
            params.rightMargin = toPx(7);
        }
        // odd element - add margin of 6.5 to left and 14 to right
        else {
            params.leftMargin = toPx(7);
            params.rightMargin = toPx(14);
        }
        // Topmost Margin
        if (position == 0 || position == 1) {
            params.topMargin = toPx(24);
        }
        holder.itemView.setLayoutParams(params);

        // Set Idol Image Using Url (Glide)
        String idolImageUrl = idol.getImageUrl();
        ImageView idolFaceIV = holder.idolFaceIV;
        if (idolImageUrl != null) {
            Glide.with(context)
                    .load(idolImageUrl)
                    .transition(DrawableTransitionOptions.withCrossFade(1000))
                    .into(idolFaceIV);
        } else {
            Glide.with(context)
                    .clear(idolFaceIV);
        }
    }

    private int toPx(int dp) {
        float density = context.getResources().getDisplayMetrics().density;
        int px = (int) (dp * density);
        return px;
    }

    @Override
    public int getItemCount() {
        return idolList.size();
    }

    // Holder
    static class IdolViewHolder extends RecyclerView.ViewHolder {

        MaterialCardView idolCard;
        ImageView idolFaceIV;
        TextView idolStageName, idolGroupName;
        CheckBox idolFaveButton;

        public IdolViewHolder(@NonNull View itemView,
                              final OnItemCheckedChangeListener listener,
                              final OnItemClickedListener clickedListener) {
            super(itemView);
            idolCard = itemView.findViewById(R.id.idol_card_view);
            idolFaceIV = itemView.findViewById(R.id.idols_idol_image_view);
            idolStageName = itemView.findViewById(R.id.idols_idol_stage_name_text_view);
            idolGroupName = itemView.findViewById(R.id.idols_idol_group_name_text_view);
            idolFaveButton = itemView.findViewById(R.id.idols_idol_favorite_button);
            // Track Changes in State of Checkbox Button
            idolFaveButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    if (listener != null) {
                        int position = getAdapterPosition();
                        if (position != RecyclerView.NO_POSITION)
                            listener.onCheckedChange(position, isChecked, idolFaveButton);
                    }
                }
            });
            // Track Item View is Clicked
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (clickedListener != null) {
                        int position = getAdapterPosition();
                        if (position != RecyclerView.NO_POSITION)
                            clickedListener.onItemClicked(position);
                    }
                }
            });
        }
    } // end of view holder class

} // end of class
