package com.timecat.module.git.sgit.activities.delegate.actions

import com.timecat.module.git.sgit.activities.RepoDetailActivity
import com.timecat.module.git.sgit.database.models.Repo

abstract class RepoAction(protected var mRepo: Repo, protected var mActivity: RepoDetailActivity) {
    abstract fun execute()
}