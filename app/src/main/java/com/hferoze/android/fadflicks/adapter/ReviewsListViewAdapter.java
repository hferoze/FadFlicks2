package com.hferoze.android.fadflicks.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.hferoze.android.fadflicks.R;

import java.util.HashMap;

public class ReviewsListViewAdapter extends BaseAdapter {
    private Context mContext;
    private HashMap<String, String> mReviewData;
    private String[] mKeys;

    public ReviewsListViewAdapter(Context context,
                                  HashMap<String, String> reviewData) {
        this.mContext = context;
        this.mReviewData = reviewData;
        mKeys = mReviewData.keySet().toArray(new String[reviewData.size()]);
    }

    @Override
    public int getCount() {
        return mReviewData.size();
    }

    @Override
    public Object getItem(int position) {
        return mReviewData.get(mKeys[position]);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        ViewHolder viewHolder;

        if (convertView == null) {
            LayoutInflater inflater = LayoutInflater.from(mContext);
            convertView = inflater.inflate(R.layout.listview_item, parent, false);
            viewHolder = new ViewHolder();

            viewHolder.tvAuthor = (TextView) convertView.findViewById(R.id.reviewAuthorTextView);
            viewHolder.tvReview = (TextView) convertView.findViewById(R.id.reviewTextView);
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        viewHolder.tvReview.setText(getItem(position).toString());
        viewHolder.tvAuthor.setText("By: " + mKeys[position]);

        return convertView;
    }

    private static class ViewHolder {
        TextView tvAuthor;
        TextView tvReview;
    }
}
