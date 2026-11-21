package com.phonepilot.a17;

import android.content.Context;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.net.ssl.HttpsURLConnection;

/** Optional provider-driven interpretation. Never executes commands; returns a validated proposal only. */
public final class GeminiBridge {
    private static final String KEY_ALIAS="PhonePilotAIUserKeyV1";
    private GeminiBridge(){}
    private static SecretKey key() throws Exception {
        KeyStore store=KeyStore.getInstance("AndroidKeyStore");store.load(null);
        if(store.containsAlias(KEY_ALIAS))return (SecretKey)store.getKey(KEY_ALIAS,null);
        KeyGenerator generator=KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES,"AndroidKeyStore");
        KeyGenParameterSpec spec=new KeyGenParameterSpec.Builder(KEY_ALIAS,KeyProperties.PURPOSE_ENCRYPT|KeyProperties.PURPOSE_DECRYPT)
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256).build();
        generator.init(spec);return generator.generateKey();
    }
    public static void setKey(Context context,String plain) throws Exception {
        if(plain.trim().isEmpty()) {context.getSharedPreferences("ai_key",0).edit().remove("cipher").apply();return;}
        Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE,key());
        byte[] iv=cipher.getIV();
        byte[] crypt=cipher.doFinal(plain.trim().getBytes(StandardCharsets.UTF_8));
        byte[] bytes=new byte[iv.length+crypt.length];
        System.arraycopy(iv,0,bytes,0,iv.length);
        System.arraycopy(crypt,0,bytes,iv.length,crypt.length);
        context.getSharedPreferences("ai_key",0).edit().putString("cipher",Base64.encodeToString(bytes,Base64.NO_WRAP)).apply();
    }
    public static boolean configured(Context context){return context.getSharedPreferences("ai_key",0).contains("cipher");}
    private static String readKey(Context context) throws Exception {
        String data=context.getSharedPreferences("ai_key",0).getString("cipher","");
        if(data.isEmpty())return "";
        byte[] bytes=Base64.decode(data,Base64.NO_WRAP);
        byte[] iv=new byte[12];System.arraycopy(bytes,0,iv,0,iv.length);
        Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE,key(),new GCMParameterSpec(128,iv));
        return new String(cipher.doFinal(bytes,12,bytes.length-12),StandardCharsets.UTF_8);
    }
    public static PilotCommand infer(Context c,String userMessage) throws Exception {
        String apiKey=readKey(c);
        if(apiKey.isBlank())throw new IllegalStateException("AI 연결 키가 설정되지 않았습니다");
        String instructions="""
            당신은 Samsung Galaxy Android 앱의 안전한 명령 계획기입니다.
            한국어 사용자 요청을 단 하나의 JSON 객체로 해석하세요. Markdown은 금지합니다.
            가능한 type: open,youtube,web,alarm,timer,calendar,settings,volume,dial,sms,maps,note,localnote,findnote,share,camera,home,answer.
            JSON keys: type, target, text, title, body, hour, minute, seconds.
            open target=사용자가 부른 앱 이름, youtube text=검색어, web target=naver/google text=검색어,
            alarm hour=24시 정수,minute=분. 시간이 모호하면 hour=-1,minute=-1.
            timer seconds=초. calendar title=일정명, hour=-1 if unknown.
            settings target=wifi/bluetooth/display/battery/general.
            volume target=up/down/mute. maps text=장소. note text=삼성 노트 공유 내용(저장 자동화 불가).
            localnote text=PhonePilot 내부 메모 저장. findnote target=제목 검색. dial text=전화번호. sms text=문자 초안.
            일반 지식 질문이나 불가능한 조작은 type=answer,text=정확하고 간단한 한국어 설명.
            다른 앱 내 버튼 누르기, 사용자 대신 메시지 전송, 인증 조작, 권한·보안 우회, 기기 내 자료 읽기는 불가능합니다.
            사용자가 명시하지 않은 전화·문자 수신인, 앱 이름, 날짜 등은 추측하지 마세요.
            모든 기기 동작은 별도 사용자 승인 후에만 처리됩니다.
            """;
        JSONObject body=new JSONObject();
        JSONArray turns=new JSONArray();
        turns.put(new JSONObject().put("role","user").put("parts",new JSONArray().put(new JSONObject().put("text",instructions+"\n사용자 입력: "+userMessage))));
        body.put("contents",turns);
        body.put("generationConfig",new JSONObject().put("responseMimeType","application/json").put("maxOutputTokens",700));
        URL u=new URL("https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash-lite:generateContent");
        HttpsURLConnection conn=(HttpsURLConnection)u.openConnection();
        try {
            conn.setRequestMethod("POST");
            conn.setConnectTimeout(12000);conn.setReadTimeout(25000);
            conn.setDoOutput(true);conn.setRequestProperty("Content-Type","application/json; charset=utf-8");
            conn.setRequestProperty("x-goog-api-key",apiKey);
            try(OutputStream output=conn.getOutputStream()){output.write(body.toString().getBytes(StandardCharsets.UTF_8));}
            int code=conn.getResponseCode();
            InputStream stream=code>=200&&code<300?conn.getInputStream():conn.getErrorStream();
            String payload;
            try(InputStream input=stream){payload=new String(input.readNBytes(150000),StandardCharsets.UTF_8);}
            if(code<200||code>=300)throw new IllegalStateException("AI 서버 요청 실패 (HTTP "+code+"). 키·모델 지원·할당량을 확인해 주세요.");
            JSONObject response=new JSONObject(payload);
            String text=response.getJSONArray("candidates").getJSONObject(0).getJSONObject("content").getJSONArray("parts").getJSONObject(0).getString("text").trim();
            if(text.startsWith("```"))text=text.replaceFirst("^```(?:json)?\\s*","").replaceFirst("\\s*```$","").trim();
            return PilotCommand.fromJson(new JSONObject(text));
        } finally {conn.disconnect();}
    }
}
