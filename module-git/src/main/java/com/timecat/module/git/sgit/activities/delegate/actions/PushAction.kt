package com.timecat.module.git.sgit.activities.delegate.actions

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.CheckBox
import android.widget.ListView
import com.timecat.element.alert.ToastUtil
import com.timecat.module.git.R
import com.timecat.module.git.sgit.dialogs.SheimiDialogFragment
import com.timecat.module.git.sgit.activities.RepoDetailActivity
import com.timecat.module.git.sgit.database.models.Repo
import com.timecat.module.git.sgit.dialogs.DummyDialogListener
import com.timecat.module.git.tasks.PushTask

class PushAction(repo: Repo, activity: RepoDetailActivity) : RepoAction(repo, activity) {
    override fun execute() {
        val remotes = mRepo.getRemotes()
        if (remotes == null || remotes.isEmpty()) {
            ToastUtil.w_long(R.string.git_alert_please_add_a_remote)
            return
        }
        val pd = PushDialog()
        pd.arguments = mRepo.bundle
        pd.show(mActivity.supportFragmentManager, "push-repo-dialog")
    }

    class PushDialog : SheimiDialogFragment() {
        private var mRepo: Repo? = null
        private var mActivity: RepoDetailActivity? = null
        private var mPushAll: CheckBox? = null
        private var mForcePush: CheckBox? = null
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
            val layout = inflater.inflate(R.layout.git_dialog_push, null)
            mPushAll = layout.findViewById<View>(R.id.pushAll) as CheckBox
            mForcePush = layout.findViewById<View>(R.id.forcePush) as CheckBox
            mRemoteList = layout.findViewById<View>(R.id.remoteList) as ListView
            mAdapter = ArrayAdapter(mActivity!!, android.R.layout.simple_list_item_1)
            val remotes = mRepo!!.getRemotes()
            mAdapter!!.addAll(remotes)
            mRemoteList!!.adapter = mAdapter
            mRemoteList!!.onItemClickListener = AdapterView.OnItemClickListener { parent, view, position, id ->
                val remote = mAdapter!!.getItem(position)
                val isPushAll = mPushAll!!.isChecked
                val isForcePush = mForcePush!!.isChecked
                push(mRepo, mActivity, remote, isPushAll, isForcePush)
                dismiss()
            }
            builder.setTitle(R.string.git_dialog_push_repo_title)
                .setView(layout)
                .setNegativeButton(R.string.git_label_cancel, DummyDialogListener())
            return builder.create()
        }
    }

    companion object {
        fun push(
            repo: Repo?, activity: RepoDetailActivity?,
            remote: String?, pushAll: Boolean, forcePush: Boolean
        ) {
            val pushTask = PushTask(
                repo!!, remote!!, pushAll, forcePush,
                activity!!.ProgressCallback(R.string.git_push_msg_init)
            )
            pushTask.executeTask()
        }
    }
}