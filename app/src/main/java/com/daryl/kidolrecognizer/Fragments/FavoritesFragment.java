package com.daryl.kidolrecognizer.Fragments;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.chaquo.python.PyObject;
import com.daryl.kidolrecognizer.Activities.HomeActivity;
import com.daryl.kidolrecognizer.Data.Idol;
import com.daryl.kidolrecognizer.Data.MyData;
import com.daryl.kidolrecognizer.R;
import com.daryl.kidolrecognizer.RecyclerView.FaveIdolListAdapterWithRecyclerView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class FavoritesFragment extends Fragment implements View.OnClickListener {

    private static final String TAG = FavoritesFragment.class.getSimpleName();

    // Data
    private final MyData myData = MyData.getMyData();

    // Recycler View Components
    private FaveIdolListAdapterWithRecyclerView faveIdolsAdapterRV;
    private ArrayList<Idol> faveIdolList;
    private RecyclerView faveIdolsRV;
    private LinearLayoutManager layoutManager;

    // Rearranging List Items
    private final ItemTouchHelper itemTouchHelper = new ItemTouchHelper(new TouchHelper());

    // Views
    private LinearLayout emptyFaveIllus;
    private MaterialButton galleryLinkBtn, rearrangeBtn;
    private LinearLayout rearrangeContainer;

    // ===========================================================================================
    // Unable to Access Views here
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.e(TAG, "onCreate");
        // Initialize Recycle View Components
        faveIdolList = new ArrayList<>();
        faveIdolsAdapterRV = new FaveIdolListAdapterWithRecyclerView(faveIdolList, getContext(), R.layout.fave_idol_item);
        layoutManager = new LinearLayoutManager(getContext(), RecyclerView.VERTICAL, false);
    }

    // ===========================================================================================
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Log.e(TAG, "onCreateView");
        View view = inflater.inflate(R.layout.fragment_favorites, container, false);
        // Recycler View Components
        faveIdolsRV = view.findViewById(R.id.favorite_idols_recycler_view);
        faveIdolsRV.setAdapter(faveIdolsAdapterRV);
        faveIdolsRV.setLayoutManager(layoutManager);
        faveIdolsAdapterRV.setOnItemCheckedChangeListener(new FaveIdolListAdapterWithRecyclerView.OnItemCheckedChangeListener() {
            @Override
            public void onCheckedChange(int position, boolean isChecked) {

                if (!isChecked) {
                    Toast.makeText(getContext(), "Unchecked", Toast.LENGTH_SHORT).show();

                    PyObject mainModule = myData.getMainModule();
                    if (mainModule != null) {

                        // Update Favorite Column of Idol to False
                        String id = faveIdolList.get(position).getId();
                        String boolVal = "False";
                        PyObject isUpdatedStr = mainModule.callAttr("update_favorite", id, boolVal);
                        boolean isUpdated = isUpdatedStr.toBoolean();
                        Log.e(TAG, "Is Favorites Updated: " + isUpdated);

                        // Remove Idol from List Recycler View
                        faveIdolList.remove(position);
                        faveIdolsAdapterRV.notifyDataSetChanged();
                        Snackbar.make(getView(), "Removed from Favorites.", Snackbar.LENGTH_SHORT)
                                .setAnchorView(getActivity().findViewById(R.id.custom_bottom_navigation))
                                .show();
                        showEmptyIllustration();

                    } // end of checking main module is not null

                } // end of checking is not checked

            } // end of onCheckedChange
        });
        // Empty Illustration
        emptyFaveIllus = view.findViewById(R.id.empty_favorites_illustration);
        galleryLinkBtn = view.findViewById(R.id.gallery_link_button);
        rearrangeContainer = view.findViewById(R.id.rearrange_box);
        rearrangeBtn = view.findViewById(R.id.rearrange_button);
        rearrangeBtn.setOnClickListener(this);
        return view;
    }

    // ===========================================================================================
    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        Log.e(TAG, "onActivityCreated");

        // Hide Views
        galleryLinkBtn.setVisibility(View.GONE);
        rearrangeContainer.setVisibility(View.GONE);

        // Get Favorite Idols
        Thread loadFavoritesThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    PyObject mainModule = myData.getMainModule();
                    if (mainModule != null) {
                        Log.e(TAG, "Main Module is NOT null");

                        // Populate List Recycler View
                        populateFavoriteIdols();

                        break;
                    }
                }
            }
        });
        loadFavoritesThread.start();
    }

    // ===========================================================================================
    @Override
    public void onResume() {
        super.onResume();
        Log.e(TAG, "onResume");
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.e(TAG, "onPause");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.e(TAG, "onDestroy");
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        Log.e(TAG, "onHiddenChanged: hidden? " + hidden);
        PyObject mainModule = myData.getMainModule();
        if (!hidden && mainModule != null) {
            Log.e(TAG, "onHiddenChanged: Main Module is NOT null");
            // Populate List Recycler View
            populateFavoriteIdols();
        }
    }

    // ===========================================================================================
    private void populateFavoriteIdols() {
        PyObject mainModule = myData.getMainModule();
        PyObject faveIdols = mainModule.callAttr("get_favorite_idols");
        List<PyObject> faveIdolsList = faveIdols.asList();
        Log.e(TAG, "Size: " + faveIdolsList.size());

        faveIdolList.clear();
        // Add Idols
        for (PyObject faveIdol: faveIdolsList) {
            List<PyObject> faveIdolValues = faveIdol.asList();
            String id = faveIdolValues.get(0).toInt() + "";
            String stageName = faveIdolValues.get(1).toString();
            String groupName = faveIdolValues.get(2).toString();
            Idol idol = new Idol(id, stageName, groupName);
            faveIdolList.add(idol);
        }
        // Update List Recycler View
        if (faveIdolList.size() > 0) {
            updateIdolsAdapter();
        }

        // Show Empty State
        showEmptyIllustration();
    }

    private void updateIdolsAdapter() {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                faveIdolsAdapterRV.notifyDataSetChanged();
            }
        });
    }

    private void updateVisibility(View view, boolean isVisible) {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (isVisible) {
                    view.setVisibility(View.VISIBLE);
                } else {
                    view.setVisibility(View.GONE);
                }
            }
        });
    }

    // ===========================================================================================
    private void showEmptyIllustration() {
        if (faveIdolList.size() > 0) {
            updateVisibility(emptyFaveIllus, false);
            updateVisibility(galleryLinkBtn, true);
            updateVisibility(rearrangeContainer, true);
//            emptyFaveIllus.setVisibility(View.GONE);
//            galleryLinkBtn.setVisibility(View.VISIBLE);
//            rearrangeContainer.setVisibility(View.VISIBLE);
        } else {
            updateVisibility(emptyFaveIllus, true);
            updateVisibility(galleryLinkBtn, false);
            updateVisibility(rearrangeContainer, false);
//            emptyFaveIllus.setVisibility(View.VISIBLE);
//            galleryLinkBtn.setVisibility(View.GONE);
//            rearrangeContainer.setVisibility(View.GONE);
        }
    }

    // ===========================================================================================
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.rearrange_button:
                if (rearrangeBtn.getText().equals("Done")) {
                    itemTouchHelper.attachToRecyclerView(null);
                    rearrangeBtn.setText("Rearrange");
                } else if (rearrangeBtn.getText().equals("Rearrange")) {
                    rearrangeBtn.setText("Done");
                    itemTouchHelper.attachToRecyclerView(faveIdolsRV);
                }
                break;
        }
    }

    // ===========================================================================================
    // Reference: Youtube - Drag and drop Reorder in Recycler View | Android
    private class TouchHelper extends ItemTouchHelper.SimpleCallback {

//        /**
//         * Creates a Callback for the given drag and swipe allowance. These values serve as
//         * defaults
//         * and if you want to customize behavior per ViewHolder, you can override
//         * {@link #getSwipeDirs(RecyclerView, ViewHolder)}
//         * and / or {@link #getDragDirs(RecyclerView, ViewHolder)}.
//         *
//         * @param dragDirs  Binary OR of direction flags in which the Views can be dragged. Must be
//         *                  composed of {@link #LEFT}, {@link #RIGHT}, {@link #START}, {@link
//         *                  #END},
//         *                  {@link #UP} and {@link #DOWN}.
//         * @param swipeDirs Binary OR of direction flags in which the Views can be swiped. Must be
//         *                  composed of {@link #LEFT}, {@link #RIGHT}, {@link #START}, {@link
//         *                  #END},
//         *                  {@link #UP} and {@link #DOWN}.
//         */

        public TouchHelper() {
            super(ItemTouchHelper.UP | ItemTouchHelper.DOWN |
                            ItemTouchHelper.START | ItemTouchHelper.END,
                    0);
        }

        @Override
        public boolean onMove(@NonNull RecyclerView recyclerView,
                              @NonNull RecyclerView.ViewHolder viewHolder,
                              @NonNull RecyclerView.ViewHolder target) {

            int fromPos = viewHolder.getAdapterPosition();
            int toPos = target.getAdapterPosition();
            // Update Array
            Collections.swap(faveIdolList, fromPos, toPos);
            // Update Display
            faveIdolsAdapterRV.notifyItemMoved(fromPos, toPos);
            return true;
        }

        @Override
        public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {

        }
    }



} // end of class
