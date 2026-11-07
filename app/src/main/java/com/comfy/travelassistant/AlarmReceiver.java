package com.comfy.travelassistant;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import androidx.core.content.ContextCompat;

public class AlarmReceiver extends BroadcastReceiver {
  @Override
  public void onReceive(Context c, Intent i) {
    Intent s = new Intent(c, SpeakService.class);
    s.putExtra("at", i.getLongExtra("at", 0L));
    s.putExtra("first", i.getStringExtra("first"));
    s.putExtra("again", i.getStringExtra("again"));
    ContextCompat.startForegroundService(c, s);
  }
}
