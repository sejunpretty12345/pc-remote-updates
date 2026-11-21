package com.phonepilot.a17;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.*;
import android.provider.CalendarContract;

/** Calendar events with an exact prefix and protocol carry commands. Never executes without approval. */
public class CalendarBridgeService extends Service {
    public static final String CHANNEL="phonepilot_channel";
    private final Handler handler=new Handler(Looper.getMainLooper());
    private static final String PREFIX="[PhonePilot]";
    private static final int NOTIFICATION_ID=1217;
    private final Runnable poll=new Runnable(){@Override public void run(){
        if(!PilotStore.stopped(CalendarBridgeService.this))scanCalendar(CalendarBridgeService.this);
        handler.postDelayed(this,30000);
    }};

    @Override public void onCreate(){
        super.onCreate();
        NotificationManager nm=(NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        nm.createNotificationChannel(new NotificationChannel(CHANNEL,"PhonePilot 연결",NotificationManager.IMPORTANCE_DEFAULT));
        Intent open=new Intent(this,MainActivity.class);
        PendingIntent pi=PendingIntent.getActivity(this,0,open,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        Notification n=new Notification.Builder(this,CHANNEL)
            .setSmallIcon(android.R.drawable.ic_menu_my_calendar)
            .setContentTitle("PhonePilot 명령 수신 중")
            .setContentText("Google 캘린더 명령을 확인합니다. 실행 전 승인이 필요합니다")
            .setOngoing(true).setContentIntent(pi).build();
        startForeground(NOTIFICATION_ID,n);
        handler.post(poll);
        PilotStore.log(this,"캘린더 연결 서비스 시작");
    }
    @Override public int onStartCommand(Intent i,int f,int startId){return START_STICKY;}
    @Override public void onDestroy(){handler.removeCallbacksAndMessages(null);PilotStore.log(this,"연결 서비스 종료");super.onDestroy();}
    @Override public IBinder onBind(Intent intent){return null;}

    public static String scanCalendar(Context c){
        if(c.checkSelfPermission(Manifest.permission.READ_CALENDAR)!=PackageManager.PERMISSION_GRANTED)return "캘린더 읽기 권한이 필요합니다";
        if(PilotStore.stopped(c))return "긴급 중지 상태입니다";
        long now=System.currentTimeMillis();
        String[] cols={CalendarContract.Events._ID,CalendarContract.Events.TITLE,CalendarContract.Events.DESCRIPTION,CalendarContract.Events.DTSTART};
        String condition=CalendarContract.Events.DTSTART+">=? AND "+CalendarContract.Events.DTSTART+"<=? AND "+CalendarContract.Events.DELETED+"=0";
        String[] args={String.valueOf(now-10*60*1000),String.valueOf(now+15*60*1000)};
        long newestStart=0;String foundId=null,foundPayload=null;
        try(Cursor rows=c.getContentResolver().query(CalendarContract.Events.CONTENT_URI,cols,condition,args,CalendarContract.Events.DTSTART+" DESC")){
            if(rows==null)return "캘린더가 동기화되지 않았습니다";
            int checked=0;
            while(rows.moveToNext() && checked++<200){
                long id=rows.getLong(0),start=rows.getLong(3);
                String title=rows.getString(1),desc=rows.getString(2);
                if(title==null||!title.startsWith(PREFIX)||desc==null||start<newestStart)continue;
                if(!desc.startsWith("{\"protocol\":\"phonepilot.v1\""))continue;
                String key=id+":"+start;
                if(key.equals(PilotStore.pref(c).getString("seen","")))continue;
                try {PilotCommand.fromJson(desc);} catch(Exception bad){PilotStore.log(c,"잘못된 캘린더 명령 건너뜀");continue;}
                newestStart=start;foundId=key;foundPayload=desc;
            }
            if(foundId!=null){
                PilotStore.pref(c).edit().putString("seen",foundId).putString("pending",foundPayload).apply();
                PilotStore.log(c,"명령 수신 완료 · 사용자 승인 대기");
                notifyApproval(c);
                return "새 명령 수신: 승인 대기";
            }
            return "새로운 명령이 없습니다 (휴대폰 캘린더 동기화 상태 확인)";
        }catch(Exception e){PilotStore.log(c,"캘린더 확인 실패: "+e.getMessage());return "캘린더 오류: "+e.getMessage();}
    }
    private static void notifyApproval(Context c){
        NotificationManager nm=(NotificationManager)c.getSystemService(NOTIFICATION_SERVICE);
        nm.createNotificationChannel(new NotificationChannel(CHANNEL,"PhonePilot 연결",NotificationManager.IMPORTANCE_DEFAULT));
        Intent in=new Intent(c,MainActivity.class).putExtra("show_pending",true).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pi=PendingIntent.getActivity(c,101,in,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        Notification n=new Notification.Builder(c,CHANNEL).setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("PhonePilot 명령 도착")
            .setContentText("눌러서 내용을 확인하고 승인하세요")
            .setAutoCancel(true).setContentIntent(pi).build();
        nm.notify(101,n);
    }
}
