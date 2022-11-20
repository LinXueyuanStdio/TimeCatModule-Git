package com.timecat.module.git.tasks

import com.timecat.module.git.sgit.database.RepoContract
import com.timecat.module.git.sgit.database.models.Repo
import org.eclipse.jgit.api.Git

class InitLocalTask(repo: Repo) : RepoOpTask(repo) {
    override fun doInBackground(vararg params: Void): Boolean {
        val result = init()
        if (!result) {
            mRepo.deleteRepoSync()
            return false
        }
        return true
    }

    override fun onPostExecute(isSuccess: Boolean) {
        super.onPostExecute(isSuccess)
        if (isSuccess) {
            mRepo.updateLatestCommitInfo()
            mRepo.updateStatus(RepoContract.REPO_STATUS_NULL)
        }
    }

    fun init(): Boolean {
        try {
            Git.init().setDirectory(mRepo.dir).call()
        } catch (e: Throwable) {
            setException(e)
            return false
        }
        return true
    }
}

fun init(repo: Repo): Boolean {
    try {
        Git.init().setDirectory(repo.dir).call()
    } catch (e: Throwable) {
        return false
    }
    return true
}
fun initLocal(repo: Repo): Boolean {
    val result = init(repo)
    if (!result) {
        repo.deleteRepoSync()
        return false
    }
    return true
}