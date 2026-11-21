package com.phonepilot.a17;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.InputType;
import android.view.*;
import android.widget.*;

public class MainActivity extends Activity {
    private final int NAVY=Color.rgb(13,22,38), PANEL=Color.rgb(27,38,57), CYAN=Color.rgb(105,228,210);
    private EditText editor;
    private TextView status,log;
    private boolean showedPending=false;

    @Override public void onCreate(Bundle b){super.onCreate(b);getWindow().setStatusBarColor(NAVY);getWindow().setNavigationBarColor(NAVY);buildUi();}
    private GradientDrawable bg(int color,int radius){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(radius);return d;}
    private void buildUi(){
        ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);scroll.setBackgroundColor(NAVY);
        LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(34,36,34,52);
        scroll.addView(root);setContentView(scroll);
        TextView kicker=text("PHONEPILOT  /  GALAXY A17",12,CYAN,true);root.addView(kicker);
        TextView title=text("휴대폰을 말로 조작하세요",25,Color.WHITE,true);title.setPadding(0,12,0,8);root.addView(title);
        TextView sub=text("유튜브 검색 · 삼성 노트 작성 · 브라우저 검색\nChatGPT ↔ 캘린더 ↔ 이 앱 (실행 전 승인)",13,0xffb1bdcb,false);sub.setLineSpacing(4,1.05f);root.addView(sub);
        status=text("연결 상태 확인 중",13,CYAN,true);status.setPadding(0,26,0,16);root.addView(status);
        editor=new EditText(this);editor.setTextColor(Color.WHITE);editor.setHintTextColor(0xff9caabc);editor.setHint("예: 유튜브에서 뚜아뚜지 검색해줘");editor.setTextSize(15);editor.setMinLines(3);editor.setGravity(Gravity.TOP);editor.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_MULTI_LINE);editor.setPadding(20,20,20,20);editor.setBackground(bg(PANEL,22));root.addView(editor,new LinearLayout.LayoutParams(-1,-2));
        button(root,"명령 확인 및 실행",CYAN,NAVY,()->{
            try{approve(PilotCommand.fromText(editor.getText().toString()),false);}catch(Exception e){toast(e.getMessage());}
        });
        addHeading(root,"연결 및 권한");
        button(root,"① 접근성 서비스 켜기",PANEL,Color.WHITE,()->startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)));
        button(root,"② 캘린더·알림 권한 허용",PANEL,Color.WHITE,this::requestRequiredPermissions);
        button(root,"③ ChatGPT 명령 수신 시작",PANEL,Color.WHITE,()->{
            if(checkSelfPermission(Manifest.permission.READ_CALENDAR)!=PackageManager.PERMISSION_GRANTED){requestRequiredPermissions();return;}
            if(Build.VERSION.SDK_INT>=26)startForegroundService(new Intent(this,CalendarBridgeService.class));else startService(new Intent(this,CalendarBridgeService.class));
            toast("명령 수신 서비스 실행됨");refresh();
        });
        button(root,"새 캘린더 명령 확인",PANEL,Color.WHITE,()->{toast(CalendarBridgeService.scanCalendar(this));showedPending=false;showPending();refresh();});
        addHeading(root,"안전 제어");
        button(root,"긴급 중지 · 모든 새 명령 차단",0xff793949,Color.WHITE,()->{
            PilotStore.pref(this).edit().putBoolean("stopped",true).remove("pending").apply();
            if(PilotAccessibilityService.instance!=null)PilotAccessibilityService.instance.stopNote("사용자가 긴급 중지");
            stopService(new Intent(this,CalendarBridgeService.class));
            PilotStore.log(this,"긴급 중지 활성화");refresh();
        });
        button(root,"중지 해제",PANEL,Color.WHITE,()->{PilotStore.pref(this).edit().putBoolean("stopped",false).apply();PilotStore.log(this,"긴급 중지 해제");refresh();});
        addHeading(root,"작업 기록");
        log=text("",12,0xffbcc6d3,false);log.setPadding(0,14,0,16);root.addView(log);
        TextView disclaimer=text("이 앱은 미검증 개발 버전입니다. Samsung Notes 화면에 따라 입력이나 저장이 실패할 수 있습니다.\n접근성 서비스는 실행 승인된 작업에만 사용하며 화면 내용을 외부로 전송하지 않습니다.",11,0xff97a5b5,false);root.addView(disclaimer);
        refresh();
    }
    private TextView text(String s,int size,int color,boolean bold){TextView v=new TextView(this);v.setText(s);v.setTextColor(color);v.setTextSize(size);if(bold)v.setTypeface(null,Typeface.BOLD);return v;}
    private void addHeading(LinearLayout p,String s){TextView t=text(s,16,Color.WHITE,true);t.setPadding(0,30,0,8);p.addView(t);}
    private void button(LinearLayout p,String label,int bg,int foreground,Runnable task){
        TextView b=text(label,14,foreground,true);b.setGravity(Gravity.CENTER);b.setBackground(bg(bg,17));b.setPadding(14,20,14,20);
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.topMargin=10;p.addView(b,lp);b.setOnClickListener(v->task.run());
    }
    private void requestRequiredPermissions(){
        if(Build.VERSION.SDK_INT>=33)requestPermissions(new String[]{Manifest.permission.READ_CALENDAR,Manifest.permission.POST_NOTIFICATIONS},42);
        else requestPermissions(new String[]{Manifest.permission.READ_CALENDAR},42);
    }
    @Override public void onRequestPermissionsResult(int code,String[] permissions,int[] results){super.onRequestPermissionsResult(code,permissions,results);refresh();}
    @Override public void onResume(){super.onResume();refresh();showPending();}
    @Override protected void onNewIntent(Intent intent){super.onNewIntent(intent);showedPending=false;showPending();}
    private void refresh(){
        if(status==null)return;
        status.setText(PilotStore.stopped(this)?"● 긴급 중지 활성화":
            "● 접근성: "+(PilotAccessibilityService.instance!=null?"연결됨":"설정 필요")+"  |  캘린더: "+
            (checkSelfPermission(Manifest.permission.READ_CALENDAR)==PackageManager.PERMISSION_GRANTED?"허용":"권한 필요"));
        if(log!=null)log.setText(PilotStore.pref(this).getString("log","아직 작업 기록이 없습니다."));
    }
    private void showPending(){
        if(showedPending||PilotStore.stopped(this))return;
        String payload=PilotStore.pref(this).getString("pending",null);
        if(payload==null)return;
        try{showedPending=true;approve(PilotCommand.fromJson(payload),true);}
        catch(Exception ex){PilotStore.pref(this).edit().remove("pending").apply();toast("명령 확인 실패: "+ex.getMessage());}
    }
    private void approve(PilotCommand task, boolean fromCalendar){
        if(PilotStore.stopped(this)){toast("긴급 중지를 먼저 해제해 주세요");return;}
        new AlertDialog.Builder(this).setTitle("이 작업을 실행할까요?")
            .setMessage(task.summary()+"\n\n실행 전 승인이 필요합니다.")
            .setPositiveButton("승인 및 실행",(d,w)->{
                if(fromCalendar)PilotStore.pref(this).edit().remove("pending").apply();
                String result=TaskExecutor.run(this,task);toast(result);refresh();
            })
            .setNegativeButton("취소",(d,w)->{
                if(fromCalendar)PilotStore.pref(this).edit().remove("pending").apply();
                PilotStore.log(this,"작업 취소: "+task.type);refresh();
            }).show();
    }
    private void toast(String s){Toast.makeText(this,s,Toast.LENGTH_LONG).show();}
}
