package com.sourcey.www.sourcey.util

import android.app.Application
import io.github.kbiakov.codeview.highlight.ColorTheme
import io.github.kbiakov.codeview.highlight.Font
import java.util.*

class SourceyService(private val app: Application, private val prefManager: PrefManager) {

    val random = Random()

    fun rand(from: Int, to: Int): Int {
        return random.nextInt(to - from) + from
    }

    fun saveSettings(setting: Settings) {
        prefManager.saveJson("setting", setting)
    }

    fun getSettings(): Settings {
        return prefManager.getJson(
                "setting",
                Settings::class.java,
                Settings(false, 0, 0)
        )
    }

    fun getFonts(): List<Font> {
        return listOf(
                Font.Default,
                Font.Monaco,
                Font.Consolas,
                Font.CourierNew,
                Font.DejaVuSansMono,
                Font.DroidSansMonoSlashed,
                Font.Inconsolata
        )
    }

    fun getFontNames(): List<String> {
        return getFonts().map {
            it.name
        }
    }

    fun getThemes(): List<ColorTheme> {
        return listOf(
                ColorTheme.DEFAULT,
                ColorTheme.MONOKAI,
                ColorTheme.SOLARIZED_LIGHT
        )
    }

    fun getThemeNames(): List<String> {
        return getThemes().map {
            it.name
        }
    }

    companion object {
        val LARGE_FILE_THRESHOLD = 200000L
        val LAST_FILE = "lastFile"
        val FIRST_LAUNCH = "firstLaunch"
    }

}