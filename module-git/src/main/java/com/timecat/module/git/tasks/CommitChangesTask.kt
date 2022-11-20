package com.timecat.module.git.tasks

import android.content.Context
import android.widget.Toast
import com.timecat.extend.arms.BaseApplication
import com.timecat.module.git.R
import com.timecat.module.git.utils.Profile
import com.timecat.module.git.sgit.database.models.Repo
import com.timecat.module.git.utils.StopTaskException
import org.eclipse.jgit.api.errors.*

class CommitChangesTask(
    repo: Repo, private val mCommitMsg: String, private val mIsAmend: Boolean,
    private val mStageAll: Boolean, private val mAuthorName: String, private val mAuthorEmail: String,
    private val mCallback: AsyncTaskPostCallback?
) : RepoOpTask(repo) {
    init {
        setSuccessMsg(R.string.git_success_commit)
    }

    override fun doInBackground(vararg params: Void): Boolean {
        return commit()
    }

    override fun onPostExecute(isSuccess: Boolean) {
        super.onPostExecute(isSuccess)
        mCallback?.onPostExecute(isSuccess)
    }

    fun commit(): Boolean {
        try {
            commit(mRepo, mStageAll, mIsAmend, mCommitMsg, mAuthorName, mAuthorEmail)
        } catch (e: StopTaskException) {
            return false
        } catch (e: GitAPIException) {
            setException(e)
            return false
        } catch (e: Throwable) {
            setException(e)
            return false
        }
        mRepo.updateLatestCommitInfo()
        return true
    }

    companion object {
        @Throws(
            Exception::class,
            NoHeadException::class,
            NoMessageException::class,
            UnmergedPathsException::class,
            ConcurrentRefUpdateException::class,
            WrongRepositoryStateException::class,
            GitAPIException::class,
            StopTaskException::class
        )
        fun commit(
            repo: Repo,
            stageAll: Boolean,
            isAmend: Boolean,
            msg: String,
            authorName: String?,
            authorEmail: String?
        ) {
            val context: Context = BaseApplication.getContext()
            val git = repo.getGit() ?: return
            val config = git.repository.config
            var committerEmail = config.getString("user", null, "email")
            var committerName = config.getString("user", null, "name")
            if (committerName == null || committerName == "") {
                committerName = Profile.getUsername(context)
            }
            if (committerEmail == null || committerEmail == "") {
                committerEmail = Profile.getEmail(context)
            }
            if (committerName!!.isEmpty() || committerEmail!!.isEmpty()) {
                Toast.makeText(context, "请在设置中设置用户名和邮箱", Toast.LENGTH_LONG).show()
                return
            }
            if (msg.isEmpty()) {
                Toast.makeText(context, "提交信息未填写", Toast.LENGTH_LONG).show()
                return
            }
            val cc = git.commit()
                .setCommitter(committerName, committerEmail).setAll(stageAll)
                .setAmend(isAmend).setMessage(msg)
            if (authorName != null && authorEmail != null) {
                cc.setAuthor(authorName, authorEmail)
            }
            cc.call()
            repo.updateLatestCommitInfo()
        }
    }
}