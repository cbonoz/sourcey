package com.sourcey.www.sourcey.util

import br.tiagohm.codeview.Language


data class Settings(
        val wrapLine: Boolean,
        val lineNumber: Boolean,
        val zoomEnabled: Boolean,
        val languageDetection: Boolean,
        val themeIndex: Int,
        val fontSize: Float,
        val language: Language
)