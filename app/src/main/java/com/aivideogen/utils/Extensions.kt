package com.aivideogen.utils

import android.content.Context
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.android.material.snackbar.Snackbar
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ── View extensions ───────────────────────────

fun View.show() { visibility = View.VISIBLE }
fun View.hide() { visibility = View.GONE }
fun View.invisible() { visibility = View.INVISIBLE }

fun View.showIf(condition: Boolean) {
    visibility = if (condition) View.VISIBLE else View.GONE
}

fun View.snackbar(message: String, duration: Int = Snackbar.LENGTH_SHORT) {
    Snackbar.make(this, message, duration).show()
}

fun View.snackbarAction(
    message: String,
    actionText: String,
    action: () -> Unit,
    duration: Int = Snackbar.LENGTH_LONG
) {
    Snackbar.make(this, message, duration)
        .setAction(actionText) { action() }
        .show()
}

// ── Context extensions ────────────────────────

fun Context.toast(message: String, long: Boolean = false) {
    Toast.makeText(this, message, if (long) Toast.LENGTH_LONG else Toast.LENGTH_SHORT).show()
}

// ── Fragment extensions ───────────────────────

fun Fragment.toast(message: String, long: Boolean = false) {
    requireContext().toast(message, long)
}

// ── String extensions ─────────────────────────

fun String.truncate(maxLength: Int, suffix: String = "…"): String {
    return if (length <= maxLength) this else take(maxLength - suffix.length) + suffix
}

// ── Long / Date extensions ────────────────────

fun Long.toFormattedDate(pattern: String = "MMM d, yyyy"): String {
    return SimpleDateFormat(pattern, Locale.getDefault()).format(Date(this))
}

fun Long.toFormattedDateTime(): String {
    return SimpleDateFormat("MMM d, yyyy  HH:mm", Locale.getDefault()).format(Date(this))
}

// ── Number extensions ─────────────────────────

fun Int.toFormattedDuration(): String {
    return when {
        this < 60  -> "${this}s"
        this < 3600 -> "${this / 60}m ${this % 60}s"
        else       -> "${this / 3600}h ${(this % 3600) / 60}m"
    }
}

fun Float.toPercent(): String = "${"%.0f".format(this * 100)}%"
