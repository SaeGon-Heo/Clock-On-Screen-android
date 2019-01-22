/*
 * Copyright 2014-2018 SaeGon Heo
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package kr.hsg.clockonscreen;

import kr.hsg.clockonscreen.FSDetector.OnFullScreenListener;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.graphics.LinearGradient;
import android.graphics.PixelFormat;
import android.graphics.Shader;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.jakewharton.threetenabp.AndroidThreeTen;

import org.threeten.bp.Instant;
import org.threeten.bp.ZoneId;
import org.threeten.bp.format.DateTimeFormatter;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public final class COSSvc extends Service implements Runnable {
    private Context mCon;
    //항상 보이게 할 뷰
    private TextView cosSvc_TV;
    private TextView cosSvc_TVGradient;
    private Object cosSvc_OutBoundLayout;
    private FSDetector cosSvc_FSDetector;
    private boolean cosSvc_FSSaved;
    private ScheduledFuture<?> cosSvc_repeater;
    private Instant cosSvc_current;
    private DateTimeFormatter cosSvc_formatter;
    private StringBuilder cosSvc_overMinFormat;
    private StringBuilder cosSvc_ClockText;
    private StringBuilder cosSvc_strBattLevelBuilder;
    private StringBuilder cosSvc_strBattBuilder;
    private int[] cosSvc_gradientColors;
    private short cosSvc_HidingTimeLength;
    private short cosSvc_HidingTime;
    private byte cosSvc_second;
    // Each bit have meaning as below
    // (8.(value)bFSModeEnabled)
    // (7.(value)overMin_need_update)
    // (6.(value)isUsing_SecElement)
    // (5.(value)thereAreOnlyString)
    // (4.(value)isBattery_Discharging)
    // (3.(value)isBattery_Charging)
    // (2.(setting)isUsing_Battery)
    // (1.(value)onDestroy called)
    private byte cosSvc_Status;
    private View.OnLayoutChangeListener cosSvc_gradientRefresher;
    private BroadcastReceiver cosSvc_receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if(action == null) return;
            // 배터리 상태 변경 시 변경 내용을 StringBuilder에 저장
            if(action.equals(Intent.ACTION_BATTERY_CHANGED)) {
                cosSvc_strBattLevelBuilder.setLength(0);
                // 배터리 level 저장 및 StringBuiler에 입력
                int level;
                try {
                    level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0) * 100 /
                            intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100);
                } catch (ArithmeticException ae) {
                    level = -1;
                }
                cosSvc_strBattLevelBuilder.append(level);
                // 배터리 충, 방전 상태 갱신
                if (intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) != 0) {
                    // 3rd, 4th bit reset
                    cosSvc_Status &= 0b11110011;
                    // 3rd bit on (isBattery_Charging)
                    cosSvc_Status |= 0b00000100;
                    if(level == 100) {
                        // 4th bit on (isBattery_Discharging)
                        // 3,4 bit 둘다 켜져있으면 풀충전 상태로 간주
                        cosSvc_Status |= 0b00001000;
                    }
                } else if(level < 16) {
                    // 3rd, 4th bit reset
                    cosSvc_Status &= 0b11110011;
                    // 4th bit on (isBattery_Discharging)
                    cosSvc_Status |= 0b00001000;
                } else {
                    // 3rd, 4th bit reset
                    cosSvc_Status &= 0b11110011;
                }
                // 마지막 % 추가
                cosSvc_strBattLevelBuilder.append('%');
            }
            // 시간이 변경되면 서비스를 껏다 켠다. / 화면이 꺼지면 Idle 서비스로 전환.
            else if(action.equals(Intent.ACTION_SCREEN_OFF) || action.equals(Intent.ACTION_DATE_CHANGED) ||
                    action.equals(Intent.ACTION_TIMEZONE_CHANGED) || action.equals(Intent.ACTION_TIME_CHANGED)) {
                // 화면꺼짐 + 시간변경 둘 다 동일한 처리로 해결
                // Idle 서비스를 키면 Idle 서비스 내에서 현재 조건이 COSSvc 서비스를
                // 켜야 하는 상황인지 확인 및 다시 켠다
                // 즉, COSSvc 서비스 재시작과 같다
                startSvc_Idle();
            }
        }
    };

    @Override
    public IBinder onBind(Intent arg0) { return null; }

    // Idle 서비스 시작. (Main 상태 탈출)
    private void startSvc_Idle() {
        mCon.startService(new Intent(mCon, COSSvc_Idle.class));
        stopSelf();
    }

    @Override
    public void onDestroy() {
        // 1st bit on (onDestroy called)
        cosSvc_Status |= 0b00000001;
        // 서비스 종료 과정
        if(cosSvc_repeater != null) {
            while(true) {
                if(cosSvc_repeater.cancel(true)) break;
            }
        }
        mHandler.removeCallbacks(this);
        mHandler.removeMessages(0);

        cosSvc_receiver.clearAbortBroadcast();
        unregisterReceiver(cosSvc_receiver);

        if(cosSvc_gradientRefresher != null) {
            if(cosSvc_TVGradient != null)
                cosSvc_TVGradient.removeOnLayoutChangeListener(cosSvc_gradientRefresher);
            else
                cosSvc_TV.removeOnLayoutChangeListener(cosSvc_gradientRefresher);
        }

        // 부착한 뷰 제거
        if(cosSvc_FSDetector != null) {
            cosSvc_FSDetector.clearAnimation();
            ((WindowManager)getSystemService(Context.WINDOW_SERVICE)).removeView(cosSvc_FSDetector);
        }

        if(cosSvc_TV != null) {
            cosSvc_TV.clearAnimation();
            ((ViewGroup)cosSvc_OutBoundLayout).removeView(cosSvc_TV);
        }

        if(cosSvc_TVGradient != null) {
            cosSvc_TVGradient.clearAnimation();
            ((ViewGroup)cosSvc_OutBoundLayout).removeView(cosSvc_TVGradient);
        }

        if(cosSvc_OutBoundLayout != null) {
            ((ViewGroup)cosSvc_OutBoundLayout).clearAnimation();
            ((WindowManager)this.getSystemService(Context.WINDOW_SERVICE)).removeView(((ViewGroup)cosSvc_OutBoundLayout));
        }

        // 진행 중 알람 제거
        stopForeground(true);

        super.onDestroy();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mCon = getApplicationContext();

        // 서비스의 Class import 수를 최소화 하고자 사용하는 SubClass
        // PowerManager, SharedPreferences, DisplayManager, Notification 등을 처리
        COSSvcSubFunc _subClass = new COSSvcSubFunc(this);

        // 언어 설정
        _subClass.setContextLocale(this);
        // 진행 중 알람 시작
        startForeground(221, _subClass.getNotification(false));

        // 만약 화면이 꺼진 상태거나 DOZE(절전)상태이면
        // Idle 서비스를 켜고 자신을 종료
        if(!_subClass.getIsScreenOnAndNotDOZEState()) {
            startSvc_Idle();
            return;
        }

        // java.time library backport init
        AndroidThreeTen.init(mCon);

        // 기본 TextView 생성 및 software 레이어로 설정.
        cosSvc_TV = new TextView(this);
        cosSvc_TV.setLayerType(View.LAYER_TYPE_SOFTWARE, null);

        // Gradient 색상을 얻어보고 null(Gradient 미사용)이 아니면 Gradient를 사용
        // OnLayoutChangeListener를 TextView에 등록 후 뷰 크기가 변경된 경우 Gradient를 재설정
        cosSvc_gradientColors = _subClass.getGradientColors();
        if(cosSvc_gradientColors != null) {
            // Gradient + Shadow 조합인 경우 OutBoundLayout를 RelativeLayout으로 하고
            // TextView 2개를 겹쳐서 집어넣어 하나는 Gradient TextView
            // 다른 하나는 그림자만 표현하도록 함
            // 이는 TextView가 Gradient 설정 후 Shadow를 넣으면 그림자 색상이
            // Gradient의 모양과 동일하게 나타나서 그림자인지 구분이 잘 안되기 때문
            if(_subClass.getShadowEnabled()) {
                // Gradient 전용 TextView 생성 및 software 레이어로 설정.
                cosSvc_TVGradient = new TextView(this);
                cosSvc_TVGradient.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
                // Gradient + Shadow 조합인 경우
                // TextView 2개를 겹치게 넣기 위해 RelativeLayout 사용
                // RelativeLayout 생성 및 software 레이어로 설정
                cosSvc_OutBoundLayout = new RelativeLayout(this);
                ((ViewGroup)cosSvc_OutBoundLayout).setLayerType(View.LAYER_TYPE_SOFTWARE, null);
                // TextView 영역 변경 시 Gradient 재설정을 위한 리스너
                cosSvc_gradientRefresher = new View.OnLayoutChangeListener() {
                    @Override
                    public void onLayoutChange(View view, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
                        // 텍스트 뷰 크기가 변경 되었는지 확인
                        if ((right - left) != (oldRight - oldLeft) || (bottom - top) != (oldBottom - oldTop)) {
                            cosSvc_TVGradient.getPaint().setShader(new LinearGradient(0, 0,
                                    right - left, bottom - top, cosSvc_gradientColors, null, Shader.TileMode.REPEAT));
                        }
                    }
                };
                cosSvc_TVGradient.addOnLayoutChangeListener(cosSvc_gradientRefresher);
            }
            // Shadow를 쓰지 않는 경우 단순히 TextView 1개를 써도 되므로
            // OutBoundLayout를 LinearLayout으로 함
            else {
                // TextView 1개만 쓰므로 LinearLayout 사용
                // LinearLayout 생성 및 software 레이어로 설정
                cosSvc_OutBoundLayout = new LinearLayout(this);
                ((ViewGroup)cosSvc_OutBoundLayout).setLayerType(View.LAYER_TYPE_SOFTWARE, null);
                // TextView 영역 변경 시 Gradient 재설정을 위한 리스너
                cosSvc_gradientRefresher = new View.OnLayoutChangeListener() {
                    @Override
                    public void onLayoutChange(View view, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
                        // 텍스트 뷰 크기가 변경 되었는지 확인
                        if ((right - left) != (oldRight - oldLeft) || (bottom - top) != (oldBottom - oldTop)) {
                            cosSvc_TV.getPaint().setShader(new LinearGradient(0, 0,
                                    right - left, bottom - top, cosSvc_gradientColors, null, Shader.TileMode.REPEAT));
                        }
                    }
                };
                cosSvc_TV.addOnLayoutChangeListener(cosSvc_gradientRefresher);
            }
        }
        // Gradient 또는 Shadow 둘 다 쓰지 않는 경우 또한
        // TextView 1개 및 OutBoundLayout를 LinearLayout으로 함
        else {
            // LinearLayout 생성 및 software 레이어로 설정
            cosSvc_OutBoundLayout = new LinearLayout(this);
            ((ViewGroup)cosSvc_OutBoundLayout).setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        }

        // 여러 설정에 따른 값 및 현재 화면켜짐상태 등을 구하여 Status 값으로 반환받고,
        // TextView, OutBoundLayout 구성 및 화면 최상단에 넣는 작업을 수행.
        cosSvc_Status = _subClass.initSettings(this, cosSvc_TV, cosSvc_TVGradient, (ViewGroup)cosSvc_OutBoundLayout);

        // 시계를 터치 시 일정시간 동안 숨기는 기능 사용 시 필요한 설정 처리
        if(_subClass.getHideByTouch()) {
            // 숨기는 시간 설정 값을 미리 저장
            // 로직 보정을 위해 1감소한 값을 사용
            cosSvc_HidingTimeLength = (short)(_subClass.getHidingTime() - 1);
            // 가장 뷰 크기가 큰 cosSvc_OutBoundLayout에서 처리
            ((ViewGroup)cosSvc_OutBoundLayout).setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {

                    // 시계 요소가 하나도 안쓰였다면 시계 갱신이 의미가 없으므로
                    // thereAreOnlyString 값이 false 인 경우에만
                    // TextView의 내용을 초기화
                    // 5th bit check (thereAreOnlyString)
                    if((cosSvc_Status & 0b00010000) == 0) {
                        // 하위 뷰 TextView의 텍스트를 초기화
                        cosSvc_TV.setText("");
                        if (cosSvc_TVGradient != null) cosSvc_TVGradient.setText("");
                    }
                    // 시계 숨기기
                    // OutBoundLayout만 사라지게 만들면 알아서 하위 뷰도 사라진다
                    ((ViewGroup)cosSvc_OutBoundLayout).setVisibility(View.GONE);
                    // 숨기는 시간을 설정
                    cosSvc_HidingTime = cosSvc_HidingTimeLength;
                }
            });
        }

        // 롱터치시 시계 표시 조건을 만족 할 때 까지 시계를 계속 숨기는 기능
        if(_subClass.getHideTemporaryByLongTouch()) {
            // 가장 뷰 크기가 큰 cosSvc_OutBoundLayout에서 처리
            ((ViewGroup)cosSvc_OutBoundLayout).setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View view) {
                    // 풀스크린 모드를 사용하는 경우
                    // 화면을 끄고 켜거나 풀스크린 상태가 바뀌는 경우에 서비스 재시작
                    // 8th bit check (bFSModeEnabled)
                    if((cosSvc_Status & 0b10000000) != 0) {
                        Toast.makeText(COSSvc.this, R.string.pref_toast_hidetheclocktemporary_fsmode, Toast.LENGTH_LONG).show();
                    }
                    // 풀스크린 모드를 안쓰는 경우
                    // 화면을 끄고 켜는 경우에만 서비스 재시작
                    else {
                        Toast.makeText(COSSvc.this, R.string.pref_toast_hidetheclocktemporary_notfsmode, Toast.LENGTH_LONG).show();
                    }
                    if (cosSvc_repeater != null) {
                        while (true) {
                            if (cosSvc_repeater.cancel(true)) break;
                        }
                        // cosSvc_repeater을 null로 만들어서
                        // 화면 회전 시 cosSvc_repeater 가 null이 아닌 경우에만
                        // 서비스를 재시작하도록 함
                        // 화면 회전 시 작동 방식은 onConfigurationChanged 참조
                        cosSvc_repeater = null;
                    }
                    // 하위 뷰 TextView의 텍스트를 초기화
                    cosSvc_TV.setText("");
                    if(cosSvc_TVGradient != null) cosSvc_TVGradient.setText("");
                    // 시계 숨기기
                    ((ViewGroup)cosSvc_OutBoundLayout).setVisibility(View.GONE);
                    return false;
                }
            });
        }

        // Full Screen Mode가 쓰이는지 확인 및 적용
        // 8th bit check (bFSModeEnabled)
        if((cosSvc_Status & 0b10000000) != 0) {
            // 풀스크린 디텍터를 생성 및 software 레이어로 설정
            cosSvc_FSDetector = new FSDetector(this);
            cosSvc_FSDetector.setLayerType(View.LAYER_TYPE_SOFTWARE, null);

            // FSDetector를 최상단에 넣기 위한 layout 설정 값
            int __type;
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                __type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
            else
                __type = WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY;
            // Fix for the cannot click install on sideloaded apps bug
            // Dont fill width, set to left side with a few pixels wide
            WindowManager.LayoutParams layout = new WindowManager.LayoutParams(
                    1, WindowManager.LayoutParams.MATCH_PARENT,
                    __type,
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE |
                            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                    PixelFormat.TRANSPARENT);
            layout.gravity = Gravity.LEFT;
            // 화면 최상단에 FSDetector를 삽입
            ((WindowManager)getSystemService(Context.WINDOW_SERVICE)).addView(cosSvc_FSDetector, layout);

            // onFullScreenListener의 경우 onStartCommand에서 부착
            // cosSvc_FSDetector가 null이 아닐 때 Intent를 확인해서
            // 설정 창이 아닌 경우에만 onFullScreenListener 부착
        }

        // 화면 꺼짐, 시간변경(3개) 액션을 분별하는 필터 생성
        IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_OFF);
        filter.addAction(Intent.ACTION_DATE_CHANGED);
        filter.addAction(Intent.ACTION_TIMEZONE_CHANGED);
        filter.addAction(Intent.ACTION_TIME_CHANGED);
        // 배터리 값을 사용하면 배터리 상태 액션을 분별하는 필터 추가
        // 2nd bit check (isUsing_Battery)
        if((cosSvc_Status & 0b00000010) != 0) {
            filter.addAction(Intent.ACTION_BATTERY_CHANGED);
            // 배터리에서 사용할 StringBuilder
            cosSvc_strBattLevelBuilder = new StringBuilder();
            cosSvc_strBattBuilder = new StringBuilder();
        }
        // 리시버 등록
        registerReceiver(cosSvc_receiver, filter);

        // 분 단위 이상의 요소를 모두 처리한 문자열을 저장하는 StringBuilder
        // 초 단위 요소가 있을 경우 각 요소에 대응되는 특수 값
        // (\uF000 ~ \uF002, 유니코드 Private Use Area)이 저장 되는 곳
        cosSvc_overMinFormat = new StringBuilder();
        // 최종 시계 문자열을 만들기 위한 StringBuilder
        cosSvc_ClockText = new StringBuilder();
        // 초 단위 요소가 안쓰인 경우 처음에 시계가 출력되지 않으므로
        // 초기 한번 업데이트를 위해 7비트를 켜준다.
        // 6th bit check (isUsing_SecElement)
        if((cosSvc_Status & 0b00100000) == 0)
            // 7th bit on (overMin_need_update)
            cosSvc_Status |= 0b01000000;
        // 시계 구조 formatter 정의
        cosSvc_formatter =
                DateTimeFormatter.ofPattern(_subClass.getClockTextFormat())
                        .withLocale(_subClass.getLocale())
                        .withZone(ZoneId.systemDefault());
        // 현재 시간
        cosSvc_current = Instant.now();
        // 초 정보 저장
        cosSvc_second = (byte)(cosSvc_current.getEpochSecond() % 60);
        // 초, 배터리를 제외한 모든 요소를 처리한 문자열을 저장
        cosSvc_overMinFormat.append(cosSvc_formatter.format(cosSvc_current));
		// 1초마다 반복 처리하는 스케쥴러 등록
        // Runnable을 서비스 자신으로 등록한다.
		if(cosSvc_repeater == null) {
            cosSvc_repeater = Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate
                    (this, 1000 - (cosSvc_current.getNano() / 1000000), 1000, TimeUnit.MILLISECONDS);
        }
        // 처음 호출에 TextView에는 빈 내용이 체워지고
        // 현재 시간을 기준으로 cosSvc_ClockText 내용이 체워지며
        // 저장된 시간이 1초 증가한다(현재 시간 + 1초)
        // 두번째 호출에 cosSvc_ClockText에 체워진 내용을 TextView에 표시하고
        // 처음 호출 때 1초 증가된 시간(현재 시간 + 1초) 기준으로
        // cosSvc_ClockText 내용을 체운다
        // 또 저장된 시간이 1초 증가한다(현재 시간 + 2초)
        // 이후 cosSvc_repeater에 의해 휴대폰 시간 상 다음 초(현재 시간 + 1초)가 되는 순간
        // sendEmptyMessage 가 발생하고 cosSvc_ClockText의 내용을
        // TextView에 표시한다. 이 때 cosSvc_ClockText안에는
        // 1초 증가된 시간(현재 시간 + 1초)기준의 내용이 담겨있으므로
        // 시간이 맞아 떨어지게 된다
        //
        // mHandler의 handleMessage를 보면 일단 TextView.setText부터 하고
        // 다음에 설정할 텍스트 내용을 갱신 후 시간을 1초 증가하는 방식으로 하였기 때문이다
        //
        // cosSvc_repeater가 아래 호출 보다 먼저 sendEmptyMessage를 하더라도
        // 총 3번 보내야 시간이 맞기 때문에 상관없다
        mHandler.sendEmptyMessage(0);
        mHandler.sendEmptyMessage(0);
    }

    // 인턴트 최초 인식 가능 위치
    // 풀스크린 디텍터 활성화 및 설정 창 여부를 확인하고
    // 설정 창이면 풀스크린 디텍터를 제거하고
    // 설정 창이 아니면 풀스크린 디텍터에 onFullScreenListener 부착
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        // FSDetector가 생성 되었는지 확인
        if(cosSvc_FSDetector != null) {
            // 설정에서 온 경우
            if(intent != null && intent.getBooleanExtra("PreferenceView", false)) {
                ((WindowManager)this.getSystemService(Context.WINDOW_SERVICE)).removeView(cosSvc_FSDetector);
                // 서비스 실행 도중 서비스가 호출 되는 경우 onStartCommand가 실행될 때
                // cosSvc_FSDetector가 null이 아니면 이곳으로 다시 오는 것도 있고,
                // 추후 onDestroy에서 removeView를 할지 안할지를 null 값으로 판단하므로
                cosSvc_FSDetector = null;
            }
            // 설정 창이 아닌 경우 hasOnFullScreenListener 확인하여
            // OnFullScreenListener가 부착되지 않은 경우에만 처리
            else if(!cosSvc_FSDetector.hasOnFullScreenListener()) {
                // FSMode를 알기 위해 SubClass 재호출
                COSSvcSubFunc _subClass = new COSSvcSubFunc(this);
                switch(_subClass.getFSMode()) {
                    case 1:
                        // 풀스크린 상태에서만 시계 표시
                        cosSvc_FSDetector.setOnFullScreenListener(new OnFullScreenListener() {
                            @Override
                            public void fsChanged(Context context, boolean bIsFS) {
                                // 풀스크린 상태가 아닐 경우 Idle 서비스로 진입
                                if (!bIsFS) {
                                    startSvc_Idle();
                                }
                            }
                        });
                        break;
                    case 2:
                        // 풀스크린 상태에 따라 다른 시계 표시
                        // 저장된 풀스크린 상태 값을 불러옴
                        cosSvc_FSSaved = _subClass.getFSSaved();
                        cosSvc_FSDetector.setOnFullScreenListener(new OnFullScreenListener() {
                            @Override
                            public void fsChanged(Context context, boolean bIsFS) {
                                // 풀스크린 상태가 설정에 저장된 값과 다를 경우
                                // 바뀐 풀스크린 상태를 저장하고 그에 맞는 시계 구조 및
                                // 위치를 불러오기 위해 COSSvc 서비스 재시작
                                if (cosSvc_FSSaved != bIsFS) {
                                    new COSSvcSubFunc(mCon).saveFS(bIsFS);
                                    startSvc_Idle();
                                }
                            }
                        });
                        break;
                }
            }
        }
        // 서비스 유지
        return START_STICKY;
    }

    // TextView만 wrap_content로 최상단에 넣으면
    // 가로 길이가 자유롭게 늘어날 수 있으나
    // TextView의 레이아웃 크기가 변할 때 발생하는 애니메이션이
    // 좌우로 떨리며 나타나서 보기 안좋다
    //
    // 따라서 현재 화면의 가로 길이를 최대로 한
    // 현재 설정에 따른 TextView의 최대 크기를 계산 한 뒤
    // 그 크기와 동일한 크기를 가진 외각 레이아웃을 먼저
    // 화면의 최상단에 넣은 것이 cosSvc_OutBoundLayout이다
    //
    // 그런데 화면 회전이 발생하는 경우 화면의 가로 길이가 달라져서
    // 가로 -> 세로 회전 시 cosSvc_OutBoundLayout 최대 크기의 가로가
    // 너무 커서 세로 화면에 못 들어 가거나
    // 세로 -> 가로 회전 시 cosSvc_OutBoundLayout 최대 크기의 가로가
    // 더 커질 수 있는 경우가 있을 수 있다
    //
    // 따라서 화면 회전을 onConfigurationChanged에서 탐지하여
    // cosSvc_OutBoundLayout의 크기를 재조정을 위한
    // 서비스 재시작을 여기서 수행한다
    //
    // 이를 위해 AndroidManifest.xml에서 현재 서비스에 아래 항목을 추가해야 한다
    // android:configChanges="orientation|screenSize"
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        // 롱터치로 시계를 끈 경우 cosSvc_repeater가 null이 된다
        // 따라서 cosSvc_repeater 값이 null이 아닌 경우에만 서비스 재시작
        if(cosSvc_repeater != null) {
            startSvc_Idle();
        }
        super.onConfigurationChanged(newConfig);
    }

    // cosSvc_repeater로 인해 매초 실행되는 부분
    public void run() {
        // 매초마다 mHandler에 Message 전송
        mHandler.sendEmptyMessage(0);
    }

    // 매 초마다 실행. 따라서 힙에서 사라지면 안됨...
    @SuppressLint("HandlerLeak")
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            // 1st bit check (onDestroy called)
            if((cosSvc_Status & 0b00000001) != 0) return;

            // cosSvc_HidingTime 값이 0이면 시계 내용 업데이트
            if (cosSvc_HidingTime == 0) {
                // 실제로 문자열 업데이트가 이루어지는 곳
                // 초 단위가 쓰인경우 초, 배터리 문자열을 처리하고 시계를 갱신
                // 6th bit check (isUsing_SecElement)
                if((cosSvc_Status & 0b00100000) != 0) {
                    // cosSvc_ClockText에 저장된 문자열로 TextView를 업데이트를 가장 먼저 수행
                    // 즉, 매초마다 일단 문자열 갱신을 최우선으로 하고
                    // 다음 갱신 때 필요한 문자열을 뒤에 만들어서
                    // 최대한 0.000초에 가까운 시기에 시계가 갱신되도록 함
                    //
                    // 시스템 렉 발생으로 Message를 처리 못한 경우 대비
                    // mHandler 내 Message가 없는 경우에만 setText
                    if(!mHandler.hasMessages(0)) {
                        cosSvc_TV.setText(cosSvc_ClockText);
                        if (cosSvc_TVGradient != null) cosSvc_TVGradient.setText(cosSvc_ClockText);
                    }
                    // 시계 내용(cosSvc_ClockText) 업데이트
                    updateClockText();
                }
                // 시스템 렉 발생으로 Message를 처리 못한 경우 대비
                // mHandler 내 Message가 없는 경우에만 setText
                // + 초 단위가 안 쓰인경우 분 단위 이상의 요소는
                // 이미 cosSvc_overMinFormat안에 저장되어 있으므로 바로 시계를 갱신한다
                // 7th bit check (overMin_need_update)
                else if((cosSvc_Status & 0b01000000) != 0 && !mHandler.hasMessages(0)) {
                    cosSvc_TV.setText(cosSvc_overMinFormat);
                    if(cosSvc_TVGradient != null) cosSvc_TVGradient.setText(cosSvc_overMinFormat);
                    // 7th bit off (overMin_need_update)
                    cosSvc_Status &= 0b10111111;
                }
            }
            // cosSvc_HidingTime 값이 0이 아니면 터치로 숨기기 상태
            // 이 경우 cosSvc_HidingTime 값을 1 줄이고 만약 0이 되면
            // 시계 레이아웃만 다시 보이도록 만들고 1초 뒤 시계가 갱신된다
            // cosSvc_HidingTimeLength 값을 로직 보정을 위해 1 감소한 이유
            else {
                cosSvc_HidingTime--;
                if(cosSvc_HidingTime == 0) {
                    // OutBoundLayout 하위 뷰(TextView)는 이미 보이는 상태이므로
                    // OutBoundLayout만 보이게 만들면 된다
                    ((ViewGroup)cosSvc_OutBoundLayout).setVisibility(View.VISIBLE);
                    // 시계 내용(cosSvc_ClockText) 업데이트
                    updateClockText();
                    // 초 단위 요소가 안쓰인 경우 분 단위는 바로 업데이트가 안되므로
                    // overMin_need_update를 켜서 숨기는 시간이 끝나자 마자 업데이트 하도록 함
                    // 다만 시계 요소가 하나도 안쓰였다면 시계를 숨길 때
                    // TextView의 내용을 초기화 하지 않으므로 갱신할 필요도 없다
                    // 따라서 thereAreOnlyString, isUsing_SecElement 둘 다
                    // false 이면 갱신하도록 한다
                    // 5th bit check (thereAreOnlyString)
                    // 6th bit check (isUsing_SecElement)
                    if((cosSvc_Status & 0b00110000) == 0)
                        // 7th bit on (overMin_need_update)
                        cosSvc_Status |= 0b01000000;
                }
            }

            // 시계 요소가 하나도 안쓰였다면 1초를 더할 필요가 없다
            // 5th bit check (thereAreOnlyString)
            if((cosSvc_Status & 0b00010000) != 0) return;

            // 시간을 더하는 부분
            // 1초를 더한다
            cosSvc_second++;
            // 만약 1분 단위가 증가 하는 경우 현재 시간을 새로 얻어와서
            // formatter를 통해 1분 이상 시간 단위 영역의 문자열을 갱신한다
            if(cosSvc_second == 60) {
                // 저장해둔 Instant에 60초를 더함
                cosSvc_current = Instant.ofEpochSecond(cosSvc_current.getEpochSecond() + 60);

                // 혹시 cosSvc_formatter가 GC 된 경우 서비스 재시작
                if(cosSvc_formatter == null) {
                    startSvc_Idle();
                }
                // 초, 배터리를 제외한 모든 요소를 처리한 문자열을 갱신
                cosSvc_overMinFormat.setLength(0);
                cosSvc_overMinFormat.append(cosSvc_formatter.format(cosSvc_current));
                cosSvc_second = 0;

                // 초 단위 요소가 안쓰인 경우에만 분 단위 이상 업데이트
                // 6th bit check (isUsing_SecElement)
                if((cosSvc_Status & 0b00100000) == 0)
                    // 7th bit on (overMin_need_update)
                    cosSvc_Status |= 0b01000000;
            }
        }

        // 시계 내용(cosSvc_ClockText)만 업데이트
        private void updateClockText() {
            // cosSvc_ClockText 초기화
            cosSvc_ClockText.setLength(0);
            // 다음 업데이트에 필요한 문자열을 계산
            // cosSvc_overMinFormat의 내용을 cosSvc_ClockText에 복사
            cosSvc_ClockText.append(cosSvc_overMinFormat);
            // 배터리가 완충/충전/방전 상태 시 문자열 미리 생성
            // 3rd, 4th bit check (isBattery_Charging)(isBattery_Discharging)
            if((cosSvc_Status & 0b00001100) != 0) {
                cosSvc_strBattBuilder.setLength(0);

                cosSvc_strBattBuilder.append(cosSvc_strBattLevelBuilder);
                // 완충 상태 이거나
                // 3rd, 4th bit check (isBattery_Charging)(isBattery_Discharging)
                if((cosSvc_Status & 0b00001100) == 0b00001100) {
                    cosSvc_strBattBuilder.append('◎');
                }
                // 충전기를 사용중이거나
                // 3rd bit check (isBattery_Charging)
                else if((cosSvc_Status & 0b00000100) != 0) {
                    if((cosSvc_second & 0x1) == 1) cosSvc_strBattBuilder.append('△');
                    else cosSvc_strBattBuilder.append('▲');
                }
                // 배터리가 15퍼 미만인 경우.
                // 4th bit check (isBattery_Discharging)
                else {
                    if((cosSvc_second & 0x1) == 1) cosSvc_strBattBuilder.append('▽');
                    else  cosSvc_strBattBuilder.append('▼');
                }
            }

            int index;
            // 초(0~59) 처리
            index = cosSvc_ClockText.indexOf("\uF002");
            while(index != -1) {
                cosSvc_ClockText.deleteCharAt(index);
                cosSvc_ClockText.insert(index, cosSvc_second);
                index = cosSvc_ClockText.indexOf("\uF002");
            }
            // 초(00~59) 처리
            index = cosSvc_ClockText.indexOf("\uF001");
            while(index != -1) {
                cosSvc_ClockText.deleteCharAt(index);
                if(cosSvc_second < 10) cosSvc_ClockText.insert(index++, 0);
                cosSvc_ClockText.insert(index, cosSvc_second);
                index = cosSvc_ClockText.indexOf("\uF001");
            }

            // 배터리 처리
            index = cosSvc_ClockText.indexOf("\uF000");
            while(index != -1) {
                cosSvc_ClockText.deleteCharAt(index);
                // 배터리가 완충, 충전, 방전 상태가 아니면 현재 배터리 잔량을 바로 표기
                if((cosSvc_Status & 0b00001100) == 0)
                    cosSvc_ClockText.insert(index, cosSvc_strBattLevelBuilder);
                    // 완충, 충전, 방전 상태에 따른 처리
                else
                    cosSvc_ClockText.insert(index, cosSvc_strBattBuilder);
                index = cosSvc_ClockText.indexOf("\uF000");
            }
        }
    };
}
