package com.sourcey.www.sourcey.dialogs


import android.app.Dialog
import android.os.Bundle
import android.support.v4.app.DialogFragment

import com.sourcey.www.sourcey.R
import android.support.v7.app.AlertDialog
import android.support.v7.widget.SwitchCompat
import android.text.Editable
import android.text.TextWatcher
import android.widget.Toast
import com.sourcey.www.sourcey.SourceyApplication
import com.sourcey.www.sourcey.activities.MainActivity
import com.sourcey.www.sourcey.util.Settings
import com.sourcey.www.sourcey.util.SourceyService
import javax.inject.Inject
import android.widget.TextView
import kotlinx.android.synthetic.main.dialog_settings.*
import org.angmarch.views.NiceSpinner


class SettingsDialogFragment : DialogFragment() {

    @Inject
    lateinit var sourceyService: SourceyService

    private lateinit var settings: Settings

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        SourceyApplication.injectionComponent.inject(this)

        settings = sourceyService.getSettings()
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = AlertDialog.Builder(this.activity!!)
        val inflater = activity!!.layoutInflater

        val view = inflater.inflate(R.layout.dialog_settings, null)
        val languageSwitch = view.findViewById<SwitchCompat>(R.id.languageSwitch)
        val themeSpinner = view.findViewById<NiceSpinner>(R.id.themeSpinner)
        val fontSpinner = view.findViewById<NiceSpinner>(R.id.fontSpinner)

        val oldSettings = sourceyService.getSettings()

        languageSwitch.isChecked = oldSettings.languageDetection
        fontSpinner.attachDataSource(sourceyService.getFontNames())
        fontSpinner.selectedIndex = oldSettings.fontIndex
        fontSpinner.dropDownListPaddingBottom = 7
        themeSpinner.attachDataSource(sourceyService.getThemeNames())
        themeSpinner.selectedIndex = oldSettings.themeIndex
        themeSpinner.dropDownListPaddingBottom = 7

        builder.setView(view).setPositiveButton(getString(R.string.save), { dialog, id ->
            val settings = Settings(languageSwitch.isChecked, fontSpinner.selectedIndex, themeSpinner.selectedIndex)
            sourceyService.saveSettings(settings)
            Toast.makeText(activity, getString(R.string.saved_settings), Toast.LENGTH_SHORT).show()

            if (activity is MainActivity) {
                (activity as MainActivity).updateCodeView(false)
            }
        })
                .setNegativeButton(getString(R.string.cancel), { dialog, id -> this@SettingsDialogFragment.getDialog().cancel() })

        return builder.create()
    }

    abstract inner class TextValidator(private val textView: TextView) : TextWatcher {

        abstract fun validate(textView: TextView, text: String)

        override fun afterTextChanged(s: Editable) {
            val text = textView.text.toString()
            validate(textView, text)
        }

        override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) { /* Don't care */
        }

        override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) { /* Don't care */
        }
    }

}
