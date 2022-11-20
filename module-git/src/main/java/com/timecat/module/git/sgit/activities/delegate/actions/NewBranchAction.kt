package com.timecat.module.git.sgit.activities.delegate.actions

import com.timecat.module.git.R
import com.timecat.module.git.sgit.activities.SheimiFragmentActivity.OnEditTextDialogClicked
import com.timecat.module.git.sgit.activities.RepoDetailActivity
import com.timecat.module.git.sgit.database.models.Repo
import com.timecat.module.git.tasks.SheimiAsyncTask.AsyncTaskPostCallback
import com.timecat.module.git.tasks.CheckoutTask

/**
 * Created by liscju - piotr.listkiewicz@gmail.com on 2015-03-15.
 */
class NewBranchAction(mRepo: Repo, mActivity: RepoDetailActivity) : RepoAction(mRepo, mActivity) {
    override fun execute() {
        mActivity.showEditTextDialog(R.string.git_dialog_create_branch_title,
            R.string.git_dialog_create_branch_hint, R.string.git_label_create,
            object : OnEditTextDialogClicked {
                override fun onClicked(branchName: String?) {
                    val checkoutTask = CheckoutTask(
                        mRepo, null, branchName!!,
                        ActivityResetPostCallback(branchName)
                    )
                    checkoutTask.executeTask()
                }
            })
    }

    private inner class ActivityResetPostCallback(private val mBranchName: String?) : AsyncTaskPostCallback {
        override fun onPostExecute(isSuccess: Boolean?) {
            mActivity.reset(mBranchName)
        }
    }
}