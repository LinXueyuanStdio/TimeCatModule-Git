package com.timecat.module.git.sgit.activities.explorer;

import android.os.Bundle;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.timecat.page.base.utils.MenuTintUtils;
import com.timecat.page.base.view.BlurringToolbar;
import com.timecat.component.identity.Attr;
import com.timecat.module.git.R;
import com.timecat.module.git.sgit.activities.SheimiFragmentActivity;
import com.timecat.module.git.sgit.adapters.FilesListAdapter;

import java.io.File;
import java.io.FileFilter;

public abstract class FileExplorerActivity extends SheimiFragmentActivity {

    public static final String RESULT_PATH = "result_path";

    private File mRootFolder;
    private File mCurrentDir;
    private ListView mFileList;
    protected FilesListAdapter mFilesListAdapter;
    private TextView mCurrentPathView;
    private TextView mUpDir;
    private ImageView mUpDirIcon;

    protected abstract File getRootFolder();

    protected abstract FileFilter getExplorerFileFilter();

    protected abstract AdapterView.OnItemClickListener getOnListItemClickListener();

    protected abstract AdapterView.OnItemLongClickListener getOnListItemLongClickListener();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.git_activity_file_list);
        BlurringToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setPaddingStatusBar(this);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        mRootFolder = getRootFolder();
        mCurrentDir = mRootFolder;

        mFileList = (ListView) findViewById(R.id.fileList);
        mCurrentPathView = (TextView) findViewById(R.id.currentPath);
        mCurrentPathView.setText(mCurrentDir.getPath());

        mUpDirIcon = (ImageView) findViewById(R.id.upDirIcon);
        mUpDirIcon.setImageResource(R.drawable.ic_folder_special_24dp);

        mUpDir = (TextView) findViewById(R.id.upDir);
        mUpDir.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                File parent = mCurrentDir.getParentFile();
                if (parent != null) {
                    setCurrentDir(parent);
                }
            }
        });

        mFilesListAdapter = new FilesListAdapter(this, getExplorerFileFilter());
        mFileList.setAdapter(mFilesListAdapter);
        mFilesListAdapter.setDir(mRootFolder);

        mFileList.setOnItemClickListener(getOnListItemClickListener());
        mFileList.setOnItemLongClickListener(getOnListItemLongClickListener());

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.git_empty_menu, menu);
        MenuTintUtils.tintAllIcons(menu, Attr.getIconColor(this));
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            final File parent = mCurrentDir.getParentFile();
            if (!mRootFolder.equals(mCurrentDir) && (parent != null)) {
                setCurrentDir(parent);
                return true;
            }
            finish();
            return true;
        }
        return false;
    }

    protected void setCurrentDir(File dir) {
        mCurrentDir = dir;
        mFilesListAdapter.setDir(mCurrentDir);
        mCurrentPathView.setText(mCurrentDir.getPath());

        if (dir.getParentFile() == null) {
            mUpDir.setVisibility(View.GONE);
            mUpDirIcon.setVisibility(View.GONE);
        } else {
            mUpDir.setVisibility(View.VISIBLE);
            mUpDirIcon.setVisibility(View.VISIBLE);
        }

    }

    protected File getCurrentDir() {
        return mCurrentDir;
    }

}
