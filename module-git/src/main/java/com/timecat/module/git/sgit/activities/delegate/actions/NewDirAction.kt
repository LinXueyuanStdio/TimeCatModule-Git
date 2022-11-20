package com.timecat.module.git.sgit.activities.delegate.actions

import com.timecat.module.git.R
import com.timecat.module.git.sgit.activities.SheimiFragmentActivity.OnEditTextDialogClicked
import com.timecat.module.git.sgit.activities.RepoDetailActivity
import com.timecat.module.git.sgit.database.models.Repo

class NewDirAction(repo: Repo, activity: RepoDetailActivity) : RepoAction(repo, activity) {
    override fun execute() {
        mActivity.showEditTextDialog(R.string.git_dialog_create_dir_title,
            R.string.git_dialog_create_dir_hint, R.string.git_label_create,
            object : OnEditTextDialogClicked {
                override fun onClicked(text: String?) {
                    mActivity.filesFragment.newDir(text)
                }
            })
    }
}