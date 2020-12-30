package com.timecat.module.git.sgit.activities.delegate.actions;

import com.timecat.module.git.R;
import com.timecat.module.git.android.activities.SheimiFragmentActivity.OnEditTextDialogClicked;
import com.timecat.module.git.sgit.activities.RepoDetailActivity;
import com.timecat.module.git.sgit.database.models.Repo;

public class NewDirAction extends RepoAction {

    public NewDirAction(Repo repo, RepoDetailActivity activity) {
        super(repo, activity);
    }

    @Override
    public void execute() {
        mActivity.showEditTextDialog(R.string.git_dialog_create_dir_title,
                R.string.git_dialog_create_dir_hint, R.string.git_label_create,
                new OnEditTextDialogClicked() {
                    @Override
                    public void onClicked(String text) {
                        mActivity.getFilesFragment().newDir(text);
                    }
                });
        mActivity.closeOperationDrawer();
    }
}
