package com.timecat.module.git.sgit.activities.delegate;

import com.timecat.module.git.android.utils.FsUtils;
import com.timecat.module.git.sgit.activities.RepoDetailActivity;
import com.timecat.module.git.sgit.activities.delegate.actions.AddAllAction;
import com.timecat.module.git.sgit.activities.delegate.actions.AddRemoteAction;
import com.timecat.module.git.sgit.activities.delegate.actions.CherryPickAction;
import com.timecat.module.git.sgit.activities.delegate.actions.CommitAction;
import com.timecat.module.git.sgit.activities.delegate.actions.ConfigAction;
import com.timecat.module.git.sgit.activities.delegate.actions.DeleteAction;
import com.timecat.module.git.sgit.activities.delegate.actions.DiffAction;
import com.timecat.module.git.sgit.activities.delegate.actions.FetchAction;
import com.timecat.module.git.sgit.activities.delegate.actions.MergeAction;
import com.timecat.module.git.sgit.activities.delegate.actions.NewBranchAction;
import com.timecat.module.git.sgit.activities.delegate.actions.NewDirAction;
import com.timecat.module.git.sgit.activities.delegate.actions.NewFileAction;
import com.timecat.module.git.sgit.activities.delegate.actions.PullAction;
import com.timecat.module.git.sgit.activities.delegate.actions.PushAction;
import com.timecat.module.git.sgit.activities.delegate.actions.RawConfigAction;
import com.timecat.module.git.sgit.activities.delegate.actions.RebaseAction;
import com.timecat.module.git.sgit.activities.delegate.actions.RemoveRemoteAction;
import com.timecat.module.git.sgit.activities.delegate.actions.RepoAction;
import com.timecat.module.git.sgit.activities.delegate.actions.ResetAction;
import com.timecat.module.git.sgit.database.models.Repo;
import com.timecat.module.git.sgit.repo.tasks.SheimiAsyncTask.AsyncTaskPostCallback;
import com.timecat.module.git.sgit.repo.tasks.repo.AddToStageTask;
import com.timecat.module.git.sgit.repo.tasks.repo.CheckoutFileTask;
import com.timecat.module.git.sgit.repo.tasks.repo.CheckoutTask;
import com.timecat.module.git.sgit.repo.tasks.repo.DeleteFileFromRepoTask;
import com.timecat.module.git.sgit.repo.tasks.repo.DeleteFileFromRepoTask.DeleteOperationType;
import com.timecat.module.git.sgit.repo.tasks.repo.MergeTask;

import org.eclipse.jgit.lib.Ref;

import java.io.File;
import java.util.ArrayList;

public class RepoOperationDelegate {
    private Repo mRepo;
    private RepoDetailActivity mActivity;
    private ArrayList<RepoAction> mActions = new ArrayList<RepoAction>();

    public RepoOperationDelegate(Repo repo, RepoDetailActivity activity) {
        mRepo = repo;
        mActivity = activity;
        initActions();
    }

    private void initActions() {
        mActions.add(new NewBranchAction(mRepo, mActivity));
        mActions.add(new PullAction(mRepo, mActivity));
        mActions.add(new PushAction(mRepo, mActivity));
        mActions.add(new AddAllAction(mRepo, mActivity));
        mActions.add(new CommitAction(mRepo, mActivity));
        mActions.add(new ResetAction(mRepo, mActivity));
        mActions.add(new MergeAction(mRepo, mActivity));
        mActions.add(new FetchAction(mRepo, mActivity));
        mActions.add(new RebaseAction(mRepo, mActivity));
        mActions.add(new CherryPickAction(mRepo, mActivity));
        mActions.add(new DiffAction(mRepo, mActivity));
        mActions.add(new NewFileAction(mRepo, mActivity));
        mActions.add(new NewDirAction(mRepo, mActivity));
        mActions.add(new AddRemoteAction(mRepo, mActivity));
        mActions.add(new RemoveRemoteAction(mRepo, mActivity));
        mActions.add(new DeleteAction(mRepo, mActivity));
        mActions.add(new RawConfigAction(mRepo, mActivity));
        mActions.add(new ConfigAction(mRepo, mActivity));
    }

    public void executeAction(int key) {
        RepoAction action = mActions.get(key);
        if (action == null)
            return;
        action.execute();
    }

    public void checkoutCommit(final String commitName) {
        CheckoutTask checkoutTask = new CheckoutTask(mRepo, commitName,
                null, new AsyncTaskPostCallback() {
            @Override
            public void onPostExecute(Boolean isSuccess) {
                mActivity.reset(commitName);
            }
        });
        checkoutTask.executeTask();
    }

    public void checkoutCommit(final String commitName, final String branch) {
        CheckoutTask checkoutTask = new CheckoutTask(mRepo, commitName, branch,
                new AsyncTaskPostCallback() {
                    @Override
                    public void onPostExecute(Boolean isSuccess) {
                        mActivity.reset(branch);
                    }
                });
        checkoutTask.executeTask();
    }

    public void mergeBranch(final Ref commit, final String ffModeStr,
                            final boolean autoCommit) {
        MergeTask mergeTask = new MergeTask(mRepo, commit, ffModeStr,
                autoCommit, new AsyncTaskPostCallback() {
            @Override
            public void onPostExecute(Boolean isSuccess) {
                mActivity.reset();
            }
        });
        mergeTask.executeTask();
    }

    public void addToStage(String filepath) {
        String relative = getRelativePath(filepath);
        AddToStageTask addToStageTask = new AddToStageTask(mRepo, relative);
        addToStageTask.executeTask();
    }

    public void checkoutFile(String filepath) {
        String relative = getRelativePath(filepath);
        CheckoutFileTask task = new CheckoutFileTask(mRepo, relative, null);
        task.executeTask();
    }

    public void deleteFileFromRepo(String filepath, DeleteOperationType deleteOperationType) {
        String relative = getRelativePath(filepath);
        DeleteFileFromRepoTask task = new DeleteFileFromRepoTask(mRepo,
                relative, deleteOperationType, new AsyncTaskPostCallback() {
            @Override
            public void onPostExecute(Boolean isSuccess) {
                // TODO Auto-generated method stub
                mActivity.getFilesFragment().reset();
            }
        });
        task.executeTask();
    }

    private String getRelativePath(String filepath) {
        File base = mRepo.getDir();
        String relative = FsUtils.getRelativePath(new File(filepath), base);
        return relative;
    }


}
