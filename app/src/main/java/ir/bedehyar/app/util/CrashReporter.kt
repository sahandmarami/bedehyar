package ir.bedehyar.app.util

import android.content.Context
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter

/**
 * Keeps the last fatal error on disk so the user can see/share it.
 * The handler is defensive: it must never crash by itself, and it always
 * delegates to the previous handler so the normal crash flow is preserved.
 */
object CrashReporter {

    private const val FILE_NAME = "crash-latest.txt"
    private const val MAX_LEN = 20_000

    fun install(context: Context) {
        val appContext = context.applicationContext
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, e ->
            try {
                val sw = StringWriter()
                e.printStackTrace(PrintWriter(sw))
                val text = buildString {
                    appendLine("زمان: " + System.currentTimeMillis())
                    appendLine("نسخه: " + appVersion(appContext))
                    appendLine("پردازه: " + thread.name)
                    appendLine(sw.toString())
                }
                File(appContext.filesDir, FILE_NAME).writeText(text.take(MAX_LEN))
            } catch (_: Throwable) {
                // never let the reporter itself crash
            }
            previous?.uncaughtException(thread, e)
        }
    }

    fun lastCrash(context: Context): String? {
        val f = File(context.applicationContext.filesDir, FILE_NAME)
        if (!f.exists()) return null
        return try {
            f.readText().take(MAX_LEN)
        } catch (_: Throwable) {
            null
        }
    }

    /**
     * گزارش کرش فقط وقتی نشان داده می‌شود که از همین نسخه نصب‌شده باشد؛
     * گزارش نسخه‌های قبلی (مثلاً بعد از آپدیت) خودکار پاک می‌شود تا کاربر
     * فکر نکند برنامه هنوز خراب است.
     */
    fun lastCrashForCurrentVersion(context: Context): String? {
        val text = lastCrash(context) ?: return null
        val versionLine = text.lineSequence()
            .firstOrNull { it.startsWith("نسخه: ") }
            ?.trim()
            // فایل بدون خط نسخه = مربوط به نسخه‌های قبل از این قابلیت
            ?: return null.also { clear(context) }
        return if (versionLine == "نسخه: " + appVersion(context)) text
        else null.also { clear(context) }
    }

    private fun appVersion(context: Context): String = try {
        val pi = context.packageManager.getPackageInfo(context.packageName, 0)
        "${pi.versionCode} (${pi.versionName})"
    } catch (_: Throwable) {
        "?"
    }

    fun clear(context: Context) {
        try {
            File(context.applicationContext.filesDir, FILE_NAME).delete()
        } catch (_: Throwable) {
        }
    }
}
