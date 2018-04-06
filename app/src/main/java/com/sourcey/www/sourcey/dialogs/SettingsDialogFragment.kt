package com.sourcey.www.sourcey.dialogs


import android.app.Dialog
import android.os.Bundle
import android.support.v4.app.DialogFragment

import com.sourcey.www.sourcey.R
import android.support.v7.app.AlertDialog
import android.support.v7.widget.SwitchCompat
import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import android.widget.Toast
import com.sourcey.www.sourcey.SourceyApplication
import com.sourcey.www.sourcey.activities.MainActivity
import com.sourcey.www.sourcey.util.Settings
import com.sourcey.www.sourcey.util.SourceyService
import javax.inject.Inject
import android.widget.TextView
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
        val fontSizeText = view.findViewById<EditText>(R.id.fontSizeText)
        val lineSwitch = view.findViewById<SwitchCompat>(R.id.lineSwitch)
        val wrapSwitch = view.findViewById<SwitchCompat>(R.id.wrapSwitch)
        val zoomSwitch = view.findViewById<SwitchCompat>(R.id.zoomSwitch)
        val languageSwitch = view.findViewById<SwitchCompat>(R.id.languageSwitch)
        val themeSpinner = view.findViewById<NiceSpinner>(R.id.themeSpinner)

        val oldSettings = sourceyService.getSettings()

        fontSizeText.addTextChangedListener(object : TextValidator(fontSizeText) {
            override fun validate(textView: TextView, text: String) {
                /* Validation code here */
                val value = text.toFloatOrNull()
                if (value != null && (value < 0 || value > 32)) {
                    Toast.makeText(activity, "Size should be between 1 and 32", Toast.LENGTH_SHORT).show()
                    fontSizeText.setText(oldSettings.fontSize.toString())
                }
            }
        });

        fontSizeText.setText(oldSettings.fontSize.toString())
        lineSwitch.isChecked = oldSettings.lineNumber
        zoomSwitch.isChecked = oldSettings.zoomEnabled
        wrapSwitch.isChecked = oldSettings.wrapLine
        languageSwitch.isChecked = oldSettings.languageDetection
        themeSpinner.attachDataSource(sourceyService.getThemeNames())
        themeSpinner.selectedIndex = oldSettings.themeIndex
        themeSpinner.dropDownListPaddingBottom = 5

        builder.setView(view).setPositiveButton(getString(R.string.save), { dialog, id ->

            val fontText = fontSizeText.text.toString().toFloatOrNull()

            val fontSize: Float
            if (fontText != null && fontText > 0) {
                fontSize = fontText
            } else {
                fontSize = oldSettings.fontSize
            }
            val settings = Settings(
                    wrapSwitch.isChecked,
                    lineSwitch.isChecked,
                    zoomSwitch.isChecked,
                    languageSwitch.isChecked,
                    themeSpinner.selectedIndex,
                    fontSize
            )

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
