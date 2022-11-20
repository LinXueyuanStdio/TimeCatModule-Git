package com.timecat.module.git.sgit.activities

import android.app.AlertDialog
import android.content.DialogInterface
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.widget.CheckBox
import android.widget.EditText
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.input.input
import com.afollestad.materialdialogs.list.listItemsSingleChoice
import com.timecat.element.alert.ToastUtil
import com.timecat.page.base.base.theme.BaseThemeActivity
import com.timecat.module.git.R
import com.timecat.module.git.utils.BasicFunctions

open class SheimiFragmentActivity : BaseThemeActivity() {
    interface OnBackClickListener {
        fun onClick(): Boolean
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        BasicFunctions.activeActivity = this
    }

    override fun onResume() {
        super.onResume()
        BasicFunctions.activeActivity = this
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            finish()
            return true
        }
        return false
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            MGIT_PERMISSIONS_REQUEST -> {
                // If request is cancelled, the result arrays are empty.
                if (!grantResults.isNotEmpty() || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    // permission denied
                    showMessageDialog(
                        R.string.git_dialog_not_supported,
                        getString(R.string.git_dialog_permission_not_granted)
                    )
                }
                return
            }
        }
    }

    protected fun checkAndRequestRequiredPermissions(permission: String) {
        if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
            // Permission is not granted, so request it from user
            ActivityCompat.requestPermissions(this, arrayOf(permission), MGIT_PERMISSIONS_REQUEST)
        }
    }

    /* View Utils Start */
    interface OnPositiveClickListener {
        fun onClick()
    }

    fun showMessageDialog(
        title: Int, msg: Int, positiveBtn: Int,
        positiveListener: OnPositiveClickListener?
    ) {
        MaterialDialog(this).show {
            title(res = title)
            message(res = msg)
            negativeButton(res = R.string.cancel)
            positiveButton(positiveBtn) {
                positiveListener?.onClick()
            }
        }
    }

    fun showMessageDialog(
        title: Int, msg: String?, positiveBtn: Int,
        positiveListener: OnPositiveClickListener?
    ) {
        MaterialDialog(this).show {
            title(res = title)
            message(text = msg)
            negativeButton(res = R.string.cancel)
            positiveButton(positiveBtn) {
                positiveListener?.onClick()
            }
        }
    }

    fun showMessageDialog(
        title: Int, msg: String?, positiveBtn: Int,
        negativeBtn: Int, positiveListener: DialogInterface.OnClickListener?,
        negativeListener: DialogInterface.OnClickListener?
    ) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle(title).setMessage(msg)
            .setPositiveButton(positiveBtn, positiveListener)
            .setNegativeButton(negativeBtn, negativeListener).show()
    }

    fun showMessageDialog(title: Int, msg: String?) {
        MaterialDialog(this).show {
            title(res = title)
            message(text = msg)
            positiveButton(R.string.ok)
        }
    }

    fun showOptionsDialog(
        title: Int, option_names: Int,
        option_listeners: Array<onOptionDialogClicked>
    ) {
        MaterialDialog(this).show {
            title(res = title)
            listItemsSingleChoice(res = option_names) { _, which, _ ->
                option_listeners[which].onClicked()
            }
        }
    }

    fun showOptionsDialog(
        title: Int, option_values: List<String>,
        option_listeners: Array<onOptionDialogClicked>
    ) {
        MaterialDialog(this).show {
            title(res = title)
            listItemsSingleChoice(items = option_values) { _, which, _ ->
                option_listeners[which].onClicked()
            }
        }
    }

    fun showEditTextDialog(
        title: Int, hint: Int, positiveBtn: Int,
        positiveListener: OnEditTextDialogClicked
    ) {
        MaterialDialog(this).show {
            title(res = title)
            positiveButton(res = positiveBtn)
            negativeButton(res = R.string.cancel)
            input(hintRes = hint, allowEmpty = false) { _, text ->
                if (text.trim { it <= ' ' }.isEmpty()) {
                    ToastUtil.w_long(R.string.git_alert_you_should_input_something)
                    return@input
                }
                positiveListener.onClicked(text.toString())
            }
        }
    }

    fun promptForPassword(
        onPasswordEntered: OnPasswordEntered,
        errorInfo: String?
    ) {
        runOnUiThread { promptForPasswordInner(onPasswordEntered, errorInfo) }
    }

    private fun promptForPasswordInner(
        onPasswordEntered: OnPasswordEntered,
        errorInfo: String?
    ) {
        val builder = AlertDialog.Builder(this)
        val inflater = layoutInflater
        val layout = inflater.inflate(
            R.layout.git_dialog_prompt_for_password,
            null
        )
        val username = layout.findViewById<View>(R.id.username) as EditText
        val password = layout.findViewById<View>(R.id.password) as EditText
        val checkBox = layout.findViewById<View>(R.id.savePassword) as CheckBox
        builder.setTitle(errorInfo ?: getString(R.string.git_dialog_prompt_for_password_title))
            .setView(layout)
            .setPositiveButton(R.string.git_label_done) { _, _ ->
                onPasswordEntered.onClicked(username.text.toString(), password.text.toString(), checkBox.isChecked)
            }
            .setNegativeButton(R.string.git_label_cancel) { _, _ ->
                onPasswordEntered.onCanceled()
            }
            .show()
    }

    interface onOptionDialogClicked {
        fun onClicked()
    }

    interface OnEditTextDialogClicked {
        fun onClicked(text: String?)
    }

    /**
     * Callback interface to receive credentials entered via UI by the user after being prompted
     * in the UI in order to connect to a remote repo
     */
    interface OnPasswordEntered {
        /**
         * Handle retrying a Remote Repo task after user supplies requested credentials
         *
         * @param username
         * @param password
         * @param savePassword
         */
        fun onClicked(username: String, password: String, savePassword: Boolean)
        fun onCanceled()
    }

    /* View Utils End */
    /* Switch Activity Animation Start */
    companion object {
        private const val MGIT_PERMISSIONS_REQUEST = 123

        /* Switch Activity Animation End */ /* ImageCache Start */
        private const val SIZE = 100 shl 20
    }
}