package com.timecat.module.git.android.utils;

import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

import com.timecat.layout.ui.utils.IconLoader;
import com.timecat.module.git.android.activities.SheimiFragmentActivity;
import com.timecat.module.git.dialogs.ExceptionDialog;

import org.jetbrains.annotations.NotNull;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import timber.log.Timber;

/**
 * Created by sheimi on 8/19/13.
 */
public class BasicFunctions {

    public static String md5(final String s) {
        try {
            // Create MD5 Hash
            MessageDigest digest = java.security.MessageDigest
                    .getInstance("MD5");
            digest.update(s.getBytes());
            byte messageDigest[] = digest.digest();

            // Create Hex String
            StringBuffer hexString = new StringBuffer();
            for (int i = 0; i < messageDigest.length; ++i) {
                String h = Integer.toHexString(0xFF & messageDigest[i]);
                while (h.length() < 2)
                    h = "0" + h;
                hexString.append(h);
            }
            return hexString.toString();

        } catch (NoSuchAlgorithmException e) {
            Timber.e(e);
        }
        return "";
    }

    public static void setAvatarImage(ImageView imageView, String email) {
        String avatarUri = "";
        if (!email.isEmpty())
            avatarUri = IconLoader.AVATAR_SCHEME + md5(email);

        IconLoader.loadIcon(getActiveActivity(),imageView,avatarUri);
    }

    private static SheimiFragmentActivity mActiveActivity;

    public static SheimiFragmentActivity getActiveActivity() {
        return mActiveActivity;
    }

    public static void setActiveActivity(SheimiFragmentActivity activity) {
        mActiveActivity = activity;
    }

    public static void showException(@NonNull @NotNull SheimiFragmentActivity activity, Throwable throwable, @StringRes final int errorTitleRes, @StringRes final int errorRes) {
        ExceptionDialog exceptionDialog = new ExceptionDialog();
        exceptionDialog.setThrowable(throwable);
        exceptionDialog.setErrorRes(errorRes);
        exceptionDialog.setErrorTitleRes(errorTitleRes);
        exceptionDialog.show(activity.getSupportFragmentManager(), "exception-dialog");
    }


    public static void showException(@NonNull @NotNull SheimiFragmentActivity activity, @NonNull Throwable throwable, @StringRes final int errorRes) {
        showException(activity, throwable, 0, errorRes);
    }

    public static void showException(@NonNull @NotNull SheimiFragmentActivity activity, @NonNull Throwable throwable) {
        showException(activity, throwable, 0);
    }
}
