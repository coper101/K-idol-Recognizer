package com.daryl.kidolrecognizer.Data;

import android.graphics.Bitmap;

// Singleton Pattern
public class MyData {

    private static MyData myData = new MyData();

    private Bitmap recognizedIdolBitmapFull;
    private Bitmap recognizedIdolBitmapCrop;
    private Idol idol;
    private String test;

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

    public String getTest() {
        return test;
    }

    public void setTest(String test) {
        this.test = test;
    }
}
