package com.comfy.travelassistant;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.speech.tts.TextToSpeech;
import android.speech.tts.Voice;
import android.webkit.JavascriptInterface;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import java.util.Set;
import org.json.JSONArray;
import org.json.JSONObject;

/** The functions the web app can call (window.ComfyNative). */
public class ComfyBridge {
  private final MainActivity a;

  ComfyBridge(MainActivity a) { this.a = a; }

  @JavascriptInterface
  public String scheduleAll(String json) {
    try {
      JSONObject o = new JSONObject(json);
      JSONArray alarms = o.optJSONArray("alarms");
      JSONObject cfg = o.optJSONObject("cfg");
      AlarmStore.prefs(a).edit()
          .putString("alarms", alarms == null ? "[]" : alarms.toString())
          .putString("cfg", cfg == null ? "{}" : cfg.toString())
          .apply();
      AlarmStore.scheduleFromPrefs(a);
    } catch (Exception e) { }
    return "{}";
  }

  @JavascriptInterface
  public String stop() {
    try {
      Intent s = new Intent(a, SpeakService.class).setAction(SpeakService.ACTION_STOP);
      a.startService(s);
    } catch (Exception e) { }
    return "{}";
  }

  @JavascriptInterface
  public String consumeFired() {
    try {
      JSONObject r = new JSONObject();
      r.put("fired", AlarmStore.consumeFired(a));
      return r.toString();
    } catch (Exception e) {
      return "{\"fired\":[]}";
    }
  }

  @JavascriptInterface
  public String testNow(String json) {
    try {
      JSONObject o = new JSONObject(json);
      Intent s = new Intent(a, SpeakService.class);
      s.putExtra("first", o.optString("first", ""));
      s.putExtra("again", o.optString("again", ""));
      s.putExtra("at", 0L);
      ContextCompat.startForegroundService(a, s);
    } catch (Exception e) { }
    return "{}";
  }

  @JavascriptInterface
  public String askNotifications() {
    if (Build.VERSION.SDK_INT >= 33) {
      a.runOnUiThread(() -> ActivityCompat.requestPermissions(a, new String[] { "android.permission.POST_NOTIFICATIONS" }, 7731));
    }
    return "{}";
  }

  @JavascriptInterface
  public String openBatterySettings() {
    a.runOnUiThread(() -> {
      try { a.startActivity(new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)); } catch (Exception e) { }
    });
    return "{}";
  }

  @JavascriptInterface
  public String canExactAlarms() {
    return "{\"exact\":" + AlarmStore.canExact(a) + ",\"sdk\":" + Build.VERSION.SDK_INT + "}";
  }

  @JavascriptInterface
  public String openExactAlarmSettings() {
    a.runOnUiThread(() -> {
      try {
        if (Build.VERSION.SDK_INT >= 31) {
          a.startActivity(new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:" + a.getPackageName())));
        } else {
          a.startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + a.getPackageName())));
        }
      } catch (Exception e) { }
    });
    return "{}";
  }

  @JavascriptInterface
  public String share(String json) {
    try {
      JSONObject o = new JSONObject(json);
      final Intent i = new Intent(Intent.ACTION_SEND);
      i.setType("text/plain");
      i.putExtra(Intent.EXTRA_SUBJECT, o.optString("title", ""));
      i.putExtra(Intent.EXTRA_TEXT, o.optString("text", ""));
      a.runOnUiThread(() -> a.startActivity(Intent.createChooser(i, "Share")));
    } catch (Exception e) { }
    return "{}";
  }

  /** Answers later by calling window.__onVoices(...) in the page. */
  @JavascriptInterface
  public String getVoices() {
    a.runOnUiThread(() -> {
      final TextToSpeech[] h = new TextToSpeech[1];
      h[0] = new TextToSpeech(a, status -> {
        JSONArray list = new JSONArray();
        try {
          if (status == TextToSpeech.SUCCESS && h[0] != null) {
            Set<Voice> vs = h[0].getVoices();
            if (vs != null) {
              for (Voice v : vs) {
                JSONObject o = new JSONObject();
                o.put("name", v.getName());
                o.put("lang", v.getLocale().toLanguageTag());
                list.put(o);
              }
            }
          }
        } catch (Exception e) { }
        try {
          JSONObject r = new JSONObject();
          r.put("voices", list);
          a.js("window.__onVoices&&window.__onVoices(" + r.toString() + ")");
        } catch (Exception e) { }
        try { if (h[0] != null) h[0].shutdown(); } catch (Exception e) { }
      });
    });
    return "{}";
  }
}
