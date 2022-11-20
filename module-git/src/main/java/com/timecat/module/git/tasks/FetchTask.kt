package com.timecat.module.git.tasks

import com.timecat.module.git.R
import com.timecat.module.git.sgit.database.models.Repo
import com.timecat.module.git.sgit.ssh.SgitTransportCallback
import org.eclipse.jgit.api.errors.TransportException

class FetchTask(private val mRemotes: Array<String>, repo: Repo, private val mCallback: AsyncTaskCallback?) : RepoRemoteOpTask(repo) {
    override fun doInBackground(vararg params: Void): Boolean {
        var result = true
        for (remote in mRemotes) {
            result = fetchRepo(remote) and result
            if (mCallback != null) {
                result = mCallback.doInBackground(*params) and result
            }
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

    private fun fetchRepo(remote: String): Boolean {
        try {
            val git = mRepo.getGit() ?: return false
            val fetchCommand = git.fetch()
                .setProgressMonitor(BasicProgressMonitor())
                .setTransportConfigCallback(SgitTransportCallback())
                .setRemote(remote)
            setCredentials(fetchCommand)
            fetchCommand.call()
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
        get() = FetchTask(mRemotes, mRepo, mCallback)
}