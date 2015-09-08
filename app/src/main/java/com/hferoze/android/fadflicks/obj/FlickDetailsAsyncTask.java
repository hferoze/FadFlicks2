package com.hferoze.android.fadflicks.obj;

import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;

import com.hferoze.android.fadflicks.util.AppConstants;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;

public class FlickDetailsAsyncTask extends AsyncTask<String, Integer, FlickDetails> {

    private final String LOG_TAG = FlickDataFetchAsyncTask.class.getSimpleName();

    private String mURI = null;
    private int mRequestType = -1;
    private FlickDetails mFlickDetails;
    private OnFlicksDetailAsyncTaskCompleteListener mListener;

    public interface OnFlicksDetailAsyncTaskCompleteListener {
        void updateResults(FlickDetails flicksInitDetails);
    }

    public FlickDetailsAsyncTask(Context context, Uri requestURI, int requestType,
                                 FlickDetails resultFlickDetails, OnFlicksDetailAsyncTaskCompleteListener l) {
        this.mURI = requestURI.toString();
        this.mRequestType = requestType;
        mFlickDetails = resultFlickDetails;
        mListener = l;
    }

    protected FlickDetails doInBackground(String... params) {

        HttpURLConnection urlConnection = null;
        BufferedReader reader = null;

        String flicksJsonStr = null;
        try {

            Log.d(LOG_TAG, "mURI: " + mURI);

            URL url = new URL(mURI);

            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestMethod("GET");
            urlConnection.connect();

            InputStream inputStream = urlConnection.getInputStream();
            if (inputStream == null) {
                return null;
            }
            reader = new BufferedReader(new InputStreamReader(inputStream));

            StringBuffer buffer = new StringBuffer();
            String line;
            while ((line = reader.readLine()) != null) {
                buffer.append(line + "\n");
            }

            if (buffer.length() == 0) {
                return null;
            }
            flicksJsonStr = buffer.toString();

        } catch (IOException e) {
            Log.e(LOG_TAG, "Error ", e);
            return null;
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
            if (reader != null) {
                try {
                    reader.close();
                } catch (final IOException e) {
                    Log.e(LOG_TAG, "Error closing stream", e);
                }
            }
        }
        try {
            return getFlickDataFromJson(flicksJsonStr);
        } catch (JSONException e) {
            Log.e(LOG_TAG, e.toString());
            e.printStackTrace();
        }
        return null;
    }

    protected void onPostExecute(FlickDetails result) {
        if (result != null)
            mListener.updateResults(result);
    }

    protected FlickDetails getFlickDataFromJson(String flicksJsonStr)
            throws JSONException {

        JSONObject flickJson;
        JSONArray flickArray;

        switch (mRequestType) {

            case AppConstants.REQ_TYPE_DETAILS:
                final String TMDB_GENRE = "genres";
                final String TMDB_GENRE_NAME = "name";
                final String TMDB_RUNTIME = "runtime";

                flickJson = new JSONObject(flicksJsonStr);
                flickArray = flickJson.getJSONArray(TMDB_GENRE);

                StringBuilder genre = new StringBuilder();
                for (int i = 0; i < flickArray.length(); i++) {

                    JSONObject flick = flickArray.getJSONObject(i);
                    genre.append(flick.getString(TMDB_GENRE_NAME));
                    genre.append("/");
                }
                try {
                    if (genre.length() > 1)
                        genre.setLength(genre.length() - 1); //removing the last "/"
                } catch (final StringIndexOutOfBoundsException e) {
                    Log.e(LOG_TAG, "StringIndexOutOfBoundsException: ", e);
                }
                mFlickDetails.setRuntime(flickJson.getString(TMDB_RUNTIME));
                mFlickDetails.setGernes(genre.toString());
                break;
            case AppConstants.REQ_TYPE_TRAILERS:
                final String TMDB_TRAILER_RESULTS = "results";
                final String TMDB_TRAILER_NAME = "name";
                final String TMDB_TRAILER_KEY = "key";
                final String YOUTUBE_LINK = "https://www.youtube.com/watch?v=";
                final String THUMB_QUALITY = "hqdefault.jpg";

                flickJson = new JSONObject(flicksJsonStr);
                flickArray = flickJson.getJSONArray(TMDB_TRAILER_RESULTS);

                ArrayList<TrailerDetails> trailersArray = new ArrayList<>();
                for (int i = 0; i < flickArray.length(); i++) {
                    JSONObject flick = flickArray.getJSONObject(i);
                    Uri.Builder thumbUri = new Uri.Builder();
                    thumbUri.scheme("http")
                            .authority("img.youtube.com")
                            .appendPath("vi")
                            .appendPath(flick.getString(TMDB_TRAILER_KEY))
                            .appendPath(THUMB_QUALITY)
                            .build();

                    trailersArray.add(new TrailerDetails(flick.getString(TMDB_TRAILER_NAME),
                            YOUTUBE_LINK + flick.getString(TMDB_TRAILER_KEY),
                            thumbUri.build()));
                }
                mFlickDetails.setTrailers(trailersArray);
                break;
            case AppConstants.REQ_TYPE_REVIEWS:
                final String TMDB_REVIEW_RESULTS = "results";
                final String TMDB_REVIEW_AUTHOR = "author";
                final String TMDB_REVIEW_CONTENT = "content";

                flickJson = new JSONObject(flicksJsonStr);
                flickArray = flickJson.getJSONArray(TMDB_REVIEW_RESULTS);

                HashMap<String, String> reviewsMap = new HashMap<>();
                for (int i = 0; i < flickArray.length(); i++) {
                    JSONObject flick = flickArray.getJSONObject(i);
                    reviewsMap.put(flick.getString(TMDB_REVIEW_AUTHOR), flick.getString(TMDB_REVIEW_CONTENT));
                }
                mFlickDetails.setReviewsMap(reviewsMap);
                break;
        }
        return mFlickDetails;
    }
}
