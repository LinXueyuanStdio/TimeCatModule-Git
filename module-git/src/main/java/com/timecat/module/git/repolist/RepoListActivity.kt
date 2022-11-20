package com.timecat.module.git.repolist

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.widget.SearchView
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import com.blankj.utilcode.util.KeyboardUtils
import com.timecat.component.commonsdk.utils.override.LogUtil
import com.timecat.component.identity.Attr
import com.timecat.element.alert.ToastUtil
import com.timecat.identity.readonly.RouterHub
import com.timecat.module.git.R
import com.timecat.module.git.clone.CloneViewModel
import com.timecat.module.git.common.OnActionClickListener
import com.timecat.module.git.databinding.GitActivityRepoListBinding
import com.timecat.module.git.sgit.activities.RepoDetailActivity
import com.timecat.module.git.sgit.activities.SheimiFragmentActivity
import com.timecat.module.git.sgit.activities.UserSettingsActivity
import com.timecat.module.git.sgit.activities.explorer.ExploreFileActivity
import com.timecat.module.git.sgit.activities.explorer.ImportRepositoryActivity
import com.timecat.module.git.sgit.adapters.RepoListAdapter
import com.timecat.module.git.sgit.database.RepoDbManager
import com.timecat.module.git.sgit.database.models.Repo
import com.timecat.module.git.sgit.dialogs.ImportLocalRepoDialog
import com.timecat.module.git.sgit.ssh.PrivateKeyUtils
import com.timecat.module.git.tasks.CloneTask
import com.timecat.module.git.transport.MGitHttpConnectionFactory
import com.timecat.module.git.transport.SSLProviderInstaller
import com.timecat.module.git.utils.PreferenceHelper
import com.timecat.page.base.utils.MenuTintUtils
import com.xiaojinzi.component.anno.RouterAnno
import java.io.File
import java.net.MalformedURLException
import java.net.URL
import java.util.*

