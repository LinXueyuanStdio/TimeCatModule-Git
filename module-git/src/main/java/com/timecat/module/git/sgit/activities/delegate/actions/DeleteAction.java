package com.timecat.module.git.sgit.activities.delegate.actions;

import com.timecat.module.git.R;
import com.timecat.module.git.sgit.activities.RepoDetailActivity;
import com.timecat.module.git.sgit.database.models.Repo;

public class DeleteAction extends RepoAction {

    public DeleteAction(Repo repo, RepoDetailActivity activity) {
        super(repo, activity);
    }

    @Override
    public void execute() {
        mActivity.showMessageDialog(R.string.git_dialog_delete_repo_title,
                R.string.git_dialog_delete_repo_msg, R.string.git_label_delete,
                () -> {
                    mRepo.deleteRepo();
                    mActivity.finish();
                });
        mActivity.closeOperationDrawer();
    }
}
