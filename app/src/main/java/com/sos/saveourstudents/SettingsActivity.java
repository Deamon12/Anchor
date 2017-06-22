package com.sos.saveourstudents;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.rey.material.widget.EditText;
import com.rey.material.widget.Slider;
import com.rey.material.widget.Spinner;
import com.sos.saveourstudents.supportclasses.Validations;

import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.LinkedList;
import java.util.List;

public class SettingsActivity extends AppCompatActivity implements View.OnClickListener {

    private EditText passwordEditText;
    private EditText passwordConfirmEditText;
    private Toolbar toolbar;
    private SharedPreferences sharedPref;
    private int currdistance;

    private Slider distanceSlider;
    private Button emailButton;
    private Button passwordButton;
    private Spinner coverSpinner;
    private TextView currDistanceDisplay;

    /**
     * Begin setting up ViewGroups' member variables and activity toolbar
     * @param savedInstanceState The bundle passed on creation, unused
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        if(!Singleton.hasBeenInitialized()){
            Singleton.initialize(this);
        }

        toolbar = (Toolbar) findViewById(R.id.settings_toolbar);
        toolbar.setTitle(R.string.app_name);
        setSupportActionBar(toolbar);
        toolbar.setNavigationIcon(R.drawable.ic_arrow_back_white_24dp);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });


        coverSpinner = (Spinner) findViewById(R.id.cover_spinner);
        distanceSlider = (Slider) findViewById(R.id.distance_slider);
        currDistanceDisplay = (TextView) findViewById(R.id.current_distance);
        emailButton = (Button) findViewById(R.id.change_email);
        passwordButton = (Button) findViewById(R.id.change_password);

        sharedPref = getSharedPreferences(
                getString(R.string.preference_file_key), Context.MODE_PRIVATE);


        String[] items = new String[7];
        for(int i = 0; i < items.length; i++)
            items[i] = "Cover " + String.valueOf(i + 1);


        ArrayAdapter<String> adapter = new CustomSpinnerAdapter(this, items);

        coverSpinner.setAdapter(adapter);
        coverSpinner.setOnItemSelectedListener(new Spinner.OnItemSelectedListener() {


            public void onItemSelected(Spinner spinner, View view, int position, long l) {
                SharedPreferences.Editor editor = sharedPref.edit();
                editor.putInt("cover_photo", getResources().obtainTypedArray(R.array.cover_imgs).getResourceId(position, R.drawable.materialwallpaperdefault));
                editor.apply();
                System.out.println("Cover photo set to: " + getResources().obtainTypedArray(R.array.cover_imgs).getResourceId(position, R.drawable.materialwallpaperdefault));
            }

        });




        currdistance = sharedPref.getInt("distance", 1);
        currDistanceDisplay.setText(currdistance + "");
        distanceSlider.setPosition(currdistance, true);
        distanceSlider.setValue(currdistance, true);

        distanceSlider.setOnPositionChangeListener(new Slider.OnPositionChangeListener() {
            @Override
            public void onPositionChanged(Slider slider, float oldPos, float newPos, int oldValue, int newValue) {
                currDistanceDisplay.setText(newValue + "");
                updateDistance(newValue);
            }

        });

        if(sharedPref.getString("provider", "").equalsIgnoreCase("sos")){
            emailButton.setOnClickListener(this);
            passwordButton.setOnClickListener(this);
        }
        else{
            emailButton.setVisibility(View.INVISIBLE);
            passwordButton.setVisibility(View.INVISIBLE);
        }

    }

    /**
     * Sets up OnClickListeners for the specified ViewGroups
     * @param v The view group in question
     */
    @Override
    public void onClick(View v) {

        if (v == emailButton) {

            //TODO STUPID LIBRARY
            /*
            new MaterialDialog.Builder(this)
                    .title("Change Email")
                    .input("Email", sharedPref.getString("email", ""), new MaterialDialog.InputCallback() {
                        @Override
                        public void onInput(MaterialDialog materialDialog, CharSequence charSequence) {

                            if (Validations.isValidEmail(charSequence)) {
                                updateEmail(charSequence.toString());
                            } else {
                                Toast.makeText(SettingsActivity.this, "Invalid Email", Toast.LENGTH_SHORT).show();
                            }
                        }
                    })
                    .negativeText("Cancel")
                    .show();
*/
        }
        else if (v == passwordButton) {
            showEditPasswordDialog();
        }
    }

    /**
     * Displays the edit password dialog after button onClick
     */
    private void showEditPasswordDialog() {

        //TODO STUPID LIBRARY
        /*
        MaterialDialog.Builder builder = new MaterialDialog.Builder(this)
                .callback(new MaterialDialog.ButtonCallback() {
                    @Override
                    public void onPositive(MaterialDialog dialog) {
                        passwordEditText = (EditText) dialog.findViewById(R.id.password_edittext);
                        passwordConfirmEditText = (EditText) dialog.findViewById(R.id.confirm_password_edittext);
                        if(areValidPasswords(passwordEditText.getText().toString(), passwordConfirmEditText.getText().toString())){
                            updatePassword(passwordEditText.getText().toString());
                        }
                        else
                            Toast.makeText(SettingsActivity.this, "Invalid password", Toast.LENGTH_SHORT).show();
                        super.onPositive(dialog);
                    }

                });

        builder.title("Change Password")
                .positiveText("Done")
                .negativeText("Cancel")
                .customView(R.layout.layout_dialog_custom, false)
                .show();
          */

    }

