package com.comfy.travelassistant;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import org.json.JSONArray;
import org.json.JSONObject;

public class AlarmStore {
  static final String PREFS = "comfy_alarms";
  static final String ACTION = "com.comfy.travelassistant.ALARM";

  static SharedPreferences prefs(Context c) {
    return c.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
  }

  static JSONObject cfg(Context c) {
    try { return new JSONObject(prefs(c).getString("cfg", "{}")); } catch (Exception e) { return new JSONObject(); }
  }

  /** True when Android allows exact alarms (always true before Android 12). */
  static boolean canExact(Context c) {
    if (Build.VERSION.SDK_INT < 31) return true;
    AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
    return am != null && am.canScheduleExactAlarms();
  }

  static void cancelAll(Context c) {
    AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
    try {
      JSONArray ids = new JSONArray(prefs(c).getString("ids", "[]"));
      for (int k = 0; k < ids.length(); k++) {
        Intent i = new Intent(c, AlarmReceiver.class).setAction(ACTION);
        PendingIntent p = PendingIntent.getBroadcast(c, ids.getInt(k), i, PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE);
        if (p != null) { am.cancel(p); p.cancel(); }
      }
    } catch (Exception e) { }
    prefs(c).edit().putString("ids", "[]").apply();
  }

  static void scheduleFromPrefs(Context c) {
    cancelAll(c);
    AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
    try {
      JSONArray arr = new JSONArray(prefs(c).getString("alarms", "[]"));
      JSONArray ids = new JSONArray();
      long now = System.currentTimeMillis();
      for (int k = 0; k < arr.length(); k++) {
        JSONObject o = arr.getJSONObject(k);
        long at = o.getLong("at");
        if (at <= now) continue;
        int id = o.getInt("id");
        Intent i = new Intent(c, AlarmReceiver.class).setAction(ACTION);
        i.putExtra("at", at);
        i.putExtra("first", o.optString("first", ""));
        i.putExtra("again", o.optString("again", ""));
        PendingIntent p = PendingIntent.getBroadcast(c, id, i, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        PendingIntent show = PendingIntent.getActivity(c, 0, new Intent(c, MainActivity.class), PendingIntent.FLAG_IMMUTABLE);
        try {
          am.setAlarmClock(new AlarmManager.AlarmClockInfo(at, show), p);
        } catch (SecurityException se) {
          // Exact alarms are not allowed yet (Android 12+). Use the closest allowed alarm so the reminder is not lost.
          am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, p);
        }
        ids.put(id);
      }
      prefs(c).edit().putString("ids", ids.toString()).apply();
    } catch (Exception e) { }
  }

  static synchronized void logFired(Context c, long at, boolean ended, boolean byUser) {
    try {
      JSONArray a = new JSONArray(prefs(c).getString("fired", "[]"));
      boolean found = false;
      for (int k = 0; k < a.length(); k++) {
        JSONObject o = a.getJSONObject(k);
        if (o.getLong("at") == at) {
          if (ended) o.put("ended", true);
          if (byUser) o.put("stopped", true);
          found = true;
        }
      }
      if (!found) {
        JSONObject o = new JSONObject();
        o.put("at", at); o.put("ended", ended); o.put("stopped", byUser); o.put("t", System.currentTimeMillis());
        a.put(o);
      }
      prefs(c).edit().putString("fired", a.toString()).apply();
    } catch (Exception e) { }
  }

  static synchronized JSONArray consumeFired(Context c) {
    JSONArray out = new JSONArray();
    JSONArray keep = new JSONArray();
    try {
      JSONArray a = new JSONArray(prefs(c).getString("fired", "[]"));
      long now = System.currentTimeMillis();
      for (int k = 0; k < a.length(); k++) {
        JSONObject o = a.getJSONObject(k);
        if (o.optBoolean("ended") || now - o.optLong("t") > 24 * 60 * 60 * 1000L) out.put(o); else keep.put(o);
      }
      prefs(c).edit().putString("fired", keep.toString()).apply();
    } catch (Exception e) { }
    return out;
  }
}
