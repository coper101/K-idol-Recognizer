package com.daryl.kidolrecognizer.Data;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.daryl.kidolrecognizer.Data.MyData;
import com.google.gson.GsonBuilder;

// Saving Object to Shared Preference using Gson Library
// Reference: https://medium.com/android-news/android-saving-model-object-in-shared-preferences-ce3c1d4f4573
public class MySharedPreference {

    private final GsonBuilder gsonBuilder;
    private final SharedPreferences sp;

    public MySharedPreference(Context context) {
        gsonBuilder = new GsonBuilder();
        sp = PreferenceManager.getDefaultSharedPreferences(context);
    }

    public boolean saveData(String key, MyData myData) {
        SharedPreferences.Editor spEditor = sp.edit();
        String jsonString = gsonBuilder.create().toJson(myData);
        spEditor.putString(key, jsonString);
        return spEditor.commit();
    }

    public MyData getData(String key) {
        MyData myData = null;
        String jsonString = sp.getString(key, null);
        if (jsonString != null) {
            myData = gsonBuilder.create().fromJson(jsonString, MyData.class);
        }
        if (myData == null) {
            Log.e("MySharedPreferences: ", "null");
        } else {
            Log.e("MySharedPreferences", "not null");
        }
        return myData;
    }
}
