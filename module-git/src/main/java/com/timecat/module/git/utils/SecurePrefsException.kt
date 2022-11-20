package com.timecat.module.git.utils

import java.lang.Exception

/**
 * Exception in SecurePrefs processing.
 */
class SecurePrefsException : Exception {
    constructor(s: String?) : super(s) {}
    constructor(e: Exception?) : super(e) {}
}