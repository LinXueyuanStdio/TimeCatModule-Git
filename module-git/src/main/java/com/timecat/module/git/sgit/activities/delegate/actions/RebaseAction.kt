package com.timecat.module.git.sgit.activities.delegate.actions

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.timecat.module.git.R
import com.timecat.module.git.sgit.dialogs.SheimiDialogFragment
import com.timecat.module.git.sgit.activities.RepoDetailActivity
import com.timecat.module.git.sgit.database.models.Repo
import com.timecat.module.git.tasks.SheimiAsyncTask.AsyncTaskPostCallback
import com.timecat.module.git.tasks.RebaseTask

class RebaseAction(repo: Repo, activity: RepoDetailActivity) : RepoAction(repo, activity) {
    override fun execute() {
        val rd = RebaseDialog()
        rd.arguments = mRepo.bundle
        rd.show(mActivity.supportFragmentManager, "rebase-dialog")
    }

    class RebaseDialog : SheimiDialogFragment() {
        private var mRepo: Repo? = null
        private var mActivity: RepoDetailActivity? = null
        private var mBranchTagList: ListView? = null
        private var mAdapter: BranchTagListAdapter? = null
        override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
            super.onCreateDialog(savedInstanceState)
            val args = arguments
            if (args != null && args.containsKey(Repo.TAG)) {
                mRepo = args.getSerializable(Repo.TAG) as Repo?
            }
            mActivity = activity as RepoDetailActivity?
            val builder = AlertDialog.Builder(mActivity)
            mBranchTagList = ListView(mActivity)
            mAdapter = BranchTagListAdapter(mActivity)
            mBranchTagList!!.adapter = mAdapter
            builder.setView(mBranchTagList)
            val branches = mRepo!!.getBranches()
            val currentBranchName = mRepo!!.getBranchName()
            for (branch in branches) {
                if (branch == currentBranchName) continue
                mAdapter!!.add(branch)
            }
            builder.setTitle(R.string.git_dialog_rebase_title)
            mBranchTagList!!.onItemClickListener = AdapterView.OnItemClickListener { adapterView: AdapterView<*>?, view: View?, position: Int, id: Long ->
                val commit = mAdapter!!.getItem(position)
                rebase(mRepo, commit, mActivity)
                dialog!!.cancel()
            }
            return builder.create()
        }

        private class BranchTagListAdapter(context: Context?) : ArrayAdapter<String?>(context!!, 0) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                var convertView = convertView
                val inflater = LayoutInflater.from(context)
                val holder: ListItemHolder
                if (convertView == null) {
                    convertView = inflater.inflate(
                        R.layout.git_listitem_dialog_choose_commit, parent,
                        false
                    )
                    holder = ListItemHolder()
                    holder.commitTitle = convertView
                        .findViewById<View>(R.id.commitTitle) as TextView
                    holder.commitIcon = convertView
                        .findViewById<View>(R.id.commitIcon) as ImageView
                    convertView.tag = holder
                } else {
                    holder = convertView.tag as ListItemHolder
                }
                val commitName = getItem(position)
                val displayName = Repo.getCommitDisplayName(commitName)
                val commitType = Repo.getCommitType(commitName)
                when (commitType) {
                    Repo.COMMIT_TYPE_HEAD -> holder.commitIcon!!.setImageResource(R.drawable.ic_branch_w)
                    Repo.COMMIT_TYPE_TAG -> holder.commitIcon!!.setImageResource(R.drawable.ic_tag_w)
                }
                holder.commitTitle!!.text = displayName
                return convertView!!
            }
        }

        private class ListItemHolder {
            var commitTitle: TextView? = null
            var commitIcon: ImageView? = null
        }
    }

    companion object {
        private fun rebase(
            repo: Repo?, branch: String?,
            activity: RepoDetailActivity?
        ) {
            val rebaseTask = RebaseTask(
                repo!!, branch!!,
                object : AsyncTaskPostCallback {
                    override fun onPostExecute(isSuccess: Boolean?) {
                        activity!!.reset()
                    }
                })
            rebaseTask.executeTask()
        }
    }
}