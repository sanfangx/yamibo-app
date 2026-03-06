import android.util.Log as AndroidLog

@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
actual object Log {
    actual fun v(tag: String, message: String, throwable: Throwable?) {
        if (throwable != null) {
            AndroidLog.v(tag, message, throwable)
        } else {
            AndroidLog.v(tag, message)
        }
    }

    actual fun d(tag: String, message: String, throwable: Throwable?) {
        if (throwable != null) {
            AndroidLog.d(tag, message, throwable)
        } else {
            AndroidLog.d(tag, message)
        }
    }

    actual fun i(tag: String, message: String, throwable: Throwable?) {
        if (throwable != null) {
            AndroidLog.i(tag, message, throwable)
        } else {
            AndroidLog.i(tag, message)
        }
    }

    actual fun w(tag: String, message: String, throwable: Throwable?) {
        if (throwable != null) {
            AndroidLog.w(tag, message, throwable)
        } else {
            AndroidLog.w(tag, message)
        }
    }

    actual fun e(tag: String, message: String, throwable: Throwable?) {
        if (throwable != null) {
            AndroidLog.e(tag, message, throwable)
        } else {
            AndroidLog.e(tag, message)
        }
    }
}
