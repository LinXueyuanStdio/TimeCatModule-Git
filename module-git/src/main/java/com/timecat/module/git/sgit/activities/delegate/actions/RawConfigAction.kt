package com.timecat.module.git.sgit.activities.delegate.actions

import android.content.Intent
import com.timecat.module.git.sgit.activities.RepoDetailActivity
import com.timecat.module.git.sgit.activities.ViewFileActivity
import com.timecat.module.git.sgit.database.models.Repo

/**
 * Created by phcoder on 05.12.15.
 */
class RawConfigAction(repo: Repo, activity: RepoDetailActivity) : RepoAction(repo, activity) {
    override fun execute() {
        val intent = Intent(mActivity, ViewFileActivity::class.java)
        intent.putExtra(ViewFileActivity.TAG_FILE_NAME, mRepo.dir.absoluteFile.toString() + "/.git/config")
        mActivity.startActivity(intent)
    }
}