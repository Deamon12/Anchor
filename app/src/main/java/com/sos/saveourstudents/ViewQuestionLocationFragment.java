package com.sos.saveourstudents;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.location.Location;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.JsonObjectRequest;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.LinkedList;
import java.util.List;


public class ViewQuestionLocationFragment extends android.support.v4.app.Fragment implements
        LocationListener,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, GoogleMap.OnMarkerDragListener, View.OnClickListener {

    // Argument tags for the bundle placed on a new instance of the fragment
    private static final String QUESTION_LOCATION = "paramCoordinates";
    private static final String USER_IMAGE = "userImage";

    // The inflater for the map's layout
    private LayoutInflater mInflater;

    // Variables necessary for communication with the GoogleApi
    private GoogleMap mMap;
    private MapView mMapView;
    private Context mContext;
    private GoogleApiClient mGoogleApiClient;
    private ImageView visibilityToggle;

    // Information specific to the question and/or viewer
    private Location mCurrentLocation;
    private Location newLocation;
    private String userImageUrl;
    private boolean mEditable;
    private boolean mIsVisible;
    private String mQuestionId;
    private boolean mIsMemberOfGroup;

    /**
     * Creates a new instance of the ViewQuestionLocationFragment
     * @param questionId The questionId of the current group
     * @param location The location that the group leader has set
     * @param userImageUrl The URL of the group leader's profile image
     * @param isEditable Whether the current viewer is the leader of the group
     * @param isVisible Whether the group leader has decided to make the location visible
     * @param isMember Whether the current viewer is a member of the group
     * @return A new instance of the ViewQuestionLocationFragment
     */
    public static ViewQuestionLocationFragment newInstance(String questionId,
                                                           Location location, String userImageUrl,
                                                           boolean isEditable, boolean isVisible,
                                                           boolean isMember) {
        ViewQuestionLocationFragment fragment = new ViewQuestionLocationFragment();
        Bundle args = new Bundle();
        args.putParcelable(QUESTION_LOCATION, location);
        args.putString(USER_IMAGE, userImageUrl);
        args.putBoolean("isEditable", isEditable);
        args.putString("questionId", questionId);
        args.putBoolean("isVisible", isVisible);
        args.putBoolean("isMember", isMember);
        fragment.setArguments(args);
        return fragment;
    }

    /**
     * Empty constructor
     */
    public ViewQuestionLocationFragment() {
        // Required empty public constructor
    }

    /**
     * Grabs user's shared preferences values
     * @param savedInstanceState Unused, necessary for overriding the method
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mCurrentLocation = getArguments().getParcelable(QUESTION_LOCATION);
            userImageUrl = getArguments().getString(USER_IMAGE);
            mEditable = getArguments().getBoolean("isEditable");
            mQuestionId = getArguments().getString("questionId");
            mIsVisible = getArguments().getBoolean("isVisible");
            mIsMemberOfGroup = getArguments().getBoolean("isMember");
        }
    }

    /**
     * Begins assigning ViewGroups to member variables after inflating views
     * @param inflater The LayoutInflater of the current context
     * @param container The container for all views in question
     * @param savedInstanceState Unused, necessary for overriding the method
     * @return The view of the activity
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mContext = getActivity();
        mInflater = inflater;
        View rootView = inflater.inflate(R.layout.fragment_view_group_location, container, false);
        mMapView = (MapView) rootView.findViewById(R.id.view_group_location_map);

        visibilityToggle = (ImageView) rootView.findViewById(R.id.visibility_toggle);

        if(mEditable)
            visibilityToggle.setOnClickListener(this);

        if(mIsVisible)
            visibilityToggle.setImageResource(R.drawable.ic_remove_red_eye_white_24dp);


        initializeMap();


        return rootView;
    }

    /**
     * Controls visibility toggling for the ImageView of the visibility icon
     */
    private void clickVisibilityToggle() {
        if(visibilityToggle.isSelected()){
            Toast.makeText(mContext, "Your location is now private", Toast.LENGTH_SHORT).show();
            visibilityToggle.setImageResource(R.drawable.ic_visibility_off_white_24dp);
        }
        else{
            Toast.makeText(mContext, "Your location is now public", Toast.LENGTH_SHORT).show();
            visibilityToggle.setImageResource(R.drawable.ic_remove_red_eye_white_24dp);
        }
    }

    /**
     * Initializes the MapView and shows the marker of the group owner's set location
     */
    private void initializeMap(){
        mMapView.onCreate(new Bundle());
        mMap = mMapView.getMap();

        if(mEditable)
            mMap.setOnMarkerDragListener(this);

        mMap.setMyLocationEnabled(true);
        mMap.getUiSettings().setIndoorLevelPickerEnabled(true);
        try {
            MapsInitializer.initialize(mContext);
        } catch (Exception e) {
            e.printStackTrace();
        }

        buildGoogleApiClient();
        mMapView.onResume();


        if(mIsVisible){
            showCustomMarker();
        }
        else if(mIsMemberOfGroup){
            showCustomMarker();
        }
    }

    /**
     * Builds the GoogleApiClient to be able to use the maps API
     */
    private synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(mContext)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        mGoogleApiClient.connect();
    }

    /**
     * Implemented method from GoogleApiClient
     */
    @Override
    public void onConnected(Bundle bundle) {
    }

    /**
     * Implemented method from GoogleApiClient
     */
    @Override
    public void onConnectionSuspended(int i) {

    }

    /**
     * Implemented method from GoogleApiClient
     */
    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

    }


    @Override
    public void onLocationChanged(Location location) {
        stopLocationUpdates();
    }

    /**
     * Stops updating the location of the MapView
     */
    private void stopLocationUpdates() {
        LocationServices.FusedLocationApi.removeLocationUpdates(
                mGoogleApiClient, this);
    }

    /**
     * On resume, resume the MapView
     */
    @Override
    public void onResume() {
        if(mMap != null && mMapView != null)
            mMapView.onResume();
        super.onResume();

    }

    /**
     * On pause, stop updating the map's location to preserve battery
     */
    @Override
    public void onPause() {
        stopLocationUpdates();
        mMapView.onPause();
        super.onPause();
    }

    /**
     * On destroy, stop updating the map's location to preserve battery
     */
    @Override
    public void onDestroy() {
        stopLocationUpdates();
        mMapView.onDestroy();
        super.onDestroy();
    }

    /**
     * Open dialog once marker has been set
     * @param marker The marker of the group's location
     */
    @Override
    public void onMarkerDragEnd(Marker marker) {
        newLocation = new Location("new");
        newLocation.setLatitude(marker.getPosition().latitude);
        newLocation.setLongitude(marker.getPosition().longitude);
        showChangeLocationDialog();

    }

    /**
     * Builds the dialog for asking the user whether they would like to set a new
     * location or not
     */
    private void showChangeLocationDialog(){

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setCancelable(false);
        builder.setMessage("Would you like to set this location?");

        builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                mCurrentLocation = newLocation;
                mMap.clear();
                showCustomMarker();
                editQuestionLocation(mCurrentLocation);

            }
        });
        builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                mMap.clear();
                showCustomMarker();

            }
        });

        AlertDialog dialog = builder.create();
        dialog.show();

    }

    /**
     * Method for displaying the custom marker of the group owner and his set location
     */
    private void showCustomMarker(){

        View markerLayout = mInflater.inflate(R.layout.custom_map_marker, null, false);
        ImageView userImage = (ImageView) markerLayout.findViewById(R.id.question_image);
        if(userImageUrl != null && !userImageUrl.equalsIgnoreCase("")){
            setMarkerImage(userImageUrl, userImage, new LatLng(mCurrentLocation.getLatitude(), mCurrentLocation.getLongitude()));

        } else {
            MarkerOptions markerOptions = new MarkerOptions()

                    .position(new LatLng(mCurrentLocation.getLatitude(), mCurrentLocation.getLongitude()))
                    .icon(BitmapDescriptorFactory.fromBitmap(createDrawableFromView(mContext, markerLayout)));


            if(mEditable)
                markerOptions.draggable(true);

            CameraPosition cameraPosition = new CameraPosition.Builder()
                    .target(new LatLng(mCurrentLocation.getLatitude(), mCurrentLocation.getLongitude()))      // Sets the center of the map to location user
                    .zoom(16)                   // Sets the zoom
                    .bearing(0)                // Sets the orientation of the camera to east
                    .tilt(40)                   // Sets the tilt of the camera to 30 degrees
                    .build();                   // Creates a CameraPosition from the builder
            mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
            mMap.addMarker(markerOptions);

        }

    }

    /**
     * Creates a Bitmap given a View object
     * @param context The context of the given view
     * @param view The view to be converted
     * @return The created bitmap
     */
    private static Bitmap createDrawableFromView(Context context, View view) {
        DisplayMetrics displayMetrics = new DisplayMetrics();
        ((Activity) context).getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        view.setLayoutParams(new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT));
        view.measure(displayMetrics.widthPixels, displayMetrics.heightPixels);
        view.layout(0, 0, displayMetrics.widthPixels, displayMetrics.heightPixels);
        view.buildDrawingCache();
        Bitmap bitmap = Bitmap.createBitmap(view.getMeasuredWidth(), view.getMeasuredHeight(), Bitmap.Config.ARGB_8888);

        Canvas canvas = new Canvas(bitmap);
        view.draw(canvas);

        return bitmap;
    }

    /**
     * Displays the group leader's profile image on the map marker
     * @param imageUrl The URL of the image
     * @param imageView The ImageView to be populated
     * @param location The location of the marker
     */
    private void setMarkerImage(String imageUrl, final ImageView imageView, final LatLng location){

        ImageLoader imageLoader = Singleton.getInstance().getImageLoader();
        imageLoader.get(imageUrl, new ImageLoader.ImageListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
            }

            @Override
            public void onResponse(ImageLoader.ImageContainer response, boolean arg1) {
                if (response.getBitmap() != null) {
                    imageView.setImageBitmap(response.getBitmap());

                }

                MarkerOptions markerOptions = new MarkerOptions()
                        .position(location)
                        .icon(BitmapDescriptorFactory.fromBitmap(createDrawableFromView(mContext, (View) imageView.getParent())));
                if (mEditable)
                    markerOptions.draggable(true);
                mMap.addMarker(markerOptions);


                CameraPosition cameraPosition = new CameraPosition.Builder()
                        .target(new LatLng(location.latitude, location.longitude))
                        .zoom(16)
                        .bearing(0)
                        .tilt(40)
                        .build();
                mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));


            }
        });

    }

    /**
     * Request to the server for updating the question's new location
     * @param newLocation The new location w/ latitude and longitude
     */
    private void editQuestionLocation(Location newLocation){

        List<NameValuePair> params = new LinkedList<>();
        params.add(new BasicNameValuePair("questionId", mQuestionId));
        params.add(new BasicNameValuePair("latitude", newLocation.getLatitude() + ""));
        params.add(new BasicNameValuePair("longitude", newLocation.getLongitude() + ""));

        String paramString = URLEncodedUtils.format(params, "utf-8");
        String url = "http://54.200.33.91:8080/com.mysql.services/rest/serviceclass/editLocation?"+paramString;

        JsonObjectRequest jsObjRequest = new JsonObjectRequest(Request.Method.GET, url,
                (JSONObject)null,
                new Response.Listener<JSONObject>(){

                    @Override
                    public void onResponse(JSONObject response) {
                        try {

                            JSONObject result = new JSONObject(response.toString());
                            if(result.getString("success").equalsIgnoreCase("1")){
                                Toast.makeText(mContext, "Location Updated!", Toast.LENGTH_SHORT)
                                        .show();
                            }
                            else{
                                Toast.makeText(mContext, "Error Updating Location", Toast.LENGTH_SHORT)
                                        .show();
                            }

                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                    }
                }, new Response.ErrorListener() {

            @Override
            public void onErrorResponse(VolleyError error) {
                if(getActivity() != null)
                    ((ViewQuestionActivity) getActivity()).mSnackBar.show();
            }

        });

        Singleton.getInstance().addToRequestQueue(jsObjRequest);

    }

    /**
     * Sets up OnClickListeners for all views that have said functionality
     * @param v The view in question
     */
    @Override
    public void onClick(View v) {
        if(v == visibilityToggle){
            clickVisibilityToggle();
        }
    }

    // Necessary for interface, no implementation needed for this Fragment
    @Override
    public void onMarkerDragStart(Marker marker) {
    }

    @Override
    public void onMarkerDrag(Marker marker) {
    }
}
