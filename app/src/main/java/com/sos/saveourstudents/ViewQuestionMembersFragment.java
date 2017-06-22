package com.sos.saveourstudents;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.JsonObjectRequest;

import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

/**
 * Fragment for the third pane of ViewQuestionActivity
 * Displays all students and tutors in the group in a ListView
 */
public class ViewQuestionMembersFragment extends Fragment {


    // Keep track of all students and tutors for ArrayAdapters
    ArrayList<JSONObject> mTutorList = null;
    ArrayList<JSONObject> mStudentList = null;
    private ListView mStudentsListView;
    private ListView mTutorsListView;
    private StudentsArrayAdapter mStudentsListViewArrayAdapter;
    private TutorsArrayAdapter mTutorsListViewArrayAdapter;

    // Used for the already-rated tutors
    private ArrayList<JSONObject> mRatedList;
    private HashMap<String, JSONObject> mRatedHashMap;

    // The current viewer's user id
    private String mViewerUserId;
    private SharedPreferences sharedPref;

    private String mQuestionId;
    public Context mContext;
    private boolean mEditable;
    private boolean mIsMemberOfGroup;

    private static final String mGetRateListUrl =
            "http://54.200.33.91:8080/com.mysql.services/rest/serviceclass/getRateList?userId=";

    /**
     * Instantiates a new instances of this fragment
     *
     * @param questionId The current question's questionId
     * @param editable   Whether the question belongs to the current viewer
     * @param isMember   Whether the current viewer is a member of the group
     * @return An instance of this fragment
     */
    public static ViewQuestionMembersFragment newInstance(String questionId, boolean editable, boolean isMember) {

        ViewQuestionMembersFragment fragment = new ViewQuestionMembersFragment();
        Bundle args = new Bundle();
        args.putString("questionId", questionId);
        args.putBoolean("editable", editable);
        args.putBoolean("isMember", isMember);
        fragment.setArguments(args);
        return fragment;
    }

    /**
     * Empty constructor
     */
    public ViewQuestionMembersFragment() {
        // Required empty public constructor
    }

