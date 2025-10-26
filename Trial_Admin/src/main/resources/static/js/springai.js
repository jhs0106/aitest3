// ############################################################################
// 텍스트 대화와 관련된 코드
// ############################################################################
window.springai = window.springai || {};


// ############################################################################
// 음성 대화와 관련된 코드
// ############################################################################
window.springai.voice = window.springai.voice || {};

// ##### 마이크를 활성화하고 소리 분석 도구 및 녹화 도구를 준비를 하는 함수 #####
springai.voice.initMic = async function (ai3) {
  //전역 변수 초기화
  springai.voice.voice = false;              	// 사람의 음성이 입력되면 true 
  springai.voice.chatting = false;           	// 질문하기 시작할 때부터 답변을 받을 때까지 true         
  springai.voice.silenceStart = null;        	// 침묵 시작 시간을 저장
  springai.voice.silenceDelay = 2000;      		// 침묵 지연 시간 2초을 저장하는 상수
  springai.voice.silenceThreshold = 0.01;  		// 침묵인지 판단할 임계상수(0~1 사이의 값) 
  springai.voice.stream = null;               // 마이크 입력 스트림 객체
  springai.voice.analyser = null;             // 소리 분석기 객체
  springai.voice.mediaRecorder = null;        // 음성 녹음기 객체
  springai.voice.recognition = null;          // 음성 인식 객체

  //사용자에게 마이크 접근 권한을 요청하고, 오디오 스트림(MediaStream)을 가져옴
  const stream = await navigator.mediaDevices.getUserMedia({ audio: true });
  springai.voice.stream = stream;

  //침묵이 지속되는지 분석을 위한 코드 ---------------
  //오디오 처리를 위한 AudioContext 생성
  const audioContext = new (window.AudioContext || window.webkitAudioContext)();
  //마이크에서 들어온 오디오 스트림을 MediaStreamAudioSourceNode로 변환
  const source = audioContext.createMediaStreamSource(stream);
  //오디오 데이터를 실시간으로 분석하는 AnalyserNode를 생성
  springai.voice.analyser = audioContext.createAnalyser();
  //음성 분석을 위한 FFT(빠른 푸리에 변환) 구간 크기 설정
  //클수록 더 정밀한 주파수 분석이 가능하지만 처리 비용이 증가(보통 512, 1024, 2048 사용)
  springai.voice.analyser.fftSize = 2048;
  //오디오 소스를 분석기에 연결
  source.connect(springai.voice.analyser);
  //-----------------------------------------------

  //미디어 녹음기 초기화
  springai.voice.initMediaRecorder(ai3);
  //음성 인식 초기화
  springai.voice.initRecognitionVoice();
};

//##### 미디어 녹음기를 초기화하는 함수 #####
springai.voice.initMediaRecorder = function (ai3) {
  //오디오 녹음을 위한 MediaRecorder 생성
  const mediaRecorder = new MediaRecorder(springai.voice.stream);
  springai.voice.mediaRecorder = mediaRecorder;

  //침묵으로 인한 음성 녹화가 중지되었을 때, 자동 호출되는 함수 지정
  mediaRecorder.ondataavailable = async (event) => {
    //음성 확인이 되었고, 녹화 데이터가 있고, 현재 대화중이 아닐 경우
    if (springai.voice.voice === true && event.data.size > 0 && springai.voice.chatting === false) {
      console.log("대화 시작");
      springai.voice.chatting = true;

      //MP3로 변환
      const webmBlob = event.data;
      const mp3Blob = await springai.voice.convertWebMToMP3(webmBlob);
      //콜백(사용자 로직) 실행 -------------
      ai3.handleVoice(mp3Blob);
      //---------------------------------
    }
    //음성 확인이 안되었거나, 녹화 데이터가 없을 경우
    else {
      mediaRecorder.start();
      springai.voice.checkSilence();
    }
  };

  console.log("음성 녹화 시작");
  mediaRecorder.start();
  console.log("침묵 감시 시작");
  springai.voice.checkSilence();
};

