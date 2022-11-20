package com.timecat.module.git.tasks

import com.timecat.module.git.R
import com.timecat.module.git.utils.FsUtils.deleteFile
import com.timecat.module.git.utils.FsUtils.joinPath
import com.timecat.module.git.sgit.database.models.Repo
import com.timecat.module.git.utils.StopTaskException

class DeleteFileFromRepoTask(
    repo: Repo, var mFilePattern: String,
    private val mOperationType: DeleteOperationType, var mCallback: AsyncTaskPostCallback?
) : RepoOpTask(repo) {
    init {
        setSuccessMsg(R.string.git_success_remove_file)
    }

    override fun doInBackground(vararg params: Void): Boolean {
        return removeFile()
    }

    override fun onPostExecute(isSuccess: Boolean) {
        super.onPostExecute(isSuccess)
        if (mCallback != null) {
            mCallback!!.onPostExecute(isSuccess)
        }
    }

    fun removeFile(): Boolean {
        try {
            when (mOperationType) {
                DeleteOperationType.DELETE -> {
                    val fileToDelete = joinPath(mRepo.dir, mFilePattern)
                    deleteFile(fileToDelete)
                }
                DeleteOperationType.REMOVE_CACHED -> {
                    val git =mRepo.getGit() ?: return false
                    git.rm().setCached(true).addFilepattern(mFilePattern).call()
                }
                DeleteOperationType.REMOVE_FORCE -> {
                    val git =mRepo.getGit() ?: return false
                    git.rm().addFilepattern(mFilePattern).call()
                }
            }
        } catch (e: StopTaskException) {
            return false
        } catch (e: Throwable) {
            setException(e)
            return false
        }
        return true
    }

    /**
     * Created by lee on 2015-01-30.
     */
    enum class DeleteOperationType {
        DELETE, REMOVE_CACHED, REMOVE_FORCE
    }
}