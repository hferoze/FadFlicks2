package com.hferoze.android.fadflicks.util;

import android.os.Environment;

import java.io.File;

public class AppConstants {

    public static final File EXTERNAL_STORAGE = Environment.getExternalStorageDirectory();

    public static final String FOLDER_NAME = "/FadFlicks/";
    public static final String ID = "id";
    public static final String BACKDROP_PATH = "backdrop_path";
    public static final String POSTER_PATH = "image_path";
    public static final String OVERVIEW = "overview";
    public static final String RELEASE_DATE = "release_date";
    public static final String VOTE_AVG = "vote_avg";
    public static final String VOTE_CNT = "vote_count";
    public static final String POPULARITY = "populatiry";
    public static final String TITLE = "title";
    public static final String JPG_EXT = ".jpg";

    public static final int GRID_IMAGE_SIZE_IDX = 2;
    public static final int DETAIL_POSTER_IMG_SIZE_IDX = 3;
    public static final int DETAIL_BACKDROP_IMG_SIZE_IDX = 4;

    public static final int RATING_RANGE = 5;
    public static final int RATING_NORM = 10;

    public static final int REQ_TYPE_DETAILS = 0;
    public static final int REQ_TYPE_TRAILERS = 1;
    public static final int REQ_TYPE_REVIEWS = 2;


    public static final int YEAR_STR_LENGTH = 4;
    public static final String[] REMOTE_IMAGE_SIZES = {"w92", "w154", "w185", "w342", "w500", "w780", "original"};

    private AppConstants() {
    }
}
