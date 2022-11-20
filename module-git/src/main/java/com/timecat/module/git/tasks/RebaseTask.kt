package com.timecat.module.git.tasks

import com.timecat.module.git.R
import com.timecat.module.git.sgit.database.models.Repo
import com.timecat.module.git.utils.StopTaskException

class RebaseTask(repo: Repo, var mUpstream: String, private val mCallback: AsyncTaskPostCallback?) : RepoOpTask(repo) {
    init {
        setSuccessMsg(R.string.git_success_rebase)
    }

    override fun doInBackground(vararg params: Void): Boolean {
        return rebase()
    }

    override fun onPostExecute(isSuccess: Boolean) {
        super.onPostExecute(isSuccess)
        mCallback?.onPostExecute(isSuccess)
    }

    fun rebase(): Boolean {
        try {
            val git = mRepo.getGit() ?: return false
            git.rebase().setUpstream(mUpstream).call()
        } catch (e: StopTaskException) {
            return false
        } catch (e: Throwable) {
            setException(e)
            return false
        }
        return true
    }
}