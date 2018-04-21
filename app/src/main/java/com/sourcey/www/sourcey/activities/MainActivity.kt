package com.sourcey.www.sourcey.activities

import android.os.Bundle
import android.app.Dialog
import android.app.ProgressDialog
import android.content.pm.PackageManager
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
import com.github.ajalt.timberkt.Timber.d
import com.github.ajalt.timberkt.Timber.e
import com.github.angads25.filepicker.model.DialogConfigs
import com.github.angads25.filepicker.model.DialogProperties
import com.github.angads25.filepicker.view.FilePickerDialog
import com.sourcey.www.sourcey.R.string.settings
import com.sourcey.www.sourcey.SourceyApplication
import com.sourcey.www.sourcey.dialogs.SettingsDialogFragment
import com.sourcey.www.sourcey.util.PrefManager
import com.sourcey.www.sourcey.util.SourceyService
import com.takusemba.spotlight.Spotlight
import java.io.File
import java.nio.charset.Charset
import javax.inject.Inject
import com.takusemba.spotlight.SimpleTarget
import io.github.kbiakov.codeview.adapters.Options
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.Job
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch


class MainActivity : AppCompatActivity(), ViewTreeObserver.OnGlobalLayoutListener {

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

        if (prefManager.getBoolean(SourceyService.FIRST_LAUNCH, true)) {
            mainLayout.viewTreeObserver.addOnGlobalLayoutListener(this)
            prefManager.saveBoolean(SourceyService.FIRST_LAUNCH, false)
        }

        fileContent = "" +
                "package io.github.kbiakov.codeviewexample;\n" +
                "\n" +
                "import android.os.Bundle;\n" +
                "import android.support.annotation.Nullable;\n" +
                "import android.support.v7.app.AppCompatActivity;\n" +
                "import android.util.Log;\n" +
                "\n" +
                "import org.jetbrains.annotations.NotNull;\n" +
                "\n" +
                "import io.github.kbiakov.codeview.CodeView;\n" +
                "import io.github.kbiakov.codeview.OnCodeLineClickListener;\n" +
                "import io.github.kbiakov.codeview.adapters.CodeWithDiffsAdapter;\n" +
                "import io.github.kbiakov.codeview.adapters.Options;\n" +
                "import io.github.kbiakov.codeview.highlight.ColorTheme;\n" +
                "import io.github.kbiakov.codeview.highlight.ColorThemeData;\n" +
                "import io.github.kbiakov.codeview.highlight.Font;\n" +
                "import io.github.kbiakov.codeview.highlight.FontCache;\n" +
                "import io.github.kbiakov.codeview.views.DiffModel;\n" +
                "\n" +
                "public class ListingsActivity extends AppCompatActivity {\n" +
                "\n" +
                "    @Override\n" +
                "    protected void onCreate(@Nullable Bundle savedInstanceState) {\n" +
                "        super.onCreate(savedInstanceState);\n" +
                "        setContentView(R.layout.activity_listings);\n" +
                "\n" +
                "        final CodeView codeView = (CodeView) findViewById(R.id.code_view);\n" +
                "\n" +
                "        /*\n" +
                "         * 1: set code content\n" +
                "         */\n" +
                "\n" +
                "        // auto language recognition\n" +
                "        codeView.setCode(getString(R.string.listing_js));\n" +
                "\n" +
                "        // specify language for code listing\n" +
                "        codeView.setCode(getString(R.string.listing_py), \"py\");" +
                "    }\n" +
                "}"

        // If recovering from an event such as a screen rotation.
        if (savedInstanceState != null) {
            val lastFile = savedInstanceState.getString(SourceyService.LAST_FILE, "")
            if (lastFile.isNotEmpty()) {
                loadSourceFile(lastFile)
                updateCodeView(true)
            }
        }
        updateCodeView(true)

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
        if (pathFile.length() > SourceyService.LARGE_FILE_THRESHOLD) {
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
                mProgressDialog?.dismiss()
                updateCodeView(true)
            }

        }
    }

    private var showedFormatComplete: Boolean = false

    fun updateCodeView(refreshCode: Boolean) {
        if (fileContent.isNotEmpty()) {
            noFileText.visibility = View.GONE
            codeView.visibility = View.VISIBLE

            val settings = sourceyService.getSettings()

            val options = Options.Default.get(this)
            options.setTheme(sourceyService.getThemes().get(settings.themeIndex))
            options.setFont(sourceyService.getFonts().get(settings.fontIndex))

            codeView.setOptions(options)

            if (refreshCode) {
                if (settings.languageDetection) {
                    codeView.setCode(fileContent) // use auto detection.
                } else {
                    codeView.setCode(fileContent, "java")
                }
                showedFormatComplete = false
            }

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
