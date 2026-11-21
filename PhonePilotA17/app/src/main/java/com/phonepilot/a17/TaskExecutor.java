package com.phonepilot.a17;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

public final class TaskExecutor {
    private TaskExecutor(){}
    public static String run(Context c,PilotCommand task) {
        if(PilotStore.stopped(c)) return "긴급 중지 중입니다";
        try {
            switch(task.type){
                case "youtube": {
                    Uri u=Uri.parse("https://www.youtube.com/results?search_query="+Uri.encode(task.value));
                    Intent youtube=new Intent(Intent.ACTION_VIEW,u).setPackage("com.google.android.youtube");
                    if(youtube.resolveActivity(c.getPackageManager())!=null) launch(c,youtube);
                    else launch(c,new Intent(Intent.ACTION_VIEW,u));
                    PilotStore.log(c,"유튜브 검색 화면 열기 요청: "+task.value);
                    return "유튜브 검색 화면 열기 요청 완료 (검색 결과 표시 여부는 기기에서 확인)";
                }
                case "web": {
                    Uri u=Uri.parse("https://search.naver.com/search.naver?query="+Uri.encode(task.value));
                    Intent browser=new Intent(Intent.ACTION_VIEW,u).setPackage("com.android.chrome");
                    if(browser.resolveActivity(c.getPackageManager())!=null) launch(c,browser);
                    else launch(c,new Intent(Intent.ACTION_VIEW,u));
                    PilotStore.log(c,"브라우저 검색 요청: "+task.value);
                    return "네이버 검색 페이지 열기 요청 완료";
                }
                case "open": {
                    String pkg;
                    switch(task.value.toLowerCase(java.util.Locale.ROOT)) {
                        case "유튜브": case "youtube": pkg="com.google.android.youtube"; break;
                        case "삼성 노트": case "노트": pkg="com.samsung.android.app.notes"; break;
                        case "네이버": case "naver": pkg="com.nhn.android.search"; break;
                        case "캘린더": pkg="com.samsung.android.calendar"; break;
                        case "크롬": case "chrome": pkg="com.android.chrome"; break;
                        default: return "허용되지 않는 앱입니다";
                    }
                    Intent i=c.getPackageManager().getLaunchIntentForPackage(pkg);
                    if(i==null) return "앱을 찾을 수 없습니다: "+task.value;
                    launch(c,i);
                    PilotStore.log(c,"앱 실행: "+task.value);
                    return task.value+" 앱 실행 요청 완료";
                }
                case "note": {
                    PilotAccessibilityService ac=PilotAccessibilityService.instance;
                    if(ac==null) return "접근성 서비스가 꺼져 있습니다. 설정에서 PhonePilot A17을 켜 주세요";
                    Intent i=c.getPackageManager().getLaunchIntentForPackage("com.samsung.android.app.notes");
                    if(i==null) return "삼성 노트 앱이 설치되어 있지 않거나 실행할 수 없습니다";
                    ac.beginNote(task.title,task.body);
                    launch(c,i);
                    PilotStore.log(c,"삼성 노트 작성 자동화 시작: "+task.title);
                    return "삼성 노트 작성 시도 시작. 노트 화면에서 입력·저장 결과를 확인하세요";
                }
                case "home": case "back": {
                    PilotAccessibilityService ac=PilotAccessibilityService.instance;
                    if(ac==null) return "접근성 서비스가 꺼져 있습니다";
                    boolean ok=ac.performGlobalAction(task.type.equals("home") ?
                            android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_HOME :
                            android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_BACK);
                    PilotStore.log(c,task.type+" 글로벌 액션: "+ok);
                    return ok?"화면 이동 요청 완료":"기기에서 화면 이동을 거부했습니다";
                }
                default: return "지원하지 않는 명령";
            }
        } catch (Exception e) {
            PilotStore.log(c,"오류: "+e.getClass().getSimpleName()+": "+e.getMessage());
            return "실행 오류: "+e.getMessage();
        }
    }
    private static void launch(Context c,Intent intent){intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);c.startActivity(intent);}
}
