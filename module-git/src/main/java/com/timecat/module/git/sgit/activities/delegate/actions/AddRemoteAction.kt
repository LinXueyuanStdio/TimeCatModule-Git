package com.timecat.module.git.sgit.activities.delegate.actions

import android.app.AlertDialog
import android.view.View
import android.widget.EditText
import com.timecat.component.commonsdk.utils.override.LogUtil
import com.timecat.element.alert.ToastUtil
import com.timecat.module.git.R
import com.timecat.module.git.sgit.activities.RepoDetailActivity
import com.timecat.module.git.sgit.database.models.Repo
import com.timecat.module.git.sgit.dialogs.DummyDialogListener
import java.io.IOException

class AddRemoteAction(repo: Repo, activity: RepoDetailActivity) : RepoAction(repo, activity) {
    override fun execute() {
        showAddRemoteDialog()
    }

    @Throws(IOException::class)
    fun addToRemote(name: String, url: String?) {
        mRepo.setRemote(name, url)
        mRepo.updateRemote()
        ToastUtil.ok_long(R.string.git_success_remote_added)
    }

    fun showAddRemoteDialog() {
        val builder = AlertDialog.Builder(mActivity)
        val inflater = mActivity.layoutInflater
        val layout = inflater.inflate(R.layout.git_dialog_add_remote, null)
        val remoteName = layout.findViewById<View>(R.id.remoteName) as EditText
        val remoteUrl = layout.findViewById<View>(R.id.remoteUrl) as EditText
        builder.setTitle(R.string.git_dialog_add_remote_title)
            .setView(layout)
            .setPositiveButton(R.string.git_dialog_add_remote_positive_label) { dialogInterface, i ->
                val name = remoteName.text.toString()
                val url = remoteUrl.text.toString()
                try {
                    addToRemote(name, url)
                } catch (e: IOException) {
                    LogUtil.e(e)
                    mActivity.showMessageDialog(
                        R.string.git_dialog_error_title,
                        mActivity.getString(R.string.git_error_something_wrong)
                    )
                }
            }
            .setNegativeButton(
                R.string.git_label_cancel,
                DummyDialogListener()
            ).show()
    }
}