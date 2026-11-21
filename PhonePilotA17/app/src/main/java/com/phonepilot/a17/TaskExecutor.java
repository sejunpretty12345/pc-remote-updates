package com.phonepilot.a17;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.ComponentName;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.media.AudioManager;
import android.net.Uri;
import android.provider.AlarmClock;
import android.provider.CalendarContract;
import android.provider.MediaStore;
import android.provider.Settings;
import android.os.Build;
import java.util.Calendar;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class TaskExecutor {
    private TaskExecutor(){}
    public static String run(Context context,PilotCommand task) {
        if(PilotStore.stopped(context) && !"settings".equals(task.type))return "긴급 중지가 활성화되어 있습니다.";
        try {
            switch(task.type) {
                case "open":return openApp(context,task.target);
                case "youtube": {
                    Uri url=Uri.parse("https://www.youtube.com/results?search_query="+Uri.encode(task.text));
                    Intent youtube=new Intent(Intent.ACTION_VIEW,url).setPackage("com.google.android.youtube");
                    start(context,canOpen(context,youtube)?youtube:new Intent(Intent.ACTION_VIEW,url));
                    return "유튜브 검색 화면 열기를 요청했습니다. 검색 결과는 유튜브에서 확인해 주세요.";
                }
                case "web": {
                    Uri url=Uri.parse(("google".equals(task.target)?"https://www.google.com/search?q=":"https://search.naver.com/search.naver?query=")+Uri.encode(task.text));
                    start(context,new Intent(Intent.ACTION_VIEW,url));
                    return "브라우저에서 검색 화면을 열었습니다.";
                }
                case "alarm": {
                    Intent a=new Intent(AlarmClock.ACTION_SET_ALARM);
                    if(task.hour>=0){a.putExtra(AlarmClock.EXTRA_HOUR,task.hour);a.putExtra(AlarmClock.EXTRA_MINUTES,Math.max(0,task.minute));}
                    if(!task.text.isEmpty())a.putExtra(AlarmClock.EXTRA_MESSAGE,task.text);
                    a.putExtra(AlarmClock.EXTRA_SKIP_UI,false);
                    start(context,a);
                    return "시계 앱에 알람 설정을 요청했습니다. 알람이 저장됐는지 확인해 주세요.";
                }
                case "timer": {
                    Intent timer=new Intent(AlarmClock.ACTION_SET_TIMER);
                    if(task.seconds>0)timer.putExtra(AlarmClock.EXTRA_LENGTH,task.seconds);
                    timer.putExtra(AlarmClock.EXTRA_SKIP_UI,false);
                    start(context,timer);
                    return "시계 앱에 타이머 설정을 요청했습니다. 설정 화면에서 확인해 주세요.";
                }
                case "calendar": {
                    Intent calendar=new Intent(Intent.ACTION_INSERT).setData(CalendarContract.Events.CONTENT_URI);
                    if(!task.title.isEmpty())calendar.putExtra(CalendarContract.Events.TITLE,task.title);
                    if(!task.body.isEmpty())calendar.putExtra(CalendarContract.Events.DESCRIPTION,task.body);
                    if(task.hour>=0){
                        Calendar date=Calendar.getInstance();date.set(Calendar.HOUR_OF_DAY,task.hour);date.set(Calendar.MINUTE,Math.max(0,task.minute));date.set(Calendar.SECOND,0);
                        if(date.getTimeInMillis()<System.currentTimeMillis())date.add(Calendar.DATE,1);
                        calendar.putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME,date.getTimeInMillis());
                        calendar.putExtra(CalendarContract.EXTRA_EVENT_END_TIME,date.getTimeInMillis()+3600000L);
                    }
                    start(context,calendar);
                    return "일정 작성 화면을 열었습니다. 저장은 캘린더에서 완료해 주세요.";
                }
                case "settings": {
                    if("stop".equals(task.target)) {
                        PilotStore.pref(context).edit().putBoolean("stopped",true).apply();
                        return "긴급 중지: 새 명령 실행을 차단했습니다.";
                    }
                    Intent settings;
                    switch(task.target) {
                        case "wifi":settings=new Intent(Build.VERSION.SDK_INT>=29?Settings.Panel.ACTION_WIFI:Settings.ACTION_WIFI_SETTINGS);break;
                        case "bluetooth":settings=new Intent(Settings.ACTION_BLUETOOTH_SETTINGS);break;
                        case "display":settings=new Intent(Settings.ACTION_DISPLAY_SETTINGS);break;
                        case "battery":settings=new Intent(Settings.ACTION_BATTERY_SAVER_SETTINGS);break;
                        default:settings=new Intent(Settings.ACTION_SETTINGS);break;
                    }
                    start(context,settings);
                    return "설정 화면을 열었습니다. 설정 변경은 직접 확인 후 진행해 주세요.";
                }
                case "volume": {
                    AudioManager audio=(AudioManager)context.getSystemService(Context.AUDIO_SERVICE);
                    int current=audio.getStreamVolume(AudioManager.STREAM_MUSIC);
                    int limit=audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
                    int delta=Math.max(1,limit/8);
                    int next="mute".equals(task.target)?0:"down".equals(task.target)?Math.max(0,current-delta):Math.min(limit,current+delta);
                    audio.setStreamVolume(AudioManager.STREAM_MUSIC,next,AudioManager.FLAG_SHOW_UI);
                    return "미디어 음량을 변경했습니다 ("+next+"/"+limit+").";
                }
                case "dial": {
                    start(context,new Intent(Intent.ACTION_DIAL,Uri.parse("tel:"+Uri.encode(task.text))));
                    return "전화번호 입력 화면을 열었습니다. 통화는 직접 시작해야 합니다.";
                }
                case "sms": {
                    Intent sms=new Intent(Intent.ACTION_SENDTO,Uri.parse("smsto:"));
                    if(!task.text.isEmpty())sms.putExtra("sms_body",task.text);
                    start(context,sms);
                    return "문자 작성 화면을 열었습니다. 자동으로 전송하지 않습니다.";
                }
                case "maps": {
                    Uri loc=Uri.parse("geo:0,0?q="+Uri.encode(task.text));
                    start(context,new Intent(Intent.ACTION_VIEW,loc));
                    return "지도 검색 화면을 열었습니다.";
                }
                case "note": {
                    Intent share=new Intent(Intent.ACTION_SEND);
                    share.setType("text/plain");
                    share.putExtra(Intent.EXTRA_SUBJECT,task.title.isEmpty()?"PhonePilot 메모":task.title);
                    share.putExtra(Intent.EXTRA_TEXT,(task.title.isEmpty()?"":task.title+"\n\n")+(task.body.isEmpty()?task.text:task.body));
                    start(context,Intent.createChooser(share,"삼성 노트 등 저장할 앱 선택"));
                    return "공유 화면을 열었습니다. 삼성 노트를 선택하고 저장해 주세요. 자동 저장은 아닙니다.";
                }
                case "localnote": {
                    String content=task.body.isEmpty()?task.text:task.body;
                    String name=task.title.isEmpty()?"빠른 메모":task.title;
                    if(content.isBlank())return "저장할 메모 내용이 없습니다.";
                    PilotStore.saveNote(context,name,content);
                    return "PhonePilot 자체 메모에 ‘"+name+"’을 저장했습니다. 삼성 노트와는 별도입니다.";
                }
                case "findnote": {
                    String found=PilotStore.findNote(context,task.target);
                    return found==null?"저장된 메모를 찾지 못했습니다.":found;
                }
                case "share": {
                    Intent share=new Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT,task.text);
                    start(context,Intent.createChooser(share,"공유할 앱 선택"));
                    return "공유 화면을 열었습니다.";
                }
                case "camera": {
                    start(context,new Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA));
                    return "카메라를 열었습니다. 사진 촬영은 직접 진행합니다.";
                }
                case "home": {
                    Intent home=new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME);
                    start(context,home);
                    return "홈 화면으로 이동을 요청했습니다.";
                }
                case "answer":return task.text;
                default:return "지원하지 않는 명령입니다.";
            }
        }catch(Exception e){return "동작 실패: "+e.getClass().getSimpleName()+". 이 앱 또는 기기가 요청을 지원하지 않을 수 있습니다.";}
    }
    public static String openApp(Context context,String target) {
        if(target==null||target.isBlank())return "앱 이름을 알려주세요.";
        String name=target.trim().toLowerCase(Locale.ROOT);
        String pkg;
        if(name.contains("유튜브")||name.equals("youtube"))pkg="com.google.android.youtube";
        else if(name.contains("삼성 노트")||name.equals("노트"))pkg="com.samsung.android.app.notes";
        else if(name.contains("크롬")||name.equals("chrome"))pkg="com.android.chrome";
        else if(name.contains("캘린더"))pkg="com.samsung.android.calendar";
        else if(name.contains("카메라"))return run(context,new PilotCommand("camera","","","","",-1,-1,0));
        else if(name.equals("설정"))return run(context,new PilotCommand("settings","general","","","",-1,-1,0));
        else pkg=null;
        if(pkg!=null){
            Intent intent=context.getPackageManager().getLaunchIntentForPackage(pkg);
            if(intent!=null){start(context,intent);return target+" 앱 열기를 요청했습니다.";}
        }
        // Manifest <queries> enables querying apps with a normal launcher, without QUERY_ALL_PACKAGES.
        List<ResolveInfo> apps=visibleApps(context);
        ResolveInfo candidate=null;
        int best=0;
        for(ResolveInfo info:apps){
            String label=info.loadLabel(context.getPackageManager()).toString().toLowerCase(Locale.ROOT);
            String id=info.activityInfo.packageName.toLowerCase(Locale.ROOT);
            int score=label.equals(name)?100:label.replace(" ","").equals(name.replace(" ",""))?95:label.contains(name)?75:id.contains(name)?30:0;
            if(score>best){best=score;candidate=info;}
        }
        if(candidate==null||best==0)return "‘"+target+"’ 앱을 찾지 못했습니다. ‘앱 목록’에서 확인해 주세요.";
        Intent launch=new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);
        launch.setComponent(new ComponentName(candidate.activityInfo.packageName,candidate.activityInfo.name));
        start(context,launch);
        return candidate.loadLabel(context.getPackageManager())+" 앱 열기를 요청했습니다.";
    }
    public static List<ResolveInfo> visibleApps(Context context) {
        Intent query=new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);
        return context.getPackageManager().queryIntentActivities(query,0);
    }
    private static boolean canOpen(Context c,Intent i){return i.resolveActivity(c.getPackageManager())!=null;}
    private static void start(Context c,Intent i){i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);c.startActivity(i);}
}
