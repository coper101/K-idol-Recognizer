package com.daryl.kidolrecognizer;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatImageButton;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.chaquo.python.PyObject;
import com.chaquo.python.Python;
import com.chaquo.python.android.AndroidPlatform;
import com.google.android.material.bottomappbar.BottomAppBar;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.snackbar.Snackbar;
import com.google.common.util.concurrent.ListenableFuture;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private static String TAG = MainActivity.class.getSimpleName();

    // CameraX
    private ExecutorService cameraExecutor;
    private PreviewView previewView;

    // Data
    private MyData myData = MyData.getMyData();

    // Recycler View
    private ArrayList<Role> roles = new ArrayList<>();
    private RolesListAdapterWithRecyclerView rolesListAdapterRV;
    private RecyclerView rolesRV;

    // Views
    private TextView
            stageNameTV, realNameTV,
            groupNameTV, entTV,
            heightTV, weightTV, bloodTypeTV,
            nationalityTV, ageTV,
            idolIgTV, groupIgTV;
    private ImageView faceIV;
    private AppCompatImageButton recognizerBtn;
    private LinearProgressIndicator recognizerProgInd;
    private CoordinatorLayout mainView;
    private Button draggableHintBtn;

    // Bottom App Bar View
    private BottomAppBar bottomAppBar;
    private MaterialButton saveBtn, favoriteBtn;

    // Persistent Bottom Sheet
    private LinearLayout perBottomSheet;
    private BottomSheetBehavior bottomSheetBehavior;
    private MyBottomBehavior myBottomBehavior = new MyBottomBehavior();

    // Bottom Sheet Dialog
    private BottomSheetDialog bottomSheetDialog;
    ImageView idolCroppedIV, idolFullIV;
    MaterialCardView idolCroppedCard, idolFullCard;

    // Trigger Recognizer
    private boolean isRecognizerActivated;

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
        recognizerBtn.setOnClickListener(this);
        saveBtn.setOnClickListener(this);
        favoriteBtn.setOnClickListener(this);

        // Init List Adapter with Recyler View
        initListAdapter();

        // Hide Progress Indicator
        recognizerProgInd.hide();

        // Bottom Bar
        setSupportActionBar(bottomAppBar);
        getSupportActionBar().hide();

        // Handle Sheet Behavior State & Slide
        bottomSheetBehavior = BottomSheetBehavior.from(perBottomSheet);
        bottomSheetBehavior.addBottomSheetCallback(myBottomBehavior);
        bottomAppBar.performHide();

        // Initialize Bottom Sheet Dialog
        initModalBottomSheet();


    } // <--- end of onCreate --->


    // ===========================================================================================
    private void initViews() {
        // CameraX
        previewView = findViewById(R.id.viewFinder);
        // Recognizer
        recognizerBtn = findViewById(R.id.recognizer_button);
        recognizerProgInd = findViewById(R.id.recognizer_progress_indicator);
        // Persistent Bottom Sheet
        perBottomSheet = findViewById(R.id.persistent_bottom_sheet);
        draggableHintBtn = findViewById(R.id.draggable_hint_button);
        // Recognizer Message
        mainView = findViewById(R.id.main_view);
        // Description
        stageNameTV = findViewById(R.id.stage_name_text_view);
        realNameTV = findViewById(R.id.real_name_text_view);
        faceIV = findViewById(R.id.face_image_view);
        groupNameTV = findViewById(R.id.group_name_text_view);
        entTV = findViewById(R.id.entertainment_text_view);
        rolesRV = findViewById(R.id.roles_recycler_view);
        ageTV = findViewById(R.id.age_text_view);
        heightTV = findViewById(R.id.height_text_view);
        weightTV = findViewById(R.id.weight_text_view);
        bloodTypeTV = findViewById(R.id.blood_type_text_view);
        nationalityTV = findViewById(R.id.nationality_text_view);
        idolIgTV = findViewById(R.id.idol_ig_text_view);
        groupIgTV = findViewById(R.id.group_ig_text_view);
        // Bottom Bar for Buttons
        bottomAppBar = findViewById(R.id.bottom_app_bar);
        saveBtn = findViewById(R.id.save_button);
        favoriteBtn = findViewById(R.id.favorite_button);

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

                if (pyObject != null && encodeBitmap != null && isRecognizerActivated) {

                    // Recognize Idol (Average Time to Recognize: 1 sec)
                    long start = System.currentTimeMillis();
                    PyObject stageNameAndBbox = pyObject.callAttr("detect_face_fr", encodeBitmap);
                    long end = System.currentTimeMillis();
                    showOrHideProgInd(false);
                    isRecognizerActivated = false;
                    Log.e(TAG, "Time Taken: " + (end-start));

                    // Faces To Java List
                    List<PyObject> stageNameAndBboxList = stageNameAndBbox.asList();
                    Log.e(TAG, stageNameAndBboxList.toString());

                    // Check for Empty List of Faces
                    if (stageNameAndBboxList.isEmpty()) {
                        showRecognizerMessage();
                        Log.e(TAG, "No Faces Detected");
                    }

                    for (PyObject stageNameAndBboxE: stageNameAndBboxList) {

                        // Show Idol's Stage Name
                        String stageName = stageNameAndBboxE.asList().get(0).toString();
                        setViewValue(stageNameTV, stageName);
                        Log.e(TAG, stageName);

                        // Show Cropped Face
                        int x1 = stageNameAndBboxE.asList().get(1).toInt();
                        int x2 = stageNameAndBboxE.asList().get(2).toInt();
                        int y1 = stageNameAndBboxE.asList().get(3).toInt();
                        int y2 = stageNameAndBboxE.asList().get(4).toInt();
                        Bitmap croppedFace = Bitmap.createBitmap(bitmapFrame, x1, y1, x2-x1, y2-y1);
                        Log.e(TAG, "Face Axis: " + x1 + " " + x2 + " " + y1 + " " + y2);
                        setImageView(croppedFace);

                        // Store Bitmap Frame Temporarily
                        myData.setRecognizedIdolBitmapCrop(croppedFace);
                        myData.setRecognizedIdolBitmapFull(bitmapFrame);
                        Log.e(TAG, "Full Bitmap: " + myData.getRecognizedIdolBitmapCrop());
                        Log.e(TAG, "Cropped Bitmap: " + myData.getRecognizedIdolBitmapCrop());

                        // Show Idol's Profile
                        PyObject profile = pyObject.callAttr("get_idol_profile", stageName);
                        Log.e(TAG, profile.toString());
                        Map<PyObject, PyObject> profileValues = profile.asMap();

                        if (!profileValues.isEmpty()) {

                            // Real Name
                            setViewValue(realNameTV, profileValues.get("Real Name (Korean)").toString());

                            // Group Name, Entertainment
                            setViewValue(groupNameTV, profileValues.get("Group Name").toString());
                            setViewValue(entTV, profileValues.get("Entertainment").toString());

                            // Roles
                            String rolesString = profileValues.get("Roles").toString();
                            String[] rolesSeparated =  rolesString.split(", ");
                            roles.clear();
                            for (String role: rolesSeparated) {
                                roles.add(new Role(role));
                            }
                            Log.e(TAG, Arrays.toString(rolesSeparated));
                            updateRoleRV();

                            // Stats
                            String birthDateStr = profileValues.get("Birth Date").toString();
                            Log.e(TAG, birthDateStr);
                            if (!birthDateStr.equalsIgnoreCase("nan")) {
                                int age = calculateAge(birthDateStr);
                                setViewValue(ageTV, age + "");
                            }
                            setViewValue(heightTV, profileValues.get("Height").toString() + " cm");
                            setViewValue(weightTV, profileValues.get("Weight").toString() + " kg");
                            setViewValue(bloodTypeTV, profileValues.get("Blood Type").toString());

                            // Nationality
                            setViewValue(nationalityTV, profileValues.get("Nationality").toString());

                        } // <-- end of checking for empty profile values -->

                    } // <-- end of iterating faces -->

                } // <-- end of checking for null -->

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
    // Reference: Stackoverflow
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
                    case R.id.group_name_text_view:
                        groupNameTV.setText(value);
                        break;
                    case R.id.entertainment_text_view:
                        entTV.setText(value);
                        break;
                    case R.id.age_text_view:
                        ageTV.setText(value);
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
                    case R.id.nationality_text_view:
                        nationalityTV.setText(value);
                        break;
                    case R.id.idol_ig_text_view:
                        idolIgTV.setText(value);
                        break;
                    case R.id.group_ig_text_view:
                        groupIgTV.setText(value);
                        break;


                }
            }
        });
    }

    private void setImageView(Bitmap bitmap){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                faceIV.setImageBitmap(bitmap);
            }
        });
    }

    private void updateRoleRV(){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                rolesListAdapterRV.notifyDataSetChanged();
            }
        });
    }

    private void showOrHideProgInd(boolean show){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (show) {
                    recognizerProgInd.show();
                } else {
                    recognizerProgInd.hide();
                }
            }
        });
    }

    private void showRecognizerMessage(){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Snackbar.make(mainView, R.string.message, Snackbar.LENGTH_SHORT)
                        .show();
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
                isRecognizerActivated = true;
                recognizerProgInd.show();
                break;
            case R.id.save_button:
                // Reference: https://www.section.io/engineering-education/bottom-sheet-dialogs-using-android-studio/
                bottomSheetDialog.show();
                if (myData.getRecognizedIdolBitmapCrop() != null && myData.getRecognizedIdolBitmapFull() != null) {
                    Log.e(TAG, "not null");
                    idolCroppedIV.setImageBitmap(myData.getRecognizedIdolBitmapCrop());
                    idolFullIV.setImageBitmap(myData.getRecognizedIdolBitmapFull());
                }
                break;
            case R.id.favorite_button:
                break;
        }
    }

    // ===========================================================================================
    private void initListAdapter() {
        rolesListAdapterRV = new RolesListAdapterWithRecyclerView(roles, this, R.layout.role_item);
        GridLayoutManager layoutManager = new GridLayoutManager(this, 1, RecyclerView.HORIZONTAL, false);
        rolesRV.setLayoutManager(layoutManager);
        rolesRV.setAdapter(rolesListAdapterRV);

        roles.add(new Role("Role 1"));
        roles.add(new Role("Role 2"));
        roles.add(new Role("Role 3"));
        rolesListAdapterRV.notifyDataSetChanged();
    }

    // ===========================================================================================
    // Reference: https://www.candidjava.com/tutorial/java-program-to-calculate-age-from-date-of-birth/
    private int calculateAge(String birthDateStr) {
        if (!birthDateStr.isEmpty()) {
            // dd/mm/yyyy
            String[] dates =  birthDateStr.split("/");

            int year = Integer.valueOf(dates[2]);
            int monthOfYear = Integer.valueOf(dates[1]);
            int dayOfMonth = Integer.valueOf(dates[0]);

            LocalDate birthDate = LocalDate.of(year, monthOfYear, dayOfMonth);
            LocalDate curDate = LocalDate.now();

            return Period.between(birthDate, curDate).getYears();
        }
        return 0;
    }

    // ===========================================================================================
    // Persistent Bottom Sheet Behavior
    private class MyBottomBehavior extends BottomSheetBehavior.BottomSheetCallback {
        private float lastSlideOffSet = 0;
        @Override
        public void onStateChanged(@NonNull View bottomSheet, int newState) {
            switch (newState) {
                case BottomSheetBehavior.STATE_EXPANDED:
                    getSupportActionBar().show();
                    break;
                case BottomSheetBehavior.STATE_COLLAPSED:
                    getSupportActionBar().hide();
                    break;
            }
        }

        @Override
        public void onSlide(@NonNull View bottomSheet, float slideOffset) {
            // Log.e(TAG, "Current SlideOffSet: " + slideOffset);
            // Log.e(TAG, "Last SlideOffSet: " + lastSlideOffSet);
            // Swipping up
            if (slideOffset > lastSlideOffSet) {
                if (slideOffset > 0.99f) {
                    getWindow().setStatusBarColor(getColor(R.color.space_gray));
                    perBottomSheet.setBackground(getDrawable(R.drawable.pers_bottom_sheet_bg_expanded));
                    draggableHintBtn.setBackgroundTintList(ContextCompat.getColorStateList(getApplicationContext(), R.color.space_gray));
                }
                if (slideOffset > 0.5) {
                    getSupportActionBar().show();
                }

                if (slideOffset > 0.3) {
                    recognizerBtn.setVisibility(View.GONE);
                }
            }
            // Swipping Down
            else if (slideOffset < lastSlideOffSet) {
                if (slideOffset < 0.99f) {
                    getWindow().setStatusBarColor(getColor(R.color.transparent));
                    perBottomSheet.setBackground(getDrawable(R.drawable.pers_bottom_sheet_bg));
                    draggableHintBtn.setBackgroundTintList(ContextCompat.getColorStateList(getApplicationContext(), R.color.white_400_A80));
                }
                if (slideOffset < 0.5) {
                    getSupportActionBar().hide();
                }
                if (slideOffset < 0.6) {
                    recognizerBtn.setVisibility(View.VISIBLE);
                }
            }
            lastSlideOffSet = slideOffset;
        }
    }

    // ===========================================================================================
    private void initModalBottomSheet() {
        bottomSheetDialog = new BottomSheetDialog(this);
        bottomSheetDialog.setContentView(R.layout.modal_bottom_sheet);
        idolCroppedIV = bottomSheetDialog.findViewById(R.id.idol_cropped_image_view);
        idolFullIV = bottomSheetDialog.findViewById(R.id.idol_full_image_view);
        idolCroppedCard = bottomSheetDialog.findViewById(R.id.idol_cropped_card);
        idolFullCard = bottomSheetDialog.findViewById(R.id.idol_full_card);
    }


} // <---  end of MainActivity --->