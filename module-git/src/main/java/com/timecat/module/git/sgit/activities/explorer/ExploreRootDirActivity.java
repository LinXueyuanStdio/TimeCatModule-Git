package com.timecat.module.git.sgit.activities.explorer;

import android.os.Environment;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;

import com.timecat.page.base.utils.MenuTintUtils;
import com.timecat.component.identity.Attr;
import com.timecat.module.git.R;
import com.timecat.module.git.sgit.database.models.Repo;

import java.io.File;
import java.io.FileFilter;

public class ExploreRootDirActivity extends FileExplorerActivity {


    @Override
    protected File getRootFolder() {
        return Environment.getExternalStorageDirectory();
    }

    @Override
    protected FileFilter getExplorerFileFilter() {
        return new FileFilter() {
            @Override
            public boolean accept(File file) {
                String filename = file.getName();
                return !filename.startsWith(".") && file.isDirectory();
            }
        };
    }

    @Override
    protected AdapterView.OnItemClickListener getOnListItemClickListener() {
        return new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view,
                                    int position, long id) {
                File file = mFilesListAdapter.getItem(position);
                if (file.isDirectory()) {
                    setCurrentDir(file);
                    return;
                }
            }
        };
    }

    @Override
    protected AdapterView.OnItemLongClickListener getOnListItemLongClickListener() {
        return null;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.git_select_root, menu);
        MenuTintUtils.tintAllIcons(menu, Attr.getIconColor(this));
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int i = item.getItemId();
        if (i == R.id.action_select_root) {
            Repo.setLocalRepoRoot(this, getCurrentDir());
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

}
