package com.timecat.module.git.repolist

import com.xiaojinzi.component.anno.FragmentAnno
import com.timecat.identity.readonly.RouterHub
import com.timecat.page.base.friend.main.BaseMainStatefulRefreshListFragment
import com.timecat.module.git.sgit.database.RepoDbManager
import com.timecat.module.git.sgit.database.models.Repo
import com.timecat.module.git.R
import com.timecat.component.router.app.NAV
import com.chad.library.adapter.base.BaseQuickAdapter
import com.timecat.module.git.sgit.database.RepoContract
import com.timecat.layout.ui.utils.IconLoader
import com.timecat.module.git.utils.BasicFunctions
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import com.timecat.module.git.utils.PreferenceHelper
import java.text.SimpleDateFormat
import java.util.*

/**
 * @author dlink
 * @email linxy59@mail2.sysu.edu.cn
 * @date 2019/7/3
 * @description null
 * @usage null
 */
@FragmentAnno(RouterHub.GIT_RepoListFragment)
class RepoListFragment : BaseMainStatefulRefreshListFragment() {
    private var mRepoListAdapter: RepoListAdapter? = null
    override fun onRefresh() {
        val cursor = RepoDbManager.queryAllRepo()
        val repo = Repo.getRepoList(context, cursor)
        Collections.sort(repo)
        cursor.close()
        if (mRepoListAdapter != null) {
            mRepoListAdapter!!.setList(repo)
        }
        mRefreshLayout.isRefreshing = false
    }

    override fun getAdapter(): RecyclerView.Adapter<out RecyclerView.ViewHolder?> {
        mRepoListAdapter = RepoListAdapter()
        return mRepoListAdapter!!
    }

    override fun title(): String {
        return "Git"
    }

    override fun initView() {}
    override fun getMenuId(): Int {
        return R.menu.git_main_fragment
    }

    override fun onMenuItemClick(item: MenuItem): Boolean {
        if (item.itemId == R.id.action_home) {
            NAV.go(RouterHub.GIT_RepoListActivity)
        }
        return super.onMenuItemClick(item)
    }

    internal inner class RepoListAdapter : BaseQuickAdapter<Repo, BaseViewHolder>(R.layout.git_repo_listitem) {
        override fun convert(holder: BaseViewHolder, repo: Repo) {
            holder.itemView.setOnClickListener { NAV.go(RouterHub.GIT_RepoDetailActivity, Repo.TAG, repo) }
            holder.setText(R.id.repoTitle, repo.diaplayName)
            holder.setText(R.id.repoRemote, repo.remoteURL)
            if (repo.repoStatus != RepoContract.REPO_STATUS_NULL) {
                holder.setVisible(R.id.commitMsgContainer, false)
                holder.setVisible(R.id.progressContainer, true)
                holder.setText(R.id.progressMsg, repo.repoStatus)
                holder.getView<View>(R.id.cancelBtn).setOnClickListener {
                    repo.deleteRepo()
                    repo.cancelTask()
                }
            } else if (repo.lastCommitter != null) {
                holder.setVisible(R.id.commitMsgContainer, true)
                holder.setVisible(R.id.progressContainer, false)
                var date: String? = ""
                if (repo.lastCommitDate != null) {
                    date = SimpleDateFormat("MM/dd/yyyy", Locale.getDefault())
                        .format(repo.lastCommitDate)
                }
                holder.setText(R.id.commitTime, date)
                holder.setText(R.id.commitMsg, repo.lastCommitMsg)
                holder.setText(R.id.commitAuthor, repo.lastCommitter)
                holder.setVisible(R.id.authorIcon, true)
                setAvatarImage(holder.getView(R.id.authorIcon), repo.lastCommitterEmail)
            }
        }
    }

    fun setAvatarImage(imageView: ImageView?, email: String) {
        var avatarUri = ""
        if (!email.isEmpty()) {
            avatarUri = IconLoader.AVATAR_SCHEME + BasicFunctions.md5(email)
        }
        IconLoader.loadIcon(_mActivity, imageView, avatarUri)
    }

    /**
     * Checks if the use of Gravatar is enabled in the preferences.
     *
     * @return true if the use of Gravatar to retrieve Avatar images is enabled, false otherwise
     */
    protected val isGravatarEnabled: Boolean
        protected get() {
            val sharedPreference = PreferenceHelper.getInstance().sharedPrefs
            return sharedPreference.getBoolean(_mActivity.getString(R.string.git_pref_key_use_gravatar), true)
        }
}