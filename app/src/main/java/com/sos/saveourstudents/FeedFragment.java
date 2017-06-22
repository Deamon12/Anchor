package com.sos.saveourstudents;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.CardView;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
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

import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * Fragment displays local question data in a recyclerView (list) layout.
 */
public class FeedFragment extends Fragment implements LocationListener, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    private final int PROFILE_ACTIVITY = 505;
    private final int VIEWQUESTION_ACTIVITY = 37;
    private SharedPreferences sharedPref;
    LocationRequest mLocationRequest;
    Location mCurrentLocation;
    Location lastKnownLocation;
    private GoogleApiClient mGoogleApiClient;

    private RecycleViewAdapter mAdapter;
    private View rootView;
    RecyclerView mRecyclerView;
    private SwipeRefreshLayout mSwipeRefreshLayout;
    Context mContext;

    private JSONArray mQuestionList;


    public static FeedFragment newInstance() {
        FeedFragment fragment = new FeedFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    // Required empty public constructor
    public FeedFragment() {
    }

    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        if (savedInstanceState != null) {
            //Restore the fragment's state here
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        //Save the fragment's state here

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        setRetainInstance(true);
        // Inflate the layout for this fragment
        rootView = inflater.inflate(R.layout.fragment_feed_layout, container,
                false);


        mContext = getActivity();

        if (!Singleton.hasBeenInitialized()) {
            Singleton.initialize(mContext);
        }

        initializeRecyclerView();
        sharedPref = mContext.getSharedPreferences(
                getString(R.string.preference_file_key), Context.MODE_PRIVATE);

        //Initial Swipe to refresh workaround
        TypedValue typed_value = new TypedValue();
        getActivity().getTheme().resolveAttribute(android.support.v7.appcompat.R.attr.actionBarSize, typed_value, true);
        mSwipeRefreshLayout.setProgressViewOffset(false, 0, getResources().getDimensionPixelSize(typed_value.resourceId));
        mSwipeRefreshLayout.setRefreshing(true);
        buildGoogleApiClient();

        return rootView;
    }

    /**
     * Instantiate and prepare RecyclerView helper method
     */
    private void initializeRecyclerView() {
        mRecyclerView = (RecyclerView) rootView.findViewById(R.id.my_recycler_view);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(mContext));
        mRecyclerView.setItemAnimator(new DefaultItemAnimator());
        mSwipeRefreshLayout = (SwipeRefreshLayout) rootView.findViewById(R.id.swipe_refresh_layout);
        mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                buildGoogleApiClient(); //Get current location and update UI
            }
        });

        mSwipeRefreshLayout.setColorSchemeColors(
                getResources().getColor(R.color.primary),
                getResources().getColor(R.color.accent),
                getResources().getColor(R.color.secondary_text),
                getResources().getColor(R.color.primary_dark));

        mRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                //System.out.println("scroll state: "+newState);
                super.onScrollStateChanged(recyclerView, newState);
            }

            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                //System.out.println("scroll dx: " + dx);
                //System.out.println("scroll dy: " + dy);
                //TODO implement FAB/toolbar hide on scroll
            }
        });

    }


    @Override
    public void onPause() {
        stopLocationUpdates();
        super.onPause();
    }

    @Override
    public void onDestroy() {
        stopLocationUpdates();
        super.onDestroy();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == VIEWQUESTION_ACTIVITY) {
            //((MainActivity) getActivity()).updateFragments();
            //((MainActivity) getActivity()).buildFab();
        }
    }


    public void getQuestionData() {

        LocationManager locationManager = (LocationManager) mContext.getSystemService(Context.LOCATION_SERVICE);
        Criteria criteria = new Criteria();
        lastKnownLocation = locationManager.getLastKnownLocation(locationManager.getBestProvider(criteria, false));

        Set<String> filterList = new HashSet<String>(sharedPref.getStringSet("filter_list", new HashSet<String>()));

        List<String> myList = new ArrayList<String>();
        myList.addAll(filterList);


        double latitude = 32.88006;
        double longitude = -117.2340133;

        if (mCurrentLocation != null) {
            latitude = mCurrentLocation.getLatitude();
            longitude = mCurrentLocation.getLongitude();
        } else if (lastKnownLocation != null) {
            latitude = lastKnownLocation.getLatitude();
            longitude = lastKnownLocation.getLongitude();
        } else {
            System.out.println("Could not get location, using UCSD as default");
            Toast.makeText(mContext, "Could not get location", Toast.LENGTH_SHORT).show();
        }


        List<NameValuePair> params = new LinkedList<NameValuePair>();
        params.add(new BasicNameValuePair("latitude", latitude + ""));
        params.add(new BasicNameValuePair("longitude", longitude + ""));
        for (int a = 0; a < myList.size(); a++) {
            params.add(new BasicNameValuePair("tags", myList.get(a)));
        }
        params.add(new BasicNameValuePair("limit", sharedPref.getInt("distance", 10) + ""));


        String paramString = URLEncodedUtils.format(params, "utf-8").replace("+", "%20");
        String url = "http://54.200.33.91:8080/com.mysql.services/rest/serviceclass/getQuestions?" + paramString;

        //System.out.println("feed get question URL: "+url);

        JsonObjectRequest jsObjRequest = new JsonObjectRequest(Request.Method.GET,
                url,
                (JSONObject) null,
                new Response.Listener<JSONObject>() {

                    @Override
                    public void onResponse(JSONObject response) {
                        try {

                            JSONObject theResponse = new JSONObject(response.toString());


                            if (!theResponse.getString("success").equalsIgnoreCase("1")) {
                                showConnectionIssueDialog();
                                return;
                            }

                            /**
                             * Correct response from server
                             */
                            if (theResponse.getString("expectResults").equalsIgnoreCase("0")) {
                                //No results to show (empty array returned)
                                JSONArray tempList = theResponse.getJSONObject("result").getJSONArray("myArrayList");
                                System.out.println(mQuestionList+" =?= "+tempList);

                                if (mQuestionList != null && tempList.equals(mQuestionList)) {
                                    //Dont change UI
                                } else {
                                    //new data has been recieved...TODO: alert user

                                    mQuestionList = theResponse.getJSONObject("result").getJSONArray("myArrayList");
                                    showQuestions();
                                }

                            } else {
                                /**
                                 * If successfully recieved message list, check our currently showing list.
                                 * If the newly recieved list is the same as the currently shown list, dont update UI.
                                 * Else, TODO: alert user user to update UI
                                 */
                                JSONArray tempList = theResponse.getJSONObject("result").getJSONArray("myArrayList");
                                System.out.println(mQuestionList+" =?= "+tempList);
                                if (mQuestionList != null && tempList.equals(mQuestionList)) {
                                    //Dont change UI
                                } else {
                                    //new data has been recieved...TODO: alert user
                                    mQuestionList = theResponse.getJSONObject("result").getJSONArray("myArrayList");
                                    showQuestions();
                                }


                            }

                            //mSwipeRefreshLayout.setRefreshing(false);


                        } catch (JSONException e) {
                            e.printStackTrace();
                        }


                    }
                }, new Response.ErrorListener() {

            @Override
            public void onErrorResponse(VolleyError error) {
                showConnectionIssueDialog();

            }
        });

        // Access the RequestQueue through singleton class.
        Singleton.getInstance().addToRequestQueue(jsObjRequest);

    }

    /**
     * Show retrieved question data to UI
     */
    private void showQuestions(){

        mSwipeRefreshLayout.setRefreshing(false);
        mAdapter = new RecycleViewAdapter(R.layout.feed_item_layout);
        mRecyclerView.setAdapter(mAdapter);
        stopLocationUpdates();
    }


    @Override
    public void onLocationChanged(Location location) {
        mCurrentLocation = location;
        getQuestionData();
        stopLocationUpdates();
    }

    @Override
    public void onConnected(Bundle bundle) {
        mCurrentLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        getLocationUpdate();
    }

    @Override
    public void onConnectionSuspended(int i) {
        showConnectionIssueDialog();
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        showConnectionIssueDialog();
    }

    private void showConnectionIssueDialog() {
        mSwipeRefreshLayout.setRefreshing(false);
        ((MainActivity) getActivity()).showSnackbar();
    }



    public void getLocationUpdate() {
        mSwipeRefreshLayout.setRefreshing(true);
        createLocationRequest();

    }

    protected void startLocationUpdates() {
        if (mGoogleApiClient.isConnected())
            LocationServices.FusedLocationApi.requestLocationUpdates(
                    mGoogleApiClient, mLocationRequest, this);
    }

    protected void stopLocationUpdates() {
        if (mGoogleApiClient.isConnected())
            LocationServices.FusedLocationApi.removeLocationUpdates(
                    mGoogleApiClient, this);
    }

    protected void createLocationRequest() {

        //System.out.println("Creating new locationRequest in feed");
        //if(mLocationRequest == null) {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(10000);
        mLocationRequest.setFastestInterval(5000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        startLocationUpdates();
        //}

    }


    protected synchronized void buildGoogleApiClient() {

        mSwipeRefreshLayout.setRefreshing(true);
        mGoogleApiClient = new GoogleApiClient.Builder(mContext)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();

        mGoogleApiClient.connect();
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



    /**
     * Feed Item recycler view. Handles UI events related to recycler layout
     */
    public class RecycleViewAdapter extends RecyclerView.Adapter<RecycleViewAdapter.ViewHolder>{

        private int rowLayout;
        public RecycleViewAdapter(int rowLayout) {
            this.rowLayout = rowLayout;
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
            View v = LayoutInflater.from(viewGroup.getContext()).inflate(rowLayout, viewGroup, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(ViewHolder viewHolder, int position) {

            try {
                String firstName = mQuestionList.getJSONObject(position).getJSONObject("map").getString("first_name");
                String lastName = mQuestionList.getJSONObject(position).getJSONObject("map").getString("last_name");
                String theDate = mQuestionList.getJSONObject(position).getJSONObject("map").getString("date");
                String topic = mQuestionList.getJSONObject(position).getJSONObject("map").getString("topic");
                String text = mQuestionList.getJSONObject(position).getJSONObject("map").getString("text");

                boolean group = mQuestionList.getJSONObject(position).getJSONObject("map").getBoolean("study_group");
                boolean tutor = mQuestionList.getJSONObject(position).getJSONObject("map").getBoolean("tutor");

                double latitude = Double.parseDouble(mQuestionList.getJSONObject(position).getJSONObject("map").getString("latitude"));
                double longitude = Double.parseDouble(mQuestionList.getJSONObject(position).getJSONObject("map").getString("longitude"));

                if (group) {
                    viewHolder.groupIcon.setColorFilter(getResources().getColor(R.color.primary));
                } else {
                    viewHolder.groupIcon.setColorFilter(getResources().getColor(R.color.hint_text_on_background));
                }

                if (tutor) {
                    viewHolder.tutorIcon.setColorFilter(getResources().getColor(R.color.primary));
                } else {
                    viewHolder.tutorIcon.setColorFilter(getResources().getColor(R.color.hint_text_on_background));
                }

                String userImageUrl = "";
                if (mQuestionList.getJSONObject(position).getJSONObject("map").has("image")) {
                    userImageUrl = mQuestionList.getJSONObject(position).getJSONObject("map").getString("image");
                }


                /**
                 * Send user image URL to volley if user has a URL
                 * otherwise, set user imageview to default image
                 */
                if (!userImageUrl.equalsIgnoreCase("")) {
                    getUserImage(userImageUrl, viewHolder.userImage);
                } else {
                    viewHolder.userImage.setImageDrawable(getResources().getDrawable(R.drawable.defaultprofile));
                }

                if (firstName.length() > 14) {
                    firstName = firstName.substring(0, 13).concat("...");
                }

                viewHolder.nameText.setText(firstName + " " + lastName.charAt(0) + '.');
                viewHolder.questionText.setText(text);

                viewHolder.dateText.setText(Singleton.getInstance().doDateLogic(theDate));
                viewHolder.topicText.setText(topic);

                String distanceType = sharedPref.getString("distanceType", "MI");


                if (mCurrentLocation != null) {
                  
                    viewHolder.distanceText.setText(
                            Singleton.getInstance().doDistanceLogic(mCurrentLocation.getLatitude(), mCurrentLocation.getLongitude(),
                                    latitude, longitude, distanceType) + "" + distanceType);
                } else if (lastKnownLocation != null) {
                    System.out.println("mCurrentLocation is null, trying lastknown");
                    viewHolder.distanceText.setText(
                            Singleton.getInstance().doDistanceLogic(lastKnownLocation.getLatitude(), lastKnownLocation.getLongitude(),
                                    latitude, longitude, distanceType) + "" + distanceType);
                } else {
                    viewHolder.distanceText.setVisibility(View.INVISIBLE);
                    System.out.println("mCurrentLocation and lastknown is null");
                }

            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        @Override
        public int getItemCount() {
            return mQuestionList.length();
        }

        public class ViewHolder extends RecyclerView.ViewHolder implements View.OnTouchListener, View.OnClickListener {

            public ImageView groupIcon;
            public ImageView tutorIcon;
            public ImageView userImage;
            public TextView questionText;
            public TextView nameText;
            public TextView dateText;
            public TextView topicText;
            public TextView distanceText;
            private CardView cardView;

            //Declare views here, dont fill them
            public ViewHolder(View itemView) {
                super(itemView);
                questionText = (TextView) itemView.findViewById(R.id.question_text);
                nameText = (TextView) itemView.findViewById(R.id.question_name_text);
                dateText = (TextView) itemView.findViewById(R.id.question_timestamp);
                topicText = (TextView) itemView.findViewById(R.id.topic_text);
                distanceText = (TextView) itemView.findViewById(R.id.question_distance);
                userImage = (ImageView) itemView.findViewById(R.id.question_image);

                groupIcon = (ImageView) itemView.findViewById(R.id.group_icon);
                tutorIcon = (ImageView) itemView.findViewById(R.id.tutor_icon);
                cardView = (CardView) itemView.findViewById(R.id.card_view);
                cardView.setOnTouchListener(this);
                userImage.setOnClickListener(this);
                nameText.setOnClickListener(this);

            }

            @Override
            public boolean onTouch(View v, MotionEvent event) {

                if (v == cardView && event.getAction() == MotionEvent.ACTION_UP) {

                    try {
                        String questionId = mQuestionList.getJSONObject(getAdapterPosition()).getJSONObject("map").getString("question_id");
                        Intent mIntent = new Intent(mContext, ViewQuestionActivity.class);
                        mIntent.putExtra("questionId", questionId);
                        startActivityForResult(mIntent, VIEWQUESTION_ACTIVITY);

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
                return false;
            }

            @Override
            public void onClick(View v) {

                if (v == nameText || v == userImage) {

                    try {
                        String userId = mQuestionList.getJSONObject(getAdapterPosition()).getJSONObject("map").getString("user_id");
                        Intent intent = new Intent(mContext, ProfileActivity.class);
                        intent.putExtra("userId", userId);
                        startActivityForResult(intent, PROFILE_ACTIVITY);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }


}