package com.timecat.module.git.android.utils;

import android.content.Context;
import android.content.SharedPreferences;

import com.timecat.module.git.R;
import com.timecat.module.git.sgit.database.models.Repo;
import com.timecat.module.git.sgit.preference.PreferenceHelper;

/**
 * Created by lee on 2015-02-01.
 */
public class Profile {

    private static boolean sHasLastCloneFail = false;
    private static Repo sLastFailRepo;

    private static SharedPreferences getProfileSharedPreference() {
        return PreferenceHelper.getInstance().getSharedPrefs();
    }

    public static String getUsername(Context context) {
        String userNamePrefKey = context.getString(R.string.git_pref_key_git_user_name);
        return getProfileSharedPreference().getString(userNamePrefKey, "");
    }

    public static String getEmail(Context context) {
        String userEmailPrefKey = context.getString(R.string.git_pref_key_git_user_email);
        return getProfileSharedPreference().getString(userEmailPrefKey, "");
    }

    public static boolean hasLastCloneFailed() {
        return sHasLastCloneFail;
    }

    public static Repo getLastCloneTryRepo() {
        return sLastFailRepo;
    }

    public static void setLastCloneFailed(Repo repo) {
        sHasLastCloneFail = true;
        sLastFailRepo = repo;
    }

    public static void setLastCloneSuccess() {
        sHasLastCloneFail = false;
    }

    public static String getCodeMirrorTheme(Context context) {
        final String[] themes = context.getResources().getStringArray(R.array.git_codemirror_theme_names);
        return themes[0];
    }
}
