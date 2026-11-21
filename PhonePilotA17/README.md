# PhonePilot A17 — mobile-only prototype

**기기:** Galaxy A17 (SM-A175N), Android 9+ (target API 35)
**상태:** 소스 구현 완료 / Android SDK 빌드 및 실기기 테스트 미완료. APK로 검증되지 않았습니다.

## 목표

ChatGPT 대화 → Google Calendar에 명령 이벤트 등록 → 동일 Google 계정으로 동기화되는 갤럭시 A17에서 알림 → 휴대폰 소유자 승인 → 앱 실행·검색·삼성 노트 텍스트 입력 시도.

컴퓨터/서버를 직접 운영하지 않고, 이미 사용 가능한 Google Calendar 연결을 전달 통로로 활용합니다. 캘린더 이벤트는 일반 일정으로 보이므로 명령 이력을 남기며, 명령 동기화에는 지연이 있습니다. 민감한 노트 본문은 Google Calendar에 저장되므로 민감한 개인정보를 명령에 포함하지 않는 것이 좋습니다.

## 설치 (컴퓨터 없이)

1. GitHub Actions의 `Build PhonePilot A17 APK` 워크플로를 실행해 `PhonePilotA17-debug-apk` 아티팩트를 받습니다. 소스 리포지토리를 배포한 경우 모바일 브라우저에서 가능합니다.
2. ZIP 아티팩트의 `app-debug.apk`를 갤럭시 A17에 설치합니다. 설치 경로에 따라 Android가 외부 앱 설치나 접근성 권한을 제한할 수 있습니다. 제한을 우회하도록 설계하지 않았습니다.
3. 앱을 열어 캘린더 읽기, 알림, 접근성 권한을 사용자 본인이 검토한 뒤 허용합니다.
4. `③ ChatGPT 명령 수신 시작`을 누릅니다. 명령을 감시할 때 앱의 상시 알림이 표시됩니다.
5. ChatGPT에서 Google Calendar 이벤트를 생성하면, 동기화 후 PhonePilot 앱에 사용자 승인 알림이 나타납니다.

## ChatGPT와 연동할 이벤트 형식

- 일정 제목: `[PhonePilot] 유튜브 검색`
- 시작: 현재 시각 근처 (예: 지금부터 1분 이내)
- 종료: 시작 이후 5분
- 설명 (전체가 정확한 JSON이어야 함):

```json
{"protocol":"phonepilot.v1","type":"youtube","query":"뚜아뚜지"}
```

삼성 노트 내용 생성 명령 예:

```json
{"protocol":"phonepilot.v1","type":"note","title":"열과 비열","body":"열량 Q = mcΔT.\n비열이 클수록 같은 질량의 온도를 올리는 데 더 많은 열이 필요합니다."}
```

기타 타입: `web` + `query`, `open` + `app` (유튜브/삼성 노트/크롬/네이버/캘린더), `home`, `back`. 사용자가 접근성 서비스를 허용하고 승인해야 화면 작업이 실행됩니다.

## 설계 및 보안 제한

- 모든 수신 명령에 대한 **휴대폰에서의 명시적인 승인** 필수
- 앱이 인터넷에서 직접 명령을 다운로드하거나 화면 내용을 외부에 전송하지 않음
- 민감한 금융 앱 및 메시지 자동 전송 기능 없음
- 긴급 중지로 신규 명령 차단·노트 자동화 취소·수신 서비스 종료
- 노트 자동화는 접근성 UI 레이블 탐색 및 ACTION_SET_TEXT 이용. **Samsung Notes 버전별 UI 차이 때문에 저장 성공은 보장하지 않음**
- YouTube 검색은 공식 웹 검색 URL을 YouTube 앱에 전달하는 방식. 앱이 해당 intent를 지원하지 않으면 브라우저로 열 수 있음
- 실행 결과를 ChatGPT 채팅으로 되돌리는 경로는 아직 없음. ChatGPT 채팅에서 완료 확인 불가
- 캘린더를 폴링하는 서비스는 Android의 배터리 정책이나 제한으로 중지될 수 있음

## 구축 방법

GitHub Actions 워크플로는 Android SDK와 Gradle 8.10.2를 설치한 Linux 러너에서 `gradle :app:assembleDebug`를 수행합니다. Android Studio가 필요 없도록 Java/Android SDK 기본 API만 사용합니다.

## 테스트 체크리스트

- [ ] GitHub Actions 컴파일 성공
- [ ] A17에 APK 설치 성공
- [ ] 접근성 서비스 권한 활성화
- [ ] Google Calendar 동기화된 이벤트 수신
- [ ] 사용자 승인/취소/긴급 중지 동작
- [ ] YouTube 앱 검색 결과 표시
- [ ] Samsung Notes 신규 노트 입력 및 재열람으로 저장 여부 검증

실기기 검증 전에는 “작동 검증 완료”라고 주장하지 않습니다.
