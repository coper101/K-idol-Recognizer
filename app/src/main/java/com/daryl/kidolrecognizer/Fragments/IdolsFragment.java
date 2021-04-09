package com.daryl.kidolrecognizer.Fragments;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageButton;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

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
import com.google.android.material.snackbar.Snackbar;

import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class IdolsFragment extends Fragment
        implements
        IdolListAdapterWithRecyclerView.OnItemCheckedChangeListener,
        IdolListAdapterWithRecyclerView.OnItemClickedListener,
        View.OnClickListener,
        SNSListAdapterWithRecyclerView.OnItemClickListener,
        TextWatcher,
        View.OnKeyListener {

    private static final String TAG = IdolsFragment.class.getSimpleName();

    // Data
    private final MyData myData = MyData.getMyData();
    private final PyObject mainModule = myData.getMainModule();

    // Recycler View Components
    // -> Idols Grid List
    private IdolListAdapterWithRecyclerView idolListAdapterRV;
    private ArrayList<Idol> idolList;
    private RecyclerView idolRV;

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
    private TextView stageNameTV, realNameTV, groupTV, entertainmentTV,
            ageTV, heightTV, weightTV, bloodTypeTV, nationalityTV;
    private BottomSheetBehavior bottomSheetDialogBehavior_Profile;

    // -> Filters
    private BottomSheetDialog bottomSheetDialog_Filters;
    private BottomSheetBehavior bottomSheetDialogBehavior_Filters;
    private AppCompatImageButton cancelBtn_Filters;


    // Search & Filter Views
    EditText searchIdolsET;
    MaterialButton filterBtn;

    // ===========================================================================================
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Initialize Recycle View Components
        // -> Idols Grid List
        idolList = new ArrayList<>();
        idolListAdapterRV = new IdolListAdapterWithRecyclerView(idolList, getContext(), R.layout.idol_item);
        // -> Idol Roles
        roleList = new ArrayList<>();
        rolesListAdapterRV = new RolesListAdapterWithRecyclerView(roleList, getContext(), R.layout.role_item);
        // -> Idol SNS
        snsList = new ArrayList<>();
        snsListAdapterRV = new SNSListAdapterWithRecyclerView(snsList, getContext(), R.layout.sns_item);
    }

    // ===========================================================================================
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_idols, container, false);
        initModalBottomSheet_Profile();
        initModalBottomSheet_Filters();
        initRVComponents(view);
        cancelBtn_Profile.setOnClickListener(this::onClick);
        cancelBtn_Filters.setOnClickListener(this::onClick);
        initViews(view);
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
            populateIdols();
        }
    }

    // ===========================================================================================
    private void populateIdols() {
        PyObject mainModule = myData.getMainModule();
        PyObject idols = mainModule.callAttr("get_all_idols");
        List<PyObject> idolsList = idols.asList();
        Log.e(TAG, "Size: " + idolsList.size());
        Log.e(TAG, idolsList.toString());

        idolList.clear();
        // Add Idols
        for (PyObject faveIdol: idolsList) {
            List<PyObject> faveIdolValues = faveIdol.asList();
            String id = faveIdolValues.get(0).toInt() + "";
            String stageName = faveIdolValues.get(1).toString();
            String groupName = faveIdolValues.get(2).toString();
            boolean isFavorite = faveIdolValues.get(3).toBoolean();
            Idol idol = new Idol(id, stageName, groupName, isFavorite);
            idolList.add(idol);
        }
        // Update List Recycler View
        if (idolList.size() > 0) {
            updateIdolListAdapter();
        }
    }

    // ===========================================================================================
    private void initViews(View view) {
        // Search & Filter
        searchIdolsET = view.findViewById(R.id.search_idols_edit_text);
        filterBtn = view.findViewById(R.id.filter_button);
        searchIdolsET.addTextChangedListener(this);
        searchIdolsET.setOnClickListener(this::onClick);
        searchIdolsET.setOnKeyListener(this);
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
        Toast.makeText(getContext(), "Display Height: " + getDisplayHeight(), Toast.LENGTH_SHORT).show();
        bottomSheetDialogBehavior_Profile.setPeekHeight(getDisplayHeight());
    }

    private void initModalBottomSheet_Filters() {
        bottomSheetDialog_Filters = new BottomSheetDialog(getContext());
        bottomSheetDialog_Filters.setContentView(R.layout.modal_bottom_sheet_filters);
        // Bottom Sheet Views
        cancelBtn_Filters = bottomSheetDialog_Filters.findViewById(R.id.filter_cancel_button);
        // Bottom Sheet Behavior
        bottomSheetDialogBehavior_Filters = bottomSheetDialog_Filters.getBehavior();
        Log.e(TAG, "Display Height: " + getDisplayHeight());
        bottomSheetDialogBehavior_Filters.setPeekHeight(getDisplayHeight());
    }

    @Override
    public void onCheckedChange(int position, boolean isChecked, CheckBox favoriteBtn) {
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

    @Override
    public void onItemClicked(int position) {
        if (bottomSheetDialog_Profile != null) {
            Log.e(TAG, "Bottom Sheet Dialog NOT null");
            Idol idol = idolList.get(position);
            populateProfileDialogSheet(idol);
            bottomSheetDialog_Profile.show();
        }
    }

    @Override
    public void onItemClick(int position) {
        SNS sns = snsList.get(position);
        launchInstagram(sns.getUsername());
    }

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
        }
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

    private void populateProfileDialogSheet(Idol idol) {
        if (mainModule != null) {
            Log.e(TAG, "Main Module NOT null");
            String id = idol.getId();

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


} // end of class
