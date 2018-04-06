package com.sourcey.www.sourcey.util

import com.sourcey.www.sourcey.BuildConfig
import java.util.*

class SourceyService(private val prefManager: PrefManager) {

    val random = Random()

    fun rand(from: Int, to: Int): Int {
        return random.nextInt(to - from) + from
    }

    companion object {

    }

}