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

import java.util.List;

public class IdolsFragment extends Fragment {

    private final static String TAG = IdolsFragment.class.getSimpleName();
    private final MyData myData = MyData.getMyData();
    private TextView idolsTV;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_idols, container, false);
        idolsTV = view.findViewById(R.id.idols_text_view);
        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        // Python to get favorite Idols
        PyObject mainModule = myData.getMainModule();
        if (mainModule != null) {
            Log.e(TAG, "Main Module is NOT null");
            PyObject idols = mainModule.callAttr("get_all_idols");
            List<PyObject> idolsList = idols.asList();
            Log.e(TAG, "Size: " + idolsList.size());
            idolsTV.setText(idolsList.toString());
        }
    }
}
