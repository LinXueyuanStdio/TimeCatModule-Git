package com.timecat.module.git.repolist;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.appcompat.widget.SearchView;
import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.ViewModelProvider;

import com.xiaojinzi.component.anno.RouterAnno;
import com.timecat.element.alert.ToastUtil;
import com.timecat.page.base.utils.MenuTintUtils;
import com.timecat.identity.readonly.RouterHub;
import com.timecat.component.identity.Attr;
import com.timecat.module.git.R;
import com.timecat.module.git.ViewHelperKt;
import com.timecat.module.git.android.activities.SheimiFragmentActivity;
import com.timecat.module.git.clone.CloneViewModel;
import com.timecat.module.git.common.OnActionClickListener;
import com.timecat.module.git.databinding.GitActivityRepoListBinding;
import com.timecat.module.git.sgit.activities.RepoDetailActivity;
import com.timecat.module.git.sgit.activities.UserSettingsActivity;
import com.timecat.module.git.sgit.activities.explorer.ExploreFileActivity;
import com.timecat.module.git.sgit.activities.explorer.ImportRepositoryActivity;
import com.timecat.module.git.sgit.adapters.RepoListAdapter;
import com.timecat.module.git.sgit.database.RepoDbManager;
import com.timecat.module.git.sgit.database.models.Repo;
import com.timecat.module.git.sgit.dialogs.ImportLocalRepoDialog;
import com.timecat.module.git.sgit.preference.PreferenceHelper;
import com.timecat.module.git.sgit.repo.tasks.repo.CloneTask;
import com.timecat.module.git.sgit.ssh.PrivateKeyUtils;
import com.timecat.module.git.transport.MGitHttpConnectionFactory;
import com.timecat.module.git.transport.SSLProviderInstaller;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import timber.log.Timber;

@RouterAnno(hostAndPath = RouterHub.GIT_RepoListActivity)
public class RepoListActivity extends SheimiFragmentActivity {

    private RepoListAdapter mRepoListAdapter;

    private static final int REQUEST_IMPORT_REPO = 0;

    private GitActivityRepoListBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        checkAndRequestRequiredPermissions(Manifest.permission.WRITE_EXTERNAL_STORAGE);

        RepoListViewModel viewModel = new ViewModelProvider(this).get(RepoListViewModel.class);
        CloneViewModel cloneViewModel = new ViewModelProvider(this).get(CloneViewModel.class);

