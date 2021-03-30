package com.daryl.kidolrecognizer.Activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatImageButton;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.chaquo.python.PyObject;
import com.chaquo.python.Python;
import com.chaquo.python.android.AndroidPlatform;
import com.daryl.kidolrecognizer.Data.MyData;
import com.daryl.kidolrecognizer.Fragments.CollectionsFragment;
import com.daryl.kidolrecognizer.Fragments.FavoritesFragment;
import com.daryl.kidolrecognizer.Fragments.IdolsFragment;
import com.daryl.kidolrecognizer.R;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HomeActivity extends AppCompatActivity
        implements View.OnClickListener, BottomNavigationView.OnNavigationItemSelectedListener {

    private static final String TAG = HomeActivity.class.getSimpleName();

    // Data
    MyData myData = MyData.getMyData();

    // Views
    private AppCompatImageButton recognizerBtn;

    // Bottom Nav
    private BottomNavigationView bottomNav;

    // CameraX
    private PreviewView previewView;
    private ExecutorService cameraExecutor;

    // Permissions
    private final int REQUEST_CODE_PERMISSIONS = 101;
    private final String[] REQUIRED_PERMISSIONS = new String[]{
            "android.permission.CAMERA"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        initViews();

        cameraExecutor = Executors.newSingleThreadExecutor();

        // Request Camera Permission If Not Granted
        if (allPermissionsGranted()) {
            startCamera();
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS);
        }

        // Button is Clicked
        recognizerBtn.setOnClickListener(this);
        bottomNav.setOnNavigationItemSelectedListener(this);

        // Check Python is Started
        if (! Python.isStarted()) {
            Python.start(new AndroidPlatform(this));
        }
        Python python = Python.getInstance();

        // Run Get Main Module on a new Thread
        // Average Time Taken: 5, 2 seconds
        Thread mainModuleThread = new Thread(
            new Runnable() {
                @Override
                public void run() {
                    long start = System.nanoTime();
                    PyObject mainModule = python.getModule("myScript");
                    myData.setMainModule(mainModule);
                    long end = System.nanoTime();
                    Log.e(TAG, "Time Taken to get Main Module: " +
                            ((end - start) / 1000000000)
                            + " seconds");
                    updateButtonState(true);
                    // Save Idols CSV to Home
                    boolean isSave = mainModule.callAttr("save_idols_data_to_home").toBoolean();
                    Log.e(TAG, "Idols CSV is Saved: " + isSave);
                }
            }
        );
        mainModuleThread.start();

    } // <-- end of onCreate -->


    // ===========================================================================================
    private void initViews() {
        recognizerBtn = findViewById(R.id.recognizer_button);
        recognizerBtn.setEnabled(false);

        previewView = findViewById(R.id.camera_preview_view);

        bottomNav = findViewById(R.id.bottom_nav_view);
        bottomNav.setSelectedItemId(R.id.recognizer_item);
    }

    // ===========================================================================================
    @Override
    public void onClick(View v) {
        int viewId = v.getId();
        if (viewId == R.id.recognizer_button) {
            Bitmap bitmapFull = previewView.getBitmap();
            myData.setRecognizedIdolBitmapFull(bitmapFull);
            Intent toMain = new Intent(this, RecognitionActivity.class);
            startActivity(toMain);
        }
    }

    // ===========================================================================================
    private void updateButtonState(boolean isEnabled) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                recognizerBtn.setEnabled(isEnabled);
            }
        });
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
            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
            }

            // (3) Select Camera Bind Lifecycle and Uses Cases

            // Use Case: Preview
            Preview preview = new Preview.Builder().build();
            CameraSelector cameraSelector = new CameraSelector.Builder()
                    .requireLensFacing(CameraSelector.LENS_FACING_BACK).build();
            preview.setSurfaceProvider(previewView.createSurfaceProvider());

            try {
                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(this, cameraSelector, preview);
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
        Log.e(TAG, "onResume called");
        if (allPermissionsGranted()) {
            startCamera();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.e(TAG, "onDestroy called");
        cameraExecutor.shutdown();
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.e(TAG, "onPause called");
    }

    // ===========================================================================================

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        Fragment selectedFragment = null;
        switch (item.getItemId()) {
            case R.id.collections_item:
                selectedFragment = new CollectionsFragment();

                break;
            case R.id.favorites_item:
                selectedFragment = new FavoritesFragment();
                break;
            case R.id.idols_item:
                selectedFragment = new IdolsFragment();
                break;
            // recognizer_item
            // selectedFragment is null
        }

        // Remove Existing Fragment if Null (Empty the Frame Container)
        if (selectedFragment == null) {
            for (Fragment fragment: getSupportFragmentManager().getFragments()) {
                getSupportFragmentManager()
                        .beginTransaction()
                        .remove(fragment)
                        .commit();
            }
            recognizerBtn.setVisibility(View.VISIBLE);
        }
        // Replace to Collections or Favorites Fragment
        else {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, selectedFragment)
                    .commit();
            recognizerBtn.setVisibility(View.GONE);
        }
        return true;
    }


} // <-- end of HomeActivity Class -->