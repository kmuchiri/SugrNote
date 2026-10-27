package com.example.sugrnote.domain.export

enum class ExportFormat(val displayName: String, val extension: String, val mimeType: String) {
    PDF("PDF Medical Report", "pdf", "application/pdf"),
    HTML("Offline Web Page", "html", "text/html"),
    CSV("Raw Data Spreadsheet", "csv", "text/csv")
}
