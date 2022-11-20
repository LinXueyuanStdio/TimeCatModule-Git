package com.timecat.module.git.sgit.activities.delegate.actions

import com.timecat.module.git.R
import com.timecat.module.git.sgit.activities.SheimiFragmentActivity.OnPositiveClickListener
import com.timecat.module.git.sgit.activities.RepoDetailActivity
import com.timecat.module.git.sgit.database.models.Repo
import com.timecat.module.git.tasks.SheimiAsyncTask.AsyncTaskPostCallback
import com.timecat.module.git.tasks.ResetCommitTask

class ResetAction(repo: Repo, activity: RepoDetailActivity) : RepoAction(repo, activity) {
    override fun execute() {
        mActivity.showMessageDialog(R.string.git_dialog_reset_commit_title,
            R.string.git_dialog_reset_commit_msg, R.string.git_action_reset, object : OnPositiveClickListener {
                override fun onClick() {
                    reset()
                }
            })
    }

    fun reset() {
        val resetTask = ResetCommitTask(mRepo,
            object : AsyncTaskPostCallback {
                override fun onPostExecute(isSuccess: Boolean?) {
                    mActivity.reset()
                }
            })
        resetTask.executeTask()
    }
}