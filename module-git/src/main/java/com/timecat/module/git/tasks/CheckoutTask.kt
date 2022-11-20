package com.timecat.module.git.tasks

import com.timecat.module.git.sgit.database.models.Repo
import com.timecat.module.git.utils.StopTaskException
import org.eclipse.jgit.api.CreateBranchCommand
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.errors.GitAPIException
import org.eclipse.jgit.api.errors.JGitInternalException

class CheckoutTask(
    repo: Repo,
    private val mCommitName: String?,
    private val mBranch: String?,
    private val mCallback: AsyncTaskPostCallback?
) : RepoOpTask(repo) {
    override fun doInBackground(vararg params: Void): Boolean {
        return checkout(mCommitName, mBranch)
    }

    override fun onPostExecute(isSuccess: Boolean) {
        super.onPostExecute(isSuccess)
        mCallback?.onPostExecute(isSuccess)
    }

    fun checkout(name: String?, newBranch: String?): Boolean {
        try {
            val git = mRepo.getGit() ?: return false
            if (name == null) {
                checkoutNewBranch(git, newBranch)
            } else {
                if (Repo.COMMIT_TYPE_REMOTE == Repo.getCommitType(name)) {
                    checkoutFromRemote(git, name, if (newBranch == null || newBranch == "") Repo.getCommitName(name) else newBranch)
                } else if (newBranch == null || newBranch == "") {
                    checkoutFromLocal(git, name)
                } else {
                    checkoutFromLocal(git, name, newBranch)
                }
            }
        } catch (e: StopTaskException) {
            return false
        } catch (e: GitAPIException) {
            setException(mException)
            return false
        } catch (e: JGitInternalException) {
            setException(mException)
            return false
        } catch (e: Throwable) {
            setException(mException)
            return false
        }
        mRepo.updateLatestCommitInfo()
        return true
    }

    @Throws(GitAPIException::class, JGitInternalException::class, StopTaskException::class)
    fun checkoutNewBranch(git: Git, name: String?) {
        git.checkout().setName(name).setCreateBranch(true).call()
    }

    @Throws(GitAPIException::class, JGitInternalException::class, StopTaskException::class)
    fun checkoutFromLocal(git: Git, name: String?) {
        git.checkout().setName(name).call()
    }

    @Throws(GitAPIException::class, JGitInternalException::class, StopTaskException::class)
    fun checkoutFromLocal(git: Git, name: String?, branch: String?) {
        git.checkout().setCreateBranch(true).setName(branch)
            .setStartPoint(name).call()
    }

    @Throws(GitAPIException::class, JGitInternalException::class, StopTaskException::class)
    fun checkoutFromRemote(git: Git, remoteBranchName: String?, branchName: String?) {
        git.checkout().setCreateBranch(true).setName(branchName)
            .setStartPoint(remoteBranchName).call()
        git.branchCreate()
            .setUpstreamMode(CreateBranchCommand.SetupUpstreamMode.SET_UPSTREAM)
            .setStartPoint(remoteBranchName).setName(branchName)
            .setForce(true).call()
    }
}