package com.daryl.kidolrecognizer;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.chaquo.python.PyObject;
import com.chaquo.python.Python;
import com.chaquo.python.android.AndroidPlatform;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.ByteArrayOutputStream;

public class MainActivity2 extends AppCompatActivity implements View.OnClickListener {

    private static String TAG = "MainActivity2";

    private ImageView faceImageView;
    private final MyData myData = MyData.getMyData();
    private Bitmap bitmapImage;

    // Views
    private MaterialToolbar topAppBar;
    private TextView
            stageNameTV, realNameTV,
            heightTV, weightTV, bloodTypeTV;
    private FloatingActionButton addAsBiasFAB;

    // Access to Module
    private PyObject pyObject;
    private Python python;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);

        // Initialize Views
        initViews();

        // Check Python is Started
        if (! Python.isStarted()) {
            Python.start(new AndroidPlatform(this));
        }
        python = Python.getInstance();
        pyObject = python.getModule("myScript");

        // Back Button is Clicked
        topAppBar.setNavigationOnClickListener(this);

    } // <--- end of onCreate --->


    private String encodeBitmapImage(Bitmap bitmap) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream);
        byte[] imageBytes = byteArrayOutputStream.toByteArray();
        return android.util.Base64.encodeToString(imageBytes, Base64.DEFAULT);
    }

    private void initViews() {
        topAppBar = findViewById(R.id.top_app_bar);
        faceImageView = findViewById(R.id.face_image_view);

        stageNameTV = findViewById(R.id.stage_name_text_view);
        realNameTV = findViewById(R.id.real_name_text_view);
        heightTV = findViewById(R.id.height_text_view);
        weightTV = findViewById(R.id.weight_text_view);
        bloodTypeTV = findViewById(R.id.blood_type_text_view);

        addAsBiasFAB = findViewById(R.id.add_as_bias_fab);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case -1: // back button of top app bar
                finish();
                break;
        }
    }

    private static class Behavior extends BottomSheetBehavior.BottomSheetCallback {
        // Handle Bottom Sheet Behavior
        @Override
        public void onStateChanged(@NonNull View bottomSheet, int newState) {
            switch (newState) {
                case BottomSheetBehavior.STATE_EXPANDED:
                    Log.e(TAG, "onStateChanged - Sheet Expanded");
                    break;
                case BottomSheetBehavior.STATE_COLLAPSED:
                    Log.e(TAG, "onStateChanged - Sheet Collapsed");
                    break;
                case BottomSheetBehavior.STATE_HALF_EXPANDED:
                    Log.e(TAG, "onStateChanged - Sheet Half-Expanded");
                    break;
            }
        }
        @Override
        public void onSlide(@NonNull View bottomSheet, float slideOffset) {

        }
    }
}