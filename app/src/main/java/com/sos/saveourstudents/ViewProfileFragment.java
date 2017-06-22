package com.sos.saveourstudents;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageLoader;
import com.rey.material.widget.FloatingActionButton;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link com.sos.saveourstudents.ViewProfileFragment.OnEditButtonListener} interface
 * to handle interaction events.
 * Use the {@link ViewProfileFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class ViewProfileFragment extends Fragment {
    private static final String ARG_PARAM1 = "student";
    private static final String ARG_ISCURRENTUSER = "isCurrentUser";

    private Student mCurrStudent;
    private boolean mIsCurrentUser;

    private OnEditButtonListener mListener;
    private ImageView mProfileImage;
    private ImageView mCoverImageView;

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param currStudent The current logged in student
     * @return A new instance of fragment ViewProfileFragment.
     */
    public static ViewProfileFragment newInstance(Student currStudent, boolean isCurrentUser) {
        ViewProfileFragment fragment = new ViewProfileFragment();
        Bundle args = new Bundle();
        args.putParcelable(ARG_PARAM1, currStudent); // ARG_PARAM1 key now holds current Student
        args.putBoolean(ARG_ISCURRENTUSER, isCurrentUser);
        fragment.setArguments(args);
        return fragment;
    }

    /**
     * Empty constructor
     */
    public ViewProfileFragment() {
        // Required empty public constructor
    }

    /**
     * Grab the bundle saved with the newInstance() call
     * @param savedInstanceState The bundle of information regarding the student
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            // Grab the current student from our created Bundle
            mCurrStudent = getArguments().getParcelable(ARG_PARAM1);
            mIsCurrentUser = getArguments().getBoolean(ARG_ISCURRENTUSER);
        } else {
            Log.e("ViewProfile", "Error during transaction!");
        }
    }

    /**
     * On View creation, begin setting up the profile image and cover image
     * @param inflater The LayoutInflater of the activity
     * @param container The container of all ViewGroups in the activity
     * @param savedInstanceState The savedInstanceState of the app on creation
     * @return
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.fragment_view_profile, container, false);
        mProfileImage = (ImageView) rootView.findViewById(R.id.profile_image);
        mCoverImageView = (ImageView) rootView.findViewById(R.id.cover_image);
        return rootView;
    }

    /**
     * When the activity is created, begin updating the contents of the profile view
     * @param savedInstanceState The saved instance state of the activity
     */
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        updateCoverImage();
        updateCurrentStudentView();

        LinearLayout aboutMeContents = (LinearLayout) getActivity()
                .findViewById(R.id.profile_aboutMeContents);
        aboutMeContents.setDividerDrawable(getActivity().getResources()
                .getDrawable(R.drawable.abc_list_divider_mtrl_alpha));
        aboutMeContents.setShowDividers(LinearLayout.SHOW_DIVIDER_BEGINNING);
        FloatingActionButton mEditButton = (FloatingActionButton) getActivity().findViewById(R.id.profile_editButton);
        if (!mIsCurrentUser) {
            mEditButton.setVisibility(View.INVISIBLE);
            mEditButton.setClickable(false);
        } else {
            // Set the OnClickListener for the edit floating action button
            mEditButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mListener != null) {
                        mListener.onEditButton();
                    }
                }
            });

        }

    }

    /**
     * Update the View with the student's current values
     */
    private void updateCurrentStudentView() {
        ((TextView) getActivity().findViewById(R.id.profile_firstName))
                .setText(mCurrStudent.getFirstName());
        ((TextView) getActivity().findViewById(R.id.profile_lastName))
                .setText(mCurrStudent.getLastName());
        ((TextView) getActivity().findViewById(R.id.profile_myRating))
                .setText(((Integer) mCurrStudent.getRating()).toString());
        ((TextView) getActivity().findViewById(R.id.profile_mySchool))
                .setText(mCurrStudent.getSchool());
        ((TextView) getActivity().findViewById(R.id.profile_myMajor))
                .setText(mCurrStudent.getMajor());
        ((TextView) getActivity().findViewById(R.id.profile_myDescription))
                .setText(mCurrStudent.getDescription());

        if (mCurrStudent.getProfilePictureUrl() == null ||
                mCurrStudent.getProfilePictureUrl().isEmpty()) {
            mProfileImage.setImageResource(R.drawable.defaultprofile);
            mCurrStudent.setProfilePicture(mProfileImage);
        } else {
            getUserImage(mCurrStudent.getProfilePictureUrl(), mProfileImage);
        }
    }

    /**
     * Updates the current cover image of the student based on Settings
     */
    private void updateCoverImage(){
        if(mIsCurrentUser)
            mCoverImageView.setImageResource(getActivity().getSharedPreferences(
                    getString(R.string.preference_file_key), Context.MODE_PRIVATE)
                    .getInt("cover_photo", R.drawable.materialwallpaperdefault));
    }

    /**
     * Asserts that ViewProfileActivity has attached the listener for the FAB
     * @param activity ViewProfileActivity
     */
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (OnEditButtonListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnEditButtonListener");
        }
    }

    /**
     * On detach, remove the listener
     */
    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     */
    public interface OnEditButtonListener {
        void onEditButton();
    }

    /**
     * Grabs the current profile's image from the Singleton's ImageLoader
     * @param imageUrl The URL of the image
     * @param imageView The ImageView to retrieve the image from
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
                    mCurrStudent.setProfilePicture(imageView);
                } else {
                    // Default image...
                    imageView.setImageDrawable(getResources().getDrawable(R.drawable.defaultprofile));
                }
            }
        });

    }
}