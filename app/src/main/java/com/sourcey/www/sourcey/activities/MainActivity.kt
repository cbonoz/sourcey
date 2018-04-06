package com.sourcey.www.sourcey.activities

import android.os.Bundle
import android.app.Dialog
import android.app.ProgressDialog
import android.content.pm.PackageManager
import com.sourcey.www.sourcey.R

import kotlinx.android.synthetic.main.activity_main.*
import android.widget.Toast
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewTreeObserver
import android.view.animation.DecelerateInterpolator
import android.widget.Button
import android.widget.TextView
import br.tiagohm.codeview.CodeView
import br.tiagohm.codeview.Language
import br.tiagohm.codeview.Theme
import com.github.ajalt.timberkt.Timber.d
import com.github.ajalt.timberkt.Timber.e
import com.github.angads25.filepicker.model.DialogConfigs
import com.github.angads25.filepicker.model.DialogProperties
import com.github.angads25.filepicker.view.FilePickerDialog
import com.sourcey.www.sourcey.SourceyApplication
import com.sourcey.www.sourcey.dialogs.SettingsDialogFragment
import com.sourcey.www.sourcey.util.PrefManager
import com.sourcey.www.sourcey.util.SourceyService
import com.takusemba.spotlight.Spotlight
import java.io.File
import java.nio.charset.Charset
import javax.inject.Inject
import com.takusemba.spotlight.SimpleTarget
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.Job
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch


class MainActivity : AppCompatActivity(), CodeView.OnHighlightListener, ViewTreeObserver.OnGlobalLayoutListener {

    override fun onGlobalLayout() {
        val lastFile = prefManager.getString("lastFile", null)
        if (lastFile == null) {
            addSpotlight()
        }
        mainLayout.viewTreeObserver.removeOnGlobalLayoutListener(this)
    }

    @Inject
    lateinit var prefManager: PrefManager
    @Inject
    lateinit var sourceyService: SourceyService

    private var fileContent: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        SourceyApplication.injectionComponent.inject(this)

        setSupportActionBar(findViewById(R.id.my_toolbar))
        my_toolbar.inflateMenu(R.menu.menu);

        if (prefManager.getBoolean("firstLaunch", true)) {
            mainLayout.viewTreeObserver.addOnGlobalLayoutListener(this)
            prefManager.saveBoolean("firstLaunch", false)
        }

