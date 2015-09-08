package com.hferoze.android.fadflicks.fragment;

import android.app.Dialog;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.hferoze.android.fadflicks.adapter.FlicksGridImagesCursorAdapter;
import com.hferoze.android.fadflicks.db.FadFlicksContract;
import com.hferoze.android.fadflicks.util.AppConstants;
import com.hferoze.android.fadflicks.activity.FadFlicksActivity;
import com.hferoze.android.fadflicks.adapter.FlicksGridImagesAdapter;
import com.hferoze.android.fadflicks.R;
import com.hferoze.android.fadflicks.util.Utils;
import com.hferoze.android.fadflicks.obj.FlickDataFetchAsyncTask;
import com.hferoze.android.fadflicks.obj.FlicksInitDetails;

import java.io.IOException;
import java.util.ArrayList;

public class FadFlicksFragment extends Fragment
        implements FlickDataFetchAsyncTask.OnAsyncTaskCompleteListener,
        LoaderManager.LoaderCallbacks<Cursor> {

    private static final String LOG_TAG = FadFlicksFragment.class.getSimpleName();

    private static final String DATA_SETTINGS_PKG = "com.android.settings";
    private static final String DATA_SETTINGS_CLASS = "com.android.settings.Settings$DataUsageSummaryActivity";
    private static final String FLICKS_DETAILS_LIST_KEY = "flicks_details";
    private static final String SELECTED_POS_KEY = "selected_position";
    private static final String SORT_BY_POPULARITY = "popularity.desc";
    private static final String SORT_BY_HIGHEST_RATED = "vote_average.desc";
    private static final String ALERT_CANCELLED_KEY = "alert_cancelled";
    private static final String DB_CNT_KEY = "db_count";
    private static final String POSTER_FILE = "poster.jpg";
    private static final String BACKDROP_FILE = "backdrop.jpg";
    private static final String VIEW_FAVORITES = "favorites";
    private static final String REQUEST_POSTED_KEY = "launch_activity_request";

    private static final int CURRENT_MODE_OL = 0;
    private static final int CURRENT_MODE_DB = 1;
    private static final int GRIDVIEW_LOADER = 0;
    private static final int DATA_STATE_DISCONNECTED = 0;
    private static final int DATA_STATE_CONNECTED = 1;

    private GridView mFlicksGridView;
    private FlicksGridImagesAdapter mFlicksGridImagesAdapter;
    private FlicksGridImagesCursorAdapter mFlicksGridImagesCursorAdapter;
    private FlickDataFetchAsyncTask mFlickDataFetchTask;
    private Utils mUtils;
    private MenuItem mCurrentOrder;
    private Dialog mAlert;
    private ActionBar mActionBar;
    private TextView mNoDataTextView;
    private TextView mNoFavsTextView;
    private Context mContext;

    private SharedPreferences mSharedPref;
    private SharedPreferences.Editor mSharedPrefEditor;

    private int mCurrentMode = -1;
    private int mDbCount = 0;
    private int mPosition = -1;
    private int data_state;
    private boolean mIsTwoPane = false;
    private boolean mAlertCancelledState = false;
    private boolean mIsDoneDownloadingData = false;

    public static ArrayList<FlicksInitDetails> flicksInitDetails;
    public boolean mActivityLaunchPost = false;

    public void FadFlicksFragment() {
    }

    public void setIsTwoPane(boolean twoPane) {
        mIsTwoPane = twoPane;
    }

    /*
    * Check if db is currently empty and notify detail view
    */
    public void checkDbAndNotify() {
        final ProgressBar mainProgressBar = (ProgressBar) getView().findViewById(R.id.mainProgressBar);
        mainProgressBar.setVisibility(View.VISIBLE);
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                 if (mDbCount==0){
                    DbEmptyListener listener = (DbEmptyListener) getActivity();
                    listener.onDbEmpty(true);
                }
                mainProgressBar.setVisibility(View.INVISIBLE);
            }
        }, 1000);
    }

    /*
    * Update current view based on selected option and data availability
    * @param sort_order: current sorting option
    */
    public void updateFlicks(String sort_order) {
        mNoFavsTextView.setVisibility(View.INVISIBLE);
        if (mUtils.isDataAvaialable()) {
            dataState(DATA_STATE_CONNECTED);
            mAlertCancelledState = false;
            mNoDataTextView.setVisibility(View.INVISIBLE);
            if (mAlert != null && mAlert.isShowing())
                mAlert.dismiss();
            if (!mIsDoneDownloadingData && mSharedPref != null) {
                mFlickDataFetchTask = new FlickDataFetchAsyncTask(getActivity(), sort_order, this);
                mFlickDataFetchTask.execute();
            } else {
                if (mIsTwoPane) {
                    updateGridBasedOnDataConnectionState();
                }
            }
        } else {
            if (!mAlertCancelledState) {
                dataAlert(DATA_STATE_DISCONNECTED);
            } else {
                Toast.makeText(mContext, getResources().getString(R.string.data_unavailable_msg), Toast.LENGTH_SHORT).show();
            }
            mNoDataTextView.setVisibility(View.VISIBLE);
            dataState(DATA_STATE_DISCONNECTED);
            updateGridBasedOnDataConnectionState();
        }
        if (mFlicksGridView != null) {
            mFlicksGridView.setSelection(mPosition);
            mFlicksGridView.setSelected(true);
        }
    }

    /*
    * Update GridView based on current data state and whether we are in Two Pane view
    */
    private void updateGridBasedOnDataConnectionState() {
        if (this.data_state == DATA_STATE_CONNECTED) {
            mFlicksGridImagesAdapter = new FlicksGridImagesAdapter(getActivity(), flicksInitDetails, false);
            mIsDoneDownloadingData = true;
            mFlicksGridView.invalidateViews();
            mFlicksGridView.setAdapter(mFlicksGridImagesAdapter);
        } else {
            mFlicksGridImagesAdapter = new FlicksGridImagesAdapter(getActivity(), flicksInitDetails, true);
            mFlicksGridView.invalidateViews();
            mFlicksGridView.setAdapter(mFlicksGridImagesAdapter);
        }
        //Select mPosition if we are in Two Pane so that detail view would show current selection
        if (mIsTwoPane) {
            if (mFlicksGridView.getAdapter().getCount()>0
                    && mPosition != mFlicksGridView.INVALID_POSITION
                    && mPosition < mFlicksGridView.getAdapter().getCount()) {
                selectPosItem(mPosition);
            }
        }
    }

    /*
    * In in Two Pane view, show currently selected item
    * @param pos: Currently selected item
    */
    public void selectPosItem(final int pos)
            throws ArrayIndexOutOfBoundsException {
        mFlicksGridView.setSelection(pos);
        mFlicksGridView.setItemChecked(pos, true);
        new Handler().post(new Runnable() {
            @Override
            public void run() {
                mFlicksGridView.performItemClick(
                        mFlicksGridView.getChildAt(pos),
                        pos,
                        mFlicksGridView.getAdapter().getItemId(pos));
            }
        });
    }

    /*
    * Set current data state
    * @param dataState: current data state
    */
    private void dataState(int dataState) {
        this.data_state = dataState;
    }

    /*
    * Show Data Unavailable popup based on current data state
    * @param dataState: current data state
    */
    private void dataAlert(int dataState) {
        if (dataState == DATA_STATE_DISCONNECTED) {
            if (mAlert != null && !mAlert.isShowing()) {
                mAlert.show();
            }
        }
    }

    /*
    * Launch data settings intent
    */
    private void launchCellularDataSettings() {
        Intent intent = new Intent();
        intent.setComponent(new ComponentName(
                DATA_SETTINGS_PKG,
                DATA_SETTINGS_CLASS));
        if (mUtils.isIntentSafe(intent))
            startActivity(intent);
    }

    /*
    * Launch Wifi Settings intent
    */
    private void launchWifiSettings() {
        Intent intent = new Intent(android.provider.Settings.ACTION_WIFI_SETTINGS);
        if (mUtils.isIntentSafe(intent))
            startActivity(intent);
    }

    /*
    * Remember if user cancelled popup
    */
    private void alertCancelled() {
        mAlertCancelledState = true;
        if (mAlert != null && mAlert.isShowing()) {
            mAlert.dismiss();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        setHasOptionsMenu(true);
        if (!mSharedPref.getString(
                getString(R.string.pref_sort_order),
                getString(R.string.pref_sort_default_value)).equals(VIEW_FAVORITES)) {
            if (mFlicksGridView != null) mFlicksGridView.setAdapter(mFlicksGridImagesAdapter);
            updateFlicks(mSharedPref.getString(
                    getString(R.string.pref_sort_order),
                    getString(R.string.pref_sort_default_value)));

        } else {
            if (mFlicksGridView != null) {
                getLoaderManager().restartLoader(GRIDVIEW_LOADER, null, this);
                mFlicksGridView.invalidate();
                mFlicksGridView.setAdapter(mFlicksGridImagesCursorAdapter);

                if (mDbCount > 0) {
                    mNoFavsTextView.setVisibility(View.INVISIBLE);
                } else {
                    mNoFavsTextView.setVisibility(View.VISIBLE);
                }

            }
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = this.getActivity().getApplicationContext();
        mSharedPref = PreferenceManager.getDefaultSharedPreferences(getActivity());
        mSharedPrefEditor = mSharedPref.edit();
        mUtils = new Utils(mContext);

        if (!mSharedPref.getString(getString(R.string.pref_sort_order),
                getString(R.string.pref_sort_default_value)).equals(VIEW_FAVORITES)) {
            mCurrentMode = CURRENT_MODE_OL;
        } else {
            mCurrentMode = CURRENT_MODE_DB;
        }

        setRetainInstance(true);
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mAlert != null && mAlert.isShowing()) {
            mAlert.dismiss();
        }
        if (mFlickDataFetchTask.getStatus() == AsyncTask.Status.RUNNING ||
                mFlickDataFetchTask.getStatus() == AsyncTask.Status.PENDING) {
            mFlickDataFetchTask.cancel(true);
        }
        setHasOptionsMenu(false);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putParcelableArrayList(FLICKS_DETAILS_LIST_KEY, flicksInitDetails);
        outState.putBoolean(ALERT_CANCELLED_KEY, mAlertCancelledState);
        outState.putBoolean(REQUEST_POSTED_KEY, mActivityLaunchPost);
        outState.putInt(DB_CNT_KEY, mDbCount);

        if (mPosition != mFlicksGridView.INVALID_POSITION) {
            outState.putInt(SELECTED_POS_KEY, mPosition);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        final View rootView = inflater.inflate(R.layout.fragment_fad_flicks, container, false);

        mActionBar = ((FadFlicksActivity) getActivity()).getSupportActionBar();

        //Setup data alert popup
        mAlert = new Dialog(getActivity());
        mAlert.requestWindowFeature(Window.FEATURE_NO_TITLE);
        mAlert.setContentView(R.layout.no_data_dialog);

        Button btnAlertCell = (Button) mAlert.findViewById(R.id.btn_alert_data_settings);
        btnAlertCell.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                launchCellularDataSettings();
            }
        });
        Button btnAlertWifi = (Button) mAlert.findViewById(R.id.btn_alert_wifi_settings);
        btnAlertWifi.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                launchWifiSettings();
            }
        });
        Button btnAlertCancel = (Button) mAlert.findViewById(R.id.btn_alert_cancel);
        btnAlertCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                alertCancelled();
            }
        });

        mFlickDataFetchTask = new FlickDataFetchAsyncTask(getActivity(), null, this);
        if (savedInstanceState == null
                || !savedInstanceState.containsKey(FLICKS_DETAILS_LIST_KEY)
                || !savedInstanceState.containsKey(REQUEST_POSTED_KEY)) {
            flicksInitDetails = new ArrayList<>();
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    mActivityLaunchPost = true;
                }
            }, new FadFlicksActivity().LAUNCH_WAIT);

            mPosition = 0;
            mDbCount = 0;
        } else {
            flicksInitDetails = savedInstanceState.getParcelableArrayList(FLICKS_DETAILS_LIST_KEY);
            mAlertCancelledState = savedInstanceState.getBoolean(ALERT_CANCELLED_KEY);
            mPosition = savedInstanceState.getInt(SELECTED_POS_KEY);
            mDbCount = savedInstanceState.getInt(DB_CNT_KEY);
        }

        //Create folder to store Favorite movies data
        try {
            mUtils.makeFadFlickFolder(AppConstants.EXTERNAL_STORAGE + AppConstants.FOLDER_NAME);
        } catch (IOException e) {
            e.printStackTrace();
        }

        mNoDataTextView = (TextView) rootView.findViewById(R.id.noDataTextView);
        mNoFavsTextView = (TextView) rootView.findViewById(R.id.noFavsTextView);

        //GridView adapter for Popular and Highest Rated views
        mFlicksGridImagesAdapter = new FlicksGridImagesAdapter(
                getActivity(),
                flicksInitDetails,
                false);

        //GridView adapter for Favorites view
        mFlicksGridImagesCursorAdapter = new FlicksGridImagesCursorAdapter(
                getActivity(),
                null,
                0
        );

        mFlicksGridView = (GridView) rootView.findViewById(R.id.main_grid_view);

        switch (mCurrentMode) {
            case CURRENT_MODE_DB: {
                mFlicksGridView.setAdapter(mFlicksGridImagesCursorAdapter);
                break;
            }
            case CURRENT_MODE_OL: {
                mFlicksGridView.setAdapter(mFlicksGridImagesAdapter);
                break;
            }
        }

        mFlicksGridView.setChoiceMode(mFlicksGridView.CHOICE_MODE_SINGLE);
        mFlicksGridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                FlicksInitDetails flicks = flicksInitDetails.get(position);

                Intent detailActivityIntent = new Intent();
                detailActivityIntent.putExtra(AppConstants.ID, flicks.getID());
                detailActivityIntent.putExtra(AppConstants.BACKDROP_PATH, flicks.getBackgroundPath());
                detailActivityIntent.putExtra(AppConstants.POSTER_PATH, flicks.getImagesPath());
                detailActivityIntent.putExtra(AppConstants.OVERVIEW, flicks.getOverview());
                detailActivityIntent.putExtra(AppConstants.RELEASE_DATE, flicks.getReleaseDate());
                detailActivityIntent.putExtra(AppConstants.VOTE_AVG, flicks.getVoteAverage());
                detailActivityIntent.putExtra(AppConstants.VOTE_CNT, flicks.getVoteCount());
                detailActivityIntent.putExtra(AppConstants.POPULARITY, flicks.getPopularity());
                detailActivityIntent.putExtra(AppConstants.TITLE, flicks.getTitle());
                mPosition = position;
                FadFlickGridItemSelectedListener listener = (FadFlickGridItemSelectedListener) getActivity();
                listener.onGridItemSelected(detailActivityIntent);
            }
        });

        mFlicksGridView.setOnTouchListener(new View.OnTouchListener() {
            final static float DISTANCE_THRESH = 10;
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
                            if (mActionBar.isShowing())
                                mActionBar.hide();
                        } else if (upY - downY > DISTANCE_THRESH) { //going up
                            if (!mActionBar.isShowing())
                                mActionBar.show();
                        }
                        break;
                }
                return false;
            }
        });

        return rootView;
    }

    @Override
    public void updateGrid(ArrayList<FlicksInitDetails> flicksInitDetails) {
        this.flicksInitDetails = flicksInitDetails;
        updateGridBasedOnDataConnectionState();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_fad_flicks_fragment, menu);

        String opt = mSharedPref.getString(
                getString(R.string.pref_sort_order),
                getString(R.string.pref_sort_default_value));

        if (opt.equals(SORT_BY_POPULARITY)) {
            menu.findItem(R.id.action_popularity).setEnabled(false);
            menu.findItem(R.id.action_highest_rated).setEnabled(true);
            menu.findItem(R.id.action_favorites).setEnabled(true);
            mCurrentOrder = menu.findItem(R.id.action_popularity);
        } else if (opt.equals(SORT_BY_HIGHEST_RATED)) {
            menu.findItem(R.id.action_popularity).setEnabled(true);
            menu.findItem(R.id.action_highest_rated).setEnabled(false);
            menu.findItem(R.id.action_favorites).setEnabled(true);
            mCurrentOrder = menu.findItem(R.id.action_highest_rated);
        } else {
            menu.findItem(R.id.action_popularity).setEnabled(true);
            menu.findItem(R.id.action_highest_rated).setEnabled(true);
            menu.findItem(R.id.action_favorites).setEnabled(false);
            mCurrentOrder = menu.findItem(R.id.action_favorites);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int id = item.getItemId();
        if (id == R.id.action_popularity) {
            if (!mSharedPref.getString(getString(R.string.pref_sort_order),
                    getString(R.string.pref_sort_default_value)).equals(SORT_BY_POPULARITY)) {

                mIsDoneDownloadingData = false;
                item.setEnabled(false);
                mCurrentOrder.setEnabled(true);
                mCurrentOrder = item;
                mSharedPrefEditor.putString(
                        getString(R.string.pref_sort_order),
                        SORT_BY_POPULARITY);
                mSharedPrefEditor.commit();
                updateFlicks(mSharedPref.getString(
                        getString(R.string.pref_sort_order),
                        getString(R.string.pref_sort_default_value)));
                mCurrentMode = CURRENT_MODE_OL;
            }
            return true;
        } else if (id == R.id.action_highest_rated) {
            if (!mSharedPref.getString(getString(R.string.pref_sort_order),
                    getString(R.string.pref_sort_default_value)).equals(SORT_BY_HIGHEST_RATED)) {

                mIsDoneDownloadingData = false;
                item.setEnabled(false);
                mCurrentOrder.setEnabled(true);
                mCurrentOrder = item;
                mSharedPrefEditor.putString(
                        getString(R.string.pref_sort_order),
                        SORT_BY_HIGHEST_RATED);
                mSharedPrefEditor.commit();
                updateFlicks(mSharedPref.getString(
                        getString(R.string.pref_sort_order),
                        getString(R.string.pref_sort_default_value)));
                mCurrentMode = CURRENT_MODE_OL;
            }
            return true;
        } else if (id == R.id.action_favorites) {
            if (!mSharedPref.getString(getString(R.string.pref_sort_order),
                    getString(R.string.pref_sort_default_value)).equals(VIEW_FAVORITES)) {
                mIsDoneDownloadingData = false;
                item.setEnabled(false);
                mCurrentOrder.setEnabled(true);
                mCurrentOrder = item;
                mSharedPrefEditor.putString(
                        getString(R.string.pref_sort_order),
                        VIEW_FAVORITES);
                mSharedPrefEditor.commit();
                mPosition = 0;
                getLoaderManager().restartLoader(GRIDVIEW_LOADER, null, this);
                if (mFlicksGridView != null) {
                    mFlicksGridView.invalidate();
                    mFlicksGridView.setAdapter(mFlicksGridImagesCursorAdapter);
                }
                mNoDataTextView.setVisibility(View.INVISIBLE);
                mCurrentMode = CURRENT_MODE_DB;
                checkDbAndNotify();
            }
            return true;
        } else if (id == R.id.refresh) {
            if (!mSharedPref.getString(
                    getString(R.string.pref_sort_order),
                    getString(R.string.pref_sort_default_value)).equals(VIEW_FAVORITES)) {
                mIsDoneDownloadingData = false;
                updateFlicks(mSharedPref.getString(
                        getString(R.string.pref_sort_order),
                        getString(R.string.pref_sort_default_value)));
            } else {
                checkDbAndNotify();
            }
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (getLoaderManager().getLoader(GRIDVIEW_LOADER) == null) {
            getLoaderManager().initLoader(GRIDVIEW_LOADER, null, this);
        }
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

        mFlicksGridImagesCursorAdapter.swapCursor(c);
        if (mCurrentMode == CURRENT_MODE_DB) {
            ArrayList<FlicksInitDetails> favorites = new ArrayList<>();
            if (!c.moveToFirst()) {
                mNoFavsTextView.setVisibility(View.VISIBLE);
                mDbCount = 0;
            } else {
                mNoFavsTextView.setVisibility(View.INVISIBLE);
                mDbCount = c.getCount();
                final String FLICK_ID = FadFlicksContract.FlicksEntry.FLICK_ID;
                final String OVERVIEW = FadFlicksContract.FlicksEntry.OVERVIEW;
                final String TITLE = FadFlicksContract.FlicksEntry.TITLE;
                final String RELEASE_DATE = FadFlicksContract.FlicksEntry.RELEASE_DATE;
                final String VOTE_AVG = FadFlicksContract.FlicksEntry.VOTE_AVG;
                final String POPULARITY = FadFlicksContract.FlicksEntry.POPULARITY;

                do {
                    String flickId = c.getString(c.getColumnIndex(FLICK_ID));
                    favorites.add(new FlicksInitDetails(Integer.parseInt(flickId),
                            BACKDROP_FILE,
                            POSTER_FILE,
                            c.getString(c.getColumnIndex(OVERVIEW)),
                            c.getString(c.getColumnIndex(TITLE)),
                            c.getString(c.getColumnIndex(RELEASE_DATE)),
                            Float.parseFloat(c.getString(c.getColumnIndex(VOTE_AVG))),
                            0,
                            Float.parseFloat(c.getString(c.getColumnIndex(POPULARITY)))));
                } while (c.moveToNext());
            }
            flicksInitDetails = favorites;
            mFlicksGridView.invalidateViews();
            mFlicksGridView.setAdapter(mFlicksGridImagesCursorAdapter);

            if (mIsTwoPane) {
                if (mPosition != mFlicksGridView.INVALID_POSITION
                        && c.getCount() > 0
                        && mPosition < c.getCount()) {
                    selectPosItem(mPosition);
                }
                if (mPosition == c.getCount()
                        && mPosition - 1 != mFlicksGridView.INVALID_POSITION) {
                    selectPosItem(mPosition - 1);
                }
            }
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mFlicksGridImagesCursorAdapter.swapCursor(null);
    }

    public interface FadFlickGridItemSelectedListener {
        public void onGridItemSelected(Intent intent);
    }

    public interface DbEmptyListener {
        public void onDbEmpty(boolean isDbEmpty);
    }
}
