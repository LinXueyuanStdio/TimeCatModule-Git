package com.timecat.module.git.sgit.dialogs;

import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.timecat.element.alert.ToastUtil;
import com.timecat.module.git.R;
import com.timecat.module.git.android.views.SheimiDialogFragment;
import com.timecat.module.git.repolist.RepoListActivity;
import com.timecat.module.git.sgit.database.models.Repo;
import com.timecat.module.git.sgit.preference.PreferenceHelper;
import com.timecat.module.git.sgit.repo.tasks.repo.InitLocalTask;

import java.io.File;

/**
 * Created by sheimi on 8/24/13.
 */

public class InitDialog extends SheimiDialogFragment implements
        View.OnClickListener {

    private EditText mLocalPath;
    private RepoListActivity mActivity;
    private Repo mRepo;
    private PreferenceHelper mPrefsHelper;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        super.onCreateDialog(savedInstanceState);
        mActivity = (RepoListActivity) getActivity();

        mPrefsHelper = PreferenceHelper.getInstance();

        AlertDialog.Builder builder = new AlertDialog.Builder(mActivity);
        LayoutInflater inflater = mActivity.getLayoutInflater();
        View layout = inflater.inflate(R.layout.git_dialog_init_repo, null);
        builder.setView(layout);

        mLocalPath = (EditText) layout.findViewById(R.id.localPath);

        // set button listener
        builder.setTitle(R.string.git_dialog_init_repo_title);
        builder.setNegativeButton(getString(R.string.git_label_cancel),
                new DummyDialogListener());
        builder.setPositiveButton(
                getString(R.string.git_dialog_init_repo_positive_label),
                new DummyDialogListener());

        return builder.create();
    }

    @Override
    public void onStart() {
        super.onStart();
        AlertDialog dialog = (AlertDialog) getDialog();
        if (dialog == null)
            return;
        Button positiveButton = (Button) dialog
                .getButton(Dialog.BUTTON_POSITIVE);
        positiveButton.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        String localPath = mLocalPath.getText().toString().trim();

        if (localPath.equals("")) {
            ToastUtil.e_long(R.string.git_alert_localpath_required);
            mLocalPath.setError(getString(R.string.git_alert_localpath_required));
            mLocalPath.requestFocus();
            return;
        }
        if (localPath.contains("/")) {
            ToastUtil.e_long(R.string.git_alert_localpath_format);
            mLocalPath.setError(getString(R.string.git_alert_localpath_format));
            mLocalPath.requestFocus();
            return;
        }

        File file = Repo.getDir(mPrefsHelper, localPath);
        if (file.exists()) {
            ToastUtil.e_long(R.string.git_alert_localpath_repo_exists);
            mLocalPath
                    .setError(getString(R.string.git_alert_localpath_repo_exists));
            mLocalPath.requestFocus();
            return;
        }

        localPath = mLocalPath.getText().toString().trim();
        mRepo = Repo.createRepo(localPath, "local repository", getString(R.string.git_initialising));

        InitLocalTask task = new InitLocalTask(mRepo);
        task.executeTask();

        dismiss();
    }

}
