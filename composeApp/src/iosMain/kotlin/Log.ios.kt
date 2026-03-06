@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
actual object Log {
    actual fun v(tag: String, message: String, throwable: Throwable?) {
        println("V/$tag: $message")
        throwable?.printStackTrace()
    }

    actual fun d(tag: String, message: String, throwable: Throwable?) {
        println("D/$tag: $message")
        throwable?.printStackTrace()
    }

    actual fun i(tag: String, message: String, throwable: Throwable?) {
        println("I/$tag: $message")
        throwable?.printStackTrace()
    }

    actual fun w(tag: String, message: String, throwable: Throwable?) {
        println("W/$tag: $message")
        throwable?.printStackTrace()
    }

    actual fun e(tag: String, message: String, throwable: Throwable?) {
        println("E/$tag: $message")
        throwable?.printStackTrace()
    }
}
