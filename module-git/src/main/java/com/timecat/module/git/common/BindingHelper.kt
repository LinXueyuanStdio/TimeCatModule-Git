package com.timecat.module.git.common

import androidx.databinding.BindingAdapter
import com.google.android.material.textfield.TextInputLayout


@BindingAdapter("android:errorText")
fun setErrorMessage(view: TextInputLayout, errorMessage: String?) {
    errorMessage.let { view.error = errorMessage }
}

