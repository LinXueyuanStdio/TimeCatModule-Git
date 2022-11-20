package com.timecat.module.git.tasks

import com.timecat.module.git.R
import com.timecat.module.git.sgit.database.models.Repo
import com.timecat.module.git.sgit.ssh.SgitTransportCallback
import org.eclipse.jgit.api.RebaseCommand
import org.eclipse.jgit.api.ResetCommand
import org.eclipse.jgit.api.errors.GitAPIException
import org.eclipse.jgit.api.errors.TransportException

class PullTask(repo: Repo, private val mRemote: String, private val mForcePull: Boolean, private val mCallback: AsyncTaskCallback?) : RepoRemoteOpTask(repo) {
    override fun doInBackground(vararg params: Void): Boolean {
        var result = pullRepo()
        if (mCallback != null) {
            result = mCallback.doInBackground(*params) and result
        }
        return result
    }

    override fun onProgressUpdate(vararg progress: String) {
        super.onProgressUpdate(*progress)
        mCallback?.onProgressUpdate(*progress)
    }

    override fun onPreExecute() {
        super.onPreExecute()
        mCallback?.onPreExecute()
    }

    override fun onPostExecute(isSuccess: Boolean) {
        super.onPostExecute(isSuccess)
        mCallback?.onPostExecute(isSuccess)
    }

    fun pullRepo(): Boolean {
        try {
            val git = mRepo.getGit() ?: return false
            val pullCommand = git.pull()
                .setRemote(mRemote)
                .setProgressMonitor(BasicProgressMonitor())
                .setTransportConfigCallback(SgitTransportCallback())
            setCredentials(pullCommand)
            var branch: String? = null
            if (mForcePull) {
                branch = git.repository.fullBranch
                if (!branch.startsWith("refs/heads/")) {
                    setException(object : GitAPIException("not on branch") {}, R.string.git_error_pull_failed_not_on_branch)
                    return false
                }
                branch = branch.substring(11)
                val bpm = BasicProgressMonitor()
                bpm.beginTask("clearing repo state", 3)
                git.repository.writeMergeCommitMsg(null)
                git.repository.writeMergeHeads(null)
                bpm.update(1)
                try {
                    git.rebase().setOperation(RebaseCommand.Operation.ABORT).call()
                } catch (e: Exception) {
                }
                bpm.update(2)
                git.reset().setMode(ResetCommand.ResetType.HARD)
                    .setRef("HEAD").call()
                bpm.endTask()
            }
            pullCommand.call()
            if (mForcePull) {
                val bpm = BasicProgressMonitor()
                bpm.beginTask("resetting to $mRemote/$branch", 1)
                git.reset().setMode(ResetCommand.ResetType.HARD)
                    .setRef("$mRemote/$branch").call()
                bpm.endTask()
            }
        } catch (e: TransportException) {
            setException(e)
            handleAuthError(this)
            return false
        } catch (e: Exception) {
            setException(e, R.string.git_error_pull_failed)
            return false
        } catch (e: OutOfMemoryError) {
            setException(e, R.string.git_error_out_of_memory)
            return false
        } catch (e: Throwable) {
            setException(e)
            return false
        }
        mRepo.updateLatestCommitInfo()
        return true
    }

    override val newTask: RepoRemoteOpTask
        get() = PullTask(mRepo, mRemote, mForcePull, mCallback)
}