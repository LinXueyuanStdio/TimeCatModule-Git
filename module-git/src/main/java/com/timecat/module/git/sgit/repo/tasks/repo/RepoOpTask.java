package com.timecat.module.git.sgit.repo.tasks.repo;

import com.timecat.element.alert.ToastUtil;
import com.timecat.module.git.R;
import com.timecat.module.git.android.activities.SheimiFragmentActivity.OnPasswordEntered;
import com.timecat.module.git.android.utils.BasicFunctions;
import com.timecat.module.git.sgit.database.models.Repo;
import com.timecat.module.git.sgit.repo.tasks.SheimiAsyncTask;

import org.eclipse.jgit.api.TransportCommand;
import org.eclipse.jgit.lib.ProgressMonitor;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;

import timber.log.Timber;

public abstract class RepoOpTask extends SheimiAsyncTask<Void, String, Boolean> {

    protected Repo mRepo;
    protected boolean mIsTaskAdded;
    private int mSuccessMsg = 0;

    public RepoOpTask(Repo repo) {
        mRepo = repo;
        mIsTaskAdded = repo.addTask(this);
    }

    protected void onPostExecute(Boolean isSuccess) {
        super.onPostExecute(isSuccess);
        mRepo.removeTask(this);
        if (!isSuccess && !isTaskCanceled()) {
            BasicFunctions.showException(BasicFunctions.getActiveActivity(), mException, mErrorRes, getErrorTitleRes());
        }
        if (isSuccess && mSuccessMsg != 0) {
            ToastUtil.ok_long(mSuccessMsg);
        }
    }

    protected void setSuccessMsg(int successMsg) {
        mSuccessMsg = successMsg;
    }

    public void executeTask() {
        if (mIsTaskAdded) {
            execute();
            return;
        }
        ToastUtil.w_long(R.string.git_error_task_running);
    }

    protected void setCredentials(TransportCommand command) {
        String username = mRepo.getUsername();
        String password = mRepo.getPassword();

        if (username != null && password != null && !username.trim().isEmpty()
                && !password.trim().isEmpty()) {
            UsernamePasswordCredentialsProvider auth = new UsernamePasswordCredentialsProvider(
                    username, password);
            command.setCredentialsProvider(auth);
        } else {
            Timber.d("no CredentialsProvider when no username/password provided");
        }

    }

    protected void handleAuthError(OnPasswordEntered onPassEntered) {
        String msg = mException.getMessage();
        Timber.w("clone Auth error: %s", msg);

        if (msg == null || ((!msg.contains("Auth fail"))
                && (!msg.toLowerCase().contains("auth")))) {
            return;
        }

        String errorInfo = null;
        if (msg.contains("Auth fail")) {
            errorInfo = BasicFunctions.getActiveActivity().getString(
                    R.string.git_dialog_prompt_for_password_title_auth_fail);
        }
        BasicFunctions.getActiveActivity().promptForPassword(onPassEntered, errorInfo);
    }

    class BasicProgressMonitor implements ProgressMonitor {

        private int mTotalWork;
        private int mWorkDone;
        private int mLastProgress;
        private String mTitle;

        @Override
        public void start(int i) {
        }

        @Override
        public void beginTask(String title, int totalWork) {
            mTotalWork = totalWork;
            mWorkDone = 0;
            mLastProgress = 0;
            if (title != null) {
                mTitle = title;
            }
            setProgress();
        }

        @Override
        public void update(int i) {
            mWorkDone += i;
            if (mTotalWork != ProgressMonitor.UNKNOWN && mTotalWork != 0 && mTotalWork - mLastProgress >= 1) {
                setProgress();
                mLastProgress = mWorkDone;
            }
        }

        @Override
        public void endTask() {
        }

        @Override
        public boolean isCancelled() {
            return isTaskCanceled();
        }

        private void setProgress() {
            String msg = mTitle;
            int showedWorkDown = Math.min(mWorkDone, mTotalWork);
            int progress = 0;
            String rightHint = "0/0";
            String leftHint = "0%";
            if (mTotalWork != 0) {
                progress = 100 * showedWorkDown / mTotalWork;
                rightHint = showedWorkDown + "/" + mTotalWork;
                leftHint = progress + "%";
            }
            publishProgress(msg, leftHint, rightHint,
                    Integer.toString(progress));
        }

    }

}