    /**
     * Retrieves the userId of the viewer through shared preferences
     * @param savedInstanceState Unused for this fragment
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mViewerUserId = getActivity()
                .getSharedPreferences(getString(R.string.preference_file_key), Context.MODE_PRIVATE)
                .getString("user_id", "");
    }

    /**
     * Sets up member variables once views have been inflated
     * @param inflater The LayoutInflater of the current activity
     * @param container The ViewGroup that will contain both ListViews
     * @param savedInstanceState Unused
     * @return The root view of the fragment
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        if (getArguments() != null) {
            mQuestionId = getArguments().getString("questionId");
            mEditable = getArguments().getBoolean("editable");
            mIsMemberOfGroup = getArguments().getBoolean("isMember");
        } else {
            //error
        }


        mContext = this.getActivity();
        View rootView = inflater.inflate(R.layout.fragment_view_group_members, container, false);

        sharedPref = mContext.getSharedPreferences(
                getString(R.string.preference_file_key), Context.MODE_PRIVATE);

        mStudentsListView = (ListView) rootView.findViewById(R.id.view_group_members_students_listView);
        mTutorsListView = (ListView) rootView.findViewById(R.id.view_group_members_tutors_listView);

        mRatedList = new ArrayList<>();
        mRatedHashMap = new HashMap<>();

        //getGroupActiveStatus(); // Also retrieveListOfRatedTutors(), getMemberData()
        retrieveListOfRatedTutors();
        return rootView;
    }

    /**
     * Populates the arrays of Students and Tutors from the server
     */
    private void getMemberData() {

        //final String currentUserId = sharedPref.getString("user_id", "");

        List<NameValuePair> params = new LinkedList<NameValuePair>();
        params.add(new BasicNameValuePair("questionId", mQuestionId));

        String paramString = URLEncodedUtils.format(params, "utf-8");
        String url = "http://54.200.33.91:8080/com.mysql.services/rest/serviceclass/viewMembers?" + paramString;

        JsonObjectRequest jsObjRequest = new JsonObjectRequest(Request.Method.GET, url,
                (JSONObject) null,
                new Response.Listener<JSONObject>() {

                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            JSONObject result = new JSONObject(response.toString());
                            if (result.getString("success").equalsIgnoreCase("1")) {
                                mStudentList = null;
                                mTutorList = null;

                                JSONArray memberList = result.getJSONObject("result").getJSONArray("myArrayList");
                                for (int a = 0; a < memberList.length(); a++) {
                                    JSONObject currStudent = memberList.getJSONObject(a).getJSONObject("map");
                                    if (memberList.getJSONObject(a).getJSONObject("map").getBoolean("tutor")) {
                                        if (mTutorList == null) {
                                            mTutorList = new ArrayList<JSONObject>();
                                        }
                                        mTutorList.add(currStudent);
                                    } else {
                                        if (mStudentList == null) {
                                            mStudentList = new ArrayList<JSONObject>();
                                        }
                                        mStudentList.add(memberList.getJSONObject(a).getJSONObject("map"));
                                    }
                                }

                                if (mTutorList != null && mTutorList.size() > 0) {
                                    mTutorsListViewArrayAdapter = new TutorsArrayAdapter(mContext, mTutorList);
                                    mTutorsListView.setAdapter(mTutorsListViewArrayAdapter);
                                }

                                if (mStudentList != null && mStudentList.size() > 0) {
                                    mStudentsListViewArrayAdapter = new StudentsArrayAdapter(mContext, mStudentList);
                                    mStudentsListView.setAdapter(mStudentsListViewArrayAdapter);
                                }
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }, new Response.ErrorListener() {

            @Override
            public void onErrorResponse(VolleyError error) {
                if (getActivity() != null)
                    ((ViewQuestionActivity) getActivity()).mSnackBar.show();
            }

        });

        // Add the request to the queue
        Singleton.getInstance().addToRequestQueue(jsObjRequest);

    }

    /**
     * The array adapter for the Students ListView
     */
    private class StudentsArrayAdapter extends ArrayAdapter<JSONObject> {
        private List<JSONObject> data;

        public StudentsArrayAdapter(Context context, List<JSONObject> objects) {
            super(context, 0, objects);
            data = objects;

        }

        @Override
        public JSONObject getItem(int position) {
            return data.get(position);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            final JSONObject currStudentMember = getItem(position);
            if (convertView == null) {
                convertView = LayoutInflater.from(getContext())
                        .inflate(R.layout.view_group_members_student_layout, parent, false);
            }
            try {
                String name = currStudentMember.getString("first_name") + " " +
                        currStudentMember.getString("last_name");
                ((TextView) convertView.findViewById(R.id.user_name)).setText(name);

                if (currStudentMember.has("image"))
                    getUserImage(currStudentMember.getString("image"), ((ImageView) convertView.findViewById(R.id.user_image)));


                final String userId = currStudentMember.getString("user_id");

                // If group leader, add functionality to remove a member from the group
                if (!userId.equalsIgnoreCase(sharedPref.getString("user_id", "")) && mEditable) {
                    convertView.setOnLongClickListener(new View.OnLongClickListener() {
                        @Override
                        public boolean onLongClick(View v) {
                            showRemoveUserDialog(userId);
                            return true;
                        }
                    });

                }

            } catch (JSONException e) {
                e.printStackTrace();
            }

            // Clicking on a member will take you to their profile
            convertView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    try {
                        startActivity(new Intent(getActivity(), ProfileActivity.class)
                                .putExtra("userId", currStudentMember.getString("user_id")));
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            });

            return convertView;
        }
    }

    /**
     * Array adapter to be used with the Tutors ListView
     * Incorporates Thumbs Up/Down functionality
     */
    private class TutorsArrayAdapter extends ArrayAdapter<JSONObject> {
        private List<JSONObject> data;

        public TutorsArrayAdapter(Context context, List<JSONObject> objects) {
            super(context, 0, objects);
            data = objects;

        }

