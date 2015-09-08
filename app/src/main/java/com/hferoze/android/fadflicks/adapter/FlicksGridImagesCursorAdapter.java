package com.hferoze.android.fadflicks.adapter;

import android.content.Context;
import android.database.Cursor;
import android.database.StaleDataException;
import android.net.Uri;
import android.support.v4.widget.CursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;

import com.hferoze.android.fadflicks.R;
import com.hferoze.android.fadflicks.fragment.FadFlicksFragment;
import com.hferoze.android.fadflicks.obj.FlicksInitDetails;
import com.hferoze.android.fadflicks.util.AppConstants;
import com.hferoze.android.fadflicks.util.Utils;

public class FlicksGridImagesCursorAdapter extends CursorAdapter {

    private Utils mUtils;
    private Context mContext;

    public FlicksGridImagesCursorAdapter(Context context, Cursor c, int flags) {
        super(context, c, flags);
        mContext = context;
        mUtils = new Utils(mContext);
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {

        View view = LayoutInflater.from(mContext).inflate(R.layout.grid_images_layout, parent, false);

        ViewHolder viewHolder = new ViewHolder(view);
        view.setTag(viewHolder);

        return view;
    }

    @Override
    public void bindView(View view, Context context, Cursor c) {
        ViewHolder viewHolder = (ViewHolder) view.getTag();

        try {
            if (FadFlicksFragment.flicksInitDetails.size() > 0 && c != null && c.getCount() > 0) {
                FlicksInitDetails flicks = FadFlicksFragment.flicksInitDetails.get(c.getPosition());

                Uri imgUrl = mUtils.buildLocalImageUri(Integer.toString(flicks.getID()), flicks.getImagesPath());
                mUtils.setImages(imgUrl, viewHolder.ivFlickPoster);

                viewHolder.tvFlickTitle.setText(flicks.getTitle());
                viewHolder.tvFlickTitle.setSelected(true);
                viewHolder.rbFlickRating.setRating((float) (flicks.getVoteAverage() * AppConstants.RATING_RANGE) / AppConstants.RATING_NORM);
                viewHolder.tvFlickReleasDate.setText(mUtils.getYear(flicks.getReleaseDate()));
            }
        } catch (StaleDataException e) {
            e.printStackTrace();
        }
    }

    private static class ViewHolder {
        ImageView ivFlickPoster;
        RatingBar rbFlickRating;
        TextView tvFlickReleasDate;
        TextView tvFlickTitle;

        public ViewHolder(View view) {
            ivFlickPoster = (ImageView) view.findViewById(R.id.gridview_imageView);
            rbFlickRating = (RatingBar) view.findViewById(R.id.gridview_ratingBar);
            tvFlickReleasDate = (TextView) view.findViewById(R.id.gridview_releaseDateTextView);
            tvFlickTitle = (TextView) view.findViewById(R.id.gridview_flickTitleTextView);
        }
    }
}
