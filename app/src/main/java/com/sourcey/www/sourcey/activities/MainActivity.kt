package com.sourcey.www.sourcey.activities

import android.os.Bundle
import android.app.Activity
import android.app.ProgressDialog
import android.content.pm.PackageManager
import com.sourcey.www.sourcey.R

import kotlinx.android.synthetic.main.activity_main.*
import android.widget.Toast
import android.support.v4.content.ContextCompat
import android.view.View
import android.view.ViewTreeObserver
import android.view.animation.DecelerateInterpolator
import br.tiagohm.codeview.CodeView
import br.tiagohm.codeview.Language
import br.tiagohm.codeview.Theme
import com.github.ajalt.timberkt.Timber.d
import com.github.ajalt.timberkt.Timber.e
import com.github.angads25.filepicker.model.DialogConfigs
import com.github.angads25.filepicker.model.DialogProperties
import com.github.angads25.filepicker.view.FilePickerDialog
import com.sourcey.www.sourcey.SourceyApplication
import com.sourcey.www.sourcey.util.PrefManager
import com.takusemba.spotlight.Spotlight
import java.io.File
import java.nio.charset.Charset
import javax.inject.Inject
import com.takusemba.spotlight.SimpleTarget
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch


class MainActivity : Activity(), CodeView.OnHighlightListener, ViewTreeObserver.OnGlobalLayoutListener {

    override fun onGlobalLayout() {
        val lastFile = prefManager.getString("lastFile", null)
        if (lastFile == null) {
            addSpotlight()
        }
        mainLayout.viewTreeObserver.removeOnGlobalLayoutListener(this)
    }

    @Inject
    lateinit var prefManager: PrefManager

    private var fileContent: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        SourceyApplication.injectionComponent.inject(this)

        val lastFile = prefManager.getString("lastFile", null)
        if (lastFile != null) {
            loadSourceFile(lastFile)
        } else {
            updateCodeView()
        }

        fab.setOnClickListener {
            launchFileDialog()
        }

        if (prefManager.getBoolean("firstLaunch", true)) {
            mainLayout.viewTreeObserver.addOnGlobalLayoutListener(this)
            prefManager.saveBoolean("firstLaunch", false)
        }
    }

    private fun addSpotlight() {

        val target = SimpleTarget.Builder(this@MainActivity).setPoint(fab)
                .setRadius(200f)
                .setTitle(getString(R.string.spotlight_title))
                .setDescription(getString(R.string.spotlight_description))
                .build()

        Spotlight.with(this).setOverlayColor(ContextCompat.getColor(this, R.color.material_grey_900))
                .setDuration(2000L)
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

    private fun loadSourceFile(pathFile: File) {
        d { "loadSourceFile ${pathFile}" }
        val job = launch(CommonPool) {
            try {
                fileContent = pathFile.bufferedReader(Charset.defaultCharset()).use {
                    it.readText()
                }
                d { "read fileContent: ${fileContent}" }
            } catch (e: Exception) {
                launch(UI) {
                    val errorMessage = "Problem Reading file. ${e.localizedMessage}"
                    e { errorMessage }
                    Toast.makeText(this@MainActivity, errorMessage, Toast.LENGTH_SHORT).show()
                }
            }

            prefManager.saveString("lastFile", pathFile.absolutePath)
            launch(UI) {
                updateCodeView()
            }

        }
    }

    private fun updateCodeView() {
        if (fileContent.isNotEmpty()) {
            noFileText.visibility = View.GONE
            codeView.visibility = View.VISIBLE

            codeView.setOnHighlightListener(this)
                    .setTheme(Theme.AGATE)
                    .setCode(fileContent)
                    .setLanguage(Language.JAVA)
                    .setWrapLine(true)
                    .setFontSize(14F)
                    .setZoomEnabled(true)
                    .setShowLineNumber(true)
                    .setWrapLine(true)
                    .apply();
        } else {
            noFileText.visibility = View.VISIBLE
            codeView.visibility = View.VISIBLE
        }

    }

    private var mProgressDialog: ProgressDialog? = null

    /*
     * Codeview methods below.
     * https://github.com/tiagohm/CodeView
     */

    override fun onStartCodeHighlight() {
        d { "startCodeHighlight " }
        mProgressDialog = ProgressDialog.show(this, null, getString(R.string.loading), true);
    }

    override fun onLanguageDetected(language: Language?, relevance: Int) {
        val languageString = "onLanguageDetected: language: " + language + " relevance: " + relevance
        d { languageString }
        Toast.makeText(this, languageString, Toast.LENGTH_SHORT).show();
        codeView.setLanguage(language)
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

}
