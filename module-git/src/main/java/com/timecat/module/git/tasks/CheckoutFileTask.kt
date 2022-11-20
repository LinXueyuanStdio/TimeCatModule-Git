package com.timecat.module.git.tasks

import com.timecat.module.git.R
import com.timecat.module.git.sgit.database.models.Repo
import com.timecat.module.git.utils.StopTaskException

class CheckoutFileTask(
    repo: Repo, private val mPath: String,
) : RepoOpTask(repo) {
    init {
        setSuccessMsg(R.string.git_success_checkout_file)
    }

    override fun doInBackground(vararg params: Void): Boolean {
        return checkout()
    }

    override fun onPostExecute(isSuccess: Boolean) {
        super.onPostExecute(isSuccess)
    }

    private fun checkout(): Boolean {
        try {
            val git = mRepo.getGit() ?: return false
            git.checkout().addPath(mPath).call()
        } catch (e: StopTaskException) {
            return false
        } catch (e: Throwable) {
            setException(e)
            return false
        }
        return true
    }
}