package com.timecat.module.git.sgit.database.models

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.os.Bundle
import android.util.SparseArray
import com.timecat.component.commonsdk.utils.override.LogUtil
import com.timecat.module.git.sgit.database.RepoContract
import com.timecat.module.git.sgit.database.RepoDbManager
import com.timecat.module.git.tasks.RepoOpTask
import com.timecat.module.git.utils.FsUtils
import com.timecat.module.git.utils.PreferenceHelper
import com.timecat.module.git.utils.StopTaskException
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.ListBranchCommand
import org.eclipse.jgit.api.errors.GitAPIException
import org.eclipse.jgit.lib.Constants
import org.eclipse.jgit.lib.Ref
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.lib.StoredConfig
import org.eclipse.jgit.revwalk.RevCommit
import org.eclipse.jgit.revwalk.RevWalk
import java.io.*
import java.util.*

/**
 * Model for a local repo
 */
class Repo(cursor: Cursor) : Comparable<Repo>, Serializable {
    var id: Int
        private set
    var localPath: String
        private set
    var remoteURL: String
        private set
    var username: String?
    var password: String?
    var repoStatus: String
        private set
    var lastCommitter: String
        private set
    var lastCommitterEmail: String
        private set
    var lastCommitDate: Date
        private set
    var lastCommitMsg: String
        private set
    private var isDeleted = false

    // lazy load
    private var mRemotes: MutableSet<String>? = null
    private var mGit: Git? = null
    private var mStoredConfig: StoredConfig? = null

    init {
        id = RepoContract.getRepoID(cursor)
        remoteURL = RepoContract.getRemoteURL(cursor)
        localPath = RepoContract.getLocalPath(cursor)
        username = RepoContract.getUsername(cursor)
        password = RepoContract.getPassword(cursor)
        repoStatus = RepoContract.getRepoStatus(cursor)
        lastCommitter = RepoContract.getLatestCommitterName(cursor)
        lastCommitterEmail = RepoContract.getLatestCommitterEmail(cursor)
        lastCommitDate = RepoContract.getLatestCommitDate(cursor)
        lastCommitMsg = RepoContract.getLatestCommitMsg(cursor)
    }

    val bundle: Bundle
        get() {
            val bundle = Bundle()
            bundle.putSerializable(TAG, this)
            return bundle
        }
    val diaplayName: String
        get() {
            if (!isExternal) return localPath
            val strs = localPath.split("/").toTypedArray()
            return strs[strs.size - 1] + " (external)"
        }
    val isExternal: Boolean
        get() = isExternal(localPath)
    val lastCommitFullMsg: String
        get() {
            val commit = getLatestCommit() ?: return lastCommitMsg
            return commit.fullMessage
        }

    fun cancelTask() {
        val task = mRepoTasks[id] ?: return
        task.cancelTask()
        removeTask(task)
    }

    fun addTask(task: RepoOpTask?): Boolean {
        if (mRepoTasks[id] != null) return false
        mRepoTasks.put(id, task)
        return true
    }

    fun removeTask(task: RepoOpTask) {
        val runningTask = mRepoTasks[id]
        if (runningTask == null || runningTask !== task) return
        mRepoTasks.remove(id)
    }

    fun updateStatus(status: String) {
        val values = ContentValues()
        repoStatus = status
        values.put(RepoContract.RepoEntry.COLUMN_NAME_REPO_STATUS, status)
        RepoDbManager.updateRepo(id.toLong(), values)
    }

    fun updateRemote() {
        val values = ContentValues()
        values.put(
            RepoContract.RepoEntry.COLUMN_NAME_REMOTE_URL,
            getRemoteOriginURL()
        )
        RepoDbManager.updateRepo(id.toLong(), values)
    }

    @Throws(IOException::class)
    private fun writeObject(out: ObjectOutputStream) {
        out.writeInt(id)
        out.writeObject(remoteURL)
        out.writeObject(localPath)
        out.writeObject(username)
        out.writeObject(password)
        out.writeObject(repoStatus)
        out.writeObject(lastCommitter)
        out.writeObject(lastCommitterEmail)
        out.writeObject(lastCommitDate)
        out.writeObject(lastCommitMsg)
    }

    @Throws(IOException::class, ClassNotFoundException::class)
    private fun readObject(stream: ObjectInputStream) {
        id = stream.readInt()
        remoteURL = stream.readObject() as String
        localPath = stream.readObject() as String
        username = stream.readObject() as String?
        password = stream.readObject() as String?
        repoStatus = stream.readObject() as String
        lastCommitter = stream.readObject() as String
        lastCommitterEmail = stream.readObject() as String
        lastCommitDate = stream.readObject() as Date
        lastCommitMsg = stream.readObject() as String
    }