        binding = DataBindingUtil.setContentView(this, R.layout.git_activity_repo_list);
        binding.setLifecycleOwner(this);
        binding.setCloneViewModel(cloneViewModel);
        binding.setViewModel(viewModel);
        binding.setClickHandler(new OnActionClickListener() {
            @Override
            public void onActionClick(String action) {
                if (ClickActions.CLONE.name().equals(action)) {
                    cloneRepo();
                } else {
                    hideCloneView();
                }
            }
        });
        binding.cloneViewInclude.toolbar.setPaddingStatusBar(this);
        setSupportActionBar(binding.cloneViewInclude.toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("Git");

        PrivateKeyUtils.migratePrivateKeys();

        initUpdatedSSL();

        mRepoListAdapter = new RepoListAdapter(this);
        binding.repoList.setAdapter(mRepoListAdapter);
        mRepoListAdapter.queryAllRepo();
        binding.repoList.setOnItemClickListener(mRepoListAdapter);
        binding.repoList.setOnItemLongClickListener(mRepoListAdapter);
        Context mContext = getApplicationContext();

        Uri uri = this.getIntent().getData();
        if (uri != null) {
            URL mRemoteRepoUrl = null;
            try {
                mRemoteRepoUrl = new URL(uri.getScheme(), uri.getHost(), uri.getPort(), uri.getPath());
            } catch (MalformedURLException e) {
                Toast.makeText(mContext, R.string.git_invalid_url, Toast.LENGTH_LONG).show();
                Timber.e(e);
            }

            if (mRemoteRepoUrl != null) {
                String remoteUrl = mRemoteRepoUrl.toString();
                String repoName = remoteUrl.substring(remoteUrl.lastIndexOf("/") + 1);
                StringBuilder repoUrlBuilder = new StringBuilder(remoteUrl);

                //need git extension to clone some repos
                if (!remoteUrl.toLowerCase().endsWith(getString(R.string.git_git_extension))) {
                    repoUrlBuilder.append(getString(R.string.git_git_extension));
                } else { //if has git extension remove it from repository name
                    repoName = repoName.substring(0, repoName.lastIndexOf('.'));
                }
                //Check if there are others repositories with same remote
                List<Repo> repositoriesWithSameRemote = Repo
                        .getRepoList(mContext, RepoDbManager.searchRepo(remoteUrl));

                //if so, just open it
                if (repositoriesWithSameRemote.size() > 0) {
                    Toast.makeText(mContext, R.string.git_repository_already_present, Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(mContext, RepoDetailActivity.class);
                    intent.putExtra(Repo.TAG, repositoriesWithSameRemote.get(0));
                    startActivity(intent);
                } else if (Repo.getDir(PreferenceHelper.getInstance(), repoName).exists()) {
                    // Repository with name end already exists, see https://github.com/maks/MGit/issues/289
                    cloneViewModel.setRemoteUrl(repoUrlBuilder.toString());
                    showCloneView();
                } else {
                    final String cloningStatus = getString(R.string.git_cloning);
                    Repo mRepo = Repo.createRepo(repoName, repoUrlBuilder.toString(), cloningStatus);
                    Boolean isRecursive = true;
                    CloneTask task = new CloneTask(mRepo, true, cloningStatus, null);
                    task.executeTask();
                }
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.git_main, menu);
        MenuItem searchItem = menu.findItem(R.id.action_search);
        configSearchAction(searchItem);
        MenuTintUtils.tintAllIcons(menu, Attr.getIconColor(this));
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent intent;
        int i = item.getItemId();
        if (i == R.id.action_new) {
            showCloneView();
            return true;
        } else if (i == R.id.action_import_repo) {
            intent = new Intent(this, ImportRepositoryActivity.class);
            startActivityForResult(intent, REQUEST_IMPORT_REPO);
            return true;
        } else if (i == R.id.action_settings) {
            intent = new Intent(this, UserSettingsActivity.class);
            startActivity(intent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void configSearchAction(MenuItem searchItem) {
        SearchView searchView = (SearchView) searchItem.getActionView();
        if (searchView == null) {
            return;
        }
        SearchListener searchListener = new SearchListener();
        searchItem.setOnActionExpandListener(searchListener);
        searchView.setIconifiedByDefault(true);
        searchView.setOnQueryTextListener(searchListener);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != Activity.RESULT_OK) {
            return;
        }
        switch (requestCode) {
            case REQUEST_IMPORT_REPO:
                final String path = data.getExtras().getString(
                        ExploreFileActivity.RESULT_PATH);
                File file = new File(path);
                File dotGit = new File(file, Repo.DOT_GIT_DIR);
                if (!dotGit.exists()) {
                    ToastUtil.e_long(getString(R.string.git_error_no_repository));
                    return;
                }
                showMessageDialog(
                        R.string.git_dialog_comfirm_import_repo_title,
                        R.string.git_dialog_comfirm_import_repo_msg,
                        R.string.git_label_import,
                        () -> {
                            Bundle args = new Bundle();
                            args.putString(ImportLocalRepoDialog.FROM_PATH, path);
                            ImportLocalRepoDialog rld = new ImportLocalRepoDialog();
                            rld.setArguments(args);
                            rld.show(getSupportFragmentManager(), "import-local-dialog");
                        });
                break;
        }
    }

    public class SearchListener implements SearchView.OnQueryTextListener, MenuItem.OnActionExpandListener {

        @Override
        public boolean onQueryTextSubmit(String s) {
            return false;
        }

        @Override
        public boolean onQueryTextChange(String s) {
            mRepoListAdapter.searchRepo(s);
            return false;
        }

        @Override
        public boolean onMenuItemActionExpand(MenuItem menuItem) {
            return true;
        }

        @Override
        public boolean onMenuItemActionCollapse(MenuItem menuItem) {
            mRepoListAdapter.queryAllRepo();
            return true;
        }

    }

    private void initUpdatedSSL() {
        if (Build.VERSION.SDK_INT < 21) {
            SSLProviderInstaller.install(this);
        }
        MGitHttpConnectionFactory.install();
        Timber.i("Installed custom HTTPS factory");
    }

    private void cloneRepo() {
        if (binding.getCloneViewModel().validate()) {
            hideCloneView();
            binding.getCloneViewModel().cloneRepo();
        }
    }

    private void showCloneView() {
        binding.getCloneViewModel().show(true);
    }

    private void hideCloneView() {
        binding.getCloneViewModel().show(false);
        ViewHelperKt.hideKeyboard(this);
    }
}