package com.example.hacprototype

import android.app.AlertDialog
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.material.snackbar.Snackbar

fun Fragment.goTo(fragment: Fragment) {
    (activity as? MainActivity)?.navigateTo(fragment)
}

fun Fragment.openMemberApp(tabId: Int = R.id.navHome) {
    HabibiaSession.selectedMemberTab = tabId
    HabibiaSession.isAdmin = false
    HabibiaSession.selectedMemberId = null
    goTo(MemberHostFragment())
}

fun Fragment.openAdminApp() {
    HabibiaSession.isAdmin = true
    goTo(AdminDashboardFragment())
}

fun Fragment.showMessage(message: String) {
    val root = view ?: activity?.findViewById(android.R.id.content) ?: return
    Snackbar.make(root, message, Snackbar.LENGTH_SHORT).show()
}

fun Fragment.consumePendingMessage() {
    HabibiaSession.pendingSnackbar?.let {
        showMessage(it)
        HabibiaSession.pendingSnackbar = null
    }
}

fun Fragment.confirmDelete(onConfirm: () -> Unit) {
    AlertDialog.Builder(requireContext())
        .setTitle(R.string.confirm_delete_title)
        .setMessage(R.string.confirm_delete_message)
        .setPositiveButton("Delete") { _, _ -> onConfirm() }
        .setNegativeButton("Cancel", null)
        .show()
}

fun ViewGroup.clearAndInflate() {
    removeAllViews()
}

fun LinearLayout.bindEmptyState(title: String, body: String) {
    removeAllViews()
    val view = LayoutInflater.from(context).inflate(R.layout.view_empty_state, this, false)
    view.findViewById<TextView>(R.id.emptyTitle).text = title
    view.findViewById<TextView>(R.id.emptyBody).text = body
    addView(view)
}

fun TextView.setChipStyle(backgroundColor: Int, textColor: Int) {
    val bg = GradientDrawable().apply {
        cornerRadius = 40f
        setColor(backgroundColor)
    }
    background = bg
    setTextColor(textColor)
    setPadding(28, 10, 28, 10)
}

fun Fragment.color(resId: Int): Int = ContextCompat.getColor(requireContext(), resId)

fun simpleWatcher(onChanged: (String) -> Unit): TextWatcher = object : TextWatcher {
    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
    override fun afterTextChanged(s: Editable?) {
        onChanged(s?.toString().orEmpty())
    }
}

fun View.bindStat(label: String, value: String) {
    findViewById<TextView>(R.id.statLabel).text = label
    findViewById<TextView>(R.id.statValue).text = value
}

fun formatAverage(value: Double): String = String.format("%.2f", value)

fun formatImprovement(value: Double): String = String.format("%+.1f%%", value)

fun formatDisplayDate(iso: String): String {
    // Expects YYYY-MM-DD; falls back to original
    val parts = iso.split("-")
    if (parts.size != 3) return iso
    val months = listOf(
        "January", "February", "March", "April", "May", "June",
        "July", "August", "September", "October", "November", "December"
    )
    val monthIndex = parts[1].toIntOrNull()?.minus(1) ?: return iso
    val day = parts[2].toIntOrNull() ?: return iso
    val month = months.getOrNull(monthIndex) ?: return iso
    return "$day $month ${parts[0]}"
}

fun competitionStatusColor(status: String): Int = when (status.uppercase()) {
    "REGISTRATION OPEN" -> Color.parseColor("#1A9ABA55")
    "CLOSED" -> Color.parseColor("#1A6B7580")
    else -> Color.parseColor("#1A528FD0")
}

fun competitionStatusTextColor(status: String): Int = when (status.uppercase()) {
    "REGISTRATION OPEN" -> Color.parseColor("#7A9A3E")
    "CLOSED" -> Color.parseColor("#6B7580")
    else -> Color.parseColor("#528FD0")
}
