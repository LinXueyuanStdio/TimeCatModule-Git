package com.timecat.module.git.sgit.activities.delegate.actions;

import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.ListView;

import com.timecat.element.alert.ToastUtil;
import com.timecat.module.git.R;
import com.timecat.module.git.android.views.SheimiDialogFragment;
import com.timecat.module.git.sgit.activities.RepoDetailActivity;
import com.timecat.module.git.sgit.database.models.Repo;
import com.timecat.module.git.sgit.dialogs.DummyDialogListener;
import com.timecat.module.git.sgit.repo.tasks.repo.PullTask;

import java.util.Set;

public class PullAction extends RepoAction {

    public PullAction(Repo repo, RepoDetailActivity activity) {
        super(repo, activity);
    }

    @Override
    public void execute() {
        Set<String> remotes = mRepo.getRemotes();
        if (remotes == null || remotes.isEmpty()) {
            ToastUtil.w_long(R.string.git_alert_please_add_a_remote);
            return;
        }
        PullDialog pd = new PullDialog();
        pd.setArguments(mRepo.getBundle());
        pd.show(mActivity.getSupportFragmentManager(), "pull-repo-dialog");
        mActivity.closeOperationDrawer();
    }

    private static void pull(Repo repo, RepoDetailActivity activity,
                             String remote, boolean forcePull) {
        PullTask pullTask = new PullTask(repo, remote, forcePull, activity.new ProgressCallback(
                R.string.git_pull_msg_init));
        pullTask.executeTask();
        activity.closeOperationDrawer();
    }

    public static class PullDialog extends SheimiDialogFragment {

        private Repo mRepo;
        private RepoDetailActivity mActivity;
        private CheckBox mForcePull;
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

            View layout = inflater.inflate(R.layout.git_dialog_pull, null);
            mForcePull = (CheckBox) layout.findViewById(R.id.forcePull);
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
                    boolean isForcePull = mForcePull.isChecked();
                    pull(mRepo, mActivity, remote, isForcePull);
                    dismiss();
                }
            });

            builder.setTitle(R.string.git_dialog_pull_repo_title)
                    .setView(layout)
                    .setNegativeButton(R.string.git_label_cancel,
                            new DummyDialogListener());
            return builder.create();
        }
    }

}
