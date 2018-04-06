package com.sourcey.www.sourcey.util

import java.util.*

class SourceyService(private val prefManager: PrefManager) {

    val random = Random()

    fun rand(from: Int, to: Int): Int {
        return random.nextInt(to - from) + from
    }

    fun saveSettings(settings: Settings) {
        prefManager.saveJson("settings", settings)
    }

    fun getSettings(): Settings {
        return prefManager.getJson(
                "settings",
                Settings::class.java,
                Settings(true, true, true,14f)
        )
    }

    companion object {
        val LARGE_FILE_THRESHOLD = 1000000
    }

}