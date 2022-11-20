package com.timecat.module.git.tasks

import com.timecat.module.git.sgit.activities.SheimiFragmentActivity.OnPasswordEntered
import com.timecat.module.git.sgit.database.models.Repo

/**
 * Super class for Tasks that operate on a git remote
 */
abstract class RepoRemoteOpTask(repo: Repo) : RepoOpTask(repo), OnPasswordEntered {
    override fun onClicked(username: String, password: String, savePassword: Boolean) {
        mRepo.username = username
        mRepo.password = password
        if (savePassword) {
            mRepo.saveCredentials()
        }
        mRepo.removeTask(this)
        newTask.executeTask()
    }

    override fun onCanceled() {}
    abstract val newTask: RepoRemoteOpTask
}