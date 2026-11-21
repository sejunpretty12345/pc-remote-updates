package com.phonepilot.a17;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.text.InputType;
import android.view.Gravity;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

public final class MainActivity extends Activity {
    private final int navy=Color.rgb(13,22,38);
    private final int panel=Color.rgb(27,38,57);
    private final int cyan=Color.rgb(105,228,210);
    private EditText editor;
    private TextView status;
    private TextView log;

    @Override public void onCreate(Bundle saved) {
        super.onCreate(saved);
        getWindow().setStatusBarColor(navy);
        getWindow().setNavigationBarColor(navy);
        showUi();
    }

    private GradientDrawable bg(int color,int radius) {
        GradientDrawable d=new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(radius);
        return d;
    }
    private TextView text(String value,int size,int color,boolean bold) {
        TextView v=new TextView(this);
        v.setText(value);
        v.setTextSize(size);
        v.setTextColor(color);
        if(bold)v.setTypeface(null,Typeface.BOLD);
        return v;
    }
    private void button(LinearLayout parent,String title,int color,int fg,Runnable action) {
        TextView v=text(title,14,fg,true);
        v.setGravity(Gravity.CENTER);
        v.setBackground(bg(color,17));
        v.setPadding(16,22,16,22);
        LinearLayout.LayoutParams params=new LinearLayout.LayoutParams(-1,-2);
        params.topMargin=12;
        parent.addView(v,params);
        v.setOnClickListener(view->action.run());
    }
    private void title(LinearLayout parent,String caption) {
        TextView v=text(caption,16,Color.WHITE,true);
        v.setPadding(0,27,0,6);
        parent.addView(v);
    }
    private void showUi() {
        ScrollView scroll=new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(navy);
        LinearLayout root=new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(32,38,32,50);
        scroll.addView(root);
        setContentView(scroll);
        root.addView(text("PHONEPILOT  /  GALAXY A17",12,cyan,true));
        TextView heading=text("안전한 휴대폰 바로가기",24,Color.WHITE,true);
        heading.setPadding(0,12,0,10);
        root.addView(heading);
        TextView sub=text("유튜브·네이버 검색과 앱 실행을 지원합니다. 삼성 노트는 공유 화면에서 직접 저장할 수 있습니다.\n\n이 버전에는 접근성 권한, 화면 제어, 원격 명령 수신이 없습니다.",13,0xffbac8d6,false);
        sub.setLineSpacing(4,1.06f);
        root.addView(sub);
        status=text("",13,cyan,true);
        status.setPadding(0,25,0,14);
        root.addView(status);
        editor=new EditText(this);
        editor.setTextColor(Color.WHITE);
        editor.setHintTextColor(0xff9caabc);
        editor.setHint("예: 유튜브에서 뚜아뚜지 검색해 줘");
        editor.setTextSize(15);
        editor.setMinLines(3);
        editor.setGravity(Gravity.TOP);
        editor.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        editor.setPadding(20,20,20,20);
        editor.setBackground(bg(panel,22));
        root.addView(editor,new LinearLayout.LayoutParams(-1,-2));
        button(root,"명령 확인 후 실행",cyan,navy,()->{
            try {
                PilotCommand command=PilotCommand.fromText(editor.getText().toString());
                approve(command);
            } catch(Exception error) {toast(error.getMessage());}
        });
        title(root,"빠른 실행");
        button(root,"YouTube 검색",panel,Color.WHITE,()->editor.setText("유튜브에서 뚜아뚜지 검색해 줘"));
        button(root,"네이버 검색",panel,Color.WHITE,()->editor.setText("네이버에서 오늘 날씨 검색해 줘"));
        button(root,"삼성 노트에 텍스트 보내기",panel,Color.WHITE,()->editor.setText("삼성 노트에 공부 내용 정리해 줘"));
        title(root,"안전 설정");
        button(root,"긴급 중지 · 작업 차단",0xff7b3f4c,Color.WHITE,()->{
            PilotStore.pref(this).edit().putBoolean("stopped",true).apply();
            PilotStore.log(this,"사용자가 작업 차단");
            refresh();
        });
        button(root,"작업 차단 해제",panel,Color.WHITE,()->{
            PilotStore.pref(this).edit().putBoolean("stopped",false).apply();
            PilotStore.log(this,"사용자가 작업 차단 해제");
            refresh();
        });
        title(root,"작업 기록");
        log=text("",12,0xffbac8d6,false);
        log.setPadding(0,12,0,12);
        root.addView(log);
        root.addView(text("Google Play 검증을 받지 않은 시험용 APK입니다. Play Protect가 설치를 차단하면 보안 기능을 끄지 마세요. 현재 ChatGPT와 휴대폰을 직접 연결하는 기능은 포함되지 않습니다.",11,0xffa4b1c1,false));
        refresh();
    }
    private void approve(PilotCommand command) {
        if(PilotStore.stopped(this)) {
            toast("긴급 중지 중입니다. 먼저 차단을 해제해 주세요.");
            return;
        }
        new AlertDialog.Builder(this)
            .setTitle("작업을 실행할까요?")
            .setMessage(command.summary()+"\n\n승인하면 해당 앱이나 공유 화면이 열립니다.")
            .setPositiveButton("승인",(dialog,which)->{
                String result=TaskExecutor.run(this,command);
                toast(result);
                refresh();
            })
            .setNegativeButton("취소",(dialog,which)->{
                PilotStore.log(this,"작업 취소");
                refresh();
            }).show();
    }
    private void refresh() {
        if(status!=null)status.setText(PilotStore.stopped(this)?"● 긴급 중지 상태":"● 로컬 실행 준비됨 · 위험 권한 요청 없음");
        if(log!=null)log.setText(PilotStore.pref(this).getString("log","작업 기록이 없습니다."));
    }
    private void toast(String s) {Toast.makeText(this,s,Toast.LENGTH_LONG).show();}
    @Override protected void onResume() {super.onResume();refresh();}
}