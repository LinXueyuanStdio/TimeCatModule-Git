package com.timecat.module.git.tasks

import com.timecat.module.git.R
import com.timecat.module.git.sgit.database.models.Repo
import com.timecat.module.git.utils.StopTaskException

class AddToStageTask(repo: Repo, var mFilePattern: String) : RepoOpTask(repo) {
    init {
        setSuccessMsg(R.string.git_success_add_to_stage)
    }

    override fun doInBackground(vararg params: Void): Boolean {
        return addToStage()
    }

    override fun onPostExecute(isSuccess: Boolean) {
        super.onPostExecute(isSuccess)
    }

    fun addToStage(): Boolean {
        try {
            val git = mRepo.getGit() ?: return false
            git.add().addFilepattern(mFilePattern).call()
        } catch (e: StopTaskException) {
            return false
        } catch (e: Throwable) {
            setException(e)
            return false
        }
        return true
    }
}