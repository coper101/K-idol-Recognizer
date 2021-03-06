package com.daryl.kidolrecognizer;

import android.graphics.Bitmap;

import java.util.List;

// Singleton Pattern
public class MyData {

    private static MyData myData = new MyData();

    Bitmap bitmapFrame;
    List<Idol> idolList;

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

    public List<Idol> getIdolList() {
        return idolList;
    }

    public void setIdolList(List<Idol> idolList) {
        this.idolList = idolList;
    }

    public int getIdolIndex(String stageName) {
        int index = -1;                              // not found
        for (int i = 0; i < idolList.size(); i++) {
            if (stageName.equalsIgnoreCase(idolList.get(i).getStageName())) {
                index = i;
                break;
            }
        }
        return index;
    }
}
