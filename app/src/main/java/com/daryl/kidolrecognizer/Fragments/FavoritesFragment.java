package com.daryl.kidolrecognizer.Fragments;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.chaquo.python.PyObject;
import com.daryl.kidolrecognizer.Activities.HomeActivity;
import com.daryl.kidolrecognizer.Data.Idol;
import com.daryl.kidolrecognizer.Data.MyData;
import com.daryl.kidolrecognizer.R;
import com.daryl.kidolrecognizer.RecyclerView.FaveIdolListAdapterWithRecyclerView;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.List;

public class FavoritesFragment extends Fragment {

    private final static String TAG = FavoritesFragment.class.getSimpleName();
    private final MyData myData = MyData.getMyData();
    private final PyObject mainModule = myData.getMainModule();

    private FaveIdolListAdapterWithRecyclerView faveIdolsAdapterRV;
    private ArrayList<Idol> faveIdolList;
    private RecyclerView faveIdolsRV;
    private LinearLayoutManager layoutManager;

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

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Log.e(TAG, "onCreateView");
        View view = inflater.inflate(R.layout.fragment_favorites, container, false);
        // Recycler View
        faveIdolsRV = view.findViewById(R.id.favorite_idols_recycler_view);
        faveIdolsRV.setAdapter(faveIdolsAdapterRV);
        faveIdolsRV.setLayoutManager(layoutManager);
        faveIdolsAdapterRV.setOnItemCheckedChangeListener(new FaveIdolListAdapterWithRecyclerView.OnItemCheckedChangeListener() {
            @Override
            public void onCheckedChange(int position, boolean isChecked) {
                if (!isChecked) {
                    Toast.makeText(getContext(), "Unchecked", Toast.LENGTH_SHORT).show();
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
                                .setAnchorView(getActivity().findViewById(R.id.bottom_nav_view))
                                .show();
                    }
                }
            }
        });
        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        Log.e(TAG, "onActivityCreated");
        // Python to get favorite Idols
        if (mainModule != null) {
            Log.e(TAG, "Main Module is NOT null");

            PyObject faveIdols = mainModule.callAttr("get_favorite_idols");
            List<PyObject> faveIdolsList = faveIdols.asList();
            Log.e(TAG, "Size: " + faveIdolsList.size());

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
            if (faveIdolsList.size() > 0) {
                faveIdolsAdapterRV.notifyDataSetChanged();
            }
        }
    }


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

}
