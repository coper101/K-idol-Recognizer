package com.daryl.kidolrecognizer;

import android.graphics.Bitmap;

import java.util.ArrayList;
import java.util.List;

// Singleton Pattern
public class MyData {

    private static MyData myData = new MyData();

    private Bitmap recognizedIdolBitmapFull;
    private Bitmap recognizedIdolBitmapCrop;
    private Idol idol;

    // Instantiation only allowed within this class
    private MyData() {}

    // Get the instance of this class
    public static MyData getMyData() {
        return myData;
    }

    public static void setMyData(MyData myData) {
        MyData.myData = myData;
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
}
