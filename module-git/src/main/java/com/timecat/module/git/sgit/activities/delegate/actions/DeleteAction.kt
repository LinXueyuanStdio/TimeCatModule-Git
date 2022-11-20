package com.timecat.module.git.sgit.activities.delegate.actions

import com.timecat.module.git.R
import com.timecat.module.git.sgit.activities.SheimiFragmentActivity.OnPositiveClickListener
import com.timecat.module.git.sgit.activities.RepoDetailActivity
import com.timecat.module.git.sgit.database.models.Repo

class DeleteAction(repo: Repo, activity: RepoDetailActivity) : RepoAction(repo, activity) {
    override fun execute() {
        mActivity.showMessageDialog(R.string.git_dialog_delete_repo_title,
            R.string.git_dialog_delete_repo_msg, R.string.git_label_delete,
            object : OnPositiveClickListener {
                override fun onClick() {
                    mRepo.deleteRepo()
                    mActivity.finish()
                }
            })
    }
}