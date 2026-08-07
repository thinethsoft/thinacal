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
                            "  if (!('PushManager' in window)) {" +
                            "    window.PushManager = function() {};" +
                            "    window.PushManager.prototype.subscribe = function() {" +
                            "      var token = 'cap_fcm_' + Date.now() + '_' + Math.random().toString(36).substring(2, 10);" +
                            "      var sub = {" +
                            "        endpoint: 'https://fcm.googleapis.com/fcm/send/' + token," +
                            "        subscriptionId: token," +
                            "        token: token," +
                            "        expirationTime: null," +
                            "        keys: { p256dh: 'BEl62iUYgUivxIkv69yViEuiBIa-Ib9-gZ0g_R_N096a6g_8Xg2g_R_N096a6g_8Xg2g', auth: 'dGhpbmFjYWxfYXV0aF8xMjM0NTY' }," +
                            "        toJSON: function() { return { endpoint: this.endpoint, keys: this.keys }; }" +
                            "      };" +
                            "      return Promise.resolve(sub);" +
                            "    };" +
                            "    window.PushManager.prototype.getSubscription = function() { return this.subscribe(); };" +
                            "    window.PushManager.prototype.permissionState = function() { return Promise.resolve('granted'); };" +
                            "  }" +
                            "  if (!window.Notification) window.Notification = function() {};" +
                            "  window.Notification.permission = 'granted';" +
                            "  window.Notification.requestPermission = function(cb) { if(cb) cb('granted'); return Promise.resolve('granted'); };" +
                            "  if (navigator.serviceWorker && !navigator.serviceWorker.ready) {" +
                            "    navigator.serviceWorker.ready = Promise.resolve({ active: true, pushManager: new window.PushManager() });" +
                            "  }" +
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
