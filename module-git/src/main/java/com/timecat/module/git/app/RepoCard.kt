package com.timecat.module.git.app

import android.content.Context
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.timecat.component.commonsdk.extension.beGone
import com.timecat.component.commonsdk.extension.beVisible
import com.timecat.component.router.app.NAV
import com.timecat.identity.readonly.RouterHub
import com.timecat.layout.ui.entity.BaseAdapter
import com.timecat.layout.ui.entity.BaseItem
import com.timecat.layout.ui.layout.setShakelessClickListener
import com.timecat.layout.ui.utils.IconLoader
import com.timecat.module.git.R
import com.timecat.module.git.android.utils.BasicFunctions
import com.timecat.module.git.sgit.database.RepoContract
import com.timecat.module.git.sgit.database.models.Repo
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.davidea.flexibleadapter.items.IFlexible
import eu.davidea.viewholders.FlexibleViewHolder
import java.text.SimpleDateFormat
import java.util.*

/**
 * @author 林学渊
 * @email linxy59@mail2.sysu.edu.cn
 * @date 2021/1/20
 * @description null
 * @usage null
 */
class RepoCard(
    val repo: Repo,
    val context: Context
) : BaseItem<RepoCard.RepoCardVH>(repo.id.toString()) {
    class RepoCardVH(view: View, adapter: FlexibleAdapter<*>) : FlexibleViewHolder(view, adapter) {
        val repoTitle: TextView = view.findViewById(R.id.repoTitle)
        val repoRemote: TextView = view.findViewById(R.id.repoRemote)
        val progressContainer: View = view.findViewById(R.id.progressContainer)
        val progressMsg: TextView = view.findViewById(R.id.progressMsg)
        val commitMsgContainer: View = view.findViewById(R.id.commitMsgContainer)
        val commitTime: TextView = view.findViewById(R.id.commitTime)
        val commitMsg: TextView = view.findViewById(R.id.commitMsg)
        val commitAuthor: TextView = view.findViewById(R.id.commitAuthor)
        val authorIcon: ImageView = view.findViewById(R.id.authorIcon)
        val cancelBtn: View = view.findViewById(R.id.cancelBtn)
    }

    override fun getLayoutRes(): Int = R.layout.git_repo_listitem

    override fun createViewHolder(view: View, adapter: FlexibleAdapter<IFlexible<RecyclerView.ViewHolder>>): RepoCardVH {
        return RepoCardVH(view, adapter)
    }

    override fun bindViewHolder(adapter: FlexibleAdapter<IFlexible<*>>, holder: RepoCardVH, position: Int, payloads: MutableList<Any>?) {
        if (adapter is BaseAdapter) {
            adapter.bindViewHolderAnimation(holder)
        }
        holder.itemView.setShakelessClickListener { NAV.go(RouterHub.GIT_RepoDetailActivity, Repo.TAG, repo) }
        holder.repoTitle.setText(repo.getDiaplayName())
        holder.repoRemote.setText(repo.getRemoteURL())

        if (repo.getRepoStatus() != RepoContract.REPO_STATUS_NULL) {
            holder.commitMsgContainer.beGone()
            holder.progressContainer.beVisible()
            holder.progressMsg.setText(repo.getRepoStatus())
            holder.cancelBtn.setShakelessClickListener {
                repo.deleteRepo()
                repo.cancelTask()
            }
        } else if (repo.getLastCommitter() != null) {
            holder.commitMsgContainer.beVisible()
            holder.progressContainer.beGone()
            var date: String? = ""
            if (repo.getLastCommitDate() != null) {
                date = SimpleDateFormat("MM/dd/yyyy", Locale.getDefault())
                    .format(repo.getLastCommitDate())
            }
            holder.commitTime.setText(date)
            holder.commitMsg.setText(repo.getLastCommitMsg())
            holder.commitAuthor.setText(repo.getLastCommitter())
            holder.authorIcon.beVisible()
            setAvatarImage(holder.authorIcon, repo.getLastCommitterEmail())
        }

    }

    private fun setAvatarImage(imageView: ImageView, email: String) {
        var avatarUri = ""
        if (!email.isEmpty()) {
            avatarUri = IconLoader.AVATAR_SCHEME + BasicFunctions.md5(email)
        }
        IconLoader.loadIcon(context, imageView, avatarUri)
    }
}