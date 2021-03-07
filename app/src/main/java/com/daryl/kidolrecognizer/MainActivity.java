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

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.chaquo.python.PyObject;
import com.chaquo.python.Python;
import com.chaquo.python.android.AndroidPlatform;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.opencv.android.OpenCVLoader;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private static String TAG = "MainActivity";

    private ExecutorService cameraExecutor;
    private Python python;
    private MyData myData = MyData.getMyData();

    private PreviewView viewFinder;
    private AppCompatButton recognizerButton;
    private ChipGroup groupsChipGroup;
    private Chip btsChip, blackpinkChip, twiceChip, redVelvetChip;

    // Access to Module
    PyObject pyObject;

    // Check OpenCV
    static {
        if (OpenCVLoader.initDebug())
            Log.d(TAG, "OpenCV installed successfully");
        else
            Log.d(TAG, "OpenCV not installed");
    }

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
        recognizerButton.setOnClickListener(this);

        // Chip is Clicked
        btsChip.setOnClickListener(this); blackpinkChip.setOnClickListener(this);
        twiceChip.setOnClickListener(this); redVelvetChip.setOnClickListener(this);

        // Retrieve Idols & Set in My Data
        retrieveIdols();

    } // <--- end of onCreate --->


    // ===========================================================================================
    private void initViews() {
        viewFinder = findViewById(R.id.viewFinder);
        recognizerButton = findViewById(R.id.recognizer_button);

        groupsChipGroup = findViewById(R.id.idol_group_chip_group);
        btsChip = findViewById(R.id.bts_chip);
        blackpinkChip = findViewById(R.id.blackpink_chip);
        twiceChip = findViewById(R.id.twice_chip);
        redVelvetChip = findViewById(R.id.red_velvet_chip);
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
            preview.setSurfaceProvider(viewFinder.createSurfaceProvider());

            // Use Case: Image Analyzer
            ImageAnalysis imageAnalyzer = new ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST) // non-blocking mode
                    .build();

            imageAnalyzer.setAnalyzer(cameraExecutor, image -> {
//                Log.d(TAG, "Image Info: " + image.getImageInfo());

                // Set the Bitmap to the Current Frame
//                Bitmap bitmapFrame = viewFinder.getBitmap();
//
//                if (bitmapFrame == null)
//                    return;
//
//                myData.setBitmapFrame(bitmapFrame);

                // Show Recognized Face
//                if (pyObject != null) {
//                    PyObject detectFacesObj = pyObject.callAttr("recognize_face", encodeBitmapImage(bitmapFrame));
//                    setDetectionsText(detectFacesObj.toString());
//                }

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
//    private void setDetectionsText(String value){
//        runOnUiThread(new Runnable() {
//            @Override
//            public void run() {
//                detectionsTextView.setText(value);
//            }
//        });
//    }

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
                Intent intent = new Intent(getApplicationContext(), MainActivity2.class);
                // Set the Bitmap to the Current Frame & Navigate to Main Activity 2
                Bitmap bitmapFrame = viewFinder.getBitmap();
                if (bitmapFrame != null) {
                    myData.setBitmapFrame(bitmapFrame);
                    ArrayList<String> checkedGroupNames = checkChipIdsToGroupNames(groupsChipGroup.getCheckedChipIds());
                    myData.setCheckedGroupNames(checkedGroupNames);
                    Log.e(TAG, myData.checkedGroupNames.toString());
                    startActivity(intent);
                }
                break;
            case R.id.bts_chip:
                Toast.makeText(getApplicationContext(), "BTS", Toast.LENGTH_SHORT).show();
                break;
            case R.id.blackpink_chip:
                Toast.makeText(getApplicationContext(), "BP", Toast.LENGTH_SHORT).show();
                break;
            case R.id.twice_chip:
                Toast.makeText(getApplicationContext(), "TWICE", Toast.LENGTH_SHORT).show();
                break;
            case R.id.red_velvet_chip:
                Toast.makeText(getApplicationContext(), "RV", Toast.LENGTH_SHORT).show();
                break;
            default:
                Toast.makeText(getApplicationContext(), "onClick Invoked", Toast.LENGTH_SHORT).show();
        }
    }

    // ===========================================================================================
    private void retrieveIdols() {
        String jsonString = jsonToString(getApplicationContext(), "idols.json");
        List<Idol> idols = idolList(jsonString);
        myData.setIdolList(idols);
        for (Idol idol: myData.getIdolList()) {
            Log.e(TAG, idol.toString());
        }
    }

    // ===========================================================================================
    private String jsonToString(Context context, String fileName) {
        AssetManager assetManager = context.getAssets();
        String jsonString = null;
        try {
            InputStream is = assetManager.open(fileName);
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            jsonString = new String(buffer, "UTF-8");
        } catch (IOException e) {
            e.printStackTrace();
        }
        return jsonString;
    }

    // ===========================================================================================
    private List<Idol> idolList(String jsonString) {
        Gson gson = new Gson();
        Type listIdolsType = new TypeToken<List<Idol>>(){}.getType();
        return gson.fromJson(jsonString, listIdolsType);
    }

    // ===========================================================================================
    private ArrayList<String> checkChipIdsToGroupNames(List<Integer> checkChipIds) {
        ArrayList<String> checkedGroupNames = new ArrayList<>();
        for (int checkChipId: checkChipIds) {
            switch (checkChipId) {
                case R.id.bts_chip:
                    checkedGroupNames.add("Bts");
                    break;
                case R.id.blackpink_chip:
                    checkedGroupNames.add("Blackpink");
                    break;
                case R.id.twice_chip:
                    checkedGroupNames.add("Twice");
                    break;
                case R.id.red_velvet_chip:
                    checkedGroupNames.add("Red Velvet");
                    break;
            }
        }
        return checkedGroupNames;
    }


} // <---  end of MainActivity ! --->