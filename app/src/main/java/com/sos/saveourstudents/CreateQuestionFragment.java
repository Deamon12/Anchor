package com.sos.saveourstudents;

import android.content.Context;
import android.content.SharedPreferences;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.JsonObjectRequest;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.rey.material.widget.EditText;

import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;


/**
 * This Fragment has 2 modes; Create question and Edit question and this distinction is distinguished after getQuestionData call
 */
public class CreateQuestionFragment extends Fragment implements View.OnClickListener, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener{

    private String mQuestionId;
    private ArrayList<String> tagList;
    private JSONObject mQuestionInfo;

    private SharedPreferences sharedPref;
    private Context mContext;
    public GoogleApiClient mGoogleApiClient;
    LocationRequest mLocationRequest;
    Location mCurrentLocation;

    private EditText questionEditText, topicEditText;
    private ImageView userImage;
    private ImageView sendButton, addTagsButton, locationToggle, groupToggle, tutorToggle;
    private TextView userName, requestGroupText, requestTutorText;
    private boolean showLocation = true;
    private boolean isInEditMode = false; //For response back to edit question frag
    LayoutInflater inflater;
    View rootView;

    private String mInGroupUrl =
            "http://54.200.33.91:8080/com.mysql.services/rest/serviceclass/inGroup?userId=";
    private String mCurrentUserId;


    /**
     * Static instantiation with questionId param
     * @param questionId String
     * @return CreateQuestionFragment
     */
    public static CreateQuestionFragment newInstance(String questionId) {
        CreateQuestionFragment fragment = new CreateQuestionFragment();
        Bundle args = new Bundle();
        args.putString("questionId", questionId);
        fragment.setArguments(args);
        return fragment;
    }

    //Empty constructor
    public CreateQuestionFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        mContext = getActivity();
        this.inflater = inflater;

        sharedPref = mContext.getSharedPreferences(
                getString(R.string.preference_file_key), Context.MODE_PRIVATE);

        mCurrentUserId = sharedPref.getString("user_id", "");

        rootView = inflater.inflate(R.layout.fragment_create_question, container,
                false);


        sendButton = (ImageView) rootView.findViewById(R.id.send_button);
        sendButton.setOnClickListener(this);

        addTagsButton = (ImageView) rootView.findViewById(R.id.add_tag_button);
        addTagsButton.setOnClickListener(this);

        locationToggle = (ImageView) rootView.findViewById(R.id.location_toggle);
        locationToggle.setOnClickListener(this);

        requestGroupText = (TextView) rootView.findViewById(R.id.study_group_text);
        requestGroupText.setOnClickListener(this);
        groupToggle = (ImageView) rootView.findViewById(R.id.study_group_toggle);
        groupToggle.setOnClickListener(this);
        groupToggle.setSelected(false);

        requestTutorText = (TextView) rootView.findViewById(R.id.tutor_text);
        requestTutorText.setOnClickListener(this);
        tutorToggle = (ImageView) rootView.findViewById(R.id.tutor_toggle);
        tutorToggle.setOnClickListener(this);
        tutorToggle.setSelected(false);

        userName = (TextView) rootView.findViewById(R.id.question_name_text);
        questionEditText = (EditText) rootView.findViewById(R.id.question_edit_text);
        topicEditText = (EditText) rootView.findViewById(R.id.topic_edit_text);
        userImage = (ImageView) rootView.findViewById(R.id.question_user_image);

        buildGoogleApiClient();

        tagList = new ArrayList<>();

        String name = sharedPref.getString("first_name", "") + " " + sharedPref.getString("last_name", "");
        userName.setText(name);
        getUserImage(sharedPref.getString("image", "image"), userImage);

        if (getArguments() != null) {
            mQuestionId = getArguments().getString("questionId");
            if(!mQuestionId.equalsIgnoreCase("")){
                getQuestionData();
                isInEditMode = true;
            }

        }

