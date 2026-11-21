package com.phonepilot.a17;

import android.accessibilityservice.AccessibilityService;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import java.util.ArrayList;
import java.util.List;

public class PilotAccessibilityService extends AccessibilityService {
    public static volatile PilotAccessibilityService instance;
    private final Handler handler=new Handler(Looper.getMainLooper());
    private String noteTitle, noteBody;
    private long started;
    private int state=0;
    private boolean inProgress=false;

    @Override public void onServiceConnected() {super.onServiceConnected();instance=this;PilotStore.log(this,"접근성 서비스 연결됨");}
    @Override public void onDestroy(){if(instance==this)instance=null;super.onDestroy();}
    @Override public void onInterrupt(){stopNote("접근성 작업 중단");}
    public void stopNote(String reason){ state=0;inProgress=false;noteTitle=null;noteBody=null;PilotStore.log(this,reason); }
    public void beginNote(String title,String body){
        if(title==null || body==null || title.length()>140 || body.length()>12000)throw new IllegalArgumentException("잘못된 노트 내용");
        noteTitle=title;noteBody=body;started=System.currentTimeMillis();state=1;inProgress=false;
        handler.postDelayed(this::process,850);
    }
    @Override public void onAccessibilityEvent(AccessibilityEvent event){
        if(state==0 || PilotStore.stopped(this))return;
        if(event==null||event.getPackageName()==null)return;
        if(!"com.samsung.android.app.notes".contentEquals(event.getPackageName()))return;
        handler.postDelayed(this::process,250);
    }
    private void process(){
        if(state==0 || inProgress)return;
        if(PilotStore.stopped(this)){stopNote("긴급 중지로 노트 자동화 취소");return;}
        if(System.currentTimeMillis()-started>30000){stopNote("30초 시간 초과: 저장 확인 필요");return;}
        AccessibilityNodeInfo root=getRootInActiveWindow();
        if(root==null)return;
        if(root.getPackageName()!=null && !"com.samsung.android.app.notes".contentEquals(root.getPackageName()))return;
        try {
            List<AccessibilityNodeInfo> inputs=new ArrayList<>(); collectInputs(root,inputs,0);
            if(state==1){
                // Existing editor can open directly when the app resumes.
                if(!inputs.isEmpty()){state=2; handler.postDelayed(this::process,450);return;}
                AccessibilityNodeInfo create=findClickable(root,new String[]{"새 노트","새 메모","노트 작성","노트 만들기","새 노트 작성","새로 만들기","Create note","Add note","Create","추가"},0);
                if(create!=null && clickNearest(create)){state=2;handler.postDelayed(this::process,900);return;}
            }
            if(state==2 && !inputs.isEmpty()){
                boolean ok;
                if(inputs.size()>=2){
                    ok=setText(inputs.get(0),noteTitle) && setText(inputs.get(1),noteBody);
                }else{
                    ok=setText(inputs.get(0),noteTitle+"\n\n"+noteBody);
                }
                if(ok){ state=3;PilotStore.log(this,"노트 입력 완료: 저장 단계 확인 중");handler.postDelayed(this::process,900); }
                else stopNote("삼성 노트 입력 실패: 화면을 확인해 주세요");
                return;
            }
            if(state==3){
                AccessibilityNodeInfo save=findClickable(root,new String[]{"저장","완료","Save","Done"},0);
                if(save!=null && clickNearest(save)){
                    PilotStore.log(this,"삼성 노트 저장 버튼 눌림. 실제 저장 여부 확인 필요");
                    state=4;
                    handler.postDelayed(this::verify,1400);
                } else {
                    // Samsung Notes often auto-saves when leaving edit mode; DO NOT claim saved.
                    PilotStore.log(this,"저장 버튼을 찾지 못했습니다. 화면에서 저장 여부를 확인해 주세요");
                    state=0;
                }
            }
        }finally{ try{root.recycle();}catch(Exception ignored){} }
    }
    private void verify(){
        if(state!=4)return;
        PilotStore.log(this,"저장 동작 요청 완료. 실제 노트 목록에서 직접 확인 필요");
        state=0;
    }
    private boolean setText(AccessibilityNodeInfo n,String text){
        if(n==null || !n.isEditable())return false;
        Bundle b=new Bundle();b.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,text);
        return n.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT,b);
    }
    private void collectInputs(AccessibilityNodeInfo n,List<AccessibilityNodeInfo> out,int depth){
        if(n==null||depth>18||out.size()>=12)return;
        if(n.isEditable() && n.isVisibleToUser())out.add(n);
        for(int i=0;i<n.getChildCount();i++)collectInputs(n.getChild(i),out,depth+1);
    }
    private AccessibilityNodeInfo findClickable(AccessibilityNodeInfo n,String[] names,int depth){
        if(n==null||depth>18)return null;
        CharSequence t=n.getText(), d=n.getContentDescription();
        for(String x:names){
            if((t!=null&&x.equalsIgnoreCase(t.toString().trim()))||(d!=null&&(d.toString().toLowerCase().contains(x.toLowerCase()))))return n;
        }
        for(int i=0;i<n.getChildCount();i++){
            AccessibilityNodeInfo found=findClickable(n.getChild(i),names,depth+1);
            if(found!=null)return found;
        }
        return null;
    }
    private boolean clickNearest(AccessibilityNodeInfo n){
        for(int i=0;n!=null && i<5;i++,n=n.getParent())if(n.isClickable() && n.isEnabled())return n.performAction(AccessibilityNodeInfo.ACTION_CLICK);
        return false;
    }
}
