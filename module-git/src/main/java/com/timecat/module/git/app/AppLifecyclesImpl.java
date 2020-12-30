/*
 * Copyright 2017 JessYan
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.timecat.module.git.app;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;

import com.jess.arms.base.delegate.AppLifecycles;
import com.timecat.module.git.BuildConfig;
import com.timecat.module.git.R;
import com.timecat.module.git.sgit.preference.PreferenceHelper;
import com.timecat.module.git.transport.MGitHttpConnectionFactory;

/**
 * ================================================ 展示 {@link AppLifecycles} 的用法
 * <p>
 * Created by JessYan on 04/09/2017 17:12
 * <a href="mailto:jess.yan.effort@gmail.com">Contact me</a>
 * <a href="https://github.com/JessYanCoding">Follow me</a>
 * ================================================
 */
public class AppLifecyclesImpl implements AppLifecycles {

    static {
        MGitHttpConnectionFactory.install();
    }

    @Override
    public void attachBaseContext(@NonNull Context base) {

    }

    @Override
    public void onCreate(@NonNull Application application) {
        setAppVersionPref(application);
    }

    @Override
    public void onTerminate(@NonNull Application application) {

    }

    private void setAppVersionPref(@NonNull Application application) {
        SharedPreferences sharedPreference = PreferenceHelper.getInstance().getSharedPrefs();
        String version = BuildConfig.VERSION_NAME;
        sharedPreference
                .edit()
                .putString(application.getString(R.string.git_preference_key_app_version), version)
                .apply();
    }
}
