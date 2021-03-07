package com.daryl.kidolrecognizer;

import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Base64;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.chaquo.python.PyObject;
import com.chaquo.python.Python;
import com.chaquo.python.android.AndroidPlatform;
import com.google.android.material.appbar.MaterialToolbar;
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
            stageNameTV, realNameTV, roleTV, descTV,
            heightTV, weightTV, bloodTypeTV;
    private FloatingActionButton addAsBiasFAB;

    // Access to Module
    private PyObject pyObject;
    private Python python;

    // ===========================================================================================
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

        // Display Idol Captured
        showIdolCaptured();

        // Display Name and Description of the Idol Detected
        showIdolDesc();

        // Back Button is Clicked
        topAppBar.setNavigationOnClickListener(this);

    } // <--- end of onCreate --->


    // ===========================================================================================
    private String encodeBitmapImage(Bitmap bitmap) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream);
        byte[] imageBytes = byteArrayOutputStream.toByteArray();
        return android.util.Base64.encodeToString(imageBytes, Base64.DEFAULT);
    }

    // ===========================================================================================
    private void initViews() {
        topAppBar = findViewById(R.id.top_app_bar);
        faceImageView = findViewById(R.id.face_image_view);

        stageNameTV = findViewById(R.id.stage_name_text_view);
        realNameTV = findViewById(R.id.real_name_text_view);
        roleTV = findViewById(R.id.role_text_view);
        descTV = findViewById(R.id.description_text_view);
        heightTV = findViewById(R.id.height_text_view);
        weightTV = findViewById(R.id.weight_text_view);
        bloodTypeTV = findViewById(R.id.blood_type_text_view);

        addAsBiasFAB = findViewById(R.id.add_as_bias_fab);
    }

    // ===========================================================================================
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case -1: // back button of top app bar
                finish();
                break;
        }
    }

    // ===========================================================================================
    private void showIdolCaptured() {
        // Set Image View to Bitmap Image Set
        bitmapImage = myData.getBitmapFrame();
        if (bitmapImage != null) {
            faceImageView.setImageBitmap(bitmapImage);
        }
    }

    // ===========================================================================================
    private void showIdolDesc() {
        if (pyObject != null) {
            // Pass the Group/s Selected (Filter)
            PyObject faces_recognized = pyObject.callAttr("recognize_face", encodeBitmapImage(bitmapImage));
            if (!faces_recognized.toString().equals("{}")) {
                String idolName = faces_recognized.asMap().get(0).asList().get(0).toString();
                int idolIndex = myData.getIdolIndex(idolName);
                if (idolIndex != -1) {
                    Idol idol = myData.idolList.get(idolIndex);
                    topAppBar.setTitle(idol.getGroupName());
                    stageNameTV.setText(idol.getStageName());
                    realNameTV.setText(idol.getRealName());
                    roleTV.setText(idol.getRole());
                    descTV.setText(idol.getDescription());
                    heightTV.setText(idol.getHeight());
                    weightTV.setText(idol.getWeight());
                    bloodTypeTV.setText(idol.getBloodType());
                }
            } else {
                topAppBar.setTitle("");
                stageNameTV.setText("No Face Detected"); realNameTV.setText("");
                roleTV.setText(""); descTV.setText("");
                heightTV.setText(""); weightTV.setText(""); bloodTypeTV.setText("");
                addAsBiasFAB.hide();
            }
        }
    }

}