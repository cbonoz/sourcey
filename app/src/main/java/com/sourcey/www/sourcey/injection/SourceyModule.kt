package com.sourcey.www.sourcey.injection

import com.google.gson.Gson

import com.sourcey.www.sourcey.SourceyApplication
import com.sourcey.www.sourcey.util.SourceyService
import com.sourcey.www.sourcey.util.PrefManager

import javax.inject.Singleton

import dagger.Module
import dagger.Provides


@Module
class SourceyModule(private val mApplication: SourceyApplication) {

    @Provides
    @Singleton
    internal fun providesApplication(): SourceyApplication {
        return mApplication
    }

    @Provides
    @Singleton
    internal fun providesGson(): Gson {
        return Gson()
    }

    @Provides
    @Singleton
    internal fun providesGenomeService(prefManager: PrefManager): SourceyService {
        return SourceyService(prefManager)
    }

    @Provides
    @Singleton
    internal fun providesPrefManager(app: SourceyApplication, gson: Gson): PrefManager {
        return PrefManager(app, gson)
    }

}
