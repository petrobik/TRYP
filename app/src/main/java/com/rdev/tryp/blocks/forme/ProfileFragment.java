package com.rdev.tryp.blocks.forme;

import android.annotation.SuppressLint;
import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.text.Editable;
import android.text.Selection;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.AutocompletePrediction;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.model.TypeFilter;
import com.google.android.libraries.places.api.net.FetchPlaceRequest;
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.rdev.tryp.ContentActivity;
import com.rdev.tryp.R;
import com.rdev.tryp.autocomplete.AutoCompleteAdapter;
import com.rdev.tryp.blocks.forme.edit_addresses.AddressEditor;
import com.rdev.tryp.blocks.forme.edit_addresses.Editor;
import com.rdev.tryp.model.TripPlace;
import com.rdev.tryp.utils.PreferenceManager;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import afu.org.checkerframework.checker.nullness.qual.NonNull;
import afu.org.checkerframework.checker.nullness.qual.Nullable;
import androidx.appcompat.widget.AppCompatEditText;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import static com.rdev.tryp.utils.Utils.KEY_HOME;
import static com.rdev.tryp.utils.Utils.KEY_RECENT_FROM_1;
import static com.rdev.tryp.utils.Utils.KEY_RECENT_FROM_2;
import static com.rdev.tryp.utils.Utils.KEY_RECENT_TO_1;
import static com.rdev.tryp.utils.Utils.KEY_RECENT_TO_2;
import static com.rdev.tryp.utils.Utils.KEY_WORK;
import static com.rdev.tryp.utils.Utils.closeKeyboard;
import static com.rdev.tryp.utils.Utils.showKeyboard;

@SuppressLint("ValidFragment")
public class ProfileFragment extends Fragment implements AutoCompleteAdapter.onPlacePicked, View.OnClickListener, Editor.IView {

    private Geocoder geocoder;
    private PlacesClient placesClient;
    private TripPlace destination, startPos;
    private AutoCompleteAdapter adapter;
    private RelativeLayout editLayout;

    private View view;
    private AppCompatEditText adressTv, adressTv2;
    private RecyclerView autoCompleteRv;
    private ImageButton back_btn, edit_btn;
    private AppCompatEditText mainEditText;
    private TextView homeEditText, workEditText;
    private CardView cardView;
    private RelativeLayout recentFirst, recentSecond, homeAddress, workAddress;
    private ImageView routeBtn;
    private Editor.IEditor editor;

    @SuppressLint("ValidFragment")
    public ProfileFragment(TripPlace startPos, TripPlace destination) {
        this.destination = destination;
        this.startPos = startPos;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_profile, container, false);

        initView();
        initRecentRoutes();
        initAutoComplete();
        editor = new AddressEditor();
        showFavoriteAddresses();

        adressTv2.requestFocus();
        showKeyboard(Objects.requireNonNull(getContext()));

