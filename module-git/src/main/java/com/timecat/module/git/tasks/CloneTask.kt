package com.timecat.module.git.tasks

import androidx.annotation.StringRes
import com.timecat.component.commonsdk.utils.override.LogUtil
import com.timecat.module.git.R
import com.timecat.module.git.sgit.database.RepoContract
import com.timecat.module.git.sgit.database.models.Repo
import com.timecat.module.git.sgit.ssh.SgitTransportCallback
import com.timecat.module.git.utils.Profile
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.errors.GitAPIException
import org.eclipse.jgit.api.errors.InvalidRemoteException
import org.eclipse.jgit.api.errors.JGitInternalException
import org.eclipse.jgit.api.errors.TransportException
import org.eclipse.jgit.lib.ProgressMonitor
import java.util.*

class CloneTask(repo: Repo, private val mCloneRecursive: Boolean, private val mCloneStatusName: String, private val mCallback: AsyncTaskCallback?) : RepoRemoteOpTask(repo) {
    override fun doInBackground(vararg v: Void): Boolean {
        var result = cloneRepo()
        if (!result) {
            LogUtil.e("del repo. clone failed")
            mRepo.deleteRepoSync()
        } else if (mCallback != null) {
            result = mCallback.doInBackground(*v) and result
        }
        return result
    }

    override fun onPostExecute(isSuccess: Boolean) {
        super.onPostExecute(isSuccess)
        if (isTaskCanceled) {
            return
        }
        if (isSuccess) {
            mRepo.updateLatestCommitInfo()
            mRepo.updateStatus(RepoContract.REPO_STATUS_NULL)
        }
    }

    fun cloneRepo(): Boolean {
        val localRepo = mRepo.dir
        val cloneCommand = Git.cloneRepository()
            .setURI(mRepo.remoteURL).setCloneAllBranches(true)
            .setProgressMonitor(RepoCloneMonitor())
            .setTransportConfigCallback(SgitTransportCallback())
            .setDirectory(localRepo)
            .setCloneSubmodules(mCloneRecursive)
        setCredentials(cloneCommand)
        try {
            cloneCommand.call()
            Profile.setLastCloneSuccess()
        } catch (e: InvalidRemoteException) {
            setException(e, R.string.git_error_invalid_remote)
            Profile.setLastCloneFailed(mRepo)
            return false
        } catch (e: TransportException) {
            setException(e)
            Profile.setLastCloneFailed(mRepo)
            handleAuthError(this)
            return false
        } catch (e: GitAPIException) {
            setException(e, R.string.git_error_clone_failed)
            return false
        } catch (e: JGitInternalException) {
            setException(e)
            return false
        } catch (e: OutOfMemoryError) {
            setException(e, R.string.git_error_out_of_memory)
            return false
        } catch (e: Throwable) {
            setException(e)
            return false
        }
        return true
    }

    override fun cancelTask() {
        super.cancelTask()
        mRepo.deleteRepo()
    }

    // need to call create repo again as when clone fails due auth error, the repo initially created gets deleted
    override val newTask: RepoRemoteOpTask
        get() {
            // need to call create repo again as when clone fails due auth error, the repo initially created gets deleted
            val userName = mRepo.username
            val password = mRepo.password
            mRepo = Repo.createRepo(mRepo.localPath, mRepo.remoteURL, mCloneStatusName)
            mRepo.username = userName
            mRepo.password = password
            return CloneTask(mRepo, mCloneRecursive, mCloneStatusName, mCallback)
        }

    @get:StringRes
    override val errorTitleRes: Int
        get() = R.string.git_error_clone_failed

    inner class RepoCloneMonitor : ProgressMonitor {
        private var mTotalWork = 0
        private var mWorkDone = 0
        private var mLastProgress = 0
        private var mTitle: String? = null
        private fun publishProgressInner() {
            var status = ""
            var percent = ""
            if (mTitle != null) {
                status = String.format(Locale.getDefault(), "%s ... ", mTitle)
                percent = "0%"
            }
            if (mTotalWork != 0) {
                val p = 100 * mWorkDone / mTotalWork
                if (p - mLastProgress < 1) {
                    return
                }
                mLastProgress = p
                percent = String.format(Locale.getDefault(), "(%d%%)", p)
            }
            mRepo.updateStatus(status + percent)
        }

        override fun start(totalTasks: Int) {
            publishProgressInner()
        }

        override fun beginTask(title: String, totalWork: Int) {
            mTotalWork = totalWork
            mWorkDone = 0
            mLastProgress = 0
            mTitle = title
            publishProgressInner()
        }

        override fun update(i: Int) {
            mWorkDone += i
            publishProgressInner()
        }

        override fun endTask() {}
        override fun isCancelled(): Boolean {
            return isTaskCanceled
        }
    }
}