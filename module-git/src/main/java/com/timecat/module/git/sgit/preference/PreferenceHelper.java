package com.timecat.module.git.sgit.preference;

import android.content.SharedPreferences;

import com.timecat.extend.arms.BaseApplication;
import com.timecat.component.setting.DEF;
import com.timecat.module.git.R;

import java.io.File;

import timber.log.Timber;

/**
 * Clean, central access to getting/setting user preferences
 */

public class PreferenceHelper {

    private static final int DEFAULT_INT = 0;
    private static final boolean DEFAULT_BOOLEAN = false;
    private static final String DEFAULT_STRING = "";

    /**
     * Returns the root directory on device storage in which new local repos will be stored
     *
     * @return null if the custom repo location user preference is *not* set
     */
    public File getRepoRoot() {
        String repoRootDir = getString(BaseApplication.getContext()
                .getString(R.string.git_pref_key_repo_root_location));
        if (repoRootDir != null && !repoRootDir.isEmpty()) {
            return new File(repoRootDir);
        } else {
            return null;
        }
    }

    public void setRepoRoot(String repoRootPath) {
        edit(BaseApplication.getContext().getString(R.string.git_pref_key_repo_root_location), repoRootPath);
        Timber.d("set root:" + repoRootPath);
    }


    public SharedPreferences getSharedPrefs() {
        return DEF.git();
    }


    private void edit(String name, String value) {
        SharedPreferences.Editor editor = getSharedPrefs().edit();
        editor.putString(name, value);
        editor.apply();
    }

    private void edit(String name, int value) {
        SharedPreferences.Editor editor = getSharedPrefs().edit();
        editor.putInt(name, value);
        editor.apply();
    }

    private void edit(String name, boolean value) {
        SharedPreferences.Editor editor = getSharedPrefs().edit();
        editor.putBoolean(name, value);
        editor.apply();
    }

    private String getString(String name) {
        return getSharedPrefs().getString(name, DEFAULT_STRING);
    }

    private int getInt(String name) {
        return getSharedPrefs().getInt(name, DEFAULT_INT);
    }

    private boolean getBoolean(String name) {
        return getSharedPrefs().getBoolean(name, DEFAULT_BOOLEAN);
    }

    private volatile static PreferenceHelper instance = null;

    private PreferenceHelper() {
    }

    public static PreferenceHelper getInstance() {
        if (instance == null) {
            synchronized (PreferenceHelper.class) {
                if (instance == null) {
                    instance = new PreferenceHelper();
                }
            }
        }

        return instance;
    }
}