    override fun compareTo(other: Repo): Int {
        return other.id - id
    }

    fun deleteRepo() {
        val thread = Thread { deleteRepoSync() }
        thread.start()
    }

    fun deleteRepoSync() {
        if (isDeleted) return
        RepoDbManager.deleteRepo(id.toLong())
        if (!isExternal) {
            val fileToDelete = dir
            FsUtils.deleteFile(fileToDelete)
        }
        isDeleted = true
    }

    fun renameRepo(repoName: String): Boolean {
        val directory = dir
        if (FsUtils.renameDirectory(directory, repoName)) {
            val values = ContentValues()
            localPath = if (isExternal) EXTERNAL_PREFIX + directory.parent + File.separator + repoName else repoName
            values.put(RepoContract.RepoEntry.COLUMN_NAME_LOCAL_PATH, localPath)
            RepoDbManager.updateRepo(id.toLong(), values)
            return true
        }
        return false
    }

    fun updateLatestCommitInfo() {
        val commit = getLatestCommit()
        val values = ContentValues()
        var email: String? = ""
        var uname: String? = ""
        var commitDateStr: String? = ""
        var msg: String? = ""
        if (commit != null) {
            val committer = commit.committerIdent
            if (committer != null) {
                email = if (committer.emailAddress != null) committer
                    .emailAddress else email
                uname = if (committer.name != null) committer.name else uname
            }
            msg = if (commit.shortMessage != null) commit.shortMessage else msg
            val date = committer!!.getWhen().time
            commitDateStr = date.toString()
        }
        values.put(
            RepoContract.RepoEntry.COLUMN_NAME_LATEST_COMMIT_DATE,
            commitDateStr
        )
        values.put(RepoContract.RepoEntry.COLUMN_NAME_LATEST_COMMIT_MSG, msg)
        values.put(RepoContract.RepoEntry.COLUMN_NAME_LATEST_COMMITTER_EMAIL, email)
        values.put(RepoContract.RepoEntry.COLUMN_NAME_LATEST_COMMITTER_UNAME, uname)
        RepoDbManager.updateRepo(id.toLong(), values)
    }

    fun getBranchName(): String {
        try {
            return getGit()!!.repository.fullBranch
        } catch (e: IOException) {
            LogUtil.e(e, "error getting branch name")
        } catch (e: StopTaskException) {
            LogUtil.e(e, "error getting branch name")
        }
        return ""
    }

    fun getBranches(): Array<String?> {
        try {
            val branchSet: MutableSet<String?> = HashSet()
            val branchList: MutableList<String?> = ArrayList()
            val localRefs = getGit()!!.branchList().call()
            for (ref in localRefs) {
                branchSet.add(ref.name)
                branchList.add(ref.name)
            }
            val remoteRefs = getGit()!!.branchList()
                .setListMode(ListBranchCommand.ListMode.REMOTE).call()
            for (ref in remoteRefs) {
                val name = ref.name
                val localName = convertRemoteName(name)
                if (branchSet.contains(localName)) continue
                branchList.add(name)
            }
            return branchList.toTypedArray()
        } catch (e: GitAPIException) {
            LogUtil.e(e)
        } catch (e: StopTaskException) {
            LogUtil.e(e)
        }
        return arrayOfNulls(0)
    }

    private fun getLatestCommit(): RevCommit? {
        try {
            val commits = getGit()!!.log().setMaxCount(1).call()
            val it: Iterator<RevCommit> = commits.iterator()
            return if (!it.hasNext()) null else it.next()
        } catch (e: GitAPIException) {
            LogUtil.e(e)
        } catch (e: StopTaskException) {
            LogUtil.e(e)
        }
        return null
    }

    private fun getCommitByRevStr(commitRevStr: String): RevCommit? {
        return try {
            val repository = getGit()!!.repository
            val id = repository.resolve(commitRevStr)
            val revWalk = RevWalk(getGit()!!.repository)
            if (id != null) revWalk.parseCommit(id) else null
        } catch (e: StopTaskException) {
            LogUtil.e(e, "error parsing commit id: $commitRevStr")
            null
        } catch (e: IOException) {
            LogUtil.e(e, "error parsing commit id: $commitRevStr")
            null
        }
    }

    fun isInitialCommit(commit: String): Boolean {
        val revCommit = getCommitByRevStr(commit)
        return revCommit != null && revCommit.parentCount == 0
    }

