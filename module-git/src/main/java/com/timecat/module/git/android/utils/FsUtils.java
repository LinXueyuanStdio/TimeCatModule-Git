package com.timecat.module.git.android.utils;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.webkit.MimeTypeMap;

import androidx.core.content.FileProvider;

import com.timecat.module.git.R;
import com.timecat.module.git.android.activities.SheimiFragmentActivity;
import com.timecat.module.git.sgit.ssh.PrivateKeyUtils;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Created by sheimi on 8/8/13.
 */
public class FsUtils {

    public static final SimpleDateFormat TIMESTAMP_FORMATTER = new SimpleDateFormat(
            "yyyyMMdd_HHmmss", Locale.getDefault());

    public static final String PROVIDER_AUTHORITY = "com.time.cat.file.provider";
    public static final String TEMP_DIR = "temp";
    private static final String LOGTAG = FsUtils.class.getSimpleName();

    private FsUtils() {
    }

    public static File createTempFile(String subfix) throws IOException {
        File dir = getExternalDir(TEMP_DIR);
        String fileName = TIMESTAMP_FORMATTER.format(new Date());
        File file = File.createTempFile(fileName, subfix, dir);
        file.deleteOnExit();
        return file;
    }

    /**
     * Get a File representing a dir within the external shared location where files can be stored specific to this app
     * creating the dir if it doesn't already exist
     *
     * @param dirname
     * @return
     */
    public static File getExternalDir(String dirname) {
        return getExternalDir(dirname, true);
    }

    /**
     * @param dirname
     * @return
     */
    public static File getInternalDir(String dirname) {
        return getExternalDir(dirname, true, false);
    }

    /**
     * Get a File representing a dir within the external shared location where files can be stored specific to this app
     *
     * @param dirname
     * @param isCreate create the dir if it does not already exist
     * @return
     */
    public static File getExternalDir(String dirname, boolean isCreate) {
        return getExternalDir(dirname, isCreate, true);
    }

    /**
     * Get a File representing a dir within the location where files can be stored specific to this app
     *
     * @param dirname    name of the dir to return
     * @param isCreate   create the dir if it does not already exist
     * @param isExternal if true, will use external *shared* storage
     * @return
     */
    public static File getExternalDir(String dirname, boolean isCreate, boolean isExternal) {
        File mDir = new File(getAppDir(isExternal), dirname);
        if (!mDir.exists() && isCreate) {
            mDir.mkdir();
        }
        return mDir;
    }

    /**
     * Get a File representing the location where files can be stored specific to this app
     *
     * @param isExternal if true, will use external *shared* storage
     * @return
     */
    public static File getAppDir(boolean isExternal) {
        SheimiFragmentActivity activeActivity = BasicFunctions.getActiveActivity();
        if (isExternal) {
            return activeActivity.getExternalFilesDir(null);
        } else {
            return activeActivity.getFilesDir();
        }
    }

    public static String getMimeType(String url) {
        String type = null;
        String extension = MimeTypeMap.getFileExtensionFromUrl(url
                .toLowerCase(Locale.getDefault()));
        if (extension != null) {
            MimeTypeMap mime = MimeTypeMap.getSingleton();
            type = mime.getMimeTypeFromExtension(extension);
        }
        if (type == null) {
            type = "text/plain";
        }
        return type;
    }

    public static String getMimeType(File file) {
        return getMimeType(Uri.fromFile(file).toString());
    }

    public static void openFile(SheimiFragmentActivity activity, File file) {
        //openFile(file, null);
        startActivityToEditFile(activity, file);
    }

    private static void startActivityToEditFile(SheimiFragmentActivity activity, File file) {
        Uri uri = FileProvider.getUriForFile(activity, PROVIDER_AUTHORITY, file);
        String mimeType = FsUtils.getMimeType(uri.toString());
        Intent editIntent = new Intent(Intent.ACTION_EDIT);
        editIntent.setDataAndType(uri, mimeType);
        editIntent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        try {
            activity.startActivity(Intent.createChooser(editIntent,
                    activity.getResources().getString(R.string.edit)));
        } catch (ActivityNotFoundException e) {
            activity.showMessageDialog(R.string.git_dialog_error_title, activity.getString(R.string.git_error_no_edit_app));
        }
    }

    public static void deleteFile(File file) {
        File to = new File(file.getAbsolutePath() + System.currentTimeMillis());
        file.renameTo(to);
        deleteFileInner(to);
    }

    private static void deleteFileInner(File file) {
        if (!file.isDirectory()) {
            file.delete();
            return;
        }
        try {
            PrivateKeyUtils.deleteDirectory(file);
        } catch (IOException e) {
            //TODO 
            e.printStackTrace();
        }
    }

    public static void copyFile(File from, File to) {
        try {
            PrivateKeyUtils.copyFile(from, to);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void copyDirectory(File from, File to) {
        if (!from.exists())
            return;
        try {
            PrivateKeyUtils.copyDirectory(from, to);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static boolean renameDirectory(File dir, String name) {
        String newDirPath = dir.getParent() + File.separator + name;
        File newDirFile = new File(newDirPath);

        return dir.renameTo(newDirFile);
    }

    public static String getRelativePath(File file, File base) {
        return base.toURI().relativize(file.toURI()).getPath();
    }

    public static File joinPath(File dir, String relative_path) {
        return new File(dir.getAbsolutePath() + File.separator + relative_path);
    }

}
