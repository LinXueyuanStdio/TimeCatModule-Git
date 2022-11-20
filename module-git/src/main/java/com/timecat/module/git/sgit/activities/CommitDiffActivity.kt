package com.timecat.module.git.sgit.activities

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.widget.ProgressBar
import androidx.appcompat.widget.ShareActionProvider
import androidx.core.view.MenuItemCompat
import com.timecat.component.commonsdk.utils.override.LogUtil
import com.timecat.component.identity.Attr
import com.timecat.element.alert.ToastUtil
import com.timecat.module.git.R
import com.timecat.module.git.sgit.database.models.Repo
import com.timecat.module.git.sgit.database.models.Repo.Companion.getCommitDisplayName
import com.timecat.module.git.tasks.CommitDiffTask
import com.timecat.module.git.tasks.CommitDiffTask.CommitDiffResult
import com.timecat.module.git.utils.CodeGuesser
import com.timecat.module.git.utils.FsUtils.getExternalDir
import com.timecat.module.git.utils.Profile
import com.timecat.page.base.utils.MenuTintUtils
import com.timecat.page.base.view.BlurringToolbar
import org.eclipse.jgit.diff.DiffEntry
import org.eclipse.jgit.lib.PersonIdent
import org.eclipse.jgit.revwalk.RevCommit
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream

