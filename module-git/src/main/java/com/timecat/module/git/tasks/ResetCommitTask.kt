package com.timecat.module.git.tasks

import com.timecat.component.commonsdk.utils.override.LogUtil
import com.timecat.module.git.R
import com.timecat.module.git.sgit.database.models.Repo
import com.timecat.module.git.utils.StopTaskException
import org.eclipse.jgit.api.RebaseCommand
import org.eclipse.jgit.api.ResetCommand
import org.eclipse.jgit.api.errors.WrongRepositoryStateException

class ResetCommitTask(repo: Repo, private val mCallback: AsyncTaskPostCallback?) : RepoOpTask(repo) {
    init {
        setSuccessMsg(R.string.git_success_reset)
    }

    override fun doInBackground(vararg params: Void): Boolean {
        return reset()
    }

    override fun onPostExecute(isSuccess: Boolean) {
        super.onPostExecute(isSuccess)
        mCallback?.onPostExecute(isSuccess)
    }

    fun reset(): Boolean {
        try {
            val git = mRepo.getGit() ?: return false
            git.repository.writeMergeCommitMsg(null)
            git.repository.writeMergeHeads(null)
            try {
                // if a rebase is in-progress, need to abort it
                git.rebase().setOperation(RebaseCommand.Operation.ABORT).call()
            } catch (e: WrongRepositoryStateException) {
                // Ignore this, it happens if rebase --abort is called without a rebase in progress.
                LogUtil.i(e, "Couldn't abort rebase while reset.")
            } catch (e: Exception) {
                setException(e, R.string.git_error_rebase_abort_failed_in_reset)
                return false
            }
            git.reset().setMode(ResetCommand.ResetType.HARD).call()
        } catch (e: StopTaskException) {
            return false
        } catch (e: Throwable) {
            setException(e)
            return false
        }
        return true
    }
}