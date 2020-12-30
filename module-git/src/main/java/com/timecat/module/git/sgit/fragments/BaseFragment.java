package com.timecat.module.git.sgit.fragments;

import android.content.Context;

import androidx.fragment.app.Fragment;

import com.timecat.module.git.android.activities.SheimiFragmentActivity;
import com.timecat.module.git.android.activities.SheimiFragmentActivity.OnBackClickListener;

/**
 * Created by sheimi on 8/7/13.
 */
public abstract class BaseFragment extends Fragment {

    public abstract OnBackClickListener getOnBackClickListener();

    private SheimiFragmentActivity mActivity;

    public abstract void reset();

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mActivity = (SheimiFragmentActivity) context;
    }

    public SheimiFragmentActivity getRawActivity() {
        return mActivity;
    }
}
