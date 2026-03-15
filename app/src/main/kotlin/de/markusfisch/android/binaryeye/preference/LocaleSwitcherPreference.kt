package de.markusfisch.android.binaryeye.preference

import android.content.Context
import android.util.AttributeSet
import android.widget.Button
import android.widget.TextView
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder
import de.markusfisch.android.binaryeye.R
import io.tolgee.Tolgee

class LocaleSwitcherPreference(
	context: Context,
	attrs: AttributeSet?
) : Preference(context, attrs) {

	init {
		layoutResource = R.layout.preference_locale_switcher
	}

	override fun onBindViewHolder(holder: PreferenceViewHolder) {
		super.onBindViewHolder(holder)
		val tolgee = Tolgee.instance
		val selectedLocaleView = holder.findViewById(R.id.selected_locale) as TextView
		
		val updateText = {
			selectedLocaleView.text = context.getString(
				R.string.selected_locale,
				tolgee.getLocale().language
			)
		}
		
		updateText()

		holder.findViewById(R.id.btn_en).setOnClickListener {
			tolgee.setLocale("en")
			updateText()
		}
		holder.findViewById(R.id.btn_fr).setOnClickListener {
			tolgee.setLocale("fr")
			updateText()
		}
		holder.findViewById(R.id.btn_cs).setOnClickListener {
			tolgee.setLocale("cs")
			updateText()
		}
	}
}
