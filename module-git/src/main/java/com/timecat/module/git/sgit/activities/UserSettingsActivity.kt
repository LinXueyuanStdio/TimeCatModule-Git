package com.timecat.module.git.sgit.activities

import android.content.Intent
import android.text.InputType
import android.view.ViewGroup
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.input.input
import com.timecat.component.setting.DEF
import com.timecat.middle.setting.BaseSettingActivity
import com.timecat.middle.setting.MaterialForm
import com.timecat.module.git.R
import com.timecat.module.git.android.utils.BasicFunctions
import com.timecat.module.git.sgit.activities.explorer.ExploreRootDirActivity
import com.timecat.module.git.sgit.activities.explorer.PrivateKeyManageActivity

/**
 * Activity for user settings
 */
class UserSettingsActivity : BaseSettingActivity() {
    override fun title(): String = "设置 git"

    override fun addSettingItems(container: ViewGroup) {
        MaterialForm(this, container).apply {
            H1(getString(R.string.git_pref_category_title_general))
            Next(
                getString(R.string.git_preference_repo_location),
                DEF.git().getString(getString(R.string.git_pref_key_repo_root_location), "") ?: ""
            ) {
                startActivity(Intent(this@UserSettingsActivity, ExploreRootDirActivity::class.java))
            }
            Divider()
            H1(getString(R.string.git_pref_category_title_git_profile))
            val usernameKey = getString(R.string.git_pref_key_git_user_name)
            Next(
                getString(R.string.git_preference_git_user_name),
                DEF.git().getString(usernameKey, "") ?: ""
            ) {
                MaterialDialog(this@UserSettingsActivity).show {
                    title(res = R.string.git_preference_git_user_name)
                    positiveButton(R.string.ok)
                    input(prefill = DEF.git().getString(usernameKey, "") ?: "") { _, text ->
                        DEF.git().putString(usernameKey, text.toString())
                    }
                }
            }
            val emailKey = getString(R.string.git_pref_key_git_user_email)
            Next(
                getString(R.string.git_preference_git_user_email),
                DEF.git().getString(emailKey, "") ?: ""
            ) {
                MaterialDialog(this@UserSettingsActivity).show {
                    title(res = R.string.git_preference_git_user_name)
                    positiveButton(R.string.ok)
                    input(
                        prefill = DEF.git().getString(emailKey, "") ?: "",
                        inputType = InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
                    ) { _, text ->
                        DEF.git().putString(emailKey, text.toString())
                    }
                }
            }
            Divider()
            H1(getString(R.string.git_pref_category_title_security))
            Next(
                getString(R.string.git_preference_manage_ssh_keys),
                getString(R.string.git_preference_manage_ssh_keys_summary)
            ) {
                startActivity(Intent(this@UserSettingsActivity, PrivateKeyManageActivity::class.java))
            }
            val sshKey = getString(R.string.git_pref_key_use_gravatar)
            Switch(getString(R.string.git_preference_use_gravatar),
                getString(R.string.git_preference_use_gravatar_summary), {
                    DEF.git().getBoolean(sshKey, true)
                }) {
                DEF.git().putBoolean(sshKey, it)
            }

        }
    }
}