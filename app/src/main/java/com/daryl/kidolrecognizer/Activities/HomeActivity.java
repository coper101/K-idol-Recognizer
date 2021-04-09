package com.daryl.kidolrecognizer.Activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatImageButton;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraControl;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.chaquo.python.PyObject;
import com.chaquo.python.Python;
import com.chaquo.python.android.AndroidPlatform;
import com.daryl.kidolrecognizer.Data.MyData;
import com.daryl.kidolrecognizer.Fragments.GalleryFragment;
import com.daryl.kidolrecognizer.Fragments.FavoritesFragment;
import com.daryl.kidolrecognizer.Fragments.IdolsFragment;
import com.daryl.kidolrecognizer.R;
import com.google.android.material.slider.RangeSlider;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HomeActivity extends AppCompatActivity
        implements View.OnClickListener {

    private static final String TAG = HomeActivity.class.getSimpleName();

    // Data
    MyData myData = MyData.getMyData();

    // Views
    private AppCompatImageButton recognizerBtn;

    // Custom Bottom Navigation
//    private BottomNavigationView bottomNav;
    private LinearLayout recognizeNavItem, galleryNavItem, favoriteNavItem, idolsNavItem;
    private ImageView recognizeIcon, galleryIcon, favoriteIcon, idolsIcon;
    /*
    0 - recognizeNavItem
    1 - galleryNavItem
    2 - favoriteNavItem
    3 - idolsNavItem
     */

    // CameraX
    private CameraControl cameraControl;
    private PreviewView previewView;
    private ExecutorService cameraExecutor;
    private RangeSlider zoomSlider;

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
//        bottomNav.setOnNavigationItemSelectedListener(this);
        recognizeNavItem.setOnClickListener(this);
        galleryNavItem.setOnClickListener(this);
        favoriteNavItem.setOnClickListener(this);
        idolsNavItem.setOnClickListener(this);

        // Check Python is Started
        if (! Python.isStarted()) {
            Python.start(new AndroidPlatform(this));
        }
        Python python = Python.getInstance();

        // Run Get Main Module on a new Thread
        // It might take a while so run it in a new thread to not interrupt the main thread
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
                    // Save Idols CSV to App Users File Path if not saved yet
                    saveIdolsCSVtoHome();
                }
            }
        );
        mainModuleThread.start();

        zoomSlider.addOnChangeListener(new RangeSlider.OnChangeListener() {
            @Override
            public void onValueChange(@NonNull RangeSlider slider, float value, boolean fromUser) {
                Log.e(TAG, "Slider Value: " + value);
                zoom(value);
            }
        });

    } // <-- end of onCreate -->


    // ===========================================================================================
    private void initViews() {
        recognizerBtn = findViewById(R.id.recognizer_button);
        recognizerBtn.setEnabled(false);

        previewView = findViewById(R.id.camera_preview_view);

//        bottomNav = findViewById(R.id.bottom_nav_view);
//        bottomNav.setSelectedItemId(R.id.recognizer_item);
        // Nav Items
        recognizeNavItem = findViewById(R.id.recognizer_nav_item);
        galleryNavItem = findViewById(R.id.gallery_nav_item);
        favoriteNavItem = findViewById(R.id.favorite_nav_item);
        idolsNavItem = findViewById(R.id.idols_nav_item);
        // Nav Icons
        recognizeIcon = findViewById(R.id.recognizer_nav_item_icon);
        galleryIcon = findViewById(R.id.gallery_nav_item_icon);
        favoriteIcon = findViewById(R.id.favorite_nav_item_icon);
        idolsIcon = findViewById(R.id.idols_nav_item_icon);

        zoomSlider = findViewById(R.id.zoom_slider);
    }

    // ===========================================================================================
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.recognizer_button:
                Bitmap bitmapFull = previewView.getBitmap();
                myData.setRecognizedIdolBitmapFull(bitmapFull);
                Intent toMain = new Intent(this, RecognitionActivity.class);
                startActivity(toMain);
                break;
            case R.id.recognizer_nav_item:
                selectNavItem(0);
                recognizeNavItem.setBackground(getDrawable(R.drawable.bottom_nav_item_bg_blue));
                galleryNavItem.setBackground(getDrawable(R.drawable.bottom_nav_item_bg_space_gray));
                favoriteNavItem.setBackground(getDrawable(R.drawable.bottom_nav_item_bg_space_gray));
                idolsNavItem.setBackground(getDrawable(R.drawable.bottom_nav_item_bg_space_gray));

                recognizeIcon.setImageTintList(ContextCompat.getColorStateList(getApplicationContext(), R.color.blue_accent));
                galleryIcon.setImageTintList(ContextCompat.getColorStateList(getApplicationContext(), R.color.blue_light_A50));
                favoriteIcon.setImageTintList(ContextCompat.getColorStateList(getApplicationContext(), R.color.blue_light_A50));
                idolsIcon.setImageTintList(ContextCompat.getColorStateList(getApplicationContext(), R.color.blue_light_A50));

                recognizeIcon.setImageDrawable(getDrawable(R.drawable.nav_item_recognizer_icon_filled));
                galleryIcon.setImageDrawable(getDrawable(R.drawable.nav_item_gallery_icon_outlined));
                favoriteIcon.setImageDrawable(getDrawable(R.drawable.nav_item_heart_icon_outlined));
                idolsIcon.setImageDrawable(getDrawable(R.drawable.nav_item_idols_icon_outlined));

                recognizeIcon.setPadding(9, 9, 9, 9);
                galleryIcon.setPadding(12, 12, 12, 12);
                favoriteIcon.setPadding(12, 12, 12, 12);
                idolsIcon.setPadding(9, 9, 9, 9);

                recognizeIcon.setPadding(toPx(9), toPx(9), toPx(9), toPx(9));
                galleryIcon.setPadding(toPx(12), toPx(12), toPx(12), toPx(12));
                favoriteIcon.setPadding(toPx(12), toPx(12), toPx(12), toPx(12));
                idolsIcon.setPadding(toPx(9), toPx(9), toPx(9), toPx(9));

                break;
            case R.id.gallery_nav_item:
                selectNavItem(1);
                recognizeNavItem.setBackground(getDrawable(R.drawable.bottom_nav_item_bg_space_gray));
                galleryNavItem.setBackground(getDrawable(R.drawable.bottom_nav_item_bg_yellow));
                favoriteNavItem.setBackground(getDrawable(R.drawable.bottom_nav_item_bg_space_gray));
                idolsNavItem.setBackground(getDrawable(R.drawable.bottom_nav_item_bg_space_gray));

                recognizeIcon.setImageTintList(ContextCompat.getColorStateList(getApplicationContext(), R.color.blue_light_A50));
                galleryIcon.setImageTintList(ContextCompat.getColorStateList(getApplicationContext(), R.color.yellow_accent));
                favoriteIcon.setImageTintList(ContextCompat.getColorStateList(getApplicationContext(), R.color.blue_light_A50));
                idolsIcon.setImageTintList(ContextCompat.getColorStateList(getApplicationContext(), R.color.blue_light_A50));

                recognizeIcon.setImageDrawable(getDrawable(R.drawable.nav_item_recognizer_icon_outlined));
                galleryIcon.setImageDrawable(getDrawable(R.drawable.nav_item_gallery_icon_filled));
                favoriteIcon.setImageDrawable(getDrawable(R.drawable.nav_item_heart_icon_outlined));
                idolsIcon.setImageDrawable(getDrawable(R.drawable.nav_item_idols_icon_outlined));

                recognizeIcon.setPadding(toPx(10), toPx(10), toPx(10), toPx(10));
                galleryIcon.setPadding(toPx(9), toPx(9), toPx(9), toPx(9));
                favoriteIcon.setPadding(toPx(12), toPx(12), toPx(12), toPx(12));
                idolsIcon.setPadding(toPx(9), toPx(9), toPx(9), toPx(9));
                break;
            case R.id.favorite_nav_item:
                selectNavItem(2);
                recognizeNavItem.setBackground(getDrawable(R.drawable.bottom_nav_item_bg_space_gray));
                galleryNavItem.setBackground(getDrawable(R.drawable.bottom_nav_item_bg_space_gray));
                favoriteNavItem.setBackground(getDrawable(R.drawable.bottom_nav_item_bg_red));
                idolsNavItem.setBackground(getDrawable(R.drawable.bottom_nav_item_bg_space_gray));

                recognizeIcon.setImageTintList(ContextCompat.getColorStateList(getApplicationContext(), R.color.blue_light_A50));
                galleryIcon.setImageTintList(ContextCompat.getColorStateList(getApplicationContext(), R.color.blue_light_A50));
                favoriteIcon.setImageTintList(ContextCompat.getColorStateList(getApplicationContext(), R.color.red_accent));
                idolsIcon.setImageTintList(ContextCompat.getColorStateList(getApplicationContext(), R.color.blue_light_A50));

                recognizeIcon.setImageDrawable(getDrawable(R.drawable.nav_item_recognizer_icon_outlined));
                galleryIcon.setImageDrawable(getDrawable(R.drawable.nav_item_gallery_icon_outlined));
                favoriteIcon.setImageDrawable(getDrawable(R.drawable.nav_item_heart_icon_filled));
                idolsIcon.setImageDrawable(getDrawable(R.drawable.nav_item_idols_icon_outlined));

                recognizeIcon.setPadding(toPx(10), toPx(10), toPx(10), toPx(10));
                galleryIcon.setPadding(toPx(12), toPx(12), toPx(12), toPx(12));
                favoriteIcon.setPadding(toPx(9), toPx(9), toPx(9), toPx(9));
                idolsIcon.setPadding(toPx(9), toPx(9), toPx(9), toPx(9));
                break;
            case R.id.idols_nav_item:
                selectNavItem(3);
                recognizeNavItem.setBackground(getDrawable(R.drawable.bottom_nav_item_bg_space_gray));
                galleryNavItem.setBackground(getDrawable(R.drawable.bottom_nav_item_bg_space_gray));
                favoriteNavItem.setBackground(getDrawable(R.drawable.bottom_nav_item_bg_space_gray));
                idolsNavItem.setBackground(getDrawable(R.drawable.bottom_nav_item_bg_green));

                recognizeIcon.setImageTintList(ContextCompat.getColorStateList(getApplicationContext(), R.color.blue_light_A50));
                galleryIcon.setImageTintList(ContextCompat.getColorStateList(getApplicationContext(), R.color.blue_light_A50));
                favoriteIcon.setImageTintList(ContextCompat.getColorStateList(getApplicationContext(), R.color.blue_light_A50));
                idolsIcon.setImageTintList(ContextCompat.getColorStateList(getApplicationContext(), R.color.green_accent));

                recognizeIcon.setImageDrawable(getDrawable(R.drawable.nav_item_recognizer_icon_outlined));
                galleryIcon.setImageDrawable(getDrawable(R.drawable.nav_item_gallery_icon_outlined));
                favoriteIcon.setImageDrawable(getDrawable(R.drawable.nav_item_heart_icon_outlined));
                idolsIcon.setImageDrawable(getDrawable(R.drawable.nav_item_idols_icon_filled));

                recognizeIcon.setPadding(toPx(10), toPx(10), toPx(10), toPx(10));
                galleryIcon.setPadding(toPx(12), toPx(12), toPx(12), toPx(12));
                favoriteIcon.setPadding(toPx(12), toPx(12), toPx(12), toPx(12));
                idolsIcon.setPadding(toPx(7), toPx(7), toPx(7), toPx(7));
                break;
        }
    }

    private int toPx(int dp) {
        float density = this.getResources().getDisplayMetrics().density;
        int px= (int) (dp * density);
        return px;
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
                Camera camera = cameraProvider.bindToLifecycle(this, cameraSelector, preview);
                cameraControl = camera.getCameraControl();
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
            // Update Last Zoom Value
            if (MyData.getMyData().getMainModule() != null) {
                float lastZoomValue = MyData.getMyData().getLastZoomValue();
                Log.e(TAG, "Last Zoom Value: " + lastZoomValue);
                if (lastZoomValue == -1.0f) {
                    Log.e(TAG, "Last Zoom Not Set");
                } else {
                    Log.e(TAG, "Last Zoom Set");
                    zoom(lastZoomValue);
                }
            }
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

//    @Override
//    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
//        Fragment selectedFragment = null;
//        switch (item.getItemId()) {
//            case R.id.collections_item:
//                selectedFragment = new GalleryFragment();
//                break;
//            case R.id.favorites_item:
//                selectedFragment = new FavoritesFragment();
//                break;
//            case R.id.idols_item:
//                selectedFragment = new IdolsFragment();
//                break;
//            // recognizer_item
//            // selectedFragment is null
//        }
//
//        // Remove Existing Fragment if Null (Empty the Frame Container)
//        if (selectedFragment == null) {
//            for (Fragment fragment: getSupportFragmentManager().getFragments()) {
//                getSupportFragmentManager()
//                        .beginTransaction()
//                        .remove(fragment)
//                        .commit();
//            }
//            recognizerBtn.setVisibility(View.VISIBLE);
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
//                getWindow().setStatusBarColor(getResources().getColor(R.color.transparent, this.getTheme()));
//            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
//                getWindow().setStatusBarColor(getResources().getColor(R.color.transparent));
//            }
//        }
//        // Replace to Collections or Favorites Fragment
//        else {
//            getSupportFragmentManager()
//                    .beginTransaction()
//                    .replace(R.id.fragment_container, selectedFragment)
//                    .commit();
//            recognizerBtn.setVisibility(View.GONE);
//            // Status Bar
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
//                getWindow().setStatusBarColor(getResources().getColor(R.color.space_gray_light_2, this.getTheme()));
//            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
//                getWindow().setStatusBarColor(getResources().getColor(R.color.space_gray_light_2));
//            }
//        }
//        return false;
//    }

    private void selectNavItem(int navItemValue) {

        Fragment selectedFragment = null;
        String fragmentTAG = null;

        String galleryFragmentTAG = GalleryFragment.class.getSimpleName();
        String favoritesFragmentTAG = FavoritesFragment.class.getSimpleName();
        String idolsFragmentTAG = IdolsFragment.class.getSimpleName();

        switch (navItemValue) {
            case 1:
                selectedFragment = new GalleryFragment();
                fragmentTAG = galleryFragmentTAG;
                break;
            case 2:
                selectedFragment = new FavoritesFragment();
                fragmentTAG = favoritesFragmentTAG;
                break;
            case 3:
                selectedFragment = new IdolsFragment();
                fragmentTAG = idolsFragmentTAG;
                break;
            // recognizer_item
            // selectedFragment is null
        }

        // Hide All Fragments
        if (selectedFragment == null && fragmentTAG == null) {
            // Hide All Fragments to show Activity (Preview View)
            for (Fragment fragment: getSupportFragmentManager().getFragments()) {
                getSupportFragmentManager()
                        .beginTransaction()
                        .hide(fragment)
                        .commit();
            }
            recognizerBtn.setVisibility(View.VISIBLE);
            zoomSlider.setVisibility(View.VISIBLE);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                getWindow().setStatusBarColor(getResources().getColor(R.color.transparent, this.getTheme()));
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                getWindow().setStatusBarColor(getResources().getColor(R.color.transparent));
            }
        }
        // Add Show Selected Fragment
        else if (selectedFragment != null && fragmentTAG != null) {
            // Check Which Fragment is Selected

            Log.e(TAG, "sel frag not null & fragment tag not null");
            // -> Gallery Fragment
            if (fragmentTAG.equals(galleryFragmentTAG)) {
                // Gallery Fragment Added?
                // YES - Show it
                Fragment fragment = getSupportFragmentManager().findFragmentByTag(galleryFragmentTAG);
                if (fragment != null) {
                    getSupportFragmentManager()
                            .beginTransaction()
                            .show(fragment)
                            .commit();
                }
                // NO - Add it
                else {
                    getSupportFragmentManager()
                            .beginTransaction()
                            .add(R.id.fragment_container, selectedFragment, galleryFragmentTAG)
                            .commit();
                }
                // Hide Other Fragments Visible
                Fragment favoriteFrag = getSupportFragmentManager().findFragmentByTag(favoritesFragmentTAG);
                Fragment idolsFrag = getSupportFragmentManager().findFragmentByTag(idolsFragmentTAG);
                if (favoriteFrag != null) {
                    getSupportFragmentManager()
                            .beginTransaction()
                            .hide(favoriteFrag)
                            .commit();
                }
                if (idolsFrag != null) {
                    getSupportFragmentManager()
                            .beginTransaction()
                            .hide(idolsFrag)
                            .commit();
                }
            }

            // -> Favorites Fragment
            else if (fragmentTAG.equals(favoritesFragmentTAG)) {
                // Favorites Fragment Added?
                // YES - Show it
                Fragment fragment = getSupportFragmentManager().findFragmentByTag(favoritesFragmentTAG);
                if (fragment != null) {
                    getSupportFragmentManager()
                            .beginTransaction()
                            .show(fragment)
                            .commit();
                }
                // NO - Add it
                else {
                    getSupportFragmentManager()
                            .beginTransaction()
                            .add(R.id.fragment_container, selectedFragment, favoritesFragmentTAG)
                            .commit();
                }
                // Hide Other Fragments Visible
                Fragment galleryFrag = getSupportFragmentManager().findFragmentByTag(galleryFragmentTAG);
                Fragment idolsFrag = getSupportFragmentManager().findFragmentByTag(idolsFragmentTAG);
                if (idolsFrag != null) {
                    getSupportFragmentManager()
                            .beginTransaction()
                            .hide(idolsFrag)
                            .commit();
                }
                if (galleryFrag != null) {
                    getSupportFragmentManager()
                            .beginTransaction()
                            .hide(galleryFrag)
                            .commit();
                }
            }

            // -> Idols Fragment
            else if (fragmentTAG.equals(idolsFragmentTAG)) {
                // Idols Fragment Added?
                // YES - Show it
                Fragment fragment = getSupportFragmentManager().findFragmentByTag(idolsFragmentTAG);
                if (fragment != null) {
                    getSupportFragmentManager()
                            .beginTransaction()
                            .show(fragment)
                            .commit();
                }
                // NO - Add it
                else {
                    getSupportFragmentManager()
                            .beginTransaction()
                            .add(R.id.fragment_container, selectedFragment, idolsFragmentTAG)
                            .commit();
                }
                // Hide Other Fragments Visible
                Fragment galleryFrag = getSupportFragmentManager().findFragmentByTag(galleryFragmentTAG);
                Fragment favoritesFrag = getSupportFragmentManager().findFragmentByTag(favoritesFragmentTAG);

                if (galleryFrag != null) {
                    getSupportFragmentManager()
                            .beginTransaction()
                            .hide(galleryFrag)
                            .commit();
                }
                if (favoritesFrag != null) {
                    getSupportFragmentManager()
                            .beginTransaction()
                            .hide(favoritesFrag)
                            .commit();
                }
            }

            // Replace to Collections or Favorites Fragment
//            getSupportFragmentManager()
//                    .beginTransaction()
//                    .replace(R.id.fragment_container, selectedFragment, fragmentTAG)
//                    .commit();

            // Hide Camera Buttons & Controls to not interfere with Fragments
            recognizerBtn.setVisibility(View.GONE);
            zoomSlider.setVisibility(View.GONE);

            // Change Color of Status Bar
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                getWindow().setStatusBarColor(getResources().getColor(R.color.space_gray_light_2, this.getTheme()));
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                getWindow().setStatusBarColor(getResources().getColor(R.color.space_gray_light_2));
            }
        }

    }

    // ===========================================================================================

    private void zoom(float percentageInDeci) {
        if (cameraControl != null) {
            Log.e(TAG, "Camera Control is NOT null\nZoom Value: " + percentageInDeci);
            myData.setLastZoomValue(percentageInDeci);
            cameraControl.setLinearZoom(percentageInDeci);
        }
    }

    // ===========================================================================================
    private void saveIdolsCSVtoHome() {
        // Persist Idols CSV Save Status
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(HomeActivity.this);
        boolean isCSVSaved = sp.getBoolean("Idols CSV Saved", false);
        Log.e(TAG, "onResume: isCSVSaved? " + isCSVSaved);

        if (!isCSVSaved) {
            if (MyData.getMyData().getMainModule() != null) {
                // Save Idols CSV to Home
                PyObject mainMod = MyData.getMyData().getMainModule();

                boolean isSave = mainMod.callAttr("save_idols_data_to_home").toBoolean();
                Log.e(TAG, "onResume: Idols CSV is Saved? " + isSave);

                boolean success = sp.edit().putBoolean("Idols CSV Saved", isSave).commit();
                Log.e(TAG, "onResume: Successful Commit? " + success);
            }
        }
    }

} // <-- end of HomeActivity Class -->