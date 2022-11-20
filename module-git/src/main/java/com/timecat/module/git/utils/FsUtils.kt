package com.timecat.module.git.utils

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.webkit.MimeTypeMap
import androidx.core.content.FileProvider
import com.timecat.module.git.R
import com.timecat.module.git.sgit.activities.SheimiFragmentActivity
import com.timecat.module.git.utils.BasicFunctions.activeActivity
import com.timecat.module.git.sgit.ssh.PrivateKeyUtils
import com.timecat.module.git.sgit.ssh.PrivateKeyUtils.deleteDirectory
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

/**
 * Created by sheimi on 8/8/13.
 */
object FsUtils {
    val TIMESTAMP_FORMATTER = SimpleDateFormat(
        "yyyyMMdd_HHmmss", Locale.getDefault()
    )
    const val PROVIDER_AUTHORITY = "com.time.cat.file.provider"
    const val TEMP_DIR = "temp"
    private val LOGTAG = FsUtils::class.java.simpleName

    @Throws(IOException::class)
    fun createTempFile(subfix: String?): File {
        val dir = getExternalDir(TEMP_DIR)
        val fileName = TIMESTAMP_FORMATTER.format(Date())
        val file = File.createTempFile(fileName, subfix, dir)
        file.deleteOnExit()
        return file
    }

    /**
     * Get a File representing a dir within the external shared location where files can be stored specific to this app
     * creating the dir if it doesn't already exist
     *
     * @param dirname
     * @return
     */
    fun getExternalDir(dirname: String?): File {
        return getExternalDir(dirname, true)
    }

    /**
     * @param dirname
     * @return
     */
    fun getInternalDir(dirname: String?): File {
        return getExternalDir(dirname, true, false)
    }

    /**
     * Get a File representing a dir within the external shared location where files can be stored specific to this app
     *
     * @param dirname
     * @param isCreate create the dir if it does not already exist
     * @return
     */
    fun getExternalDir(dirname: String?, isCreate: Boolean): File {
        return getExternalDir(dirname, isCreate, true)
    }

    /**
     * Get a File representing a dir within the location where files can be stored specific to this app
     *
     * @param dirname    name of the dir to return
     * @param isCreate   create the dir if it does not already exist
     * @param isExternal if true, will use external *shared* storage
     * @return
     */
    fun getExternalDir(dirname: String?, isCreate: Boolean, isExternal: Boolean): File {
        val mDir = File(getAppDir(isExternal), dirname)
        if (!mDir.exists() && isCreate) {
            mDir.mkdir()
        }
        return mDir
    }

    /**
     * Get a File representing the location where files can be stored specific to this app
     *
     * @param isExternal if true, will use external *shared* storage
     * @return
     */
    fun getAppDir(isExternal: Boolean): File? {
        val activeActivity = activeActivity
        return if (isExternal) {
            activeActivity!!.getExternalFilesDir(null)
        } else {
            activeActivity!!.filesDir
        }
    }

    fun getMimeType(url: String): String {
        var type: String? = null
        val extension = MimeTypeMap.getFileExtensionFromUrl(
            url
                .lowercase(Locale.getDefault())
        )
        if (extension != null) {
            val mime = MimeTypeMap.getSingleton()
            type = mime.getMimeTypeFromExtension(extension)
        }
        if (type == null) {
            type = "text/plain"
        }
        return type
    }

    @JvmStatic
    fun getMimeType(file: File?): String {
        return getMimeType(Uri.fromFile(file).toString())
    }

    @JvmStatic
    fun openFile(activity: SheimiFragmentActivity, file: File) {
        //openFile(file, null);
        startActivityToEditFile(activity, file)
    }

    private fun startActivityToEditFile(activity: SheimiFragmentActivity, file: File) {
        val uri = FileProvider.getUriForFile(activity, PROVIDER_AUTHORITY, file)
        val mimeType = getMimeType(uri.toString())
        val editIntent = Intent(Intent.ACTION_EDIT)
        editIntent.setDataAndType(uri, mimeType)
        editIntent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        try {
            activity.startActivity(
                Intent.createChooser(
                    editIntent,
                    activity.resources.getString(R.string.edit)
                )
            )
        } catch (e: ActivityNotFoundException) {
            activity.showMessageDialog(R.string.git_dialog_error_title, activity.getString(R.string.git_error_no_edit_app))
        }
    }

    @JvmStatic
    fun deleteFile(file: File) {
        val to = File(file.absolutePath + System.currentTimeMillis())
        file.renameTo(to)
        deleteFileInner(to)
    }

    private fun deleteFileInner(file: File) {
        if (!file.isDirectory) {
            file.delete()
            return
        }
        try {
            deleteDirectory(file)
        } catch (e: IOException) {
            //TODO 
            e.printStackTrace()
        }
    }

    @JvmStatic
    fun copyFile(from: File?, to: File?) {
        try {
            PrivateKeyUtils.copyFile(from, to)
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    @JvmStatic
    fun copyDirectory(from: File, to: File?) {
        if (!from.exists()) return
        try {
            PrivateKeyUtils.copyDirectory(from, to!!)
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    @JvmStatic
    fun renameDirectory(dir: File, name: String): Boolean {
        val newDirPath = dir.parent + File.separator + name
        val newDirFile = File(newDirPath)
        return dir.renameTo(newDirFile)
    }

    @JvmStatic
    fun getRelativePath(file: File, base: File): String {
        return base.toURI().relativize(file.toURI()).path
    }

    @JvmStatic
    fun joinPath(dir: File, relative_path: String): File {
        return File(dir.absolutePath + File.separator + relative_path)
    }
}