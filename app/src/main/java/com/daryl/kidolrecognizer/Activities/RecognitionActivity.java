package com.daryl.kidolrecognizer.Activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatImageButton;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.chaquo.python.PyObject;
import com.daryl.kidolrecognizer.Data.Idol;
import com.daryl.kidolrecognizer.Data.MyData;
import com.daryl.kidolrecognizer.R;
import com.daryl.kidolrecognizer.RecyclerView.Role;
import com.daryl.kidolrecognizer.RecyclerView.RolesListAdapterWithRecyclerView;
import com.daryl.kidolrecognizer.RecyclerView.SNS;
import com.daryl.kidolrecognizer.RecyclerView.SNSListAdapterWithRecyclerView;
import com.google.android.material.bottomappbar.BottomAppBar;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.progressindicator.LinearProgressIndicator;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;


public class RecognitionActivity extends AppCompatActivity
        implements View.OnClickListener, CompoundButton.OnCheckedChangeListener, SNSListAdapterWithRecyclerView.OnItemClickListener {

    private static final String TAG = RecognitionActivity.class.getSimpleName();

    // Data
    private MyData myData;

    // Recycler View - Roles & SNS
    private ArrayList<Role> roles = new ArrayList<>();
    private RolesListAdapterWithRecyclerView rolesListAdapterRV;
    private RecyclerView rolesRV;

    private ArrayList<SNS> snsList = new ArrayList<>();
    private SNSListAdapterWithRecyclerView snsListAdapterRV;
    private RecyclerView snsRV;

    // Views
    private TextView
            stageNameTV, realNameTV,
            groupNameTV, entTV,
            heightTV, weightTV, bloodTypeTV,
            nationalityTV, ageTV;
    private ImageView faceIV, capturedIV;
    private MaterialCardView imageFaceCard, ageCard, heightCard, weightCard, bloodTypeCard;
    private AppCompatImageButton backBtn;
    private LinearProgressIndicator recognizerProgInd;
    private CoordinatorLayout mainView;
    private Button draggableHintBtn;

    // Bottom App Bar View
    private BottomAppBar bottomAppBar;
    private MaterialButton saveBtn;
    private CheckBox favoriteBtn;

    // Persistent Bottom Sheet
    private LinearLayout perBottomSheet;
    private BottomSheetBehavior bottomSheetBehavior;
    private final MyBottomBehavior myBottomBehavior = new MyBottomBehavior();

    // Bottom Sheet Dialog
    private BottomSheetDialog bottomSheetDialog;
    private ImageView idolCroppedIV, idolFullIV;
    private MaterialCardView idolCroppedCard, idolFullCard;
    private MaterialButton cancelBtn;

    // Access to Module
    private PyObject mainModule;

    // ===========================================================================================
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recognition);

        // Data
        myData = MyData.getMyData();
        mainModule = myData.getMainModule();

        // Initialize Views
        initViews();

        // Button / Card is Clicked
        backBtn.setOnClickListener(this);
        saveBtn.setOnClickListener(this);
        favoriteBtn.setOnCheckedChangeListener(this);
        ageCard.setOnClickListener(this);
        weightCard.setOnClickListener(this);
        heightCard.setOnClickListener(this);
        bloodTypeCard.setOnClickListener(this);

        // Initialize List Adapter with Recyler View - Roles & SNS
        initListAdapter();
        initSNSListAdapter();

        // Hide Progress Indicator
        recognizerProgInd.hide();

        // Bottom Bar
        setSupportActionBar(bottomAppBar);
        getSupportActionBar().hide();

        // Handle Persistent Bottom Sheet State & Slide
        bottomSheetBehavior = BottomSheetBehavior.from(perBottomSheet);
        bottomSheetBehavior.addBottomSheetCallback(myBottomBehavior);
        bottomAppBar.performHide();
        bottomSheetBehavior.setDraggable(false);

        // Initialize Bottom Sheet Dialog
        initModalBottomSheet();

        // Perform Recognition
        if (mainModule == null) {
            Log.e(TAG, "Main Module is null");
        } else {
            Log.e(TAG, "Main Module is NOT null");
            Bitmap bitmapFull = myData.getRecognizedIdolBitmapFull();
            capturedIV.setImageBitmap(bitmapFull);
            shuffleRecogBtnRippleColor();
            shuffleProgIndColors();
            recognizerProgInd.show();
            Thread recognitionThread = new Thread(
                    new Runnable() {
                        @Override
                        public void run() {
                            long start = System.nanoTime();
                            performRecognition();
                            long end = System.nanoTime();
                            Log.e(TAG, "Time Taken for Recognition: " +
                                    (end - start) * 1000000000);
                            showOrHideProgInd(false);
                        }
                    }
            );
            recognitionThread.start();
        }

    } // <-- end of onCreate -->


    // ===========================================================================================
    private void initViews() {
        // Recognizer
        capturedIV = findViewById(R.id.captured_image_view);
        backBtn = findViewById(R.id.back_button);
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
        imageFaceCard = findViewById(R.id.face_image_card_view);
        ageCard = findViewById(R.id.age_card_view);
        heightCard = findViewById(R.id.height_card_view);
        weightCard = findViewById(R.id.weight_card_view);
        bloodTypeCard = findViewById(R.id.blood_type_card_view);
        groupNameTV = findViewById(R.id.group_name_text_view);
        entTV = findViewById(R.id.entertainment_text_view);
        rolesRV = findViewById(R.id.roles_recycler_view);
        ageTV = findViewById(R.id.age_text_view);
        heightTV = findViewById(R.id.height_text_view);
        weightTV = findViewById(R.id.weight_text_view);
        bloodTypeTV = findViewById(R.id.blood_type_text_view);
        nationalityTV = findViewById(R.id.nationality_text_view);
        snsRV = findViewById(R.id.sns_recycler_view);
        // Bottom Bar for Buttons
        bottomAppBar = findViewById(R.id.bottom_app_bar);
        saveBtn = findViewById(R.id.save_button);
        favoriteBtn = findViewById(R.id.favorite_button);

    }

    // ===========================================================================================
    @Override
    protected void onResume() {
        super.onResume();
        Log.e(TAG, "onResume called");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.e(TAG, "onDestroy called");
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.e(TAG, "onPause called");
    }

    // ===========================================================================================
    private void performRecognition() {

        // Capture Image
        Bitmap bitmapFrame = myData.getRecognizedIdolBitmapFull();

        if (mainModule != null && bitmapFrame != null) {

            String encodeBitmap = encodeBitmapImage(bitmapFrame);

            // Get type of bitmap passed to python
            PyObject type = mainModule.callAttr("getType", bitmapFrame);
            Log.e(TAG, type.toString());

            // Recognize Idol (Average Time to Recognize: 2 sec)
            long start = System.nanoTime();
            PyObject stageNameAndBbox = mainModule.callAttr("detect_face_fr", encodeBitmap);
            long end = System.nanoTime();
            showOrHideProgInd(false);
            Log.e(TAG, "Time Taken to Recognize Face: " + ((end - start) / 1000000000) + "\n" +
                    "Difference in Nano: " + (end - start));

            // Faces To Java List
            List<PyObject> stageNameAndBboxList = stageNameAndBbox.asList();
            Log.e(TAG, stageNameAndBboxList.toString());

            // Check for Empty List of Faces
            if (stageNameAndBboxList.isEmpty()) {
                Log.e(TAG, "No Match");
                setViewValue(stageNameTV, "No Match");
                setViewValue(realNameTV, "Try again");
                setImageViewNoMatch();
                updateOutlineProvider(1);
                bottomSheetBehavior.setDraggable(false);
            } else {
                bottomSheetBehavior.setDraggable(true);
            }

            for (PyObject IdAndBboxE: stageNameAndBboxList) {

                updateOutlineProvider(0);

                // Get Id of Idol
                String id = IdAndBboxE.asList().get(0).toString();
                Log.e(TAG, id);

                // Show Cropped Face
                int x1 = IdAndBboxE.asList().get(1).toInt();
                int x2 = IdAndBboxE.asList().get(2).toInt();
                int y1 = IdAndBboxE.asList().get(3).toInt();
                int y2 = IdAndBboxE.asList().get(4).toInt();
                Bitmap croppedFace = Bitmap.createBitmap(bitmapFrame, x1, y1, x2-x1, y2-y1);
                Log.e(TAG, "Face Axis: " + x1 + " " + x2 + " " + y1 + " " + y2);
                setImageView(croppedFace);

                // Store Bitmap Frame Temporarily (for displaying and saving)
                myData.setRecognizedIdolBitmapCrop(croppedFace);
                myData.setRecognizedIdolBitmapFull(bitmapFrame);
                Log.e(TAG, "Full Bitmap: " + myData.getRecognizedIdolBitmapCrop());
                Log.e(TAG, "Cropped Bitmap: " + myData.getRecognizedIdolBitmapCrop());

                // Get Idol's Profile
                start = System.nanoTime();
                PyObject profile = mainModule.callAttr("get_idol_profile", id);
                end = System.nanoTime();
                Log.e(TAG, "Time Taken to get Profile Values: " + ((end - start) / 1000000000) +
                        " seconds" + "\n" +
                        "Difference in Nano: " + (end - start));
                Log.e(TAG, profile.toString());
                Map<PyObject, PyObject> profileValues = profile.asMap();

                if (!profileValues.isEmpty()) {

                    // Stage Name
                    String stageName = profileValues.get("Stage Name").toString();
                    setViewValue(stageNameTV, stageName);

                    // Real Name
                    String realName = profileValues.get("Real Name (Korean)").toString();
                    setViewValue(realNameTV, realName);

                    // Group Name, Entertainment
                    String group = profileValues.get("Group Name").toString();
                    String entertainment = profileValues.get("Entertainment").toString();
                    setViewValue(groupNameTV, group);
                    setViewValue(entTV, entertainment);

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
                    int age = 0;
                    if (!birthDateStr.equalsIgnoreCase("nan")) {
                        age = calculateAge(birthDateStr);
                        setViewValue(ageTV, age + "");
                    }
                    String height = profileValues.get("Height").toString() + " cm";
                    setViewValue(heightTV, height);
                    String weight = profileValues.get("Weight").toString() + " kg";
                    setViewValue(weightTV, weight);
                    String blood_type = profileValues.get("Blood Type").toString();
                    setViewValue(bloodTypeTV, blood_type);

                    // Nationality
                    String nationality = profileValues.get("Nationality").toString();
                    setViewValue(nationalityTV, nationality);

                    // SNS
                    String personalIG = profileValues.get("Personal IG").toString();
                    String groupIG = profileValues.get("Group IG").toString();
                    snsList.clear();
                    if (!personalIG.equals("None")) {
                        snsList.add(new SNS(personalIG, "Personal IG"));
                    }
                    snsList.add(new SNS(groupIG, "Group IG"));
                    updateSNSRV();

                    // Favorite
                    boolean isFavorite = profileValues.get("Favorite").toBoolean();
                    updateFaveButton(isFavorite);

                    // Store Idol Temporarily (for saving)
                    Idol idol = new Idol(id, stageName, realName, group, entertainment,
                            String.valueOf(age), height, weight, blood_type, nationality,
                            isFavorite, roles, snsList);
                    myData.setIdol(idol);

                    Log.e(TAG, myData.getIdol().getStageName());

                } // <-- end of checking for empty profile values -->

            } // <-- end of iterating faces -->

        } // <-- end of checking for null -->
    }

    // ===========================================================================================
    // Reference: Stackoverflow
    // Touch the UI Components inside a Thread
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

    private void setImageViewNoMatch(){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                faceIV.setImageDrawable(getDrawable(R.drawable.main_no_match_illustration));
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

    private void updateSNSRV(){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                snsListAdapterRV.notifyDataSetChanged();
            }
        });
    }

    private void updateFaveButton(boolean isFave) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                favoriteBtn.setChecked(isFave);
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

    private void updateOutlineProvider(int key) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                switch (key) {
                    case 0:
                        imageFaceCard.setOutlineProvider(ViewOutlineProvider.BACKGROUND);
                        break;
                    case 1:
                        imageFaceCard.setOutlineProvider(null);
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
            case R.id.back_button:
                finish();
                break;
            case R.id.save_button:
                bottomSheetDialog.show();
                if (myData.getRecognizedIdolBitmapCrop() != null && myData.getRecognizedIdolBitmapFull() != null) {
                    Log.e(TAG, "Idol Crop & Full Bitmap Not Null");
                    idolCroppedIV.setImageBitmap(myData.getRecognizedIdolBitmapCrop());
                    idolFullIV.setImageBitmap(myData.getRecognizedIdolBitmapFull());
                }
                break;
            case R.id.idol_cropped_card:
                try {
                    boolean isSaved = saveImage(myData.getIdol().getStageName(), myData.getRecognizedIdolBitmapCrop());
                    bottomSheetDialog.hide();
                    Toast.makeText(this, "Saved to Gallery", Toast.LENGTH_SHORT).show();
                    Log.e(TAG, isSaved + "");
                } catch (IOException e) {
                    Log.e(TAG, "Not saved");
                }
                break;
            case R.id.idol_full_card:
                try {
                    boolean isSaved = saveImage(myData.getIdol().getStageName(), myData.getRecognizedIdolBitmapFull());
                    bottomSheetDialog.hide();
                    Toast.makeText(this, "Saved to Gallery", Toast.LENGTH_SHORT).show();
                    Log.e(TAG, isSaved + "");
                } catch (IOException e) {
                    Log.e(TAG, "Not saved");
                }
                break;
            case R.id.cancel_button:
                bottomSheetDialog.dismiss();
                break;
            case R.id.age_card_view:
                Toast.makeText(this, "Age", Toast.LENGTH_SHORT).show();
                break;
            case R.id.weight_card_view:
                Toast.makeText(this, "Weight", Toast.LENGTH_SHORT).show();
                break;
            case R.id.height_card_view:
                Toast.makeText(this, "Height", Toast.LENGTH_SHORT).show();
                break;
            case R.id.blood_type_card_view:
                Toast.makeText(this, "Blood Type", Toast.LENGTH_SHORT).show();
                break;
        }
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        Log.e(TAG, "isChecked: " + isChecked);
        String id = myData.getIdol().getId();
        if (mainModule != null) {
            String boolVal = isChecked ? "True" : "False";
            PyObject isUpdatedStr = mainModule.callAttr("update_favorite", id, boolVal);
            boolean isUpdated = isUpdatedStr.toBoolean();
            if (isUpdated && isChecked) {
                Toast.makeText(this, "You liked this idol", Toast.LENGTH_SHORT).show();
            } else if (isUpdated && !isChecked) {
                Toast.makeText(this, "You disliked this idol", Toast.LENGTH_SHORT).show();
            }

            PyObject profile = mainModule.callAttr("get_idol_profile", id);
            Map<PyObject, PyObject> profileValues = profile.asMap();
            String stageName = profileValues.get("Stage Name").toString();
            boolean isFavorite = profileValues.get("Favorite").toBoolean();
            Toast.makeText(this, stageName + " " + isFavorite, Toast.LENGTH_SHORT).show();

            // Update Data Idol
            myData.getIdol().setFavorite(isFavorite);

        }
    }

    // ===========================================================================================
    private void initListAdapter() {
        rolesListAdapterRV = new RolesListAdapterWithRecyclerView(roles, this, R.layout.role_item);
        GridLayoutManager layoutManager = new GridLayoutManager(this, 1, RecyclerView.HORIZONTAL, false);
        rolesRV.setLayoutManager(layoutManager);
        rolesRV.setAdapter(rolesListAdapterRV);
    }

    private void initSNSListAdapter() {
        snsListAdapterRV = new SNSListAdapterWithRecyclerView(snsList, this, R.layout.sns_item);
        GridLayoutManager layoutManager = new GridLayoutManager(this, 1, RecyclerView.HORIZONTAL, false);
        snsRV.setLayoutManager(layoutManager);
        snsRV.setAdapter(snsListAdapterRV);
        snsListAdapterRV.setOnItemClickListener(this);
    }

    // SNS List Adapter OnClick
    @Override
    public void onItemClick(int position) {
        Toast.makeText(this, "Position Clicked: " + position, Toast.LENGTH_SHORT).show();
        SNS sns = snsList.get(position);
        launchInstagram(sns.getUsername());
    }

    // ===========================================================================================
    // Reference: https://www.candidjava.com/tutorial/java-program-to-calculate-age-from-date-of-birth/
    private int calculateAge(String birthDateStr) {
        if (!birthDateStr.isEmpty()) {
            // dd/mm/yyyy
            String[] dates =  birthDateStr.split("/");

            int year = Integer.parseInt(dates[2]);
            int monthOfYear = Integer.parseInt(dates[1]);
            int dayOfMonth = Integer.parseInt(dates[0]);

            // API Level 26 (Oreo) and above
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                LocalDate birthDate = LocalDate.of(year, monthOfYear, dayOfMonth);
                LocalDate curDate = LocalDate.now();
                return Period.between(birthDate, curDate).getYears();
            }
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
                    backBtn.setVisibility(View.GONE);
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
                    backBtn.setVisibility(View.VISIBLE);
                }
            }
            lastSlideOffSet = slideOffset;
        }
    }

    // ===========================================================================================
    // Reference: https://www.section.io/engineering-education/bottom-sheet-dialogs-using-android-studio/
    private void initModalBottomSheet() {
        bottomSheetDialog = new BottomSheetDialog(this);
        bottomSheetDialog.setContentView(R.layout.modal_bottom_sheet);
        // Modal Bottom Sheet Views
        idolCroppedIV = bottomSheetDialog.findViewById(R.id.idol_cropped_image_view);
        idolFullIV = bottomSheetDialog.findViewById(R.id.idol_full_image_view);
        idolCroppedCard = bottomSheetDialog.findViewById(R.id.idol_cropped_card);
        idolFullCard = bottomSheetDialog.findViewById(R.id.idol_full_card);
        cancelBtn = bottomSheetDialog.findViewById(R.id.cancel_button);
        // Card is Clicked
        idolFullCard.setOnClickListener(this);
        idolCroppedCard.setOnClickListener(this);
        cancelBtn.setOnClickListener(this);
    }

    // ===========================================================================================
    // Reference: https://stackoverflow.com/questions/63776744/save-bitmap-image-to-specific-location-of-gallery-android-10
    private boolean saveImage(String filename, Bitmap bitmap) throws IOException {
        boolean isSaved = false;
        OutputStream fos;
        String folderName =  "My Kpop Idols";
        // API Level 29 (Q)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContentResolver resolver = getApplicationContext().getContentResolver();
            ContentValues contentValues = new ContentValues();
            contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, filename);
            contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/png");
            contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, "DCIM/" + folderName);
            Uri imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues);
            fos = resolver.openOutputStream(imageUri);
        }
        // API Level 28 (Pie) & Lower
        else {
            String imagesDir = Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_DCIM).toString() + File.separator + folderName;
            File fileDir = new File(imagesDir);
            if (!fileDir.exists()) {
                fileDir.mkdir();
            }
            File imageFile = new File(fileDir, filename + ".png");
            fos = new FileOutputStream(imageFile);
        }
        isSaved = bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
        fos.flush();
        fos.close();
        return isSaved;
    }

    // ===========================================================================================
    // Reference: https://stackoverflow.com/questions/39807071/how-can-i-launch-instagram-app-on-button-click-android-studio
    private void launchInstagram(String username) {
        // Open on Brower / Instgram App
        Uri uri = Uri.parse("https://instagram.com/" + username);
        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        startActivity(intent);
    }

    // ===========================================================================================
    private void shuffleProgIndColors() {
        ArrayList<Integer> accentColors = new ArrayList<>();
        accentColors.add(getColor(R.color.green_accent));
        accentColors.add(getColor(R.color.red_accent));
        accentColors.add(getColor(R.color.yellow_accent));
        accentColors.add(getColor(R.color.blue_accent));
        Collections.shuffle(accentColors);

        // Change Progress Indicator Color
        int color1 = accentColors.get(0);
        int color2 = accentColors.get(1);
        int color3 = accentColors.get(2);
        int color4 = accentColors.get(3);
        recognizerProgInd.setIndicatorColor(color1, color2, color3, color4);
    }

    private void shuffleRecogBtnRippleColor() {
        ArrayList<Drawable> drawables = new ArrayList<>();
        drawables.add(getDrawable(R.drawable.button_recognizer_ripple_green));
        drawables.add(getDrawable(R.drawable.button_recognizer_ripple_red));
        drawables.add(getDrawable(R.drawable.button_recognizer_ripple_yellow));
        drawables.add(getDrawable(R.drawable.button_recognizer_ripple_blue));
        // random number from 0 - 3
        int randNum = new Random().nextInt(drawables.size());
        // Change Recognizer Button Ripple Color
        backBtn.setBackground(drawables.get(randNum));
    }


} // <--  end of RecognitionActivity Class -->