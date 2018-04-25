package com.sourcey.www.sourcey.util

import br.tiagohm.codeview.Language
import br.tiagohm.codeview.Theme
import java.util.*

class SourceyService(private val prefManager: PrefManager) {

    val random = Random()

    fun rand(from: Int, to: Int): Int {
        return random.nextInt(to - from) + from
    }

    fun getThemes(): List<Theme> {
        return Theme.ALL
    }

    fun saveSettings(settings: Settings) {
        prefManager.saveJson("settings", settings)
    }

    fun getSettings(): Settings {
        return prefManager.getJson(
                "settings",
                Settings::class.java,
                Settings(true, true, true, false, 0, 14f, Language.JAVA)
        )
    }

    fun getThemeNames(): List<String> {
        return getThemes().map {
            it.name
        }

    }

    fun getMaxFileSize(): Long {
        return LARGE_FILE_THRESHOLD
    }

    companion object {
        val LARGE_FILE_THRESHOLD = 400000L // 500 KB
        val LAST_FILE = "lastFile"
        val FIRST_LAUNCH = "firstLaunch"
    }

}