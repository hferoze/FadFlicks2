package com.hferoze.android.fadflicks.obj;

import android.net.Uri;

public class TrailerDetails {
    private String mTitle, mLink;
    private Uri mThumb;

    public TrailerDetails(String title, String link, Uri thumb) {
        this.mTitle = title;
        this.mLink = link;
        this.mThumb = thumb;
    }

    public String getTitle() {
        return mTitle;
    }

    public String getLink() {
        return mLink;
    }

    public Uri getThumb() {
        return mThumb;
    }
}
