package com.daryl.kidolrecognizer.Data;

import android.graphics.Bitmap;

import com.chaquo.python.PyObject;

import java.util.ArrayList;
import java.util.HashMap;

// Singleton Pattern
public class MyData {

    private static MyData myData = new MyData();

    private Bitmap recognizedIdolBitmapFull;
    private Bitmap recognizedIdolBitmapCrop;
    private Idol idol;
    private PyObject mainModule;
    private float lastZoomValue = -1.0f;
    private ArrayList<Idol> allIdols;

    private HashMap<String, HashMap<String, String>> sortsAndFilterMaps;

    // Instantiation only allowed within this class
    private MyData() {}

    // Get the instance of this class
    public static MyData getMyData() {
        return myData;
    }

    // Idol Image
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

    // Idol
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

    // Main Module
    public PyObject getMainModule() {
        return mainModule;
    }

    public void setMainModule(PyObject mainModule) {
        this.mainModule = mainModule;
    }

    // Zoom
    public float getLastZoomValue() {
        return lastZoomValue;
    }

    public void setLastZoomValue(float lastZoomValue) {
        this.lastZoomValue = lastZoomValue;
    }

    // Idols
    public ArrayList<Idol> getAllIdols() {
        return allIdols;
    }

    public void setAllIdols(ArrayList<Idol> allIdols) {
        this.allIdols = allIdols;
    }

    // Filter & Sort
    public HashMap<String, HashMap<String, String>> getSortsAndFilterMaps() {
        return sortsAndFilterMaps;
    }

    public void setSortsAndFilterMaps(HashMap<String, HashMap<String, String>> sortsAndFilterMaps) {
        this.sortsAndFilterMaps = sortsAndFilterMaps;
    }


} // end class