        return rootView;
    }


    /**
     * Toggle helper method
     */
    private void clickGroupButton(){
        if(!groupToggle.isSelected()) {
            groupToggle.setColorFilter(getResources().getColor(R.color.primary_dark));
            requestGroupText.setTextColor(getResources().getColor(R.color.primary_dark));
        }
        else{
            groupToggle.setColorFilter(getResources().getColor(R.color.hint_text));
            requestGroupText.setTextColor(getResources().getColor(R.color.hint_text));
        }
        groupToggle.setSelected(!groupToggle.isSelected());
    }

    /**
     * Toggle helper method
     */
    private void clickTutorButton(){
        if(!tutorToggle.isSelected()) {
            tutorToggle.setColorFilter(getResources().getColor(R.color.primary_dark));
            requestTutorText.setTextColor(getResources().getColor(R.color.primary_dark));
        }
        else{
            tutorToggle.setColorFilter(getResources().getColor(R.color.hint_text));
            requestTutorText.setTextColor(getResources().getColor(R.color.hint_text));
        }
        tutorToggle.setSelected(!tutorToggle.isSelected());

    }


    /**
     * Implemented Listener for all view onclick functionalities
     * @param v view
     */
    @Override
    public void onClick(View v) {

        if(v == sendButton){
            InputMethodManager imm = (InputMethodManager)mContext.getSystemService(
                    Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(questionEditText.getWindowToken(), 0);

            boolean error = false;

            if(!tutorToggle.isSelected() && !groupToggle.isSelected()) {
                Toast.makeText(getActivity(), "Please Request a Group and/or Tutor",
                        Toast.LENGTH_SHORT)
                        .show();
                error = true;
            }
            if(topicEditText.getText().toString().equalsIgnoreCase("")) {
                Toast.makeText(getActivity(), "Please write a topic!", Toast.LENGTH_SHORT).show();
                error = true;
            }

            if(topicEditText.getText().toString().length() > 20) {
                Toast.makeText(getActivity(), "Your topic is too long!", Toast.LENGTH_SHORT).show();
                error = true;
            }

            if(questionEditText.getText().toString().equalsIgnoreCase("")) {
                questionEditText.setError("You need to write a question!");
                error = true;
            }
            else{questionEditText.clearError();}
            if(mCurrentLocation == null && showLocation){
                Toast.makeText(mContext, "Cant find location!", Toast.LENGTH_SHORT).show();
                error = true;
            }

            if(!error){
                checkIfUserIsInGroup();
            }

        }
        else if(v == addTagsButton){

            TagDialogFragment newFragment = TagDialogFragment.newInstance(1, tagList);
            newFragment.setTargetFragment(CreateQuestionFragment.this, 1);
            newFragment.show(getActivity().getSupportFragmentManager(), "");

        }
        else if(v == locationToggle){
            if(showLocation){
                locationToggle.setImageDrawable(getResources().getDrawable(R.drawable.ic_location_off_grey600_36dp));
            }
            else{
                locationToggle.setImageDrawable(getResources().getDrawable(R.drawable.ic_location_on_grey600_36dp));
            }

            showLocation = !showLocation;
        }
        else if(v == groupToggle || v == requestGroupText){
            clickGroupButton();

        }
        else if(v == tutorToggle || v == requestTutorText){
            clickTutorButton();

        }
    }


    /**
     * Volley call to retrieve question info if this fragment is in edit mode
     */
    private void getQuestionData() {

        List<NameValuePair> params = new LinkedList<>();
        params.add(new BasicNameValuePair("questionId", mQuestionId));

        String paramString = URLEncodedUtils.format(params, "utf-8");
        String url = "http://54.200.33.91:8080/com.mysql.services/rest/serviceclass/viewQuestion?"+paramString;

        JsonObjectRequest jsObjRequest = new JsonObjectRequest(Request.Method.GET, url,
                (JSONObject)null,
                new Response.Listener<JSONObject>(){

                    @Override
                    public void onResponse(JSONObject response) {
                        try {

                            JSONObject result = new JSONObject(response.toString());
                            if(result.getString("success").equalsIgnoreCase("1")){

                                JSONArray questionAndTags = result.getJSONObject("result").getJSONArray("myArrayList");

                                mQuestionInfo = questionAndTags.getJSONObject(0).getJSONObject("map");
                                tagList = new ArrayList<>();
                                if(questionAndTags.length() > 1){
                                    for(int a = 1; a < questionAndTags.length(); a++){
                                        tagList.add(questionAndTags.getJSONObject(a).getJSONObject("map").getString("tag"));
                                    }
                                }

                                showQuestionDetails();
                            }
                            else{
                                //Error...
                            }

                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }, new Response.ErrorListener() {

            @Override
            public void onErrorResponse(VolleyError error) {
                System.out.println("Error with connection or url: " + error.toString());
            }

        });

        Singleton.getInstance().addToRequestQueue(jsObjRequest);
    }


    /**
     * Volley call to create question and send it to server
     */
    private void sendQuestionToServer(){

        double latitude = 0.0;
        double longitude = 0.0;

        if (mCurrentLocation == null) {
            if (getLastKnownLocation() != null) {
                mCurrentLocation = getLastKnownLocation();
                latitude = mCurrentLocation.getLatitude();
                longitude = mCurrentLocation.getLongitude();
            } else {
                if (showLocation && !isInEditMode) {
                    Toast.makeText(mContext, "Unable to send this post", Toast.LENGTH_SHORT).show();
                    return;
                } else {

                }
            }
        }
        else{
            latitude = mCurrentLocation.getLatitude();
            longitude = mCurrentLocation.getLongitude();
        }

        List<NameValuePair> params = new LinkedList<>();

        if(!isInEditMode){
            params.add(new BasicNameValuePair("userId", sharedPref.getString("user_id", "")));
            params.add(new BasicNameValuePair("latitude", latitude+""));
            params.add(new BasicNameValuePair("longitude", longitude + ""));
        }
        else
            params.add(new BasicNameValuePair("questionId", mQuestionId));

        params.add(new BasicNameValuePair("text", questionEditText.getText().toString()));

        for (int a = 0; a < tagList.size(); a++) {
            params.add(new BasicNameValuePair("tags", tagList.get(a)));
        }
        params.add(new BasicNameValuePair("tutor", (tutorToggle.isSelected() ? 1 : 0)+""));
        params.add(new BasicNameValuePair("studyGroup", (groupToggle.isSelected() ? 1 : 0)+""));
        params.add(new BasicNameValuePair("topic", topicEditText.getText().toString()));
        params.add(new BasicNameValuePair("visibleLocation", (showLocation ? 1 : 0)+""));

        String paramString = URLEncodedUtils.format(params, "utf-8")
                .replaceAll("%27", "%27%27");

        String url;
        if(isInEditMode)
            url = "http://54.200.33.91:8080/com.mysql.services/rest/serviceclass/editQuestion?"+paramString;
        else
            url = "http://54.200.33.91:8080/com.mysql.services/rest/serviceclass/createQuestion?"+paramString;

        JsonObjectRequest jsObjRequest = new JsonObjectRequest(Request.Method.GET, url,
                (JSONObject)null,
                new Response.Listener<JSONObject>(){

                    @Override
                    public void onResponse(JSONObject response) {
                        try {

                            JSONObject result = new JSONObject(response.toString());
                            if(!result.getString("success").equalsIgnoreCase("1")){

                                //Error...
                                if(result.getString("result").startsWith("Duplicate")){
                                    Toast.makeText(mContext, "You already have an active post", Toast.LENGTH_SHORT).show();
                                }
                                else{
                                    if(isInEditMode)
                                        Toast.makeText(mContext, "Error editing question", Toast.LENGTH_SHORT).show();
                                    else
                                        Toast.makeText(mContext, "Error creating question", Toast.LENGTH_SHORT).show();
                                }
                            }
                            else{
                                getActivity().setResult(getActivity().RESULT_OK);
                                getActivity().finish();
                            }

                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }, new Response.ErrorListener() {

            @Override
            public void onErrorResponse(VolleyError error) { }

        });

        Singleton.getInstance().addToRequestQueue(jsObjRequest);

    }

    protected void startLocationUpdates() {
        LocationServices.FusedLocationApi.requestLocationUpdates(
                mGoogleApiClient, mLocationRequest, this);
    }

    protected void createLocationRequest() {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(10000);
        mLocationRequest.setFastestInterval(5000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    /**
     * Necessary for location updates
     */
    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(mContext)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener( this)
                .addApi(LocationServices.API)
                .build();
        mGoogleApiClient.connect();
    }

    /**
     * Implemented Method for GoogleApiClient Callbacks
     */
    @Override
    public void onConnected(Bundle bundle) {
        mCurrentLocation = LocationServices.FusedLocationApi.getLastLocation(
                mGoogleApiClient);
        System.out.println("Connected to GoogleApi: "+mCurrentLocation);
        createLocationRequest();
        startLocationUpdates();
    }

    /**
     * Implemented Method for GoogleApiClient Callbacks
     */
    @Override
    public void onConnectionSuspended(int i) {

    }

    /**
     * Implemented Method for GoogleApiClient Callbacks
     */
    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

    }

    /**
     * Implemented Method for LocationListener Callbacks
     */
    @Override
    public void onLocationChanged(Location location) {
        mCurrentLocation = location;
        System.out.println("Location: "+location);
        //Stop updates after we get a location....
        stopLocationUpdates();
    }

    protected void stopLocationUpdates() {
        LocationServices.FusedLocationApi.removeLocationUpdates(
                mGoogleApiClient, this);
    }

    /**
     * LRU query
     * @param imageUrl String url
     * @param imageView View to show retrieved image
     */
    private void getUserImage(String imageUrl, final ImageView imageView){

        ImageLoader imageLoader = Singleton.getInstance().getImageLoader();
        imageLoader.get(imageUrl, new ImageLoader.ImageListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
            }

            @Override
            public void onResponse(ImageLoader.ImageContainer response, boolean arg1) {
                if (response.getBitmap() != null) {

                    imageView.setImageBitmap(response.getBitmap());
                } else {
                    // Default image...
                    imageView.setImageDrawable(getResources().getDrawable(R.drawable.defaultprofile));
                }
            }
        });

    }


    private Location getLastKnownLocation() {
        LocationManager locationManager = (LocationManager) mContext.getSystemService(Context.LOCATION_SERVICE);
        Criteria criteria = new Criteria();
        Location location = locationManager.getLastKnownLocation(locationManager.getBestProvider(criteria, false));
        if (location != null) {
            return location;
        }

        return null;
    }


    /**
     * Passes taglist data structure between TagDialog and CreateFragment
     * @param activeFilters Set<String>
     */
    public void passTagList(Set<String> activeFilters) {
        if(activeFilters.size() > 0 ){
            tagList.clear();
            tagList.addAll(activeFilters);
            addTagsButton.setColorFilter(getResources().getColor(R.color.primary_dark));
        }
        else{
            tagList.clear();
            addTagsButton.setColorFilter(getResources().getColor(R.color.hint_text));
        }

    }


    /**
     * Update UI after retrieving Question Data
     */
    private void showQuestionDetails(){


        try {
            boolean studyBool = mQuestionInfo.getBoolean("study_group");
            boolean tutorBool = mQuestionInfo.getBoolean("tutor");

            if(tutorBool)
                clickTutorButton();

            if(studyBool)
                clickGroupButton();

            questionEditText.setText(mQuestionInfo.getString("text"));
            topicEditText.setText(mQuestionInfo.getString("topic"));

            if(tagList.size() > 0){
                addTagsButton.setColorFilter(getResources().getColor(R.color.primary_dark));
            }

            int isVisible = mQuestionInfo.getInt("visible_location");
            if(isVisible == 0)
                locationToggle.setImageResource(R.drawable.ic_location_off_grey600_36dp);
            else
                locationToggle.setImageResource(R.drawable.ic_location_on_grey600_36dp);

        } catch (JSONException e) {
            e.printStackTrace();
        }

    }

    /**
     * Disables being able to create questions if user is already in a group
     */
    private void checkIfUserIsInGroup() {
        String isJoinerInGroup = mInGroupUrl + mCurrentUserId;
        JsonObjectRequest inGroupRequest = new JsonObjectRequest(Request.Method.GET, isJoinerInGroup,
                (JSONObject) null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                try {
                    JSONObject result = new JSONObject(response.toString());
                    if(result.getInt("expectResults") != 0 && !isInEditMode) {
                        Toast.makeText(getActivity(), "You are already in a group!", Toast.LENGTH_LONG)
                                .show();
                        getActivity().finishActivity(getActivity().RESULT_CANCELED);
                    }
                    else {
                        sendQuestionToServer();
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {

            }
        });
        Singleton.getInstance().addToRequestQueue(inGroupRequest);
    }


}