        @Override
        public JSONObject getItem(int position) {
            return data.get(position);
        }


        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            final JSONObject currStudentMember = getItem(position);

            if (convertView == null) {
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.view_group_members_tutors_layout, parent, false);
            }

            try {
                ((TextView) convertView.findViewById(R.id.tutor_firstName))
                        .setText(currStudentMember.getString("first_name"));

                ((TextView) convertView.findViewById(R.id.tutor_lastName))
                        .setText(currStudentMember.getString("last_name"));


                if (currStudentMember.has("image"))
                    getUserImage(currStudentMember.getString("image"), ((ImageView) convertView.findViewById(R.id.tutor_profile_image)));


                final String userId = currStudentMember.getString("user_id");


                if (!userId.equalsIgnoreCase(sharedPref.getString("user_id", "")) && mEditable) {
                    convertView.setOnLongClickListener(new View.OnLongClickListener() {
                        @Override
                        public boolean onLongClick(View v) {
                            showRemoveUserDialog(userId);

                            return true;
                        }
                    });

                }


                ImageView mThumbsUpButton = ((ImageView) convertView.findViewById(R.id.view_group_members_thumbs_up));
                ImageView mThumbsDownButton = ((ImageView) convertView.findViewById(R.id.view_group_members_thumbs_down));

                // You must be a member of the group to see tutors
                // You cannot rate yourself
                if (mIsMemberOfGroup && !userId.equals(mViewerUserId)) {
                    mThumbsUpButton.setVisibility(View.VISIBLE);
                    mThumbsDownButton.setVisibility(View.VISIBLE);
                    setUpThumbsUpButton(mThumbsUpButton, mThumbsDownButton,
                            currStudentMember.getString("user_id"), currStudentMember);
                    setUpThumbsDownButton(mThumbsUpButton, mThumbsDownButton,
                            currStudentMember.getString("user_id"), currStudentMember);

                } else {
                    mThumbsUpButton.setVisibility(View.GONE);
                    mThumbsDownButton.setVisibility(View.GONE);
                }


            } catch (JSONException e) {
                e.printStackTrace();
            }

