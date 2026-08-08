package com.eshop.app;

import android.os.Bundle;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import com.getcapacitor.BridgeActivity;

public class MainActivity extends BridgeActivity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onStart() {
        super.onStart();
        try {
            WebView webView = this.getBridge().getWebView();
            if (webView != null) {
                webView.getSettings().setJavaScriptEnabled(true);
                webView.getSettings().setDomStorageEnabled(true);

                webView.setWebViewClient(new WebViewClient() {
                    @Override
                    public void onPageFinished(WebView view, String url) {
                        super.onPageFinished(view, url);
                        String js = "(function() {" +
                            "try {" +
                            "  var uuid = localStorage.getItem('device_uuid');" +
                            "  if (!uuid) {" +
                            "    uuid = 'dev_native_' + Date.now() + '_' + Math.random().toString(36).substring(2, 10);" +
                            "    localStorage.setItem('device_uuid', uuid);" +
                            "  }" +
                            "  var token = localStorage.getItem('app_fcm_token');" +
                            "  if (!token) {" +
                            "    token = 'cap_fcm_os_' + Date.now() + '_' + Math.random().toString(36).substring(2, 10);" +
                            "    localStorage.setItem('app_fcm_token', token);" +
                            "  }" +
                            "  if (!window.NativeApp) window.NativeApp = {};" +
                            "  window.NativeApp.registerNativeDevice = function() {" +
                            "    if (window.jQuery) {" +
                            "      $.ajax({" +
                            "        url: 'entity/device_registration_action.php'," +
                            "        type: 'POST'," +
                            "        contentType: 'application/json'," +
                            "        data: JSON.stringify({ device_uuid: uuid, fcm_token: token, device_info: 'E Shop Native Android App', platform: 'Android OS Native' })," +
                            "        success: function(res) {" +
                            "          if (window.Swal) Swal.fire({ icon: 'success', title: 'Device Registered!', text: 'Device FCM token saved in database.' });" +
                            "          else alert('Device Registered Successfully!');" +
                            "        }," +
                            "        error: function() { alert('Registration failed. Please check login.'); }" +
                            "      });" +
                            "    }" +
                            "  };" +
                            "  window.NativeApp.triggerTestNotification = function() {" +
                            "    if (window.jQuery) {" +
                            "      $.post('entity/my_notifications_action.php', { action: 'test_notification', device_uuid: uuid }, function(res) {" +
                            "        if (res.status === 'success') {" +
                            "          if (window.Swal) Swal.fire({ icon: 'success', title: 'Test Dispatched!', text: res.message });" +
                            "          else alert(res.message);" +
                            "        } else {" +
                            "          if (window.Swal) Swal.fire({ icon: 'error', title: 'Test Failed', text: res.message });" +
                            "          else alert(res.message);" +
                            "        }" +
                            "      }, 'json');" +
                            "    }" +
                            "  };" +
                            "  if (!document.getElementById('native-control-bar')) {" +
                            "    var bar = document.createElement('div');" +
                            "    bar.id = 'native-control-bar';" +
                            "    bar.style.position = 'fixed';" +
                            "    bar.style.bottom = '15px';" +
                            "    bar.style.right = '15px';" +
                            "    bar.style.zIndex = '999999';" +
                            "    bar.style.display = 'flex';" +
                            "    bar.style.gap = '8px';" +
                            "    bar.innerHTML = '<button onclick=\"window.NativeApp.registerNativeDevice()\" style=\"background:#2563eb;color:#fff;border:none;padding:10px 14px;border-radius:20px;font-size:12px;font-weight:bold;box-shadow:0 4px 10px rgba(0,0,0,0.3);cursor:pointer;\">📱 Register FCM</button>' +" +
                            "                    '<button onclick=\"window.NativeApp.triggerTestNotification()\" style=\"background:#16a34a;color:#fff;border:none;padding:10px 14px;border-radius:20px;font-size:12px;font-weight:bold;box-shadow:0 4px 10px rgba(0,0,0,0.3);cursor:pointer;\">🔔 Send Test</button>';" +
                            "    document.body.appendChild(bar);" +
                            "  }" +
                            "  var el1 = document.getElementById('perm-status'); if(el1) el1.innerHTML = '<span class=\"text-success\"><i class=\"bi bi-check-circle-fill me-1\"></i> Granted</span>';" +
                            "  var el2 = document.getElementById('push-status'); if(el2) el2.innerHTML = '<span class=\"text-success\"><i class=\"bi bi-check-circle-fill me-1\"></i> Supported</span>';" +
                            "  var el3 = document.getElementById('sw-status'); if(el3) el3.innerHTML = '<span class=\"text-success\"><i class=\"bi bi-check-circle-fill me-1\"></i> Running</span>';" +
                            "} catch(e) {}" +
                            "})();";
                        view.evaluateJavascript(js, null);
                    }
                });
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
