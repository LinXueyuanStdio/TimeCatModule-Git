package com.timecat.module.git.sgit.activities.delegate.actions

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ListView
import com.timecat.component.commonsdk.utils.override.LogUtil
import com.timecat.element.alert.ToastUtil
import com.timecat.module.git.R
import com.timecat.module.git.sgit.activities.RepoDetailActivity
import com.timecat.module.git.sgit.database.models.Repo
import com.timecat.module.git.sgit.dialogs.DummyDialogListener
import com.timecat.module.git.sgit.dialogs.SheimiDialogFragment
import java.io.IOException

class RemoveRemoteAction(repo: Repo, activity: RepoDetailActivity) : RepoAction(repo, activity) {
    override fun execute() {
        val remotes = mRepo.getRemotes()
        if (remotes == null || remotes.isEmpty()) {
            ToastUtil.w_long(R.string.git_alert_please_add_a_remote)
            return
        }
        val dialog = RemoveRemoteDialog()
        dialog.arguments = mRepo.bundle
        dialog.show(mActivity.supportFragmentManager, "remove-remote-dialog")
    }

    class RemoveRemoteDialog : SheimiDialogFragment() {
        private var mRepo: Repo? = null
        private var mActivity: RepoDetailActivity? = null
        private var mRemoteList: ListView? = null
        private var mAdapter: ArrayAdapter<String>? = null
        override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
            super.onCreateDialog(savedInstanceState)
            val args = arguments
            if (args != null && args.containsKey(Repo.TAG)) {
                mRepo = args.getSerializable(Repo.TAG) as Repo?
            }
            mActivity = activity as RepoDetailActivity?
            val builder = AlertDialog.Builder(mActivity)
            val inflater = mActivity!!.layoutInflater
            val layout = inflater.inflate(R.layout.git_dialog_remove_remote, null)
            mRemoteList = layout.findViewById<View>(R.id.remoteList) as ListView
            mAdapter = ArrayAdapter(
                mActivity!!,
                android.R.layout.simple_list_item_1
            )
            val remotes = mRepo!!.getRemotes()
            mAdapter!!.addAll(remotes)
            mRemoteList!!.adapter = mAdapter
            mRemoteList!!.onItemClickListener = AdapterView.OnItemClickListener { parent, view, position, id ->
                val remote = mAdapter!!.getItem(position)
                try {
                    removeRemote(mRepo, remote)
                } catch (e: IOException) {
                    LogUtil.e(e)
                    mActivity?.showMessageDialog(R.string.git_dialog_error_title, getString(R.string.git_error_something_wrong))
                }
                dismiss()
            }
            builder.setTitle(R.string.git_dialog_remove_remote_title)
                .setView(layout)
                .setNegativeButton(R.string.git_label_cancel, DummyDialogListener())
            return builder.create()
        }
    }

    companion object {
        @Throws(IOException::class)
        fun removeRemote(repo: Repo?, remote: String?) {
            remote?.let {
                repo?.removeRemote(it)
            }
            ToastUtil.ok_long(R.string.git_success_remote_removed)
        }
    }
}