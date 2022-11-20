package com.timecat.module.git.tasks

import com.timecat.extend.arms.BaseApplication
import com.timecat.module.git.R
import com.timecat.module.git.utils.BasicFunctions.activeActivity
import com.timecat.module.git.sgit.database.models.Repo
import com.timecat.module.git.utils.StopTaskException
import org.eclipse.jgit.api.MergeCommand.FastForwardMode
import org.eclipse.jgit.api.errors.GitAPIException
import org.eclipse.jgit.lib.Ref

class MergeTask(
    repo: Repo,
    private val mCommit: Ref,
    private val mFFModeStr: String,
    private val mAutoCommit: Boolean,
    private val mCallback: AsyncTaskPostCallback?
) : RepoOpTask(repo) {
    override fun doInBackground(vararg params: Void): Boolean {
        return mergeBranch()
    }

    override fun onPostExecute(isSuccess: Boolean) {
        super.onPostExecute(isSuccess)
        mCallback?.onPostExecute(isSuccess)
    }

    fun mergeBranch(): Boolean {
        val stringArray = BaseApplication.getContext().getResources().getStringArray(R.array.git_merge_ff_type)
        var ffMode = FastForwardMode.FF
        if (mFFModeStr == stringArray[1]) {
            // FF Only
            ffMode = FastForwardMode.FF_ONLY
        } else if (mFFModeStr == stringArray[2]) {
            // No FF
            ffMode = FastForwardMode.NO_FF
        }
        try {
            val git = mRepo.getGit() ?: return false
            git.merge().include(mCommit).setFastForward(ffMode).call()
        } catch (e: GitAPIException) {
            setException(e)
            return false
        } catch (e: StopTaskException) {
            return false
        } catch (e: Throwable) {
            setException(e)
            return false
        }
        if (mAutoCommit) {
            val b1 = mRepo.getBranchName()
            val b2 = mCommit.name
            val msg: String = if (b1 == null) {
                String.format(
                    "Merge branch '%s'",
                    Repo.getCommitDisplayName(b2)
                )
            } else {
                String.format(
                    "Merge branch '%s' into %s",
                    Repo.getCommitDisplayName(b2),
                    Repo.getCommitDisplayName(b1)
                )
            }
            try {
                CommitChangesTask.commit(mRepo, false, false, msg, null, null)
            } catch (e: GitAPIException) {
                setException(e)
                return false
            } catch (e: StopTaskException) {
                return false
            } catch (e: Throwable) {
                setException(e)
                return false
            }
        }
        mRepo.updateLatestCommitInfo()
        return true
    }
}