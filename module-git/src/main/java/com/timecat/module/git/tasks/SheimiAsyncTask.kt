package com.timecat.module.git.tasks

import android.os.AsyncTask
import androidx.annotation.StringRes
import com.timecat.module.git.R

abstract class SheimiAsyncTask<A, B, C> : AsyncTask<A, B, C>() {
    @JvmField
    protected var mException: Throwable? = null
    @JvmField
    protected var mErrorRes = 0
    protected fun setException(e: Throwable?) {
        mException = e
    }

    protected fun setException(e: Throwable?, errorRes: Int) {
        mException = e
        mErrorRes = errorRes
    }

    var isTaskCanceled = false
        private set

    open fun cancelTask() {
        isTaskCanceled = true
    }
    //TODO(kaeptmblaubaer1000): maybe make abstract?
    /**
     * This method is to be overridden and should return the resource that
     * is used as the title as the
     * [com.timecat.module.git.dialogs.ExceptionDialog] title when the
     * task fails with an exception.
     */
    @get:StringRes
    open val errorTitleRes: Int
        get() = R.string.git_dialog_error_title

    interface AsyncTaskPostCallback {
        fun onPostExecute(isSuccess: Boolean?)
    }

    interface AsyncTaskCallback {
        fun doInBackground(vararg params: Void?): Boolean
        fun onPreExecute()
        fun onProgressUpdate(vararg progress: String?)
        fun onPostExecute(isSuccess: Boolean?)
    }
}