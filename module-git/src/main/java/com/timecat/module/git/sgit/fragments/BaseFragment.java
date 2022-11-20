package com.timecat.module.git.sgit.fragments;

import android.content.Context;

import com.timecat.module.git.sgit.activities.SheimiFragmentActivity;
import com.timecat.module.git.sgit.activities.SheimiFragmentActivity.OnBackClickListener;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

/**
 * Created by sheimi on 8/7/13.
 */
public abstract class BaseFragment extends Fragment {

    public abstract OnBackClickListener getOnBackClickListener();

    private SheimiFragmentActivity mActivity;

    public abstract void reset();

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        mActivity = (SheimiFragmentActivity) context;
    }

    public SheimiFragmentActivity getRawActivity() {
        return mActivity;
    }
}
