package com.example.ui.quran

/**
 * INACTIVE BACKUP ARCHIVE OF THE OLD QURAN SYSTEM.
 *
 * Kept strictly as an inactive backup per project requirements ("পুরোনো ফাইলের ব্যাকআপ রাখবে এবং
 * নতুন সিস্টেম সফলভাবে যাচাই না করে স্থায়ীভাবে মুছবে না").
 *
 * All active references to the old synthetic 610-page canvas text lines and Google Drive WebView
 * have been removed from the active Quran Reader and replaced by the verified 616-page 15-Line
 * Indo-Pak Quran PDF (`assets/quran_hafezi_15line.pdf`, SHA-256:
 * `1305a0bcde5b9ec30b70ef7a9150c0bfcfc9af5735912b20cd204fc41a719828`).
 */
@Deprecated(
    message = "Inactive legacy backup only. Do not reference in active UI.",
    level = DeprecationLevel.WARNING
)
object HafeziLegacySystemBackup {
    const val LEGACY_TOTAL_PAGES_ASSUMPTION = 610
    const val LEGACY_DRIVE_FILE_ID = "19wzQyGpJ1Vg06rYBeQ5lVWyZ1HGkbgT2"
    const val BACKUP_STATUS_NOTE =
        "Replaced by internal native Android PdfRenderer using bundled 15-Line Indo-Pak Quran PDF (616 pages, SHA-256: 1305a0bcde5b9ec30b70ef7a9150c0bfcfc9af5735912b20cd204fc41a719828)."
}
