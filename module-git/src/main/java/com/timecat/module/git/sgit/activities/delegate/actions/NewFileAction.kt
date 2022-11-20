package com.timecat.module.git.sgit.activities.delegate.actions

import com.timecat.component.commonsdk.utils.override.LogUtil
import com.timecat.module.git.R
import com.timecat.module.git.sgit.activities.RepoDetailActivity
import com.timecat.module.git.sgit.activities.SheimiFragmentActivity.OnEditTextDialogClicked
import com.timecat.module.git.sgit.database.models.Repo
import java.io.IOException

class NewFileAction(repo: Repo, activity: RepoDetailActivity) : RepoAction(repo, activity) {
    override fun execute() {
        mActivity.showEditTextDialog(R.string.git_dialog_create_file_title,
            R.string.git_dialog_create_file_hint, R.string.git_label_create,
            object : OnEditTextDialogClicked {
                override fun onClicked(text: String?) {
                    try {
                        mActivity.filesFragment.newFile(text)
                    } catch (e: IOException) {
                        LogUtil.e(e)
                        mActivity.showMessageDialog(
                            R.string.git_dialog_error_title,
                            mActivity.getString(R.string.git_error_something_wrong)
                        )
                    }
                }
            })
    }
}