package com.timecat.module.git.sgit.activities.delegate.actions

import android.app.AlertDialog
import android.app.Dialog
import com.timecat.module.git.R
import com.timecat.module.git.sgit.activities.RepoDetailActivity
import com.timecat.module.git.sgit.database.models.Repo
import com.timecat.module.git.sgit.dialogs.DummyDialogListener
import com.timecat.module.git.tasks.FetchTask

class FetchAction(repo: Repo, activity: RepoDetailActivity) : RepoAction(repo, activity) {
    override fun execute() {
        fetchDialog().show()
    }

    private fun fetch(remotes: Array<String>) {
        val fetchTask = FetchTask(remotes, mRepo, mActivity.ProgressCallback(R.string.git_fetch_msg_init))
        fetchTask.executeTask()
    }

    private fun fetchDialog(): Dialog {
        val builder = AlertDialog.Builder(mActivity)
        val originRemotes = mRepo.getRemotes().toTypedArray()
        val remotes = ArrayList<String>()
        return builder.setTitle(R.string.git_dialog_fetch_title)
            .setMultiChoiceItems(originRemotes, null) { dialogInterface, index, isChecked ->
                if (isChecked) {
                    remotes.add(originRemotes[index])
                } else {
                    for (i in remotes.indices) {
                        if (remotes[i] === originRemotes[index]) {
                            remotes.removeAt(i)
                        }
                    }
                }
            }
            .setPositiveButton(R.string.git_dialog_fetch_positive_button) { dialogInterface, i -> fetch(remotes.toTypedArray()) }
            .setNeutralButton(R.string.git_dialog_fetch_all_button) { dialogInterface, i -> fetch(originRemotes) }
            .setNegativeButton(android.R.string.cancel, DummyDialogListener())
            .create()
    }
}