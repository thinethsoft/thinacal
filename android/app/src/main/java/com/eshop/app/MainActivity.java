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
                String currentUa = webView.getSettings().getUserAgentString();
                if (currentUa != null && !currentUa.contains("AndroidApp")) {
                    webView.getSettings().setUserAgentString(currentUa + " AndroidApp Capacitor");
                }
                webView.setWebViewClient(new WebViewClient() {
                    @Override
                    public void onPageFinished(WebView view, String url) {
                        super.onPageFinished(view, url);
                        String js = "(function() {" +
                            "try {" +
                            "  if (navigator.serviceWorker && !navigator.serviceWorker.ready) {" +
                            "    navigator.serviceWorker.ready = Promise.resolve({" +
                            "      active: true," +
                            "      pushManager: {" +
                            "        getSubscription: function() { return Promise.resolve(null); }," +
                            "        subscribe: function() { return Promise.resolve(null); }" +
                            "      }" +
                            "    });" +
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
