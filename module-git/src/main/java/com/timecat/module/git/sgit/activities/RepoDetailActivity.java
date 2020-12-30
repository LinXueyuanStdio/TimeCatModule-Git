package com.timecat.module.git.sgit.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.xiaojinzi.component.anno.RouterAnno;
import com.jess.arms.utils.LogUtils;
import com.timecat.element.alert.ToastUtil;
import com.timecat.page.base.utils.MenuTintUtils;
import com.timecat.page.base.view.BlurringToolbar;
import com.timecat.identity.readonly.RouterHub;
import com.timecat.component.identity.Attr;
import com.timecat.module.git.R;
import com.timecat.module.git.android.activities.SheimiFragmentActivity;
import com.timecat.module.git.sgit.activities.delegate.RepoOperationDelegate;
import com.timecat.module.git.sgit.database.models.Repo;
import com.timecat.module.git.sgit.fragments.BaseFragment;
import com.timecat.module.git.sgit.fragments.CommitsFragment;
import com.timecat.module.git.sgit.fragments.FilesFragment;
import com.timecat.module.git.sgit.fragments.StatusFragment;
import com.timecat.module.git.sgit.repo.tasks.SheimiAsyncTask.AsyncTaskCallback;

@RouterAnno(hostAndPath = RouterHub.GIT_RepoDetailActivity)
public class RepoDetailActivity extends SheimiFragmentActivity {

    private ActionBar mActionBar;

    private FilesFragment mFilesFragment;
    private CommitsFragment mCommitsFragment;
    private StatusFragment mStatusFragment;

    private TabItemPagerAdapter mTabItemPagerAdapter;
    private ViewPager mViewPager;
    private Button mCommitNameButton;
    private ImageView mCommitType;
    private MenuItem mSearchItem;

    private Repo mRepo;

    private View mPullProgressContainer;
    private ProgressBar mPullProgressBar;
    private TextView mPullMsg;
    private TextView mPullLeftHint;
    private TextView mPullRightHint;

    private RepoOperationDelegate mRepoDelegate;

