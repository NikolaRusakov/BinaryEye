package de.markusfisch.android.binaryeye.app

import android.content.Context
import android.os.Build
import io.tolgee.Tolgee
import java.util.Locale

fun Context.applyLocale(localeName: String) {
	val tolgee = Tolgee.instance
	if (localeName.isEmpty()) {
		return
	}
	val localeParts = localeName.split("-")
	val locale = if (localeParts.size == 2) {
//		Locale(localeParts[0], localeParts[1])
		tolgee.setLocale(localeParts[1])
	} else {
//		Locale(localeName)
		tolgee.setLocale(localeName)
	}
//	Locale.setDefault(locale)
	val conf = resources.configuration
	if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
		@Suppress("DEPRECATION")
		conf.locale = locale
	} else {
//		conf.setLocale(locale)
		tolgee.setLocale(localeName)
	}
	resources.updateConfiguration(conf, resources.displayMetrics)
}
