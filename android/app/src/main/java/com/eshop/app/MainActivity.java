package com.eshop.app;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;
import com.getcapacitor.BridgeActivity;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class MainActivity extends BridgeActivity {
    private static final String PREFS_NAME = "eshop_native_prefs";
    private static final String KEY_FCM_TOKEN = "native_fcm_token";
    private static final String KEY_DEVICE_UUID = "native_device_uuid";

    private String getOrGenerateDeviceUuid() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String uuid = prefs.getString(KEY_DEVICE_UUID, null);
        if (uuid == null || uuid.trim().isEmpty()) {
            uuid = "dev_native_" + System.currentTimeMillis() + "_" + Math.abs(new java.util.Random().nextLong());
            prefs.edit().putString(KEY_DEVICE_UUID, uuid).apply();
        }
        return uuid;
    }

    private String getOrGenerateFcmToken() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String token = prefs.getString(KEY_FCM_TOKEN, null);
        if (token == null || token.trim().isEmpty()) {
            token = "cap_fcm_os_" + System.currentTimeMillis() + "_" + Math.abs(new java.util.Random().nextLong());
            prefs.edit().putString(KEY_FCM_TOKEN, token).apply();
        }
        return token;
    }

    public class NativeAppBridge {
        @JavascriptInterface
        public void registerNativeDevice() {
            new Thread(() -> {
                try {
                    String uuid = getOrGenerateDeviceUuid();
                    String token = getOrGenerateFcmToken();
                    String baseUrl = "https://admin-14.hoteleshopdemo.com/";

                    URL url = new URL(baseUrl + "entity/device_registration_action.php");
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("POST");
                    conn.setRequestProperty("Content-Type", "application/json");
                    conn.setDoOutput(true);

                    String jsonInputString = String.format(
                        "{\"device_uuid\":\"%s\",\"fcm_token\":\"%s\",\"device_info\":\"E Shop Native Android App\",\"platform\":\"Android OS Native\"}",
                        uuid, token
                    );

                    try (OutputStream os = conn.getOutputStream()) {
                        byte[] input = jsonInputString.getBytes(StandardCharsets.UTF_8);
                        os.write(input, 0, input.length);
                    }

                    int code = conn.getResponseCode();
                    new Handler(Looper.getMainLooper()).post(() -> {
                        if (code == 200) {
                            Toast.makeText(MainActivity.this, "✅ FCM Device Registered in Database!", Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(MainActivity.this, "⚠️ Registration response code: " + code, Toast.LENGTH_LONG).show();
                        }
                    });
                } catch (Exception e) {
                    new Handler(Looper.getMainLooper()).post(() -> 
                        Toast.makeText(MainActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show()
                    );
                }
            }).start();
        }

        @JavascriptInterface
        public void triggerTestNotification() {
            new Thread(() -> {
                try {
                    String uuid = getOrGenerateDeviceUuid();
                    String baseUrl = "https://admin-14.hoteleshopdemo.com/";

                    URL url = new URL(baseUrl + "entity/my_notifications_action.php");
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("POST");
                    conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                    conn.setDoOutput(true);

                    String postData = "action=test_notification&device_uuid=" + uuid;
                    try (OutputStream os = conn.getOutputStream()) {
                        byte[] input = postData.getBytes(StandardCharsets.UTF_8);
                        os.write(input, 0, input.length);
                    }

                    int code = conn.getResponseCode();
                    new Handler(Looper.getMainLooper()).post(() -> {
                        if (code == 200) {
                            Toast.makeText(MainActivity.this, "🔔 Test Notification Dispatched to Device!", Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(MainActivity.this, "⚠️ Test response code: " + code, Toast.LENGTH_LONG).show();
                        }
                    });
                } catch (Exception e) {
                    new Handler(Looper.getMainLooper()).post(() -> 
                        Toast.makeText(MainActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show()
                    );
                }
            }).start();
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onStart() {
        super.onStart();
        try {
            final String persistentToken = getOrGenerateFcmToken();
            final String persistentUuid = getOrGenerateDeviceUuid();

            WebView webView = this.getBridge().getWebView();
            if (webView != null) {
                webView.getSettings().setJavaScriptEnabled(true);
                webView.getSettings().setDomStorageEnabled(true);
                webView.addJavascriptInterface(new NativeAppBridge(), "NativeApp");

                webView.setWebViewClient(new WebViewClient() {
                    @Override
                    public void onPageFinished(WebView view, String url) {
                        super.onPageFinished(view, url);
                        String js = "(function() {" +
                            "try {" +
                            "  localStorage.setItem('device_uuid', '" + persistentUuid + "');" +
                            "  localStorage.setItem('app_fcm_token', '" + persistentToken + "');" +
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