    val localBranches: List<Ref>
        get() {
            try {
                return getGit()!!.branchList().call()
            } catch (e: GitAPIException) {
                LogUtil.e(e)
            } catch (e: StopTaskException) {
                LogUtil.e(e)
            }
            return ArrayList()
        }

    // convert refs/tags/[branch] -> heads/[branch]
    val tags: Array<String?>
        get() {
            try {
                val refs = getGit()!!.tagList().call()
                val tags = arrayOfNulls<String>(refs.size)
                // convert refs/tags/[branch] -> heads/[branch]
                for (i in tags.indices) {
                    tags[i] = refs[i].name
                }
                return tags
            } catch (e: GitAPIException) {
                LogUtil.e(e)
            } catch (e: StopTaskException) {
                LogUtil.e(e)
            }
            return arrayOfNulls(0)
        }
    val currentDisplayName: String
        get() = getCommitDisplayName(getBranchName())

    val dir: File
        get() {
            val prefHelper = PreferenceHelper.getInstance()
            return getDir(prefHelper, localPath)
        }

    @Throws(StopTaskException::class)
    fun getGit(): Git? {
        return if (mGit != null) mGit else try {
            val repoFile = dir
            mGit = Git.open(repoFile)
            mGit
        } catch (e: IOException) {
            LogUtil.e(e)
            throw StopTaskException()
        }
    }

    @Throws(StopTaskException::class)
    fun getStoredConfig(): StoredConfig? {
        if (mStoredConfig == null) {
            mStoredConfig = getGit()?.repository?.config
        }
        return mStoredConfig
    }

    fun getRemoteOriginURL(): String {
        try {
            val config = getStoredConfig()
            val origin = config!!.getString("remote", "origin", "url")
            if (origin != null && !origin.isEmpty()) return origin
            val remoteNames = config.getSubsections("remote")
            return if (remoteNames.size == 0) "" else config.getString(
                "remote", remoteNames.iterator()
                    .next(), "url"
            )
        } catch (e: StopTaskException) {
        }
        return ""
    }

    fun getRemotes(): Set<String> {
        if (mRemotes != null) return mRemotes!!
        try {
            val config = getStoredConfig()
            val remotes = config!!.getSubsections("remote")
            mRemotes = HashSet(remotes)
            return mRemotes!!
        } catch (e: StopTaskException) {
        }
        return HashSet()
    }

    @Throws(IOException::class)
    fun setRemote(remote: String, url: String?) {
        try {
            val config = getStoredConfig()
            val remoteNames = config!!.getSubsections("remote")
            if (remoteNames.contains(remote)) {
                throw IOException(String.format("Remote %s already exists.", remote))
            }
            config.setString("remote", remote, "url", url)
            val fetch = String.format("+refs/heads/*:refs/remotes/%s/*", remote)
            config.setString("remote", remote, "fetch", fetch)
            config.save()
            mRemotes!!.add(remote)
        } catch (e: StopTaskException) {
        }
    }

    @Throws(IOException::class)
    fun removeRemote(remote: String) {
        try {
            val config = getStoredConfig()
            val remoteNames = config!!.getSubsections("remote")
            if (!remoteNames.contains(remote)) {
                throw IOException(String.format("Remote %s does not exist.", remote))
            }
            config.unsetSection("remote", remote)
            config.save()
            mRemotes!!.remove(remote)
        } catch (_: StopTaskException) {
        }
    }

    fun saveCredentials() {
        RepoDbManager.persistCredentials(id.toLong(), username, password)
    }

