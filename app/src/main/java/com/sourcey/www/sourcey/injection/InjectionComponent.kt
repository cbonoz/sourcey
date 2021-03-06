package com.sourcey.www.sourcey.injection

import com.sourcey.www.sourcey.activities.MainActivity
import com.sourcey.www.sourcey.dialogs.SettingsDialogFragment

import javax.inject.Singleton

import dagger.Component

@Singleton
@Component(modules = arrayOf(SourceyModule::class))
interface InjectionComponent {

    // Activities
    fun inject(activity: MainActivity)


//    fun inject(activity: LoginActivity)
//    fun inject(activity: SplashActivity)

    // Fragments
    fun inject(fragment: SettingsDialogFragment)
//    fun inject(favoritesFragment: FavoritesFragment)
//    fun inject(genomeFragment: GenomeFragment)
//    fun inject(recipeFragment: RecipeFragment)
}
