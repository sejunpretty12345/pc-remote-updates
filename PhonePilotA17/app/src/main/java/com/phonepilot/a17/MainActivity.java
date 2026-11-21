package com.phonepilot.a17;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.speech.RecognizerIntent;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.content.Context;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class MainActivity extends Activity {
    private final int navy=0xff0B1423,panel=0xff17263A,cyan=0xff73E8D9,sub=0xffAEBCC9,muted=0xff8597AA;
    private final ExecutorService worker=Executors.newSingleThreadExecutor();
    private final Handler handler=new Handler(Looper.getMainLooper());
    private LinearLayout chat;
    private EditText input;
    private ScrollView scroll;
    private TextView state;
    private boolean busy=false;

    @Override public void onCreate(Bundle saved) {
        super.onCreate(saved);
        getWindow().setStatusBarColor(navy);
        getWindow().setNavigationBarColor(navy);
        createUi();loadHistory();
        if(PilotStore.pref(this).getString("history","[]").equals("[]"))addMessage("AI","안녕하세요. 앱 실행·검색·알람·일정·메모 등을 요청할 수 있습니다. 예: ‘유튜브에서 뚜아뚜지 검색해 줘’, ‘카카오톡 켜줘’, ‘오후 7시 알람 맞춰줘’.\n\n일부 기능은 해당 앱에서 최종 확인이 필요합니다.",true);
    }
    private GradientDrawable bg(int color,int radius){GradientDrawable shape=new GradientDrawable();shape.setColor(color);shape.setCornerRadius(radius);return shape;}
    private TextView label(String txt,int size,int color,boolean bold) {
        TextView v=new TextView(this);v.setText(txt);v.setTextColor(color);v.setTextSize(size);if(bold)v.setTypeface(null,Typeface.BOLD);return v;
    }
    private void addTopButton(LinearLayout bar,String text,Runnable click){
        TextView v=label(text,12,Color.WHITE,true);v.setPadding(17,15,17,15);v.setBackground(bg(panel,16));
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,-2,1);p.setMargins(3,4,3,5);
        bar.addView(v,p);v.setGravity(Gravity.CENTER);v.setOnClickListener(w->click.run());
    }
    private void createUi(){
        LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setBackgroundColor(navy);
        root.setPadding(14,20,14,10);setContentView(root);
        LinearLayout head=new LinearLayout(this);head.setOrientation(LinearLayout.VERTICAL);root.addView(head);
        head.addView(label("PHONEPILOT AI   /   GALAXY A17",11,cyan,true));
        TextView title=label("무엇을 해드릴까요?",24,Color.WHITE,true);title.setPadding(0,10,0,6);head.addView(title);
        state=label("오프라인 명령 실행 가능",12,sub,false);head.addView(state);
        LinearLayout bar=new LinearLayout(this);bar.setOrientation(LinearLayout.HORIZONTAL);bar.setPadding(0,10,0,4);root.addView(bar);
        addTopButton(bar,"앱 목록",this::showApps);
        addTopButton(bar,"내 메모",this::showNotes);
        addTopButton(bar,"AI 설정",this::showAiSettings);
        addTopButton(bar,"긴급 중지",this::toggleStop);
        scroll=new ScrollView(this);scroll.setFillViewport(true);LinearLayout.LayoutParams scrollParams=new LinearLayout.LayoutParams(-1,0,1);root.addView(scroll,scrollParams);
        chat=new LinearLayout(this);chat.setOrientation(LinearLayout.VERTICAL);chat.setPadding(0,12,0,18);scroll.addView(chat);
        LinearLayout composer=new LinearLayout(this);composer.setOrientation(LinearLayout.HORIZONTAL);composer.setGravity(Gravity.BOTTOM|Gravity.CENTER_VERTICAL);root.addView(composer);
        input=new EditText(this);input.setBackground(bg(panel,20));input.setTextColor(Color.WHITE);input.setHintTextColor(muted);input.setHint("앱 열기, 검색, 알람, 메모...");
        input.setMinLines(2);input.setMaxLines(4);input.setTextSize(14);input.setPadding(18,12,18,12);input.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        composer.addView(input,new LinearLayout.LayoutParams(0,-2,1));
        TextView voice=label("🎤",23,Color.WHITE,true);voice.setGravity(Gravity.CENTER);voice.setPadding(13,16,13,16);composer.addView(voice);voice.setOnClickListener(v->voiceInput());
        TextView send=label("➤",24,navy,true);send.setGravity(Gravity.CENTER);send.setBackground(bg(cyan,20));send.setPadding(20,15,20,15);composer.addView(send);send.setOnClickListener(v->send());
        TextView disclaimer=label("모든 실행 명령은 확인 후 처리됩니다. 접근성·화면 읽기 권한은 사용하지 않습니다.",10,muted,false);
        disclaimer.setPadding(2,8,2,2);root.addView(disclaimer);
        refreshState();
    }
    private void refreshState(){if(state!=null)state.setText(PilotStore.stopped(this)?"● 긴급 중지 활성화":"● 오프라인 명령 사용 가능 · "+(GeminiBridge.configured(this)?"선택형 Gemini AI 연결됨":"Gemini 연결은 선택사항"));}
    private void addMessage(String speaker,String content,boolean transientOnly){
        if(!transientOnly)PilotStore.log(this,speaker,content);
        LinearLayout container=new LinearLayout(this);container.setOrientation(LinearLayout.VERTICAL);container.setPadding(12,13,12,13);
        container.setBackground(bg("나".equals(speaker)?0xff243D49:panel,19));
        LinearLayout.LayoutParams params=new LinearLayout.LayoutParams(-1,-2);
        params.setMargins("나".equals(speaker)?45:0,0,"나".equals(speaker)?0:30,12);
        chat.addView(container,params);
        TextView who=label("나".equals(speaker)?"나":"PhonePilot AI",11,"나".equals(speaker)?cyan:0xffA4F0E5,true);
        container.addView(who);
        TextView text=label(content,14,Color.WHITE,false);text.setTextIsSelectable(true);text.setPadding(0,7,0,1);text.setLineSpacing(4,1.03f);container.addView(text);
        scroll.post(()->scroll.fullScroll(View.FOCUS_DOWN));
    }
    private void loadHistory() {
        try{
            JSONArray history=new JSONArray(PilotStore.pref(this).getString("history","[]"));
            for(int i=0;i<history.length();i++){
                JSONObject item=history.getJSONObject(i);
                addMessage(item.optString("who","AI"),item.optString("line",""),true);
            }
        }catch(Exception ignored){}
    }
    private void send(){
        String message=input.getText().toString().trim();if(message.isBlank())return;
        input.setText("");
        submit(message);
    }
    private void submit(String text){
        if(busy){addMessage("AI","현재 명령을 해석하고 있습니다.",false);return;}
        addMessage("나",text,false);
        if(PilotStore.stopped(this)) {addMessage("AI","긴급 중지가 활성화되어 있습니다. 상단에서 해제하기 전에는 명령을 실행할 수 없습니다.",false);return;}
        PilotCommand local=PilotCommand.parseLocal(text);
        if(local!=null){propose(local);return;}
        if(!GeminiBridge.configured(this)) {
            addMessage("AI","이 요청은 기본 오프라인 명령으로 해석하지 못했습니다. ‘AI 설정’에서 선택형 Gemini 연결을 하면 더 자연스러운 표현을 해석할 수 있습니다.\n기본 지원: 앱 열기, 유튜브·웹 검색, 알람, 타이머, 캘린더, 볼륨, 지도, 메모, 전화번호 입력, 문자 초안, 카메라, 설정.",false);
            return;
        }
        busy=true;state.setText("● Gemini에 명령 해석 요청 중 (이 문장만 전송)");
        worker.submit(()->{
            try{
                PilotCommand command=GeminiBridge.infer(this,text);
                handler.post(()->{busy=false;refreshState();propose(command);});
            }catch(Exception e){
                handler.post(()->{busy=false;refreshState();addMessage("AI","AI 해석 오류: "+e.getMessage()+"\n기본 명령은 인터넷 연결 없이 사용 가능합니다.",false);});
            }
        });
    }
    private void propose(PilotCommand task) {
        if("answer".equals(task.type)) {addMessage("AI",task.text,false);return;}
        new AlertDialog.Builder(this).setTitle("작업 실행 승인")
            .setMessage(task.summary()+"\n\n이 앱이 다른 앱에 접근하는 경우, 실제 저장·전송·변경은 해당 앱에서 직접 확인해야 할 수 있습니다.")
            .setPositiveButton("실행",(d,w)->{
                String result=TaskExecutor.run(this,task);
                addMessage("AI",result,false);
                refreshState();
            })
            .setNegativeButton("취소",(d,w)->addMessage("AI","명령을 취소했습니다.",false)).show();
    }
    private void voiceInput(){
        Intent recognizer=new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        recognizer.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        recognizer.putExtra(RecognizerIntent.EXTRA_LANGUAGE,"ko-KR");
        recognizer.putExtra(RecognizerIntent.EXTRA_PROMPT,"명령을 말씀하세요");
        try{startActivityForResult(recognizer,501);}catch(Exception e){addMessage("AI","이 기기에서 음성 인식 앱을 실행할 수 없습니다. 키보드를 이용해 주세요.",false);}
    }
    @Override protected void onActivityResult(int code,int result,Intent data){
        super.onActivityResult(code,result,data);
        if(code==501&&result==RESULT_OK&&data!=null){
            ArrayList<String> phrases=data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            if(phrases!=null&&!phrases.isEmpty())input.setText(phrases.get(0));
        }
    }
    private void showAiSettings(){
        EditText key=new EditText(this);key.setSingleLine(true);
        key.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_PASSWORD);
        key.setHint(GeminiBridge.configured(this)?"새 키로 교체 (현재 키 저장됨)":"선택 사항: Gemini API 키");
        LinearLayout container=new LinearLayout(this);container.setPadding(35,10,35,0);container.addView(key);
        new AlertDialog.Builder(this).setTitle("선택형 Gemini AI 연결")
            .setMessage("기본 기능은 API 키 없이 작동합니다. 키를 입력하면 이해하기 어려운 명령 문장만 Google Gemini 서버로 전송합니다. AI 키는 기기 Android Keystore로 암호화합니다. API 호출에 요금이나 사용량 제한이 발생할 수 있습니다. 이 앱은 Gemini 공식 앱이 아닙니다.")
            .setView(container)
            .setPositiveButton("키 저장",(d,w)->{
                String value=key.getText().toString().trim();
                if(value.isEmpty()){Toast.makeText(this,"빈 키는 저장하지 않습니다",Toast.LENGTH_SHORT).show();return;}
                try{GeminiBridge.setKey(this,value);refreshState();addMessage("AI","선택형 Gemini AI 키를 안전하게 저장했습니다. 아직 실제 API 연결 테스트는 수행되지 않았습니다.",false);}
                catch(Exception e){addMessage("AI","키 저장 실패: "+e.getMessage(),false);}
            })
            .setNeutralButton("키 삭제",(d,w)->{
                try{GeminiBridge.setKey(this,"");refreshState();addMessage("AI","저장된 Gemini API 키를 삭제했습니다.",false);}
                catch(Exception e){addMessage("AI","키 삭제 오류",false);}
            })
            .setNegativeButton("닫기",null).show();
    }
    private void showApps(){
        List<ResolveInfo> apps=TaskExecutor.visibleApps(this);
        apps.sort(Comparator.comparing(a->a.loadLabel(getPackageManager()).toString(),String.CASE_INSENSITIVE_ORDER));
        List<String> labels=new ArrayList<>();List<ResolveInfo> items=new ArrayList<>();
        for(ResolveInfo info:apps){
            String name=info.loadLabel(getPackageManager()).toString();
            if(labels.contains(name))continue;
            labels.add(name);items.add(info);
            if(labels.size()>=200)break;
        }
        if(labels.isEmpty()){addMessage("AI","Android가 설치된 앱 목록 조회를 허용하지 않았습니다. 앱 이름을 직접 입력해 보세요.",false);return;}
        new AlertDialog.Builder(this).setTitle("열 수 있는 앱 ("+labels.size()+"개)")
            .setItems(labels.toArray(new String[0]),(d,index)->propose(new PilotCommand("open",labels.get(index),"","","",-1,-1,0)))
            .setNegativeButton("닫기",null).show();
    }
    private void showNotes(){
        JSONArray notes=PilotStore.notes(this);
        if(notes.length()==0){addMessage("AI","PhonePilot에 저장된 메모가 없습니다. ‘내 메모에 과학 공부하기 저장해 줘’라고 말해 보세요. 삼성 노트와는 별도입니다.",false);return;}
        List<String> titles=new ArrayList<>();
        for(int i=0;i<notes.length();i++)titles.add(notes.optJSONObject(i).optString("title","메모"));
        new AlertDialog.Builder(this).setTitle("내 메모")
            .setItems(titles.toArray(new String[0]),(dialog,index)->{
                JSONObject note=notes.optJSONObject(index);
                if(note==null)return;
                String value=note.optString("body","");
                new AlertDialog.Builder(this).setTitle(note.optString("title"))
                    .setMessage(value).setPositiveButton("공유",(a,b)->propose(new PilotCommand("note","",value,note.optString("title"),"",-1,-1,0)))
                    .setNegativeButton("닫기",null).show();
            }).setNegativeButton("닫기",null).show();
    }
    private void toggleStop(){
        boolean next=!PilotStore.stopped(this);
        PilotStore.pref(this).edit().putBoolean("stopped",next).apply();
        refreshState();addMessage("AI",next?"긴급 중지: 새 명령 실행을 차단했습니다.":"긴급 중지를 해제했습니다.",false);
    }
    @Override protected void onDestroy(){worker.shutdownNow();super.onDestroy();}
}