// ##### 마이크 입력로부터 음성 인식을 하는 함수 #####
springai.voice.initRecognitionVoice = function () {
  // 음성 인식 전역 변수 초기화
  springai.voice.voice = false;
  // 음성 인식을 제공하는 SpeechRecognition 생성
  const SpeechRecognition = window.SpeechRecognition || window.webkitSpeechRecognition;
  const recognition = new SpeechRecognition();
  springai.voice.recognition = recognition;
  // 음성이 한국어일 것이다를 알려주는 힌트 설정(명확한 영어도 인식될 수 있음)
  recognition.lang = 'ko-KR';
  // true: 음성 확인되면 매번 onresult 콜백
  recognition.interimResults = true;
  // false: 음성 확인 후, 몇초간(브라우저 고정값, 1~2초) 침묵이 되면 인식 자동 종료
  recognition.continuous = false;
  // 인식을 시작할 때 콜백되는 함수
  recognition.onstart = function () {
  };
  // 음성 확인되었을 때 콜백되는 함수
  recognition.onresult = function (event) {
    // 변환된 텍스트 얻기(정식 STT로 사용하기에는 인식 정확도 낮음)
    const transcript = event.results[0][0].transcript;
    // 텍스트가 있고, 한글이 포함되어 있을 경우
    if (transcript.length > 0 && springai.voice.isKorean(transcript)) {
      console.log("한국어 음성 확인");
      springai.voice.voice = true;
    }
  };
  // 인식을 종료할 때 콜백되는 함수
  recognition.onend = function () {
    // 브라우저에서 자동 종료시켰을 경우, 재시작 시킴
    if (!springai.voice.voice) {
      recognition.start();
    }
  };

  console.log("음성 인식 시작");
  recognition.start();
};

// ##### 한글이 1개라도 포함되어 있는지 체크하는 함수 #####
springai.voice.isKorean = function (text) {
  const koreanRegex = /[가-힣]/;
  const isKorean = koreanRegex.test(text);
  return isKorean;
};

// ##### 침묵이 지속되는지 체크하는 함수 #####
springai.voice.checkSilence = function () {
  // 분석 결과를 저장할 바이트 배열을 생성
  const dataArray = new Uint8Array(springai.voice.analyser.fftSize);
  // 오디오 파형 데이터를 dataArray에 복사
  // 각 값은 0~255 범위의 8비트 정수이며, 오디오 신호의 진폭을 나타냄
  // 128이 중심(0에 해당), 0 또는 255는 최대 음파 진폭
  springai.voice.analyser.getByteTimeDomainData(dataArray);
  // Uint8Array인 dataArray를 일반 배열로 변환한 뒤, 각 값을 정규화된 부동소수점 형태로 변환
  // 즉, 0~255 범위를 -1.0 ~ +1.0 범위로 바꿈
  const normalized = Array.from(dataArray).map(v => v / 128 - 1);
  // RMS(Root Mean Square) = 정규화된 신호의 제곱 평균 제곱근
  // RMS는 음성 볼륨 크기을 나타내며, 값이 클수록 말소리가 크거나 배경 소음이 심하다는 뜻
  // RMS ≈ 0: 침묵
  // RMS ≈ 1: 최대 볼륨
  const rms = Math.sqrt(normalized.reduce((sum, v) => sum + v * v, 0) / normalized.length);
  // 음성 볼륨이 침묵 임계상수 보다 작을 경우
  if (rms < springai.voice.silenceThreshold) {
    // 침묵 시작 시간 설정이 되어 있지 않은 경우
    if (!springai.voice.silenceStart) {
      // 침묵 시작 시간 설정
      springai.voice.silenceStart = Date.now();
    }
    // 침묵이 silenceDelay 동안 지속될 경우
    else if ((Date.now() - springai.voice.silenceStart) > springai.voice.silenceDelay) {
      // 음성 녹화 중이라면, 음성 녹화 중지
      if (springai.voice.mediaRecorder.state === 'recording') {
        springai.voice.mediaRecorder.stop();
        springai.voice.recognition.stop();
      }
      // 침묵 시작 시간 없애기
      springai.voice.silenceStart = null;
      return;
    }
  }
  // 음성 볼륨이 침묵 임계상수와 같거나 클 경우
  else {
    // 침묵 시작 시간 없애기
    springai.voice.silenceStart = null;
  }

  // 침묵이 지속되는지 계속 체크: 재귀 호출
  requestAnimationFrame(springai.voice.checkSilence);
};

