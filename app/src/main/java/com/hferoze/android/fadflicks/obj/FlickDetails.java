package com.hferoze.android.fadflicks.obj;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.HashMap;

public class FlickDetails implements Parcelable {

    private String mRuntime, mGenres;
    private ArrayList<TrailerDetails> mTrailersArray;
    private HashMap<String, String> mReviewsMap;

    public FlickDetails() {
    }

    private FlickDetails(Parcel in) {
        mRuntime = in.readString();
        mGenres = in.readString();
    }

    public String getRuntime() {
        return mRuntime;
    }

    public String getGernes() {
        return mGenres;
    }

    public ArrayList<TrailerDetails> getTrailersArray() {
        return mTrailersArray;
    }

    public HashMap getReviewsMap() {
        return mReviewsMap;
    }

    public void setRuntime(String runtime) {
        this.mRuntime = runtime;
    }

    public void setGernes(String genres) {
        this.mGenres = genres;
    }

    public void setTrailers(ArrayList<TrailerDetails> trailersArray) {
        this.mTrailersArray = trailersArray;
    }

    public void setReviewsMap(HashMap<String, String> reviewsMap) {
        this.mReviewsMap = reviewsMap;
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel out, int flags) {
        out.writeString(mRuntime);
        out.writeString(mGenres);
    }

    public static final Parcelable.Creator<FlickDetails> CREATOR = new Parcelable.Creator<FlickDetails>() {
        public FlickDetails createFromParcel(Parcel in) {
            return new FlickDetails(in);
        }

        public FlickDetails[] newArray(int size) {
            return new FlickDetails[size];
        }
    };
}
