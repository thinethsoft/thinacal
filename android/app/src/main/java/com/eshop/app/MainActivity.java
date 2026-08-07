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
                            "  if (navigator.serviceWorker && !navigator.serviceWorker.ready) {" +
                            "    navigator.serviceWorker.ready = Promise.resolve({ active: true });" +
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
