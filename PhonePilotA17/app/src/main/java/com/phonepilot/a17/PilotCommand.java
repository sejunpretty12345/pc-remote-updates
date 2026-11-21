package com.phonepilot.a17;

import org.json.JSONObject;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class PilotCommand {
    public final String type,target,text,title,body;
    public final int hour,minute,seconds;
    public PilotCommand(String type,String target,String text,String title,String body,int hour,int minute,int seconds) {
        this.type=type;this.target=target;this.text=text;this.title=title;this.body=body;
        this.hour=hour;this.minute=minute;this.seconds=seconds;
    }
    private static PilotCommand of(String type,String target,String text) {return new PilotCommand(type,target,text,"","",-1,-1,0);}
    public static PilotCommand fromJson(JSONObject j) {
        String type=j.optString("type","").trim().toLowerCase(Locale.ROOT);
        if(!type.matches("open|youtube|web|alarm|timer|calendar|settings|volume|dial|sms|maps|note|localnote|findnote|share|camera|home|answer"))throw new IllegalArgumentException("허용되지 않은 명령 유형");
        String target=j.optString("target",""),text=j.optString("text","");
        String title=j.optString("title",""),body=j.optString("body","");
        if(target.length()>100||text.length()>1000||title.length()>140||body.length()>7000)throw new IllegalArgumentException("입력 길이 제한 초과");
        int hour=j.optInt("hour",-1),minute=j.optInt("minute",-1),seconds=j.optInt("seconds",0);
        if(hour>23||hour< -1||minute>59||minute< -1||seconds<0||seconds>86400)throw new IllegalArgumentException("시간 범위가 올바르지 않습니다");
        if("open".equals(type)&&target.isBlank())throw new IllegalArgumentException("앱 이름이 필요합니다");
        if("dial".equals(type)&&!text.matches("[+0-9 ()-]{0,35}"))throw new IllegalArgumentException("전화번호 형식 오류");
        if("answer".equals(type))return new PilotCommand(type,"",text,"","",-1,-1,0);
        return new PilotCommand(type,target,text,title,body,hour,minute,seconds);
    }
    private static String trimmed(String s) {
        return s.replaceAll("^(그냥|좀|제발|어|응|혹시|자|이제|지금)\\s*","")
            .replaceAll("(?i)(?:을|를|좀)?\\s*(?:해 ?줘|해 ?주세요|해줄래|해주세요|부탁해|부탁해요|열어 ?줘|켜 ?줘|검색해 ?줘|찾아 ?줘|띄워 ?줘|켜주세요|열어주세요|해|줘)[.!?~\\s]*$","").trim();
    }
    private static String searchText(String s,String marker) {
        String x=s.replaceFirst("(?i)^.*?"+marker+"(?:에서|로|에|를|앱을|앱에서|를 열고|에 들어가서)?\\s*", "");
        return trimmed(x.replaceAll("(?i)(검색|찾아|보여)\\s*(해줘|해 ?주세요|해|줘)?[.!?\\s]*$", ""));
    }
    private static int[] parseTime(String s) {
        Matcher matcher=Pattern.compile("(\\d{1,2})\\s*시(?:\\s*(\\d{1,2})\\s*분)?").matcher(s);
        if(!matcher.find())return new int[]{-1,-1};
        int hour=Integer.parseInt(matcher.group(1));
        int minute=matcher.group(2)==null?0:Integer.parseInt(matcher.group(2));
        if(hour>23||minute>59)return new int[]{-1,-1};
        if(s.contains("오후")&&hour<12)hour+=12;
        if(s.contains("오전")&&hour==12)hour=0;
        if(!s.contains("오전")&&!s.contains("오후")&&hour<=12&&s.contains("저녁")&&hour<12)hour+=12;
        return new int[]{hour,minute};
    }
    public static PilotCommand parseLocal(String raw) {
        String s=raw==null?"":raw.trim();
        if(s.length()>8000||s.isEmpty())return null;
        if(s.startsWith("{")) {try{return fromJson(new JSONObject(s));}catch(Exception ignored){return null;}}
        String lower=s.toLowerCase(Locale.ROOT);
        if(s.contains("긴급 중지")||s.equals("중지"))return of("settings","stop","");
        if(s.contains("내 메모")&& (s.contains("보여")||s.contains("찾")||s.contains("열")))return of("findnote","","");
        if(s.contains("내 메모")||s.contains("여기 메모")||s.contains("앱 안에 메모")) {
            String body=trimmed(s.replaceAll("^.*?(?:내 메모|여기 메모|앱 안에 메모)(?:에|로|를)?\\s*", ""));
            if(!body.isEmpty())return new PilotCommand("localnote","",body,"빠른 메모","",-1,-1,0);
        }
        if((s.contains("노트")||s.contains("메모"))&&(s.contains("작성")||s.contains("적어")||s.contains("정리")||s.contains("저장")||s.contains("추가")||s.contains("만들"))) {
            String body=trimmed(s.replaceAll("^.*?(?:삼성 ?노트|노트|메모)(?:에|에다가|를|로)?\\s*", ""));
            if(body.isEmpty())return of("open","삼성 노트","");
            return new PilotCommand("note","",body,"AI 메모","",-1,-1,0);
        }
        if(lower.contains("youtube")||s.contains("유튜브")) {
            if(s.contains("검색")||s.contains("찾")||s.contains("틀어")) {
                String q=searchText(s,"(?:유튜브|youtube)");
                if(!q.isEmpty())return of("youtube","",q);
            }
            return of("open","유튜브","");
        }
        if(s.contains("알람")||s.contains("알림 시계")) {
            int[] t=parseTime(s);
            return new PilotCommand("alarm","",s.replaceAll("[0-9]+\\s*시(?:\\s*[0-9]+\\s*분)?","").replaceAll("(알람|맞춰|설정|해 ?줘|오전|오후)","").trim(),"","",t[0],t[1],0);
        }
        if(s.contains("타이머")) {
            Matcher h=Pattern.compile("(\\d+)\\s*시간").matcher(s);
            Matcher m=Pattern.compile("(\\d+)\\s*분").matcher(s);
            Matcher sec=Pattern.compile("(\\d+)\\s*초").matcher(s);
            int seconds=(h.find()?Integer.parseInt(h.group(1))*3600:0)+(m.find()?Integer.parseInt(m.group(1))*60:0)+(sec.find()?Integer.parseInt(sec.group(1)):0);
            return new PilotCommand("timer","", "", "","",-1,-1,seconds);
        }
        if(s.contains("일정")||s.contains("스케줄")||s.contains("캘린더에")) {
            String title=trimmed(s.replaceAll("(캘린더에|일정에|일정을|일정|스케줄|등록|추가|기록|만들어|해줘)","").trim());
            return new PilotCommand("calendar","","",title,"",-1,-1,0);
        }
        if(s.contains("볼륨")||s.contains("음량")||s.contains("소리 크게")||s.contains("소리 작게")) {
            String dir=s.contains("낮")||s.contains("줄")||s.contains("작게")?"down":s.contains("음소거")?"mute":"up";
            return of("volume",dir,"");
        }
        if(s.contains("와이파이")||lower.contains("wifi")||lower.contains("wi-fi"))return of("settings","wifi","");
        if(s.contains("블루투스"))return of("settings","bluetooth","");
        if(s.contains("밝기")||s.contains("화면 설정"))return of("settings","display","");
        if(s.contains("배터리 설정"))return of("settings","battery","");
        if(s.contains("설정")&&(s.contains("열")||s.contains("켜")))return of("settings","general","");
        if(s.contains("지도")||s.contains("길찾기")||s.contains("위치 검색")) {
            String destination=trimmed(s.replaceAll("(지도에서|지도|위치|길찾기|검색|찾아줘|찾아)", "").trim());
            return of("maps","",destination);
        }
        if(s.contains("전화")&&s.matches(".*[0-9]{2,}.*")) {
            Matcher number=Pattern.compile("(?:\\+?\\d[0-9 -]{6,20})").matcher(s);
            if(number.find())return of("dial","",number.group().trim());
        }
        if(s.contains("문자")&&(s.contains("작성")||s.contains("보내")||s.contains("써")))return of("sms","", "");
        if(s.contains("사진 찍")||s.contains("카메라 켜")||s.contains("카메라 열"))return of("camera","","");
        if(s.contains("홈 화면")||s.equals("홈으로"))return of("home","","");
        if(s.contains("검색")||s.contains("찾아봐")) {
            String q=trimmed(s.replaceAll("^(구글에서|네이버에서|크롬에서|인터넷에서|웹에서|브라우저에서|검색창에서)", "")
                .replaceAll("(검색|찾아봐|찾아줘)", "").trim());
            if(!q.isEmpty())return of("web",s.contains("구글")?"google":"naver",q);
        }
        if(s.contains("노트")&& (s.contains("열")||s.contains("켜")))return of("open","삼성 노트","");
        if(s.contains("메모")&& (s.contains("공유")||s.contains("보내")))return of("share","",s);
        // General app launching: name followed by '켜줘/실행해줘/열어줘'.
        Matcher open=Pattern.compile("^(.+?)\\s*(?:앱)?\\s*(?:을|를)?\\s*(?:켜 ?줘|열어 ?줘|실행(?:해 ?줘|해 ?주세요|해)|켜주세요|열어주세요|실행해줘|틀어 ?줘)[.!?\\s]*$").matcher(s);
        if(open.find()) {
            String name=trimmed(open.group(1)).replaceAll("^(그|그거|앱)\\s+","").trim();
            if(!name.isEmpty())return of("open",name,"");
        }
        return null;
    }
    public String summary() {
        switch(type) {
            case "youtube":return "YouTube에서 ‘"+text+"’ 검색 화면 열기";
            case "web":return "웹에서 ‘"+text+"’ 검색 화면 열기";
            case "open":return "‘"+target+"’ 앱 실행";
            case "alarm":return hour<0?"알람 설정 화면 열기":"알람 "+hour+"시 "+minute+"분 설정 요청 (시계 앱에서 확인)";
            case "timer":return seconds<=0?"타이머 화면 열기":seconds+"초 타이머 설정 요청";
            case "calendar":return "새 일정 작성 화면 열기: "+title+" (직접 저장)";
            case "settings":return "설정 화면 열기: "+target;
            case "volume":return "미디어 음량 "+("up".equals(target)?"높이기":"down".equals(target)?"낮추기":"음소거");
            case "dial":return "전화 앱에서 번호 확인: "+text+" (자동 발신 없음)";
            case "sms":return "문자 작성 화면 열기 (자동 전송 없음)";
            case "maps":return "지도에서 ‘"+text+"’ 검색";
            case "note":return "삼성 노트에 보낼 텍스트 공유 (직접 저장): "+(body.isEmpty()?text:body);
            case "localnote":return "PhonePilot 내부에 직접 메모 저장: "+(body.isEmpty()?text:body);
            case "findnote":return "저장된 PhonePilot 메모 열기";
            case "share":return "내용 공유";
            case "camera":return "카메라 실행";
            case "home":return "홈 화면으로 이동";
            case "answer":return text;
            default:return type;
        }
    }
}
