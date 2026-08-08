package com.slanotifier.app

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.webkit.URLUtil
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class UrlSettingsActivity : AppCompatActivity() {

    companion object {
        private const val PREFS_NAME = "sla_app_settings"
        private const val KEY_SYSTEM_URL = "system_url"
        public const val DEFAULT_URL = "https://admin-14.hoteleshopdemo.com/"

        fun getSavedUrl(context: Context): String {
            val saved = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getString(KEY_SYSTEM_URL, null)
            return if (!saved.isNullOrBlank()) saved else DEFAULT_URL
        }

        fun saveUrl(context: Context, url: String) {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_SYSTEM_URL, url)
                .apply()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.dialog_url_settings)

        val etUrlInput = findViewById<EditText>(R.id.etUrlInput)
        val btnSaveUrl = findViewById<Button>(R.id.btnSaveUrl)
        val btnCancelUrl = findViewById<Button>(R.id.btnCancelUrl)
        val btnTestNotification = findViewById<Button>(R.id.btnTestNotification)
        val btnRegisterDeviceDb = findViewById<Button>(R.id.btnRegisterDeviceDb)

        val currentUrl = getSavedUrl(this)
        if (!currentUrl.isNullOrBlank()) {
            etUrlInput.setText(currentUrl)
        }

        btnTestNotification.setOnClickListener {
            NotificationHelper.triggerSlaNotification(
                this,
                "TEST_" + (1000..9999).random(),
                "E Shop Test Notification",
                "Your E Shop Phone Notification & Ringtone are working 100% perfectly!"
            )
            Toast.makeText(this, "🔔 Test Notification Dispatched!", Toast.LENGTH_SHORT).show()
        }

        btnRegisterDeviceDb.setOnClickListener {
            val resultIntent = Intent().apply {
                putExtra("REGISTER_DEVICE_DB", true)
            }
            setResult(Activity.RESULT_OK, resultIntent)
            Toast.makeText(this, "📱 Registering Device Token in Database...", Toast.LENGTH_SHORT).show()
            finish()
        }

        btnSaveUrl.setOnClickListener {
            var input = etUrlInput.text.toString().trim()
            if (input.isBlank()) {
                Toast.makeText(this, "URL cannot be empty", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (!input.startsWith("http://") && !input.startsWith("https://")) {
                input = "https://$input"
            }

            if (!URLUtil.isValidUrl(input)) {
                Toast.makeText(this, getString(R.string.invalid_url), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            saveUrl(this, input)
            Toast.makeText(this, "System URL saved!", Toast.LENGTH_SHORT).show()

            val resultIntent = Intent().apply {
                putExtra("UPDATED_URL", input)
            }
            setResult(Activity.RESULT_OK, resultIntent)
            finish()
        }

        btnCancelUrl.setOnClickListener {
            finish()
        }
    }
}
