package com.timecat.module.git.sgit.adapters;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.net.Uri;
import android.os.Parcelable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.timecat.element.alert.ToastUtil;
import com.timecat.module.git.R;
import com.timecat.module.git.sgit.activities.SheimiFragmentActivity;
import com.timecat.module.git.utils.BasicFunctions;
import com.timecat.module.git.repolist.RepoListActivity;
import com.timecat.module.git.sgit.activities.RepoDetailActivity;
import com.timecat.module.git.sgit.database.RepoContract;
import com.timecat.module.git.sgit.database.RepoDbManager;
import com.timecat.module.git.sgit.database.RepoDbManager.RepoDbObserver;
import com.timecat.module.git.sgit.database.models.Repo;

import org.jetbrains.annotations.NotNull;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Created by sheimi on 8/6/13.
 */
public class RepoListAdapter extends ArrayAdapter<Repo> implements
        RepoDbObserver, AdapterView.OnItemClickListener,
        AdapterView.OnItemLongClickListener {

    private static final int QUERY_TYPE_SEARCH = 0;
    private static final int QUERY_TYPE_QUERY = 1;

    private int mQueryType = QUERY_TYPE_QUERY;
    private String mSearchQueryString;
    private RepoListActivity mActivity;
    private static final String TAG = RepoListAdapter.class.getSimpleName();

    public RepoListAdapter(Context context) {
        super(context, 0);
        RepoDbManager.registerDbObserver(RepoContract.RepoEntry.TABLE_NAME, this);
        mActivity = (RepoListActivity) context;
    }

    public void searchRepo(String query) {
        mQueryType = QUERY_TYPE_SEARCH;
        mSearchQueryString = query;
        requery();
    }

    public void queryAllRepo() {
        mQueryType = QUERY_TYPE_QUERY;
        requery();
    }

    private void requery() {
        Cursor cursor = null;
        switch (mQueryType) {
            case QUERY_TYPE_SEARCH:
                cursor = RepoDbManager.searchRepo(mSearchQueryString);
                break;
            case QUERY_TYPE_QUERY:
                cursor = RepoDbManager.queryAllRepo();
                break;
        }
        List<Repo> repo = Repo.getRepoList(mActivity, cursor);
        Collections.sort(repo);
        cursor.close();
        clear();
        addAll(repo);
        notifyDataSetChanged();
    }

    @NotNull
    @Override
    public View getView(int position, View convertView, @NotNull ViewGroup parent) {
        if (convertView == null) {
            convertView = newView(getContext(), parent);
        }
        bindView(convertView, position);
        return convertView;
    }

    public View newView(Context context, ViewGroup parent) {
        LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(R.layout.git_repo_listitem, parent, false);
        RepoListItemHolder holder = new RepoListItemHolder();
        holder.repoTitle = (TextView) view.findViewById(R.id.repoTitle);
        holder.repoRemote = (TextView) view.findViewById(R.id.repoRemote);
        holder.commitAuthor = (TextView) view.findViewById(R.id.commitAuthor);
        holder.commitMsg = (TextView) view.findViewById(R.id.commitMsg);
        holder.commitTime = (TextView) view.findViewById(R.id.commitTime);
        holder.authorIcon = (ImageView) view.findViewById(R.id.authorIcon);
        holder.progressContainer = view.findViewById(R.id.progressContainer);
        holder.commitMsgContainer = view.findViewById(R.id.commitMsgContainer);
        holder.progressMsg = (TextView) view.findViewById(R.id.progressMsg);
        holder.cancelBtn = (ImageView) view.findViewById(R.id.cancelBtn);
        view.setTag(holder);
        return view;
    }

    public void bindView(View view, int position) {
        RepoListItemHolder holder = (RepoListItemHolder) view.getTag();
        final Repo repo = getItem(position);

        holder.repoTitle.setText(repo.getDiaplayName());
        holder.repoRemote.setText(repo.getRemoteURL());

        if (!repo.getRepoStatus().equals(RepoContract.REPO_STATUS_NULL)) {
            holder.commitMsgContainer.setVisibility(View.GONE);
            holder.progressContainer.setVisibility(View.VISIBLE);
            holder.progressMsg.setText(repo.getRepoStatus());
            holder.cancelBtn.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    repo.deleteRepo();
                    repo.cancelTask();
                }
            });
        } else if (repo.getLastCommitter() != null) {
            holder.commitMsgContainer.setVisibility(View.VISIBLE);
            holder.progressContainer.setVisibility(View.GONE);

            String date = "";
            if (repo.getLastCommitDate() != null) {
                date = new SimpleDateFormat("MM/dd/yyyy", Locale.getDefault())
                        .format(repo.getLastCommitDate());
            }
            holder.commitTime.setText(date);
            holder.commitMsg.setText(repo.getLastCommitMsg());
            holder.commitAuthor.setText(repo.getLastCommitter());
            holder.authorIcon.setVisibility(View.VISIBLE);
            BasicFunctions.setAvatarImage(holder.authorIcon, repo.getLastCommitterEmail());
        }
    }

    @Override
    public void nofityChanged() {
        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                requery();
            }
        });
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
        Repo repo = getItem(position);
        Intent intent = new Intent(mActivity, RepoDetailActivity.class);
        intent.putExtra(Repo.TAG, repo);
        mActivity.startActivity(intent);
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> adapterView, View view, int position, long id) {
        final Repo repo = getItem(position);
        if (!repo.getRepoStatus().equals(RepoContract.REPO_STATUS_NULL))
            return false;
        Context context = getContext();
        if (context instanceof SheimiFragmentActivity) {
            showRepoOptionsDialog((SheimiFragmentActivity) context, repo);
        }
        return true;
    }

    private void showRepoOptionsDialog(final SheimiFragmentActivity context, final Repo repo) {

        SheimiFragmentActivity.onOptionDialogClicked[] dialog = new SheimiFragmentActivity.onOptionDialogClicked[]{
                new SheimiFragmentActivity.onOptionDialogClicked() {
                    @Override
                    public void onClicked() {
                        showRenameRepoDialog(context, repo);
                    }
                },
                new SheimiFragmentActivity.onOptionDialogClicked() {
                    @Override
                    public void onClicked() {
                        showRemoveRepoDialog(context, repo);
                    }
                },
                null
        };

        final String remoteRaw = repo.getRemoteURL().toLowerCase();

        boolean repoHasHttpRemote = (remoteRaw != null && !remoteRaw.equals("local repository") && remoteRaw.contains("http"));

        if (repoHasHttpRemote) {
            //TODO : Transform ssh uri in http?
            dialog[2] = new SheimiFragmentActivity.onOptionDialogClicked() {
                @Override
                public void onClicked() {

                    //remove git extension if present
                    String repoUrl =
                            remoteRaw.endsWith(context.getString(R.string.git_git_extension)) ?
                                    remoteRaw.substring(0, remoteRaw.lastIndexOf('.')) :
                                    remoteRaw;

                    Intent openUrlIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(repoUrl));

                    //get activities that open this url
                    List<ResolveInfo> activitiesToOpenUrlIntentList = context.getPackageManager().queryIntentActivities(openUrlIntent, 0);

                    List<Intent> intentList = new ArrayList<Intent>();

                    //Get application info to exclude it from the intent chooser
                    ApplicationInfo applicationInfo = context.getApplicationInfo();
                    int stringId = applicationInfo.labelRes;
                    String applicationName = (stringId == 0) ? applicationInfo.nonLocalizedLabel.toString().toLowerCase() : context.getString(stringId).toLowerCase();

                    if (!activitiesToOpenUrlIntentList.isEmpty()) {
                        for (ResolveInfo resolveInfo : activitiesToOpenUrlIntentList) {
                            String packageName = resolveInfo.activityInfo.packageName;
                            //create and add intent from other applications
                            //this way MGit doesn't show up to open the remote url
                            if (!packageName.toLowerCase().contains(applicationName)) {
                                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(repoUrl));
                                intent.setComponent(new ComponentName(packageName, resolveInfo.activityInfo.name));
                                intent.setAction(Intent.ACTION_VIEW);
                                intent.setPackage(packageName);
                                intentList.add(intent);
                            }
                        }
                        if (!intentList.isEmpty()) {
                            String title = String.format(context.getString(R.string.git_dialog_open_remote_title), repo.getDiaplayName());
                            Intent chooserIntent = Intent.createChooser(intentList.remove(0), title);
                            chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, intentList.toArray(new Parcelable[intentList.size()]));
                            context.startActivity(chooserIntent);
                        } else {
                            Log.i(TAG, context.getString(R.string.git_dialog_open_remote_no_app_available));
                            Toast.makeText(context, R.string.git_dialog_open_remote_no_app_available, Toast.LENGTH_LONG).show();
                        }
                    }
                }
            };
        }

        if (repoHasHttpRemote) {
            List<String> stringList = new ArrayList<>(3);
            stringList.addAll(Arrays.asList(context.getResources().getStringArray(R.array.git_dialog_choose_repo_action_items)));
            stringList.add(context.getString(R.string.git_dialog_open_remote));

            context.showOptionsDialog(
                    R.string.git_dialog_choose_option,
                    stringList,
                    dialog
            );
        } else {
            context.showOptionsDialog(
                    R.string.git_dialog_choose_option,
                    R.array.git_dialog_choose_repo_action_items,
                    dialog
            );
        }
    }

    private void showRemoveRepoDialog(SheimiFragmentActivity context, final Repo repo) {
        context.showMessageDialog(
                R.string.git_dialog_delete_repo_title,
                R.string.git_dialog_delete_repo_msg,
                R.string.git_label_delete,
                () -> {
                    repo.deleteRepo();
                    repo.cancelTask();
                }
        );
    }

    private void showRenameRepoDialog(final SheimiFragmentActivity context, final Repo repo) {
        context.showEditTextDialog(
                R.string.git_dialog_rename_repo_title,
                R.string.git_dialog_rename_repo_hint,
                R.string.git_label_rename,
                newRepoName -> {
                    if (!repo.renameRepo(newRepoName)) {
                        ToastUtil.e_long(R.string.git_error_rename_repo_fail);
                    }
                }
        );
    }

    private class RepoListItemHolder {
        public TextView repoTitle;
        public TextView repoRemote;
        public TextView commitAuthor;
        public TextView commitMsg;
        public TextView commitTime;
        public ImageView authorIcon;
        public View progressContainer;
        public View commitMsgContainer;
        public TextView progressMsg;
        public ImageView cancelBtn;
    }

}