class CommitDiffActivity : SheimiFragmentActivity() {
    private var mDiffContent: WebView? = null
    private var mLoading: ProgressBar? = null
    private var mOldCommit: String? = null
    private var mNewCommit: String? = null
    private var mShowDescription = false
    private var mRepo: Repo? = null
    private var mCommit: RevCommit? = null
    private var mDiffStrs: List<String>? = null
    private var mDiffEntries: List<DiffEntry>? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.git_activity_view_diff)
        setupActionBar()
        mDiffContent = findViewById<View>(R.id.fileContent) as WebView
        mLoading = findViewById<View>(R.id.loading) as ProgressBar
        val extras = intent.extras
        mOldCommit = extras!!.getString(OLD_COMMIT)
        mNewCommit = extras.getString(NEW_COMMIT)
        mShowDescription = extras.getBoolean(SHOW_DESCRIPTION)
        mRepo = extras.getSerializable(Repo.TAG) as Repo?
        var title = getCommitDisplayName(mNewCommit)
        if (mOldCommit != null) title += " : " + getCommitDisplayName(mOldCommit)
        setTitle(getString(R.string.git_title_activity_commit_diff) + title)
        loadFileContent()
    }

    private fun loadFileContent() {
        mDiffContent!!.addJavascriptInterface(CodeLoader(), JS_INF)
        mDiffContent!!.loadDataWithBaseURL(
            "file:///android_asset/", HTML_TMPL,
            "text/html", "utf-8", null
        )
        val webSettings = mDiffContent!!.settings
        webSettings.javaScriptEnabled = true
        mDiffContent!!.webChromeClient = object : WebChromeClient() {
            override fun onConsoleMessage(message: String, lineNumber: Int, sourceID: String) {
                LogUtil.d("$message -- From line $lineNumber of $sourceID")
            }

            fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
                return false
            }
        }
        mDiffContent!!.setBackgroundColor(Color.TRANSPARENT)
    }

    private fun setupActionBar() {
        val toolbar = findViewById<BlurringToolbar>(R.id.toolbar)
        toolbar.setPaddingStatusBar(this)
        setSupportActionBar(toolbar)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.git_diff_commits, menu)
        val item = menu.findItem(R.id.action_share_diff)
        val shareActionProvider = MenuItemCompat.getActionProvider(item) as ShareActionProvider?
        val shareIntent = Intent(Intent.ACTION_SEND)
        shareIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        val futurePathName = Uri.fromFile(sharedDiffPathName())
        shareIntent.putExtra(Intent.EXTRA_STREAM, futurePathName)
        shareIntent.data = futurePathName
        shareIntent.type = "text/x-patch"
        shareActionProvider!!.setOnShareTargetSelectedListener { source, intent ->
            try {
                val diff = sharedDiffPathName()
                saveDiff(FileOutputStream(diff))
            } catch (e: IOException) {
                ToastUtil.e_long(R.string.git_alert_file_creation_failure)
            }
            false
        }
        shareActionProvider.setShareIntent(shareIntent)
        MenuTintUtils.tintAllIcons(menu, Attr.getIconColor(this))
        return true
    }

    private fun formatCommitInfo(): String {
        val committer: PersonIdent = mCommit!!.committerIdent
        val author: PersonIdent = mCommit!!.authorIdent
        return """commit $mNewCommit
Author:     ${author.name} <${author.emailAddress}>
AuthorDate: ${author.getWhen()}
Commit:     ${committer.name} <${committer.emailAddress}>
CommitDate: ${committer.getWhen()}
"""
    }

    @Throws(IOException::class)
    private fun saveDiff(fos: OutputStream?) {
        /* FIXME: LOCK!!! */
        if (mCommit != null) {
            val message: String
            fos!!.write(formatCommitInfo().toByteArray())
            fos.write("\n".toByteArray())
            message = mCommit!!.fullMessage
            for (line in message.split("\n").dropLastWhile { it.isEmpty() }.toTypedArray()) {
                fos.write("    $line\n".toByteArray())
            }
            fos.write("\n".toByteArray())
        }
        for (str in mDiffStrs!!) {
            fos!!.write(str.toByteArray())
        }
    }

    private fun sharedDiffPathName(): File {
        // Should we rather use createTempFile?
        var fname = mNewCommit
        if (mOldCommit != null) fname += "_$mOldCommit"
        return File(getExternalDir("diff", true), "$fname.diff")
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_SAVE_DIFF && resultCode == RESULT_OK) {
            val diffUri = data!!.data
            try {
                saveDiff(contentResolver.openOutputStream(diffUri!!))
            } catch (e: IOException) {
                ToastUtil.e_long(R.string.git_alert_file_creation_failure)
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val i = item.itemId
        if (i == android.R.id.home) {
            finish()
            return true
        } else if (i == R.id.action_save_diff) {
            val intent = Intent(Intent.ACTION_CREATE_DOCUMENT)
                .setType("text/x-patch")
                .putExtra(Intent.EXTRA_TITLE, getCommitDisplayName(mNewCommit) + ".diff")
            startActivityForResult(intent, REQUEST_SAVE_DIFF)
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private inner class CodeLoader {
        @JavascriptInterface
        fun getDiff(index: Int): String {
            return mDiffStrs!![index]
        }

        @JavascriptInterface
        fun haveCommitInfo(): Boolean {
            return mCommit != null
        }

        @JavascriptInterface
        fun getCommitInfo(): String = if (mCommit == null) {
            ""
        } else formatCommitInfo()

        @JavascriptInterface
        fun getCommitMessage(): String = if (mCommit == null) {
            ""
        } else mCommit!!.fullMessage

        @JavascriptInterface
        fun getChangeType(index: Int): String {
            val diff = mDiffEntries!![index]
            val ct = diff.changeType
            return ct.toString()
        }

        @JavascriptInterface
        fun getOldPath(index: Int): String {
            val diff = mDiffEntries!![index]
            return diff.oldPath
        }

        @JavascriptInterface
        fun getNewPath(index: Int): String {
            val diff = mDiffEntries!![index]
            return diff.newPath
        }

        @JavascriptInterface
        fun getDiffEntries() {
            val oldCommit = if (mOldCommit != null) mOldCommit!! else "$mNewCommit^"
            val diffTask = CommitDiffTask(
                mRepo!!, oldCommit, mNewCommit!!, object : CommitDiffResult {
                    override fun pushResult(diffEntries: List<DiffEntry>?, diffStrs: List<String>?, description: RevCommit?) {
                        mDiffEntries = diffEntries
                        mDiffStrs = diffStrs
                        mCommit = description
                        mLoading!!.visibility = View.GONE
                        mDiffContent!!.loadUrl(CodeGuesser.wrapUrlScript("notifyEntriesReady();"))
                    }
                }, mShowDescription
            )
            diffTask.executeTask()
        }

        @JavascriptInterface
        fun getDiffSize(): Int {
            return mDiffEntries!!.size
        }

        @JavascriptInterface
        fun getTheme(): String {
            return Profile.getCodeMirrorTheme(applicationContext)
        }
    }

    companion object {
        const val OLD_COMMIT = "old commit"
        const val NEW_COMMIT = "new commit"
        const val SHOW_DESCRIPTION = "show_description"
        private const val REQUEST_SAVE_DIFF = 1
        private const val JS_INF = "CodeLoader"
        private const val HTML_TMPL = ("<!doctype html>"
            + "<head>"
            + " <script src=\"js/jquery.js\"></script>"
            + " <script src=\"js/highlight.pack.js\"></script>"
            + " <script src=\"js/local_commits_diff.js\"></script>"
            + " <link type=\"text/css\" rel=\"stylesheet\" href=\"css/rainbow.css\" />"
            + " <link type=\"text/css\" rel=\"stylesheet\" href=\"css/local_commits_diff.css\" />"
            + "</head><body></body>")
    }
}