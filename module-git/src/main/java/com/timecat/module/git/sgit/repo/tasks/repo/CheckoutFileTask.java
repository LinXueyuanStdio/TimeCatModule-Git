package com.timecat.module.git.sgit.repo.tasks.repo;

import com.timecat.module.git.R;
import com.timecat.module.git.sgit.database.models.Repo;
import com.timecat.module.git.sgit.exception.StopTaskException;

public class CheckoutFileTask extends RepoOpTask {

    private AsyncTaskPostCallback mCallback;
    private String mPath;

    public CheckoutFileTask(Repo repo, String path,
                            AsyncTaskPostCallback callback) {
        super(repo);
        mCallback = callback;
        mPath = path;
        setSuccessMsg(R.string.git_success_checkout_file);
    }

    @Override
    protected Boolean doInBackground(Void... params) {
        return checkout();
    }

    protected void onPostExecute(Boolean isSuccess) {
        super.onPostExecute(isSuccess);
        if (mCallback != null) {
            mCallback.onPostExecute(isSuccess);
        }
    }

    private boolean checkout() {
        try {
            mRepo.getGit().checkout().addPath(mPath).call();
        } catch (StopTaskException e) {
            return false;
        } catch (Throwable e) {
            setException(e);
            return false;
        }
        return true;
    }

}
