package com.timecat.module.git.repolist;

import android.content.SharedPreferences;
import android.database.Cursor;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.viewholder.BaseViewHolder;
import com.timecat.page.base.friend.main.BaseMainStatefulRefreshListFragment;
import com.timecat.identity.readonly.RouterHub;
import com.timecat.component.router.app.NAV;
import com.timecat.layout.ui.utils.IconLoader;
import com.timecat.module.git.R;
import com.timecat.module.git.android.utils.BasicFunctions;
import com.timecat.module.git.sgit.database.RepoContract;
import com.timecat.module.git.sgit.database.RepoDbManager;
import com.timecat.module.git.sgit.database.models.Repo;
import com.timecat.module.git.sgit.preference.PreferenceHelper;
import com.xiaojinzi.component.anno.FragmentAnno;

import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import static com.timecat.layout.ui.utils.IconLoader.AVATAR_SCHEME;

/**
 * @author dlink
 * @email linxy59@mail2.sysu.edu.cn
 * @date 2019/7/3
 * @description null
 * @usage null
 */
@FragmentAnno(RouterHub.GIT_RepoListFragment)
public class RepoListFragment extends BaseMainStatefulRefreshListFragment {

    private RepoListAdapter mRepoListAdapter;

    @Override
    public void onRefresh() {
        Cursor cursor = RepoDbManager.queryAllRepo();
        List<Repo> repo = Repo.getRepoList(getContext(), cursor);
        Collections.sort(repo);
        cursor.close();
        if (mRepoListAdapter != null) {
            mRepoListAdapter.setList(repo);
        }
        mRefreshLayout.setRefreshing(false);
    }

    @NonNull
    @Override
    protected RecyclerView.Adapter<? extends RecyclerView.ViewHolder> getAdapter() {
        mRepoListAdapter = new RepoListAdapter();
        return mRepoListAdapter;
    }

    @NonNull
    @Override
    protected String title() {
        return "Git";
    }

    @Override
    protected void initView() {

    }

    @Override
    protected int getMenuId() {
        return R.menu.git_main_fragment;
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        if (item.getItemId() == R.id.action_home) {
            NAV.go(RouterHub.GIT_RepoListActivity);
        }
        return super.onMenuItemClick(item);
    }

    class RepoListAdapter extends BaseQuickAdapter<Repo, BaseViewHolder> {

        public RepoListAdapter() {
            super(R.layout.git_repo_listitem);
        }

        @Override
        protected void convert(BaseViewHolder holder, Repo repo) {
            holder.itemView.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    NAV.go(RouterHub.GIT_RepoDetailActivity, Repo.TAG, repo);
                }
            });
            holder.setText(R.id.repoTitle, repo.getDiaplayName());
            holder.setText(R.id.repoRemote, repo.getRemoteURL());

            if (!repo.getRepoStatus().equals(RepoContract.REPO_STATUS_NULL)) {
                holder.setVisible(R.id.commitMsgContainer, false);
                holder.setVisible(R.id.progressContainer, true);
                holder.setText(R.id.progressMsg, repo.getRepoStatus());
                holder.getView(R.id.cancelBtn).setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        repo.deleteRepo();
                        repo.cancelTask();
                    }
                });
            } else if (repo.getLastCommitter() != null) {
                holder.setVisible(R.id.commitMsgContainer, true);
                holder.setVisible(R.id.progressContainer, false);

                String date = "";
                if (repo.getLastCommitDate() != null) {
                    date = new SimpleDateFormat("MM/dd/yyyy", Locale.getDefault())
                            .format(repo.getLastCommitDate());
                }
                holder.setText(R.id.commitTime, date);
                holder.setText(R.id.commitMsg, repo.getLastCommitMsg());
                holder.setText(R.id.commitAuthor, repo.getLastCommitter());
                holder.setVisible(R.id.authorIcon, true);
                setAvatarImage(holder.getView(R.id.authorIcon), repo.getLastCommitterEmail());
            }
        }

    }

    public void setAvatarImage(ImageView imageView, String email) {
        String avatarUri = "";
        if (!email.isEmpty()) {
            avatarUri = AVATAR_SCHEME + BasicFunctions.md5(email);
        }
        IconLoader.loadIcon(_mActivity, imageView, avatarUri);
    }

    /**
     * Checks if the use of Gravatar is enabled in the preferences.
     *
     * @return true if the use of Gravatar to retrieve Avatar images is enabled, false otherwise
     */
    protected boolean isGravatarEnabled() {
        SharedPreferences sharedPreference = PreferenceHelper.getInstance().getSharedPrefs();
        return sharedPreference.getBoolean(_mActivity.getString(R.string.git_pref_key_use_gravatar), true);
    }

}