            // Clicking on a tutor takes you to their profile
            convertView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    try {
                        startActivity(new Intent(getActivity(), ProfileActivity.class)
                                .putExtra("userId", currStudentMember.getString("user_id")));
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            });


            return convertView;
        }

        /**
         * Sets up the thumbs up button drawable for functionality
         *
         * @param mThumbsUpButton   The ImageView for the thumbs up button
         * @param mThumbsDownButton The ImageView for the thumbs down button
         * @param userId            The userId of the tutor being rated
         * @param userJson          The JSONObject of the tutor being rated
         */
        private void setUpThumbsUpButton(final ImageView mThumbsUpButton,
                                         final ImageView mThumbsDownButton, final String userId,
                                         final JSONObject userJson) {
            boolean rated = false;
            for (int i = 0; i < mRatedList.size(); i++) {
                try {
                    if (userId.equals(mRatedList.get(i).getString("rated_user"))) {
                        if (mRatedList.get(i).getInt("rating") == 1) {
                            mThumbsUpButton.setColorFilter(getResources().getColor(R.color.primary));
                            mThumbsUpButton.setSelected(true);
                        } else {
                            mThumbsUpButton.setColorFilter(getResources().getColor(R.color.divider_color));
                            mThumbsUpButton.setSelected(false);
                        }
                        rated = true;
                        break;
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            if (!rated) {
                mThumbsUpButton.setColorFilter(getResources().getColor(R.color.divider_color));
                mThumbsUpButton.setSelected(false);
            }

            mThumbsUpButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    if (!mThumbsUpButton.isSelected()) {

                        mThumbsUpButton.setColorFilter(getResources().getColor(R.color.primary));
                        mThumbsUpButton.setSelected(true);
                        // Case 2 - Thumbs down button is currently clicked
                        if (mThumbsDownButton.isSelected()) {
                            mThumbsDownButton.setColorFilter(getResources().getColor(R.color.divider_color));
                            mThumbsDownButton.setSelected(false);
                        }
                        rateTutor(mViewerUserId, userId, 1, userJson);

                    }
                    // Thumbs Up Button is currently clicked
                    else {
                        mThumbsUpButton.setColorFilter(getResources().getColor(R.color.divider_color));
                        mThumbsUpButton.setSelected(false);
                        rateTutor(mViewerUserId, userId, 0, userJson);
                    }
                }
            });
        }

        /**
         * Sets up thumbs down button drawable functionality
         *
         * @param mThumbsUpButton   The thumbs up button of the tutor being rated
         * @param mThumbsDownButton The thumbs down button of the tutor being rated
         * @param userId            The userId of the tutor being rated
         * @param userJson          The JSONObject of the tutor being rated
         */
        private void setUpThumbsDownButton(final ImageView mThumbsUpButton,
                                           final ImageView mThumbsDownButton, final String userId,
                                           final JSONObject userJson) {
            boolean rated = false;
            for (int i = 0; i < mRatedList.size(); i++) {
                try {
                    if (userId.equals(mRatedList.get(i).getString("rated_user"))) {
                        if (mRatedList.get(i).getInt("rating") == -1) {
                            mThumbsDownButton.setColorFilter(getResources().getColor(R.color.red));
                            mThumbsDownButton.setSelected(true);
                        } else {
                            mThumbsDownButton.setColorFilter(getResources().getColor(R.color.divider_color));
                            mThumbsDownButton.setSelected(false);
                        }
                        rated = true;
                        break;
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            if (!rated) {
                mThumbsDownButton.setColorFilter(getResources().getColor(R.color.divider_color));
                mThumbsDownButton.setSelected(false);
            }

            mThumbsDownButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // Thumbs Down Button is not clicked
                    if (!mThumbsDownButton.isSelected()) {
                        mThumbsDownButton.setColorFilter(getResources().getColor(R.color.red));
                        mThumbsDownButton.setSelected(true);
                        // Case 2 - Thumbs Up Button is currently clicked
                        if (mThumbsUpButton.isSelected()) {
                            mThumbsUpButton.setColorFilter(getResources().getColor(R.color.divider_color));
                            mThumbsUpButton.setSelected(false);
                        }

                        rateTutor(mViewerUserId, userId, -1, userJson);
                    }
                    // Thumbs Down Button is currently clicked
                    else {
                        mThumbsDownButton.setColorFilter(getResources().getColor(R.color.divider_color));
                        mThumbsDownButton.setSelected(false);
                        rateTutor(mViewerUserId, userId, 0, userJson);
                    }
                }
            });
        }

    }

    /**
     * Grabs the current user's image for displaying in the ListView
     *
     * @param imageUrl  The URL of the image
     * @param imageView The ImageVIew to be populated
     */
    private void getUserImage(String imageUrl, final ImageView imageView) {

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
                    imageView.setImageDrawable(getResources().getDrawable(R.drawable.defaultprofile));
                }
            }
        });

    }

    /**
     * Builds the dialog for removing a user from the group
     *
     * @param userId The userId of the member to be removed
     */
    private void showRemoveUserDialog(final String userId) {

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setCancelable(false);
        builder.setMessage("Would you like to remove this user from your group?");

        builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                removeUser(userId);
                if (mStudentList != null) {
                    for (int i = 0; i < mStudentList.size(); i++) {
                        try {
                            if (mStudentList.get(i).getString("user_id").equals(userId)) {
                                mStudentList.remove(i);
                                mStudentsListViewArrayAdapter.notifyDataSetChanged();
                            }
                        } catch (JSONException e) {
                            Log.e("ViewMembers", "Error removing student from list!");
                            e.printStackTrace();
                        }
                    }
                }
                if (mTutorList != null) {
                    for (int i = 0; i < mTutorList.size(); i++) {
                        try {
                            if (mTutorList.get(i).getString("user_id").equals(userId)) {
                                mTutorList.remove(i);
                                mTutorsListViewArrayAdapter.notifyDataSetChanged();
                            }
                        } catch (JSONException e) {
                            Log.e("ViewMembers", "Error removing student from list!");
                            e.printStackTrace();
                        }
                    }
                }
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
     * Request to the server to remove the user from the group
     * @param userId The userId of the user to be removed
     */
    private void removeUser(String userId) {

        List<NameValuePair> params = new LinkedList<NameValuePair>();
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
                                getMemberData();
                            } else {
                                Toast.makeText(mContext, "Error removing user", Toast.LENGTH_SHORT)
                                        .show();
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }, new Response.ErrorListener() {

            @Override
            public void onErrorResponse(VolleyError error) {
                if (getActivity() != null)
                    ((ViewQuestionActivity) getActivity()).mSnackBar.show();
            }

        });
        Singleton.getInstance().addToRequestQueue(jsObjRequest);
    }

    /**
     * Request to database for rating the tutor
     * @param userId The userId of the student doing the rating
     * @param ratedUserId The userId of the tutor being rated
     * @param like Whether the end result was a like, dislike, or undis/like
     * @param userJson The JSONObject of the tutor
     */
    private void rateTutor(final String userId, final String ratedUserId, Integer like, final JSONObject userJson) {

        List<NameValuePair> params = new LinkedList<NameValuePair>();
        params.add(new BasicNameValuePair("userId", userId));
        params.add(new BasicNameValuePair("ratedUserId", ratedUserId));
        params.add(new BasicNameValuePair("like", like.toString()));

        Boolean rated = false;
        if (mRatedHashMap.containsKey(ratedUserId)) rated = true;

        params.add(new BasicNameValuePair("rated", rated.toString()));

        String paramString = URLEncodedUtils.format(params, "utf-8");
        String url = "http://54.200.33.91:8080/com.mysql.services/rest/serviceclass/rateTutor?" + paramString;

        JsonObjectRequest jsObjRequest = new JsonObjectRequest(Request.Method.GET, url,
                (JSONObject) null,
                new Response.Listener<JSONObject>() {

                    @Override
                    public void onResponse(JSONObject response) {
                        try {

                            JSONObject result = new JSONObject(response.toString());
                            if (result.getString("success").equalsIgnoreCase("1")) {
                                if (!mRatedHashMap.containsKey(ratedUserId)) {
                                    mRatedHashMap.put(ratedUserId, userJson);
                                    mRatedList.add(userJson);
                                }
                                Toast.makeText(mContext, "User rated", Toast.LENGTH_SHORT);

                            } else {
                                Toast.makeText(mContext, "Error rating", Toast.LENGTH_SHORT);
                            }

                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                    }
                }, new Response.ErrorListener() {

            @Override
            public void onErrorResponse(VolleyError error) {
                if (getActivity() != null)
                    ((ViewQuestionActivity) getActivity()).mSnackBar.show();
            }

        });

        Singleton.getInstance().addToRequestQueue(jsObjRequest);

    }

    /**
     * Populates the lists related to grabbing the current rated data for the viewer
     */
    protected void retrieveListOfRatedTutors() {
        String getRateList = mGetRateListUrl + mViewerUserId;
        JsonObjectRequest rateListRequest = new JsonObjectRequest(Request.Method.GET,
                getRateList, (JSONObject) null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                try {
                    JSONObject result = new JSONObject(response.toString());
                    if (result.getInt("expectResults") > 0) {
                        JSONArray arrayOfRatedTutors = result.getJSONObject("result")
                                .getJSONArray("myArrayList");
                        for (int i = 0; i < arrayOfRatedTutors.length(); i++) {
                            JSONObject currStudent = arrayOfRatedTutors.getJSONObject(i)
                                    .getJSONObject("map");
                            mRatedList.add(currStudent);
                            mRatedHashMap.put(currStudent.getString("rated_user"),
                                    currStudent);
                        }
                    }

                    getMemberData();

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {

                    }
                });
        Singleton.getInstance().addToRequestQueue(rateListRequest);
    }

}