    private static final int FILES_FRAGMENT_INDEX = 0;
    private static final int COMMITS_FRAGMENT_INDEX = 1;
    private static final int STATUS_FRAGMENT_INDEX = 2;
    private static final int BRANCH_CHOOSE_ACTIVITY = 0;
    private int mSelectedTab;

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case BRANCH_CHOOSE_ACTIVITY:
                String branchName = mRepo.getBranchName();
                if (branchName == null) {
                    ToastUtil.e_long(R.string.git_error_something_wrong);
                    return;
                }
                reset(branchName);
                break;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mRepo = (Repo) getIntent().getSerializableExtra(Repo.TAG);
        // aweful hack! workaround for null repo when returning from BranchChooser, but going to
        // shortly refactor passing in serialised repo, so not worth doing more to fix for now
        if (mRepo == null) {
            finish();
            return;
        }
        repoInit();
        setTitle(mRepo.getDiaplayName());
        setContentView(R.layout.git_activity_repo_detail_content);
        setupActionBar();
        createFragments();
        setupViewPager();
        setupPullProgressView();
        setupDrawer();
        mCommitNameButton = (Button) findViewById(R.id.commitName);
        mCommitType = (ImageView) findViewById(R.id.commitType);
        mCommitNameButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(RepoDetailActivity.this, BranchChooserActivity.class);
                intent.putExtra(Repo.TAG, mRepo);
                startActivityForResult(intent, BRANCH_CHOOSE_ACTIVITY);
            }
        });
        String branchName = mRepo.getBranchName();
        if (branchName == null) {
            ToastUtil.e_long(R.string.git_error_something_wrong);
            return;
        }
        resetCommitButtonName(branchName);
    }

    public RepoOperationDelegate getRepoDelegate() {
        if (mRepoDelegate == null) {
            mRepoDelegate = new RepoOperationDelegate(mRepo, this);
        }
        return mRepoDelegate;
    }

    private void setupViewPager() {
        mViewPager = (ViewPager) findViewById(R.id.pager);
        mTabItemPagerAdapter = new TabItemPagerAdapter(getSupportFragmentManager());
        mViewPager.setAdapter(mTabItemPagerAdapter);
        mViewPager.addOnPageChangeListener(mTabItemPagerAdapter);
    }

    private void setupDrawer() {
    }

    private void setupPullProgressView() {
        mPullProgressContainer = findViewById(R.id.pullProgressContainer);
        mPullProgressContainer.setVisibility(View.GONE);
        mPullProgressBar = (ProgressBar) mPullProgressContainer
                .findViewById(R.id.pullProgress);
        mPullMsg = (TextView) mPullProgressContainer.findViewById(R.id.pullMsg);
        mPullLeftHint = (TextView) mPullProgressContainer
                .findViewById(R.id.leftHint);
        mPullRightHint = (TextView) mPullProgressContainer
                .findViewById(R.id.rightHint);
    }

    private void setupActionBar() {
        BlurringToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setPaddingStatusBar(this);
        setSupportActionBar(toolbar);
        mActionBar = getSupportActionBar();
        if (mActionBar != null) {
            mActionBar.setDisplayShowTitleEnabled(true);
            mActionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    private void createFragments() {
        mFilesFragment = FilesFragment.newInstance(mRepo);
        mCommitsFragment = CommitsFragment.newInstance(mRepo, null);
        mStatusFragment = StatusFragment.newInstance(mRepo);
    }

    private void resetCommitButtonName(String commitName) {
        int commitType = Repo.getCommitType(commitName);
        switch (commitType) {
            case Repo.COMMIT_TYPE_REMOTE:
                // change the display name to local branch
                commitName = Repo.convertRemoteName(commitName);
            case Repo.COMMIT_TYPE_HEAD:
                mCommitType.setVisibility(View.VISIBLE);
                mCommitType.setImageResource(R.drawable.ic_branch_w);
                break;
            case Repo.COMMIT_TYPE_TAG:
                mCommitType.setVisibility(View.VISIBLE);
                mCommitType.setImageResource(R.drawable.ic_tag_w);
                break;
            case Repo.COMMIT_TYPE_TEMP:
                mCommitType.setVisibility(View.GONE);
                break;
        }
        String displayName = Repo.getCommitDisplayName(commitName);
        mCommitNameButton.setText(displayName);
    }

    public void reset(String commitName) {
        resetCommitButtonName(commitName);
        reset();
    }

    public void reset() {
        mFilesFragment.reset();
        mCommitsFragment.reset();
        mStatusFragment.reset();
    }

    public void setFilesFragment(FilesFragment filesFragment) {
        mFilesFragment = filesFragment;
    }

    public FilesFragment getFilesFragment() {
        return mFilesFragment;
    }

    public void setCommitsFragment(CommitsFragment commitsFragment) {
        mCommitsFragment = commitsFragment;
    }

    public void setStatusFragment(StatusFragment statusFragment) {
        mStatusFragment = statusFragment;
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.git_repo_detail, menu);
        mSearchItem = menu.findItem(R.id.action_search);
        mSearchItem.setOnActionExpandListener(mTabItemPagerAdapter);
        mSearchItem.setVisible(mSelectedTab == COMMITS_FRAGMENT_INDEX);
        SearchView searchView = (SearchView) mSearchItem.getActionView();
        if (searchView != null) {
            searchView.setIconifiedByDefault(true);
            searchView.setOnQueryTextListener(mTabItemPagerAdapter);
        }
        MenuTintUtils.tintAllIcons(menu, Attr.getIconColor(this));
        return true;
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_BACK:
            case KeyEvent.KEYCODE_DEL:
                int position = mViewPager.getCurrentItem();
                OnBackClickListener onBackClickListener = mTabItemPagerAdapter
                        .getItem(position).getOnBackClickListener();
                if (onBackClickListener != null) {
                    if (onBackClickListener.onClick()) {
                        return true;
                    }
                }
                finish();
                return true;
            case KeyEvent.KEYCODE_F:
                mViewPager.setCurrentItem(FILES_FRAGMENT_INDEX);
                return true;
            case KeyEvent.KEYCODE_C:
                mViewPager.setCurrentItem(COMMITS_FRAGMENT_INDEX);
                return true;
            case KeyEvent.KEYCODE_S:
                mViewPager.setCurrentItem(STATUS_FRAGMENT_INDEX);
                return true;
            case KeyEvent.KEYCODE_SLASH:
                if (event.isShiftPressed()) {
                    showKeyboardShortcutsHelpOverlay();
                }
                return true;
            default:
                return super.onKeyUp(keyCode, event);
        }
    }

    private void showKeyboardShortcutsHelpOverlay() {
        showMessageDialog(R.string.git_dialog_keymap_title, getString(R.string.git_dialog_keymap_mesg));
    }

    public void error() {
        finish();
        ToastUtil.e_long(R.string.git_error_unknown);
    }

    public class ProgressCallback implements AsyncTaskCallback {

        private int mInitMsg;

        public ProgressCallback(int initMsg) {
            mInitMsg = initMsg;
        }

        @Override
        public void onPreExecute() {
            mPullMsg.setText(mInitMsg);
            Animation anim = AnimationUtils.loadAnimation(
                    RepoDetailActivity.this, R.anim.fade_in);
            mPullProgressContainer.setAnimation(anim);
            mPullProgressContainer.setVisibility(View.VISIBLE);
            mPullLeftHint.setText(R.string.git_progress_left_init);
            mPullRightHint.setText(R.string.git_progress_right_init);
        }

        @Override
        public void onProgressUpdate(String... progress) {
            mPullMsg.setText(progress[0]);
            mPullLeftHint.setText(progress[1]);
            mPullRightHint.setText(progress[2]);
            mPullProgressBar.setProgress(Integer.parseInt(progress[3]));
        }

        @Override
        public void onPostExecute(Boolean isSuccess) {
            Animation anim = AnimationUtils.loadAnimation(
                    RepoDetailActivity.this, R.anim.fade_out);
            mPullProgressContainer.setAnimation(anim);
            mPullProgressContainer.setVisibility(View.GONE);
            reset();
        }

        @Override
        public boolean doInBackground(Void... params) {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                return false;
            }
            return true;
        }

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int i = item.getItemId();
        LogUtils.warnInfo("click");
        if (i == android.R.id.home) {
            finish();
            return true;

        } else if (i == R.id.newBranchName) {
            getRepoDelegate().executeAction(0);
            return true;
        } else if (i == R.id.pull) {
            getRepoDelegate().executeAction(1);
            return true;
        } else if (i == R.id.push) {
            getRepoDelegate().executeAction(2);
            return true;
        } else if (i == R.id.addAllToStage) {
            getRepoDelegate().executeAction(3);
            return true;
        } else if (i == R.id.commit) {
            getRepoDelegate().executeAction(4);
            return true;
        } else if (i == R.id.reset) {
            getRepoDelegate().executeAction(5);
            return true;
        } else if (i == R.id.merge) {
            getRepoDelegate().executeAction(6);
            return true;
        } else if (i == R.id.fetch) {
            getRepoDelegate().executeAction(7);
            return true;
        } else if (i == R.id.rebase) {
            getRepoDelegate().executeAction(8);
            return true;
        } else if (i == R.id.cherryPick) {
            getRepoDelegate().executeAction(9);
            return true;
        } else if (i == R.id.diff) {
            getRepoDelegate().executeAction(10);
            return true;
        } else if (i == R.id.newFile) {
            getRepoDelegate().executeAction(11);
            return true;
        } else if (i == R.id.newDir) {
            getRepoDelegate().executeAction(12);
            return true;
        } else if (i == R.id.addRemote) {
            getRepoDelegate().executeAction(13);
            return true;
        } else if (i == R.id.removeRemote) {
            getRepoDelegate().executeAction(14);
            return true;
        } else if (i == R.id.delete) {
            getRepoDelegate().executeAction(15);
            return true;
        } else if (i == R.id.rawConfig) {
            getRepoDelegate().executeAction(16);
            return true;
        } else if (i == R.id.config) {
            getRepoDelegate().executeAction(17);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void closeOperationDrawer() {
    }

    public void enterDiffActionMode() {
        mViewPager.setCurrentItem(COMMITS_FRAGMENT_INDEX);
        mCommitsFragment.enterDiffActionMode();
    }

    private void repoInit() {
        mRepo.updateLatestCommitInfo();
        mRepo.getRemotes();
    }

    class TabItemPagerAdapter extends FragmentPagerAdapter implements ViewPager.OnPageChangeListener, SearchView.OnQueryTextListener, MenuItem.OnActionExpandListener {

        private final int[] PAGE_TITLE = {R.string.git_tab_files_label,
                R.string.git_tab_commits_label, R.string.git_tab_status_label};

        public TabItemPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public BaseFragment getItem(int position) {
            switch (position) {
                case FILES_FRAGMENT_INDEX:
                    return mFilesFragment;
                case COMMITS_FRAGMENT_INDEX:
                    return mCommitsFragment;
                case STATUS_FRAGMENT_INDEX:
                    mStatusFragment.reset();
                    return mStatusFragment;
            }
            return mFilesFragment;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return getString(PAGE_TITLE[position]);
        }

        @Override
        public int getCount() {
            return PAGE_TITLE.length;
        }

        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

        }

        @Override
        public void onPageSelected(int position) {
            mSelectedTab = position;
            if (mSearchItem != null) {
                mSearchItem.setVisible(position == COMMITS_FRAGMENT_INDEX);
            }
        }

        @Override
        public void onPageScrollStateChanged(int state) {

        }

        @Override
        public boolean onQueryTextSubmit(String query) {
            switch (mViewPager.getCurrentItem()) {
                case COMMITS_FRAGMENT_INDEX:
                    mCommitsFragment.setFilter(query);
                    break;
            }
            return true;
        }

        @Override
        public boolean onQueryTextChange(String query) {
            switch (mViewPager.getCurrentItem()) {
                case COMMITS_FRAGMENT_INDEX:
                    mCommitsFragment.setFilter(query);
                    break;
            }
            return true;
        }

        @Override
        public boolean onMenuItemActionExpand(MenuItem menuItem) {
            return true;
        }

        @Override
        public boolean onMenuItemActionCollapse(MenuItem menuItem) {
            switch (mViewPager.getCurrentItem()) {
                case COMMITS_FRAGMENT_INDEX:
                    mCommitsFragment.setFilter(null);
                    break;
            }
            return true;
        }

    }

}
