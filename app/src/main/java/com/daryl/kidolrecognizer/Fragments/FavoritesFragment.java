package com.daryl.kidolrecognizer.Fragments;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageButton;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.chaquo.python.PyObject;
import com.daryl.kidolrecognizer.Activities.HomeActivity;
import com.daryl.kidolrecognizer.Data.Idol;
import com.daryl.kidolrecognizer.Data.MyData;
import com.daryl.kidolrecognizer.Data.Role;
import com.daryl.kidolrecognizer.Data.SNS;
import com.daryl.kidolrecognizer.R;
import com.daryl.kidolrecognizer.RecyclerView.FaveIdolListAdapterWithRecyclerView;
import com.daryl.kidolrecognizer.RecyclerView.RolesListAdapterWithRecyclerView;
import com.daryl.kidolrecognizer.RecyclerView.SNSListAdapterWithRecyclerView;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class FavoritesFragment extends Fragment
        implements
        View.OnClickListener,
        FaveIdolListAdapterWithRecyclerView.OnItemCheckedChangeListener,
        FaveIdolListAdapterWithRecyclerView.OnItemClickedListener,
        SNSListAdapterWithRecyclerView.OnItemClickListener{

    private static final String TAG = FavoritesFragment.class.getSimpleName();
    public static final String PATH = "Kpop_Idols";

    // Data
    private final MyData myData = MyData.getMyData();

    // Recycler View Components
    // -> Favorite Idols
    private FaveIdolListAdapterWithRecyclerView faveIdolsAdapterRV;
    private ArrayList<Idol> faveIdolList;
    private RecyclerView faveIdolsRV;

    // -> Idol Roles
    private RolesListAdapterWithRecyclerView rolesListAdapterRV;
    private ArrayList<Role> roleList;
    private RecyclerView roleRV;

    // -> Idol SNS
    private SNSListAdapterWithRecyclerView snsListAdapterRV;
    private ArrayList<SNS> snsList;
    private RecyclerView snsRV;

    // Rearranging List Items
    private final ItemTouchHelper itemTouchHelper = new ItemTouchHelper(new TouchHelper());

    // Views
    private LinearLayout emptyFaveIllus;
    private MaterialButton galleryLinkBtn, rearrangeBtn;
    private LinearLayout rearrangeContainer;

    // Bottom Sheet Dialog
    // -> Idol Profile
    private BottomSheetDialog bottomSheetDialog_Profile;
    private AppCompatImageButton cancelBtn_Profile;
    private ImageView faceIV;
    private MaterialCardView faceCard;
    private TextView stageNameTV, realNameTV, groupTV, entertainmentTV,
            ageTV, heightTV, weightTV, bloodTypeTV, nationalityTV;
    private BottomSheetBehavior bottomSheetDialogBehavior_Profile;

    // Firebase
    DatabaseReference kpopIdols = FirebaseDatabase.getInstance().getReference(PATH);

    // ===========================================================================================
    // Unable to Access Views here
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.e(TAG, "onCreate");
        // Initialize Recycle View Components
        // -> Favorite Idols
        faveIdolList = new ArrayList<>();
        faveIdolsAdapterRV = new FaveIdolListAdapterWithRecyclerView(faveIdolList, getContext(), R.layout.fave_idol_item);
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
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Log.e(TAG, "onCreateView");
        View view = inflater.inflate(R.layout.fragment_favorites, container, false);
        // Empty Illustration
        initEmptyIllusComponents(view);
        // Rearranging Fave Idols Components
        initRearrangingFavesComponents(view);
        // Bottom Sheet
        initModalBottomSheet_Profile();
        cancelBtn_Profile.setOnClickListener(this::onClick);
        // Recycler View Components
        initRVComponents(view);
        return view;
    }

    // ===========================================================================================
    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        Log.e(TAG, "onActivityCreated");

        // Hide Views
        galleryLinkBtn.setVisibility(View.GONE);
        rearrangeContainer.setVisibility(View.GONE);

        // Get Favorite Idols
        Thread loadFavoritesThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    PyObject mainModule = myData.getMainModule();
                    if (mainModule != null) {
                        Log.e(TAG, "Main Module is NOT null");

                        // Populate List Recycler View
                        populateFavoriteIdols();

                        // Set Image Url of Each Idol Stored in List
                        if (!faveIdolList.isEmpty()) {
                            retrieveIdolFaces();
                        }

                        break;
                    }
                }
            }
        });
        loadFavoritesThread.start();
    }

    // ===========================================================================================
    @Override
    public void onResume() {
        super.onResume();
        Log.e(TAG, "onResume");
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.e(TAG, "onPause");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.e(TAG, "onDestroy");
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        Log.e(TAG, "onHiddenChanged: hidden? " + hidden);
        PyObject mainModule = myData.getMainModule();
        if (!hidden && mainModule != null) {
            Log.e(TAG, "onHiddenChanged: Main Module is NOT null");
            // Populate List Recycler View
            populateFavoriteIdols();
            // Set Image Url of Each Idol Stored in List
            if (!faveIdolList.isEmpty()) {
                retrieveIdolFaces();
            }
        }
    }

    // ===========================================================================================
    private void initEmptyIllusComponents(View view) {
        emptyFaveIllus = view.findViewById(R.id.empty_favorites_illustration);
        galleryLinkBtn = view.findViewById(R.id.gallery_link_button);
    }

    private void initRearrangingFavesComponents(View view) {
        rearrangeContainer = view.findViewById(R.id.rearrange_box);
        rearrangeBtn = view.findViewById(R.id.rearrange_button);
        rearrangeBtn.setOnClickListener(this);
    }

    private void initRVComponents(View view) {
        // Recycler View Components
        LinearLayoutManager layoutManager_faveIdolList = new LinearLayoutManager(getContext(), RecyclerView.VERTICAL, false);
        GridLayoutManager layoutManager_SNSList = new GridLayoutManager(getContext(), 1, RecyclerView.HORIZONTAL, false);
        GridLayoutManager layoutManager_RoleList = new GridLayoutManager(getContext(), 1, RecyclerView.HORIZONTAL, false);
        // -> Idol Grid List
        faveIdolsRV = view.findViewById(R.id.favorite_idols_recycler_view);
        faveIdolsRV.setAdapter(faveIdolsAdapterRV);
        faveIdolsRV.setLayoutManager(layoutManager_faveIdolList);
        faveIdolsAdapterRV.setOnItemCheckedChangeListener(this::onCheckedChange);
        faveIdolsAdapterRV.setOnItemClickedListener(this::onItemClicked);
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

    // ===========================================================================================
    private void populateProfileDialogSheet(Idol idol) {
        PyObject mainModule = myData.getMainModule();

        // Face Image
        updateIdolFaceImage(idol.getImageUrl());

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

    private void populateFavoriteIdols() {
        PyObject mainModule = myData.getMainModule();
        PyObject faveIdols = mainModule.callAttr("get_favorite_idols");
        List<PyObject> faveIdolsList = faveIdols.asList();
        Log.e(TAG, "Size: " + faveIdolsList.size());

        faveIdolList.clear();
        // Add Idols
        for (PyObject faveIdol: faveIdolsList) {
            List<PyObject> faveIdolValues = faveIdol.asList();
            String id = faveIdolValues.get(0).toInt() + "";
            String stageName = faveIdolValues.get(1).toString();
            String groupName = faveIdolValues.get(2).toString();
            Idol idol = new Idol(id, stageName, groupName);
            faveIdolList.add(idol);
        }
        // Update List Recycler View
        if (faveIdolList.size() > 0) {
            updateIdolsAdapter();
        }

        // Show Empty State
        showEmptyIllustration();
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
    private void updateIdolsAdapter() {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                faveIdolsAdapterRV.notifyDataSetChanged();
            }
        });
    }

    private void updateVisibility(View view, boolean isVisible) {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (isVisible) {
                    view.setVisibility(View.VISIBLE);
                } else {
                    view.setVisibility(View.GONE);
                }
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

    // ===========================================================================================
    private void showEmptyIllustration() {
        if (faveIdolList.size() > 0) {
            updateVisibility(emptyFaveIllus, false);
            updateVisibility(galleryLinkBtn, true);
            updateVisibility(rearrangeContainer, true);
        } else {
            updateVisibility(emptyFaveIllus, true);
            updateVisibility(galleryLinkBtn, false);
            updateVisibility(rearrangeContainer, false);
        }
    }

    // ===========================================================================================
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.rearrange_button:
                if (rearrangeBtn.getText().equals("Done")) {
                    itemTouchHelper.attachToRecyclerView(null);
                    rearrangeBtn.setText("Rearrange");
                } else if (rearrangeBtn.getText().equals("Rearrange")) {
                    rearrangeBtn.setText("Done");
                    itemTouchHelper.attachToRecyclerView(faveIdolsRV);
                }
                break;
            case R.id.profile_cancel_button:
                if (bottomSheetDialog_Profile != null) {
                    bottomSheetDialog_Profile.dismiss();
                }
                break;
        }
    }

    // FaveIdol onCheckedChange
    @Override
    public void onCheckedChange(int position, boolean isChecked) {
        if (!isChecked) {
            Toast.makeText(getContext(), "Unchecked", Toast.LENGTH_SHORT).show();

            PyObject mainModule = myData.getMainModule();
            if (mainModule != null) {

                // Update Favorite Column of Idol to False
                String id = faveIdolList.get(position).getId();
                String boolVal = "False";
                PyObject isUpdatedStr = mainModule.callAttr("update_favorite", id, boolVal);
                boolean isUpdated = isUpdatedStr.toBoolean();
                Log.e(TAG, "Is Favorites Updated: " + isUpdated);

                // Remove Idol from List Recycler View
                faveIdolList.remove(position);
                faveIdolsAdapterRV.notifyDataSetChanged();
                Snackbar.make(getView(), "Removed from Favorites.", Snackbar.LENGTH_SHORT)
                        .setAnchorView(getActivity().findViewById(R.id.custom_bottom_navigation))
                        .show();
                showEmptyIllustration();

            } // end of checking main module is not null

        } // end of checking is not checked
    }

    // FaveIdol onClick
    @Override
    public void onItemClicked(int position) {
        if (bottomSheetDialog_Profile != null) {
            Log.e(TAG, "Bottom Sheet Dialog NOT null");
            Idol idol = faveIdolList.get(position);
            // Get from CSV
            populateProfileDialogSheet(idol);
            bottomSheetDialog_Profile.show();
        } else {
            Log.e(TAG, "Bottom Sheet Dialog is null");
        }
    }

    // SNS onClick
    @Override
    public void onItemClick(int position) {
        SNS sns = snsList.get(position);
        launchInstagram(sns.getUsername());
    }

    // ===========================================================================================
    // Reference: Youtube - Drag and drop Reorder in Recycler View | Android
    private class TouchHelper extends ItemTouchHelper.SimpleCallback {

//        /**
//         * Creates a Callback for the given drag and swipe allowance. These values serve as
//         * defaults
//         * and if you want to customize behavior per ViewHolder, you can override
//         * {@link #getSwipeDirs(RecyclerView, ViewHolder)}
//         * and / or {@link #getDragDirs(RecyclerView, ViewHolder)}.
//         *
//         * @param dragDirs  Binary OR of direction flags in which the Views can be dragged. Must be
//         *                  composed of {@link #LEFT}, {@link #RIGHT}, {@link #START}, {@link
//         *                  #END},
//         *                  {@link #UP} and {@link #DOWN}.
//         * @param swipeDirs Binary OR of direction flags in which the Views can be swiped. Must be
//         *                  composed of {@link #LEFT}, {@link #RIGHT}, {@link #START}, {@link
//         *                  #END},
//         *                  {@link #UP} and {@link #DOWN}.
//         */

        public TouchHelper() {
            super(ItemTouchHelper.UP | ItemTouchHelper.DOWN |
                            ItemTouchHelper.START | ItemTouchHelper.END,
                    0);
        }

        @Override
        public boolean onMove(@NonNull RecyclerView recyclerView,
                              @NonNull RecyclerView.ViewHolder viewHolder,
                              @NonNull RecyclerView.ViewHolder target) {

            int fromPos = viewHolder.getAdapterPosition();
            int toPos = target.getAdapterPosition();
            // Update Array
            Collections.swap(faveIdolList, fromPos, toPos);
            // Update Display
            faveIdolsAdapterRV.notifyItemMoved(fromPos, toPos);
            return true;
        }

        @Override
        public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {

        }
    }


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

                    for (Idol faveIdol: faveIdolList) {
                        if (faveIdol.getId().equals(idolId)) {
                            // Set Image Url of Idol if its Matches the ID
                            faveIdol.setImageUrl(idolImageUrl);
                            Log.e(TAG, "onDataChange: " + faveIdol.getImageUrl());
                        }
                    }
                }
                updateIdolsAdapter();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "onCancelled: " + error);
            }

        });
    }

} // end of class