// ##### WebM Blob을 MP3 Blob으로 변환 #####
// OpenAi의 gpt-4o-mini-audio의 입력은 audio/mp3 또는 audio/wav만 가능
springai.voice.convertWebMToMP3 = async function (webmBlob) {
  // WebM Blob → ArrayBuffer → AudioBuffer(PCM) 디코딩
  const arrayBuffer = await webmBlob.arrayBuffer();
  const audioCtx = new (window.AudioContext || window.webkitAudioContext)();
  const audioBuf = await audioCtx.decodeAudioData(arrayBuffer);

  // PCM 데이터 추출 (첫 번째 채널만 사용)
  const float32Data = audioBuf.getChannelData(0);
  const sampleRate = audioBuf.sampleRate;

  // LameJS Mp3Encoder 인스턴스 생성 (채널=1, 샘플레이트, 비트레이트=128kbps)
  const mp3Encoder = new lamejs.Mp3Encoder(1, sampleRate, 128);
  const samplesPerFrame = 1152;
  let mp3DataChunks = [];

  // Float32 → Int16 변환 함수
  function floatTo16BitPCM(input) {
    const output = new Int16Array(input.length);
    for (let i = 0; i < input.length; i++) {
      let s = Math.max(-1, Math.min(1, input[i]));
      output[i] = s < 0 ? s * 0x8000 : s * 0x7FFF;
    }
    return output;
  }

  // 프레임 단위 인코딩
  for (let i = 0; i < float32Data.length; i += samplesPerFrame) {
    const sliceF32 = float32Data.subarray(i, i + samplesPerFrame);
    const sliceI16 = floatTo16BitPCM(sliceF32);
    const mp3buf = mp3Encoder.encodeBuffer(sliceI16);
    if (mp3buf.length) mp3DataChunks.push(mp3buf);
  }
  // 남은 버퍼 flush
  const endBuf = mp3Encoder.flush();
  if (endBuf.length) mp3DataChunks.push(endBuf);

  // Blob으로 병합해 반환
  return new Blob(mp3DataChunks, { type: 'audio/mp3' });
};

// ##### 스트리밍 음성 데이터를 재생하는 함수 #####
springai.voice.playAudioFormStreamingData = async function (response, audioPlayer) {
  try {
    // 스트리밍을 위한 미디어소스 생성과 audioPlaye 소스로 설정
    const mediaSource = new MediaSource();
    audioPlayer.src = URL.createObjectURL(mediaSource);

    // 스트림이 열리면 콜백되는 함수 등록
    mediaSource.addEventListener('sourceopen', async () => {
      // 본문의 오디오 데이터 타입을 알려주고 데이터 버퍼 준비
      // MIME 타입은 서버에서 실제 인코딩한 포맷으로 맞춰야 함
      // 예) MP3: 'audio/mpeg', WAV: 'audio/wav'
      const sourceBuffer = mediaSource.addSourceBuffer('audio/mpeg');
      // 응답 본문을 읽는 리더 얻기
      const reader = response.body.getReader();
      // 스트리밍되는 데이터가 있을 동안 반복
      while (true) {
        // 스트리밍 음성 데이터(청크) 읽기
        const { done, value } = await reader.read();
        //스트리밍이 종료될 경우 스트림을 닫고 반복 중지
        if (done) {
          mediaSource.endOfStream();
          break;
        }
        // 스트리밍이 계속 진행 중일 경우
        await new Promise(resolve => {
          // 버퍼 데이터가 갱신 완료될 때마다 핸들러(resolve) 실행, 
          // { once: true }: 핸들러를 한 번만 실행한 후 자동으로 제거
          sourceBuffer.addEventListener('updateend', resolve, { once: true });
          // 버퍼에 데이터 추가
          sourceBuffer.appendBuffer(value);
        });
      }
    });
    // 재생 시작
    audioPlayer.play();
  } catch (error) {
    console.log(error);
  }
};


// ##### 스피커 애니메이션 제어 함수 #####
springai.voice.controlSpeakerAnimation = function (speackerId, flag) {
  if (flag) {
    document.getElementById(speackerId).classList.add("speakerPulse");
  } else {
    document.getElementById(speackerId).classList.remove("speakerPulse");
  }
};