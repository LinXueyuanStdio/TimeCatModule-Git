package com.timecat.module.git.tasks

import com.timecat.module.git.sgit.database.models.Repo
import com.timecat.module.git.utils.StopTaskException
import org.eclipse.jgit.api.errors.GitAPIException
import org.eclipse.jgit.errors.NoWorkTreeException

class StatusTask(repo: Repo, private val mCallback: GetStatusCallback?) : RepoOpTask(repo) {
    interface GetStatusCallback {
        fun postStatus(result: String?)
    }

    private val mResult = StringBuffer()
    override fun doInBackground(vararg params: Void): Boolean {
        return status()
    }

    override fun onPostExecute(isSuccess: Boolean) {
        super.onPostExecute(isSuccess)
        if (mCallback != null && isSuccess) {
            mCallback.postStatus(mResult.toString())
        }
    }

    override fun executeTask() {
        execute()
    }

    private fun status(): Boolean {
        try {
            val git = mRepo.getGit() ?: return false
            val status = git.status().call()
            convertStatus(status)
        } catch (e: NoWorkTreeException) {
            setException(e)
            return false
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

    private fun convertStatus(status: org.eclipse.jgit.api.Status) {
        if (!status.hasUncommittedChanges() && status.isClean) {
            mResult.append("Nothing to commit, working directory clean")
            return
        }
        // TODO if working dir not clean
        convertStatusSet("Added files:", status.added)
        convertStatusSet("Changed files:", status.changed)
        convertStatusSet("Removed files:", status.removed)
        convertStatusSet("Missing files:", status.missing)
        convertStatusSet("Modified files:", status.modified)
        convertStatusSet("Conflicting files:", status.conflicting)
        convertStatusSet("Untracked files:", status.untracked)
    }

    private fun convertStatusSet(type: String, status: Set<String>) {
        if (status.isEmpty()) return
        mResult.append(type)
        mResult.append("\n\n")
        for (s in status) {
            mResult.append('\t')
            mResult.append(s)
            mResult.append('\n')
        }
        mResult.append("\n")
    }
}