package com.hferoze.android.fadflicks.util;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.util.DisplayMetrics;
import android.widget.ImageView;

import com.hferoze.android.fadflicks.R;
import com.squareup.picasso.NetworkPolicy;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class Utils {

    private Context mContext;

    public Utils(Context ctx) {
        mContext = ctx;
    }

    public boolean isDataAvaialable() {
        ConnectivityManager cm =
                (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        boolean isDataConnected = activeNetwork != null &&
                activeNetwork.isConnectedOrConnecting();
        if (isDataConnected) {
            if (activeNetwork.getType() == ConnectivityManager.TYPE_MOBILE
                    || activeNetwork.getType() == ConnectivityManager.TYPE_WIFI) {
                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    public String getYear(String date) {
        if (!date.equals("null") &&
                date.length() >= AppConstants.YEAR_STR_LENGTH && date != null) {
            if (date.substring(0, AppConstants.YEAR_STR_LENGTH).matches("^-?\\d+$"))
                return date.substring(0, AppConstants.YEAR_STR_LENGTH);
        }
        return "";
    }

    public static float getDpi(float px, Context context) {
        Resources resources = context.getResources();
        DisplayMetrics metrics = resources.getDisplayMetrics();
        float dpi = px / (metrics.densityDpi / 160f);
        return dpi;
    }

    public boolean isIntentSafe(Intent intent) {
        //From developer.android.com
        PackageManager packageManager = mContext.getPackageManager();
        List<ResolveInfo> activities = packageManager.queryIntentActivities(intent, 0);
        return activities.size() > 0;
    }

    public boolean deleteFavoriteFlicksImagesFolder(File dir) {
        if (dir.isDirectory()) {
            String[] files = dir.list();
            for (int i = 0; i < files.length; i++) {
                boolean success = deleteFavoriteFlicksImagesFolder(new File(dir, files[i]));
                if (!success) {
                    return false;
                }
            }
        }
        return dir.delete();
    }

    public void makeFadFlickFolder(String folderName)
            throws IOException {
        File folder = new File(folderName);
        if (!folder.exists())
            folder.mkdir();

    }

    public void saveImagesToDisk(String urlPath, String fileName, String id) {
        SaveFavoriteFlickImages target =
                new SaveFavoriteFlickImages(AppConstants.FOLDER_NAME + id, fileName);
        Picasso.
                with(mContext).
                load(urlPath).
                into(target);
    }

    public Uri buildLocalImageUri(String flickId, String imagePath) {
        Uri.Builder imgUri = new Uri.Builder();
        return imgUri.scheme("file").
                appendPath(AppConstants.EXTERNAL_STORAGE.toString()).
                appendPath(AppConstants.FOLDER_NAME).
                appendPath(flickId).
                appendEncodedPath(imagePath).build();
    }

    public Uri buildURI(String img_size, String img_path) {
        Uri.Builder builder = new Uri.Builder();
        builder.scheme("http")
                .authority("image.tmdb.org")
                .appendPath("t")
                .appendPath("p")
                .appendPath(img_size)
                .appendEncodedPath(img_path)
                .build();
        return builder.build();
    }

    public void setBackgroundImages(Uri path, ImageView iv) {
        Picasso.with(mContext).
                load(path).
                networkPolicy(isDataAvaialable() ? NetworkPolicy.NO_CACHE : NetworkPolicy.OFFLINE)
                .error(R.mipmap.error_img)
                .fit()
                .centerCrop()
                .into(iv);
    }

    public void setImages(Uri path, ImageView iv) {
        Picasso.with(mContext).
                load(path).
                networkPolicy(isDataAvaialable() ? NetworkPolicy.NO_CACHE : NetworkPolicy.OFFLINE)
                .error(R.mipmap.error_img)
                .into(iv);
    }
}
