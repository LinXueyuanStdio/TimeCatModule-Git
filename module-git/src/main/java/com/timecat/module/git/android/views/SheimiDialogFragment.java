package com.timecat.module.git.android.views;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;

import com.timecat.module.git.android.activities.SheimiFragmentActivity;

public class SheimiDialogFragment extends DialogFragment {

    // It's safe to assume onAttach is called before other code.
    @SuppressWarnings("NullableProblems")
    @NonNull
    private SheimiFragmentActivity mActivity;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        mActivity = (SheimiFragmentActivity) context;
    }

    @NonNull
    public SheimiFragmentActivity getRawActivity() {
        return mActivity;
    }
}
