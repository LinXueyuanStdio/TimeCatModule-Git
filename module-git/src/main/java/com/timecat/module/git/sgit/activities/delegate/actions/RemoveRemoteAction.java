package com.timecat.module.git.sgit.activities.delegate.actions;

import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.timecat.element.alert.ToastUtil;
import com.timecat.module.git.R;
import com.timecat.module.git.android.views.SheimiDialogFragment;
import com.timecat.module.git.sgit.activities.RepoDetailActivity;
import com.timecat.module.git.sgit.database.models.Repo;
import com.timecat.module.git.sgit.dialogs.DummyDialogListener;

import java.io.IOException;
import java.util.Set;

import timber.log.Timber;

public class RemoveRemoteAction extends RepoAction {

    public RemoveRemoteAction(Repo repo, RepoDetailActivity activity) {
        super(repo, activity);
    }

    @Override
    public void execute() {
        Set<String> remotes = mRepo.getRemotes();
        if (remotes == null || remotes.isEmpty()) {
            ToastUtil.w_long(R.string.git_alert_please_add_a_remote);
            return;
        }

        RemoveRemoteDialog dialog = new RemoveRemoteDialog();
        dialog.setArguments(mRepo.getBundle());
        dialog.show(mActivity.getSupportFragmentManager(), "remove-remote-dialog");
        mActivity.closeOperationDrawer();
    }

    public static void removeRemote(Repo repo, RepoDetailActivity activity, String remote) throws IOException {
        repo.removeRemote(remote);
        ToastUtil.ok_long(R.string.git_success_remote_removed);
    }

    public static class RemoveRemoteDialog extends SheimiDialogFragment {
        private Repo mRepo;
        private RepoDetailActivity mActivity;
        private ListView mRemoteList;
        private ArrayAdapter<String> mAdapter;

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            super.onCreateDialog(savedInstanceState);
            Bundle args = getArguments();
            if (args != null && args.containsKey(Repo.TAG)) {
                mRepo = (Repo) args.getSerializable(Repo.TAG);
            }

            mActivity = (RepoDetailActivity) getActivity();
            AlertDialog.Builder builder = new AlertDialog.Builder(mActivity);
            LayoutInflater inflater = mActivity.getLayoutInflater();

            View layout = inflater.inflate(R.layout.git_dialog_remove_remote, null);
            mRemoteList = (ListView) layout.findViewById(R.id.remoteList);

            mAdapter = new ArrayAdapter<String>(mActivity,
                    android.R.layout.simple_list_item_1);
            Set<String> remotes = mRepo.getRemotes();
            mAdapter.addAll(remotes);
            mRemoteList.setAdapter(mAdapter);

            mRemoteList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view,
                                        int position, long id) {
                    String remote = mAdapter.getItem(position);
                    try {
                        removeRemote(mRepo, mActivity, remote);
                    } catch (IOException e) {
                        Timber.e(e);
                        mActivity.showMessageDialog(R.string.git_dialog_error_title, getString(R.string.git_error_something_wrong));
                    }
                    dismiss();
                }
            });

            builder.setTitle(R.string.git_dialog_remove_remote_title)
                    .setView(layout)
                    .setNegativeButton(R.string.git_label_cancel, new DummyDialogListener());
            return builder.create();
        }
    }

}
