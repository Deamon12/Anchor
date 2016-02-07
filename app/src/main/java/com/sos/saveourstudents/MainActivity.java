package com.sos.saveourstudents;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.TranslateAnimation;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.rey.material.widget.SnackBar;
import com.sos.saveourstudents.supportclasses.NavDrawerAdapter;
import com.sos.saveourstudents.supportclasses.RecyclerItemClickListener;
import com.sos.saveourstudents.supportclasses.SlidingTabLayout;

import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private final int SETTINGS_ACTIVITY = 363;
    private final int PROFILE_ACTIVITY = 505;
    private final int QUESTION_ACTIVITY = 3334;
    private boolean fabShowing = true;

    private FabBroadcastReciever fabBroadcastReciever;
    private ViewPager mViewPager;
    private SlidingTabLayout mTabs;

    private ViewPagerAdapter viewPagerAdapter;
    private RecyclerView mRecyclerView;                           // Declaring RecyclerView
    private RecyclerView.Adapter mAdapter;                        // Declaring Adapter For Recycler View
    private DrawerLayout mDrawer;                                 // Declaring DrawerLayout

    private ActionBarDrawerToggle mDrawerToggle;

    private com.rey.material.widget.FloatingActionButton fab;

    private SnackBar mSnackBar;

    int ICONS[] = {R.drawable.ic_person_black_24dp,R.drawable.ic_exit_to_app_black_24dp, R.drawable.ic_settings_black_24dp};
    String TITLES[] = {"Profile","Logout","Settings"};

    private SharedPreferences sharedPref;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        if(!Singleton.hasBeenInitialized()){
            Singleton.initialize(this);
        }

        fab = (com.rey.material.widget.FloatingActionButton) findViewById(R.id.fab_image);
        hideFab();
        sharedPref = getSharedPreferences(
                getString(R.string.preference_file_key), Context.MODE_PRIVATE);

        if(!sharedPref.contains("first_name")){//Your not logged in. Go to login activity
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        }

        Toolbar toolbar = (Toolbar) findViewById(R.id.the_toolbar);
        if (toolbar != null) {
            toolbar.setTitle(R.string.app_name);
            setSupportActionBar(toolbar);
        }

        fabBroadcastReciever = new FabBroadcastReciever();


        mSnackBar = (SnackBar)findViewById(R.id.main_sn);

        mSnackBar.applyStyle(R.style.SnackBarSingleLine)
                .text("Connection timed out")
                .actionText("RETRY")
                .duration(0)
                .actionClickListener(new SnackBar.OnActionClickListener() {
                    @Override
                    public void onActionClick(SnackBar snackBar, int i) {
                        hideSnackbar();
                    }
                });


        buildFab();




        mViewPager = (ViewPager) this.findViewById(R.id.pager);
        viewPagerAdapter = new ViewPagerAdapter(this.getSupportFragmentManager());
        mViewPager.setAdapter(viewPagerAdapter);
        mTabs = (SlidingTabLayout) this.findViewById(R.id.tabs);
        mTabs.setDistributeEvenly(true);

        mTabs.setViewPager(mViewPager);

        mTabs.setCustomTabColorizer(new SlidingTabLayout.TabColorizer() {
            @Override
            public int getIndicatorColor(int position) {
                return MainActivity.this.getResources().getColor(R.color.primary_light);
            }
        });


        mRecyclerView = (RecyclerView) findViewById(R.id.RecyclerView); // Assigning the RecyclerView Object to the xml View

        mRecyclerView.setHasFixedSize(true);                            // Letting the system know we wont change the size of the list

        updateNavDrawer();

        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mRecyclerView.addOnItemTouchListener(
                new RecyclerItemClickListener(this, new RecyclerItemClickListener.OnItemClickListener() {
                    //Nav drawer listener
                    @Override public void onItemClick(View view, int position) {

                        if(position == 1){
                            Intent intent = new Intent(MainActivity.this, ProfileActivity.class);
                            intent.putExtra("userId", sharedPref.getString("user_id", ""));
                            startActivityForResult(intent, PROFILE_ACTIVITY);

                        }
                        else if(position == 2){
                            Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                            startActivity(intent);
                            finish();
                        }
                        else if(position == 3){
                            Intent intent = new Intent(MainActivity.this,SettingsActivity.class);
                            startActivityForResult(intent, SETTINGS_ACTIVITY);
                        }

                    }
                })
        );

        mDrawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawerToggle = new ActionBarDrawerToggle(this, mDrawer, toolbar,R.string.openDrawer,R.string.closeDrawer){

            @Override
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
            }

            @Override
            public void onDrawerClosed(View drawerView) {
                super.onDrawerClosed(drawerView);
            }

        };
        mDrawer.setDrawerListener(mDrawerToggle);
        mDrawerToggle.syncState();


    }

    @Override
    protected void onResume() {
        //Custom BC listener, if user gets removed or accepted to group
        IntentFilter iff = new IntentFilter();
        iff.addAction("com.sos.saveourstudents.CUSTOM_INTENT");
        registerReceiver(fabBroadcastReciever, iff);
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();

        if(mDrawer != null)
            mDrawer.closeDrawers();

        unregisterReceiver(fabBroadcastReciever);

    }


    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        // Sync the toggle state after onRestoreInstanceState has occurred.
        mDrawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        //mDrawerToggle.onConfigurationChanged(newConfig);
    }

    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && mViewPager.getCurrentItem() != 0) {
            mViewPager.setCurrentItem(0);
            return false;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        if (item.getItemId() == R.id.action_filter) {

            Set<String> filterList = new HashSet<String>(sharedPref.getStringSet("filter_list", new HashSet<String>()));
            ArrayList<String> myList = new ArrayList<String>();
            myList.addAll(filterList);


            TagDialogFragment newFragment = TagDialogFragment.newInstance(0, myList);

            newFragment.show(getSupportFragmentManager(), "dialog");

        }

        return super.onOptionsItemSelected(item);
    }


    public void hideFab(){
        if(fabShowing){

            TranslateAnimation anim = new TranslateAnimation(0, 0, 0, 300);
            anim.setDuration(200);
            anim.setFillAfter(true);
            fab.startAnimation(anim);
            fab.setVisibility(View.GONE);
            fabShowing = false;
        }
    }

    public void showFab(){

        if(!fabShowing && mSnackBar.getState() != 1){
            TranslateAnimation anim = new TranslateAnimation( 0, 0 , 300, 0 );
            fab.setVisibility(View.VISIBLE);
            anim.setDuration(200);
            anim.setFillAfter( true );
            fab.startAnimation(anim);
            fabShowing = true;
        }


    }

    //TODO: too many updates....adjust this. Disabled for now
    public void updateFragments(){

        if(((FeedFragment) viewPagerAdapter.getItem(0)).mContext != null)
            ((FeedFragment) viewPagerAdapter.getItem(0)).buildGoogleApiClient();

        if(((MapFragment) viewPagerAdapter.getItem(1)).getActivity() != null){
            ((MapFragment) viewPagerAdapter.getItem(1)).buildGoogleApiClient();
        }

    }


    @Override
    public void onClick(View v) {
        System.out.println("clicked tabs: ");
    }


    public void buildFab(){
        hideFab();
        getQuestionActiveStatus();
    }


    private void getQuestionActiveStatus(){

        List<NameValuePair> params = new LinkedList<NameValuePair>();
        params.add(new BasicNameValuePair("userId", sharedPref.getString("user_id", "")));
        String paramString = URLEncodedUtils.format(params, "utf-8");
        String url = "http://54.200.33.91:8080/com.mysql.services/rest/serviceclass/hasQuestion?"+paramString;
        //System.out.println("url: " + url);

        JsonObjectRequest jsObjRequest = new JsonObjectRequest(Request.Method.GET, url,
                (JSONObject)null,
                new Response.Listener<JSONObject>(){

                    @Override
                    public void onResponse(JSONObject response) {
                        try {

                            final JSONObject result = new JSONObject(response.toString());
                            //System.out.println("has questions result "+result);
                            if(!result.getString("success").equalsIgnoreCase("1")){
                                //Error...
                            }
                            else{

                                if(result.getString("expectResults").equalsIgnoreCase("1")){
                                    final String questionId = result.getJSONObject("result").getJSONArray("myArrayList").getJSONObject(0).getJSONObject("map").getString("question_id");
                                    fab.setIcon(getResources().getDrawable(R.drawable.ic_create_white_24dp), false);
                                    fab.setOnClickListener(new View.OnClickListener() {
                                        @Override
                                        public void onClick(View v) {
                                            Intent mIntent = new Intent(MainActivity.this, ViewQuestionActivity.class);
                                            mIntent.putExtra("questionId", questionId);
                                            startActivityForResult(mIntent, QUESTION_ACTIVITY);
                                        }
                                    });
                                    showFab();
                                }
                                //Is in a group?
                                else{
                                    getGroupActiveStatus();
                                }

                            }

                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }, new Response.ErrorListener() {

            @Override
            public void onErrorResponse(VolleyError error) {
                System.out.println("Error with connection or url: " + error.toString());
                showSnackbar();
            }

        });

        Singleton.getInstance().addToRequestQueue(jsObjRequest);


    }
    private void getGroupActiveStatus(){

        List<NameValuePair> params = new LinkedList<NameValuePair>();
        params.add(new BasicNameValuePair("userId", sharedPref.getString("user_id", "")));

        String paramString = URLEncodedUtils.format(params, "utf-8");
        String url = "http://54.200.33.91:8080/com.mysql.services/rest/serviceclass/inGroup?"+paramString;

        //System.out.println("getGroupActiveStatus url: " + url);

        JsonObjectRequest jsObjRequest = new JsonObjectRequest(Request.Method.GET, url,
                (JSONObject)null,
                new Response.Listener<JSONObject>(){

                    @Override
                    public void onResponse(JSONObject response) {
                        try {

                            final JSONObject result = new JSONObject(response.toString());
                            //System.out.println("getGroupActiveStatus result "+result);

                            if(!result.getString("success").equalsIgnoreCase("1")){
                                //System.out.println("Error getting active group status: "+result);
                            }
                            else{

                                if(result.getString("expectResults").equalsIgnoreCase("1")){
                                    final String questionId = result.getJSONObject("result").getJSONArray("myArrayList").getJSONObject(0).getJSONObject("map").getString("question_id");
                                    fab.setIcon(getResources().getDrawable(R.drawable.ic_group_white_24dp), false);
                                    fab.setOnClickListener(new View.OnClickListener() {
                                        @Override
                                        public void onClick(View v) {
                                            Intent mIntent = new Intent(MainActivity.this, ViewQuestionActivity.class);
                                            mIntent.putExtra("questionId", questionId);
                                            startActivityForResult(mIntent, QUESTION_ACTIVITY);
                                        }
                                    });
                                    showFab();
                                }
                                //Add question
                                else{
                                    fab.setIcon(getResources().getDrawable(R.drawable.ic_add_white_24dp), false);
                                    fab.setOnClickListener(new View.OnClickListener() {
                                        @Override
                                        public void onClick(View v) {
                                            Intent mIntent = new Intent(MainActivity.this, CreateQuestionActivity.class); //TODO rename, dialogize
                                            mIntent.putExtra("questionId", "");
                                            startActivityForResult(mIntent, QUESTION_ACTIVITY);
                                        }
                                    });
                                    showFab();
                                }


                            }

                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                    }
                }, new Response.ErrorListener() {

            @Override
            public void onErrorResponse(VolleyError error) {
                //Toast.makeText(MainActivity.this, "Connection Error", Toast.LENGTH_SHORT).show();
                showSnackbar();
            }

        });

        Singleton.getInstance().addToRequestQueue(jsObjRequest);
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        // Check which request we're responding to
        if (requestCode == QUESTION_ACTIVITY) {
            //System.out.println("Returned from edit Question");
            buildFab();
            updateFragments();
            if (resultCode == RESULT_OK) {
                //System.out.println("Returned from edit Question ok");
            }
        }

        else if(requestCode == PROFILE_ACTIVITY){
            updateNavDrawer();
            updateFragments();
        }
        else if(requestCode == SETTINGS_ACTIVITY){
            updateNavDrawer();
            updateFragments();
        }

        else {
            super.onActivityResult(requestCode, resultCode, data);
        }

    }


    private void updateNavDrawer(){

        String name = sharedPref.getString("first_name", "") + " "+ sharedPref.getString("last_name", "");
        mAdapter = new NavDrawerAdapter(TITLES, ICONS, name,
                sharedPref.getString("email", "email"),
                sharedPref.getString("image", "image"),
                sharedPref.getInt("cover_photo", R.drawable.materialwallpaperdefault));

        mRecyclerView.setAdapter(mAdapter);

    }


    public class FabBroadcastReciever extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            //System.out.println("Recieved custom broadcast in main ACtivity");
            buildFab();
        }
    }

    protected void showSnackbar(){
        mSnackBar.show();
        fab.setVisibility(View.GONE);
        hideFab();
    }

    protected void hideSnackbar(){
        mSnackBar.dismiss();
        fab.setVisibility(View.VISIBLE);
        buildFab();
        updateFragments();
    }



}






/**
 * Handles all work that pertains to the 2 main fragments
 */
class ViewPagerAdapter extends FragmentPagerAdapter {

    public Fragment
            feedFragment,
            mapFragment;

    String[] tabNames = {"Feed", "Map"};


    public ViewPagerAdapter(FragmentManager fm) {
        super(fm);
        feedFragment = FeedFragment.newInstance();
        mapFragment = MapFragment.newInstance();
    }

    @Override
    public CharSequence getPageTitle(int position) {
        return tabNames[position];
    }

    @Override
    public Fragment getItem(int position) {
        if(position == 0){
            return feedFragment;
        }
        else{
            return mapFragment;
        }

    }

    @Override
    public int getCount() {
        return tabNames.length;
    }

    @Override
    public int getItemPosition(Object object) {
        return POSITION_NONE;
    }



}


