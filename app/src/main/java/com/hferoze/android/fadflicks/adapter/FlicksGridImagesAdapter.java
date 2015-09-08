package com.hferoze.android.fadflicks.adapter;

import android.content.Context;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;

import com.hferoze.android.fadflicks.util.AppConstants;
import com.hferoze.android.fadflicks.R;
import com.hferoze.android.fadflicks.util.Utils;
import com.hferoze.android.fadflicks.obj.FlicksInitDetails;

import java.util.List;

public class FlicksGridImagesAdapter extends BaseAdapter {

    private List<FlicksInitDetails> mFlicksInitDetails;
    private Utils mUtils;
    private Context mContext;
    private boolean mLocal;

    public FlicksGridImagesAdapter(Context c,
                                   List<FlicksInitDetails> flicksInitDetailsArray, boolean local) {
        mContext = c;
        mUtils = new Utils(mContext);
        mFlicksInitDetails = flicksInitDetailsArray;
        mLocal = local;
    }

    @Override
    public int getCount() {
        return mFlicksInitDetails.size();
    }

    @Override
    public Object getItem(int arg0) {
        return mFlicksInitDetails.get(arg0);
    }

    @Override
    public long getItemId(int arg0) {
        return arg0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        ViewHolder viewHolder;

        if (convertView == null) {
            LayoutInflater inflater = LayoutInflater.from(mContext);
            convertView = inflater.inflate(R.layout.grid_images_layout, parent, false);

            viewHolder = new ViewHolder();
            viewHolder.ivFlickPoster = (ImageView) convertView.findViewById(R.id.gridview_imageView);
            viewHolder.rbFlickRating = (RatingBar) convertView.findViewById(R.id.gridview_ratingBar);
            viewHolder.tvFlickReleasDate = (TextView) convertView.findViewById(R.id.gridview_releaseDateTextView);
            viewHolder.tvFlickTitle = (TextView) convertView.findViewById(R.id.gridview_flickTitleTextView);
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        FlicksInitDetails flicks = mFlicksInitDetails.get(position);

        if (!mLocal) {
            Uri imgUrl = mUtils.buildURI(AppConstants.REMOTE_IMAGE_SIZES[AppConstants.GRID_IMAGE_SIZE_IDX], flicks.getImagesPath());
            mUtils.setImages(imgUrl, viewHolder.ivFlickPoster);
        } else {
            Uri imgUrl = mUtils.buildLocalImageUri(Integer.toString(flicks.getID()), flicks.getImagesPath());
            mUtils.setImages(imgUrl, viewHolder.ivFlickPoster);
        }

        viewHolder.tvFlickTitle.setText(flicks.getTitle());
        viewHolder.tvFlickTitle.setSelected(true);
        viewHolder.rbFlickRating.setRating((float) (flicks.getVoteAverage() * AppConstants.RATING_RANGE) / AppConstants.RATING_NORM);
        viewHolder.tvFlickReleasDate.setText(mUtils.getYear(flicks.getReleaseDate()));
        return convertView;
    }

    private static class ViewHolder {
        ImageView ivFlickPoster;
        RatingBar rbFlickRating;
        TextView tvFlickReleasDate;
        TextView tvFlickTitle;
    }
}
