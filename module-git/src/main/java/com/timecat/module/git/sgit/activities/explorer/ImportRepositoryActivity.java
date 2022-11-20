package com.timecat.module.git.sgit.activities.explorer;

import android.app.Activity;
import android.content.Intent;
import android.os.Environment;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.AdapterView;

import com.timecat.component.identity.Attr;
import com.timecat.element.alert.ToastUtil;
import com.timecat.module.git.R;
import com.timecat.module.git.sgit.database.models.Repo;
import com.timecat.module.git.tasks.InitLocalTask;
import com.timecat.page.base.utils.MenuTintUtils;

import java.io.File;
import java.io.FileFilter;

public class ImportRepositoryActivity extends FileExplorerActivity {

    @Override
    protected File getRootFolder() {
        return Environment.getExternalStorageDirectory();
    }

    @Override
    protected FileFilter getExplorerFileFilter() {
        return new FileFilter() {
            @Override
            public boolean accept(File file) {
                return file.isDirectory();
            }
        };
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.git_import_repo, menu);
        MenuTintUtils.tintAllIcons(menu, Attr.getIconColor(this));
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int i = item.getItemId();
        if (i == R.id.action_create_external) {
            File dotGit = new File(getCurrentDir(), Repo.DOT_GIT_DIR);
            if (dotGit.exists()) {
                ToastUtil.w_long(R.string.git_alert_is_already_a_git_repo);
                return true;
            }
            showMessageDialog(R.string.git_dialog_create_external_title, R.string.git_dialog_create_external_msg, R.string.git_dialog_create_external_positive_label, new OnPositiveClickListener() {
                @Override
                public void onClick() {
                    createExternalGitRepo();
                }
            });
            return true;
        } else if (i == R.id.action_import_external) {
            Intent intent = new Intent();
            intent.putExtra(RESULT_PATH, getCurrentDir().getAbsolutePath());
            setResult(Activity.RESULT_OK, intent);
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected AdapterView.OnItemClickListener getOnListItemClickListener() {
        return (adapterView, view, position, id) -> {
            File file = mFilesListAdapter.getItem(position);
            if (file.isDirectory()) {
                setCurrentDir(file);
            }
        };
    }

    @Override
    protected AdapterView.OnItemLongClickListener getOnListItemLongClickListener() {
        return null;
    }

    void createExternalGitRepo() {
        File current = getCurrentDir();
        String localPath = Repo.EXTERNAL_PREFIX + current;

        Repo repo = Repo.createRepo(localPath, "local repository", getString(R.string.git_importing));

        InitLocalTask task = new InitLocalTask(repo);
        task.executeTask();
        finish();
    }
}
