package com.hferoze.android.fadflicks.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class FlicksDBHelper extends SQLiteOpenHelper {

    static final String DATABASE_NAME = "favoriteflicks.db";
    static final String TABLE_NAME = "favoriteflickstable";
    static final int DATABASE_VERSION = 2;
    static final String CREATE_TABLE = " CREATE TABLE " +
            TABLE_NAME +
            " (" +
            FadFlicksContract.FlicksEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
            FadFlicksContract.FlicksEntry.FLICK_ID + " TEXT NOT NULL, " +
            FadFlicksContract.FlicksEntry.TITLE + " TEXT NOT NULL, " +
            FadFlicksContract.FlicksEntry.RELEASE_DATE + " TEXT NOT NULL, " +
            FadFlicksContract.FlicksEntry.VOTE_AVG + " TEXT NOT NULL, " +
            FadFlicksContract.FlicksEntry.RUNTIME + " TEXT NOT NULL, " +
            FadFlicksContract.FlicksEntry.GENRE + " TEXT NOT NULL, " +
            FadFlicksContract.FlicksEntry.POPULARITY + " TEXT NOT NULL, " +
            FadFlicksContract.FlicksEntry.OVERVIEW + " TEXT NOT NULL" +
            ");";

    public FlicksDBHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.w(FlicksDBHelper.class.getName(),
                "Upgrading database from version " + oldVersion + " to "
                        + newVersion + ". Old data will be destroyed");
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        onCreate(db);
    }
}
