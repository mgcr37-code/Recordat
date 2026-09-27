package com.manuel.recordatorios;

import android.app.*;
import android.content.*;
import android.os.Build;
import java.time.*;

final class Scheduler {
    static final String CHANNEL="reminders";
    static final long[] MINUTES={0,15,30,60,1440};
    static void channel(Context c) { NotificationManager nm=c.getSystemService(NotificationManager.class); nm.createNotificationChannel(new NotificationChannel(CHANNEL,"Avisos de recordatorios",NotificationManager.IMPORTANCE_HIGH)); }
    static PendingIntent pending(Context c,long id,int slot) { Intent i=new Intent(c,AlarmReceiver.class).putExtra("id",id).putExtra("slot",slot); return PendingIntent.getBroadcast(c,request(id,slot),i,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE); }
    static int request(long id,int slot) { return (int)((id*7+slot)&0x7fffffff); }
    static void cancel(Context c,long id) { AlarmManager am=c.getSystemService(AlarmManager.class); for(int slot=0;slot<5;slot++) am.cancel(pending(c,id,slot)); }
    static void schedule(Context c,Store.Item item) {
        cancel(c,item.id); if(item.done)return;
        AlarmManager am=c.getSystemService(AlarmManager.class);
        for(int slot=0;slot<MINUTES.length;slot++) {
            if((Integer.parseInt(item.offsets)&(1<<slot))==0)continue;
            long at=item.whenMs-MINUTES[slot]*60000L; if(at<=System.currentTimeMillis())continue;
            PendingIntent p=pending(c,item.id,slot);
            if(Build.VERSION.SDK_INT>=31 && !am.canScheduleExactAlarms())am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,at,p);
            else am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,at,p);
        }
    }
    static void rescheduleAll(Context c) { Store s=new Store(c); for(Store.Item i:s.all()) { if(!i.done && !"none".equals(i.repeat) && i.whenMs<System.currentTimeMillis()) { i.whenMs=next(i.whenMs,i.repeat); s.save(i); } schedule(c,i); } }
    static long next(long start,String repeat) {
        ZonedDateTime d=Instant.ofEpochMilli(start).atZone(ZoneId.systemDefault()); long now=System.currentTimeMillis();
        do { if("daily".equals(repeat))d=d.plusDays(1); else if("weekly".equals(repeat))d=d.plusWeeks(1); else if("monthly".equals(repeat))d=d.plusMonths(1); else break; } while(d.toInstant().toEpochMilli()<=now);
        return d.toInstant().toEpochMilli();
    }
}
