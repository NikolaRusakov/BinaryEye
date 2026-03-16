package de.markusfisch.android.binaryeye.app

import android.app.Application
import de.comahe.i18n4k.createLocale
//import de.markusfisch.android.binaryeye.BuildConfig
import de.markusfisch.android.binaryeye.R
import de.markusfisch.android.binaryeye.database.Database
import de.markusfisch.android.binaryeye.preference.Preferences
import io.tolgee.Tolgee
import io.tolgee.storage.TolgeeStorageProviderAndroid
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.component3

val db = Database()
val prefs = Preferences()

class BinaryEyeApp : Application() {
override fun onCreate() {
    super.onCreate()
    db.open(this)
    prefs.init(this)
    val locales = resources.getStringArray(R.array.locale_values)
    val availableLocales = locales.map {
      val (language, countryOr, script) = it.replace("-r", "-").split("-".toRegex(), 2)
        .padWithNulls(3)
      val country = (script ?: "").ifEmpty { countryOr.orEmpty() }
      createLocale(language.orEmpty(), script, country)
    }
    Tolgee.init {
      contentDelivery {
        url = "https://cdn.tolg.ee/dbbedc13592d9ea9945332d83c1dc800"
        path = { "values-$it/strings.xml" }
        storage = TolgeeStorageProviderAndroid(this@BinaryEyeApp, 1)
        availableLocales(availableLocales)
      }
      defaultLanguage("cs")
    }
  }
}


inline fun <reified E> List<E>.padWithNulls(limit: Int): List<E?> {
  if (this.size >= limit) {
    return this
  }
  return this.toMutableList() + Array<E?>(limit - this.size) { null }
}