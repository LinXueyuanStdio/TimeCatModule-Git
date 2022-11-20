package com.timecat.module.git.sgit.activities.delegate.actions

import android.app.AlertDialog
import android.view.LayoutInflater
import androidx.databinding.DataBindingUtil
import com.timecat.component.commonsdk.utils.override.LogUtil
import com.timecat.module.git.R
import com.timecat.module.git.databinding.GitDialogRepoConfigBinding
import com.timecat.module.git.sgit.activities.RepoDetailActivity
import com.timecat.module.git.sgit.database.models.GitConfig
import com.timecat.module.git.sgit.database.models.Repo
import com.timecat.module.git.utils.StopTaskException

/**
 * Action to display configuration for a Repo
 */
class ConfigAction(repo: Repo, activity: RepoDetailActivity) : RepoAction(repo, activity) {
    override fun execute() {
        try {
            val binding = DataBindingUtil.inflate<GitDialogRepoConfigBinding>(LayoutInflater.from(mActivity), R.layout.git_dialog_repo_config, null, false)
            val gitConfig = GitConfig(mRepo)
            binding.viewModel = gitConfig
            val builder = AlertDialog.Builder(mActivity)
            builder.setView(binding.root)
                .setNeutralButton(R.string.git_label_done, null)
                .create().show()
        } catch (e: StopTaskException) {
            //FIXME: show error to user
            LogUtil.e(e)
        }
    }
}