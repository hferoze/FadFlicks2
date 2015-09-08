package com.hferoze.android.fadflicks.util;


import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.util.Log;

import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import java.io.File;
import java.io.FileOutputStream;

public class SaveFavoriteFlickImages implements Target {

    private static final String LOG_TAG = SaveFavoriteFlickImages.class.getSimpleName();

    private String mFolderName;
    private String mFileName;

    SaveFavoriteFlickImages(String folder, String fileName) {
        this.mFolderName = folder;
        this.mFileName = fileName;
    }

    @Override
    public void onBitmapLoaded(final Bitmap bitmap, Picasso.LoadedFrom from) {

        new Thread(new Runnable() {
            @Override
            public void run() {
                boolean success = true;
                File folder = new File(AppConstants.EXTERNAL_STORAGE + mFolderName + "/");
                if (!folder.exists()) {
                    Log.d(LOG_TAG, "creating: " + folder);
                    success = folder.mkdir();
                } else {
                    Log.d(LOG_TAG, "Folder exists: " + folder);
                }
                if (success) {
                    File file = new File(folder + "/" + mFileName);
                    Log.d(LOG_TAG, "saving image... " + file);
                    try {
                        file.createNewFile();
                        FileOutputStream ostream = new FileOutputStream(file);
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, ostream);
                        ostream.close();
                    } catch (Exception e) {
                        Log.e(LOG_TAG, "Error failed to save image... " + e.toString());
                    }
                } else {
                    Log.e(LOG_TAG, "Couldn't create directory to save images... ");
                }
            }
        }).start();
    }

    @Override
    public void onBitmapFailed(Drawable errorDrawable) {
    }

    @Override
    public void onPrepareLoad(Drawable placeHolderDrawable) {

    }
}
