package com.manuel.recordatorios;

import android.content.*;
import android.database.Cursor;
import android.database.sqlite.*;
import java.util.*;

final class Store extends SQLiteOpenHelper {
    static final class Item {
        long id, whenMs; String title, offsets, repeat; boolean done;
        Item(long id, String title, long whenMs, String offsets, String repeat, boolean done) { this.id=id; this.title=title; this.whenMs=whenMs; this.offsets=offsets; this.repeat=repeat; this.done=done; }
    }
    Store(Context c) { super(c, "reminders.db", null, 1); }
    @Override public void onCreate(SQLiteDatabase db) { db.execSQL("CREATE TABLE reminders (id INTEGER PRIMARY KEY AUTOINCREMENT, title TEXT NOT NULL, when_ms INTEGER NOT NULL, offsets TEXT NOT NULL, repeat_rule TEXT NOT NULL, done INTEGER NOT NULL DEFAULT 0)"); }
    @Override public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {}
    long save(Item i) {
        ContentValues v=new ContentValues(); v.put("title",i.title); v.put("when_ms",i.whenMs); v.put("offsets",i.offsets); v.put("repeat_rule",i.repeat); v.put("done",i.done?1:0);
        if(i.id==0) return getWritableDatabase().insertOrThrow("reminders",null,v);
        getWritableDatabase().update("reminders",v,"id=?",new String[]{String.valueOf(i.id)}); return i.id;
    }
    Item get(long id) { try(Cursor c=getReadableDatabase().rawQuery("SELECT * FROM reminders WHERE id=?",new String[]{String.valueOf(id)})) { return c.moveToFirst()?read(c):null; } }
    List<Item> all() { List<Item> result=new ArrayList<>(); try(Cursor c=getReadableDatabase().rawQuery("SELECT * FROM reminders ORDER BY done ASC, when_ms ASC",null)) { while(c.moveToNext()) result.add(read(c)); } return result; }
    void delete(long id) { getWritableDatabase().delete("reminders","id=?",new String[]{String.valueOf(id)}); }
    private Item read(Cursor c) { return new Item(c.getLong(c.getColumnIndexOrThrow("id")),c.getString(c.getColumnIndexOrThrow("title")),c.getLong(c.getColumnIndexOrThrow("when_ms")),c.getString(c.getColumnIndexOrThrow("offsets")),c.getString(c.getColumnIndexOrThrow("repeat_rule")),c.getInt(c.getColumnIndexOrThrow("done"))!=0); }
}
