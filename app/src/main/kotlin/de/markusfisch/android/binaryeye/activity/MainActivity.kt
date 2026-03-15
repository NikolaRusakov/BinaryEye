package de.markusfisch.android.binaryeye.activity

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.floatingactionbutton.FloatingActionButton
import de.markusfisch.android.binaryeye.R
import de.markusfisch.android.binaryeye.app.PERMISSION_LOCATION
import de.markusfisch.android.binaryeye.app.PERMISSION_WRITE
import de.markusfisch.android.binaryeye.app.permissionGrantedCallback
import de.markusfisch.android.binaryeye.app.prefs
import de.markusfisch.android.binaryeye.app.setFragment
import de.markusfisch.android.binaryeye.database.Scan
import de.markusfisch.android.binaryeye.fragment.DecodeFragment
import de.markusfisch.android.binaryeye.fragment.EncodeFragment
import de.markusfisch.android.binaryeye.fragment.HistoryFragment
import de.markusfisch.android.binaryeye.fragment.PreferencesFragment
import de.markusfisch.android.binaryeye.net.isEncodeDeeplink
import de.markusfisch.android.binaryeye.view.colorSystemAndToolBars
import de.markusfisch.android.binaryeye.view.initBars
import io.tolgee.Tolgee
import io.tolgee.TolgeeAndroid
import io.tolgee.TolgeeContextWrapper
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity(), Tolgee.ChangeListener  {
private val tolgee: TolgeeAndroid = Tolgee.instance

	override fun onRequestPermissionsResult(
		requestCode: Int,
		permissions: Array<String>,
		grantResults: IntArray
	) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
			PERMISSION_LOCATION, PERMISSION_WRITE -> if (
				grantResults.isNotEmpty() &&
				grantResults[0] == PackageManager.PERMISSION_GRANTED
			) {
				permissionGrantedCallback?.invoke()
				permissionGrantedCallback = null
			}
		}
	}

	override fun onSupportNavigateUp(): Boolean {
		val fm = supportFragmentManager
		if (fm.backStackEntryCount > 0) {
			fm.popBackStack()
		} else {
			finish()
		}
		return true
	}

	override fun attachBaseContext(base: Context?) {
		super.attachBaseContext(TolgeeContextWrapper.wrap(base))
	}

	override fun onCreate(state: Bundle?) {
		super.onCreate(state)
		tolgee.addChangeListener(this)
		setContentView(R.layout.activity_main)
		val context = applicationContext
		lifecycleScope.launch {
			tolgee.changeFlow.collect {
				// Re-translate views without recreating the Activity for smoother UX
				tolgee.retranslate(this@MainActivity) // or recreate() for more complex activities

				// Make sure the app title is updated
				// Make sure the app title stays updated
				setTitle(tolgee.t(context, R.string.scan_code))

				// Still need to manually update parameterized strings and plurals
				// updateParameterizedStrings()
			}
		}

		initBars()
		supportActionBar?.setDisplayHomeAsUpEnabled(true)

		supportFragmentManager.addOnBackStackChangedListener {
			colorSystemAndToolBars(this@MainActivity)
		}

		if (state == null) {
			val fragment = intent?.getFragmentForIntent()
			if (fragment == null) {
				finish()
				return
			}
			supportFragmentManager?.setFragment(fragment)
		}

		findViewById<FloatingActionButton>(R.id.language).setOnClickListener {
			showLanguageDialog()
		}
	}

	private fun showLanguageDialog() {
		val names = resources.getStringArray(R.array.locale_names)
		val values = resources.getStringArray(R.array.locale_values)
		val currentIndex = values.indexOf(prefs.customLocale).coerceAtLeast(0)
		AlertDialog.Builder(this)
			.setTitle(R.string.custom_locale)
			.setSingleChoiceItems(names, currentIndex) { dialog, which ->
				val locale = values[which]
				prefs.customLocale = locale
				tolgee.setLocale(locale)
				tolgee.preload(this)
				dialog.dismiss()

			}
			.setNegativeButton(android.R.string.cancel, null)
			.show()
	}

	override fun onTranslationsChanged() {

		// Re-translate views without recreating the Activity for smoother UX
		tolgee.retranslate(this) // or recreate() for more complex activities


		setTitle(tolgee.t(applicationContext, R.string.scan_code))


		// Still need to manually update parameterized strings and plurals
		updateParameterizedStrings()
	}

	companion object {
		private const val PREFERENCES = "preferences"
		private const val HISTORY = "history"
		private const val ENCODE = "encode"
		const val DECODED = "decoded"

		private fun Intent.getFragmentForIntent(): Fragment? = when {
			hasExtra(PREFERENCES) -> PreferencesFragment()
			hasExtra(HISTORY) -> HistoryFragment()

			dataString?.let(::isEncodeDeeplink) == true -> {
				val uri = Uri.parse(dataString)
				EncodeFragment.newInstance(
					content = uri.getQueryParameter("content"),
					format = uri.getQueryParameter("format")?.uppercase(),
					execute = uri.getQueryParameter("execute")
						.let { it == "" || it.toBoolean() }
				)
			}

			hasExtra(ENCODE) -> EncodeFragment.newInstance(
				getStringExtra(ENCODE)
			)

			hasExtra(DECODED) -> getScanExtra(DECODED)?.let {
				DecodeFragment.newInstance(it)
			}

			else -> PreferencesFragment()
		}

		fun getPreferencesIntent(context: Context): Intent {
			val intent = Intent(context, MainActivity::class.java)
			intent.putExtra(PREFERENCES, true)
			return intent
		}

		fun getHistoryIntent(context: Context): Intent {
			val intent = Intent(context, MainActivity::class.java)
			intent.putExtra(HISTORY, true)
			return intent
		}

		fun getEncodeIntent(
			context: Context,
			text: String? = "",
			isExternal: Boolean = false
		): Intent {
			val intent = Intent(context, MainActivity::class.java)
			intent.putExtra(ENCODE, text)
			if (isExternal) {
				val flagActivityClearTask = if (
					Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB
				) {
					Intent.FLAG_ACTIVITY_CLEAR_TASK
				} else 0
				intent.addFlags(
					Intent.FLAG_ACTIVITY_NO_HISTORY or
							flagActivityClearTask or
							Intent.FLAG_ACTIVITY_NEW_TASK
				)
			}
			return intent
		}

		fun getDecodeIntent(context: Context, scan: Scan): Intent {
			val intent = Intent(context, MainActivity::class.java)
			intent.putExtra(DECODED, scan)
			return intent
		}
	}
}

@Suppress("DEPRECATION")
private fun Intent.getScanExtra(name: String): Scan? = if (
	Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
) {
	try {
		getParcelableExtra(name, Scan::class.java)
	} catch (_: Throwable) {
		// `getParcelableExtra(name, Class)` can throw on some versions
		// of Android when the ClassLoader associated with the Bundle's
		// lazy-decoded value is null in certain scenarios (e.g., after
		// an activity restart where the system re-delivers the intent).
		// Simply fallback to the "deprecated" `getParcelableExtra(name)`
		// which works in this case, because it doesn't verify the
		// parcelable's class against Scan::class.java.
		null
	}
} else {
	null
} ?: getParcelableExtra(name)
