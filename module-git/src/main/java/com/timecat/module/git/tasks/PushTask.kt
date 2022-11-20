package com.timecat.module.git.tasks

import com.timecat.module.git.R
import com.timecat.module.git.utils.BasicFunctions.activeActivity
import com.timecat.module.git.sgit.database.models.Repo
import com.timecat.module.git.sgit.ssh.SgitTransportCallback
import org.eclipse.jgit.api.errors.TransportException
import org.eclipse.jgit.transport.RefSpec
import org.eclipse.jgit.transport.RemoteRefUpdate

class PushTask(
    repo: Repo, private val mRemote: String, private val mPushAll: Boolean, private val mForcePush: Boolean,
    private val mCallback: AsyncTaskCallback?
) : RepoRemoteOpTask(repo) {
    private val resultMsg = StringBuffer()
    override fun doInBackground(vararg params: Void): Boolean {
        var result = pushRepo()
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
        if (isSuccess) {
            activeActivity!!.showMessageDialog(
                R.string.git_dialog_push_result, resultMsg.toString()
            )
        }
    }

    fun pushRepo(): Boolean {
        try {
            val git = mRepo.getGit() ?: return false
            val pushCommand = git.push().setPushTags()
                .setProgressMonitor(BasicProgressMonitor())
                .setTransportConfigCallback(SgitTransportCallback())
                .setRemote(mRemote)
            if (mPushAll) {
                pushCommand.setPushAll()
            } else {
                val spec = RefSpec(mRepo.getBranchName())
                pushCommand.setRefSpecs(spec)
            }
            if (mForcePush) {
                pushCommand.isForce = true
            }
            setCredentials(pushCommand)
            val result = pushCommand.call()
            for (r in result) {
                val updates = r.remoteUpdates
                for (update in updates) {
                    parseRemoteRefUpdate(update)
                }
            }
        } catch (e: TransportException) {
            setException(e)
            handleAuthError(this)
            return false
        } catch (e: Exception) {
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

    private fun parseRemoteRefUpdate(update: RemoteRefUpdate) {
        var msg: String? = null
        when (update.status) {
            RemoteRefUpdate.Status.AWAITING_REPORT -> msg = String.format(
                "[%s] Push process is awaiting update report from remote repository.\n",
                update.remoteName
            )
            RemoteRefUpdate.Status.NON_EXISTING -> msg = String.format(
                "[%s] Remote ref didn't exist.\n",
                update.remoteName
            )
            RemoteRefUpdate.Status.NOT_ATTEMPTED -> msg = String.format(
                "[%s] Push process hasn't yet attempted to update this ref.\n",
                update.remoteName
            )
            RemoteRefUpdate.Status.OK -> msg = String.format(
                "[%s] Success push to remote ref.\n",
                update.remoteName
            )
            RemoteRefUpdate.Status.REJECTED_NODELETE -> msg = String.format(
                """
    [%s] Remote ref update was rejected, because remote side doesn't support/allow deleting refs.
    
    """.trimIndent(),
                update.remoteName
            )
            RemoteRefUpdate.Status.REJECTED_NONFASTFORWARD -> {
                msg = String.format(
                    """
    [%s] Remote ref update was rejected, as it would cause non fast-forward update.
    
    """.trimIndent(),
                    update.remoteName
                )
                val reason = update.message
                msg = if (reason == null || reason.isEmpty()) {
                    String.format(
                        "[%s] Remote ref update was rejected.\n",
                        update.remoteName
                    )
                } else {
                    String.format(
                        "[%s] Remote ref update was rejected, because %s.\n",
                        update.remoteName, reason
                    )
                }
            }
            RemoteRefUpdate.Status.REJECTED_OTHER_REASON -> {
                val reason = update.message
                msg = if (reason == null || reason.isEmpty()) {
                    String.format(
                        "[%s] Remote ref update was rejected.\n",
                        update.remoteName
                    )
                } else {
                    String.format(
                        "[%s] Remote ref update was rejected, because %s.\n",
                        update.remoteName, reason
                    )
                }
            }
            RemoteRefUpdate.Status.REJECTED_REMOTE_CHANGED -> msg = String.format(
                """
    [%s] Remote ref update was rejected, because old object id on remote repository wasn't the same as defined expected old object.
    
    """.trimIndent(),
                update.remoteName
            )
            RemoteRefUpdate.Status.UP_TO_DATE -> msg = String.format(
                "[%s] remote ref is up to date\n",
                update.remoteName
            )
        }
        resultMsg.append(msg)
    }

    override val newTask: RepoRemoteOpTask
        get() = PushTask(mRepo, mRemote, mPushAll, mForcePush, mCallback)
}