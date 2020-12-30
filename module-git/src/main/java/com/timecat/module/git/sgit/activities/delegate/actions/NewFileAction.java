package com.timecat.module.git.sgit.activities.delegate.actions;

import com.timecat.module.git.R;
import com.timecat.module.git.android.activities.SheimiFragmentActivity.OnEditTextDialogClicked;
import com.timecat.module.git.sgit.activities.RepoDetailActivity;
import com.timecat.module.git.sgit.database.models.Repo;

import java.io.IOException;

import timber.log.Timber;

public class NewFileAction extends RepoAction {

    public NewFileAction(Repo repo, RepoDetailActivity activity) {
        super(repo, activity);
    }

    @Override
    public void execute() {
        mActivity.showEditTextDialog(R.string.git_dialog_create_file_title,
                R.string.git_dialog_create_file_hint, R.string.git_label_create,
                new OnEditTextDialogClicked() {
                    @Override
                    public void onClicked(String text) {
                        try {
                            mActivity.getFilesFragment().newFile(text);
                        } catch (IOException e) {
                            Timber.e(e);
                            mActivity.showMessageDialog(R.string.git_dialog_error_title,
                                    mActivity.getString(R.string.git_error_something_wrong));
                        }
                    }
                });
        mActivity.closeOperationDrawer();
    }
}
