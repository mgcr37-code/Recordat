package com.manuel.recordatorios;

import android.app.*;
import android.content.*;

public final class ActionReceiver extends android.content.BroadcastReceiver {
    @Override public void onReceive(Context c,Intent in) {
        long id=in.getLongExtra("id",-1); Store s=new Store(c); Store.Item i=s.get(id); if(i==null)return;
        if("done".equals(in.getAction())) { i.done=true; s.save(i); Scheduler.cancel(c,id); }
        else if("snooze".equals(in.getAction())) {
            Intent alarm=new Intent(c,AlarmReceiver.class).putExtra("id",id).putExtra("slot",5);
            PendingIntent p=PendingIntent.getBroadcast(c,Scheduler.request(id,5),alarm,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
            AlarmManager am=c.getSystemService(AlarmManager.class); long at=System.currentTimeMillis()+600000;
            if(android.os.Build.VERSION.SDK_INT>=31&&!am.canScheduleExactAlarms())am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,at,p); else am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,at,p);
        }
        c.getSystemService(NotificationManager.class).cancel(Scheduler.request(id,0));
    }
}