@RouterAnno(hostAndPath = RouterHub.GIT_RepoListActivity)
class RepoListActivity : SheimiFragmentActivity() {
    private lateinit var mRepoListAdapter: RepoListAdapter
    private lateinit var binding: GitActivityRepoListBinding
    lateinit var cloneViewModel: CloneViewModel
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        checkAndRequestRequiredPermissions(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        val viewModel = ViewModelProvider(this).get(RepoListViewModel::class.java)
        cloneViewModel = ViewModelProvider(this).get(CloneViewModel::class.java)
        binding = DataBindingUtil.setContentView(this, R.layout.git_activity_repo_list)
        binding.setLifecycleOwner(this)
        binding.setCloneViewModel(cloneViewModel)
        binding.setViewModel(viewModel)
        binding.setClickHandler(object : OnActionClickListener {
            override fun onActionClick(action: String) {
                if (ClickActions.CLONE.name == action) {
                    cloneRepo()
                } else {
                    hideCloneView()
                }
            }
        })
        binding.cloneViewInclude.toolbar.setPaddingStatusBar(this)
        setSupportActionBar(binding.cloneViewInclude.toolbar)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        supportActionBar!!.setTitle("Git")
        PrivateKeyUtils.migratePrivateKeys()
        initUpdatedSSL()
        mRepoListAdapter = RepoListAdapter(this)
        binding.repoList.adapter = mRepoListAdapter
        mRepoListAdapter.queryAllRepo()
        binding.repoList.onItemClickListener = mRepoListAdapter
        binding.repoList.onItemLongClickListener = mRepoListAdapter
        val mContext = applicationContext
        val uri = this.intent.data
        if (uri != null) {
            var mRemoteRepoUrl: URL? = null
            try {
                mRemoteRepoUrl = URL(uri.scheme, uri.host, uri.port, uri.path)
            } catch (e: MalformedURLException) {
                Toast.makeText(mContext, R.string.git_invalid_url, Toast.LENGTH_LONG).show()
                LogUtil.e(e)
            }
            if (mRemoteRepoUrl != null) {
                val remoteUrl = mRemoteRepoUrl.toString()
                var repoName = remoteUrl.substring(remoteUrl.lastIndexOf("/") + 1)
                val repoUrlBuilder = StringBuilder(remoteUrl)

                //need git extension to clone some repos
                if (!remoteUrl.lowercase(Locale.getDefault()).endsWith(getString(R.string.git_git_extension))) {
                    repoUrlBuilder.append(getString(R.string.git_git_extension))
                } else { //if has git extension remove it from repository name
                    repoName = repoName.substring(0, repoName.lastIndexOf('.'))
                }
                //Check if there are others repositories with same remote
                val repositoriesWithSameRemote = Repo
                    .getRepoList(mContext, RepoDbManager.searchRepo(remoteUrl))

                //if so, just open it
                if (repositoriesWithSameRemote.size > 0) {
                    Toast.makeText(mContext, R.string.git_repository_already_present, Toast.LENGTH_SHORT).show()
                    val intent = Intent(mContext, RepoDetailActivity::class.java)
                    intent.putExtra(Repo.TAG, repositoriesWithSameRemote[0])
                    startActivity(intent)
                } else if (Repo.getDir(PreferenceHelper.getInstance(), repoName).exists()) {
                    // Repository with name end already exists, see https://github.com/maks/MGit/issues/289
                    cloneViewModel.remoteUrl = repoUrlBuilder.toString()
                    showCloneView()
                } else {
                    val cloningStatus = getString(R.string.git_cloning)
                    val mRepo = Repo.createRepo(repoName, repoUrlBuilder.toString(), cloningStatus)
                    val isRecursive = true
                    val task = CloneTask(mRepo, true, cloningStatus, null)
                    task.executeTask()
                }
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.git_main, menu)
        val searchItem = menu.findItem(R.id.action_search)
        configSearchAction(searchItem)
        MenuTintUtils.tintAllIcons(menu, Attr.getIconColor(this))
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val intent: Intent
        val i = item.itemId
        if (i == R.id.action_new) {
            showCloneView()
            return true
        } else if (i == R.id.action_import_repo) {
            intent = Intent(this, ImportRepositoryActivity::class.java)
            startActivityForResult(intent, REQUEST_IMPORT_REPO)
            return true
        } else if (i == R.id.action_settings) {
            intent = Intent(this, UserSettingsActivity::class.java)
            startActivity(intent)
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    fun configSearchAction(searchItem: MenuItem) {
        val searchView = searchItem.actionView as SearchView? ?: return
        val searchListener = SearchListener()
        searchItem.setOnActionExpandListener(searchListener)
        searchView.setIconifiedByDefault(true)
        searchView.setOnQueryTextListener(searchListener)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode != RESULT_OK) {
            return
        }
        when (requestCode) {
            REQUEST_IMPORT_REPO -> {
                val path = data!!.extras!!.getString(
                    ExploreFileActivity.RESULT_PATH
                )
                val file = File(path)
                val dotGit = File(file, Repo.DOT_GIT_DIR)
                if (!dotGit.exists()) {
                    ToastUtil.e_long(getString(R.string.git_error_no_repository))
                    return
                }
                showMessageDialog(
                    R.string.git_dialog_comfirm_import_repo_title,
                    R.string.git_dialog_comfirm_import_repo_msg,
                    R.string.git_label_import,
                    object : OnPositiveClickListener {
                        override fun onClick() {
                            val args = Bundle()
                            args.putString(ImportLocalRepoDialog.FROM_PATH, path)
                            val rld = ImportLocalRepoDialog()
                            rld.arguments = args
                            rld.show(supportFragmentManager, "import-local-dialog")
                        }
                    })
            }
        }
    }

    inner class SearchListener : SearchView.OnQueryTextListener, MenuItem.OnActionExpandListener {
        override fun onQueryTextSubmit(s: String): Boolean {
            return false
        }

        override fun onQueryTextChange(s: String): Boolean {
            mRepoListAdapter.searchRepo(s)
            return false
        }

        override fun onMenuItemActionExpand(menuItem: MenuItem): Boolean {
            return true
        }

        override fun onMenuItemActionCollapse(menuItem: MenuItem): Boolean {
            mRepoListAdapter.queryAllRepo()
            return true
        }
    }

    private fun initUpdatedSSL() {
        if (Build.VERSION.SDK_INT < 21) {
            SSLProviderInstaller.install(this)
        }
        MGitHttpConnectionFactory.install()
        LogUtil.i("Installed custom HTTPS factory")
    }

    private fun cloneRepo() {
        if (cloneViewModel.validate()) {
            hideCloneView()
            cloneViewModel.cloneRepo()
        }
    }

    private fun showCloneView() {
        cloneViewModel.show(true)
    }

    private fun hideCloneView() {
        cloneViewModel.show(false)
        KeyboardUtils.hideSoftInput(this)
    }

    companion object {
        private const val REQUEST_IMPORT_REPO = 0
    }
}