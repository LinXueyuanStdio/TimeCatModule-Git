package com.timecat.module.git.sgit.activities.delegate.actions

import android.app.AlertDialog
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.timecat.module.git.R
import com.timecat.module.git.utils.Profile
import com.timecat.module.git.sgit.activities.RepoDetailActivity
import com.timecat.module.git.sgit.database.models.Repo
import com.timecat.module.git.sgit.dialogs.DummyDialogListener
import com.timecat.module.git.tasks.SheimiAsyncTask.AsyncTaskPostCallback
import com.timecat.module.git.tasks.CommitChangesTask
import org.eclipse.jgit.lib.PersonIdent
import java.util.*

class CommitAction(repo: Repo, activity: RepoDetailActivity) : RepoAction(repo, activity) {
    override fun execute() {
        commit()
    }

    private fun commit(
        commitMsg: String, isAmend: Boolean, stageAll: Boolean, authorName: String?,
        authorEmail: String?
    ) {
        val commitTask = CommitChangesTask(mRepo, commitMsg,
            isAmend, stageAll, authorName!!, authorEmail!!, object : AsyncTaskPostCallback {
                override fun onPostExecute(isSuccess: Boolean?) {
                    mActivity.reset()
                }
            })
        commitTask.executeTask()
    }

    private inner class Author(val name: String, val email: String) : Comparable<Author> {
        private val mKeywords: ArrayList<String>
        private val SPLIT_KEYWORDS = " |\\.|-|_|@"

        init {
            mKeywords = ArrayList()
            Collections.addAll(mKeywords, *name.lowercase(Locale.getDefault()).split(SPLIT_KEYWORDS).toTypedArray())
            Collections.addAll(mKeywords, *email.lowercase(Locale.getDefault()).split(SPLIT_KEYWORDS).toTypedArray())
        }

        internal constructor(personIdent: PersonIdent) : this(personIdent.name, personIdent.emailAddress) {}

        fun displayString(): String {
            return name + " <" + email + ">"
        }

        override fun equals(o: Any?): Boolean {
            return if (o !is Author) {
                false
            } else name == o.name && email == o.email
        }

        override fun hashCode(): Int {
            return name.hashCode() + email.hashCode() * 997
        }

        override fun compareTo(another: Author): Int {
            val c1: Int
            c1 = name.compareTo(another.name)
            return if (c1 != 0) c1 else email.compareTo(another.email)
        }

        fun matches(constraint: String): Boolean {
            var constraint = constraint
            constraint = constraint.lowercase(Locale.getDefault())
            if (email.lowercase(Locale.getDefault()).startsWith(constraint)) {
                return true
            }
            if (name.lowercase(Locale.getDefault()).startsWith(constraint)) {
                return true
            }
            for (constraintKeyword in constraint.split(SPLIT_KEYWORDS).toTypedArray()) {
                var ok = false
                for (keyword in mKeywords) {
                    if (keyword.startsWith(constraintKeyword)) {
                        ok = true
                        break
                    }
                }
                if (!ok) {
                    return false
                }
            }
            return true
        }
    }

