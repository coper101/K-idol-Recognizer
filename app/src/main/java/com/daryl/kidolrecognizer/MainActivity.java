package com.daryl.kidolrecognizer;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.chaquo.python.PyObject;
import com.chaquo.python.Python;
import com.chaquo.python.android.AndroidPlatform;
import com.google.common.util.concurrent.ListenableFuture;

import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private static String TAG = "MainActivity";

    private ExecutorService cameraExecutor;
    private MyData myData = MyData.getMyData();

    private PreviewView previewView;
    private AppCompatButton recognizerButton;

    private TextView
            stageNameTV, realNameTV, roleTV, descTV,
            heightTV, weightTV, bloodTypeTV;

    // Access to Module
    PyObject pyObject;
    private Python python;

    // Permissions
    private final int REQUEST_CODE_PERMISSIONS = 101;
    private final String[] REQUIRED_PERMISSIONS = new String[]{
            "android.permission.CAMERA"
    };

    // ===========================================================================================
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Request Camera Permission If Not Granted
        if (allPermissionsGranted()) {
            startCamera();
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS);
        }

        cameraExecutor = Executors.newSingleThreadExecutor();

        // Initialize Views
        initViews();

        // Check Python is Started
        if (! Python.isStarted()) {
            Python.start(new AndroidPlatform(this));
        }
        python = Python.getInstance();
        pyObject = python.getModule("myScript");

        // Button is Clicked
        recognizerButton.setOnClickListener(this);;


    } // <--- end of onCreate --->


    // ===========================================================================================
    private void initViews() {
        previewView = findViewById(R.id.viewFinder);
        recognizerButton = findViewById(R.id.recognizer_button);

        stageNameTV = findViewById(R.id.stage_name_text_view);
        realNameTV = findViewById(R.id.real_name_text_view);
        roleTV = findViewById(R.id.role_text_view);
        descTV = findViewById(R.id.description_text_view);
        heightTV = findViewById(R.id.height_text_view);
        weightTV = findViewById(R.id.weight_text_view);
        bloodTypeTV = findViewById(R.id.blood_type_text_view);
    }

    // ===========================================================================================
    // CameraX References:
    // https://developer.android.com/training/camerax/architecture
    // https://codelabs.developers.google.com/codelabs/camerax-getting-started#0
    private void startCamera() {

        // (1) Request Camera Provider
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            // (2) Check Camera Provider Availability
            ProcessCameraProvider cameraProvider = null;
            try {
                cameraProvider = cameraProviderFuture.get();
            } catch (ExecutionException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            // (3) Select Camera Bind Lifecycle and Uses Cases

            // Use Case: Preview
            Preview preview = new Preview.Builder().build();
            CameraSelector cameraSelector = new CameraSelector.Builder()
                    .requireLensFacing(CameraSelector.LENS_FACING_BACK).build();
            preview.setSurfaceProvider(previewView.createSurfaceProvider());

            // Use Case: Image Analyzer
            ImageAnalysis imageAnalyzer = new ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST) // non-blocking mode
                    .build();

            imageAnalyzer.setAnalyzer(cameraExecutor, image -> {

                Bitmap bitmapFrame = previewView.getBitmap();
                String encodeBitmap = encodeBitmapImage(bitmapFrame);

                if (pyObject != null && encodeBitmap != null) {

                    // Recognize Idol (Average Time to Recognize: 1 sec)
                    long start = System.currentTimeMillis();
                    PyObject stageName = pyObject.callAttr("detect_face_fr", encodeBitmap);
                    long end = System.currentTimeMillis();
                    Log.e(TAG, "Time Taken: " + (end-start));

                    List<PyObject> stageNames = stageName.asList();
                    for (PyObject name: stageNames) {
                        // Show Idol's Stage Name
                        String nameStr = name.toString();
                        setViewValue(stageNameTV, nameStr);
                        Log.e(TAG, nameStr);
                        // Show Idol's Profile
                        PyObject profile = pyObject.callAttr("get_idol_profile", nameStr);
                        Map<PyObject, PyObject> profileValues = profile.asMap();
                        if (!profileValues.isEmpty()) {
                            setViewValue(realNameTV, profileValues.get("Real Name (Korean)").toString());
                        }
                        Log.e(TAG, profile.toString());

                    }

                }

                image.close();
            });

            try {
                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalyzer);
            } catch (Exception exc) {
                Log.e(TAG, "Use case binding failed", exc);
            }
        }, ContextCompat.getMainExecutor(this));

    }

    // ===========================================================================================
    private boolean allPermissionsGranted() {
        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    // ===========================================================================================
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            // start camera when granted
            if (allPermissionsGranted()) {
                startCamera();
                // exit app when not granted
            } else {
                Toast.makeText(this,
                        "Permissions not granted by the user.",
                        Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    // ===========================================================================================
    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume called");
        // image analysis is called again
        if (allPermissionsGranted()) {
            startCamera();
            Log.d(TAG, "image analysis started");
        }
    }

    // ===========================================================================================
    @Override
    protected void onDestroy() {
        super.onDestroy();
        cameraExecutor.shutdown();
    }

    // ===========================================================================================
    // Set the Text of View outside the main
    private void setViewValue(View view, String value){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                switch (view.getId()) {
                    case R.id.stage_name_text_view:
                        stageNameTV.setText(value);
                        break;
                    case R.id.real_name_text_view:
                        realNameTV.setText(value);
                        break;
                    case R.id.role_text_view:
                        roleTV.setText(value);
                        break;
                    case R.id.description_text_view:
                        descTV.setText(value);
                        break;
                    case R.id.height_text_view:
                        heightTV.setText(value);
                        break;
                    case R.id.weight_text_view:
                        weightTV.setText(value);
                        break;
                    case R.id.blood_type_text_view:
                        bloodTypeTV.setText(value);
                        break;
                }
            }
        });
    }

    // ===========================================================================================
    private String encodeBitmapImage(Bitmap bitmap) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream);
        byte[] imageBytes = byteArrayOutputStream.toByteArray();
        return android.util.Base64.encodeToString(imageBytes, Base64.DEFAULT);
    }

    // ===========================================================================================
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.recognizer_button:
                break;
        }
    }


} // <---  end of MainActivity ! --->