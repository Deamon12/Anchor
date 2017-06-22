package com.sos.saveourstudents;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
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
import com.google.android.gms.location.LocationServices;
import com.rey.material.widget.EditText;
import com.rey.material.widget.FloatingActionButton;

import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * Fragment for first pane of ViewQuestionActivity
 */
public class ViewQuestionFragment extends Fragment implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, View.OnClickListener, LocationListener {

    private final int EDIT_QUESTION = 2345;

    private GoogleApiClient mGoogleApiClient;
    private Location mCurrentLocation;
    private SharedPreferences sharedPref;

    private RecycleViewAdapter mAdapter;
    private RecyclerView mRecyclerView;
    private LinearLayout taglist;
    private FloatingActionButton fab;
    private ImageView sendButton;
    private EditText commentEditText;
    private TextView userName;
    private TextView questionDate;
    private TextView questionTopic;
    private TextView questionDistance;
    private TextView questionText;
    private ImageView userImage;
    private ImageView tutorIcon;
    private ImageView groupIcon;

    private boolean mStudyGroupBool;
    private boolean mTutorBool;


    public Context mContext;
    private String mQuestionId;

    private JSONObject mQuestionInfo;
    private ArrayList tags;

    /**
     * Creates a new instance of the ViewQuestionFragment
     * @param questionId The questionId of the current group
     * @param isEditable Whether the group is being viewed by the owner
     * @return
     */
    public static ViewQuestionFragment newInstance(String questionId, boolean isEditable) {
        ViewQuestionFragment fragment = new ViewQuestionFragment();
        Bundle args = new Bundle();
        args.putString("questionId", questionId);
        args.putBoolean("isEditable", isEditable);
        fragment.setArguments(args);
        return fragment;
    }

    /**
     * Empty constructor
     */
    public ViewQuestionFragment() {
        // Required empty public constructor
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
        View rootView = inflater.inflate(R.layout.fragment_view_question, container, false);

        if (getArguments() != null) {
            mQuestionId = getArguments().getString("questionId");
            boolean mEditable = getArguments().getBoolean("isEditable");
        }else{
            Toast.makeText(mContext, "QuestionId empty in viewQuestiomFrag" , Toast.LENGTH_SHORT).show();
        }

        sharedPref = mContext.getSharedPreferences(
                getString(R.string.preference_file_key), Context.MODE_PRIVATE);

        buildGoogleApiClient();

        userImage = (ImageView) rootView.findViewById(R.id.question_image);



        userName = (TextView) rootView.findViewById(R.id.question_name_text);
        questionText = (TextView) rootView.findViewById(R.id.question_text);
        questionDate = (TextView) rootView.findViewById(R.id.question_timestamp);
        questionDistance = (TextView) rootView.findViewById(R.id.question_distance);
        questionTopic = (TextView) rootView.findViewById(R.id.question_topic_text);
        tutorIcon = (ImageView) rootView.findViewById(R.id.tutor_icon);
        groupIcon = (ImageView) rootView.findViewById(R.id.group_icon);

        taglist = (LinearLayout) rootView.findViewById(R.id.tag_list_layout);

        mRecyclerView = (RecyclerView) rootView.findViewById(R.id.my_recycler_view);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(mContext));
        mRecyclerView.setItemAnimator(new DefaultItemAnimator());

        fab = (FloatingActionButton) rootView.findViewById(R.id.group_action);
        fab.setVisibility(View.INVISIBLE);

        sendButton = (ImageView) rootView.findViewById(R.id.send_button);
        sendButton.setOnClickListener(this);

        commentEditText = (EditText) rootView.findViewById(R.id.comment_edittext);

        getCommentsData();

