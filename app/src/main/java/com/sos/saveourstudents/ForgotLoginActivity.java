package com.sos.saveourstudents;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.rey.material.widget.EditText;

import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.LinkedList;
import java.util.List;

/**
 * Activity that handles password recovery. Single volley call that resets user password and sends a randomized
 * string to their given email address. The password can later be changed after they login.
 */
public class ForgotLoginActivity extends Activity implements View.OnClickListener {

    Button sendEmailBtn;
    EditText emailentry;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_login);

        if(!Singleton.hasBeenInitialized()){
            Singleton.initialize(this);
        }

        sendEmailBtn = (Button)findViewById(R.id.send_credentials_to_email_btn);
        emailentry = (EditText) findViewById(R.id.forgot_login_email_textfield);
        sendEmailBtn.setOnClickListener(this);

    }

    /**
     * Implemented View event listener
     * @param v
     */
    @Override
    public void onClick(View v) {
        if(v == sendEmailBtn) {
            if(emailentry.getText().toString().isEmpty()) {
                Log.d("debug","errmsg empty");
                emailentry.setError("");
            }else{

                emailentry.clearError();
                sendRecoveryEmail(emailentry.getText().toString());
            }
        }
    }


    /**
     * Notify server to send random recovery password to given email address.
     * @param email email address to send recovery password to
     */
    private void sendRecoveryEmail(final String email){

        List<NameValuePair> params = new LinkedList<NameValuePair>();
        params.add(new BasicNameValuePair("email", email));

        String paramString = URLEncodedUtils.format(params, "utf-8");
        String url = "http://54.200.33.91:8080/com.mysql.services/rest/serviceclass/forgotPassword?" + paramString;

        final JsonObjectRequest jsonObjReq = new JsonObjectRequest(Request.Method.GET, url,
                (JSONObject) null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                try {
                    if(response.getString("expectResults").equalsIgnoreCase("0")) {
                        Toast.makeText(ForgotLoginActivity.this, R.string.invalidEmail, Toast.LENGTH_SHORT).show();
                    } else if (response.getString("expectResults").equalsIgnoreCase("1")) {
                        //email exists
                        Toast.makeText(ForgotLoginActivity.this, R.string.emailSent, Toast.LENGTH_SHORT).show();
                        finish();
                        return;
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