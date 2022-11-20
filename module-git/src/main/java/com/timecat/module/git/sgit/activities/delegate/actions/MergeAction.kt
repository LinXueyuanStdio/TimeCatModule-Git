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
import org.eclipse.jgit.lib.Ref

class MergeAction(repo: Repo, activity: RepoDetailActivity) : RepoAction(repo, activity) {
    override fun execute() {
        val md = MergeDialog()
        md.arguments = mRepo.bundle
        md.show(mActivity.supportFragmentManager, "merge-repo-dialog")
    }

    class MergeDialog : SheimiDialogFragment() {
        private var mRepo: Repo? = null
        private var mActivity: RepoDetailActivity? = null
        private var mBranchTagList: ListView? = null
        private var mSpinner: Spinner? = null
        private var mAdapter: BranchTagListAdapter? = null
        private var mCheckbox: CheckBox? = null
        override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
            super.onCreateDialog(savedInstanceState)
            val args = arguments
            if (args != null && args.containsKey(Repo.TAG)) {
                mRepo = args.getSerializable(Repo.TAG) as Repo?
            }
            mActivity = activity as RepoDetailActivity?
            val inflater = mActivity!!.layoutInflater
            val layout = inflater.inflate(R.layout.git_dialog_merge, null)
            val builder = AlertDialog.Builder(mActivity)
            mBranchTagList = layout.findViewById<View>(R.id.branchList) as ListView
            mSpinner = layout.findViewById<View>(R.id.ffSpinner) as Spinner
            mCheckbox = layout.findViewById<View>(R.id.autoCommit) as CheckBox
            mAdapter = BranchTagListAdapter(mActivity)
            mBranchTagList!!.adapter = mAdapter
            builder.setView(layout)
            val branches = mRepo!!.localBranches
            val currentBranchDisplayName = mRepo!!.currentDisplayName
            for (branch in branches) {
                if (Repo.getCommitDisplayName(branch.name) ==
                    currentBranchDisplayName
                ) continue
                mAdapter!!.add(branch)
            }
            builder.setTitle(R.string.git_dialog_merge_title)
            mBranchTagList?.setOnItemClickListener { adapterView, view, position, id ->
                val commit = mAdapter!!.getItem(position)
                val mFFString = mSpinner!!.selectedItem
                    .toString()
                mActivity!!.repoDelegate.mergeBranch(
                    commit,
                    mFFString, mCheckbox!!.isChecked
                )
                dialog!!.cancel()
            }
            return builder.create()
        }

        private class BranchTagListAdapter(context: Context?) : ArrayAdapter<Ref?>(context!!, 0) {
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
                val commitName = getItem(position)!!.name
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
}