    /**
     * Verifies that the two entered passwords are equal
     * @param incomingPass1 The first password
     * @param incomingPass2 The second password
     * @return True if equal, false otherwise
     */
    private boolean areValidPasswords(String incomingPass1, String incomingPass2) {

        int result = Validations.areValidPasswords(incomingPass1, incomingPass2);

        if (result == Validations.INCORRECT_LENGTH_TOP) {
            Toast.makeText(this, "Invalid Password Length", Toast.LENGTH_SHORT).show();
            return false;
        }
        else if (result == Validations.INCORRECT_LENGTH_BOT) {
            Toast.makeText(this, "Invalid Password Length", Toast.LENGTH_SHORT).show();
            return false;
        }
        else if (result == Validations.REPEAT_NOT_SAME) {
            Toast.makeText(this, "Passwords Do Not Match", Toast.LENGTH_SHORT).show();
            return false;
        }

        return true;
    }

    /**
     * Updates the password of the user on the server
     * @param newPassword The new password entered
     */
    private void updatePassword(final String newPassword){


        List<NameValuePair> params = new LinkedList<>();
        params.add(new BasicNameValuePair("userId", sharedPref.getString("user_id", "")));
        params.add(new BasicNameValuePair("password", Validations.get_SHA_1_SecurePassword(newPassword)));

        String paramString = URLEncodedUtils.format(params, "utf-8");
        String url = "http://54.200.33.91:8080/com.mysql.services/rest/serviceclass/resetPassword?"+paramString;

        JsonObjectRequest jsObjRequest = new JsonObjectRequest(Request.Method.GET, url,
                (JSONObject)null,
                new Response.Listener<JSONObject>(){

                    @Override
                    public void onResponse(JSONObject response) {
                        try {

                            JSONObject result = new JSONObject(response.toString());
                            if(result.getString("success").equalsIgnoreCase("1")){
                                Toast.makeText(SettingsActivity.this, "Your password has been updated!", Toast.LENGTH_SHORT).show();

                            }
                            else{
                                Toast.makeText(SettingsActivity.this, "Error updating your password!", Toast.LENGTH_SHORT).show();
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
     * Updates the email of the user on the server
     * @param newEmail The newly entered email
     */
    private void updateEmail(final String newEmail){

        List<NameValuePair> params = new LinkedList<>();
        params.add(new BasicNameValuePair("email", newEmail));
        params.add(new BasicNameValuePair("userId", sharedPref.getString("user_id", "")));

        String paramString = URLEncodedUtils.format(params, "utf-8");
        String url = "http://54.200.33.91:8080/com.mysql.services/rest/serviceclass/resetEmail?"+paramString;

        JsonObjectRequest jsObjRequest = new JsonObjectRequest(Request.Method.GET, url,
                (JSONObject)null,
                new Response.Listener<JSONObject>(){

                    @Override
                    public void onResponse(JSONObject response) {
                        try {

                            JSONObject result = new JSONObject(response.toString());
                            if(result.getString("success").equalsIgnoreCase("1")){

                                SharedPreferences.Editor editor = sharedPref.edit();
                                editor.putString("email", newEmail);
                                editor.commit();
                                Toast.makeText(SettingsActivity.this, "Your email has been updated!", Toast.LENGTH_SHORT).show();
                            }
                            else{
                                Toast.makeText(SettingsActivity.this, "Error updating your email!", Toast.LENGTH_SHORT).show();
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
     * Updates the distance preferences of the user
     * @param distance The new distance
     */
    private void updateDistance(int distance){
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putInt("distance", distance);
        editor.commit();
    }

    /**
     * Displays a spinner for changing your cover image
     */
    public class CustomSpinnerAdapter extends ArrayAdapter<String>{
        final TypedArray imgs = getResources().obtainTypedArray(R.array.cover_imgs);

        public CustomSpinnerAdapter(Context context, String[] objects) {
            super(context, R.layout.spinner_cover_item, objects);
        }

        @Override
        public View getDropDownView(int position, View convertView,ViewGroup parent) {
            return getCustomView(position, parent);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            return getCustomView(position, parent);
        }

        public View getCustomView(int position, ViewGroup parent) {

            LayoutInflater inflater=getLayoutInflater();
            View row = inflater.inflate(R.layout.spinner_cover_item, parent, false);

            ImageView icon = (ImageView)row.findViewById(R.id.cover_image);
            icon.setImageResource(imgs.getResourceId(position, 0));

            return row;
        }
    }







}