        return view;
    }

    private void initView() {
        adressTv = view.findViewById(R.id.adress_tv);
        adressTv2 = view.findViewById(R.id.adress_tv_2);
        cardView = view.findViewById(R.id.top_card_view);
        autoCompleteRv = view.findViewById(R.id.autoCompleteRv);
        back_btn = view.findViewById(R.id.back_btn);
        recentFirst = view.findViewById(R.id.recent_relative_layout);
        recentSecond = view.findViewById(R.id.recent_relative_layout_2);
        homeAddress = view.findViewById(R.id.home_location_relative_layout);
        workAddress = view.findViewById(R.id.work_location_relative_layout);
        homeEditText = view.findViewById(R.id.home_tv);
        workEditText = view.findViewById(R.id.work_tv);
        edit_btn = view.findViewById(R.id.edit_btn);
        editLayout = view.findViewById(R.id.edit_layout);
        routeBtn = view.findViewById(R.id.route_btn);

        cardView.setBackgroundResource(R.drawable.card_view_bg);
        autoCompleteRv.setLayoutManager(new LinearLayoutManager(getContext()));
        back_btn.setOnClickListener(this);
        recentSecond.setOnClickListener(this);
        recentFirst.setOnClickListener(this);
        homeAddress.setOnClickListener(this);
        workAddress.setOnClickListener(this);
        edit_btn.setOnClickListener(this);
        routeBtn.setOnClickListener(this);
        homeEditText.setText(PreferenceManager.getString(KEY_HOME));
        workEditText.setText(PreferenceManager.getString(KEY_WORK));
        adressTv.setOnFocusChangeListener((v, hasFocus) -> {
            resetAddressView(adressTv2);
            adapter.setData(new ArrayList<>());
            mainEditText = adressTv;
            setCursotEnd(adressTv);
        });

        adressTv2.setOnFocusChangeListener((v, hasFocus) -> {
            resetAddressView(adressTv);
            adapter.setData(new ArrayList<>());
            mainEditText = adressTv2;
            setCursotEnd(adressTv2);
        });
    }

    private void resetAddressView(TextView textView) {
        if (textView.equals(adressTv)) {
            if (startPos != null) {
                adressTv.setText(startPos.getLocale());
            } else {
                adressTv.setText("");
            }
        }

        if (textView.equals(adressTv2)) {
            if (destination != null) {
                adressTv2.setText(destination.getLocale());
            } else {
                adressTv2.setText("");
            }
        }
    }

    private void showFavoriteAddresses() {
        TripPlace home = PreferenceManager.getTripPlace(KEY_HOME);
        TripPlace work = PreferenceManager.getTripPlace(KEY_WORK);

        if (home != null) {
            homeEditText.setText(home.getLocale());
        } else {
            homeEditText.setText("add home address");
        }

        if (work != null) {
            workEditText.setText(work.getLocale());
        } else {
            workEditText.setText("add work address");
        }
    }

    private void initAutoComplete() {
        geocoder = new Geocoder(getContext());

        if (startPos == null) {
            startPos = new TripPlace();
        }
        if (destination == null) {
            destination = new TripPlace();
        }

        adressTv.setText(startPos.getLocale());
        adressTv2.setText(destination.getLocale());

        adapter = new AutoCompleteAdapter(new ArrayList<>(), ProfileFragment.this);
        autoCompleteRv.setAdapter(adapter);

        //Autocomplete Realisation
        adressTv.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                mainEditText = adressTv;
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                placesClient = Places.createClient(Objects.requireNonNull(getContext()));
                FindAutocompletePredictionsRequest request = FindAutocompletePredictionsRequest.builder()
                        .setTypeFilter(TypeFilter.ADDRESS)
                        .setQuery(charSequence.toString())
                        .build();
                placesClient.findAutocompletePredictions(request).addOnCompleteListener(task -> adapter.setData(Objects.requireNonNull(task.getResult()).getAutocompletePredictions()));
            }

            @Override
            public void afterTextChanged(Editable s) {
                adapter.setData(new ArrayList<>());
            }
        });

        adressTv2.addTextChangedListener(new TextWatcher() {

            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                mainEditText = adressTv2;
                adapter.setData(new ArrayList<>());
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                placesClient = Places.createClient(Objects.requireNonNull(getContext()));
                FindAutocompletePredictionsRequest request = FindAutocompletePredictionsRequest.builder()
                        .setTypeFilter(TypeFilter.ADDRESS)
                        .setQuery(charSequence.toString())
                        .build();
                placesClient.findAutocompletePredictions(request).addOnCompleteListener(task -> adapter.setData(Objects.requireNonNull(task.getResult()).getAutocompletePredictions()));
            }

            @Override
            public void afterTextChanged(Editable s) {
                adapter.setData(new ArrayList<>());
            }
        });

    }

    private void initRecentRoutes() {
        TextView firstFrom, firstTo, secondFrom, secondTo;
        TripPlace place;

        firstFrom = view.findViewById(R.id.from_tv_1);
        firstTo = view.findViewById(R.id.to_tv_1);
        secondFrom = view.findViewById(R.id.from_tv_2);
        secondTo = view.findViewById(R.id.to_tv_2);

        place = PreferenceManager.getTripPlace(KEY_RECENT_FROM_1);
        if (place != null) {
            firstFrom.setText(place.getLocale());
        } else {
            recentFirst.setVisibility(View.INVISIBLE);
            recentFirst.setOnClickListener(null);
        }

        place = PreferenceManager.getTripPlace(KEY_RECENT_TO_1);
        if (place != null) {
            firstTo.setText(place.getLocale());
        }


        place = PreferenceManager.getTripPlace(KEY_RECENT_FROM_2);
        if (place != null) {
            secondFrom.setText(place.getLocale());
        } else {
            recentSecond.setVisibility(View.INVISIBLE);
            recentSecond.setOnClickListener(null);
        }

        place = PreferenceManager.getTripPlace(KEY_RECENT_TO_2);
        if (place != null) {
            secondTo.setText(place.getLocale());
        }
    }

    public Address getLocationFromAddress(Context context, String strAddress) {

        Geocoder coder = new Geocoder(context);
        List<Address> address;
        Address result_location = null;

        try {
            // May throw an IOException
            address = coder.getFromLocationName(strAddress, 5);
            if (address == null) {
                return null;
            }

            result_location = address.get(0);

        } catch (IOException ex) {

            ex.printStackTrace();
        }

        return result_location;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.back_btn:
                ((ContentActivity) Objects.requireNonNull(getActivity())).goHome();
                break;
            case R.id.recent_relative_layout:
                getRecentFirst();
                break;
            case R.id.recent_relative_layout_2:
                getRecentSecond();
                break;
            case R.id.home_location_relative_layout:
                homeEditText.clearFocus();
                setTripPlace(PreferenceManager.getTripPlace(KEY_HOME));
                break;
            case R.id.work_location_relative_layout:
                setTripPlace(PreferenceManager.getTripPlace(KEY_WORK));
                break;
            case R.id.edit_btn:
                editor.editAddresses(getActivity(), this);
                break;
            case R.id.route_btn:
                setupTrip();
        }
    }

    private void setupTrip(){
        if(isTripPlaceNotEmpty(startPos) && isTripPlaceNotEmpty(destination)) {
            onDestination(startPos, destination);
            saveRouteInRecent(startPos, destination);
        }
        closeKeyboard(Objects.requireNonNull(getContext()));
    }

    private boolean isTripPlaceNotEmpty(TripPlace tripPlace){
        if(tripPlace.getLocale() == null){
            return false;
        }
        return tripPlace.getCoord() != null;
    }

    private void setTripPlace(TripPlace tripPlace) {

        if (tripPlace == null) {
            return;
        }

        if (mainEditText == null) {
            mainEditText = adressTv;
        }

        if (mainEditText.equals(adressTv2)) {
            destination = tripPlace;
            resetAddressView(adressTv2);
        }
        if (mainEditText.equals(adressTv)) {
            startPos = tripPlace;
            resetAddressView(adressTv);
        }

        setCursotEnd(mainEditText);
    }

    @Override
    public void onPlace(AutocompletePrediction prediction) {
        if(prediction.getPrimaryText(null).toString().equals(Objects.requireNonNull(mainEditText.getText()).toString())){
            mainEditText.setText(prediction.getFullText(null));
            adapter.data.clear();
            adapter.notifyDataSetChanged();
        } else {
            mainEditText.setText(prediction.getPrimaryText(null));
        }
        setCursotEnd(mainEditText);
        List<Place.Field> placeFields = Arrays.asList(Place.Field.LAT_LNG, Place.Field.NAME);
        FetchPlaceRequest request = FetchPlaceRequest.newInstance(prediction.getPlaceId(), placeFields);
        placesClient.fetchPlace(request).addOnCompleteListener(task -> {
            if (mainEditText.equals(adressTv2)) {
                destination.setCoord(Objects.requireNonNull(task.getResult()).getPlace().getLatLng());
                destination.setLocale(prediction.getFullText(null).toString());
            }
            if (mainEditText.equals(adressTv)) {
                startPos.setCoord(Objects.requireNonNull(task.getResult()).getPlace().getLatLng());
                startPos.setLocale(prediction.getFullText(null).toString());
            }
        });
    }

    private void onDestination(TripPlace start, TripPlace end) {
        ((ContentActivity) Objects.requireNonNull(getActivity())).popBackStack();
        ((ContentActivity) getActivity()).onDestinationPicked(start, end);
    }

    private void saveRouteInRecent(TripPlace start, TripPlace end) {
        TripPlace secondFrom, secondTo;
        secondFrom = PreferenceManager.getTripPlace(KEY_RECENT_FROM_1);
        secondTo = PreferenceManager.getTripPlace(KEY_RECENT_TO_1);

        PreferenceManager.setTripPlace(KEY_RECENT_FROM_1, start);
        PreferenceManager.setTripPlace(KEY_RECENT_TO_1, end);

        PreferenceManager.setTripPlace(KEY_RECENT_FROM_2, secondFrom);
        PreferenceManager.setTripPlace(KEY_RECENT_TO_2, secondTo);

    }

    private void getRecentFirst() {
        startPos = PreferenceManager.getTripPlace(KEY_RECENT_FROM_1);
        destination = PreferenceManager.getTripPlace(KEY_RECENT_TO_1);
        setupTrip();
    }

    private void getRecentSecond() {
        startPos = PreferenceManager.getTripPlace(KEY_RECENT_FROM_2);
        destination = PreferenceManager.getTripPlace(KEY_RECENT_TO_2);
        setupTrip();
    }

    @Override
    public void showAddresses() {
        showFavoriteAddresses();
    }

    private void setCursotEnd(EditText editText){
        int position = editText.length();
        Editable etext = editText.getText();
        Selection.setSelection(etext, position);
    }
}