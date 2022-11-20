package com.timecat.module.git.utils

import android.content.Context
import android.widget.ImageView
import androidx.annotation.StringRes
import com.timecat.component.commonsdk.utils.override.LogUtil
import com.timecat.layout.ui.utils.IconLoader
import com.timecat.middle.block.ext.showDialog
import com.timecat.module.git.R
import com.timecat.module.git.sgit.activities.SheimiFragmentActivity
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException

/**
 * Created by sheimi on 8/19/13.
 */
object BasicFunctions {
    @JvmStatic
    fun md5(s: String): String {
        try {
            // Create MD5 Hash
            val digest = MessageDigest.getInstance("MD5")
            digest.update(s.toByteArray())
            val messageDigest = digest.digest()

            // Create Hex String
            val hexString = StringBuffer()
            for (i in messageDigest.indices) {
                var h = Integer.toHexString(0xFF and messageDigest[i].toInt())
                while (h.length < 2) h = "0$h"
                hexString.append(h)
            }
            return hexString.toString()
        } catch (e: NoSuchAlgorithmException) {
            LogUtil.e(e)
        }
        return ""
    }

    @JvmStatic
    fun setAvatarImage(imageView: ImageView?, email: String) {
        var avatarUri = ""
        if (!email.isEmpty()) avatarUri = IconLoader.AVATAR_SCHEME + md5(email)
        IconLoader.loadIcon(activeActivity, imageView, avatarUri)
    }

    @JvmStatic
    var activeActivity: SheimiFragmentActivity? = null

    @JvmStatic
    fun showException(context: Context, throwable: Throwable?, @StringRes errorTitleRes: Int, @StringRes errorRes: Int) {
        context.showDialog {
            title(if (errorTitleRes != 0) errorTitleRes else R.string.git_dialog_error_title)
            message(errorRes)
            positiveButton(R.string.git_dialog_error_send_report)
            negativeButton(R.string.cancel)
        }
    }

    @JvmOverloads
    fun showException(activity: Context, throwable: Throwable, @StringRes errorRes: Int = 0) {
        showException(activity, throwable, 0, errorRes)
    }
}