    companion object {
        /**
         * Generated serialVersionID
         */
        private const val serialVersionUID = -4921633809823078219L

        @JvmField
        val TAG = Repo::class.java.simpleName
        const val COMMIT_TYPE_HEAD = 0
        const val COMMIT_TYPE_TAG = 1
        const val COMMIT_TYPE_TEMP = 2
        const val COMMIT_TYPE_REMOTE = 3
        const val COMMIT_TYPE_UNKNOWN = -1
        const val DOT_GIT_DIR = ".git"
        const val EXTERNAL_PREFIX = "external://"
        const val REPO_DIR = "repo"
        private val mRepoTasks = SparseArray<RepoOpTask>()

        @JvmStatic
        fun createRepo(localPath: String?, remoteURL: String?, status: String?): Repo {
            return getRepoById(RepoDbManager.createRepo(localPath, remoteURL, status))
        }

        @JvmStatic
        fun importRepo(localPath: String?, status: String?): Repo {
            return getRepoById(RepoDbManager.importRepo(localPath, status))
        }

        fun getRepoById(id: Long): Repo {
            val c = RepoDbManager.getRepoById(id)
            c.moveToFirst()
            val repo = Repo(c)
            c.close()
            return repo
        }

        @JvmStatic
        fun getRepoList(context: Context?, cursor: Cursor): MutableList<Repo> {
            val repos: MutableList<Repo> = ArrayList()
            cursor.moveToFirst()
            while (!cursor.isAfterLast) {
                repos.add(Repo(cursor))
                cursor.moveToNext()
            }
            return repos
        }

        fun isExternal(path: String): Boolean {
            return path.startsWith(EXTERNAL_PREFIX)
        }

        fun getCommitType(splits: Array<String>): Int {
            if (splits.size == 4) return COMMIT_TYPE_REMOTE
            if (splits.size != 3) return COMMIT_TYPE_TEMP
            val type = splits[1]
            return if ("tags" == type) COMMIT_TYPE_TAG else COMMIT_TYPE_HEAD
        }

        /**
         * Returns the type of ref based on the refs full path within .git/
         *
         * @param fullRefName
         * @return
         */
        @JvmStatic
        fun getCommitType(fullRefName: String?): Int {
            if (fullRefName != null && fullRefName.startsWith(Constants.R_REFS)) {
                if (fullRefName.startsWith(Constants.R_HEADS)) {
                    return COMMIT_TYPE_HEAD
                } else if (fullRefName.startsWith(Constants.R_TAGS)) {
                    return COMMIT_TYPE_TAG
                } else if (fullRefName.startsWith(Constants.R_REMOTES)) {
                    return COMMIT_TYPE_REMOTE
                }
            }
            return COMMIT_TYPE_UNKNOWN
        }

        /**
         * Return just the name of the ref, with any prefixes like "heads", "remotes", "tags" etc.
         *
         * @param name
         * @return
         */
        fun getCommitName(name: String): String? {
            val splits = name.split("/").toTypedArray()
            val type = getCommitType(splits)
            when (type) {
                COMMIT_TYPE_TEMP, COMMIT_TYPE_TAG, COMMIT_TYPE_HEAD -> return getCommitDisplayName(name)
                COMMIT_TYPE_REMOTE -> return splits[3]
            }
            return null
        }

        /**
         * @param ref
         * @return Shortened version of full ref path, suitable for display in UI
         */
        @JvmStatic
        fun getCommitDisplayName(ref: String?): String {
            return if (getCommitType(ref) == COMMIT_TYPE_REMOTE) {
                if (ref != null && ref.length > Constants.R_REFS.length) ref.substring(Constants.R_REFS.length) else ""
            } else Repository.shortenRefName(ref)
        }

        /**
         * @param remote
         * @return null if remote is not found to be a remote ref in this repo
         */
        @JvmStatic
        fun convertRemoteName(remote: String): String? {
            return if (getCommitType(remote) != COMMIT_TYPE_REMOTE) {
                null
            } else {
                val splits = remote.split("/").toTypedArray()
                String.format("refs/heads/%s", splits[3])
            }
        }

        @JvmStatic
        fun getDir(preferenceHelper: PreferenceHelper, localpath: String): File {
            if (isExternal(localpath)) {
                return File(localpath.substring(EXTERNAL_PREFIX.length))
            }
            var repoDir = preferenceHelper.repoRoot
            return if (repoDir == null) {
                repoDir = FsUtils.getExternalDir(REPO_DIR, true)
                LogUtil.d("PRESET repo path:" + File(repoDir, localpath).absolutePath)
                File(repoDir, localpath)
            } else {
                repoDir = File(repoDir, localpath)
                LogUtil.d("CUSTOM repo path:$repoDir")
                repoDir
            }
        }

        @JvmStatic
        fun setLocalRepoRoot(context: Context?, repoRoot: File) {
            val prefs = PreferenceHelper.getInstance()
            val oldRoot = prefs.repoRoot
            prefs.setRepoRoot(repoRoot.absolutePath)

            // need to make any existing "internal" repos "external" so that their paths are still correct
            val allRepos = getRepoList(context, RepoDbManager.queryAllRepo())
            for (repo in allRepos) {
                if (!repo.isExternal) {
                    repo.localPath = EXTERNAL_PREFIX + oldRoot.absolutePath + "/" + repo.localPath
                    RepoDbManager.setLocalPath(repo.id.toLong(), repo.localPath)
                }
            }
        }
    }
}