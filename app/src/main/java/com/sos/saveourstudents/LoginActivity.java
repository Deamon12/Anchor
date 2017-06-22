package com.sos.saveourstudents;

import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.FacebookSdk;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.facebook.Profile;
import com.facebook.ProfileTracker;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.google.android.gms.plus.Plus;
import com.google.android.gms.plus.model.people.Person;
import com.sos.saveourstudents.supportclasses.Validations;

import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public class LoginActivity extends AppCompatActivity implements View.OnClickListener, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    //G+
    private static final int RC_SIGN_IN = 0;
    // Logcat tag
    private static final String TAG = "LoginActivity";


    // Google client to interact with Google API
    private GoogleApiClient mGoogleApiClient;
    private boolean mIntentInProgress;
    private ConnectionResult mConnectionResult;
    private ImageView googleSignin;


    //FB
    CallbackManager callbackManager;
    private ProfileTracker profileTracker;
    LoginManager loginManager;
    ImageView fbLogin;
    String facebookEmail = null;
    String userImageUrl = "";

    //GCM
    public static final String PROPERTY_REG_ID = "registration_id";
    private static final String PROPERTY_APP_VERSION = "appVersion";
    private final static int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;
    String SENDER_ID = "862374215545"; //Unique identifier for our project
    GoogleCloudMessaging gcm;
    String regid;

    boolean isLogging = false;

    Toast prompt;
    Context appContext;
    TextView logoLabel;
    TextView forgotLoginBtn, signupBtn;
    com.rey.material.widget.EditText usernameField, passwordField;
    Button loginBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FacebookSdk.sdkInitialize(getApplicationContext());
        callbackManager = CallbackManager.Factory.create();
        loginManager = LoginManager.getInstance();

        setContentView(R.layout.activity_login);

        if (!Singleton.hasBeenInitialized()) {
            Singleton.initialize(this);
        }

        //Attempt to find deviceID for GCM notifications
        registerInBackground();

        forgotLoginBtn = (TextView) findViewById(R.id.forgot_login_btn);
        forgotLoginBtn.setOnClickListener(this);
        signupBtn = (TextView) findViewById(R.id.signup_btn);
        signupBtn.setOnClickListener(this);
        usernameField = (com.rey.material.widget.EditText) findViewById(R.id.username_textfield);
        passwordField = (com.rey.material.widget.EditText) findViewById(R.id.password_textfield);
        loginBtn = (Button) findViewById(R.id.login_btn);
        loginBtn.setOnClickListener(this);
        appContext = getApplicationContext();

        googleSignin = (ImageView) findViewById(R.id.google_login_btn);
        googleSignin.setOnClickListener(this);
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this).addApi(Plus.API)
                .addScope(Plus.SCOPE_PLUS_LOGIN).build();

        logoutAll();

        /**
         * This is the result from selecting facebook login button
         */
        loginManager.registerCallback(callbackManager, new FacebookCallback<LoginResult>() {
            @Override
            public void onSuccess(LoginResult loginResult) {

                GraphRequest request = GraphRequest.newMeRequest(
                        loginResult.getAccessToken(),
                        new GraphRequest.GraphJSONObjectCallback() {
                            @Override
                            public void onCompleted(JSONObject object, GraphResponse response) {

                                try {

                                    Profile profile = Profile.getCurrentProfile();

                                    if(profile != null) {
                                        userImageUrl = profile.getProfilePictureUri(100, 100).toString();
                                        facebookEmail = object.getString("email");

                                        createSOSUser("facebook",
                                                profile.getFirstName(),
                                                profile.getLastName(),
                                                profile.getId(),
                                                facebookEmail);
                                    }

                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                            }
                        });
                Bundle parameters = new Bundle();
                parameters.putString("fields", "id,name,email");
                request.setParameters(parameters);
                request.executeAsync();
            }

            @Override
            public void onCancel() {
            }

            @Override
            public void onError(FacebookException exception) {
            }
        });


/**
 * This tracks changes to facebook logged in status
 */
        profileTracker = new ProfileTracker() {
            @Override
            protected void onCurrentProfileChanged(Profile oldProfile, Profile currentProfile) {

            }
        };


        profileTracker.startTracking();
        fbLogin = (ImageView) findViewById(R.id.facebook_login_btn);
        fbLogin.setOnClickListener(this);


        //GCM
        if (checkPlayServices()) {
            gcm = GoogleCloudMessaging.getInstance(this);
            regid = getRegistrationId(this);
            if (regid.isEmpty()) {
                registerInBackground();
            } else {
                System.out.println("Device registration ID: " + regid);
            }
        } else {
            Log.i(TAG, "No valid Google Play Services APK found.");
        }

    }

    //Clear shared prefs, logout of all accounts
    private void logoutAll() {
        doFacebookLogout();
        signOutFromGplus();
        getSharedPreferences(getString(R.string.preference_file_key), Context.MODE_PRIVATE).edit().clear().commit();
    }

    /**
     * Gets the current registration ID for application on GCM service.
     * <p/>
     * If result is empty, the app needs to register.
     * @return registration ID, or empty string if there is no existing
     * registration ID.
     */
    private String getRegistrationId(Context context) {
        SharedPreferences sharedPref = getSharedPreferences(
                getString(R.string.preference_file_key), Context.MODE_PRIVATE);
        String registrationId = sharedPref.getString(PROPERTY_REG_ID, "");
        if (registrationId.isEmpty()) {
            Log.i(TAG, "Registration not found.");
            return "";
        }
        // Check if app was updated; if so, it must clear the registration ID
        // since the existing registration ID is not guaranteed to work with
        // the new app version.
        int registeredVersion = sharedPref.getInt(PROPERTY_APP_VERSION, Integer.MIN_VALUE);
        int currentVersion = getAppVersion(context);
        if (registeredVersion != currentVersion) {
            Log.i(TAG, "App version changed.");
            return "";
        }
        return registrationId;
    }


    /**
     * @return Application's version code from the {@code PackageManager}.
     */
    private static int getAppVersion(Context context) {
        try {
            PackageInfo packageInfo = context.getPackageManager()
                    .getPackageInfo(context.getPackageName(), 0);
            return packageInfo.versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            // should never happen
            throw new RuntimeException("Could not get package name: " + e);
        }
    }


    /**
     * Registers the application with GCM servers asynchronously.
     * <p/>
     * Stores the registration ID and app versionCode in the application's
     * shared preferences.
     */
    private void registerInBackground() {

        new AsyncTask() {

            @Override
            protected String doInBackground(Object[] params) {
                String msg = "";
                try {
                    if (gcm == null) {
                        gcm = GoogleCloudMessaging.getInstance(LoginActivity.this);
                    }
                    regid = gcm.register(SENDER_ID);
                    msg = "Device registered, registration ID=" + regid;
                    //System.out.println("Device registered, registration ID=" + regid);
                    SharedPreferences sharedPref = getSharedPreferences(
                            getString(R.string.preference_file_key), Context.MODE_PRIVATE);
                    SharedPreferences.Editor editor = sharedPref.edit();
                    editor.putString("PROPERTY_REG_ID", regid);
                    editor.commit();

                } catch (IOException ex) {
                    msg = "Error :" + ex.getMessage();
                    // If there is an error, don't just keep trying to register.
                    // Require the user to click a button again, or perform
                    // exponential back-off.
                }
                return msg;
            }


            protected void onPostExecute(String msg) {
            }

        }.execute(null, null, null);
    }


    private boolean checkPlayServices() {
        int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        if (resultCode != ConnectionResult.SUCCESS) {
            if (GooglePlayServicesUtil.isUserRecoverableError(resultCode)) {
                GooglePlayServicesUtil.getErrorDialog(resultCode, this,
                        PLAY_SERVICES_RESOLUTION_REQUEST).show();
            } else {
                Log.i(TAG, "This device is not supported.");
                finish();
            }
            return false;
        }
        return true;
    }


    @Override
    protected void onResume() {
        super.onResume();
        checkPlayServices();
    }

    protected void onStart() {
        super.onStart();
    }

    protected void onStop() {
        super.onStop();
        if (mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        //G+ response
        if (requestCode == RC_SIGN_IN) {
            if (resultCode != RESULT_OK) {
            }

            mIntentInProgress = false;

            if (!mGoogleApiClient.isConnecting()) {
                mGoogleApiClient.connect();
            }
        } else {   //FB Callback
            callbackManager.onActivityResult(requestCode, resultCode, data);
        }


    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.

        if (item.getItemId() == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onClick(View v) {
        if (v == fbLogin) {
            doFacebookLogin();

        } else if (v == googleSignin) {
            mGoogleApiClient.connect();
            signOutFromGplus(); //cleanup call

            if (mGoogleApiClient.isConnected()) {
                signInWithGplus();
            }

        } else if (v == loginBtn) {
            boolean error = false;
            if (usernameField.getText().toString().isEmpty()) {
                usernameField.setError("");
                error = true;
            }
            if (passwordField.getText().toString().isEmpty()) {
                passwordField.setError("");
                error = true;
            }

            if(!error) {
                usernameField.clearError();
                passwordField.clearError();
                doSOSLogin("SOS", usernameField.getText().toString(), passwordField.getText().toString());
            }

        } else if (v == signupBtn) {
            startActivity(new Intent(this, SignupActivity.class));

        } else if (v == forgotLoginBtn) {
            startActivity(new Intent(this, ForgotLoginActivity.class));
        }

    }


    public void doFacebookLogin() {//TODO Needs adjusting serverside, Duplicate emails present a problem.
        loginManager.logInWithReadPermissions(this, Arrays.asList("public_profile", "email"));
    }

    public void doFacebookLogout() {
        loginManager.logOut();
    }

    @Override
    public void onConnected(Bundle arg0) {

        //Userinfo
        if (Plus.PeopleApi.getCurrentPerson(mGoogleApiClient) != null) {
            Person currentPerson = Plus.PeopleApi
                    .getCurrentPerson(mGoogleApiClient);

            userImageUrl = currentPerson.getImage().getUrl().replace("sz=50", "sz=200");
            String firstName = currentPerson.getName().getGivenName();
            String lastName = currentPerson.getName().getFamilyName();
            String userId = currentPerson.getId();
            String email = Plus.AccountApi.getAccountName(mGoogleApiClient);

            createSOSUser("google", firstName, lastName, userId, email);

        } else {
            Toast.makeText(getApplicationContext(),
                    "Person information is null", Toast.LENGTH_LONG).show();
        }

    }

    @Override
    public void onConnectionSuspended(int arg0) {
        mGoogleApiClient.connect();
    }

    public void onConnectionFailed(ConnectionResult result) {
        if (!result.hasResolution()) {
            GooglePlayServicesUtil.getErrorDialog(result.getErrorCode(), this,
                    0).show();
            return;
        }

        if (!mIntentInProgress) {
            // Store the ConnectionResult for later usage
            mConnectionResult = result;
            resolveSignInError();

        }
    }

    private void signInWithGplus() {
        if (!mGoogleApiClient.isConnecting()) {
            resolveSignInError();
        }
    }

    private void signOutFromGplus() {
        if (mGoogleApiClient.isConnected()) {
            Plus.AccountApi.clearDefaultAccount(mGoogleApiClient);
            mGoogleApiClient.disconnect();
            mGoogleApiClient.connect();
        }
    }

    /**
     * Method to resolve any signin errors
     */
    private void resolveSignInError() {
        if (mConnectionResult.hasResolution()) {
            try {
                mIntentInProgress = true;
                mConnectionResult.startResolutionForResult(this, RC_SIGN_IN);
            } catch (IntentSender.SendIntentException e) {
                mIntentInProgress = false;
                mGoogleApiClient.connect();
            }
        }
    }


    private void createSOSUser(final String provider, String firstName, String lastName,
                               final String password, final String email) {

        String deviceId = "";
        if (regid != null)
            deviceId = regid;

        List<NameValuePair> params = new LinkedList<NameValuePair>();
        params.add(new BasicNameValuePair("firstName", firstName));
        params.add(new BasicNameValuePair("lastName", lastName));
        params.add(new BasicNameValuePair("password", Validations.get_SHA_1_SecurePassword(password)));
        params.add(new BasicNameValuePair("email", email));
        params.add(new BasicNameValuePair("image", userImageUrl));
        params.add(new BasicNameValuePair("deviceId", deviceId));

        String paramString = URLEncodedUtils.format(params, "utf-8");//.replace("+", "%20");
        String url = "http://54.200.33.91:8080/com.mysql.services/rest/serviceclass/createUser?"+paramString;

        JsonObjectRequest jsonObjReq = new JsonObjectRequest(Request.Method.GET, url,
                (JSONObject) null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                try {

                    //If success = 1, result = success string
                    //If success = 0, result = error string
                    if (response.getString("success").equalsIgnoreCase("1")) {
                    } else {
                        if (response.getString("result").substring(0, response.getString("result").indexOf(" ")).equalsIgnoreCase("Duplicate")) {
                            isLogging = false;
                        }
                    }

                    doSOSLogin(provider, email, password);

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }, new Response.ErrorListener() {

            @Override
            public void onErrorResponse(VolleyError error) {
                System.out.println("Error: " + error.toString());
                isLogging = false;
            }

        });

        Singleton.getInstance().addToRequestQueue(jsonObjReq);

    }


    /**
     * Volley call to check users credentials against the server.
     * Depending on result, continue or halt user. On success close this activity and transfer user to
     * MainActivity, else prompt incorrect login
     * @param provider String - SOS, Facebook, or Google
     * @param email String - FB required a separate call to get this info
     * @param password String - unencrypted
     */
    private void doSOSLogin(final String provider, String email, String password) {

        String url = "http://54.200.33.91:8080/com.mysql.services/rest/serviceclass/doLogin?" +
                "email=" + email +
                "&password=" + Validations.get_SHA_1_SecurePassword(password);

        JsonObjectRequest jsonObjReq = new JsonObjectRequest(Request.Method.GET, url,
                (JSONObject) null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {

                try {

                    if (response.getString("success").equalsIgnoreCase("1") && response.getInt("expectResults") >= 1){
                        SharedPreferences sharedPref = getSharedPreferences(
                                getString(R.string.preference_file_key), Context.MODE_PRIVATE);
                        SharedPreferences.Editor editor = sharedPref.edit();
                        editor.putString("first_name", response.getJSONObject("result").getJSONArray("myArrayList").getJSONObject(0).getJSONObject("map").getString("first_name"));
                        editor.putString("last_name", response.getJSONObject("result").getJSONArray("myArrayList").getJSONObject(0).getJSONObject("map").getString("last_name"));
                        editor.putString("email", response.getJSONObject("result").getJSONArray("myArrayList").getJSONObject(0).getJSONObject("map").getString("email"));

                        if(response.getJSONObject("result").getJSONArray("myArrayList").getJSONObject(0).getJSONObject("map").has("image"))
                            editor.putString("image", response.getJSONObject("result").getJSONArray("myArrayList").getJSONObject(0).getJSONObject("map").getString("image"));

                        editor.putString("user_id", response.getJSONObject("result").getJSONArray("myArrayList").getJSONObject(0).getJSONObject("map").getString("user_id"));
                        editor.putString("provider", provider);
                        editor.putInt("distance", 10);
                        editor.commit();

                        Intent mainActivity = new Intent(LoginActivity.this, MainActivity.class);
                        startActivity(mainActivity);
                        finish();
                    } else if (response.getString("success").equalsIgnoreCase("1") && response.getString("expectResults").equalsIgnoreCase("0")
                            && provider.equalsIgnoreCase("sos")) {
                        prompt = Toast.makeText(appContext, "Invalid login!", Toast.LENGTH_SHORT);
                        prompt.show();
                    }else{
                    }


                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }, new Response.ErrorListener() {

            @Override
            public void onErrorResponse(VolleyError error) {
                System.out.println("Error: " + error.toString());
            }
        });

        Singleton.getInstance().addToRequestQueue(jsonObjReq);

    }


}