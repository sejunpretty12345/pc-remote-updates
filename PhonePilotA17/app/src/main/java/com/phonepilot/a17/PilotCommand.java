package com.phonepilot.a17;

import org.json.JSONObject;

public final class PilotCommand {
    public final String type,title,value,body;
    private PilotCommand(String type,String title,String value,String body) {
        this.type=type;this.title=title;this.value=value;this.body=body;
    }
    public static PilotCommand fromJson(String raw) throws Exception {
        JSONObject obj=new JSONObject(raw);
        if(!"phonepilot.v1".equals(obj.optString("protocol"))) throw new IllegalArgumentException("프로토콜 불일치");
        String type=obj.optString("type").trim();
        if(!type.matches("youtube|web|note|open|home"))throw new IllegalArgumentException("지원하지 않는 명령입니다");
        String title=obj.optString("title",""),value=obj.optString("query",obj.optString("app",""));
        String body=obj.optString("body","");
        if(title.length()>140||value.length()>500||body.length()>12000)throw new IllegalArgumentException("명령 길이 제한 초과");
        if("note".equals(type)&&(title.isEmpty()||body.isEmpty()))throw new IllegalArgumentException("노트 제목과 본문이 필요합니다");
        if(("youtube".equals(type)||"web".equals(type))&&value.isEmpty())throw new IllegalArgumentException("검색어가 필요합니다");
        if("open".equals(type)&&!value.matches("유튜브|youtube|삼성 노트|노트|크롬|chrome|네이버|naver|캘린더"))throw new IllegalArgumentException("열 수 없는 앱입니다");
        return new PilotCommand(type,title,value,body);
    }
    public static PilotCommand fromText(String raw) {
        String s=raw==null?"":raw.trim();
        if(s.isEmpty())throw new IllegalArgumentException("명령을 입력해 주세요");
        if(s.startsWith("{")) {
            try{return fromJson(s);}catch(Exception e){throw new IllegalArgumentException(e.getMessage());}
        }
        String lower=s.toLowerCase(java.util.Locale.ROOT);
        if(s.contains("유튜브")||lower.contains("youtube")) {
            String q=searchPart(s,"유튜브|youtube");
            if(q.isEmpty()||q.equals(s))return new PilotCommand("open","","유튜브","");
            return new PilotCommand("youtube","",q,"");
        }
        if(s.contains("네이버")||s.contains("크롬")||lower.contains("chrome")) {
            String q=searchPart(s,"네이버|크롬|chrome");
            if(q.isEmpty())return new PilotCommand("open","","크롬","");
            return new PilotCommand("web","",q,"");
        }
        if(s.contains("노트")) {
            String b=s.replaceFirst("^.*?(삼성 ?노트|노트)(에|에다가|를|을)?\\s*","")
                .replaceAll("(작성|적어|기록|정리)(해 ?줘|해 ?주세요|해)?[.!?]*$","").trim();
            if(b.isEmpty())return new PilotCommand("open","","삼성 노트","");
            return new PilotCommand("note","빠른 메모","",b);
        }
        if(s.contains("캘린더"))return new PilotCommand("open","","캘린더","");
        if(s.equals("홈")||s.contains("홈 화면"))return new PilotCommand("home","","","");
        throw new IllegalArgumentException("지원 명령: 유튜브·네이버 검색, 앱 열기, 삼성 노트 공유");
    }
    private static String searchPart(String text,String apps) {
        String s=text.replaceFirst("(?i)^.*?(?:"+apps+")\\s*(?:앱)?\\s*(?:에서|에|들어가서|열고|켜고)?\\s*","")
            .replaceAll("(?:을|를)?\\s*(?:검색(?:해 ?줘|해 ?주세요|해)?|찾아 ?줘|찾아 ?주세요|찾아)(?:요)?[.!?]*$","").trim();
        return s.replaceFirst("^에서\\s*","");
    }
    public String summary() {
        switch(type) {
            case "youtube": return "YouTube에서 ‘"+value+"’ 검색";
            case "web": return "브라우저에서 ‘"+value+"’ 검색";
            case "note": return "노트 공유 창 열기: "+title+"\n"+body.substring(0,Math.min(160,body.length()))+"\n(저장은 사용자가 직접 진행)";
            case "open": return value+" 앱 실행";
            case "home": return "홈 화면으로 이동";
            default:return type;
        }
    }
}