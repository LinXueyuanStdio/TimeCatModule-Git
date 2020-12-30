package com.timecat.module.git.sgit.repo.tasks.repo;

import com.timecat.module.git.R;
import com.timecat.module.git.sgit.database.models.Repo;
import com.timecat.module.git.sgit.exception.StopTaskException;

public class AddToStageTask extends RepoOpTask {

    public String mFilePattern;

    public AddToStageTask(Repo repo, String filepattern) {
        super(repo);
        mFilePattern = filepattern;
        setSuccessMsg(R.string.git_success_add_to_stage);
    }

    @Override
    protected Boolean doInBackground(Void... params) {
        return addToStage();
    }

    protected void onPostExecute(Boolean isSuccess) {
        super.onPostExecute(isSuccess);
    }

    public boolean addToStage() {
        try {
            mRepo.getGit().add().addFilepattern(mFilePattern).call();
        } catch (StopTaskException e) {
            return false;
        } catch (Throwable e) {
            setException(e);
            return false;
        }
        return true;
    }
}
