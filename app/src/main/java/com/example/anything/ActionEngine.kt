package com.example.armvisionassist

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.util.Patterns
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup

object ActionEngine {

    fun generateSmartActions(context: Context, text: String, chipGroup: ChipGroup) {
        chipGroup.removeAllViews()

        val phoneMatcher = Patterns.PHONE.matcher(text)
        while (phoneMatcher.find()) {
            val number = phoneMatcher.group()
            addChip(context, chipGroup, "📞 Call $number") {
                context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$number")))
            }
        }

        val urlMatcher = Patterns.WEB_URL.matcher(text)
        while (urlMatcher.find()) {
            val url = urlMatcher.group()
            val cleanUrl = if (!url.startsWith("http")) "https://$url" else url
            addChip(context, chipGroup, "🌐 Open Link") {
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(cleanUrl)))
            }
        }

        // Always add Copy
        addChip(context, chipGroup, "📋 Copy Text") {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
            val clip = android.content.ClipData.newPlainText("Scanned", text)
            clipboard.setPrimaryClip(clip)
            android.widget.Toast.makeText(context, "Copied!", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    private fun addChip(context: Context, parent: ChipGroup, label: String, onClick: () -> Unit) {
        val chip = Chip(context).apply {
            text = label
            setChipIconResource(android.R.drawable.ic_menu_search)
            isClickable = true
            setOnClickListener { onClick() }
            chipBackgroundColor = android.content.res.ColorStateList.valueOf(Color.parseColor("#2962FF"))
            setTextColor(Color.WHITE)
        }
        parent.addView(chip)
    }
}