        // If recovering from an event such as a screen rotation.
        if (savedInstanceState != null) {
            val lastFile = savedInstanceState.getString("lastFile", "")
            if (lastFile.isNotEmpty()) {
                loadSourceFile(lastFile)
                updateCodeView(true)
            }
        }

    }

    private fun addSpotlight() {

        val target = SimpleTarget.Builder(this@MainActivity).setPoint(findViewById<View>(R.id.action_load))
                .setRadius(200f)
                .setTitle(getString(R.string.spotlight_title))
                .setDescription(getString(R.string.spotlight_description))
                .build()

        Spotlight.with(this).setOverlayColor(ContextCompat.getColor(this, R.color.md_blue_grey_500))
                .setDuration(1000L)
                .setAnimation(DecelerateInterpolator(2f))
                .setTargets(target)
                .setClosedOnTouchedOutside(true)
                .start();
    }

    private var dialog: FilePickerDialog? = null

    private fun launchFileDialog() {
        d { "launchFileDialog" }

        val properties = DialogProperties();
        properties.selection_mode = DialogConfigs.SINGLE_MODE;
        properties.selection_type = DialogConfigs.FILE_SELECT;
        properties.root = File(DialogConfigs.DEFAULT_DIR);
        properties.error_dir = File(DialogConfigs.DEFAULT_DIR);
        properties.offset = File(DialogConfigs.DEFAULT_DIR);
        properties.extensions = null;

        dialog = FilePickerDialog(this, properties);

        dialog!!.setTitle(getString(R.string.file_dialog_title));
        dialog!!.setDialogSelectionListener({
            d { "selected: " + it.toString() }
            if (it.isNotEmpty()) {
                val path = it.get(0)
                loadSourceFile(path)
            }
        });
        dialog!!.show()
    }

    private fun loadSourceFile(path: String) {
        loadSourceFile(File(path))
    }

    private var job: Job? = null

    private fun loadSourceFile(pathFile: File) {
        d { "loadSourceFile ${pathFile}" }
        noFileText.visibility = View.GONE
        val message: String
        if (fileContent.length > SourceyService.LARGE_FILE_THRESHOLD) {
            message = getString(R.string.loading_file_large)
        } else {
            message = getString(R.string.loading_file)
        }

        mProgressDialog = ProgressDialog.show(this, null, message)
        job = launch(CommonPool) {
            try {
                fileContent = pathFile.bufferedReader(Charset.defaultCharset()).use {
                    it.readText()
                }
                d { "read fileContent: ${fileContent.length} bytes" }
            } catch (e: Exception) {
                launch(UI) {
                    mProgressDialog?.dismiss()
                    val errorMessage = "Problem Reading file. ${e.localizedMessage}"
                    e { errorMessage }
                    Toast.makeText(this@MainActivity, errorMessage, Toast.LENGTH_SHORT).show()
                }
            }

            prefManager.saveString("lastFile", pathFile.absolutePath)
            launch(UI) {
                updateCodeView(true)
            }

        }
    }

    fun updateCodeView(refreshCode: Boolean) {
        if (fileContent.isNotEmpty()) {
            noFileText.visibility = View.GONE
            codeView.visibility = View.VISIBLE

            val settings = sourceyService.getSettings()
            val language: Language?
            if (settings.languageDetection) {
                language = Language.AUTO
            } else {
                language = Language.VIM
            }

            val themes = sourceyService.getThemes()

            val theme: Theme
            if (settings.themeIndex < themes.size) {
                theme = themes.get(settings.themeIndex)
            } else {
                theme = Theme.AGATE
            }

            if (refreshCode) {
                codeView.setCode(fileContent)
            }

            codeView.setOnHighlightListener(this)
                    .setTheme(theme)
                    .setLanguage(language)
                    .setWrapLine(settings.wrapLine)
                    .setFontSize(settings.fontSize)
                    .setShowLineNumber(settings.lineNumber)
                    .setZoomEnabled(settings.zoomEnabled)
                    .apply();
        } else {
            noFileText.visibility = View.VISIBLE
            codeView.visibility = View.GONE
        }

    }

    override fun onDestroy() {
        super.onDestroy()
        job?.cancel()
    }

    private var mProgressDialog: ProgressDialog? = null

    /*
     * Codeview methods below.
     * https://github.com/tiagohm/CodeView
     */

    override fun onStartCodeHighlight() {
        d { "startCodeHighlight " }
        val message: String
        if (fileContent.length > SourceyService.LARGE_FILE_THRESHOLD) {
            message = getString(R.string.applying_syntax_large)
        } else {
            message = getString(R.string.applying_syntax)
        }

        mProgressDialog?.dismiss()
        mProgressDialog = ProgressDialog.show(this, null, message, true);
    }

    override fun onLanguageDetected(language: Language?, relevance: Int) {
        val languageString = "Detected language: " + language + " relevance: " + relevance
        d { languageString }
        Toast.makeText(this, languageString, Toast.LENGTH_SHORT).show();

        if (sourceyService.getSettings().languageDetection) {
            codeView.setLanguage(language)
            launch(UI) {
                codeView.apply()
            }
        }
    }

    override fun onFontSizeChanged(sizeInPx: Int) {
        d { "font-size: " + sizeInPx + "px" }
    }

    override fun onLineClicked(lineNumber: Int, content: String) {
        d { "line: " + lineNumber + " html: " + content }
    }

    override fun onFinishCodeHighlight() {
        d { "finishCodeHighlight " }
        mProgressDialog?.dismiss();
        mProgressDialog = null
    }

/*
 * File permission request below.
 */

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            FilePickerDialog.EXTERNAL_READ_PERMISSION_GRANT -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    dialog?.show()
                } else {
                    //Permission has not been granted. Notify the user.
                    Toast.makeText(this@MainActivity, getString(R.string.permission_required), Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu, menu)
        return true
    }


    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        R.id.action_settings -> {
            val newFragment = SettingsDialogFragment()
            newFragment.show(supportFragmentManager, "settings")
            true
        }
        R.id.action_load -> {
            launchFileDialog()
            true
        }
        R.id.action_info -> {
            launchInfoDialog()
            true
        }

        else -> {
            // If we got here, the user's action was not recognized.
            // Invoke the superclass to handle it.
            super.onOptionsItemSelected(item)
        }
    }

    private fun launchInfoDialog() {
        val dialog = Dialog(this); // Context, this, etc.
        dialog.setContentView(R.layout.dialog_info);
        dialog.setTitle(getString(R.string.about));
        dialog.show();
        dialog.findViewById<Button>(R.id.infoButton).setOnClickListener {
            dialog.dismiss()
        }

        val lastFile = prefManager.getString("lastFile", null)
        if (lastFile != null) {
            val infoString = StringBuilder().append("File: ${lastFile}").append("\n")
            if (codeView.lineCount > 0) {
                infoString.append("Lines: ${codeView.lineCount}").append("\n")
            }
            if (codeView.language != null) {
                infoString.append("Language Detected: ${codeView.language}").append("\n")
            }
            val fileInfoText = dialog.findViewById<TextView>(R.id.fileInfoText)
            fileInfoText.setText(infoString.toString())
        } else {
            val lastFileLoadedHeader = dialog.findViewById<TextView>(R.id.lastFileLoadedHeader)
            lastFileLoadedHeader.visibility = View.GONE
        }

    }

    public override fun onSaveInstanceState(savedInstanceState: Bundle) {
        super.onSaveInstanceState(savedInstanceState)
        savedInstanceState.putString("lastFile", prefManager.getString("lastFile", ""))
    }

}
