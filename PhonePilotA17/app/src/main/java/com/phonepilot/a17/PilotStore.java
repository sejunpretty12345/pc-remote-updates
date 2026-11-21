package com.phonepilot.a17;

import android.content.Context;
import android.content.SharedPreferences;

public final class PilotStore {
    private PilotStore(){}
    public static SharedPreferences pref(Context c){ return c.getSharedPreferences("phonepilot",Context.MODE_PRIVATE); }
    public static void log(Context c,String message){
        SharedPreferences p=pref(c);
        String old=p.getString("log","");
        String next=new java.text.SimpleDateFormat("MM-dd HH:mm:ss",java.util.Locale.KOREA).format(new java.util.Date())+"  "+message+"\n"+old;
        p.edit().putString("log",next.substring(0,Math.min(6000,next.length()))).apply();
    }
    public static boolean stopped(Context c){ return pref(c).getBoolean("stopped",false); }
}
