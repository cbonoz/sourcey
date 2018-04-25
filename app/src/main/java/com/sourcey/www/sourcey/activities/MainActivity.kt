package com.sourcey.www.sourcey.activities

import android.os.Bundle
import android.app.Dialog
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Handler
import android.support.design.widget.Snackbar
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
import com.github.ybq.android.spinkit.SpinKitView
import com.sourcey.www.sourcey.R.id.*
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

        loadingSpinner = findViewById<SpinKitView>(R.id.loadingSpinner)

        setSupportActionBar(findViewById(R.id.my_toolbar))
        my_toolbar.inflateMenu(R.menu.menu);

        if (prefManager.getBoolean(SourceyService.FIRST_LAUNCH, true)) {
            mainLayout.viewTreeObserver.addOnGlobalLayoutListener(this)
            prefManager.saveBoolean(SourceyService.FIRST_LAUNCH, false)
        }

        // If recovering from an event such as a screen rotation.
        if (savedInstanceState != null) {
            val lastFile = savedInstanceState.getString(SourceyService.LAST_FILE, "")
            if (lastFile.isNotEmpty()) {
                loadSourceFile(lastFile)
            }
        } else {
            showNoFileView()
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
    private lateinit var loadingSpinner: SpinKitView

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

    private fun loadSourceFile(pathFile: File, testFile: Boolean = false) {
        d { "loadSourceFile ${pathFile}" }
        showLoadingView()
        // Cancel a pre-existing job if it is active.
        job?.cancel()
        job = launch(CommonPool) {
            try {
                if (testFile) {
                    val in_s = resources.openRawResource(R.raw.mergesort)
                    val b = ByteArray(in_s.available())
                    in_s.read(b)
                    fileContent = String(b)
                } else {
                    fileContent = pathFile.bufferedReader(Charset.defaultCharset()).use {
                        it.readText()
                    }
                    prefManager.saveString("lastFile", pathFile.absolutePath)
                }
                d { "read fileContent: ${fileContent.length} bytes" }

                launch(UI) {
                    updateCodeView(true)
                }

            } catch (e: Exception) {
                launch(UI) {
                    showNoFileView()
                    val errorMessage = "Problem Reading file. ${e.localizedMessage}"
                    e { errorMessage }
                    Toast.makeText(this@MainActivity, errorMessage, Toast.LENGTH_SHORT).show()
                }
            }

        }
    }

    fun updateCodeView(refreshCode: Boolean) {
        if (fileContent.isNotEmpty()) {
            val settings = sourceyService.getSettings()
            val language: Language?
            if (settings.languageDetection) {
                language = Language.AUTO
            } else {
                language = settings.language
            }

            val themes = sourceyService.getThemes()

            val theme: Theme
            if (settings.themeIndex < themes.size) {
                theme = themes.get(settings.themeIndex)
            } else {
                theme = Theme.AGATE
            }

            codeView.setOnHighlightListener(this)
                    .setTheme(theme)
                    .setLanguage(language)
                    .setWrapLine(settings.wrapLine)
                    .setFontSize(settings.fontSize)
                    .setShowLineNumber(settings.lineNumber)
                    .setZoomEnabled(settings.zoomEnabled)

            if (refreshCode) {
                codeView.setCode(fileContent);
            }

            codeView.apply();

        } else {
            showNoFileView()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        job?.cancel()
    }

    private fun showCodeView() {
        launch(UI) {
            noFileText.visibility = View.GONE
            codeView.visibility = View.VISIBLE
            loadingSpinner.visibility = View.GONE
        }
    }

    private fun showNoFileView() {
        noFileText.visibility = View.VISIBLE
        codeView.visibility = View.GONE
        loadingSpinner.visibility = View.GONE
    }

    private fun showLoadingView() {
        noFileText.visibility = View.GONE
        codeView.visibility = View.INVISIBLE
        loadingSpinner.visibility = View.VISIBLE
    }

    private val handler: Handler = Handler()
    private val longFileNotification = {
        showSnackbar(getString(R.string.large_file_format_takes_longer))
    }

    /*
     * Codeview methods below.
     * https://github.com/tiagohm/CodeView
     */
    override fun onStartCodeHighlight() {
        handler.postDelayed(longFileNotification, 8000)
        d { "startCodeHighlight " }
        if (fileContent.length > SourceyService.LARGE_FILE_THRESHOLD) {
            showSnackbar(getString(R.string.applying_syntax_large))
        }
    }

    override fun onLanguageDetected(language: Language?, relevance: Int) {
        showCodeView()
        val languageString = "Detected language: ${language}"
        d { languageString }
        showSnackbar(languageString)

        if (sourceyService.getSettings().languageDetection) {
            launch(UI) {
                codeView.setLanguage(language).apply()
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
        handler.removeCallbacks(longFileNotification)
        d { "finishCodeHighlight " }
        showCodeView()
        showSnackbar(getString(R.string.applying_formatting_done))
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
        dialog.findViewById<Button>(R.id.closeButton).setOnClickListener {
            dialog.dismiss()
        }

        val loadSampleButton = dialog.findViewById<Button>(R.id.loadSampleButton)
        loadSampleButton.setOnClickListener {
            loadSourceFile(File(""), true)
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
        savedInstanceState.putString(SourceyService.LAST_FILE, prefManager.getString("lastFile", ""))
    }

    private fun showSnackbar(message: String) {
        val snackbar = Snackbar.make(mainLayout, message, Snackbar.LENGTH_SHORT);
        snackbar.show()
    }
}
