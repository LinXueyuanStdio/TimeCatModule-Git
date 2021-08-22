package com.timecat.module.git.dialogs

//import androidx.annotation.StringRes
import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.StringRes
import com.timecat.module.git.R
import com.timecat.module.git.android.views.SheimiDialogFragment
import com.timecat.module.git.sgit.dialogs.DummyDialogListener

class ExceptionDialog : SheimiDialogFragment() {
    private var mThrowable: Throwable? = null

    @StringRes
    private var mErrorRes: Int = 0

    @StringRes
    var errorTitleRes: Int = 0
    private lateinit var errorMessage: TextView

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        super.onCreateDialog(savedInstanceState)

        val builder = AlertDialog.Builder(rawActivity)
        val inflater = rawActivity.layoutInflater
        val layout = inflater.inflate(R.layout.git_dialog_exception, null)
        errorMessage = layout.findViewById<TextView>(R.id.error_message)
        errorMessage.setText(mErrorRes)

        builder.setView(layout)

        // set button listener
        builder.setTitle(if (errorTitleRes != 0) errorTitleRes else R.string.git_dialog_error_title)
        builder.setNegativeButton(
            getString(R.string.git_label_cancel),
            DummyDialogListener()
        )
        builder.setPositiveButton(
            getString(R.string.git_dialog_error_send_report),
            DummyDialogListener()
        )

        return builder.create()
    }

    override fun onStart() {
        super.onStart()
        val dialog = dialog as AlertDialog
        val positiveButton = dialog.getButton(Dialog.BUTTON_POSITIVE) as Button
        positiveButton.setOnClickListener {
            Toast.makeText(context, mThrowable?.localizedMessage, Toast.LENGTH_LONG).show()
            dismiss()
        }
        val negativeButton = dialog.getButton(Dialog.BUTTON_NEGATIVE)
        negativeButton.setOnClickListener {
            dismiss()
        }
    }

    fun setThrowable(throwable: Throwable?) {
        mThrowable = throwable
    }

    fun setErrorRes(@StringRes errorRes: Int) {
        mErrorRes = errorRes
    }
}
