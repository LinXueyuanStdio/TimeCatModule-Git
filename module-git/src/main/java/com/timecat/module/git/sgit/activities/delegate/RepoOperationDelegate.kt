package com.timecat.module.git.sgit.activities.delegate

import com.timecat.module.git.utils.FsUtils.getRelativePath
import com.timecat.module.git.sgit.activities.RepoDetailActivity
import com.timecat.module.git.sgit.activities.delegate.actions.*
import com.timecat.module.git.sgit.database.models.Repo
import com.timecat.module.git.tasks.SheimiAsyncTask.AsyncTaskPostCallback
import com.timecat.module.git.tasks.*
import com.timecat.module.git.tasks.DeleteFileFromRepoTask.DeleteOperationType
import org.eclipse.jgit.lib.Ref
import java.io.File

class RepoOperationDelegate(private val mRepo: Repo, private val mActivity: RepoDetailActivity) {
    private val mActions = ArrayList<RepoAction>()

    init {
        initActions()
    }

    private fun initActions() {
        mActions.add(NewBranchAction(mRepo, mActivity))
        mActions.add(PullAction(mRepo, mActivity))
        mActions.add(PushAction(mRepo, mActivity))
        mActions.add(AddAllAction(mRepo, mActivity))
        mActions.add(CommitAction(mRepo, mActivity))
        mActions.add(ResetAction(mRepo, mActivity))
        mActions.add(MergeAction(mRepo, mActivity))
        mActions.add(FetchAction(mRepo, mActivity))
        mActions.add(RebaseAction(mRepo, mActivity))
        mActions.add(CherryPickAction(mRepo, mActivity))
        mActions.add(DiffAction(mRepo, mActivity))
        mActions.add(NewFileAction(mRepo, mActivity))
        mActions.add(NewDirAction(mRepo, mActivity))
        mActions.add(AddRemoteAction(mRepo, mActivity))
        mActions.add(RemoveRemoteAction(mRepo, mActivity))
        mActions.add(DeleteAction(mRepo, mActivity))
        mActions.add(RawConfigAction(mRepo, mActivity))
        mActions.add(ConfigAction(mRepo, mActivity))
    }

    fun executeAction(key: Int) {
        val action = mActions[key] ?: return
        action.execute()
    }

    fun checkoutCommit(commitName: String) {
        val checkoutTask = CheckoutTask(mRepo, commitName,
            null, object : AsyncTaskPostCallback {
                override fun onPostExecute(isSuccess: Boolean?) {
                    mActivity.reset(commitName)
                }
            })
        checkoutTask.executeTask()
    }

    fun checkoutCommit(commitName: String?, branch: String?) {
        val checkoutTask = CheckoutTask(mRepo, commitName!!, branch!!,
            object : AsyncTaskPostCallback {
                override fun onPostExecute(isSuccess: Boolean?) {
                    mActivity.reset(branch)
                }
            })
        checkoutTask.executeTask()
    }

    fun mergeBranch(
        commit: Ref?, ffModeStr: String?,
        autoCommit: Boolean
    ) {
        val mergeTask = MergeTask(mRepo, commit!!, ffModeStr!!,
            autoCommit, object : AsyncTaskPostCallback {
                override fun onPostExecute(isSuccess: Boolean?) {
                    mActivity.reset()
                }
            })
        mergeTask.executeTask()
    }

    fun addToStage(filepath: String) {
        val relative = getRelativePath(filepath)
        val addToStageTask = AddToStageTask(mRepo, relative)
        addToStageTask.executeTask()
    }

    fun checkoutFile(filepath: String) {
        val relative = getRelativePath(filepath)
        val task = CheckoutFileTask(mRepo, relative)
        task.executeTask()
    }

    fun deleteFileFromRepo(filepath: String, deleteOperationType: DeleteOperationType?) {
        val relative = getRelativePath(filepath)
        val task = DeleteFileFromRepoTask(mRepo,
            relative, deleteOperationType!!, object : AsyncTaskPostCallback {
                override fun onPostExecute(isSuccess: Boolean?) {
                    // TODO Auto-generated method stub
                    mActivity.filesFragment.reset()
                }
            })
        task.executeTask()
    }

    private fun getRelativePath(filepath: String): String {
        val base = mRepo.dir
        return getRelativePath(File(filepath), base)
    }
}