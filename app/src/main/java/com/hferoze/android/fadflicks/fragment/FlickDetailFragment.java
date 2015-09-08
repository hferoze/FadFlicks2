package com.hferoze.android.fadflicks.fragment;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.database.Cursor;
import android.database.StaleDataException;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.widget.ShareActionProvider;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RatingBar;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewFlipper;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hferoze.android.fadflicks.db.FadFlicksContract;
import com.hferoze.android.fadflicks.util.AppConstants;
import com.hferoze.android.fadflicks.R;
import com.hferoze.android.fadflicks.adapter.ReviewsListViewAdapter;
import com.hferoze.android.fadflicks.obj.TrailerDetails;
import com.hferoze.android.fadflicks.util.Utils;
import com.hferoze.android.fadflicks.obj.FlickDetails;
import com.hferoze.android.fadflicks.obj.FlickDetailsAsyncTask;
import com.squareup.picasso.NetworkPolicy;
import com.squareup.picasso.Picasso;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;


public class FlickDetailFragment extends Fragment
        implements FlickDetailsAsyncTask.OnFlicksDetailAsyncTaskCompleteListener,
        LoaderManager.LoaderCallbacks<Cursor> {

    private static final String LOG_TAG = FlickDetailFragment.class.getSimpleName();

    public static final String INTENT_BUNDLE = "intent_bundle";
    private static final String TRAILERS_JSON = "trailers_json";
    private static final String REVIEWS_MAP = "reviews_map";
    private static final String FLICKS_QUERY_INFO_KEY = "flicks_query_info";
    private static final String IS_FROM_ACTIVITY = "from_activity";
    private static final String IS_DB_EMPTY = "is_db_empty";
    private static final String POSTER_FILE = "poster.jpg";
    private static final String BACKDROP_FILE = "backdrop.jpg";
    private static final String TRAILER_IMAGES_FILES = "trailer";
    private static final String VIEW_FAVORITES = "favorites";

    private static final int DETAILVIEW_LOADER = 1;

    private Utils mUtils;
    private FlickDetails mFlickDetailsList;

    private ScrollView mMainScrollView;
    private RelativeLayout mTrailersLayout;
    private RelativeLayout mEmptyLayout;
    private RelativeLayout mReviewsLayout;
    private ViewFlipper mTrailerThumbFrame;
    private ListView mReviewListView;
    private ReviewsListViewAdapter mReviewsListAdapter;
    private Bundle mFlickInfo;
    private ImageView mDetailPosterImageView;
    private ImageView mDetailBackdropImageView;
    private ImageView mToggleAddToFavoriteImageView;
    private TextView mDetailTitle;
    private TextView mDetailReleaseDate;
    private TextView mDetailPopularity;
    private TextView mDetailSummary;
    private TextView mDetailRunTime;
    private TextView mDdetailGenre;
    private RatingBar mDetailRating;

    private ShareActionProvider mShareActionProvider;
    private SharedPreferences mSharedPref;
    private SharedPreferences.Editor mSharedPrefEditor;

    private Uri mPosterUrl;
    private Uri mBackdropImgUrl;
    private float mFavoritesBtnYPos = -1;
    private boolean mFavoriteBtnPosYSaved = false;
    private boolean mIsFavBtnHidden = false;
    private boolean mFromActivity = false;
    private boolean mIsMarkedForDelete = false;
    private boolean mIsDoneDownloadingData = false;

    private Context mContext;

    public void FlickDetailFragment() {
    }

    /**
     *set first trailer for share
     */
    private void setShareIntent() {

        if (mShareActionProvider != null) {
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("text/plain");
            intent.putExtra(Intent.EXTRA_SUBJECT, mFlickInfo.getString(AppConstants.TITLE));
            intent.putExtra(Intent.EXTRA_TEXT, mFlickDetailsList.getTrailersArray().get(0).getLink());
            mShareActionProvider.setShareIntent(intent);
        }
    }

    /**
     * Animate favorite button's visibility
     * @param show : whether to show (1) or disappear (0)
     * @param pos : current position of the view
     */
    private void favoriteBtnVisibility(int show, float pos) {
       final int NEW_POS_Y = 500;
        if (show == 0) {
            translateView(mToggleAddToFavoriteImageView, pos + NEW_POS_Y);
        } else {
            translateView(mToggleAddToFavoriteImageView, pos);
        }
    }


    private void translateView(View btn, float y) {
        final int DURATION = 500;
        ObjectAnimator animRotate = ObjectAnimator.ofFloat(btn, "y", y);
        animRotate.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {

            }

            @Override
            public void onAnimationEnd(Animator animation) {
                if (mIsFavBtnHidden){
                    mIsFavBtnHidden = false;
                }else{
                    mIsFavBtnHidden = true;
                }
            }

            @Override
            public void onAnimationCancel(Animator animation) {

            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }
        });
        animRotate.setDuration(DURATION);
        animRotate.start();
    }

    /**
     * Animate favorite button
     * @param btn : view to animate
     * @param angle : The amount of angle to rotate
     */
    private void animateBtn(View btn, float angle) {
        final int DURATION = 700;
        ObjectAnimator animRotate = ObjectAnimator.ofFloat(btn, "rotation", 0f, angle);
        animRotate.setDuration(DURATION);
        animRotate.start();
    }

    /*
    * Slideshow animation for trailers
    * @param alphaIn: current visibility
    * @param alphaOut: final visibility
    *
    * @return : Slideshow Animation based on In animation or Out animation
    */
    private Animation setSlideShowAnimation(int alphaIn, int alphaOut, int duration) {
        Animation anim = new AlphaAnimation(alphaIn, alphaOut);
        if (alphaIn < alphaOut) anim.setInterpolator(new DecelerateInterpolator());
        if (alphaIn > alphaOut) {
            anim.setInterpolator(new AccelerateInterpolator());
            anim.setStartOffset(1000);
        }
        anim.setDuration(duration);
        return anim;
    }


    /**
    * Setup Detail view
    */
    public void setupView(Uri poster, Uri backdrop) {

        mPosterUrl = poster;
        mBackdropImgUrl = backdrop;

        /*setup background images*/
        if (getResources().getConfiguration().orientation
                == Configuration.ORIENTATION_LANDSCAPE) {
            mUtils.setImages(mPosterUrl, mDetailPosterImageView);
            mUtils.setBackgroundImages(mBackdropImgUrl, mDetailBackdropImageView);
        } else if (getResources().getConfiguration().orientation
                == Configuration.ORIENTATION_PORTRAIT) {
            mUtils.setImages(mPosterUrl, mDetailPosterImageView);
            mUtils.setBackgroundImages(mPosterUrl, mDetailBackdropImageView);
        }

        /*set title*/
        String title = mFlickInfo.getString(AppConstants.TITLE);
        mDetailTitle.setText(title);
        mDetailReleaseDate.setText("( " + mUtils.getYear(mFlickInfo.getString(AppConstants.RELEASE_DATE)) + " )");

        /*set release date*/
        DecimalFormat newFormat = new DecimalFormat("#.#");
        float popVal = Float.valueOf(newFormat.format(mFlickInfo.getFloat(AppConstants.POPULARITY)));

        /*set popularity*/
        mDetailPopularity.setText(popVal + "/100");

        /*set rating*/
        mDetailRating.setRating((mFlickInfo.getFloat(AppConstants.VOTE_AVG) * AppConstants.RATING_RANGE) / AppConstants.RATING_NORM);

        /*set overview*/
        mDetailSummary.setText(mFlickInfo.getString(AppConstants.OVERVIEW));
    }

    /**
     * Delete content from local database
     *  @param flickId: ID of current move
     */
    public void deleteFlickFromDb(final String flickId) {

        /*Query db to check if content exists*/
        if (queryDb(flickId)) {

            final Uri flick = FadFlicksContract.FlicksEntry.CONTENT_URI;
            new Thread(new Runnable() {
                @Override
                public void run() {
                    int del = mContext.getContentResolver().delete(
                            flick, FadFlicksContract.FlicksEntry.FLICK_ID + " = " + flickId, null);

                    String folder = AppConstants.FOLDER_NAME + flickId;
                    mUtils.deleteFavoriteFlicksImagesFolder(new File(AppConstants.EXTERNAL_STORAGE + folder));
                }
            }).start();
        }
    }

    /**
     * Add content to local database
     * Added data may miss some of the images depending on the network speed
     * @return true if successfully added to db
     */
    public boolean addFlickToDb() {
        ContentValues values = new ContentValues();
        String flickId = Integer.toString(mFlickInfo.getInt(AppConstants.ID));
        mUtils.saveImagesToDisk(mPosterUrl.toString(), POSTER_FILE, flickId);
        mUtils.saveImagesToDisk(mBackdropImgUrl.toString(), BACKDROP_FILE, flickId);

        int i = 0;
        if (mFlickDetailsList.getTrailersArray() != null) {
            JSONObject trailersCountJson = new JSONObject();
            JSONArray list = new JSONArray();

            final String TRAILER_OBJ = "trailers";
            final String TRAILER_TITLE = "title";
            final String TRAILER_LINK = "link";

            for (i = 0; i < mFlickDetailsList.getTrailersArray().size(); i++) {
                final TrailerDetails trailerDetails = mFlickDetailsList.getTrailersArray().get(i);
                String trailerImageFile = TRAILER_IMAGES_FILES + "_" + i + AppConstants.JPG_EXT;
                mUtils.saveImagesToDisk(trailerDetails.getThumb().toString(), trailerImageFile, flickId);
                try {
                    JSONObject trailersJson = new JSONObject();
                    trailersJson.put(TRAILER_TITLE, trailerDetails.getTitle());
                    trailersJson.put(TRAILER_LINK, trailerDetails.getLink());
                    list.put(trailersJson);
                } catch (JSONException e) {
                    Log.e(LOG_TAG, "JSON Error Saving Trailer " + e.toString());
                }
            }
            try {
                trailersCountJson.put(TRAILER_OBJ, list);
                mSharedPrefEditor.putString(TRAILERS_JSON + "_" + flickId, trailersCountJson.toString());
                mSharedPrefEditor.commit();
            } catch (JSONException e) {
                Log.e(LOG_TAG, "JSON Error Saving Trailer to SharedPreferences" + e.toString());
            }
        }
        if (mFlickDetailsList.getReviewsMap() != null) {
            mSharedPrefEditor.putString(REVIEWS_MAP + "_" + flickId, new JSONObject(mFlickDetailsList.getReviewsMap()).toString());
            mSharedPrefEditor.commit();
        }

        values.put(FadFlicksContract.FlicksEntry.FLICK_ID,
                Integer.toString(mFlickInfo.getInt(AppConstants.ID)));
        values.put(FadFlicksContract.FlicksEntry.TITLE,
                mFlickInfo.getString(AppConstants.TITLE));
        values.put(FadFlicksContract.FlicksEntry.RELEASE_DATE,
                mFlickInfo.getString(AppConstants.RELEASE_DATE));
        values.put(FadFlicksContract.FlicksEntry.VOTE_AVG,
                Float.toString(mFlickInfo.getFloat(AppConstants.VOTE_AVG)));
        values.put(FadFlicksContract.FlicksEntry.RUNTIME,
                mFlickDetailsList.getRuntime());
        values.put(FadFlicksContract.FlicksEntry.GENRE,
                mFlickDetailsList.getGernes());
        values.put(FadFlicksContract.FlicksEntry.POPULARITY,
                Float.toString(mFlickInfo.getFloat(AppConstants.POPULARITY)));
        values.put(FadFlicksContract.FlicksEntry.OVERVIEW,
                mFlickInfo.getString(AppConstants.OVERVIEW));


        Uri flick = FadFlicksContract.FlicksEntry.CONTENT_URI;

        if (mContext.getContentResolver().insert(
                flick, values).toString().length() > 0) {
            return true;
        }
        return false;
    }

    /**
     * Query database to check if content exists
     * @param flickId: ID of current move
     * @return: true if content exists
     */
    public boolean queryDb(String flickId) {
        Uri flick = FadFlicksContract.FlicksEntry.CONTENT_URI;
        Cursor c = mContext.getContentResolver().query(flick, null, null, null, "_id");
        try {
            if (!c.moveToFirst()) {
                Log.d(LOG_TAG, "no content yet");
            } else {
                do {
                    if (c.getString(c.getColumnIndex(FadFlicksContract.FlicksEntry.FLICK_ID)).equals(flickId)) {
                        return true;
                    }
                } while (c.moveToNext());
            }
        } catch (StaleDataException e) {
            e.printStackTrace();

        } finally {
            c.close();
        }
        return false;
    }

    /**
     * Load data from TMDB using AsyncTask
     * @param flickID: ID of current movie
     */
    private void loadFlickDataFromTMDB(String flickID) {
        if (!mIsDoneDownloadingData) {
            Log.d(LOG_TAG, " downloading .....");
            new FlickDetailsAsyncTask(mContext,
                    buildDetailURI(flickID,
                            AppConstants.REQ_TYPE_DETAILS), AppConstants.REQ_TYPE_DETAILS, mFlickDetailsList, this).execute();
            new FlickDetailsAsyncTask(mContext,
                    buildDetailURI(flickID,
                            AppConstants.REQ_TYPE_TRAILERS), AppConstants.REQ_TYPE_TRAILERS, mFlickDetailsList, this).execute();
            new FlickDetailsAsyncTask(mContext,
                    buildDetailURI(flickID,
                            AppConstants.REQ_TYPE_REVIEWS), AppConstants.REQ_TYPE_REVIEWS, mFlickDetailsList, this).execute();
        }
    }

    /**
     * Load data from Local database using ContentProvider
     * @param flickId: ID of current movie
     * @param c: loader cursor
     */
    private void loadFlickDataFromDb(String flickId, Cursor c) {
        if (mFlickDetailsList != null) {

            final String RUNTIME = FadFlicksContract.FlicksEntry.RUNTIME;
            final String GENRE = FadFlicksContract.FlicksEntry.GENRE;

            mFlickDetailsList.setRuntime(c.getString(c.getColumnIndex(RUNTIME)));
            mFlickDetailsList.setGernes(c.getString(c.getColumnIndex(GENRE)));

            mToggleAddToFavoriteImageView.setImageResource(R.mipmap.fav_selected);
            try {
                final String TRAILER_OBJ = "trailers";
                final String TRAILER_TITLE = "title";
                final String TRAILER_LINK = "link";

                if (mSharedPref.getString(TRAILERS_JSON + "_" + flickId, null) != null) {
                    JSONObject trailersJson = new JSONObject(mSharedPref.getString(TRAILERS_JSON + "_" + flickId, null));
                    if (trailersJson != null) {
                        JSONArray flickArray = trailersJson.getJSONArray(TRAILER_OBJ);

                        ArrayList<TrailerDetails> trailersArray = new ArrayList<>();
                        for (int i = 0; i < flickArray.length(); i++) {
                            JSONObject jsonFlick = flickArray.getJSONObject(i);
                            trailersArray.add(new TrailerDetails(jsonFlick.getString(TRAILER_TITLE),
                                    jsonFlick.getString(TRAILER_LINK),
                                    mUtils.buildLocalImageUri(flickId, TRAILER_IMAGES_FILES + "_" + i + AppConstants.JPG_EXT)));
                        }
                        mFlickDetailsList.setTrailers(trailersArray);
                    }
                }
            } catch (JSONException e) {
                e.printStackTrace();
            } catch (NullPointerException e) {
                e.printStackTrace();
            }
            try {
                if (mSharedPref.getString(REVIEWS_MAP + "_" + flickId, null) != null) {
                    String reviewsMStr = mSharedPref.getString(REVIEWS_MAP + "_" + flickId, null);
                    if (reviewsMStr != null) {
                        HashMap<String, String> reviewsMap =
                                new ObjectMapper().readValue(reviewsMStr, HashMap.class);
                        mFlickDetailsList.setReviewsMap(reviewsMap);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            } catch (NullPointerException e) {
                e.printStackTrace();
            }

            updateFlickQueryInfo();

            setupView(mUtils.buildLocalImageUri(flickId, POSTER_FILE),
                    mUtils.buildLocalImageUri(flickId, BACKDROP_FILE));
        }
    }

    /**
     * Update views based on available data
     * @throws NullPointerException if missing data
     */
    private void updateFlickQueryInfo()
            throws NullPointerException {

        if (mFlickDetailsList != null) {
            mIsDoneDownloadingData = true;
            mDetailRunTime.setText(mFlickDetailsList.getRuntime() + " min");
            mDdetailGenre.setText(mFlickDetailsList.getGernes());

            if (mFlickDetailsList.getTrailersArray() != null
                    && mFlickDetailsList.getReviewsMap() != null) {
                if (mFlickDetailsList.getTrailersArray().size() > 0) {
                    populateTrailers();
                    setShareIntent(); //add first trailer to share intent
                } else {
                    if (mTrailersLayout != null)
                        mTrailersLayout.setVisibility(View.GONE);
                }
                if (mFlickDetailsList.getReviewsMap().size() > 0) {
                    populateReviews();
                } else {
                    if (mReviewsLayout != null)
                        mReviewsLayout.setVisibility(View.GONE);
                }
            }
        }
    }

    /**
     * Build Uri for the requestType
     * @param flickId: ID of the current Movie
     * @param requestType: Type of request to send to TMDB: 1. REQ_TYPE_DETAILS 2. REQ_TYPE_TRAILERS or 3. REQ_TYPE_REVIEWS
     *
     * @return: Uri for the requestType
     */
    private Uri buildDetailURI(String flickId, int requestType) {

        Uri.Builder builder = new Uri.Builder();
        final String URI_SCHEME = "http";
        final String URI_AUTH = "api.themoviedb.org";
        final String URI_APPEND_PATH1 = "3";
        final String URI_APPEND_PATH2 = "movie";
        final String URI_API_KEY = "api_key";
        final String URI_VIDEO = "videos";
        final String URI_REVIEWS = "reviews";

        switch (requestType) {
            case AppConstants.REQ_TYPE_DETAILS:
                builder.scheme(URI_SCHEME)
                        .authority(URI_AUTH)
                        .appendPath(URI_APPEND_PATH1)
                        .appendPath(URI_APPEND_PATH2)
                        .appendPath(flickId)
                        .appendQueryParameter(URI_API_KEY, mContext.getString(R.string.api_key))
                        .build();
                return builder.build();
            case AppConstants.REQ_TYPE_TRAILERS:
                builder.scheme(URI_SCHEME)
                        .authority(URI_AUTH)
                        .appendPath(URI_APPEND_PATH1)
                        .appendPath(URI_APPEND_PATH2)
                        .appendPath(flickId)
                        .appendPath(URI_VIDEO)
                        .appendQueryParameter(URI_API_KEY, mContext.getString(R.string.api_key))
                        .build();
                return builder.build();
            case AppConstants.REQ_TYPE_REVIEWS:
                builder.scheme(URI_SCHEME)
                        .authority(URI_AUTH)
                        .appendPath(URI_APPEND_PATH1)
                        .appendPath(URI_APPEND_PATH2)
                        .appendPath(flickId)
                        .appendPath(URI_REVIEWS)
                        .appendQueryParameter(URI_API_KEY, mContext.getString(R.string.api_key))
                        .build();
                return builder.build();
            default:
                return builder.appendPath("").build();
        }
    }

    /**
     * Setup images for Trailer slideshow
     * @param imagePath: Uri to the image file
     * @param trailerImageView: ImageView to set trailer thumb image with
     */
    private void setTrailersThumbImages(Uri imagePath, ImageView trailerImageView) {

        Picasso.with(mContext).
                load(imagePath).
                networkPolicy(mUtils.isDataAvaialable() ? NetworkPolicy.NO_CACHE : NetworkPolicy.OFFLINE).
                into(trailerImageView);
    }

    /**
     * Populate Trailers titles, slideshow and links and start slideshow
     * @throws NullPointerException
     */
    private void populateTrailers()
            throws NullPointerException {

        final int NUM_OF_FRAMES_THRESH = 1;
        final int SLIDESHOW_FLOW_FLIP_INTERVAL = 4000;
        final int ANIM_DURATION = 1000;
        final int IMAGEVIEW_HEIGHT = (int) mUtils.getDpi(2160, mContext);
        final int TEXTVIEW_SIZE = 20;

        if (getActivity() != null) {
            int i = 0;
            for (i = 0; i < mFlickDetailsList.getTrailersArray().size(); i++) {
                final TrailerDetails trailerDetails = mFlickDetailsList.getTrailersArray().get(i);

                RelativeLayout relativeLayout = new RelativeLayout(mContext);
                RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(
                        RelativeLayout.LayoutParams.MATCH_PARENT,
                        RelativeLayout.LayoutParams.MATCH_PARENT);

                RelativeLayout.LayoutParams imageViewParams = new RelativeLayout.LayoutParams(
                        RelativeLayout.LayoutParams.MATCH_PARENT,
                        IMAGEVIEW_HEIGHT);

                //add trailer thumb
                ImageView imageView = new ImageView(getActivity());
                imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
                imageViewParams.addRule(RelativeLayout.CENTER_IN_PARENT);
                imageView.setLayoutParams(imageViewParams);
                setTrailersThumbImages(trailerDetails.getThumb(), imageView);
                imageView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(trailerDetails.getLink()));
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        if (mUtils.isIntentSafe(intent)) mContext.startActivity(intent);
                        if (mUtils.isIntentSafe(intent)) mContext.startActivity(intent);
                    }
                });

                relativeLayout.addView(imageView);

                //add trailer title
                TextView tv = new TextView(getActivity());
                tv.setText(trailerDetails.getTitle());
                tv.setTextAppearance(getActivity(), R.style.PortraitTitleTextStyle);
                tv.setTypeface(Typeface.DEFAULT_BOLD);
                tv.setTextSize(TEXTVIEW_SIZE);
                params.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
                params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
                tv.setLayoutParams(params);
                relativeLayout.addView(tv);

                mTrailerThumbFrame.addView(relativeLayout);
            }

            if (i > NUM_OF_FRAMES_THRESH) {
                mTrailerThumbFrame.setAutoStart(true);
                mTrailerThumbFrame.setFlipInterval(SLIDESHOW_FLOW_FLIP_INTERVAL);
                mTrailerThumbFrame.setInAnimation(setSlideShowAnimation(0, 1, ANIM_DURATION));
                mTrailerThumbFrame.setOutAnimation(setSlideShowAnimation(1, 0, ANIM_DURATION));
                mTrailerThumbFrame.startFlipping();
            }
        }
    }

    /**
     * Populate Reviews ListView
     * @throws NullPointerException
     */
    private void populateReviews()
            throws NullPointerException {
        try {
            HashMap<String, String> reviewMap = mFlickDetailsList.getReviewsMap();
            mReviewsListAdapter = new ReviewsListViewAdapter(mContext,
                    reviewMap);
            mReviewListView.setAdapter(mReviewsListAdapter);

            mReviewListView.setOnTouchListener(new ListView.OnTouchListener() {
                final float DISTANCE_THRESH = 4;
                float downY = 0;
                float upY = 0;

                /*
                * Added onTouch to make sure ListView responds properly to user's touch
                */
                @Override
                public boolean onTouch(View v, MotionEvent event) {

                    View viewTop = mReviewListView.getChildAt(0);
                    int topView = viewTop.getTop();

                    int action = event.getAction();
                    switch (action) {
                        case MotionEvent.ACTION_DOWN:
                            mReviewListView.getParent().requestDisallowInterceptTouchEvent(true); //make sure ListView scrolls when touched
                            downY = event.getRawY();
                            break;
                        case MotionEvent.ACTION_UP:
                            upY = event.getRawY();
                            break;
                        case MotionEvent.ACTION_MOVE:
                            upY = event.getRawY();
                            /*
                            * If at top item, ListView scroll is disabled when going further up
                            */
                            if (topView == 0 && mReviewListView.getFirstVisiblePosition() == 0) {
                                if (upY - downY < DISTANCE_THRESH) {  //going down
                                    mReviewListView.getParent().requestDisallowInterceptTouchEvent(true);
                                } else if (upY - downY > DISTANCE_THRESH) { //going up
                                    mReviewListView.getParent().requestDisallowInterceptTouchEvent(false);
                                }
                            } else {
                                /*
                                * If at last item, ListView scroll is disabled when going further down
                                */
                                if (mReviewListView.getLastVisiblePosition() == mReviewListView.getAdapter().getCount() - 1
                                        && mReviewListView.getChildAt(mReviewListView.getChildCount() - 1).getBottom() <= mReviewListView.getHeight()) {
                                    if (upY - downY < DISTANCE_THRESH) {  //going down
                                        mReviewListView.getParent().requestDisallowInterceptTouchEvent(false);
                                    } else if (upY - downY > DISTANCE_THRESH) { //going up
                                        mReviewListView.getParent().requestDisallowInterceptTouchEvent(true);
                                    }
                                }
                            }
                            break;
                    }
                    v.onTouchEvent(event);
                    return true;
                }
            });
        } catch (NullPointerException e) {
            Log.e(LOG_TAG, "NullPointerException in populateReviews(): " + e.toString());
        } catch (Exception e) {
            Log.e(LOG_TAG, "Exception in populateReviews(): " + e.toString());
        }
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = this.getActivity().getApplicationContext();
        mUtils = new Utils(getActivity());
        mSharedPref = PreferenceManager.getDefaultSharedPreferences(getActivity());
        mSharedPrefEditor = mSharedPref.edit();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putParcelable(FLICKS_QUERY_INFO_KEY, mFlickDetailsList);
        outState.putBundle(INTENT_BUNDLE, mFlickInfo);
        outState.putBoolean(IS_FROM_ACTIVITY, mFromActivity);
        super.onSaveInstanceState(outState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {


        View rootView = inflater.inflate(R.layout.fragment_flick_detail, container, false);

        mDetailPosterImageView = (ImageView) rootView.findViewById(R.id.detailview_posterImageView);
        mDetailBackdropImageView = (ImageView) rootView.findViewById(R.id.detailview_backdropImageView);
        mDetailTitle = (TextView) rootView.findViewById(R.id.detailview_flickTitleTextView);
        mDetailReleaseDate = (TextView) rootView.findViewById(R.id.detailview_releaseDateTextView);
        mDetailPopularity = (TextView) rootView.findViewById(R.id.detailview_popularityTextView);
        mDetailRating = (RatingBar) rootView.findViewById(R.id.detailview_ratingBar);
        mDetailSummary = (TextView) rootView.findViewById(R.id.detailview_flickOverview);

        mDetailRunTime = (TextView) rootView.findViewById(R.id.detailview_durationTextView);
        mDdetailGenre = (TextView) rootView.findViewById(R.id.detailview_genreTextView);

        mTrailersLayout = (RelativeLayout) rootView.findViewById(R.id.slideshowRelativeLayout);
        mTrailerThumbFrame = (ViewFlipper) rootView.findViewById(R.id.trailerThumbViewFlipper);

        mReviewListView = (ListView) rootView.findViewById(R.id.reviewsListView);
        mReviewsLayout = (RelativeLayout) rootView.findViewById(R.id.reviewsRelativeLayout);

        mToggleAddToFavoriteImageView = (ImageView) rootView.findViewById(R.id.detailview_favoriteThisImageView);

        mMainScrollView = (ScrollView) rootView.findViewById(R.id.mainScrollView);

        mEmptyLayout = (RelativeLayout) rootView.findViewById(R.id.favorites_empty);

        //Get Bundle if Two pane view
        Bundle bundle = getArguments();

        if (bundle != null &&
                mFlickInfo == null &&
                bundle.containsKey(INTENT_BUNDLE)) {
            mFlickInfo = bundle.getBundle(INTENT_BUNDLE);
            mFlickDetailsList = new FlickDetails();
            mFromActivity = bundle.getBoolean(IS_FROM_ACTIVITY, false);
        }else {
            if (bundle!=null && bundle.containsKey(IS_DB_EMPTY)){
                if (bundle.getBoolean(IS_DB_EMPTY, false)){
                    mEmptyLayout.setVisibility(View.VISIBLE);
                }
            }
        }

        //Get data from activity if single pane view
        if (savedInstanceState == null
                || !savedInstanceState.containsKey(FLICKS_QUERY_INFO_KEY)
                || !savedInstanceState.containsKey(INTENT_BUNDLE)) {
            Intent in = getActivity().getIntent();
            if (in != null
                    && in.hasExtra(AppConstants.ID)
                    && in.hasExtra(IS_FROM_ACTIVITY)) {
                mFlickInfo = in.getExtras();
                mFlickDetailsList = new FlickDetails();
                mFromActivity = in.getBooleanExtra(IS_FROM_ACTIVITY, false);
            }
        } else {
            mFlickDetailsList = savedInstanceState.getParcelable(FLICKS_QUERY_INFO_KEY);
            mFlickInfo = savedInstanceState.getBundle(INTENT_BUNDLE);
            mFromActivity = savedInstanceState.getBoolean(IS_FROM_ACTIVITY);
            updateFlickQueryInfo();
        }

        if (mFlickInfo == null && !mFromActivity) {
            mEmptyLayout.setVisibility(View.VISIBLE);
        } else {
            mEmptyLayout.setVisibility(View.INVISIBLE);
        }

        if (mFlickInfo != null) {
            if (queryDb(Integer.toString(mFlickInfo.getInt(AppConstants.ID)))) {
                mToggleAddToFavoriteImageView.setImageResource(R.mipmap.fav_selected);
            } else {
                mToggleAddToFavoriteImageView.setImageResource(R.mipmap.fav_not_selected);
                setupView(mUtils.buildURI(AppConstants.REMOTE_IMAGE_SIZES[
                                AppConstants.DETAIL_POSTER_IMG_SIZE_IDX], mFlickInfo.getString(AppConstants.POSTER_PATH)),
                        mUtils.buildURI(AppConstants.REMOTE_IMAGE_SIZES[
                                AppConstants.DETAIL_BACKDROP_IMG_SIZE_IDX], mFlickInfo.getString(AppConstants.BACKDROP_PATH)));
            }

            mToggleAddToFavoriteImageView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (!queryDb(Integer.toString(mFlickInfo.getInt(AppConstants.ID)))) {
                        animateBtn(mToggleAddToFavoriteImageView, 360);
                        mIsMarkedForDelete = false;
                        mToggleAddToFavoriteImageView.setImageResource(R.mipmap.fav_selected);
                        if (!addFlickToDb()) {
                            Toast.makeText(mContext, "Failed to save Favorite Flick information", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        if (!mIsMarkedForDelete) {
                            animateBtn(mToggleAddToFavoriteImageView, -360);
                            mToggleAddToFavoriteImageView.setImageResource(R.mipmap.fav_not_selected);
                            mIsMarkedForDelete = true;
                        } else {
                            animateBtn(mToggleAddToFavoriteImageView, 360);
                            mToggleAddToFavoriteImageView.setImageResource(R.mipmap.fav_selected);
                            mIsMarkedForDelete = false;
                        }
                        deleteFlickFromDb(Integer.toString(mFlickInfo.getInt(AppConstants.ID)));
                    }
                }
            });
        }

        mMainScrollView.setOnTouchListener(new FavBtnOnTouchListener());
        mTrailersLayout.setOnTouchListener(new FavBtnOnTouchListener());

        return rootView;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mIsMarkedForDelete) {
            deleteFlickFromDb(Integer.toString(mFlickInfo.getInt(AppConstants.ID)));
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        //making sure the menu looks good!
        if (getView() != null && getView().isShown()) {
            setHasOptionsMenu(true);
        } else {
            if (!mFromActivity) {
                setHasOptionsMenu(false);
            } else {
                setHasOptionsMenu(true);
            }
        }

        if (mFlickInfo != null) {

            if (mUtils.isDataAvaialable() && !queryDb(Integer.toString(mFlickInfo.getInt(AppConstants.ID)))) {
                loadFlickDataFromTMDB(Integer.toString(mFlickInfo.getInt(AppConstants.ID)));
                mToggleAddToFavoriteImageView.setImageResource(R.mipmap.fav_not_selected);
            } else {
                final String flickId = Integer.toString(mFlickInfo.getInt(AppConstants.ID));
                if (queryDb(flickId)) {
                    mToggleAddToFavoriteImageView.setImageResource(R.mipmap.fav_selected);
                    final Uri flick = FadFlicksContract.FlicksEntry.CONTENT_URI;
                    Cursor c = mContext.getContentResolver().query(flick, null, null, null, "_id");
                    if (!c.moveToFirst()) {
                        Log.d(LOG_TAG, "no favorites");
                    } else {
                        do {
                            String flickIdDb = c.getString(c.getColumnIndex(FadFlicksContract.FlicksEntry.FLICK_ID));
                            if (flickIdDb.equals(flickId)) {
                                loadFlickDataFromDb(flickId, c);
                            }
                        } while (c.moveToNext());
                    }
                }
            }
        }
    }

    @Override
    public void updateResults(FlickDetails flicksInitDetails) {
        updateFlickQueryInfo();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_flick_detail, menu);
        MenuItem item = menu.findItem(R.id.menu_item_share);
        mShareActionProvider = (ShareActionProvider) MenuItemCompat.getActionProvider(item);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (getLoaderManager().getLoader(DETAILVIEW_LOADER) == null) {
            getLoaderManager().initLoader(DETAILVIEW_LOADER, null, this);
        }
        setHasOptionsMenu(true);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        Uri flick = FadFlicksContract.FlicksEntry.CONTENT_URI;
        CursorLoader cursorLoader = new CursorLoader(getActivity(), flick, null, null, null, "_id");
        return cursorLoader;
    }

    /*
    * Only called in "Favorites" mode
    */
    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor c) {
        if (mSharedPref.getString(getString(R.string.pref_sort_order),
                getString(R.string.pref_sort_default_value)).equals(VIEW_FAVORITES)) {
            if (!c.moveToFirst()) {
                if (mFromActivity)
                    getActivity().finish();
                else
                    mEmptyLayout.setVisibility(View.VISIBLE);
            } else {
                mEmptyLayout.setVisibility(View.INVISIBLE);

                final String FLICK_ID = FadFlicksContract.FlicksEntry.FLICK_ID;

                do {
                    String flickIdDb = c.getString(c.getColumnIndex(FLICK_ID));
                    if (mFlickInfo != null && Integer.toString(mFlickInfo.getInt(AppConstants.ID)).equals(flickIdDb)) {
                        final String flickId = Integer.toString(mFlickInfo.getInt(AppConstants.ID));
                        loadFlickDataFromDb(flickId, c);
                    }
                } while (c.moveToNext());
            }
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
    }

    /*
    * OnTouchListener class for both main ScrollView and RelativeLayout
    * (Trailer's Slideshow) to make sure Favorite button is animated correctly
    */
    class FavBtnOnTouchListener implements View.OnTouchListener {
        final static float DISTANCE_THRESH = 1;
        float downY = 0;
        float upY = 0;

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            int action = event.getAction();
            switch (action) {
                case MotionEvent.ACTION_DOWN:
                    downY = event.getRawY();
                    break;
                case MotionEvent.ACTION_UP:
                    upY = event.getRawY();
                    if (upY - downY < DISTANCE_THRESH) {  //going down
                        if (!mIsFavBtnHidden) {
                            if (!mFavoriteBtnPosYSaved) {
                                mFavoritesBtnYPos = mToggleAddToFavoriteImageView.getY();
                                mFavoriteBtnPosYSaved = true;
                            }
                            favoriteBtnVisibility(0, mToggleAddToFavoriteImageView.getY());
                        }
                    } else if (upY - downY > DISTANCE_THRESH) { //going up
                        if (mIsFavBtnHidden) favoriteBtnVisibility(1, mFavoritesBtnYPos);
                    }
                    break;
            }
            return false;
        }
    }
}
