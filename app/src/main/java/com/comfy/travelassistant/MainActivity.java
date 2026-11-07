package com.comfy.travelassistant;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.webkit.JsPromptResult;
import android.webkit.JsResult;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.FrameLayout;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.webkit.WebViewAssetLoader;

public class MainActivity extends Activity {
  private static final String HOST = "appassets.androidplatform.net";
  private WebView web;
  private String pendingGo = null;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    pendingGo = goFrom(getIntent());
    WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

    FrameLayout root = new FrameLayout(this);
    root.setBackgroundColor(Color.parseColor("#0B1B33"));
    web = new WebView(this);
    web.setBackgroundColor(Color.parseColor("#0B1B33"));
    root.addView(web, new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
    setContentView(root);

    ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
      Insets i = insets.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.ime());
      v.setPadding(i.left, i.top, i.right, i.bottom);
      return WindowInsetsCompat.CONSUMED;
    });
    new WindowInsetsControllerCompat(getWindow(), root).setAppearanceLightStatusBars(false);

    WebSettings s = web.getSettings();
    s.setJavaScriptEnabled(true);
    s.setDomStorageEnabled(true);
    s.setMediaPlaybackRequiresUserGesture(false);
    s.setAllowFileAccess(false);
    s.setAllowContentAccess(false);

    final WebViewAssetLoader loader = new WebViewAssetLoader.Builder()
        .setDomain(HOST)
        .addPathHandler("/assets/", new WebViewAssetLoader.AssetsPathHandler(this))
        .build();

    web.setWebViewClient(new WebViewClient() {
      @Override
      public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
        return loader.shouldInterceptRequest(request.getUrl());
      }

      @Override
      public void onPageFinished(WebView view, String url) {
        runGo();
      }

      @Override
      public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
        Uri u = request.getUrl();
        if (HOST.equals(u.getHost())) return false;
        try { startActivity(new Intent(Intent.ACTION_VIEW, u)); } catch (Exception e) { }
        return true;
      }
    });

    web.setWebChromeClient(new WebChromeClient() {
      @Override
      public boolean onJsAlert(WebView v, String url, String msg, JsResult r) {
        new AlertDialog.Builder(MainActivity.this).setMessage(msg)
            .setPositiveButton("OK", (d, w) -> r.confirm())
            .setOnCancelListener(d -> r.cancel()).show();
        return true;
      }

      @Override
      public boolean onJsConfirm(WebView v, String url, String msg, JsResult r) {
        new AlertDialog.Builder(MainActivity.this).setMessage(msg)
            .setPositiveButton("OK", (d, w) -> r.confirm())
            .setNegativeButton("Cancel", (d, w) -> r.cancel())
            .setOnCancelListener(d -> r.cancel()).show();
        return true;
      }

      @Override
      public boolean onJsPrompt(WebView v, String url, String msg, String def, JsPromptResult r) {
        final EditText et = new EditText(MainActivity.this);
        if (def != null) et.setText(def);
        new AlertDialog.Builder(MainActivity.this).setMessage(msg).setView(et)
            .setPositiveButton("OK", (d, w) -> r.confirm(et.getText().toString()))
            .setNegativeButton("Cancel", (d, w) -> r.cancel())
            .setOnCancelListener(d -> r.cancel()).show();
        return true;
      }
    });

    web.addJavascriptInterface(new ComfyBridge(this), "ComfyNative");
    web.loadUrl("https://" + HOST + "/assets/index.html");
  }

  private String goFrom(Intent i) {
    if (i == null || i.getAction() == null) return null;
    if ("com.comfy.travelassistant.LEAVE".equals(i.getAction())) return "leave";
    if ("com.comfy.travelassistant.ADD".equals(i.getAction())) return "add";
    return null;
  }

  private void runGo() {
    if (pendingGo != null && web != null) {
      String g = pendingGo;
      pendingGo = null;
      js("window.__go&&window.__go('" + g + "')");
    }
  }

  void js(final String code) {
    runOnUiThread(() -> web.evaluateJavascript(code, null));
  }

  @Override
  protected void onResume() {
    super.onResume();
    if (web != null) js("window.__resume&&window.__resume()");
  }

  @Override
  protected void onNewIntent(Intent intent) {
    super.onNewIntent(intent);
    setIntent(intent);
    String g = goFrom(intent);
    if (g != null) { pendingGo = g; runGo(); }
  }

  @Override
  public void onBackPressed() {
    web.evaluateJavascript("(function(){return window.__back?window.__back():false})()", v -> {
      if (!"true".equals(v)) moveTaskToBack(true);
    });
  }

  @Override
  protected void onDestroy() {
    if (web != null) { web.removeJavascriptInterface("ComfyNative"); web.destroy(); }
    super.onDestroy();
  }
}
