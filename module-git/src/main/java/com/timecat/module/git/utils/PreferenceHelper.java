package com.timecat.module.git.utils;

import android.content.SharedPreferences;

import com.timecat.component.setting.DEF;
import com.timecat.extend.arms.BaseApplication;
import com.timecat.module.git.R;

import java.io.File;

/**
 * Clean, central access to getting/setting user preferences
 */

public class PreferenceHelper {
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
    }


    public SharedPreferences getSharedPrefs() {
        return DEF.git();
    }

    private void edit(String name, String value) {
        SharedPreferences.Editor editor = getSharedPrefs().edit();
        editor.putString(name, value);
        editor.apply();
    }

    private String getString(String name) {
        return getSharedPrefs().getString(name, DEFAULT_STRING);
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
