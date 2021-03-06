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

import java.io.ByteArrayOutputStream;

public class MainActivity2 extends AppCompatActivity {

    private static String TAG = "MainActivity2";

    private MaterialToolbar topAppBar;
    private ImageView faceImageView;
    private MyData myData = MyData.getMyData();

    // Views
    private TextView
            stageNameTV,
            realNameTV,
            roleTV,
            descTV,
            heightTV,
            weightTV,
            bloodTypeTV;


    // Access to Module
    private PyObject pyObject;
    private Python python;

    // ===========================================================================================
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);

        // Views
        topAppBar = findViewById(R.id.top_app_bar);
        faceImageView = findViewById(R.id.face_image_view);

        stageNameTV = findViewById(R.id.stage_name_text_view);
        realNameTV = findViewById(R.id.real_name_text_view);
        roleTV = findViewById(R.id.role_text_view);
        descTV = findViewById(R.id.description_text_view);
        heightTV = findViewById(R.id.height_text_view);
        weightTV = findViewById(R.id.weight_text_view);
        bloodTypeTV = findViewById(R.id.blood_type_text_view);

        // Check Python is Started
        if (! Python.isStarted()) {
            Python.start(new AndroidPlatform(this));
        }

        python = Python.getInstance();
        pyObject = python.getModule("myScript");

        // Set Image View to Bitmap Frame Set
        Bitmap bitmapFrame = myData.getBitmapFrame();
        if (bitmapFrame != null) {
            faceImageView.setImageBitmap(bitmapFrame);
        }

        if (pyObject != null) {
            PyObject faces_recognized = pyObject.callAttr("recognize_face", encodeBitmapImage(bitmapFrame));
            if (!faces_recognized.toString().equals("{}")) {
                String idolName = faces_recognized.asMap().get(0).asList().get(0).toString();
                int idolIndex = myData.getIdolIndex(idolName);
                if (idolIndex != -1) {
                    stageNameTV.setText(myData.idolList.get(idolIndex).getStageName());
                    realNameTV.setText(myData.idolList.get(idolIndex).getRealName());
                    roleTV.setText(myData.idolList.get(idolIndex).getRole());
                    descTV.setText(myData.idolList.get(idolIndex).getDescription());
                    heightTV.setText(myData.idolList.get(idolIndex).getHeight());
                    weightTV.setText(myData.idolList.get(idolIndex).getWeight());
                    bloodTypeTV.setText(myData.idolList.get(idolIndex).getBloodType());
                }
            } else {
                stageNameTV.setText("No Face Detected");
            }
        }

        topAppBar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

    } // <--- end of onCreate --->


    // ===========================================================================================
    private String encodeBitmapImage(Bitmap bitmap) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream);
        byte[] imageBytes = byteArrayOutputStream.toByteArray();
        return android.util.Base64.encodeToString(imageBytes, Base64.DEFAULT);
    }


}