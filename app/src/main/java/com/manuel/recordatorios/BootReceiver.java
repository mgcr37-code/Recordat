package com.manuel.recordatorios;
import android.content.*;
public final class BootReceiver extends BroadcastReceiver { @Override public void onReceive(Context c,Intent i) { Scheduler.rescheduleAll(c); } }
