package com.phonepilot.a17;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONObject;

public final class PilotStore {
    private PilotStore() {}
    public static SharedPreferences pref(Context c) {return c.getSharedPreferences("phonepilot_assistant",Context.MODE_PRIVATE);}
    public static boolean stopped(Context c) {return pref(c).getBoolean("stopped",false);}
    public static synchronized void log(Context c,String who,String line) {
        try {
            JSONArray old=new JSONArray(pref(c).getString("history","[]"));
            JSONArray current=new JSONArray();
            for(int i=Math.max(0,old.length()-39);i<old.length();i++)current.put(old.getJSONObject(i));
            current.put(new JSONObject().put("who",who).put("line",line).put("time",System.currentTimeMillis()));
            pref(c).edit().putString("history",current.toString()).apply();
        } catch(Exception ignored) {}
    }
    public static synchronized void saveNote(Context c,String title,String body) throws Exception {
        JSONArray old=new JSONArray(pref(c).getString("notes","[]"));
        JSONArray out=new JSONArray();
        boolean replaced=false;
        for(int i=0;i<old.length();i++) {
            JSONObject note=old.getJSONObject(i);
            if(note.optString("title").equals(title)) {
                out.put(new JSONObject().put("title",title).put("body",body).put("edited",System.currentTimeMillis()));
                replaced=true;
            } else out.put(note);
        }
        if(!replaced)out.put(new JSONObject().put("title",title).put("body",body).put("edited",System.currentTimeMillis()));
        pref(c).edit().putString("notes",out.toString()).apply();
    }
    public static JSONArray notes(Context c) {
        try{return new JSONArray(pref(c).getString("notes","[]"));}
        catch(Exception e){return new JSONArray();}
    }
    public static String findNote(Context c,String title) {
        JSONArray notes=notes(c);
        for(int i=notes.length()-1;i>=0;i--){
            JSONObject item=notes.optJSONObject(i);
            if(item!=null && (title.isEmpty()||item.optString("title").contains(title)))return item.optString("title")+"\n\n"+item.optString("body");
        }
        return null;
    }
}
