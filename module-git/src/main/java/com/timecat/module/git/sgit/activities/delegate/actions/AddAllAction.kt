package com.timecat.module.git.sgit.activities.delegate.actions

import com.timecat.module.git.sgit.activities.RepoDetailActivity
import com.timecat.module.git.sgit.database.models.Repo
import com.timecat.module.git.tasks.AddToStageTask

class AddAllAction(repo: Repo, activity: RepoDetailActivity) : RepoAction(repo, activity) {
    override fun execute() {
        val addTask = AddToStageTask(mRepo, ".")
        addTask.executeTask()
    }
}