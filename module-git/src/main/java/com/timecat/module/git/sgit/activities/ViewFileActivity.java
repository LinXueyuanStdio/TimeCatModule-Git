package com.timecat.module.git.sgit.activities;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import androidx.appcompat.widget.SearchView;
import androidx.core.view.MenuItemCompat;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.PagerTitleStrip;
import androidx.viewpager.widget.ViewPager;

import com.timecat.page.base.utils.MenuTintUtils;
import com.timecat.page.base.view.BlurringToolbar;
import com.timecat.component.identity.Attr;
import com.timecat.module.git.R;
import com.timecat.module.git.utils.FsUtils;
import com.timecat.module.git.sgit.database.models.Repo;
import com.timecat.module.git.sgit.dialogs.ChooseLanguageDialog;
import com.timecat.module.git.sgit.fragments.BaseFragment;
import com.timecat.module.git.sgit.fragments.CommitsFragment;
import com.timecat.module.git.sgit.fragments.ViewFileFragment;

import org.jetbrains.annotations.NotNull;

import java.io.File;

public class ViewFileActivity extends SheimiFragmentActivity {

    public static String TAG_FILE_NAME = "file_name";
    public static String TAG_MODE = "mode";
    public static short TAG_MODE_NORMAL = 0;
    public static short TAG_MODE_SSH_KEY = 1;
    private CommitsFragment mCommitsFragment;
    private short mActivityMode = TAG_MODE_NORMAL;
    private static final int FILE_FRAGMENT_INDEX = 0;
    private static final int COMMITS_FRAGMENT_INDEX = 1;
    private ViewPager mViewPager;
    private Repo mRepo;
    private TabItemPagerAdapter mTabItemPagerAdapter;
    private ViewFileFragment mFileFragment;
    private int mCurrentTab;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.git_activity_view_file);
        BlurringToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setPaddingStatusBar(this);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        mRepo = (Repo) getIntent().getSerializableExtra(Repo.TAG);
        mViewPager = (ViewPager) findViewById(R.id.pager);
        mTabItemPagerAdapter = new TabItemPagerAdapter(getSupportFragmentManager());
        mViewPager.setAdapter(mTabItemPagerAdapter);
        mViewPager.setOnPageChangeListener(mTabItemPagerAdapter);
        Bundle b = new Bundle();
        Bundle extras = getIntent().getExtras();
        String fileName = extras.getString(TAG_FILE_NAME);
        mActivityMode = extras.getShort(TAG_MODE, TAG_MODE_NORMAL);
        b.putString(TAG_FILE_NAME, fileName);
        if (mRepo != null) {
            b.putSerializable(Repo.TAG, mRepo);
            mCommitsFragment = CommitsFragment.newInstance(mRepo, FsUtils.getRelativePath(new File(fileName), mRepo.getDir()));
        }
        if (mRepo == null) {
            PagerTitleStrip strip = (PagerTitleStrip) findViewById(R.id.pager_title_strip);
            strip.setVisibility(View.GONE);
        }
        mFileFragment = new ViewFileFragment();
        mFileFragment.setArguments(b);
        mActivityMode = extras.getShort(TAG_MODE, TAG_MODE_NORMAL);
        b.putShort(TAG_MODE, mActivityMode);
        setTitle(new File(fileName).getName());
    }

    class TabItemPagerAdapter extends FragmentPagerAdapter implements
            ViewPager.OnPageChangeListener,
            SearchView.OnQueryTextListener,
            MenuItemCompat.OnActionExpandListener {

        private final int[] PAGE_TITLE = {R.string.git_tab_file_label, R.string.git_tab_commits_label};

        public TabItemPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @NotNull
        @Override
        public BaseFragment getItem(int position) {
            switch (position) {
                case FILE_FRAGMENT_INDEX:
                    return mFileFragment;
                case COMMITS_FRAGMENT_INDEX:
                    return mCommitsFragment;
            }
            return mFileFragment;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return getString(PAGE_TITLE[position]);
        }

        @Override
        public int getCount() {
            if (mRepo == null) {
                return 1;
            }
            return PAGE_TITLE.length;
        }

        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

        }

        @Override
        public void onPageSelected(int position) {
            mCurrentTab = position;
            invalidateOptionsMenu();
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

    private void setSaveStatus(MenuItem mi) {
        if (mFileFragment.getEditMode()) {
            mi.setIcon(R.drawable.ic_save_white_24dp);
            mi.setTitle(R.string.git_action_edit_save);
        } else {
            mi.setIcon(R.drawable.ic_edit_white_24dp);
            mi.setTitle(R.string.git_action_edit);
        }
        MenuTintUtils.tintMenuItemIcon(mi, Attr.getIconColor(this));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.git_view_file, menu);
        if (mActivityMode == TAG_MODE_SSH_KEY) {
            menu.removeItem(R.id.action_edit);
            menu.removeItem(R.id.action_edit_in_other_app);
            menu.removeItem(R.id.action_choose_language);
        } else {
            menu.removeItem(R.id.action_copy_all);
        }
        if (mActivityMode != TAG_MODE_SSH_KEY) {
            MenuItem mi = menu.findItem(R.id.action_edit);
            setSaveStatus(mi);
            mi.setVisible(mCurrentTab == FILE_FRAGMENT_INDEX);
            mi = menu.findItem(R.id.action_edit_in_other_app);
            mi.setVisible(mCurrentTab == FILE_FRAGMENT_INDEX);
            mi = menu.findItem(R.id.action_choose_language);
            mi.setVisible(mCurrentTab == FILE_FRAGMENT_INDEX);
        }
        if (mRepo != null) {
            MenuItem searchItem = menu.findItem(R.id.action_search);
            MenuItemCompat.setOnActionExpandListener(searchItem, mTabItemPagerAdapter);
            SearchView searchView = (SearchView) searchItem.getActionView();
            if (searchView != null) {
                searchView.setIconifiedByDefault(true);
                searchView.setOnQueryTextListener(mTabItemPagerAdapter);
            }
            searchItem.setVisible(mCurrentTab == COMMITS_FRAGMENT_INDEX);
        } else {
            menu.removeItem(R.id.action_search);
        }
        MenuTintUtils.tintAllIcons(menu, Attr.getIconColor(this));
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int i = item.getItemId();
        if (i == android.R.id.home) {
            finish();
            return true;
        } else if (i == R.id.action_edit_in_other_app) {
            if (mActivityMode == TAG_MODE_SSH_KEY) {
                return true;
            }
            FsUtils.openFile(this, mFileFragment.getFile());

        } else if (i == R.id.action_edit) {
            if (mActivityMode == TAG_MODE_SSH_KEY) {
                return true;
            }
            mFileFragment.setEditMode(!mFileFragment.getEditMode());
            setSaveStatus(item);
            return true;
        } else if (i == R.id.action_choose_language) {
            if (mActivityMode == TAG_MODE_SSH_KEY) {
                return true;
            }
            ChooseLanguageDialog cld = new ChooseLanguageDialog();
            cld.show(getSupportFragmentManager(), "choose language");
            return true;
        } else if (i == R.id.action_copy_all) {
            mFileFragment.copyAll();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void setLanguage(String lang) {
        mFileFragment.setLanguage(lang);
    }
}
