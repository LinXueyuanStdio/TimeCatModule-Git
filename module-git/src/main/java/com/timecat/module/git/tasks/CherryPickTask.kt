package com.timecat.module.git.tasks

import com.timecat.module.git.R
import com.timecat.module.git.sgit.database.models.Repo
import com.timecat.module.git.utils.StopTaskException

class CherryPickTask(
    repo: Repo, var mCommitStr: String,
    private val mCallback: AsyncTaskPostCallback?
) : RepoOpTask(repo) {
    init {
        setSuccessMsg(R.string.git_success_cherry_pick)
    }

    override fun doInBackground(vararg params: Void): Boolean {
        return cherrypick()
    }

    override fun onPostExecute(isSuccess: Boolean) {
        super.onPostExecute(isSuccess)
        mCallback?.onPostExecute(isSuccess)
    }

    fun cherrypick(): Boolean {
        try {
            val git = mRepo.getGit() ?: return false
            val commit = git.repository.resolve(mCommitStr)
            git.cherryPick().include(commit).call()
        } catch (e: StopTaskException) {
            return false
        } catch (e: Throwable) {
            setException(e)
            return false
        }
        return true
    }
}