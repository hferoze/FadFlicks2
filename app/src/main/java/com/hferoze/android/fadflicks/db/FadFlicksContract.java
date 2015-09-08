package com.hferoze.android.fadflicks.db;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.net.Uri;
import android.provider.BaseColumns;

public class FadFlicksContract {


    public static final String CONTENT_AUTHORITY = "com.hferoze.android.provider.FavoriteFlick";

    public static final Uri BASE_CONTENT_URI = Uri.parse("content://" + CONTENT_AUTHORITY);

    public static final String PATH_FLICK = "flick";

    public static final class FlicksEntry implements BaseColumns {

        public static final Uri CONTENT_URI =
                BASE_CONTENT_URI.buildUpon().appendPath(PATH_FLICK).build();

        public static final String CONTENT_TYPE =
                ContentResolver.CURSOR_DIR_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + PATH_FLICK;
        public static final String CONTENT_ITEM_TYPE =
                ContentResolver.CURSOR_ITEM_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + PATH_FLICK;
        public static final String TABLE_NAME = "favoriteflickstable";
        public static final String ID = "id";
        public static final String FLICK_ID = "flick_id";
        public static final String TITLE = "title";
        public static final String RELEASE_DATE = "release_date";
        public static final String VOTE_AVG = "vote_avg";
        public static final String RUNTIME = "runtime";
        public static final String GENRE = "genre";
        public static final String POPULARITY = "popularity";
        public static final String OVERVIEW = "overview";
    }
}

