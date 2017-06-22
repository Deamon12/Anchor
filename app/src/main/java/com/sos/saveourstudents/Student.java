package com.sos.saveourstudents;

import android.os.Parcel;
import android.os.Parcelable;
import android.widget.ImageView;

/**
 * Encapsulates all student information
 */
public class Student implements Parcelable{
    private String firstName;
    private String lastName;
    private final int rating;
    private String school;
    private String major;
    private String description;
    private ImageView profilePicture;
    private String profilePictureUrl;

    /**
     * Sets up a Student object based on given parameters, self explanatory
     * @param firstName First name of the student
     * @param lastName Last name of the student
     * @param rating Current rating in the database
     * @param school Personal school
     * @param major Personal major
     * @param description Personal description
     * @param profilePictureUrl URL for the ImageViews set with the Student
     */
    public Student(String firstName, String lastName, int rating, String school, String major,
                   String description, String profilePictureUrl) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.rating = rating;
        this.school = school;
        this.major = major;
        this.description = description;
        this.profilePictureUrl = profilePictureUrl;
        this.profilePicture = null;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public int getRating() {
        return rating;
    }
    public String getSchool() {
        return school;
    }

    public void setSchool(String school) {
        this.school = school;
    }

    public String getMajor() {
        return major;
    }

    public void setMajor(String major) {
        this.major = major;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public ImageView getProfilePicture() {
        return profilePicture;
    }

    public void setProfilePicture(ImageView profilePicture) {
        this.profilePicture = profilePicture;
    }

    public String getProfilePictureUrl() {
        return profilePictureUrl;
    }

    public void setProfilePictureUrl(String profilePictureUrl) {
        this.profilePictureUrl = profilePictureUrl;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {

    }
}