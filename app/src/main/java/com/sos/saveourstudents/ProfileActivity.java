package com.sos.saveourstudents;

import android.app.FragmentManager;
import android.content.Context;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.Response.ErrorListener;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Activity that maintains EditProfile and ViewProfile Fragments
 */
public class ProfileActivity extends AppCompatActivity
        implements ViewProfileFragment.OnEditButtonListener,
        EditProfileFragment.OnDoneButtonListener {

    private Student mCurrStudent;
    private ViewProfileFragment mViewProfileFragment;
    private EditProfileFragment mEditProfileFragment;

    private final String userIdTag = "userId";
    private final String userIdTag_sharedPreferences = "user_id";
    private final String mUserURL =
            "http://54.200.33.91:8080/com.mysql.services/rest/serviceclass/getUserById?userId=";
    private String mUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);


        if(!Singleton.hasBeenInitialized()){
            Singleton.initialize(this);
        }

        mUserId = getIntent().getStringExtra(userIdTag);

        String url = mUserURL + mUserId;

        JsonObjectRequest studentRequest = new JsonObjectRequest(Request.Method.GET,
                url, (JSONObject) null, new ProfileResponseListener(), new ProfileErrorListener());

        Singleton.getInstance().addToRequestQueue(studentRequest);

        Toolbar toolbar = (Toolbar) findViewById(R.id.profile_toolbar);
        if (toolbar != null) {
            toolbar.setTitle(R.string.app_name);
            setSupportActionBar(toolbar);
            toolbar.setNavigationIcon(R.drawable.ic_arrow_back_white_24dp);
            toolbar.setNavigationOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onBackPressed();
                }
            });
        }
    }


    /**
     * Overrides onBackPressed()
     * EditProfileFragment should go back to ViewProfileFragment
     */
    @Override
    public void onBackPressed() {

        if (mEditProfileFragment != null) {
            FragmentManager fragmentManager = getFragmentManager();
            fragmentManager.beginTransaction()
                    .remove(mEditProfileFragment)
                    .attach(mViewProfileFragment)
                    .commit();
            mEditProfileFragment = null;
        }
        else
            super.onBackPressed();

    }


    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_profile, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onEditButton() {
        mEditProfileFragment = EditProfileFragment.newInstance(mCurrStudent);
        getFragmentManager().beginTransaction()
                .detach(mViewProfileFragment)
                .add(R.id.profile_activity_container, mEditProfileFragment)
                .addToBackStack(null)
                .commit();
    }

    @Override
    public void onDoneButton() {
        if (mEditProfileFragment != null && mViewProfileFragment != null) {
            mEditProfileFragment.updateStudent();

            if(getCurrentFocus() != null) {
                InputMethodManager in = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                in.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(),
                        InputMethodManager.HIDE_NOT_ALWAYS);
            }
            FragmentManager fragmentManager = getFragmentManager();
            fragmentManager.beginTransaction()
                    .remove(mEditProfileFragment)
                    .attach(mViewProfileFragment)
                    .commit();
            mEditProfileFragment = null;
        } else {
            Log.e("ProfileActivity", "NullPointerException for a fragment");
        }
    }

    private boolean isCurrentlyTheUser() {
        if (getSharedPreferences(getString(R.string.preference_file_key), Context.MODE_PRIVATE)
                .getString(userIdTag_sharedPreferences, "").equals(mUserId))
            return true;
        else return false;
    }

    /**
     * Handles the JSONObject response
     * Creates the student to be displayed in the ViewProfileFragment
     * Performs the transaction through the FragmentManager
     */
    class ProfileResponseListener implements Response.Listener<JSONObject> {

        @Override
        public void onResponse(JSONObject response) {
            try {
                JSONObject theResponse = new JSONObject(response.toString())
                        .getJSONObject("result")
                        .getJSONArray("myArrayList")
                        .getJSONObject(0)
                        .getJSONObject("map");


                String imageUrl = "";
                if(theResponse.has("image"))
                    imageUrl = theResponse.getString("image");

                mCurrStudent = new Student(theResponse.getString("first_name"),
                        theResponse.getString("last_name"), theResponse.getInt("rating"),
                        theResponse.getString("school"), theResponse.getString("major"),
                        theResponse.getString("description"), imageUrl);

                mViewProfileFragment = ViewProfileFragment.newInstance(mCurrStudent,
                        isCurrentlyTheUser());
                getFragmentManager().beginTransaction()
                        .add(R.id.profile_activity_container, mViewProfileFragment)
                        .addToBackStack(null)
                        .commit();

            } catch (JSONException e) {
                Log.e("ProfileActivity", "Error retrieving student from database!");
                e.printStackTrace();
            }
        }
    }
    class ProfileErrorListener implements ErrorListener {

        @Override
        public void onErrorResponse(VolleyError error) {

        }
    }


}