package com.manuel.recordatorios;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.*;
import android.provider.Settings;
import android.speech.RecognizerIntent;
import android.view.*;
import android.widget.*;
import java.text.DateFormat;
import java.time.*;
import java.util.*;
import java.util.regex.*;

public class MainActivity extends Activity {
    private Store store; private LinearLayout list; private EditText draft;
    private static final int SPEECH=42;
    @Override public void onCreate(Bundle state) { super.onCreate(state); store=new Store(this); Scheduler.channel(this); showHome(); if(Build.VERSION.SDK_INT>=33&&checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},1); }
    private TextView label(String s,int size) { TextView v=new TextView(this); v.setText(s); v.setTextSize(size); v.setTextColor(Color.rgb(28,35,47)); v.setPadding(12,12,12,12); return v; }
    private LinearLayout column() { LinearLayout l=new LinearLayout(this); l.setOrientation(1); l.setPadding(18,18,18,18); l.setBackgroundColor(Color.rgb(248,249,252)); return l; }
    private Button button(String text,Runnable click) { Button b=new Button(this); b.setText(text); b.setAllCaps(false); b.setOnClickListener(v->click.run()); return b; }
    private void showHome() {
        LinearLayout root=column(); root.addView(label("Recordatorios",28)); root.addView(button("+ Crear recordatorio",()->editor(null)));
        ScrollView scroll=new ScrollView(this); list=column(); scroll.addView(list); root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1)); setContentView(root); refresh();
    }
    private void refresh() {
        list.removeAllViews(); List<Store.Item> items=store.all(); if(items.isEmpty())list.addView(label("Todavía no hay recordatorios.",18));
        for(Store.Item item:items) {
            LinearLayout row=column(); row.setBackgroundColor(Color.WHITE);
            row.addView(label((item.done?"✓  ":"●  ")+item.title,19));
            row.addView(label(DateFormat.getDateTimeInstance(DateFormat.MEDIUM,DateFormat.SHORT).format(new Date(item.whenMs))+" · "+repeatName(item.repeat)+(item.done?" · Realizado":""),14));
            row.addView(button("Editar",()->editor(item)));
            list.addView(row); View spacer=new View(this); list.addView(spacer,new LinearLayout.LayoutParams(1,10));
        }
    }
    private String repeatName(String r) { switch(r) { case "daily":return "Cada día"; case "weekly":return "Cada semana"; case "monthly":return "Cada mes"; default:return "Una vez"; } }
    private void editor(Store.Item current) {
        LinearLayout form=column(); form.addView(label(current==null?"Nuevo recordatorio":"Editar recordatorio",25));
        draft=new EditText(this); draft.setSingleLine(false); draft.setMinLines(2); draft.setHint("Ej.: Llamar a Juan mañana a las 3 de la tarde, avísame 30 minutos antes"); draft.setText(current==null?"":current.title); form.addView(draft);
        form.addView(button("🎤 Dictar",()->listen())); form.addView(label("Revisa la fecha y la hora antes de guardar.",14));
        Parsed parsed=current==null?parse(""):new Parsed(current.title,current.whenMs,0);
        DatePicker date=new DatePicker(this); Calendar cal=Calendar.getInstance(); cal.setTimeInMillis(parsed.when); date.init(cal.get(Calendar.YEAR),cal.get(Calendar.MONTH),cal.get(Calendar.DAY_OF_MONTH),null); form.addView(date);
        TimePicker time=new TimePicker(this); time.setIs24HourView(true); time.setHour(cal.get(Calendar.HOUR_OF_DAY)); time.setMinute(cal.get(Calendar.MINUTE)); form.addView(time);
        form.addView(label("Avisarme",19)); CheckBox[] checks=new CheckBox[5]; String[] labels={"A la hora indicada","15 minutos antes","30 minutos antes","1 hora antes","1 día antes"};
        int mask=current==null?3:Integer.parseInt(current.offsets);
        for(int k=0;k<5;k++){checks[k]=new CheckBox(this);checks[k].setText(labels[k]);checks[k].setChecked((mask&(1<<k))!=0);form.addView(checks[k]);}
        form.addView(label("Repetir",19)); Spinner repeat=new Spinner(this); String[] names={"Una vez","Cada día","Cada semana","Cada mes"}; repeat.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,names)); String[] rules={"none","daily","weekly","monthly"}; if(current!=null)for(int k=0;k<4;k++)if(rules[k].equals(current.repeat))repeat.setSelection(k); form.addView(repeat);
        draft.addTextChangedListener(new android.text.TextWatcher(){ public void beforeTextChanged(CharSequence s,int st,int count,int after){} public void onTextChanged(CharSequence s,int st,int before,int count){} public void afterTextChanged(android.text.Editable e){ if(current!=null)return; Parsed p=parse(e.toString()); if(p.confidence>0){ Calendar c=Calendar.getInstance(); c.setTimeInMillis(p.when); date.updateDate(c.get(Calendar.YEAR),c.get(Calendar.MONTH),c.get(Calendar.DAY_OF_MONTH));time.setHour(c.get(Calendar.HOUR_OF_DAY));time.setMinute(c.get(Calendar.MINUTE)); } if(p.offset>=0&&p.offset<5)checks[p.offset].setChecked(true); }});
        form.addView(button("Guardar",()->{
            String title=draft.getText().toString().trim(); if(title.isEmpty()){toast("Escribe el recordatorio");return;}
            Calendar c=Calendar.getInstance(); c.set(date.getYear(),date.getMonth(),date.getDayOfMonth(),time.getHour(),time.getMinute(),0);c.set(Calendar.MILLISECOND,0);
            if(c.getTimeInMillis()<=System.currentTimeMillis()){toast("Elige una fecha y hora futuras");return;}
            int bits=0;for(int k=0;k<5;k++)if(checks[k].isChecked())bits|=1<<k; if(bits==0){toast("Selecciona al menos un aviso");return;}
            Store.Item i=new Store.Item(current==null?0:current.id,title,c.getTimeInMillis(),String.valueOf(bits),rules[repeat.getSelectedItemPosition()],false);
            i.id=store.save(i); Scheduler.schedule(this,i); if(current!=null)getSystemService(NotificationManager.class).cancelAll(); showHome(); askExactPermission();
        }));
        if(current!=null) {
            form.addView(button(current.done?"Reactivar":"Marcar como realizado",()->{current.done=!current.done;store.save(current);Scheduler.schedule(this,current);showHome();}));
            form.addView(button("Eliminar",()->new AlertDialog.Builder(this).setMessage("¿Eliminar este recordatorio?").setNegativeButton("Cancelar",null).setPositiveButton("Eliminar",(d,w)->{Scheduler.cancel(this,current.id);store.delete(current.id);showHome();}).show()));
        }
        form.addView(button("Volver",this::showHome)); ScrollView scroll=new ScrollView(this);scroll.addView(form);setContentView(scroll);
    }
    private void askExactPermission() { if(Build.VERSION.SDK_INT>=31){AlarmManager am=getSystemService(AlarmManager.class);if(!am.canScheduleExactAlarms())new AlertDialog.Builder(this).setMessage("Para que los avisos lleguen a la hora indicada, permite las alarmas exactas de Recordatorios en los ajustes de Android.").setPositiveButton("Abrir ajustes",(d,w)->startActivity(new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).setData(android.net.Uri.parse("package:"+getPackageName())))).setNegativeButton("Después",null).show();} }
    private void listen() { Intent i=new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH); i.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);i.putExtra(RecognizerIntent.EXTRA_LANGUAGE,"es-ES");i.putExtra(RecognizerIntent.EXTRA_PROMPT,"Di tu recordatorio");try{startActivityForResult(i,SPEECH);}catch(Exception e){toast("No hay servicio de reconocimiento de voz disponible");} }
    @Override protected void onActivityResult(int req,int result,Intent data){super.onActivityResult(req,result,data);if(req==SPEECH&&result==RESULT_OK&&data!=null){ArrayList<String> words=data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);if(words!=null&&!words.isEmpty()&&draft!=null)draft.setText(words.get(0));}}
    private void toast(String s){Toast.makeText(this,s,Toast.LENGTH_LONG).show();}
    private static class Parsed { String title; long when; int confidence,offset=-1; Parsed(String t,long w,int c){title=t;when=w;confidence=c;} }
    private Parsed parse(String input) {
        String s=input.toLowerCase(Locale.ROOT); ZonedDateTime now=ZonedDateTime.now(); ZonedDateTime at=now.plusDays(1).withHour(9).withMinute(0).withSecond(0).withNano(0); int confidence=0;
        if(s.contains("pasado mañana")||s.contains("pasado manana")){at=now.plusDays(2);confidence++;}else if(s.contains("mañana")||s.contains("manana")){at=now.plusDays(1);confidence++;}else if(s.contains("hoy")){at=now;confidence++;}
        Matcher d=Pattern.compile("\\b(\\d{1,2})[/.-](\\d{1,2})(?:[/.-](\\d{2,4}))?\\b").matcher(s);
        if(d.find())try{int year=d.group(3)==null?now.getYear():Integer.parseInt(d.group(3));if(year<100)year+=2000;at=at.withYear(year).withMonth(Integer.parseInt(d.group(2))).withDayOfMonth(Integer.parseInt(d.group(1)));confidence++;}catch(Exception ignored){}
        Matcher h=Pattern.compile("(?:\\ba\\s+las?\\s+|\\b)(\\d{1,2})(?::(\\d{2}))?\\s*(am|pm|de la tarde|de la noche|de la mañana)?").matcher(s);
        while(h.find()){int hour=Integer.parseInt(h.group(1));if(hour>23||h.group(2)==null&&h.group(3)==null)continue;int minute=h.group(2)==null?0:Integer.parseInt(h.group(2));String mod=h.group(3);if(mod!=null&&(mod.contains("tarde")||mod.contains("noche")||mod.equals("pm"))&&hour<12)hour+=12;if(mod!=null&&(mod.equals("am")||mod.contains("mañana"))&&hour==12)hour=0;try{at=at.withHour(hour).withMinute(minute);confidence++;break;}catch(Exception ignored){}}
        Parsed p=new Parsed(input,at.toInstant().toEpochMilli(),confidence);
        if(s.matches(".*(?:av[ií]same|avisa).*?(?:un d[ií]a|1 d[ií]a).*antes.*"))p.offset=4;
        else if(s.matches(".*(?:av[ií]same|avisa).*?30 minutos?.*antes.*"))p.offset=2;
        else if(s.matches(".*(?:av[ií]same|avisa).*?15 minutos?.*antes.*"))p.offset=1;
        else if(s.matches(".*(?:av[ií]same|avisa).*?(?:una hora|1 hora).*antes.*"))p.offset=3;
        return p;
    }
}
