package nl.ibs.gym8020.data

import android.content.Context
import android.content.res.Configuration
import java.util.Locale

/**
 * App language (D-88): English by default, Dutch selectable in Settings. Stored in plain SharedPreferences
 * because it is needed synchronously when the app and each screen start.
 */
object AppLanguage {
    const val ENGLISH = "en"
    const val DUTCH = "nl"
    private const val PREFS = "app_language"
    private const val KEY = "language"

    fun get(context: Context): String =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, ENGLISH) ?: ENGLISH

    fun set(context: Context, language: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY, language).apply()
    }

    /** A context in the chosen language; used by the Application and every Activity. */
    fun wrap(base: Context): Context {
        val locale = Locale(get(base))
        Locale.setDefault(locale)
        val config = Configuration(base.resources.configuration)
        config.setLocale(locale)
        return base.createConfigurationContext(config)
    }
}