    private inner class AuthorsAdapter(context: Context?, var arrayList: List<Author>) : BaseAdapter(), Filterable {
        var mOriginalValues: List<Author>? = null
        var inflater: LayoutInflater

        init {
            inflater = LayoutInflater.from(context)
        }

        override fun getCount(): Int {
            return arrayList.size
        }

        override fun getItem(position: Int): Any {
            return arrayList[position].displayString()
        }

        override fun getItemId(position: Int): Long {
            return position.toLong()
        }

        private inner class ViewHolder {
            var textView: TextView? = null
        }

        override fun getView(position: Int, convertView: View, parent: ViewGroup): View {
            var convertView = convertView
            var holder: ViewHolder? = null
            if (convertView == null) {
                holder = ViewHolder()
                convertView = inflater.inflate(android.R.layout.simple_dropdown_item_1line, null)
                holder!!.textView = convertView as TextView
                convertView.setTag(holder)
            } else {
                holder = convertView.tag as ViewHolder
            }
            holder!!.textView!!.text = arrayList[position].displayString()
            return convertView
        }

        override fun getFilter(): Filter {
            return object : Filter() {
                override fun publishResults(constraint: CharSequence, results: FilterResults) {
                    arrayList = results.values as List<Author> // has the filtered values
                    notifyDataSetChanged() // notifies the data with new filtered values
                }

                override fun performFiltering(constraint: CharSequence): FilterResults {
                    val results = FilterResults() // Holds the results of a filtering operation in values
                    val FilteredArrList: MutableList<Author> = ArrayList()
                    if (mOriginalValues == null) {
                        mOriginalValues = ArrayList(arrayList) // saves the original data in mOriginalValues
                    }
                    if (constraint == null || constraint.length == 0) {
                        results.count = mOriginalValues!!.size
                        results.values = mOriginalValues
                    } else {
                        for (i in mOriginalValues!!.indices) {
                            val data = mOriginalValues!![i]
                            if (data.matches(constraint.toString())) {
                                FilteredArrList.add(data)
                            }
                        }
                        // set the Filtered result to return
                        results.count = FilteredArrList.size
                        results.values = FilteredArrList
                    }
                    return results
                }
            }
        }
    }

    private fun commit() {
        val builder = AlertDialog.Builder(mActivity)
        val inflater = mActivity.layoutInflater
        val layout = inflater.inflate(R.layout.git_dialog_commit, null)
        val commitMsg = layout.findViewById<View>(R.id.commitMsg) as EditText
        val commitAuthor = layout.findViewById<View>(R.id.commitAuthor) as AutoCompleteTextView
        val isAmend = layout.findViewById<View>(R.id.isAmend) as CheckBox
        val autoStage = layout.findViewById<View>(R.id.autoStage) as CheckBox
        val authors = HashSet<Author>()
        try {
            val git = mRepo.getGit() ?: return
            val commits = git.log().setMaxCount(500).call()
            for (commit in commits) {
                authors.add(Author(commit.authorIdent))
            }
        } catch (e: Exception) {
        }
        val profileUsername = Profile.getUsername(mActivity.applicationContext)
        val profileEmail = Profile.getEmail(mActivity.applicationContext)
        if (profileUsername.isNotBlank() && profileEmail.isNotBlank()) {
            authors.add(Author(profileUsername, profileEmail))
        }
        val authorList = ArrayList(authors)
        authorList.sort()
        val adapter = AuthorsAdapter(mActivity, authorList)
        commitAuthor.setAdapter(adapter)
        isAmend.setOnCheckedChangeListener { buttonView, isChecked ->
            if (isChecked) {
                commitMsg.setText(mRepo.lastCommitFullMsg)
            } else {
                commitMsg.setText("")
            }
        }
        val d = builder.setTitle(R.string.git_dialog_commit_title)
            .setView(layout)
            .setPositiveButton(R.string.git_dialog_commit_positive_label, null)
            .setNegativeButton(
                R.string.git_label_cancel,
                DummyDialogListener()
            ).create()
        d.setOnShowListener {
            val b = d.getButton(AlertDialog.BUTTON_POSITIVE)
            b.setOnClickListener(View.OnClickListener {
                val msg = commitMsg.text.toString()
                val author = commitAuthor.text.toString().trim { it <= ' ' }
                var authorName: String? = null
                var authorEmail: String? = null
                val ltidx: Int
                if (msg.trim { it <= ' ' } == "") {
                    commitMsg.error = mActivity.getString(R.string.git_error_no_commit_msg)
                    return@OnClickListener
                }
                if (author != "") {
                    ltidx = author.indexOf('<')
                    if (!author.endsWith(">") || ltidx == -1) {
                        commitAuthor.error = mActivity.getString(R.string.git_error_invalid_author)
                        return@OnClickListener
                    }
                    authorName = author.substring(0, ltidx)
                    authorEmail = author.substring(ltidx + 1, author.length - 1)
                }
                val amend = isAmend.isChecked
                val stage = autoStage.isChecked
                commit(msg, amend, stage, authorName, authorEmail)
                d.dismiss()
            }
            )
        }
        d.show()
    }
}