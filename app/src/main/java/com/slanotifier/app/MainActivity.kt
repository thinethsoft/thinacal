package com.slanotifier.app

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.webkit.*
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var progressBar: ProgressBar
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private lateinit var unconfiguredLayout: LinearLayout
    private lateinit var btnSetupUrl: Button
    private lateinit var fabSettings: com.google.android.material.floatingactionbutton.FloatingActionButton
    private lateinit var fabCall: com.google.android.material.floatingactionbutton.FloatingActionButton

    private val settingsLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val shouldRegisterDb = result.data?.getBooleanExtra("REGISTER_DEVICE_DB", false) ?: false
            if (shouldRegisterDb) {
                webView.postDelayed({
                    webView.evaluateJavascript("if(window.registerDeviceInDatabase) window.registerDeviceInDatabase();", null)
                }, 300)
            }
            val updatedUrl = result.data?.getStringExtra("UPDATED_URL")
            if (!updatedUrl.isNullOrBlank()) {
                loadWebUrl(updatedUrl)
            }
        }
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            Log.d("MainActivity", "POST_NOTIFICATIONS permission granted")
            NotificationHelper.createNotificationChannel(this)
        } else {
            Toast.makeText(this, "Notification permission is required for SLA task alerts", Toast.LENGTH_LONG).show()
        }
    }

    private val requestRecordAudioPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            Log.d("MainActivity", "RECORD_AUDIO permission granted")
        } else {
            Toast.makeText(this, "Microphone permission is required for Voice Calls", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        webView = findViewById(R.id.webView)
        progressBar = findViewById(R.id.progressBar)
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout)
        unconfiguredLayout = findViewById(R.id.unconfiguredLayout)
        btnSetupUrl = findViewById(R.id.btnSetupUrl)
        fabSettings = findViewById(R.id.fabSettings)
        fabCall = findViewById(R.id.fabCall)

        NotificationHelper.createNotificationChannel(this)
        requestNotificationPermission()
        requestRecordAudioPermission()
        requestBatteryOptimizationExemption()

        setupWebView()

        btnSetupUrl.setOnClickListener {
            openUrlSettings()
        }

        fabSettings.setOnClickListener {
            openUrlSettings()
        }

        fabCall.setOnClickListener {
            val savedBase = UrlSettingsActivity.getSavedUrl(this) ?: ""
            if (!savedBase.isNullOrBlank()) {
                val base = if (savedBase.endsWith("/")) savedBase.substring(0, savedBase.lastIndexOf('/') + 1) else "$savedBase/"
                loadWebUrl(base + "call_center.php")
            } else {
                webView.evaluateJavascript("if(window.openVoiceCallSelector) window.openVoiceCallSelector();", null)
            }
        }

        swipeRefreshLayout.setOnRefreshListener {
            webView.reload()
        }

        // Fix accidental reload when scrolling: Only trigger refresh at absolute top
        swipeRefreshLayout.setOnChildScrollUpCallback { _, _ ->
            webView.scrollY > 0
        }

        handleIntent(intent)

        val savedUrl = UrlSettingsActivity.getSavedUrl(this)
        if (savedUrl.isNullOrBlank()) {
            showUnconfiguredState()
            openUrlSettings()
        } else {
            loadWebUrl(savedUrl)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent == null) return
        val taskId = intent.getStringExtra("EXTRA_TASK_ID")
        val action = intent.getStringExtra("EXTRA_ACTION")
        val targetUrl = intent.getStringExtra("TARGET_URL")

        if (action == "ANSWER_CALL" || !targetUrl.isNullOrBlank()) {
            NotificationHelper.stopContinuousRingtone()
            if (!targetUrl.isNullOrBlank()) {
                val fullUrl = if (targetUrl.startsWith("http")) targetUrl else {
                    val savedBase = UrlSettingsActivity.getSavedUrl(this) ?: ""
                    val base = if (savedBase.endsWith("/")) savedBase.substring(0, savedBase.lastIndexOf('/') + 1) else "$savedBase/"
                    base + targetUrl
                }
                loadWebUrl(fullUrl)
            }
        } else if (!taskId.isNullOrBlank() && action == "ACKNOWLEDGE") {
            TaskManager.acknowledgeTask(this, taskId)
            Toast.makeText(this, "SLA Task Acknowledged: $taskId", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupWebView() {
        val settings = webView.settings
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.databaseEnabled = true
        settings.useWideViewPort = true
        settings.loadWithOverviewMode = true
        settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
        settings.mediaPlaybackRequiresUserGesture = false

        // Register JS Bridge for web app integration
        webView.addJavascriptInterface(WebAppInterface(this), "AndroidBridge")

        // Enable OS-level Service Worker & WebPush Support
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            try {
                val swController = ServiceWorkerController.getInstance()
                swController.serviceWorkerWebSettings.allowContentAccess = true
                swController.serviceWorkerWebSettings.blockNetworkLoads = false
                swController.setServiceWorkerClient(object : ServiceWorkerClient() {
                    override fun shouldInterceptRequest(request: WebResourceRequest?): WebResourceResponse? {
                        return super.shouldInterceptRequest(request)
                    }
                })
            } catch (e: Exception) {
                Log.e("MainActivity", "Error setting up ServiceWorkerController", e)
            }
        }

        webView.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                progressBar.visibility = View.VISIBLE
                injectNotificationPolyfill(view)
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                progressBar.visibility = View.GONE
                swipeRefreshLayout.isRefreshing = false
                injectNotificationPolyfill(view)
            }

            override fun onReceivedError(
                view: WebView?,
                request: WebResourceRequest?,
                error: WebResourceError?
            ) {
                progressBar.visibility = View.GONE
                swipeRefreshLayout.isRefreshing = false
            }
        }

        webView.webChromeClient = object : WebChromeClient() {
            override fun onPermissionRequest(request: PermissionRequest?) {
                // Grant all requested web permissions (Notifications, Geolocation, Media)
                request?.grant(request.resources)
            }

            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                if (newProgress == 100) {
                    progressBar.visibility = View.GONE
                } else {
                    progressBar.visibility = View.VISIBLE
                    progressBar.progress = newProgress
                }
            }
        }
    }

    private fun injectNotificationPolyfill(view: WebView?) {
        val jsPolyfill = """
            (function() {
                try {
                    if (!window.WebRTCCallManager) {
                        var scriptTag = document.createElement('script');
                        scriptTag.src = 'js/webrtc_call.js?v=' + Date.now();
                        document.head.appendChild(scriptTag);
                    }
                    if (!document.getElementById('voiceCallModal') && window.jQuery) {
                        $.get('includes/call_modal.php', function(html) {
                            if (!document.getElementById('voiceCallModal')) {
                                $('body').append(html);
                            }
                        });
                    }
                    if (window.Notification) {
                        window.Notification.permission = 'granted';
                        window.Notification.requestPermission = function(callback) {
                            if (callback) callback('granted');
                            return Promise.resolve('granted');
                        };
                    }
                    
                    var OrigNotification = window.Notification;
                    window.Notification = function(title, options) {
                        options = options || {};
                        if (window.AndroidBridge) {
                            window.AndroidBridge.triggerAlert(
                                options.tag || ('task_' + Date.now()),
                                title || 'E Shop Alert',
                                options.body || ''
                            );
                        }
                        if (OrigNotification) {
                            try { return new OrigNotification(title, options); } catch(e) {}
                        }
                    };
                    window.Notification.permission = 'granted';

                    window.registerDeviceInDatabase = function() {
                        if (window.AndroidBridge && window.jQuery) {
                            var uuid = window.AndroidBridge.getPersistentDeviceUuid();
                            var token = window.AndroidBridge.getPersistentFcmToken();
                            $.ajax({
                                url: 'entity/device_registration_action.php',
                                type: 'POST',
                                contentType: 'application/json',
                                data: JSON.stringify({
                                    device_uuid: uuid,
                                    fcm_token: token,
                                    webpush_subscription: null,
                                    device_info: 'E Shop Mobile App (Kotlin Native)',
                                    platform: 'Android'
                                }),
                                success: function(res) {
                                    if (window.Swal) {
                                        Swal.fire({ icon: 'success', title: 'Device Registered!', text: 'FCM Token saved successfully in database.' });
                                    } else {
                                        alert('FCM Token Saved Successfully!');
                                    }
                                },
                                error: function() {
                                    alert('Registration failed. Make sure you are logged in.');
                                }
                            });
                        }
                    };

                    window.sendTestNotificationFromApp = function() {
                        if (window.AndroidBridge && window.jQuery) {
                            var uuid = window.AndroidBridge.getPersistentDeviceUuid();
                            $.post('entity/my_notifications_action.php', { action: 'test_notification', device_uuid: uuid }, function(res) {
                                if (res.status === 'success') {
                                    if (window.Swal) Swal.fire({ icon: 'success', title: 'Dispatched!', text: res.message });
                                    else alert(res.message);
                                } else {
                                    if (window.Swal) Swal.fire({ icon: 'error', title: 'Test Failed', text: res.message });
                                    else alert(res.message);
                                }
                            }, 'json');
                        }
                    };

                    var el1 = document.getElementById('perm-status'); if(el1) el1.innerHTML = '<span class=\"text-success\"><i class=\"bi bi-check-circle-fill me-1\"></i> Granted</span>';
                    var el2 = document.getElementById('push-status'); if(el2) el2.innerHTML = '<span class=\"text-success\"><i class=\"bi bi-check-circle-fill me-1\"></i> Supported</span>';
                    var el3 = document.getElementById('sw-status'); if(el3) el3.innerHTML = '<span class=\"text-success\"><i class=\"bi bi-check-circle-fill me-1\"></i> Running</span>';
                } catch(e) {
                    console.error('Notification bridge error:', e);
                }
            })();
        """.trimIndent()
        view?.evaluateJavascript(jsPolyfill, null)
    }

    private val pollHandler = android.os.Handler(android.os.Looper.getMainLooper())
    private val pollRunnable = object : Runnable {
        override fun run() {
            checkPendingNotifications()
            pollHandler.postDelayed(this, 15000) // Check every 15 seconds
        }
    }

    private fun checkPendingNotifications() {
        val checkJs = """
            (function() {
                try {
                    if (window.AndroidBridge && window.jQuery) {
                        $.getJSON('entity/my_notifications_action.php?action=get_unread_count', function(data) {
                            if (data && data.status === 'success' && data.unread_count > 0 && data.latest) {
                                window.AndroidBridge.triggerAlert(
                                    'notif_' + data.latest.id,
                                    data.latest.title || 'E Shop Notification',
                                    data.latest.message || ''
                                );
                            }
                        }).fail(function() {});
                    }
                } catch(e) {}
            })();
        """.trimIndent()
        webView.evaluateJavascript(checkJs, null)
    }

    override fun onResume() {
        super.onResume()
        pollHandler.post(pollRunnable)
    }

    override fun onPause() {
        super.onPause()
        pollHandler.removeCallbacks(pollRunnable)
    }

    private fun loadWebUrl(url: String) {
        unconfiguredLayout.visibility = View.GONE
        webView.visibility = View.VISIBLE
        webView.loadUrl(url)
    }

    private fun showUnconfiguredState() {
        webView.visibility = View.GONE
        unconfiguredLayout.visibility = View.VISIBLE
    }

    private fun openUrlSettings() {
        val intent = Intent(this, UrlSettingsActivity::class.java)
        settingsLauncher.launch(intent)
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private fun requestRecordAudioPermission() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestRecordAudioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    private fun requestBatteryOptimizationExemption() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
            if (!pm.isIgnoringBatteryOptimizations(packageName)) {
                try {
                    val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                        data = Uri.parse("package:$packageName")
                    }
                    startActivity(intent)
                } catch (e: Exception) {
                    Log.e("MainActivity", "Cannot request battery optimization exemption", e)
                }
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_refresh -> {
                webView.reload()
                true
            }
            R.id.action_settings -> {
                openUrlSettings()
                true
            }
            R.id.action_test_alert -> {
                val testTaskId = "TASK_" + (1000..9999).random()
                TaskManager.addUnacknowledgedTask(
                    this,
                    testTaskId,
                    "URGENT: Task #$testTaskId",
                    "Critical SLA breach in 5 mins! Respond immediately."
                )
                NotificationHelper.triggerSlaNotification(
                    this,
                    testTaskId,
                    "URGENT: Task #$testTaskId",
                    "Critical SLA breach in 5 mins! Respond immediately."
                )
                Toast.makeText(this, "Triggered SLA Alert for $testTaskId", Toast.LENGTH_SHORT).show()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack()
        } else {
            super.onBackPressed()
        }
    }
}
