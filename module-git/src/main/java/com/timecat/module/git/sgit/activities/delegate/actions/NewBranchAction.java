package com.timecat.module.git.sgit.activities.delegate.actions;

import com.timecat.module.git.R;
import com.timecat.module.git.android.activities.SheimiFragmentActivity;
import com.timecat.module.git.sgit.activities.RepoDetailActivity;
import com.timecat.module.git.sgit.database.models.Repo;
import com.timecat.module.git.sgit.repo.tasks.SheimiAsyncTask.AsyncTaskPostCallback;
import com.timecat.module.git.sgit.repo.tasks.repo.CheckoutTask;

/**
 * Created by liscju - piotr.listkiewicz@gmail.com on 2015-03-15.
 */
public class NewBranchAction extends RepoAction {
    public NewBranchAction(Repo mRepo, RepoDetailActivity mActivity) {
        super(mRepo, mActivity);
    }

    @Override
    public void execute() {
        mActivity.showEditTextDialog(R.string.git_dialog_create_branch_title,
                R.string.git_dialog_create_branch_hint, R.string.git_label_create,
                new SheimiFragmentActivity.OnEditTextDialogClicked() {
                    @Override
                    public void onClicked(String branchName) {
                        CheckoutTask checkoutTask = new CheckoutTask(mRepo, null, branchName,
                                new ActivityResetPostCallback(branchName));
                        checkoutTask.executeTask();
                    }
                });
        mActivity.closeOperationDrawer();
    }

    private class ActivityResetPostCallback implements AsyncTaskPostCallback {
        private final String mBranchName;

        public ActivityResetPostCallback(String branchName) {
            mBranchName = branchName;
        }

        @Override
        public void onPostExecute(Boolean isSuccess) {
            mActivity.reset(mBranchName);
        }
    }
}
