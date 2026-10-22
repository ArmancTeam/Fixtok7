package com.tiktok.tv

import android.os.Bundle
import android.app.AlertDialog
import android.os.Build
import android.webkit.WebView
import androidx.activity.result.contract.ActivityResultContracts
import android.widget.ArrayAdapter
import android.widget.SeekBar
import android.webkit.CookieManager
import androidx.appcompat.app.AppCompatActivity
import com.tiktok.tv.databinding.ActivitySettingsBinding

/**
 * TV-friendly настройки. Всё управляется D-pad: элементы focusable,
 * видимое focused-состояние через @drawable/focusable_bg.
 *
 * RESULT_OK возвращается в MainActivity, если сессия изменилась
 * (вход выполнен или logout) — тогда лента перезагружается.
 */
class SettingsActivity : AppCompatActivity() {

    companion object {
        const val PREFS_NAME = "fixtok_settings"
        const val KEY_BASS = "bass_enabled"
        const val KEY_MOUSE_SPEED = "mouse_speed"
        const val KEY_CURSOR_COLOR = "cursor_color"
        const val KEY_CURSOR_SHAPE = "cursor_shape"
        const val KEY_MOUSE_ENABLED = "mouse_enabled"
        const val KEY_CLEAN_SCREEN = "clean_screen"
        const val KEY_AMOLED = "amoled_enabled"
        const val KEY_CURSOR_SIZE = "cursor_size"
    }

    private lateinit var binding: ActivitySettingsBinding

