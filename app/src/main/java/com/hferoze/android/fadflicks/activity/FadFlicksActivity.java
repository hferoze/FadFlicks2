package com.hferoze.android.fadflicks.activity;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Handler;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.RelativeLayout;

import com.hferoze.android.fadflicks.R;
import com.hferoze.android.fadflicks.fragment.FadFlicksFragment;
import com.hferoze.android.fadflicks.fragment.FlickDetailFragment;

public class FadFlicksActivity extends ActionBarActivity
        implements FadFlicksFragment.FadFlickGridItemSelectedListener,
        FadFlicksFragment.DbEmptyListener {

    private static final String DETAILFRAGMENT_TAG = "FLICKDETAILTAG";
    private static final String IS_FROM_ACTIVITY = "from_activity";
    private static final String IS_DB_EMPTY = "is_db_empty";
    public static final int LAUNCH_WAIT = 1500;

    private RelativeLayout mSplashView;

    private FlickDetailFragment mFlickDetailFragment;
    private FadFlicksFragment mFadFlicksFragment;

    private boolean mIsTwoPane;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        supportRequestWindowFeature(Window.FEATURE_ACTION_BAR_OVERLAY);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fad_flicks);

        mSplashView = (RelativeLayout) findViewById(R.id.splash_view);

        getSupportActionBar().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        getSupportActionBar().setDisplayOptions(0, ActionBar.DISPLAY_SHOW_TITLE);
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        mFadFlicksFragment = ((FadFlicksFragment) getSupportFragmentManager()
                .findFragmentById(R.id.gridFragment));

        mFlickDetailFragment = new FlickDetailFragment();

        View fragView = this.findViewById(R.id.detailFragment);
        mIsTwoPane = fragView != null && fragView.getVisibility() == View.VISIBLE;

        mFadFlicksFragment.setIsTwoPane(mIsTwoPane);

        if (!mIsTwoPane) {
            if (mFadFlicksFragment.mActivityLaunchPost) {
                mSplashView.setVisibility(View.INVISIBLE);
            } else {
                launchSplash();
            }
        } else {
            if (mFadFlicksFragment.mActivityLaunchPost) {
                mSplashView.setVisibility(View.INVISIBLE);
            }
            launchSplash();

            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.detailFragment, mFlickDetailFragment, DETAILFRAGMENT_TAG)
                    .commit();
        }
    }

    /*
    * Send movie information to details view
    */
    @Override
    public void onGridItemSelected(Intent intent) {

        if (mIsTwoPane) {
            Bundle args = new Bundle();
            args.putBundle(FlickDetailFragment.INTENT_BUNDLE, intent.getExtras());
            args.putBoolean(IS_FROM_ACTIVITY, false);
            FlickDetailFragment fragment = new FlickDetailFragment();
            fragment.setArguments(args);
            getSupportFragmentManager().beginTransaction()
                    .setTransitionStyle(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                    .replace(R.id.detailFragment, fragment, DETAILFRAGMENT_TAG)
                    .commit();
        } else {
            Intent detailActivityIntent = new Intent(this, FlickDetailActivity.class);
            detailActivityIntent.putExtras(intent);
            detailActivityIntent.putExtra(IS_FROM_ACTIVITY, true);
            startActivity(detailActivityIntent);
        }
    }

    /*
    * Launch Splash screen
    */
    private void launchSplash() {
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                clearSplash(mSplashView);
            }
        }, LAUNCH_WAIT);
    }

    /*
    * Clear Splash screen
    * @param splash: The splash view RelativeLayout
    */
    private void clearSplash(final RelativeLayout splash) {

        if (splash != null) {
            ObjectAnimator animX = ObjectAnimator.ofFloat(splash, "alpha", 1, 0);
            animX.setDuration(700);
            animX.start();
            animX.addListener(new Animator.AnimatorListener() {
                @Override
                public void onAnimationStart(Animator animation) {
                }

                @Override
                public void onAnimationEnd(Animator animation) {
                    splash.setVisibility(View.INVISIBLE);
                }

                @Override
                public void onAnimationCancel(Animator animation) {
                }

                @Override
                public void onAnimationRepeat(Animator animation) {
                }
            });
        }
    }

    /*
    * Clear Splash screen
    * @param isDbEmpty: boolean value, true if database is empty
    */
    @Override
    public void onDbEmpty(boolean isDbEmpty) {
        if (mIsTwoPane) {
            Bundle args = new Bundle();
            args.putBoolean(IS_DB_EMPTY, isDbEmpty);
            FlickDetailFragment fragment = new FlickDetailFragment();
            fragment.setArguments(args);
            getSupportFragmentManager().beginTransaction()
                    .setTransitionStyle(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                    .replace(R.id.detailFragment, fragment, DETAILFRAGMENT_TAG)
                    .commit();
        }
    }
}