        if(((ViewQuestionActivity) getActivity()).mQuestionInfo == null) {
            getQuestionData();
        }
        else{
            mQuestionInfo = ((ViewQuestionActivity) getActivity()).mQuestionInfo;
            tags = ((ViewQuestionActivity) getActivity()).tags;
            try {
                showQuestionDetails(mQuestionInfo);
                showQuestionTags(tags);
                buildFab();

            } catch (JSONException e) {
                e.printStackTrace();
            }
            userImage.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    try {
                        String ownerId = mQuestionInfo.getString("user_id");
                        Intent intent = new Intent(mContext, ProfileActivity.class);
                        intent.putExtra("userId", ownerId);
                        startActivity(intent);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            });
        }

        return rootView;
    }

    /**
     * Retrieves the question data from the database
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
                                tags = new ArrayList<>();
                                if(questionAndTags.length() > 1){
                                    for(int a = 1; a < questionAndTags.length(); a++){
                                       tags.add(questionAndTags.getJSONObject(a).getJSONObject("map").getString("tag"));
                                    }

                                    showQuestionTags(tags);
                                }

                                userImage.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                        try {
                                            String ownerId = mQuestionInfo.getString("user_id");
                                            Intent intent = new Intent(mContext, ProfileActivity.class);
                                            intent.putExtra("userId", ownerId);
                                            startActivity(intent);
                                        } catch (JSONException e) {
                                            e.printStackTrace();
                                        }
                                    }
                                });

                                showQuestionDetails(mQuestionInfo);

                                buildFab();


                            }
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


        Singleton.getInstance().addToRequestQueue(jsObjRequest);

    }

    /**
     * Retrieves the comments data from the server
     */
    private void getCommentsData() {

        List<NameValuePair> params = new LinkedList<>();
        params.add(new BasicNameValuePair("questionId", mQuestionId));

        String paramString = URLEncodedUtils.format(params, "utf-8");
        String url = "http://54.200.33.91:8080/com.mysql.services/rest/serviceclass/getComments?"+paramString;


        JsonObjectRequest jsObjRequest = new JsonObjectRequest(Request.Method.GET, url,
                (JSONObject)null,
                new Response.Listener<JSONObject>(){

                    @Override
                    public void onResponse(JSONObject response) {
                        try {

                            JSONObject result = new JSONObject(response.toString());
                            if(result.getString("success").equalsIgnoreCase("1")){

                                JSONArray commentList = result.getJSONObject("result").getJSONArray("myArrayList");

                                mAdapter = new RecycleViewAdapter(commentList);
                                mRecyclerView.setAdapter(mAdapter);

                            }

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
        Singleton.getInstance().addToRequestQueue(jsObjRequest);

    }

    /**
     * Begins setting values to the ViewGroups on the fragment
     * @param details The JSONObject that includes all question query results
     * @throws JSONException
     */
    private void showQuestionDetails(JSONObject details) throws JSONException {

        String firstName = details.getString("first_name");

        if(firstName.length() > 14) {
            firstName = firstName.substring(0, 13).concat("...");
        }
        String userNameText = firstName + " " +details.getString("last_name");
        String topicText = details.getString("topic");
        String question = details.getString("text");
        String dateText = details.getString("date");
        double latitude = details.getDouble("latitude");
        double longitude = details.getDouble("longitude");


        if(details.has("image")) {
            String userImageUrl = details.getString("image");
            getUserImage(userImageUrl, userImage);
        }

        mStudyGroupBool = details.getBoolean("study_group");
        mTutorBool = details.getBoolean("tutor");


        userName.setText(userNameText);
        questionText.setText(question);
        questionTopic.setText(topicText);
        questionDate.setText(Singleton.getInstance().doDateLogic(dateText));

        if(mCurrentLocation != null)
            questionDistance.setText(Singleton.getInstance().doDistanceLogic(latitude, longitude, mCurrentLocation.getLatitude(), mCurrentLocation.getLongitude(), "MI")+"MI");
        else
            questionDistance.setVisibility(View.INVISIBLE);

        if(mTutorBool)
        tutorIcon.setColorFilter(getResources().getColor(R.color.primary));

        if(mStudyGroupBool)
        groupIcon.setColorFilter(getResources().getColor(R.color.primary));

    }

    /**
     * Begins setting up horizontal scrollview ViewGroup for tags
     * @param tags The ArrayList of tags
     */
    private void showQuestionTags(ArrayList<String> tags){

        if(tags != null && tags.size() > 0){
            for(int i = 0; i < tags.size(); i++) {
                TextView text = new TextView(mContext);
                text.setText("#"+tags.get(i)+ "   ");
                text.setTextColor(getResources().getColor(R.color.primary_dark));
                taglist.addView(text);
            }

        }

    }

    /**
     * Sets up the FAB depending on user details
     */
    public void buildFab() {

            String currentUserId = sharedPref.getString("user_id", "");
            String questionOwner = "";
        try {
            questionOwner = mQuestionInfo.getString("user_id");
        } catch (JSONException e) {
            e.printStackTrace();
        }


        if(currentUserId != null ? currentUserId.equalsIgnoreCase(questionOwner) : false){
                //owner
                fab.setIcon(getResources().getDrawable(R.drawable.ic_create_white_24dp), false);
                fab.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent mIntent = new Intent(mContext, CreateQuestionActivity.class);
                        mIntent.putExtra("questionId", mQuestionId);
                        startActivityForResult(mIntent, EDIT_QUESTION);
                    }
                });
                fab.setVisibility(View.VISIBLE);
            }else{
                getGroupActiveStatus();
            }

    }

    /**
     * Grabs the group leader's image from the server
     * @param imageUrl The URL of the image
     * @param imageView The ImageView to be populated
     */
    private void getUserImage(String imageUrl, final ImageView imageView){

        ImageLoader imageLoader = Singleton.getInstance().getImageLoader();

        imageLoader.get(imageUrl, new ImageLoader.ImageListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                //Log.e(TAG, "Image Load Error: " + error.getMessage());
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
     * Sets up the Google API Client
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
     * Once GoogleApi is connected, grab the location
     * @param bundle Stored bundle, unused
     */
    @Override
    public void onConnected(Bundle bundle) {
        mCurrentLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
    }

    /**
     * When connection is suspended, report the error
     * @param i Unused, necessary for overriding
     */
    @Override
    public void onConnectionSuspended(int i) {
        showConnectionIssueDialog();
    }

    /**
     * When connection has failed, report the error
     * @param connectionResult The result of the connection
     */
    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        showConnectionIssueDialog();
    }

    /**
     * Sets up OnClickListeners for specified views
     * @param v The view in question
     */
    @Override
    public void onClick(View v) {
        if(v == sendButton){
            if(!commentEditText.getText().toString().equalsIgnoreCase("")) {
                commentEditText.clearError();
                InputMethodManager imm = (InputMethodManager) mContext.getSystemService(
                        Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(commentEditText.getWindowToken(), 0);
                addComment();
            }
            else{
                commentEditText.setError("");
            }


        }

    }

    /**
     * When location is changed, change member variable to reflect location change
     * @param location The new location
     */
    @Override
    public void onLocationChanged(Location location) {
        mCurrentLocation = location;

    }

    /**
     * Snackbar dialog for connection issues
     */
    private void showConnectionIssueDialog(){
        Toast.makeText(mContext, "Connection error, try again", Toast.LENGTH_SHORT).show();
        if( getActivity() != null)
            ((ViewQuestionActivity) getActivity()).mSnackBar.show();
    }

    /**
     * Sets up the RecyclerView of comments
     */
    public class RecycleViewAdapter extends RecyclerView.Adapter<RecycleViewAdapter.ViewHolder>{

        private final JSONArray commentList;
        private final int rowLayout;

        public RecycleViewAdapter(JSONArray commentList) {
            this.commentList = commentList;
            this.rowLayout = R.layout.question_comment_item;
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
            View v = LayoutInflater.from(viewGroup.getContext()).inflate(rowLayout, viewGroup, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(ViewHolder viewHolder, int position) {

            try {

                String firstName = commentList.getJSONObject(position).getJSONObject("map").getString("first_name");
                String lastName = commentList.getJSONObject(position).getJSONObject("map").getString("last_name");
                String text = commentList.getJSONObject(position).getJSONObject("map").getString("comment");
                String date = commentList.getJSONObject(position).getJSONObject("map").getString("posted");


                if(commentList.getJSONObject(position).getJSONObject("map").has("image") &&
                        !commentList.getJSONObject(position).getJSONObject("map").getString("image").equalsIgnoreCase("")){
                    String userImageUrl = commentList.getJSONObject(position).getJSONObject("map").getString("image");
                    getUserImage(userImageUrl, viewHolder.userImage);
                }


                viewHolder.nameText.setText(firstName + " " + lastName);

                viewHolder.questionText.setText(text);
                viewHolder.dateText.setText(Singleton.getInstance().doDateLogic(date));



            } catch (JSONException e) {
                e.printStackTrace();
            }


        }

        @Override
        public int getItemCount() {
            return commentList.length();
        }

        public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {


            public final ImageView userImage;
            public final TextView questionText;
            public final TextView nameText;
            public final TextView dateText;

            //Declare views here, dont fill them
            public ViewHolder(View itemView) {
                super(itemView);
                questionText = (TextView) itemView.findViewById(R.id.comment_text);
                nameText = (TextView) itemView.findViewById(R.id.comment_name_text);
                dateText = (TextView) itemView.findViewById(R.id.comment_timestamp);
                userImage = (ImageView) itemView.findViewById(R.id.comment_user_image);


                userImage.setOnClickListener(this);
                nameText.setOnClickListener(this);

            }


            @Override
            public void onClick(View v) {

                if((v == nameText || v == userImage)){

                    try {
                        String userId = commentList.getJSONObject(getAdapterPosition()).getJSONObject("map").getString("user_id");
                        Intent intent = new Intent(mContext, ProfileActivity.class);
                        intent.putExtra("userId", userId);
                        startActivity(intent);
                    } catch (JSONException e) {
                        e.printStackTrace();
                        Toast.makeText(mContext, "Missing UserId", Toast.LENGTH_SHORT).show();
                    }

                }

            }
        }

    }

    /**
     * FAB option for members in the group that are not the group owner
     */
    private void showRemoveYourselfDialog(){

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setMessage("Are you sure you want to remove yourself from this group?");

        builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                removeYourselfFromGroup(sharedPref.getString("user_id", ""));
            }
        });
        builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {

            }
        });

        AlertDialog dialog = builder.create();
        dialog.show();

    }

    /**
     * FAB option for non-members viewing the group
     */
    private void showJoinDialog(){

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        if(mStudyGroupBool && mTutorBool) {
            builder.setMessage("Would you like to join as a tutor or a group member?");
            builder.setPositiveButton("Member", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    sendAskToJoinGroup(0);
                }
            });
            builder.setNegativeButton("Tutor", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    sendAskToJoinGroup(1);
                }
            });
        } else if (mStudyGroupBool) {
            builder.setMessage("Would you like to join as a group member?");
            builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    sendAskToJoinGroup(0);
                }
            });
            builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            });
        } else if (mTutorBool) {
            builder.setMessage("Would you like to join as a tutor?");
            builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    sendAskToJoinGroup(1);
                }
            });
            builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            });
        }
        AlertDialog dialog = builder.create();
        dialog.show();

    }

    /**
     * Reports newly added comment to the server and updates the RecyclerView
     */
    private void addComment() {
        SharedPreferences sharedPref = mContext.getSharedPreferences(
                getString(R.string.preference_file_key), Context.MODE_PRIVATE);

        List<NameValuePair> params = new LinkedList<>();
        params.add(new BasicNameValuePair("questionId", mQuestionId));
        params.add(new BasicNameValuePair("userId", sharedPref.getString("user_id", "")));
        params.add(new BasicNameValuePair("comment", commentEditText.getText().toString()));

        String paramString = URLEncodedUtils.format(params, "utf-8")
                .replaceAll("%27", "%27%27");

        String url = "http://54.200.33.91:8080/com.mysql.services/rest/serviceclass/addComment?"+paramString;

        JsonObjectRequest jsObjRequest = new JsonObjectRequest(Request.Method.GET, url,
                (JSONObject)null,
                new Response.Listener<JSONObject>(){

                    @Override
                    public void onResponse(JSONObject response) {
                        try {

                            JSONObject result = new JSONObject(response.toString());
                            if(result.getString("success").equalsIgnoreCase("1")){
                                commentEditText.setText("");
                                getCommentsData();
                            }
                            else{
                                Toast.makeText(mContext, "Error posting comment", Toast.LENGTH_SHORT).show();
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

        Singleton.getInstance().addToRequestQueue(jsObjRequest);

    }

    /**
     * Sends join group request on dialog acceptance
     * @param type Tutor or member
     */
    private void sendAskToJoinGroup(int type) {
        SharedPreferences sharedPref = mContext.getSharedPreferences(
                getString(R.string.preference_file_key), Context.MODE_PRIVATE);

        List<NameValuePair> params = new LinkedList<>();
        params.add(new BasicNameValuePair("questionId", mQuestionId));
        params.add(new BasicNameValuePair("userId", sharedPref.getString("user_id", "")));
        params.add(new BasicNameValuePair("tutor", type + ""));

        String paramString = URLEncodedUtils.format(params, "utf-8");
        String url = "http://54.200.33.91:8080/com.mysql.services/rest/serviceclass/askToJoinGroup?"+paramString;

        JsonObjectRequest jsObjRequest = new JsonObjectRequest(Request.Method.GET, url,
                (JSONObject)null,
                new Response.Listener<JSONObject>(){

                    @Override
                    public void onResponse(JSONObject response) {
                        try {

                            JSONObject result = new JSONObject(response.toString());
                            if(result.getString("success").equalsIgnoreCase("1")){
                                Toast.makeText(mContext, "Requested to join the group!", Toast.LENGTH_SHORT).show();
                            }
                            else{
                                Toast.makeText(mContext, "Error Sending Group Request!", Toast.LENGTH_SHORT).show();
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

        Singleton.getInstance().addToRequestQueue(jsObjRequest);

    }

    /**
     * Grabs the status of the viewer and whether he is in the group
     */
    private void getGroupActiveStatus(){

        List<NameValuePair> params = new LinkedList<>();
        params.add(new BasicNameValuePair("userId", sharedPref.getString("user_id", "")));

        String paramString = URLEncodedUtils.format(params, "utf-8");
        String url = "http://54.200.33.91:8080/com.mysql.services/rest/serviceclass/inGroup?"+paramString;

        JsonObjectRequest jsObjRequest = new JsonObjectRequest(Request.Method.GET, url,
                (JSONObject)null,
                new Response.Listener<JSONObject>(){

                    @Override
                    public void onResponse(JSONObject response) {
                        try {

                            JSONObject result = new JSONObject(response.toString());
                            if(result.getString("success").equalsIgnoreCase("1")){
                                if(result.getString("expectResults").equalsIgnoreCase("1")) {
                                    String questionId = result.getJSONObject("result").getJSONArray("myArrayList").getJSONObject(0).getJSONObject("map").getString("question_id");

                                    if (questionId.equalsIgnoreCase(mQuestionId)) {
                                        //show remove yourself button

                                        fab.setIcon(getResources().getDrawable(R.drawable.ic_remove_white_24dp), false);
                                        fab.setOnClickListener(new View.OnClickListener() {
                                            @Override
                                            public void onClick(View v) {
                                                //Do edit dialog??
                                                showRemoveYourselfDialog();
                                            }
                                        });
                                        fab.setVisibility(View.VISIBLE);
                                    }
                                }
                                else{//Not in a group. Show want to add button
                                    fab.setIcon(getResources().getDrawable(R.drawable.ic_person_add_white_18dp), false);
                                    fab.setOnClickListener(new View.OnClickListener() {
                                        @Override
                                        public void onClick(View v) {
                                            showJoinDialog();
                                        }
                                    });
                                    fab.setVisibility(View.VISIBLE);

                                }

                            }

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

        Singleton.getInstance().addToRequestQueue(jsObjRequest);

    }


        private void removeYourselfFromGroup(final String userId) {

            List<NameValuePair> params = new LinkedList<>();
            params.add(new BasicNameValuePair("userId", userId));

            String paramString = URLEncodedUtils.format(params, "utf-8");
            String url = "http://54.200.33.91:8080/com.mysql.services/rest/serviceclass/removeUser?" + paramString;

            JsonObjectRequest jsObjRequest = new JsonObjectRequest(Request.Method.GET, url,
                    (JSONObject) null,
                    new Response.Listener<JSONObject>() {

                        @Override
                        public void onResponse(JSONObject response) {
                            try {

                                JSONObject result = new JSONObject(response.toString());
                                if (result.getString("success").equalsIgnoreCase("1")) {
                                    Toast.makeText(mContext, "User removed", Toast.LENGTH_SHORT).show();
                                    buildFab();
                                    //getActivity().setResult(getActivity().RESULT_OK);
                                    //getActivity().finish();
                                } else {
                                    Toast.makeText(mContext, "Error removing user", Toast.LENGTH_SHORT).show();
                                }

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

            Singleton.getInstance().addToRequestQueue(jsObjRequest);

        }

    /**
     * Once CreateQuestionActivity has finished, record results
     * @param requestCode The requestCode from CreateQuestionActivity
     * @param resultCode The resultCode and whether it was a success
     * @param data The data sent back
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        // Check which request we're responding to
        if (requestCode == EDIT_QUESTION) {
            // Make sure the request was successful
            if (resultCode == Activity.RESULT_OK) {
                ((ViewQuestionActivity) getActivity()).getQuestionData();
                mQuestionInfo = ((ViewQuestionActivity) getActivity()).mQuestionInfo;
                tags = ((ViewQuestionActivity) getActivity()).tags;
                try {
                    showQuestionDetails(mQuestionInfo);
                    showQuestionTags(tags);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }
    }

}

