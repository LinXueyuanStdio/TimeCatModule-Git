package com.timecat.module.git.sgit.fragments;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import com.timecat.component.commonsdk.utils.override.LogUtil;
import com.timecat.element.alert.ToastUtil;
import com.timecat.module.git.R;
import com.timecat.module.git.sgit.activities.SheimiFragmentActivity;
import com.timecat.module.git.sgit.activities.SheimiFragmentActivity.OnBackClickListener;
import com.timecat.module.git.sgit.activities.ViewFileActivity;
import com.timecat.module.git.sgit.adapters.FilesListAdapter;
import com.timecat.module.git.sgit.database.models.Repo;
import com.timecat.module.git.sgit.dialogs.RepoFileOperationDialog;
import com.timecat.module.git.utils.FsUtils;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;

import androidx.annotation.NonNull;

/**
 * Created by sheimi on 8/5/13.
 */
public class FilesFragment extends RepoDetailFragment {

    private static String CURRENT_DIR = "current_dir";

    private ListView mFilesList;
    private FilesListAdapter mFilesListAdapter;

    private File mCurrentDir;
    private File mRootDir;

    private Repo mRepo;

    public static FilesFragment newInstance(Repo mRepo) {
        FilesFragment fragment = new FilesFragment();
        Bundle bundle = new Bundle();
        bundle.putSerializable(Repo.TAG, mRepo);
        fragment.setArguments(bundle);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.git_fragment_files, container, false);
        getRawActivity().setFilesFragment(this);

        Bundle bundle = getArguments();
        mRepo = (Repo) bundle.getSerializable(Repo.TAG);
        if (mRepo == null && savedInstanceState != null) {
            mRepo = (Repo) savedInstanceState.getSerializable(Repo.TAG);
        }
        if (mRepo == null) {
            return v;
        }
        mRootDir = mRepo.getDir();

        mFilesList = (ListView) v.findViewById(R.id.filesList);

        mFilesListAdapter = new FilesListAdapter(getActivity(), new FileFilter() {
            @Override
            public boolean accept(File file) {
                String name = file.getName();
                if (name.equals(".git")) {return false;}
                return true;
            }
        });
        mFilesList.setAdapter(mFilesListAdapter);

        mFilesList.setOnItemClickListener((adapterView, view, position, id) -> {
            File file = mFilesListAdapter.getItem(position);
            if (file.isDirectory()) {
                setCurrentDir(file);
                return;
            }
            String mime = FsUtils.getMimeType(file);
            if (mime.startsWith("text")) {
                Intent intent = new Intent(getActivity(),
                        ViewFileActivity.class);
                intent.putExtra(ViewFileActivity.TAG_FILE_NAME,
                        file.getAbsolutePath());
                intent.putExtra(Repo.TAG, mRepo);
                getRawActivity().startActivity(intent);
                return;
            }
            try {
                FsUtils.openFile(((SheimiFragmentActivity) getActivity()), file);
            } catch (ActivityNotFoundException e) {
                LogUtil.e(e);
                ((SheimiFragmentActivity) getActivity()).showMessageDialog(R.string.git_dialog_error_title,
                        getString(R.string.git_error_can_not_open_file));
            }
        });

        mFilesList.setOnItemLongClickListener((adapterView, view, position, id) -> {
            File file = mFilesListAdapter.getItem(position);
            RepoFileOperationDialog dialog = new RepoFileOperationDialog();
            Bundle args = new Bundle();
            args.putString(RepoFileOperationDialog.FILE_PATH, file.getAbsolutePath());
            dialog.setArguments(args);
            dialog.show(getChildFragmentManager(), "repo-file-op-dialog");
            return true;
        });

        if (savedInstanceState != null) {
            String currentDirPath = savedInstanceState.getString(CURRENT_DIR);
            if (currentDirPath != null) {
                mCurrentDir = new File(currentDirPath);
                setCurrentDir(mCurrentDir);
            }
        }
        reset();
        return v;
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putSerializable(Repo.TAG, mRepo);
        if (mCurrentDir != null) {
            outState.putString(CURRENT_DIR, mCurrentDir.getAbsolutePath());
        }
    }

    /**
     * Set the directory listing currently being displayed
     *
     * @param dir
     */
    public void setCurrentDir(File dir) {
        mCurrentDir = dir;
        if (mFilesListAdapter != null) {
            mFilesListAdapter.setDir(mCurrentDir);
        }
    }

    /**
     * If the root dir has previously been set, set the root dir to be the currently displayed
     * directory listing.
     */
    public void resetCurrentDir() {
        if (mRootDir == null) {return;}
        setCurrentDir(mRootDir);
    }

    @Override
    public void reset() {
        resetCurrentDir();
    }

    public void newDir(String name) {
        File file = new File(mCurrentDir, name);
        if (file.exists()) {
            ToastUtil.e_long(R.string.git_alert_file_exists);
            return;
        }
        file.mkdir();
        setCurrentDir(mCurrentDir);
    }

    /**
     * Create a new file within the currently displayed directory
     *
     * @param name
     */
    public void newFile(String name) throws IOException {
        File file = new File(mCurrentDir, name);
        if (file.exists()) {
            ToastUtil.e_long(R.string.git_alert_file_exists);
            return;
        }
        file.createNewFile();
        setCurrentDir(mCurrentDir);
    }

    @Override
    public OnBackClickListener getOnBackClickListener() {
        return new OnBackClickListener() {
            @Override
            public boolean onClick() {
                if (mRootDir == null || mCurrentDir == null) {return false;}
                if (mRootDir.equals(mCurrentDir)) {return false;}
                File parent = mCurrentDir.getParentFile();
                setCurrentDir(parent);
                return true;
            }
        };
    }
}
