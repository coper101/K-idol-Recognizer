package com.daryl.kidolrecognizer.Fragments;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Outline;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.view.inputmethod.InputMethodManager;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageButton;
import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.chaquo.python.PyObject;
import com.daryl.kidolrecognizer.Data.Idol;
import com.daryl.kidolrecognizer.Data.MyData;
import com.daryl.kidolrecognizer.Data.Role;
import com.daryl.kidolrecognizer.Data.SNS;
import com.daryl.kidolrecognizer.R;
import com.daryl.kidolrecognizer.RecyclerView.IdolListAdapterWithRecyclerView;
import com.daryl.kidolrecognizer.RecyclerView.RolesListAdapterWithRecyclerView;
import com.daryl.kidolrecognizer.RecyclerView.SNSListAdapterWithRecyclerView;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipDrawable;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class IdolsFragment extends Fragment
        implements
        IdolListAdapterWithRecyclerView.OnItemCheckedChangeListener,
        IdolListAdapterWithRecyclerView.OnItemClickedListener,
        View.OnClickListener,
        SNSListAdapterWithRecyclerView.OnItemClickListener,
        TextWatcher,
        View.OnKeyListener,
        ChipGroup.OnCheckedChangeListener, CompoundButton.OnCheckedChangeListener {

    private static final String TAG = IdolsFragment.class.getSimpleName();
    public static final String PATH = "Kpop_Idols";

    // Data
    private final MyData myData = MyData.getMyData();

    // Recycler View Components
    // -> Idols Grid List
    private IdolListAdapterWithRecyclerView idolListAdapterRV;
    private ArrayList<Idol> idolList;
    private RecyclerView idolRV;
    // for quick access of ID
    private LinkedHashMap<String, Idol> idolMap;

    // -> Idol Roles
    private RolesListAdapterWithRecyclerView rolesListAdapterRV;
    private ArrayList<Role> roleList;
    private RecyclerView roleRV;

    // -> Idol SNS
    private SNSListAdapterWithRecyclerView snsListAdapterRV;
    private ArrayList<SNS> snsList;
    private RecyclerView snsRV;

    // Bottom Sheet Dialog
    // -> Idol Profile
    private BottomSheetDialog bottomSheetDialog_Profile;
    private AppCompatImageButton cancelBtn_Profile;
    private ImageView faceIV;
    private MaterialCardView faceCard;
    private TextView stageNameTV, realNameTV, groupTV, entertainmentTV,
            ageTV, heightTV, weightTV, bloodTypeTV, nationalityTV;
    private BottomSheetBehavior bottomSheetDialogBehavior_Profile;

    // -> Filters
    private BottomSheetDialog bottomSheetDialog_Filters;
    private BottomSheetBehavior bottomSheetDialogBehavior_Filters;
    private AppCompatImageButton cancelBtn_Filters;
    private MaterialButton applyBtn;
    private ChipGroup
            genderFilterChipGroup, bloodTypeChipGroup, entertainmentChipGroup_Filter,
            stageNameChipGroup, groupNameChipGroup, entertainmentChipGroup_Sort, groupDebutYearChipGroup;
    private Chip activeIdols;

    // Search & Filter Views
    EditText searchIdolsET;
    MaterialButton filterBtn;
    int sortAndFilterCount;
    TextView noResultsTV;

    // Firebase
    DatabaseReference kpopIdols = FirebaseDatabase.getInstance().getReference(PATH);

    // Check Internet Connection
    Network activeNetwork;


    // ===========================================================================================
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Initialize Recycle View Components
        // -> Idols Grid List
        idolList = new ArrayList<>();
        idolMap = new LinkedHashMap<>();
        idolListAdapterRV = new IdolListAdapterWithRecyclerView(idolList, getContext(), R.layout.idol_item);
        // -> Idol Roles
        roleList = new ArrayList<>();
        rolesListAdapterRV = new RolesListAdapterWithRecyclerView(roleList, getContext(), R.layout.role_item);
        // -> Idol SNS
        snsList = new ArrayList<>();
        snsListAdapterRV = new SNSListAdapterWithRecyclerView(snsList, getContext(), R.layout.sns_item);
        // Network
        ConnectivityManager connectivityManager = (ConnectivityManager) getContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        activeNetwork = connectivityManager.getActiveNetwork();
        // Track Number of Filters & Sorts
        sortAndFilterCount = 0;
    }

    // ===========================================================================================
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_idols, container, false);
        // Bottom Sheets
        initModalBottomSheet_Profile();
        cancelBtn_Profile.setOnClickListener(this::onClick);
        initModalBottomSheet_Filters();
        cancelBtn_Filters.setOnClickListener(this::onClick);
        // Recycler View Components
        initRVComponents(view);
        // Search & Filter Components
        initSearchAndFilterViews(view);
        filterBtn.setOnClickListener(this::onClick);
        return view;
    }

    // ===========================================================================================
    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        Thread loadIdolsThread = new Thread(new Runnable() {
            @Override
            public void run() {
                // Get All Idols
                while (true) {
                    PyObject mainModule = myData.getMainModule();
                    if (mainModule != null) {
                        Log.e(TAG, "Main Module is NOT null");

                        populateIdols();

                        // Set Image Url of Each Idol Stored in List
                        if (!idolList.isEmpty()) {
                            retrieveIdolFaces();
                        }
                        // Display Can't Load Image Message
                        showNoInternetMessage();

                        // Persist All Idols Data
                        myData.setAllIdols(idolList);

                        // Add Chips Dynamically
                        addChips();
                        enableFilterBtn(true);

                        break;
                    }
                }
            }
        });
        loadIdolsThread.start();
    }

    // ===========================================================================================
    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        Log.e(TAG, "onHiddenChanged: hidden? " + hidden);
        PyObject mainModule = myData.getMainModule();
        if (!hidden && mainModule != null) {
            Thread reloadAllIdols = new Thread(new Runnable() {
                @Override
                public void run() {
                    if (myData.getSortsAndFilterMaps() != null) {
                        sortAndFilterIdols(myData.getSortsAndFilterMaps());
                    } else {
                        populateIdols();
                    }
                    // Display Can't Load Image Message
                    showNoInternetMessage();
                }

            });
            reloadAllIdols.start();
        }
    }

    // ===========================================================================================
    private void populateIdols() {
        noResultsTV.setVisibility(View.GONE);
        // Get All Idols from CSV
        PyObject mainModule = myData.getMainModule();
        PyObject idols = mainModule.callAttr("get_all_idols");
        List<PyObject> idolsList = idols.asList();
        Log.e(TAG, "Size: " + idolsList.size());
        Log.e(TAG, idolsList.toString());

        idolList.clear();
        idolMap.clear();
        // Add Idols
        for (PyObject faveIdol: idolsList) {
            List<PyObject> faveIdolValues = faveIdol.asList();
            String id = faveIdolValues.get(0).toInt() + "";
            String stageName = faveIdolValues.get(1).toString();
            String groupName = faveIdolValues.get(2).toString();
            boolean isFavorite = faveIdolValues.get(3).toBoolean();
            Idol idol = new Idol(id, stageName, groupName, isFavorite);
            idolList.add(idol);
            idolMap.put(idol.getId(), idol);
        }
        // Retrieved Faces & Update List Recycler View
        if (!idolList.isEmpty()) {
            retrieveIdolFaces();
            updateIdolListAdapter();
        }
    }

    private void populateProfileDialogSheet(Idol idol) {
        PyObject mainModule = myData.getMainModule();

        if (mainModule != null) {
            Log.e(TAG, "Main Module NOT null");
            String id = idol.getId();

            // Face Image
            updateIdolFaceImage(idol.getImageUrl());

            // long start = System.nanoTime();
            PyObject profile = mainModule.callAttr("get_idol_profile", id);
            // long end = System.nanoTime();
            // Log.e(TAG, "Time Taken to get Profile Values: " + ((end - start) / 1000000000) +
            // " seconds" + "\n" + "Difference in Nano: " + (end - start));
            // Log.e(TAG, profile.toString());
            Map<PyObject, PyObject> profileValues = profile.asMap();

            if (!profileValues.isEmpty()) {

                // Stage Name
                String stageName = profileValues.get("Stage Name").toString();
                stageNameTV.setText(stageName);

                // Real Name
                String realName = profileValues.get("Real Name (Korean)").toString();
                realNameTV.setText(realName);

                // Group Name
                String group = profileValues.get("Group Name").toString();
                groupTV.setText(group);

                // Entertainment
                String entertainment = profileValues.get("Entertainment").toString();
                entertainmentTV.setText(entertainment);

                // Roles
                String rolesString = profileValues.get("Roles").toString();
                String[] rolesSeparated = rolesString.split(", ");
                roleList.clear();
                for (String role: rolesSeparated) {
                    Role roleObject = new Role(role);
                    roleList.add(roleObject);
                }
                // Log.e(TAG, Arrays.toString(rolesSeparated));
                rolesListAdapterRV.notifyDataSetChanged();

                // Stats
                // -> Age
                String birthDateStr = profileValues.get("Birth Date").toString();
                // Log.e(TAG, birthDateStr);
                int age = 0;
                if (!birthDateStr.equalsIgnoreCase("nan")) {
                    age = calculateAge(birthDateStr);
                    ageTV.setText(age + "");
                }

                // -> Height
                String height = profileValues.get("Height").toString() + " cm";
                heightTV.setText(height);

                // -> Weight
                String weight = profileValues.get("Weight").toString() + " kg";
                weightTV.setText(weight);

                // -> Blood Type
                String bloodType = profileValues.get("Blood Type").toString();
                bloodTypeTV.setText("Type " + bloodType);

                // Nationality
                String nationality = profileValues.get("Nationality").toString();
                nationalityTV.setText(nationality);

                // SNS
                String personalIG = profileValues.get("Personal IG").toString();
                String groupIG = profileValues.get("Group IG").toString();
                snsList.clear();
                if (!personalIG.equals("None")) {
                    SNS personal_ig = new SNS(personalIG, "Personal IG");
                    snsList.add(personal_ig);
                }
                snsList.add(new SNS(groupIG, "Group IG"));
                snsListAdapterRV.notifyDataSetChanged();
            }
        } else {
            Log.e(TAG, "Main Module NULL");
        }
    }

    // ===========================================================================================
    private void initSearchAndFilterViews(View view) {
        // Search & Filter
        searchIdolsET = view.findViewById(R.id.search_idols_edit_text);
        filterBtn = view.findViewById(R.id.filter_button);
        filterBtn.setEnabled(false);
        searchIdolsET.addTextChangedListener(this);
        searchIdolsET.setOnClickListener(this::onClick);
        searchIdolsET.setOnKeyListener(this);
        noResultsTV = view.findViewById(R.id.no_results_text_view);
        noResultsTV.setVisibility(View.GONE);
    }

    private void initRVComponents(View view) {
        // Recycler View Components
        GridLayoutManager layoutManager_IdolList = new GridLayoutManager(getContext(), 2);
        GridLayoutManager layoutManager_SNSList = new GridLayoutManager(getContext(), 1, RecyclerView.HORIZONTAL, false);
        GridLayoutManager layoutManager_RoleList = new GridLayoutManager(getContext(), 1, RecyclerView.HORIZONTAL, false);
        // -> Idol Grid List
        idolRV = view.findViewById(R.id.idols_recycler_view);
        idolRV.setAdapter(idolListAdapterRV);
        idolRV.setLayoutManager(layoutManager_IdolList);
        idolListAdapterRV.setOnItemCheckedChangeListener(this::onCheckedChange);
        idolListAdapterRV.setOnItemClickedListener(this::onItemClicked);
        // -> Idol Roles
        roleRV = bottomSheetDialog_Profile.findViewById(R.id.roles_recycler_view);
        roleRV.setAdapter(rolesListAdapterRV);
        roleRV.setLayoutManager(layoutManager_RoleList);
        // -> Idol SNS
        snsRV = bottomSheetDialog_Profile.findViewById(R.id.sns_recycler_view);
        snsRV.setAdapter(snsListAdapterRV);
        snsRV.setLayoutManager(layoutManager_SNSList);
        snsListAdapterRV.setOnItemClickListener(this::onItemClick);
    }

    private void initModalBottomSheet_Profile() {
        bottomSheetDialog_Profile = new BottomSheetDialog(getContext());
        bottomSheetDialog_Profile.setContentView(R.layout.modal_bottom_sheet_idol_profile);
        // Bottom Sheet Views
        cancelBtn_Profile = bottomSheetDialog_Profile.findViewById(R.id.profile_cancel_button);
        faceIV = bottomSheetDialog_Profile.findViewById(R.id.face_image_view);
        faceCard = bottomSheetDialog_Profile.findViewById(R.id.face_image_card_view);
        stageNameTV = bottomSheetDialog_Profile.findViewById(R.id.stage_name_text_view);
        realNameTV = bottomSheetDialog_Profile.findViewById(R.id.real_name_text_view);
        groupTV = bottomSheetDialog_Profile.findViewById(R.id.group_name_text_view);
        entertainmentTV = bottomSheetDialog_Profile.findViewById(R.id.entertainment_text_view);
        ageTV = bottomSheetDialog_Profile.findViewById(R.id.age_text_view);
        weightTV = bottomSheetDialog_Profile.findViewById(R.id.weight_text_view);
        heightTV = bottomSheetDialog_Profile.findViewById(R.id.height_text_view);
        bloodTypeTV = bottomSheetDialog_Profile.findViewById(R.id.blood_type_text_view);
        nationalityTV = bottomSheetDialog_Profile.findViewById(R.id.nationality_text_view);
        // Bottom Sheet Behavior
        bottomSheetDialogBehavior_Profile = bottomSheetDialog_Profile.getBehavior();
        // Toast.makeText(getContext(), "Display Height: " + getDisplayHeight(), Toast.LENGTH_SHORT).show();
        bottomSheetDialogBehavior_Profile.setPeekHeight(getDisplayHeight());
    }

    private void initModalBottomSheet_Filters() {
        bottomSheetDialog_Filters = new BottomSheetDialog(getContext());
        bottomSheetDialog_Filters.setContentView(R.layout.modal_bottom_sheet_filters);
        // Bottom Sheet Views
        cancelBtn_Filters = bottomSheetDialog_Filters.findViewById(R.id.filter_cancel_button);
        applyBtn = bottomSheetDialog_Filters.findViewById(R.id.sort_and_filter_apply_button);
        applyBtn.setOnClickListener(this);
        // Filter Views
        activeIdols = bottomSheetDialog_Filters.findViewById(R.id.filter_active_idols);
        genderFilterChipGroup = bottomSheetDialog_Filters.findViewById(R.id.filter_gender);
        bloodTypeChipGroup = bottomSheetDialog_Filters.findViewById(R.id.filter_blood_type);
        entertainmentChipGroup_Filter = bottomSheetDialog_Filters.findViewById(R.id.filter_entertainment);
        // Sort Views
        stageNameChipGroup = bottomSheetDialog_Filters.findViewById(R.id.sort_stage_name);
        groupNameChipGroup = bottomSheetDialog_Filters.findViewById(R.id.sort_group_name);
        entertainmentChipGroup_Sort = bottomSheetDialog_Filters.findViewById(R.id.sort_entertainment);
        groupDebutYearChipGroup = bottomSheetDialog_Filters.findViewById(R.id.sort_group_debut_year);
        // Sort & Filter onCheckedChange Listener
        activeIdols.setOnCheckedChangeListener(this::onCheckedChanged);
        genderFilterChipGroup.setOnCheckedChangeListener(this::onCheckedChanged);
        bloodTypeChipGroup.setOnCheckedChangeListener(this::onCheckedChanged);
        entertainmentChipGroup_Filter.setOnCheckedChangeListener(this::onCheckedChanged);
        stageNameChipGroup.setOnCheckedChangeListener(this::onCheckedChanged);
        groupNameChipGroup.setOnCheckedChangeListener(this::onCheckedChanged);
        entertainmentChipGroup_Sort.setOnCheckedChangeListener(this::onCheckedChanged);
        groupDebutYearChipGroup.setOnCheckedChangeListener(this::onCheckedChanged);
        // Bottom Sheet Behavior
        bottomSheetDialogBehavior_Filters = bottomSheetDialog_Filters.getBehavior();
        Log.e(TAG, "Display Height: " + getDisplayHeight());
        bottomSheetDialogBehavior_Filters.setPeekHeight(getDisplayHeight());
    }

    // ===========================================================================================
    @Override
    public void onCheckedChange(int position, boolean isChecked, CheckBox favoriteBtn) {
        PyObject mainModule = myData.getMainModule();
        if (mainModule != null && favoriteBtn.isPressed()) {
            // Update Favorite Column of Idol to True OR False
            String id = idolList.get(position).getId();
            String boolVal = isChecked ? "True" : "False";
            PyObject isUpdatedStr = mainModule.callAttr("update_favorite", id, boolVal);
            boolean isUpdated = isUpdatedStr.toBoolean();
            Log.e(TAG, "Is Favorites Updated: " + isUpdated);

            // Feedback
            if (isUpdated) {
                Snackbar.make(getView(), "You liked this idol.", Snackbar.LENGTH_SHORT)
                        .setAnchorView(getActivity().findViewById(R.id.custom_bottom_navigation))
                        .show();
            }
        }
    }

    // Idol onClick
    @Override
    public void onItemClicked(int position) {
        if (bottomSheetDialog_Profile != null) {
            Log.e(TAG, "Bottom Sheet Dialog NOT null");
            Idol idol = idolList.get(position);
            // Get from CSV
            populateProfileDialogSheet(idol);
            bottomSheetDialog_Profile.show();
        }
    }

    // SNS onClick
    @Override
    public void onItemClick(int position) {
        SNS sns = snsList.get(position);
        launchInstagram(sns.getUsername());
    }

    // Button onClick
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.profile_cancel_button:
                if (bottomSheetDialog_Profile != null) {
                    bottomSheetDialog_Profile.dismiss();
                }
                break;
            case R.id.filter_cancel_button:
                if (bottomSheetDialog_Filters != null) {
                    bottomSheetDialog_Filters.dismiss();
                }
                break;
            case R.id.search_idols_edit_text:
                Toast.makeText(getContext(), "edit text clicked", Toast.LENGTH_SHORT).show();
                break;
            case R.id.filter_button:
                bottomSheetDialog_Filters.show();
                break;
            case R.id.sort_and_filter_apply_button:
                // Get All Selected Sort & Filters
                HashMap<String, HashMap<String, String>> sortsAndFilterMaps = selectedSortsAndFilter();
                Log.e(TAG, "Filters Map: " + sortsAndFilterMaps.get("Filters").toString());
                Log.e(TAG, "Sorts Map: " + sortsAndFilterMaps.get("Sorts").toString());
                if (!sortsAndFilterMaps.get("Filters").isEmpty() || !sortsAndFilterMaps.get("Sorts").isEmpty()) {
                    // Save and Perform Sort & Filters
                    myData.setSortsAndFilterMaps(sortsAndFilterMaps);
                    sortAndFilterIdols(sortsAndFilterMaps);
                } else {
                    // Save Empty Sort & Filters and Load All Idols
                    myData.setSortsAndFilterMaps(null);
                    populateIdols();
                }
                // Perform Sort & Filters
                bottomSheetDialog_Filters.dismiss();
                break;
        }
    }

    // Chip Group
    @Override
    public void onCheckedChanged(ChipGroup group, int checkedId) {
        Log.e(TAG, "onCheckedChanged: " + checkedId + "");
        updateSortAndFilterCount();
    }

    // Chip
    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        Log.e(TAG, "onCheckedChanged: " + isChecked + "");
        updateSortAndFilterCount();
    }

    private void updateSortAndFilterCount() {
        sortAndFilterCount = 0;

        if (activeIdols.isChecked())
            sortAndFilterCount += 1;

        if (genderFilterChipGroup.getCheckedChipId() != -1)
            sortAndFilterCount += 1;

        if (bloodTypeChipGroup.getCheckedChipId() != -1)
            sortAndFilterCount += 1;

        if (entertainmentChipGroup_Filter.getCheckedChipId() != -1)
            sortAndFilterCount += 1;

        if (stageNameChipGroup.getCheckedChipId() != -1)
            sortAndFilterCount += 1;

        if (groupDebutYearChipGroup.getCheckedChipId() != -1)
            sortAndFilterCount += 1;

        if (entertainmentChipGroup_Sort.getCheckedChipId() != -1)
            sortAndFilterCount += 1;

        if (groupNameChipGroup.getCheckedChipId() != -1)
            sortAndFilterCount += 1;

        if (sortAndFilterCount > 0) {
            applyBtn.setText("Apply " + "(" + sortAndFilterCount + ")");
        } else {
            applyBtn.setText("Apply");
        }
        Log.e(TAG, sortAndFilterCount + "");
    }

    // ===========================================================================================
    private int getDisplayHeight() {
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getActivity()
                .getWindowManager()
                .getDefaultDisplay()
                .getRealMetrics(displayMetrics);
        int height = displayMetrics.heightPixels;
        // int width = displayMetrics.widthPixels;
        return height;
    }

    private int calculateAge(String birthDateStr) {
        if (!birthDateStr.isEmpty()) {
            // dd/mm/yyyy
            String[] dates =  birthDateStr.split("/");

            int birthYear = Integer.parseInt(dates[2]);
            int monthOfYear = Integer.parseInt(dates[1]);
            int dayOfMonth = Integer.parseInt(dates[0]);

            LocalDate birthDate = LocalDate.of(birthYear, monthOfYear, dayOfMonth);
            LocalDate curDate = LocalDate.now();
            return Period.between(birthDate, curDate).getYears();
        }
        return 0;
    }

    private void launchInstagram(String username) {
        Uri uri = Uri.parse("https://instagram.com/" + username);
        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        startActivity(intent);
    }

    // ===========================================================================================
    // Text Watcher Methods

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {

    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {

    }

    @Override
    public void afterTextChanged(Editable s) {
        searchIdolsET.clearFocus();
    }

    @Override
    public boolean onKey(View v, int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_ENTER) {
            InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(getContext().INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(searchIdolsET.getWindowToken(), 0);
            filterBtn.setVisibility(View.VISIBLE);
            searchIdolsET.clearFocus();
        }
        return false;
    }

    // ===========================================================================================
    private void updateIdolListAdapter() {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                idolListAdapterRV.notifyDataSetChanged();
            }
        });
    }

    private void updateIdolFaceImage(String imageUrl) {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (imageUrl != null) {
                    Glide.with(getContext()).load(imageUrl).into(faceIV);
                    faceCard.setOutlineProvider(ViewOutlineProvider.BACKGROUND);
                } else {
                    Glide.with(getContext()).clear(faceIV);
                    faceCard.setOutlineProvider(null);
                }
            }
        });
    }

    private void enableFilterBtn(boolean isEnabled) {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                filterBtn.setEnabled(isEnabled);
            }
        });
    }

    // ===========================================================================================
    private void retrieveIdolFaces() {
        Log.e(TAG, "retrieveIdolFaces: In");

        kpopIdols.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Log.e(TAG, "onDataChange: In");
                for (DataSnapshot dataSnapshot: snapshot.getChildren()) {
                    String idolId = dataSnapshot.getKey();
                    String idolImageUrl = dataSnapshot.child("Image_Url").getValue(String.class);
                    String idolStageName = dataSnapshot.child("Stage_Name").getValue(String.class);
                    Log.e(TAG, "onDataChange: " + "\n" +
                            "Idol ID: " + idolId + "\n" +
                            "Idol Image Url: " + idolImageUrl + "\n" +
                            "Idol Stage Name: " + idolStageName);
//                    int idolIndex = Integer.parseInt(idolId) - 1;
//                    Log.e(TAG, "onDataChange: idolIndex - " + idolIndex);
//                    Idol idol = idolList.get(idolIndex);
//                    idol.setImageUrl(idolImageUrl);
//                    Log.e(TAG, "onDataChange: " + idol.getImageUrl());

                    if (idolMap.containsKey(idolId)) {
                        int idolIdx = idolList.indexOf(idolMap.get(idolId));
                        idolList.get(idolIdx).setImageUrl(idolImageUrl);
                    }
                }
                updateIdolListAdapter();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "onCancelled: " + error);
            }

        });
    }

    private void showNoInternetMessage() {
        // No Internet Connection
        if (activeNetwork == null) {
            Snackbar.make(getView(), "Connect to Internet to View Images", Snackbar.LENGTH_LONG)
                    .setAnchorView(getActivity().findViewById(R.id.custom_bottom_navigation))
                    .show();
        } else {
            Log.e(TAG, "showNoInternetMessage: Connected to Internet");
        }
    }

    // Add Blood Type & Entertainments Dynamically from CSV
    private void addChips() {
        PyObject mainModule = myData.getMainModule();
        if (mainModule != null) {
            // Get Unique Blood Types
            PyObject bloodTypes = mainModule.callAttr("unique_values_from_col", "Blood Type");
            List<PyObject> bloodTypeList = bloodTypes.asList();
            Log.e(TAG, bloodTypeList.toString());
            for (int i = 0; i < bloodTypeList.size(); i++) {
                String bloodTypeString = bloodTypeList.get(i).toString();
                if (!bloodTypeString.equals("nan")) {
                    // Add Chip to Chip Group for Each Blood Type
                    Chip chip = new Chip(getContext());
                    ChipDrawable chipDrawable = ChipDrawable.createFromAttributes(getContext(), null, 0, R.style.FilterChip);
                    chip.setChipDrawable(chipDrawable);
                    chip.setText(bloodTypeString);
                    chip.setWidth(ViewGroup.LayoutParams.WRAP_CONTENT);
                    chip.setHeight(ViewGroup.LayoutParams.MATCH_PARENT);
                    chip.setMinHeight(toPx(56));
                    chip.setMinimumHeight(toPx(56));
                    chip.setTextAppearance(getContext(), R.style.ChipTextAppearance);
                    chip.setChipEndPadding(toPx(12));
                    chip.setChipStartPadding(toPx(12));
                    chip.setOutlineProvider(null);
                    chip.setElevation(0);
                    bloodTypeChipGroup.addView(chip);
                }
            }
            // Get Unique Entertainment Names
            PyObject entNames = mainModule.callAttr("unique_values_from_col", "Entertainment");
            List<PyObject> entNameList = entNames.asList();
            Log.e(TAG, entNameList.toString());
            for (int i = 0; i < entNameList.size(); i++) {
                String entNameString = entNameList.get(i).toString();
                if (!entNameString.equals("nan")) {
                    // Add Chip to Chip Group for Each Blood Type
                    Chip chip = new Chip(getContext());
                    ChipDrawable chipDrawable = ChipDrawable.createFromAttributes(getContext(), null, 0, R.style.FilterChip);
                    chip.setChipDrawable(chipDrawable);
                    chip.setText(entNameString);
                    chip.setWidth(ViewGroup.LayoutParams.WRAP_CONTENT);
                    chip.setHeight(ViewGroup.LayoutParams.MATCH_PARENT);
                    chip.setMinHeight(toPx(56));
                    chip.setMinimumHeight(toPx(56));
                    chip.setTextAppearance(getContext(), R.style.ChipTextAppearance);
                    chip.setChipEndPadding(toPx(12));
                    chip.setChipStartPadding(toPx(12));
                    chip.setOutlineProvider(null);
                    chip.setElevation(0);
                    entertainmentChipGroup_Filter.addView(chip);
                }
            }
        }
    }

    private int toPx(int dp) {
        float density = getContext().getResources().getDisplayMetrics().density;
        int px = (int) (dp * density);
        return px;
    }

    // ===========================================================================================
    private HashMap<String, HashMap<String, String>> selectedSortsAndFilter() {
        HashMap<String, HashMap<String, String>> sortAndFilterMaps = new HashMap<>();
        HashMap<String, String> filtersMap = new HashMap<>();
        HashMap<String, String> sortsMap = new HashMap<>();
        // -> Filters
        // Active Idols - True or False
        boolean isActive = activeIdols.isChecked();
        if (isActive) {
            filtersMap.put("Active", "True");
        }
        // Gender - Male or Female
        int genderId = genderFilterChipGroup.getCheckedChipId();
        if (genderId != -1) {
            Chip genderChip = bottomSheetDialog_Filters.findViewById(genderId);
            String gender = genderChip.getText().toString();
            filtersMap.put("Gender", "'" + gender + "'");
        }
        // Blood Type
        for (int i = 0; i < bloodTypeChipGroup.getChildCount(); i++){
            Chip bloodTypeChip = (Chip) bloodTypeChipGroup.getChildAt(i);
            if (bloodTypeChip.isChecked()) {
                String bloodType = bloodTypeChip.getText().toString();
                filtersMap.put("Blood Type", "'" + bloodType + "'");
            }
        }
        // Entertainment
        for (int i = 0; i < entertainmentChipGroup_Filter.getChildCount(); i++){
            Chip entChip = (Chip) entertainmentChipGroup_Filter.getChildAt(i);
            if (entChip.isChecked()) {
                String ent = entChip.getText().toString();
                filtersMap.put("Entertainment", "'" + ent + "'");
            }
        }
        // -> Sort
        // Stage Name
        int stageNameOrderId = stageNameChipGroup.getCheckedChipId();
        if (stageNameOrderId != -1) {
            Chip stageNameOrderChip = stageNameChipGroup.findViewById(stageNameOrderId);
            String stageNameOrder = stageNameOrderChip.getText().toString();
            int orderBoolNum = stageNameOrder.equals("A - Z") ? 1 : 0;
            sortsMap.put("Stage Name", orderBoolNum + "");
        }
        // Group Name
        int groupNameOrderId = groupNameChipGroup.getCheckedChipId();
        if (groupNameOrderId != -1) {
            Chip groupNameOrderIdChip = groupNameChipGroup.findViewById(groupNameOrderId);
            String groupNameOrder = groupNameOrderIdChip.getText().toString();
            int orderBoolNum = groupNameOrder.equals("A - Z") ? 1 : 0;
            sortsMap.put("Group Name", orderBoolNum + "");
        }
        // Entertainment
        int entOrderId = entertainmentChipGroup_Sort.getCheckedChipId();
        if (entOrderId != -1) {
            Chip entOrderChip = entertainmentChipGroup_Sort.findViewById(entOrderId);
            String entOrder = entOrderChip.getText().toString();
            int orderBoolNum = entOrder.equals("A - Z") ? 1 : 0;
            sortsMap.put("Entertainment", orderBoolNum + "");
        }
        // Group Debut Year
        int yearOrderId = groupDebutYearChipGroup.getCheckedChipId();
        if (yearOrderId != -1) {
            Chip yearOrderChip = groupDebutYearChipGroup.findViewById(yearOrderId);
            String yearOrder = yearOrderChip.getText().toString();
            int orderBoolNum = yearOrder.equals("Recent - Oldest") ? 0 : 1;
            sortsMap.put("Group Debut Year", orderBoolNum + "");
        }
        sortAndFilterMaps.put("Filters", filtersMap);
        sortAndFilterMaps.put("Sorts", sortsMap);
        return sortAndFilterMaps;
    }

    private void sortAndFilterIdols(HashMap<String, HashMap<String, String>> sortsAndFilterMaps) {

        // Store Filter Values (Conditional Format)
        String filterQuery = "";

        // Store Sort Values (Column Names & Order Numbers)
        final int sortSize = sortsAndFilterMaps.get("Sorts").size();
        String[] sortColumns = new String[sortSize];
        int[] sortOrders = new int[sortSize];

        int count = 0;
        for (Map.Entry<String, HashMap<String, String>> entry: sortsAndFilterMaps.entrySet()) {
            Log.e(TAG, "onClick: " + entry.getKey());
            for (Map.Entry<String, String> e: entry.getValue().entrySet()) {
                // Log.e(TAG, "onClick: " + e.getKey() + " " + e.getValue());
                // -> Build Filter Query
                if (entry.getKey().equals("Filters")) {
                    String column = e.getKey().replace(" ", "_");
                    String value = e.getValue();
                    filterQuery += column + " == " + value;
                    if (count != entry.getValue().entrySet().size() - 1)
                        filterQuery += " & ";
                }
                // -> Add to Sort Array
                else {
                    String column = e.getKey().replace(" ", "_");
                    int value = Integer.parseInt(e.getValue());
                    sortColumns[count] = column;
                    sortOrders[count] = value;
                }
                count += 1;
            }
            count = 0;
        }

        Log.e(TAG, "Filter Query: " + filterQuery);
        Log.e(TAG, "Sort Columns " + Arrays.toString(sortColumns));
        Log.e(TAG, "Sort Values " + Arrays.toString(sortOrders));

        // Populate Idols
        populateSortedAndFilteredIdols(filterQuery, sortColumns, sortOrders);
    }

    private void populateSortedAndFilteredIdols(String filterQuery, String[] sortColumns, int[] sortOrders) {
        PyObject mainModule = myData.getMainModule();
        if (mainModule != null) {
            PyObject idols = mainModule.callAttr("sort_and_filter", filterQuery, sortColumns, sortOrders);

            List<PyObject> idolsList = idols.asList();
            Log.e(TAG, "Size: " + idolsList.size());
            Log.e(TAG, idolsList.toString());

            if (!idolsList.isEmpty()) {
                Log.e(TAG, "Idols Found");
                noResultsTV.setVisibility(View.GONE);

                idolList.clear();
                idolMap.clear();

                // Add Idols
                for (PyObject curIdol: idolsList) {
                    List<PyObject> idolValues = curIdol.asList();
                    String id = idolValues.get(0).toInt() + "";
                    String stageName = idolValues.get(1).toString();
                    String groupName = idolValues.get(2).toString();
                    boolean isFavorite = idolValues.get(3).toBoolean();
                    Idol idol = new Idol(id, stageName, groupName, isFavorite);
                    idolList.add(idol);
                    idolMap.put(idol.getId(), idol);
                }

                // Retrieved Faces & Update List Recycler View
                retrieveIdolFaces();
                updateIdolListAdapter();
            } else {
                Log.e(TAG, "No Idols Found");
                noResultsTV.setVisibility(View.VISIBLE);
            }

            Log.e(TAG, "Idol List: " + idolList.toString());
            Log.e(TAG, "Idol Map: " + idolMap.toString());

        }
    }

} // end of class
