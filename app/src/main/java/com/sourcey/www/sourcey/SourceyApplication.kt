package com.sourcey.www.sourcey

import android.app.Application
import com.github.ajalt.timberkt.Timber
import com.github.ajalt.timberkt.d

import com.sourcey.www.sourcey.injection.DaggerInjectionComponent
import com.sourcey.www.sourcey.injection.InjectionComponent
import com.sourcey.www.sourcey.injection.SourceyModule
import timber.log.Timber.DebugTree



class SourceyApplication : Application() {
    private var mInjectionComponent: InjectionComponent? = null

    override fun onCreate() {
        super.onCreate()
        Timber.plant(DebugTree())
//        if (BuildConfig.DEBUG) {
//            Timber.plant(DebugTree())
//        } else {
//            Timber.plant(CrashReportingTree())
//        }

        d { "onCreate "}
        mInjectionComponent = DaggerInjectionComponent.builder()
                .sourceyModule(SourceyModule(this))
                .build()

        app = this
    }

    companion object {
        private var app: SourceyApplication? = null

        val injectionComponent: InjectionComponent
            get() = app!!.mInjectionComponent!!
    }
}
