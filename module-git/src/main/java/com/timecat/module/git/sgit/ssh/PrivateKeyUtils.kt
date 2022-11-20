package com.timecat.module.git.sgit.ssh

import com.timecat.module.git.utils.FsUtils
import com.timecat.module.git.sgit.ssh.PrivateKeyUtils
import com.jcraft.jsch.JSch
import com.jcraft.jsch.KeyPair
import java.io.*
import java.lang.Exception
import java.lang.IllegalArgumentException
import kotlin.Throws

object PrivateKeyUtils {
    @JvmStatic
    val privateKeyFolder: File
        get() = FsUtils.getInternalDir("ssh")
    @JvmStatic
    val publicKeyFolder: File
        get() = FsUtils.getInternalDir("sshpub")

    @JvmStatic
    fun getPublicKey(privateKey: File): File {
        return File(
            publicKeyFolder,
            privateKey.name
        )
    }

    @JvmStatic
    fun getPublicKeyEnsure(privateKey: File): File {
        val publicKey = getPublicKey(privateKey)
        if (!publicKey.exists()) {
            try {
                val jsch = JSch()
                val kpair = KeyPair.load(jsch, privateKey.absolutePath)
                kpair.writePublicKey(FileOutputStream(publicKey), "mgit")
                kpair.dispose()
            } catch (e: Exception) {
                //TODO
                e.printStackTrace()
            }
        }
        return publicKey
    }

    @JvmStatic
    fun migratePrivateKeys() {
        val oldDir = FsUtils.getExternalDir("ssh")
        if (oldDir.exists()) {
            try {
                copyDirectory(oldDir, privateKeyFolder)
                deleteDirectory(oldDir)
            } catch (ex: IOException) {
                ex.printStackTrace()
            }
        }
    }

    @JvmStatic
    @Throws(IOException::class)
    fun copyFile(src: File?, dst: File?) {
        val `in`: InputStream = FileInputStream(src)
        try {
            val out: OutputStream = FileOutputStream(dst)
            try {
                // Transfer bytes from in to out
                val buf = ByteArray(1024)
                var len: Int
                while (`in`.read(buf).also { len = it } > 0) {
                    out.write(buf, 0, len)
                }
            } finally {
                out.close()
            }
        } finally {
            `in`.close()
        }
    }

    // If targetLocation does not exist, it will be created.
    @JvmStatic
    @Throws(IOException::class)
    fun copyDirectory(sourceLocation: File, targetLocation: File) {
        if (sourceLocation.isDirectory) {
            if (!targetLocation.exists() && !targetLocation.mkdirs()) {
                throw IOException("Cannot create dir " + targetLocation.absolutePath)
            }
            val children = sourceLocation.list()
            for (i in children.indices) {
                copyDirectory(
                    File(sourceLocation, children[i]),
                    File(targetLocation, children[i])
                )
            }
        } else {

            // make sure the directory we plan to store the recording in exists
            val directory = targetLocation.parentFile
            if (directory != null && !directory.exists() && !directory.mkdirs()) {
                throw IOException("Cannot create dir " + directory.absolutePath)
            }
            val `in`: InputStream = FileInputStream(sourceLocation)
            val out: OutputStream = FileOutputStream(targetLocation)

            // Copy the bits from instream to outstream
            val buf = ByteArray(1024)
            var len: Int
            while (`in`.read(buf).also { len = it } > 0) {
                out.write(buf, 0, len)
            }
            `in`.close()
            out.close()
        }
    }

    /**
     * Cleans a directory without deleting it.
     *
     * @param directory directory to clean
     * @throws IOException              in case cleaning is unsuccessful
     * @throws IllegalArgumentException if `directory` does not exist or is not a directory
     */
    @Throws(IOException::class)
    fun cleanDirectory(directory: File) {
        val files = verifiedListFiles(directory)
        var exception: IOException? = null
        for (file in files) {
            try {
                forceDelete(file)
            } catch (ioe: IOException) {
                exception = ioe
            }
        }
        if (null != exception) {
            throw exception
        }
    }

    /**
     * Deletes a file. If file is a directory, delete it and all sub-directories.
     *
     *
     * The difference between File.delete() and this method are:
     *
     *  * A directory to be deleted does not have to be empty.
     *  * You get exceptions when a file or directory cannot be deleted.
     * (java.io.File methods returns a boolean)
     *
     *
     * @param file file or directory to delete, must not be `null`
     * @throws NullPointerException  if the directory is `null`
     * @throws FileNotFoundException if the file was not found
     * @throws IOException           in case deletion is unsuccessful
     */
    @Throws(IOException::class)
    private fun forceDelete(file: File) {
        if (file.isDirectory) {
            deleteDirectory(file)
        } else {
            val filePresent = file.exists()
            if (!file.delete()) {
                if (!filePresent) {
                    throw FileNotFoundException("File does not exist: $file")
                }
                val message = "Unable to delete file: $file"
                throw IOException(message)
            }
        }
    }

    /**
     * Deletes a directory recursively.
     *
     * @param directory directory to delete
     * @throws IOException              in case deletion is unsuccessful
     * @throws IllegalArgumentException if `directory` does not exist or is not a directory
     */
    @JvmStatic
    @Throws(IOException::class)
    fun deleteDirectory(directory: File) {
        if (!directory.exists()) {
            return
        }
        cleanDirectory(directory)
        if (!directory.delete()) {
            val message = "Unable to delete directory $directory."
            throw IOException(message)
        }
    }

    /**
     * Lists files in a directory, asserting that the supplied directory satisfies exists and is a directory
     *
     * @param directory The directory to list
     * @return The files in the directory, never null.
     * @throws IOException if an I/O error occurs
     */
    @Throws(IOException::class)
    private fun verifiedListFiles(directory: File): Array<File> {
        if (!directory.exists()) {
            val message = "$directory does not exist"
            throw IllegalArgumentException(message)
        }
        if (!directory.isDirectory) {
            val message = "$directory is not a directory"
            throw IllegalArgumentException(message)
        }
        return directory.listFiles()
            ?: // null if security restricted
            throw IOException("Failed to list contents of $directory")
    }
}