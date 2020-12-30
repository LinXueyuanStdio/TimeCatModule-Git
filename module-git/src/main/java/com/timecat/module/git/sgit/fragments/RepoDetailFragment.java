package com.timecat.module.git.sgit.fragments;

import com.timecat.module.git.sgit.activities.RepoDetailActivity;

public abstract class RepoDetailFragment extends BaseFragment {

    public RepoDetailActivity getRawActivity() {
        return (RepoDetailActivity) super.getRawActivity();
    }

}
