package com.daryl.kidolrecognizer.Fragments;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.chaquo.python.PyObject;
import com.daryl.kidolrecognizer.Data.MyData;
import com.daryl.kidolrecognizer.R;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class FavoritesFragment extends Fragment {

    private final static String TAG = FavoritesFragment.class.getSimpleName();
    private final MyData myData = MyData.getMyData();
    private TextView faveIdolsTV;

    // Unable to Access Views here
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.e(TAG, "onCreate");
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Log.e(TAG, "onCreateView");
        View view = inflater.inflate(R.layout.fragment_favorites, container, false);
        faveIdolsTV = view.findViewById(R.id.fave_idols_text_view);
        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        Log.e(TAG, "onActivityCreated");
        // Python to get favorite Idols
        PyObject mainModule = myData.getMainModule();
        if (mainModule != null) {
            Log.e(TAG, "Main Module is NOT null");
            PyObject faveIdols = mainModule.callAttr("get_favorite_idols");
            List<PyObject> faveIdolsList = faveIdols.asList();
            Log.e(TAG, "Size: " + faveIdolsList.size());
            faveIdolsTV.setText(faveIdols.asList().toString());
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