    /** Giriş tamamlanırsa (yeni oturum) sonucu MainActivity'ye ilet: akış yenilensin. */
    private val loginLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) setResult(RESULT_OK)
            updateAccountStatus()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)

        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        window.statusBarColor = android.graphics.Color.BLACK
        window.navigationBarColor = android.graphics.Color.BLACK

        // ── Bass boost ────────────────────────────────────────────────

        binding.settingsBass.isChecked =
            prefs.getBoolean(KEY_BASS, true)

        binding.settingsBass.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean(KEY_BASS, isChecked).apply()
        }

        // ── AMOLED mode ──────────────────────────────────────────────
        binding.settingsAmoled.isChecked = prefs.getBoolean(KEY_AMOLED, false)
        binding.root.setBackgroundColor(
            if (prefs.getBoolean(KEY_AMOLED, false)) {
                android.graphics.Color.BLACK
            } else {
                getColor(com.tiktok.tv.R.color.fixtok_background)
            }
        )
        binding.settingsAmoled.setOnCheckedChangeListener { _, checked ->
            prefs.edit().putBoolean(KEY_AMOLED, checked).apply()
            binding.root.setBackgroundColor(
                if (checked) android.graphics.Color.BLACK
                else getColor(com.tiktok.tv.R.color.fixtok_background)
            )
        }

        // ── D-pad virtual mouse ───────────────────────────────────────
        binding.settingsMouseSpeed.progress = (prefs.getInt(KEY_MOUSE_SPEED, 5) - 1).coerceIn(0, 9)
        updateMouseSpeedLabel(binding.settingsMouseSpeed.progress + 1)
        binding.settingsMouseSpeed.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val speed = progress + 1
                prefs.edit().putInt(KEY_MOUSE_SPEED, speed).apply()
                updateMouseSpeedLabel(speed)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) = Unit
            override fun onStopTrackingTouch(seekBar: SeekBar?) = Unit
        })

        binding.settingsCursorSize.progress = (prefs.getInt(KEY_CURSOR_SIZE, 32) - 16).coerceIn(0, 48)
        binding.settingsCursorSizeLabel.text = getString(R.string.settings_cursor_size_label, binding.settingsCursorSize.progress + 16)
        binding.settingsCursorSize.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val size = progress + 16
                prefs.edit().putInt(KEY_CURSOR_SIZE, size).apply()
                binding.settingsCursorSizeLabel.text = getString(R.string.settings_cursor_size_label, size)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) = Unit
            override fun onStopTrackingTouch(seekBar: SeekBar?) = Unit
        })

        // Saklanan anahtarlar sabit (İngilizce); ekranda gösterilen etiketler yerelleştirilmiş.
        val colorKeys = VirtualCursorView.COLOR_KEYS
        val colorLabels = resources.getStringArray(R.array.cursor_color_labels).toList()
        binding.settingsCursorColor.adapter = ArrayAdapter(
            this, android.R.layout.simple_spinner_item, colorLabels
        ).also { it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
        binding.settingsCursorColor.setSelection(
            colorKeys.indexOf(prefs.getString(KEY_CURSOR_COLOR, VirtualCursorView.DEFAULT_COLOR)).coerceAtLeast(0)
        )
        binding.settingsCursorColor.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                prefs.edit().putString(KEY_CURSOR_COLOR, colorKeys[position]).apply()
                binding.settingsCursorPreview.applyPrefs(prefs, resize = false)
            }
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) = Unit
        }

        val shapeKeys = VirtualCursorView.SHAPE_KEYS
        val shapeLabels = resources.getStringArray(R.array.cursor_shape_labels).toList()
        binding.settingsCursorShape.adapter = ArrayAdapter(
            this, android.R.layout.simple_spinner_item, shapeLabels
        ).also { it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
        binding.settingsCursorShape.setSelection(
            shapeKeys.indexOf(prefs.getString(KEY_CURSOR_SHAPE, VirtualCursorView.DEFAULT_SHAPE)).coerceAtLeast(0)
        )
        binding.settingsCursorShape.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                prefs.edit().putString(KEY_CURSOR_SHAPE, shapeKeys[position]).apply()
                binding.settingsCursorPreview.applyPrefs(prefs, resize = false)
            }
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) = Unit
        }

        // Canlı önizleme: kaydedilmiş ayarlarla başla.
        binding.settingsCursorPreview.applyPrefs(prefs, resize = false)

        binding.settingsMouseEnabled.isChecked = prefs.getBoolean(KEY_MOUSE_ENABLED, false)
        binding.settingsMouseEnabled.setOnCheckedChangeListener { _, checked -> prefs.edit().putBoolean(KEY_MOUSE_ENABLED, checked).apply() }
        binding.settingsCleanScreen.isChecked = prefs.getBoolean(KEY_CLEAN_SCREEN, false)
        binding.settingsCleanScreen.setOnCheckedChangeListener { _, checked -> prefs.edit().putBoolean(KEY_CLEAN_SCREEN, checked).apply() }

        // ── Аккаунт ───────────────────────────────────────────────────

        updateAccountStatus()

        binding.settingsLogin.setOnClickListener {
            loginLauncher.launch(android.content.Intent(this, LoginActivity::class.java))
        }

        binding.settingsLogout.setOnClickListener {
            logout()
        }

        binding.settingsDone.setOnClickListener {
            finish()
        }

        binding.settingsDiagnostics.setOnClickListener { showDiagnostics() }
        binding.settingsCheckUpdates.setOnClickListener { checkForUpdates() }
    }

    private fun checkForUpdates() {
        if (!GitHubUpdateChecker.isConfigured()) {
            showUpdateDialog(getString(R.string.settings_update_not_configured))
            return
        }
        GitHubUpdateChecker.check(this) { result, error ->
            if (isFinishing || isDestroyed) return@check
            val message = when {
                error != null -> getString(R.string.settings_update_error, error)
                result == null -> getString(R.string.settings_update_error, "?")
                result.isNewer -> getString(R.string.settings_update_available, result.latestTag, result.releaseUrl)
                else -> getString(R.string.settings_update_latest, result.latestTag)
            }
            showUpdateDialog(message)
        }
    }

    private fun showUpdateDialog(message: String) {
        AlertDialog.Builder(this)
            .setTitle(R.string.settings_update_title)
            .setMessage(message)
            .setPositiveButton(android.R.string.ok, null)
            .show()
    }

    private fun showDiagnostics() {
        val version = try {
            val info = packageManager.getPackageInfo(packageName, 0)
            val versionCode = if (Build.VERSION.SDK_INT >= 28) info.longVersionCode else info.versionCode.toLong()
            "${info.versionName ?: "?"} ($versionCode)"
        } catch (_: Exception) { "?" }
        val webViewVersion = if (Build.VERSION.SDK_INT >= 26) {
            WebView.getCurrentWebViewPackage()?.versionName ?: "?"
        } else "Android System WebView"
        val details = getString(
            R.string.settings_diagnostics_body,
            version, packageName, Build.VERSION.RELEASE, Build.VERSION.SDK_INT,
            "${Build.MANUFACTURER} ${Build.MODEL}", webViewVersion
        )
        AlertDialog.Builder(this)
            .setTitle(R.string.settings_diagnostics_title)
            .setMessage(details)
            .setPositiveButton(android.R.string.ok, null)
            .show()
    }

    private fun updateMouseSpeedLabel(speed: Int) {
        binding.settingsMouseSpeedLabel.text = getString(R.string.settings_mouse_speed_label, speed)
    }

    private fun updateAccountStatus() {
        val loggedIn =
            LoginActivity.hasSessionCookie(CookieManager.getInstance())
        binding.settingsAccountStatus.text = getString(
            if (loggedIn) R.string.account_status_logged_in
            else R.string.account_status_logged_out
        )
    }

    /**
     * Logout: TikTok oturum çerezleri gerçekten silinir (süresi geçirilir ve flush edilir),
     * arayüz güncellenir, sonuç MainActivity'ye döner ve akış oturumsuz yenilenir.
     */
    private fun logout() {
        LoginActivity.clearSession(CookieManager.getInstance())
        setResult(RESULT_OK)
        updateAccountStatus()
    }

    override fun onResume() {
        super.onResume()
        // Вернулся из LoginActivity — обновить статус.
        updateAccountStatus()
    }
}
