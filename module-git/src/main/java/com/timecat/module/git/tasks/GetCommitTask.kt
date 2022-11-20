package com.timecat.module.git.tasks

import com.timecat.module.git.sgit.database.models.Repo
import com.timecat.module.git.utils.StopTaskException
import org.eclipse.jgit.api.errors.GitAPIException
import org.eclipse.jgit.revwalk.RevCommit

class GetCommitTask(repo: Repo, private val mFile: String?, private val mCallback: GetCommitCallback?) : RepoOpTask(repo) {
    private var mResult: MutableList<RevCommit> = ArrayList()

    interface GetCommitCallback {
        fun postCommits(commits: List<RevCommit>)
    }

    override fun executeTask() {
        execute()
    }

    override fun doInBackground(vararg params: Void): Boolean {
        return getCommitsList()
    }

    override fun onPostExecute(isSuccess: Boolean) {
        super.onPostExecute(isSuccess)
        mCallback?.postCommits(mResult)
    }

    fun getCommitsList(): Boolean {
        try {
            val git = mRepo.getGit() ?: return false
            val cmd = git.log()
            if (mFile != null) cmd.addPath(mFile)
            val commits = cmd.call()
            mResult = ArrayList()
            for (commit in commits) {
                mResult.add(commit)
            }
        } catch (e: GitAPIException) {
            setException(e)
            return false
        } catch (e: StopTaskException) {
            return false
        } catch (e: Throwable) {
            setException(e)
            return false
        }
        return true
    }
}