package com.sos.saveourstudents;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.JsonObjectRequest;
import com.rey.material.widget.FloatingActionButton;

import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.LinkedList;
import java.util.List;


/**
 * Activity for the push notification sent after a user requests to join your group
 */
public class MemberWantsToJoinActivity extends AppCompatActivity implements View.OnClickListener {

    private String mQuestionId; // The questionId of the group
    private String mUserId; // The userId of the requester

    private String mType;
    FloatingActionButton acceptButton;
    FloatingActionButton declineButton;
    RelativeLayout userProfile;

    private String mInGroupUrl =
            "http://54.200.33.91:8080/com.mysql.services/rest/serviceclass/inGroup?userId=";
    private String mRemoveMemberUrl =
            "http://54.200.33.91:8080/com.mysql.services/rest/serviceclass/removeUser?userId=";

    /**
     * Initializes the singleton and begin setting up member variables
     * @param savedInstanceState Unused, necessary for method
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.member_join_layout);

        if(!Singleton.hasBeenInitialized()){
            Singleton.initialize(this);
        }

        if (getIntent().getExtras() != null) {
            mUserId = getIntent().getExtras().getString("userId");
            mQuestionId = getIntent().getExtras().getString("questionId");
            mType = getIntent().getExtras().getString("type");
        }


        getUserInfo();

        userProfile = (RelativeLayout) findViewById(R.id.joining_member_info);
        acceptButton = (FloatingActionButton) findViewById(R.id.accept_fab);
        declineButton = (FloatingActionButton) findViewById(R.id.decline_fab);

        userProfile.setOnClickListener(this);
        acceptButton.setOnClickListener(this);
        declineButton.setOnClickListener(this);
    }

    /**
     * Setup for all OnClickListeners for the views of the activity
     * @param v The view in question
     */
    public void onClick(View v) {

        if (v == acceptButton) {
            removeUserFromCurrentGroup();
        } else if (v == declineButton) {
            finish();
        } else if (v == userProfile) {
            Intent profile = new Intent(this, ProfileActivity.class);
            profile.putExtra("userId", mUserId);
            startActivity(profile);
        }

    }

    /**
     * Grabs the current user's image from the database
     * @param imageUrl The URL of the image
     * @param imageView The ImageView to be populated
     */
    private void getUserImage(String imageUrl, final ImageView imageView) {

        ImageLoader imageLoader = Singleton.getInstance().getImageLoader();
        // If you are using normal ImageView
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
                }
            }
        });

    }

    /**
     * Grabs the requester's info from the database
     */
    private void getUserInfo() {

        List<NameValuePair> params = new LinkedList<NameValuePair>();
        params.add(new BasicNameValuePair("userId", mUserId));

        String paramString = URLEncodedUtils.format(params, "utf-8");
        String url = "http://54.200.33.91:8080/com.mysql.services/rest/serviceclass/getUserById?" + paramString;

        JsonObjectRequest jsObjRequest = new JsonObjectRequest(Request.Method.GET, url,
                (JSONObject) null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            JSONObject result = new JSONObject(response.toString());
                            if (result.getString("success").equalsIgnoreCase("1")) {

                                JSONObject userObject = result.getJSONObject("result").getJSONArray("myArrayList").getJSONObject(0).getJSONObject("map");
                                String name = userObject.getString("first_name") + " " + userObject.getString("last_name");

                                TextView joiningMemberName = (TextView) findViewById(R.id.member_name);
                                joiningMemberName.setText(userObject.getString("first_name"));

                                TextView memberInfoName = (TextView) findViewById(R.id.joining_member_name);
                                memberInfoName.setText(name);

                                TextView memberInfoMajor = (TextView) findViewById(R.id.joining_member_major);
                                memberInfoMajor.setText(userObject.getString("major"));


                                if (userObject.has("image")) {
                                    ImageView userImage = (ImageView) findViewById(R.id.joining_member_image);
                                    getUserImage(userObject.getString("image"), userImage);
                                }

                            } else {
                                //Error...
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
     * If accept button is clicked, the member is added to the group in a server call
     */
    private void addMembertoGroup() {

        List<NameValuePair> params = new LinkedList<NameValuePair>();
        params.add(new BasicNameValuePair("questionId", mQuestionId));
        params.add(new BasicNameValuePair("userId", mUserId));
        params.add(new BasicNameValuePair("tutor", (mType.equalsIgnoreCase("2") ? 1 : 0) + ""));

        String paramString = URLEncodedUtils.format(params, "utf-8");
        String url = "http://54.200.33.91:8080/com.mysql.services/rest/serviceclass/acceptUser?" + paramString;

        JsonObjectRequest jsObjRequest = new JsonObjectRequest(Request.Method.GET, url,
                (JSONObject) null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            JSONObject result = new JSONObject(response.toString());
                            if (result.getString("success").equalsIgnoreCase("1")) {
                                finish();
                            } else {
                                Toast.makeText(MemberWantsToJoinActivity.this, "Error Accepting User", Toast.LENGTH_SHORT).show();
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
     * Removes the user from his current group if accepted and he is current in a group
     */
    private void removeUserFromCurrentGroup() {
        String isJoinerInGroup = mInGroupUrl + mUserId;
        JsonObjectRequest inGroupRequest = new JsonObjectRequest(Request.Method.GET, isJoinerInGroup,
                (JSONObject) null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                try {
                    JSONObject result = new JSONObject(response.toString());
                    if(result.getInt("expectResults") != 0) {
                        String removeMemberFromGroup = mRemoveMemberUrl + mUserId;
                        JsonObjectRequest removeMemberRequest =
                                new JsonObjectRequest(Request.Method.GET, removeMemberFromGroup,
                                        (JSONObject) null, new RemoveUserFromGroupResponseListener(),
                                        new Response.ErrorListener() {
                                            @Override
                                            public void onErrorResponse(VolleyError error) {

                                            }
                                        });
                        Singleton.getInstance().addToRequestQueue(removeMemberRequest);

                    }
                    else {
                        addMembertoGroup();
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

    /**
     * ResponseListener for removing the user from his current group
     * Starts call to add the member to the group that is accepting said user
     */
    private class RemoveUserFromGroupResponseListener implements Response.Listener<JSONObject> {
        @Override
        public void onResponse(JSONObject response) {
            addMembertoGroup();
        }
    }
}
