package com.phonepilot.a17;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import java.util.Locale;

public final class TaskExecutor {
    private TaskExecutor() {}
    public static String run(Context context,PilotCommand command) {
        if(PilotStore.stopped(context))return "긴급 중지 상태입니다";
        try {
            switch(command.type) {
                case "youtube": {
                    Uri url=Uri.parse("https://www.youtube.com/results?search_query="+Uri.encode(command.value));
                    Intent app=new Intent(Intent.ACTION_VIEW,url).setPackage("com.google.android.youtube");
                    Intent fallback=new Intent(Intent.ACTION_VIEW,url);
                    launch(context,resolvable(context,app)?app:fallback);
                    PilotStore.log(context,"유튜브 검색 열기 요청: "+command.value);
                    return "유튜브 검색 페이지 열기를 요청했습니다";
                }
                case "web": {
                    Uri url=Uri.parse("https://search.naver.com/search.naver?query="+Uri.encode(command.value));
                    Intent chrome=new Intent(Intent.ACTION_VIEW,url).setPackage("com.android.chrome");
                    Intent fallback=new Intent(Intent.ACTION_VIEW,url);
                    launch(context,resolvable(context,chrome)?chrome:fallback);
                    PilotStore.log(context,"네이버 검색 열기 요청: "+command.value);
                    return "네이버 검색 페이지 열기를 요청했습니다";
                }
                case "open": {
                    String pkg;
                    switch(command.value.toLowerCase(Locale.ROOT)) {
                        case "유튜브": case "youtube":pkg="com.google.android.youtube";break;
                        case "삼성 노트": case "노트":pkg="com.samsung.android.app.notes";break;
                        case "크롬": case "chrome":pkg="com.android.chrome";break;
                        case "네이버": case "naver":pkg="com.nhn.android.search";break;
                        case "캘린더":pkg="com.samsung.android.calendar";break;
                        default:return "지원하지 않는 앱입니다";
                    }
                    Intent i=context.getPackageManager().getLaunchIntentForPackage(pkg);
                    if(i==null)return "앱을 찾을 수 없습니다: "+command.value;
                    launch(context,i);
                    PilotStore.log(context,"앱 실행 요청: "+command.value);
                    return command.value+" 앱 실행을 요청했습니다";
                }
                case "note": {
                    Intent share=new Intent(Intent.ACTION_SEND);
                    share.setType("text/plain");
                    share.putExtra(Intent.EXTRA_SUBJECT,command.title);
                    share.putExtra(Intent.EXTRA_TEXT,command.title+"\n\n"+command.body);
                    Intent chooser=Intent.createChooser(share,"메모를 저장할 앱을 선택하세요");
                    launch(context,chooser);
                    PilotStore.log(context,"노트 공유 화면 열기 요청");
                    return "공유 창을 열었습니다. 삼성 노트를 선택해 직접 저장해 주세요";
                }
                case "home": {
                    Intent home=new Intent(Intent.ACTION_MAIN);
                    home.addCategory(Intent.CATEGORY_HOME);
                    launch(context,home);
                    PilotStore.log(context,"홈 화면 열기 요청");
                    return "홈 화면 열기를 요청했습니다";
                }
                default:return "지원하지 않는 명령";
            }
        }catch(Exception ex) {
            PilotStore.log(context,"실행 오류: "+ex.getClass().getSimpleName());
            return "실행 실패: "+ex.getMessage();
        }
    }
    private static boolean resolvable(Context c,Intent i) {
        return i.resolveActivity(c.getPackageManager())!=null;
    }
    private static void launch(Context c,Intent i) {
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        c.startActivity(i);
    }
}