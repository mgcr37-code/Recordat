package com.manuel.recordatorios;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import java.text.DateFormat;
import java.util.Date;

public final class AlarmReceiver extends android.content.BroadcastReceiver {
    @Override public void onReceive(Context c,Intent intent) {
        long id=intent.getLongExtra("id",-1); int slot=intent.getIntExtra("slot",0); Store s=new Store(c); Store.Item i=s.get(id);
        if(i==null||i.done)return;
        Scheduler.channel(c);
        Intent open=new Intent(c,MainActivity.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent content=PendingIntent.getActivity(c,Scheduler.request(id,6),open,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        Notification.Builder b=new Notification.Builder(c,Scheduler.CHANNEL).setSmallIcon(android.R.drawable.ic_popup_reminder).setContentTitle(i.title)
            .setContentText((slot==0||slot==5)?"Es el momento · "+DateFormat.getDateTimeInstance().format(new Date(i.whenMs)):"Próximo · "+DateFormat.getDateTimeInstance().format(new Date(i.whenMs)))
            .setAutoCancel(true).setContentIntent(content);
        b.addAction(new Notification.Action.Builder(null,"Posponer 10 min",action(c,id,"snooze",0)).build());
        b.addAction(new Notification.Action.Builder(null,"Realizado",action(c,id,"done",1)).build());
        if(c.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)==PackageManager.PERMISSION_GRANTED) c.getSystemService(NotificationManager.class).notify(Scheduler.request(id,slot),b.build());
        if(slot==0 && !"none".equals(i.repeat)) { i.whenMs=Scheduler.next(i.whenMs,i.repeat); s.save(i); Scheduler.schedule(c,i); }
    }
    private PendingIntent action(Context c,long id,String what,int slot) { Intent in=new Intent(c,ActionReceiver.class).setAction(what).putExtra("id",id); return PendingIntent.getBroadcast(c,Scheduler.request(id,10+slot),in,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE); }
}
