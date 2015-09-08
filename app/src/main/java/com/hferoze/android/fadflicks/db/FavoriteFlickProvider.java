package com.hferoze.android.fadflicks.db;

import android.annotation.TargetApi;
import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.text.TextUtils;

import java.util.HashMap;

public class FavoriteFlickProvider extends ContentProvider {

    private static final UriMatcher sUriMatcher = buildUriMatcher();
    private FlicksDBHelper mOpenHelper;

    public static final int FLICKS = 100;
    public static final int FLICKS_ID = 101;

    private static HashMap<String, String> FlicksMap;
    private SQLiteDatabase database;

    private static final String sFlickIdSelection =
            FadFlicksContract.FlicksEntry.TABLE_NAME +
                    "." + FadFlicksContract.FlicksEntry.FLICK_ID + " = ? ";


    static UriMatcher buildUriMatcher() {
        final UriMatcher matcher = new UriMatcher(UriMatcher.NO_MATCH);
        final String authority = FadFlicksContract.CONTENT_AUTHORITY;

        matcher.addURI(authority, FadFlicksContract.PATH_FLICK, FLICKS);
        matcher.addURI(authority, FadFlicksContract.PATH_FLICK + "/#", FLICKS_ID);

        return matcher;
    }

    @Override
    public boolean onCreate() {
        mOpenHelper = new FlicksDBHelper(getContext());

        database = mOpenHelper.getWritableDatabase();

        if (database == null)
            return false;
        else
            return true;
    }

    @Override
    public String getType(Uri uri) {

        final int match = sUriMatcher.match(uri);

        switch (match) {

            case FLICKS:
                return FadFlicksContract.FlicksEntry.CONTENT_ITEM_TYPE;
            case FLICKS_ID:
                return FadFlicksContract.FlicksEntry.CONTENT_TYPE;
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection,
                        String[] selectionArgs, String sortOrder) {
        SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();
        queryBuilder.setTables(mOpenHelper.TABLE_NAME);

        switch (sUriMatcher.match(uri)) {
            case FLICKS:
                queryBuilder.setProjectionMap(FlicksMap);
                break;
            case FLICKS_ID:
                queryBuilder.appendWhere(FadFlicksContract.FlicksEntry._ID + "=" + uri.getLastPathSegment());
                break;
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }
        if (sortOrder == null || sortOrder == "") {
            sortOrder = FadFlicksContract.FlicksEntry._ID;
        }
        Cursor cursor = queryBuilder.query(
                database,
                projection,
                selection,
                selectionArgs,
                null,
                null,
                sortOrder);

        cursor.setNotificationUri(getContext().getContentResolver(), uri);

        return cursor;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        long row = database.insert(mOpenHelper.TABLE_NAME, "", values);
        if (row > 0) {
            Uri newUri = ContentUris.withAppendedId(FadFlicksContract.FlicksEntry.CONTENT_URI, row);
            getContext().getContentResolver().notifyChange(newUri, null);
            return newUri;
        }
        throw new SQLException("Fail to add a new record: " + uri);
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection,
                      String[] selectionArgs) {
        int res = 0;

        switch (sUriMatcher.match(uri)) {
            case FLICKS:
                res = database.update(mOpenHelper.TABLE_NAME, values, selection, selectionArgs);
                break;
            case FLICKS_ID:
                res = database.update(mOpenHelper.TABLE_NAME, values, FadFlicksContract.FlicksEntry._ID +
                        " = " + uri.getLastPathSegment() +
                        (!TextUtils.isEmpty(selection) ? " AND (" +
                                selection + ')' : ""), selectionArgs);
                break;
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }
        getContext().getContentResolver().notifyChange(uri, null);
        return res;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        int res = 0;

        switch (sUriMatcher.match(uri)) {
            case FLICKS:
                res = database.delete(mOpenHelper.TABLE_NAME, selection, selectionArgs);
                break;
            case FLICKS_ID:
                String id = uri.getLastPathSegment();
                res = database.delete(mOpenHelper.TABLE_NAME, FadFlicksContract.FlicksEntry._ID + " = " + id +
                        (!TextUtils.isEmpty(selection) ? " AND (" +
                                selection + ')' : ""), selectionArgs);
                break;
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }

        getContext().getContentResolver().notifyChange(uri, null);
        return res;
    }

    @Override
    @TargetApi(11)
    public void shutdown() {
        mOpenHelper.close();
        super.shutdown();
    }
}