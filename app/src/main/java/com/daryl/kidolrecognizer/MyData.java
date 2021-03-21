package com.daryl.kidolrecognizer;

import android.graphics.Bitmap;

import java.util.ArrayList;
import java.util.List;

// Singleton Pattern
public class MyData {

    private static MyData myData = new MyData();

    Bitmap bitmapFrame;
    ArrayList<String> checkedGroupNames;

    // Instantiation only allowed within this class
    private MyData() {}

    // Get the instance of this class
    public static MyData getMyData() {
        return myData;
    }

    public void setBitmapFrame(Bitmap bitmapFrame) {
        this.bitmapFrame = bitmapFrame;
    }

    public Bitmap getBitmapFrame() {
        return bitmapFrame;
    }

    public ArrayList<String> getCheckedGroupNames() {
        return checkedGroupNames;
    }

    public void setCheckedGroupNames(ArrayList<String> checkedGroupNames) {
        this.checkedGroupNames = checkedGroupNames;
    }

}
