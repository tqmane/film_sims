package com.tqmane.filmsim

import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.Window
import android.widget.Button
import android.widget.EditText
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import com.tqmane.filmsim.di.UpdateCheckerWrapper
import com.tqmane.filmsim.ui.MainScreen
import com.tqmane.filmsim.ui.MainViewModel
import com.tqmane.filmsim.util.ReleaseInfo
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Single Activity – acts only as the Compose host and DI entry-point.
 * All UI logic lives in [MainScreen] composable and [MainViewModel].
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val vm: MainViewModel by viewModels()

    @Inject
    lateinit var updateChecker: UpdateCheckerWrapper

    private val pickMedia =
        registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            uri?.let { vm.loadImage(it) }
        }

    // ─── Lifecycle ──────────────────────────────────────

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            MainScreen(
                viewModel = vm,
                onPickImage = { launchPicker() },
                onShowSettings = { showSettingsDialog() },
                onShowUpdateDialog = { showUpdateDialog(it) }
            )
        }

        vm.checkForUpdates()
    }

    // ─── Image picker ───────────────────────────────────

    private fun launchPicker() =
        pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))

    // ─── Settings dialog ────────────────────────────────

    private fun showSettingsDialog() {
        val dialog = Dialog(this, R.style.Theme_FilmSims).apply {
            requestWindowFeature(Window.FEATURE_NO_TITLE)
            setContentView(R.layout.dialog_settings)
            window?.setLayout(
                (resources.displayMetrics.widthPixels * 0.90).toInt(),
                android.view.ViewGroup.LayoutParams.WRAP_CONTENT
            )
            window?.setBackgroundDrawableResource(android.R.color.transparent)
        }
        val tvPath = dialog.findViewById<TextView>(R.id.tvSavePath).apply { text = vm.settings.savePath }
        val tvQuality = dialog.findViewById<TextView>(R.id.tvQualityValue).apply { text = "${vm.settings.saveQuality}%" }
        dialog.findViewById<TextView>(R.id.btnChangePath).setOnClickListener { showPathDialog(tvPath) }
        dialog.findViewById<SeekBar>(R.id.qualitySlider).apply {
            progress = vm.settings.saveQuality
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                    val q = progress.coerceAtLeast(10)
                    tvQuality.text = "${q}%"
                    vm.settings.saveQuality = q
                }
                override fun onStartTrackingTouch(sb: SeekBar?) {}
                override fun onStopTrackingTouch(sb: SeekBar?) {}
            })
        }
        dialog.findViewById<Button>(R.id.btnCloseSettings).setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    private fun showPathDialog(tvPath: TextView) {
        val et = EditText(this).apply {
            setText(vm.settings.savePath)
            setTextColor(Color.WHITE)
            setHintTextColor(Color.GRAY)
            hint = getString(R.string.save_path_hint)
            setPadding(48, 32, 48, 32)
        }
        AlertDialog.Builder(this, R.style.Theme_FilmSims)
            .setTitle(getString(R.string.enter_save_folder))
            .setView(et)
            .setPositiveButton(getString(R.string.save)) { _, _ ->
                val p = et.text.toString().trim()
                if (p.isNotEmpty()) {
                    vm.settings.savePath = p
                    tvPath.text = p
                    Toast.makeText(this, getString(R.string.save_path_changed, p), Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    // ─── Update dialog ──────────────────────────────────

    private fun showUpdateDialog(release: ReleaseInfo) {
        val dialog = Dialog(this, R.style.Theme_FilmSims).apply {
            requestWindowFeature(Window.FEATURE_NO_TITLE)
            setContentView(R.layout.dialog_update)
            window?.setLayout(
                (resources.displayMetrics.widthPixels * 0.90).toInt(),
                android.view.ViewGroup.LayoutParams.WRAP_CONTENT
            )
            window?.setBackgroundDrawableResource(android.R.color.transparent)
        }
        dialog.findViewById<TextView>(R.id.tvVersionInfo).text =
            getString(R.string.new_version_available, release.version)
        dialog.findViewById<TextView>(R.id.tvReleaseNotes).apply {
            if (release.releaseNotes.isNotBlank()) {
                visibility = android.view.View.VISIBLE
                text = release.releaseNotes
            }
        }
        dialog.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnLater)
            .setOnClickListener {
                updateChecker.skipVersion(release.version)
                dialog.dismiss()
            }
        dialog.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnUpdate)
            .setOnClickListener {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(release.htmlUrl)))
                dialog.dismiss()
            }
        dialog.show()
    }
}
