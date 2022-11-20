package com.timecat.module.git.tasks

import com.timecat.module.git.R
import com.timecat.module.git.sgit.database.models.Repo
import com.timecat.module.git.utils.StopTaskException
import org.eclipse.jgit.api.errors.GitAPIException
import org.eclipse.jgit.diff.DiffEntry
import org.eclipse.jgit.diff.DiffFormatter
import org.eclipse.jgit.dircache.DirCacheIterator
import org.eclipse.jgit.errors.AmbiguousObjectException
import org.eclipse.jgit.errors.IncorrectObjectTypeException
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.revwalk.RevCommit
import org.eclipse.jgit.treewalk.AbstractTreeIterator
import org.eclipse.jgit.treewalk.CanonicalTreeParser
import org.eclipse.jgit.treewalk.EmptyTreeIterator
import org.eclipse.jgit.treewalk.FileTreeIterator
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.UnsupportedEncodingException

class CommitDiffTask(
    repo: Repo, private val mOldCommit: String, private val mNewCommit: String,
    private val mCallback: CommitDiffResult?, private val mShowDescription: Boolean
) : RepoOpTask(repo) {
    private var mDiffEntries: List<DiffEntry>? = null
    private var mDiffStrs: MutableList<String>? = null
    private var mCommits: Iterable<RevCommit>? = null
    private var mDiffFormatter: DiffFormatter? = null
    private var mDiffOutput: ByteArrayOutputStream? = null

    interface CommitDiffResult {
        fun pushResult(diffEntries: List<DiffEntry>?, diffStrs: List<String>?, description: RevCommit?)
    }

    override fun doInBackground(vararg params: Void): Boolean {
        val result = commitDiff
        if (!result) {
            return false
        }
        mDiffStrs = ArrayList(mDiffEntries!!.size)
        for (diffEntry in mDiffEntries!!) {
            try {
                val diffStr = parseDiffEntry(diffEntry)
                mDiffStrs?.add(diffStr)
            } catch (e: StopTaskException) {
                return false
            }
        }
        return true
    }

    override fun onPostExecute(isSuccess: Boolean) {
        super.onPostExecute(isSuccess)
        var retCommit: RevCommit? = null
        if (isSuccess && mCallback != null && mDiffEntries != null) {
            if (mCommits != null) {
                for (commit in mCommits!!) {
                    retCommit = commit
                    break
                }
            }
            mCallback.pushResult(mDiffEntries, mDiffStrs, retCommit)
        }
    }

    @Throws(IOException::class)
    private fun getTreeIterator(repo: Repository, commit: String): AbstractTreeIterator {
        if (commit == "dircache") {
            return DirCacheIterator(repo.readDirCache())
        }
        if (commit == "filetree") {
            return FileTreeIterator(repo)
        }
        val treeId = repo.resolve("$commit^{tree}") ?: throw NullPointerException()
        val treeIter = CanonicalTreeParser()
        val reader = repo.newObjectReader()
        treeIter.reset(reader, treeId)
        return treeIter
    }

    val commitDiff: Boolean
        get() {
            try {
                val git = mRepo.getGit() ?: return false
                val repo = git.repository
                mDiffOutput = ByteArrayOutputStream()
                mDiffFormatter = DiffFormatter(mDiffOutput)
                mDiffFormatter!!.setRepository(repo)
                val mOldCommitTreeIterator = if (mRepo.isInitialCommit(mNewCommit)) EmptyTreeIterator() else getTreeIterator(repo, mOldCommit)
                val mNewCommitTreeIterator = getTreeIterator(repo, mNewCommit)
                mDiffEntries = mDiffFormatter!!.scan(mOldCommitTreeIterator, mNewCommitTreeIterator)
                mCommits = if (mShowDescription) {
                    val newCommitId = repo.resolve(mNewCommit)
                    git.log().add(newCommitId).setMaxCount(1).call()
                } else {
                    ArrayList()
                }
                return true
            } catch (e: GitAPIException) {
                setException(e)
            } catch (e: IncorrectObjectTypeException) {
                setException(e, R.string.git_error_diff_failed)
            } catch (e: AmbiguousObjectException) {
                setException(e, R.string.git_error_diff_failed)
            } catch (e: IOException) {
                setException(e, R.string.git_error_diff_failed)
            } catch (e: IllegalStateException) {
                setException(e, R.string.git_error_diff_failed)
            } catch (e: NullPointerException) {
                setException(e, R.string.git_error_diff_failed)
            } catch (e: StopTaskException) {
            }
            return false
        }

    @Throws(StopTaskException::class)
    private fun parseDiffEntry(diffEntry: DiffEntry): String {
        return try {
            mDiffOutput!!.reset()
            mDiffFormatter!!.format(diffEntry)
            mDiffFormatter!!.flush()
            mDiffOutput!!.toString("UTF-8")
        } catch (e: UnsupportedEncodingException) {
            setException(e, R.string.git_error_diff_failed)
            throw StopTaskException()
        } catch (e: IOException) {
            setException(e, R.string.git_error_diff_failed)
            throw StopTaskException()
        }
    }

    override fun executeTask() {
        execute()
    }
}