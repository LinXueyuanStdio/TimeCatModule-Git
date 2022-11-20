package com.timecat.module.git.sgit.activities.delegate.actions

import com.timecat.module.git.sgit.activities.RepoDetailActivity
import com.timecat.module.git.sgit.database.models.Repo

class DiffAction(repo: Repo, activity: RepoDetailActivity) : RepoAction(repo, activity) {
    override fun execute() {
        mActivity.enterDiffActionMode()
    }
}