package com.timecat.module.git.sgit.activities.delegate.actions;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

import com.timecat.element.alert.ToastUtil;
import com.timecat.module.git.R;
import com.timecat.module.git.sgit.activities.RepoDetailActivity;
import com.timecat.module.git.sgit.database.models.Repo;
import com.timecat.module.git.sgit.dialogs.DummyDialogListener;

import java.io.IOException;

import timber.log.Timber;

public class AddRemoteAction extends RepoAction {

    public AddRemoteAction(Repo repo, RepoDetailActivity activity) {
        super(repo, activity);
    }

    @Override
    public void execute() {
        showAddRemoteDialog();
        mActivity.closeOperationDrawer();
    }

    public void addToRemote(String name, String url) throws IOException {
        mRepo.setRemote(name, url);
        mRepo.updateRemote();
        ToastUtil.ok_long(R.string.git_success_remote_added);
    }

    public void showAddRemoteDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(mActivity);
        LayoutInflater inflater = mActivity.getLayoutInflater();
        View layout = inflater.inflate(R.layout.git_dialog_add_remote, null);
        final EditText remoteName = (EditText) layout
                .findViewById(R.id.remoteName);
        final EditText remoteUrl = (EditText) layout
                .findViewById(R.id.remoteUrl);

        builder.setTitle(R.string.git_dialog_add_remote_title)
                .setView(layout)
                .setPositiveButton(R.string.git_dialog_add_remote_positive_label,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(
                                    DialogInterface dialogInterface, int i) {
                                String name = remoteName.getText().toString();
                                String url = remoteUrl.getText().toString();
                                try {
                                    addToRemote(name, url);
                                } catch (IOException e) {
                                    Timber.e(e);
                                    mActivity.showMessageDialog(R.string.git_dialog_error_title,
                                            mActivity.getString(R.string.git_error_something_wrong));
                                }
                            }
                        })
                .setNegativeButton(R.string.git_label_cancel,
                        new DummyDialogListener()).show();
    }

}
