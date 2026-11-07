package com.comfy.travelassistant;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.speech.tts.Voice;
import androidx.core.app.NotificationCompat;
import java.util.Locale;
import java.util.Set;
import org.json.JSONObject;

public class SpeakService extends Service implements TextToSpeech.OnInitListener {
  static final String CH = "comfy_alarm";
  static final String ACTION_STOP = "com.comfy.travelassistant.STOP";

  private TextToSpeech tts;
  private boolean stopped = false;
  private boolean started = false;
  private String first = "";
  private String again = "";
  private long at = 0L;
  private int repeats = 0;
  private final Handler h = new Handler(Looper.getMainLooper());
  private PowerManager.WakeLock wl;
  private JSONObject cfg = new JSONObject();

  @Override
  public IBinder onBind(Intent i) { return null; }

  private String str(Intent i, String k) {
    String s = i == null ? null : i.getStringExtra(k);
    return s == null ? "" : s;
  }

  @Override
  public int onStartCommand(Intent intent, int flags, int startId) {
    if (intent != null && ACTION_STOP.equals(intent.getAction())) {
      shutdown(true);
      return START_NOT_STICKY;
    }
    if (started) return START_NOT_STICKY;
    started = true;
    first = str(intent, "first");
    again = str(intent, "again");
    at = intent == null ? 0L : intent.getLongExtra("at", 0L);
    cfg = AlarmStore.cfg(this);
    startForegroundNow();
    try {
      PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
      wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "comfy:speak");
      wl.acquire(); // held until the user presses Stop (released in shutdown/onDestroy)
    } catch (Exception e) { }
    if (at != 0L) AlarmStore.logFired(this, at, false, false);
    tts = new TextToSpeech(this, this);
    return START_NOT_STICKY;
  }

  private void startForegroundNow() {
    NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
    if (Build.VERSION.SDK_INT >= 26) {
      NotificationChannel ch = new NotificationChannel(CH, "Travel reminders", NotificationManager.IMPORTANCE_HIGH);
      ch.setSound(null, null);
      ch.enableVibration(true);
      nm.createNotificationChannel(ch);
    }
    Intent open = new Intent(this, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
    PendingIntent po = PendingIntent.getActivity(this, 0, open, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
    Intent stop = new Intent(this, SpeakService.class).setAction(ACTION_STOP);
    PendingIntent ps = PendingIntent.getService(this, 1, stop, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
    Notification n = new NotificationCompat.Builder(this, CH)
        .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
        .setContentTitle("Travel reminder")
        .setContentText(first)
        .setStyle(new NotificationCompat.BigTextStyle().bigText(first))
        .setCategory(NotificationCompat.CATEGORY_ALARM)
        .setPriority(NotificationCompat.PRIORITY_MAX)
        .setOngoing(true)
        .setContentIntent(po)
        .addAction(0, "Stop", ps)
        .build();
    if (Build.VERSION.SDK_INT >= 29) {
      startForeground(77, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK);
    } else {
      startForeground(77, n);
    }
  }

  @Override
  public void onInit(int status) {
    if (status != TextToSpeech.SUCCESS || stopped) { shutdown(false); return; }
    try {
      tts.setAudioAttributes(new AudioAttributes.Builder()
          .setUsage(AudioAttributes.USAGE_ALARM)
          .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build());
      int r = tts.setLanguage(Locale.getDefault());
      if (r == TextToSpeech.LANG_MISSING_DATA || r == TextToSpeech.LANG_NOT_SUPPORTED) tts.setLanguage(Locale.US);
      String vn = cfg.optString("voice", "");
      if (!vn.isEmpty()) {
        Set<Voice> vs = tts.getVoices();
        if (vs != null) {
          for (Voice v : vs) {
            if (v.getName().equals(vn)) { tts.setVoice(v); break; }
          }
        }
      }
      tts.setPitch((float) cfg.optDouble("pitch", 1.1));
      tts.setSpeechRate((float) cfg.optDouble("rate", 0.95));
    } catch (Exception e) { }
    tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
      @Override public void onStart(String id) { }
      @Override public void onDone(String id) { h.post(() -> afterSpeech()); }
      @Override public void onError(String id) { h.post(() -> afterSpeech()); }
    });
    long delay = 0L;
    if (cfg.optInt("chime", 1) == 1) {
      try { new ToneGenerator(AudioManager.STREAM_ALARM, 80).startTone(ToneGenerator.TONE_PROP_BEEP2, 600); } catch (Exception e) { }
      delay = 1000L;
    }
    h.postDelayed(() -> speakNow(first), delay);
  }

  private void speakNow(String t) {
    if (stopped || tts == null) return;
    Bundle p = new Bundle();
    float vol = (float) cfg.optDouble("vol", 1.0);
    if (cfg.optInt("rise", 0) == 1) vol = Math.min(vol, 0.35f + repeats * 0.2f);
    p.putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, vol);
    p.putInt(TextToSpeech.Engine.KEY_PARAM_STREAM, AudioManager.STREAM_ALARM);
    tts.speak(t, TextToSpeech.QUEUE_FLUSH, p, "u" + repeats);
  }

  private void afterSpeech() {
    if (stopped) return;
    int max = cfg.optInt("maxrep", 0);
    if (max > 0 && repeats + 1 >= max) { h.postDelayed(() -> shutdown(false), 1500L); return; }
    repeats++;
    h.postDelayed(() -> speakNow(again), cfg.optLong("gap", 1000L));
  }

  private void shutdown(boolean byUser) {
    if (!stopped && at != 0L) AlarmStore.logFired(this, at, true, byUser);
    stopped = true;
    h.removeCallbacksAndMessages(null);
    try { if (tts != null) { tts.stop(); tts.shutdown(); } } catch (Exception e) { }
    try { if (wl != null && wl.isHeld()) wl.release(); } catch (Exception e) { }
    stopForeground(true);
    stopSelf();
  }

  @Override
  public void onDestroy() {
    if (!stopped && at != 0L) AlarmStore.logFired(this, at, true, false);
    stopped = true;
    try { if (tts != null) tts.shutdown(); } catch (Exception e) { }
    try { if (wl != null && wl.isHeld()) wl.release(); } catch (Exception e) { }
    super.onDestroy();
  }
}
