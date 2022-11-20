package com.timecat.module.git.sgit.dialogs;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.timecat.component.commonsdk.utils.override.LogUtil;
import com.timecat.module.git.R;
import com.timecat.module.git.sgit.activities.explorer.PrivateKeyManageActivity;
import com.timecat.module.git.utils.SecurePrefsHelper;

import java.io.File;

import androidx.annotation.NonNull;

/**
 * Allowing editing password for a stored private key
 */

public class EditKeyPasswordDialog extends SheimiDialogFragment implements
        View.OnClickListener, DialogInterface.OnClickListener {

    private File mKeyFile;
    private PrivateKeyManageActivity mActivity;
    public static final String KEY_FILE_EXTRA = "extra_key_file";
    private EditText mPassword;

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        super.onCreateDialog(savedInstanceState);
        mActivity = (PrivateKeyManageActivity) getActivity();
        AlertDialog.Builder builder = new AlertDialog.Builder(mActivity);
        Bundle args = getArguments();
        if (args != null && args.containsKey(KEY_FILE_EXTRA)) {
            mKeyFile = new File(args.getString(KEY_FILE_EXTRA));
        }

        builder.setTitle(getString(R.string.git_dialog_edit_key_password_title));
        View view = mActivity.getLayoutInflater().inflate(R.layout.git_dialog_prompt_for_password_only, null);

        builder.setView(view);
        mPassword = (EditText) view.findViewById(R.id.password);

        // set button listener
        builder.setNegativeButton(R.string.git_label_cancel, new DummyDialogListener());
        builder.setPositiveButton(R.string.git_label_save, new DummyDialogListener());

        return builder.create();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(KEY_FILE_EXTRA, mKeyFile.getAbsolutePath());
    }

    @Override
    public void onStart() {
        super.onStart();
        AlertDialog dialog = (AlertDialog) getDialog();
        if (dialog == null) {
            return;
        }
        Button positiveButton = dialog.getButton(Dialog.BUTTON_POSITIVE);
        positiveButton.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        String newPassword = mPassword.getText().toString().trim();
        try {
            SecurePrefsHelper.getInstance().set(mKeyFile.getName(), newPassword);
        } catch (Exception e) {
            LogUtil.e(e);
        }
        mActivity.refreshList();
        dismiss();
    }

    @Override
    public void onClick(DialogInterface dialogInterface, int i) {
    }

}
