package com.timecat.module.git.sgit.activities.delegate.actions;

import android.content.Intent;

import com.timecat.module.git.sgit.activities.RepoDetailActivity;
import com.timecat.module.git.sgit.activities.ViewFileActivity;
import com.timecat.module.git.sgit.database.models.Repo;

/**
 * Created by phcoder on 05.12.15.
 */
public class RawConfigAction extends RepoAction {

    public RawConfigAction(Repo repo, RepoDetailActivity activity) {
        super(repo, activity);
    }

    @Override
    public void execute() {
        Intent intent = new Intent(mActivity, ViewFileActivity.class);
        intent.putExtra(ViewFileActivity.TAG_FILE_NAME,
                mRepo.getDir().getAbsoluteFile() + "/.git/config");
        mActivity.startActivity(intent);
    }
}
