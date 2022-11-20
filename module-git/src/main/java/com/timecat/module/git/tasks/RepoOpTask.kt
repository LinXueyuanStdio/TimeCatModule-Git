package com.timecat.module.git.tasks

import com.timecat.component.commonsdk.utils.override.LogUtil
import com.timecat.element.alert.ToastUtil
import com.timecat.module.git.R
import com.timecat.module.git.sgit.activities.SheimiFragmentActivity.OnPasswordEntered
import com.timecat.module.git.sgit.database.models.Repo
import com.timecat.module.git.utils.BasicFunctions
import com.timecat.module.git.utils.BasicFunctions.activeActivity
import org.eclipse.jgit.api.TransportCommand
import org.eclipse.jgit.lib.ProgressMonitor
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider
import java.util.*

abstract class RepoOpTask(protected var mRepo: Repo) : SheimiAsyncTask<Void, String, Boolean>() {
    protected var mIsTaskAdded: Boolean
    private var mSuccessMsg = 0

    init {
        mIsTaskAdded = mRepo.addTask(this)
    }

    protected override fun onPostExecute(isSuccess: Boolean) {
        super.onPostExecute(isSuccess)
        mRepo.removeTask(this)
        if (!isSuccess && !isTaskCanceled) {
            BasicFunctions.showException(activeActivity!!, mException, mErrorRes, errorTitleRes)
        }
        if (isSuccess && mSuccessMsg != 0) {
            ToastUtil.ok_long(mSuccessMsg)
        }
    }

    protected fun setSuccessMsg(successMsg: Int) {
        mSuccessMsg = successMsg
    }

    open fun executeTask() {
        if (mIsTaskAdded) {
            execute()
            return
        }
        ToastUtil.w_long(R.string.git_error_task_running)
    }

    protected fun setCredentials(command: TransportCommand<*, *>) {
        val username = mRepo.username
        val password = mRepo.password
        if (username != null && password != null && !username.trim { it <= ' ' }.isEmpty()
            && !password.trim { it <= ' ' }.isEmpty()
        ) {
            val auth = UsernamePasswordCredentialsProvider(
                username, password
            )
            command.setCredentialsProvider(auth)
        } else {
            LogUtil.d("no CredentialsProvider when no username/password provided")
        }
    }

    protected fun handleAuthError(onPassEntered: OnPasswordEntered) {
        val msg = mException!!.message
        LogUtil.w("clone Auth error: $msg")
        if (msg == null || (!msg.contains("Auth fail")
                && !msg.lowercase(Locale.getDefault()).contains("auth"))
        ) {
            return
        }
        var errorInfo: String? = null
        if (msg.contains("Auth fail")) {
            errorInfo = activeActivity!!.getString(
                R.string.git_dialog_prompt_for_password_title_auth_fail
            )
        }
        activeActivity?.promptForPassword(onPassEntered, errorInfo)
    }

    internal inner class BasicProgressMonitor : ProgressMonitor {
        private var mTotalWork = 0
        private var mWorkDone = 0
        private var mLastProgress = 0
        private var mTitle: String? = null
        override fun start(i: Int) {}
        override fun beginTask(title: String, totalWork: Int) {
            mTotalWork = totalWork
            mWorkDone = 0
            mLastProgress = 0
            mTitle = title
            setProgress()
        }

        override fun update(i: Int) {
            mWorkDone += i
            if (mTotalWork != ProgressMonitor.UNKNOWN && mTotalWork != 0 && mTotalWork - mLastProgress >= 1) {
                setProgress()
                mLastProgress = mWorkDone
            }
        }

        override fun endTask() {}
        override fun isCancelled(): Boolean {
            return isTaskCanceled
        }

        private fun setProgress() {
            val msg = mTitle
            val showedWorkDown = Math.min(mWorkDone, mTotalWork)
            var progress = 0
            var rightHint = "0/0"
            var leftHint = "0%"
            if (mTotalWork != 0) {
                progress = 100 * showedWorkDown / mTotalWork
                rightHint = "$showedWorkDown/$mTotalWork"
                leftHint = "$progress%"
            }
            publishProgress(
                msg, leftHint, rightHint,
                progress.toString()
            )
        }
    }
}