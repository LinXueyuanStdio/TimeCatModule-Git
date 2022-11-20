package com.timecat.module.git.sgit.database.models

import com.timecat.component.commonsdk.utils.override.LogUtil
import org.eclipse.jgit.lib.StoredConfig
import java.io.IOException

/**
 * Model for Git configuration
 */
class GitConfig(repo: Repo) {
    private val mConfig: StoredConfig?
    private val USER_SECTION = "name"
    private val NAME_SUBSECTION = "name"
    private val EMAIL_SUBSECTION = "email"

    /**
     * Create a Git Config for a specific repo
     *
     * @param repo
     */
    init {
        mConfig = repo.getStoredConfig()
    }

    var userName: String?
        get() = getSubsection(NAME_SUBSECTION)
        set(name) = setSubsection(NAME_SUBSECTION, name)
    var userEmail: String?
        get() = getSubsection(EMAIL_SUBSECTION)
        set(email) = setSubsection(EMAIL_SUBSECTION, email)

    private fun setSubsection(subsection: String, value: String?) {
        if (value == null || value == "") {
            mConfig?.unset(USER_SECTION, null, subsection)
        } else {
            mConfig?.setString(USER_SECTION, null, subsection, value)
        }
        try {
            mConfig?.save()
        } catch (e: IOException) {
            LogUtil.e(e)
        }
    }

    private fun getSubsection(subsection: String): String? {
        return mConfig?.getString(USER_SECTION, null, subsection)
    }
}