package com.daryl.kidolrecognizer.Data;

import android.graphics.Bitmap;

import com.chaquo.python.PyObject;

import java.util.ArrayList;

// Singleton Pattern
public class MyData {

    private static MyData myData = new MyData();

    private Bitmap recognizedIdolBitmapFull;
    private Bitmap recognizedIdolBitmapCrop;
    private Idol idol;
    private PyObject mainModule;
    private float lastZoomValue = -1.0f;
    private ArrayList<Idol> allIdols;

    // Instantiation only allowed within this class
    private MyData() {}

    // Get the instance of this class
    public static MyData getMyData() {
        return myData;
    }

    public Bitmap getRecognizedIdolBitmapFull() {
        return recognizedIdolBitmapFull;
    }

    public void setRecognizedIdolBitmapFull(Bitmap recognizedIdolBitmapFull) {
        this.recognizedIdolBitmapFull = recognizedIdolBitmapFull;
    }

    public Bitmap getRecognizedIdolBitmapCrop() {
        return recognizedIdolBitmapCrop;
    }

    public void setRecognizedIdolBitmapCrop(Bitmap recognizedIdolBitmapCrop) {
        this.recognizedIdolBitmapCrop = recognizedIdolBitmapCrop;
    }

    public Idol getIdol() {
        return idol;
    }

    public void setIdol(Idol idol) {
        this.idol = idol;
    }

    public boolean isAllNotNull() {
        return idol != null && recognizedIdolBitmapCrop != null && recognizedIdolBitmapFull != null;
    }

    public static void setMyData(MyData myData) {
        MyData.myData = myData;
    }

    public PyObject getMainModule() {
        return mainModule;
    }

    public void setMainModule(PyObject mainModule) {
        this.mainModule = mainModule;
    }

    public float getLastZoomValue() {
        return lastZoomValue;
    }

    public void setLastZoomValue(float lastZoomValue) {
        this.lastZoomValue = lastZoomValue;
    }

    public ArrayList<Idol> getAllIdols() {
        return allIdols;
    }

    public void setAllIdols(ArrayList<Idol> allIdols) {
        this.allIdols = allIdols;
    }

} // end class
