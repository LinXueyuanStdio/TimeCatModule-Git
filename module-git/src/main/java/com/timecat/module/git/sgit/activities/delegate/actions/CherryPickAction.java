package com.timecat.module.git.sgit.activities.delegate.actions;

import com.timecat.module.git.R;
import com.timecat.module.git.android.activities.SheimiFragmentActivity.OnEditTextDialogClicked;
import com.timecat.module.git.sgit.activities.RepoDetailActivity;
import com.timecat.module.git.sgit.database.models.Repo;
import com.timecat.module.git.sgit.repo.tasks.SheimiAsyncTask.AsyncTaskPostCallback;
import com.timecat.module.git.sgit.repo.tasks.repo.CherryPickTask;

public class CherryPickAction extends RepoAction {

    public CherryPickAction(Repo repo, RepoDetailActivity activity) {
        super(repo, activity);
    }

    @Override
    public void execute() {
        mActivity.showEditTextDialog(R.string.git_dialog_cherrypick_title,
                R.string.git_dialog_cherrypick_msg_hint,
                R.string.git_dialog_label_cherrypick,
                new OnEditTextDialogClicked() {
                    @Override
                    public void onClicked(String text) {
                        cherrypick(text);
                    }
                });
        mActivity.closeOperationDrawer();
    }

    public void cherrypick(String commit) {
        CherryPickTask task = new CherryPickTask(mRepo, commit, new AsyncTaskPostCallback() {
            @Override
            public void onPostExecute(Boolean isSuccess) {
                mActivity.reset();
            }
        });
        task.executeTask();
    }

}
