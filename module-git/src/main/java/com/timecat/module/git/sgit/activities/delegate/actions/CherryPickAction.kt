package com.timecat.module.git.sgit.activities.delegate.actions

import com.timecat.module.git.R
import com.timecat.module.git.sgit.activities.SheimiFragmentActivity.OnEditTextDialogClicked
import com.timecat.module.git.sgit.activities.RepoDetailActivity
import com.timecat.module.git.sgit.database.models.Repo
import com.timecat.module.git.tasks.SheimiAsyncTask.AsyncTaskPostCallback
import com.timecat.module.git.tasks.CherryPickTask

class CherryPickAction(repo: Repo, activity: RepoDetailActivity) : RepoAction(repo, activity) {
    override fun execute() {
        mActivity.showEditTextDialog(R.string.git_dialog_cherrypick_title,
            R.string.git_dialog_cherrypick_msg_hint,
            R.string.git_dialog_label_cherrypick,
            object : OnEditTextDialogClicked {
                override fun onClicked(text: String?) {
                    cherrypick(text)
                }
            })
    }

    fun cherrypick(commit: String?) {
        val task = CherryPickTask(mRepo, commit!!, object : AsyncTaskPostCallback {
            override fun onPostExecute(isSuccess: Boolean?) {
                mActivity.reset()
            }
        })
        task.executeTask()
    }
}