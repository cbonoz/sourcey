package com.sourcey.www.sourcey.injection

import android.app.Application
import com.google.gson.Gson

import com.sourcey.www.sourcey.SourceyApplication
import com.sourcey.www.sourcey.util.SourceyService
import com.sourcey.www.sourcey.util.PrefManager

import javax.inject.Singleton

import dagger.Module
import dagger.Provides


@Module
class SourceyModule(private val mApplication: Application) {

    @Provides
    @Singleton
    internal fun providesApplication(): Application {
        return mApplication
    }

    @Provides
    @Singleton
    internal fun providesGson(): Gson {
        return Gson()
    }

    @Provides
    @Singleton
    internal fun providesGenomeService(mApplication: Application, prefManager: PrefManager): SourceyService {
        return SourceyService(mApplication, prefManager)
    }

    @Provides
    @Singleton
    internal fun providesPrefManager(app: Application, gson: Gson): PrefManager {
        return PrefManager(app, gson)
    }

}
