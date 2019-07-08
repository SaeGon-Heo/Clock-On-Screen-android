/*
 * Copyright 2014-2019 SaeGon Heo
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
import android.graphics.Point;
import android.graphics.Shader;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;
import android.view.Display;
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

import java.util.Locale;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public final class COSSvc extends Service implements Runnable {
    // 반복 사용 될 단순 문자 및 문자열 저장
    private static final char CHAR_PERCENT = '%';
    private static final char CHAR_BATTSTATE_FULL = '◎';
    private static final char CHAR_BATTSTATE_CHARGING1 = '△';
    private static final char CHAR_BATTSTATE_CHARGING2 = '▲';
    private static final char CHAR_BATTSTATE_DISCHARGING1 = '▽';
    private static final char CHAR_BATTSTATE_DISCHARGING2 = '▼';
    private static final char CHAR_NETSTATE_NONE = '⌀';
    private static final char CHAR_NETSTATE_CELLULAR = '⇵';
    private static final char CHAR_NETSTATE_WIFI = '≋';
    private static final char CHAR_NETSTATE_ETHERNET = '⌂';
    private static final String STR_EMPTY = "";
    private static final String STR_SYMBOL_SECOND = "\uF002";
    private static final String STR_SYMBOL_SECOND_FILLZERO = "\uF001";
    private static final String STR_SYMBOL_BATT = "\uF000";
    private static final String STR_SYMBOL_NETWORKSTATE = "\uF003";

    // Network State 상수
    private static final byte NETSTATE_NONE = 0;
    private static final byte NETSTATE_CELLULAR = 1;
    private static final byte NETSTATE_WIFI = 2;
    private static final byte NETSTATE_CELLULAR_WIFI = 3;
    private static final byte NETSTATE_ETHERNET = 4;

    // Layout Attach State 상수
    private static final byte LAYOUT_IS_DETACHED = 0;
    private static final byte LAYOUT_IS_ATTACHED = 1;
    private static final byte LAYOUT_IS_DETACHING = 2;
    private static final byte LAYOUT_IS_ATTACHING = 3;

    private Context mCon;
    //항상 보이게 할 뷰
    private TextView cosSvc_TV;
    private TextView cosSvc_TVGradient;
    private Object cosSvc_OutBoundLayout;
    private FSDetector cosSvc_FSDetector;
    private byte cosSvc_FSMode;
    // 풀스크린 상태는 풀스크린 상태에 따라 시계를 다르게 표시할 경우에만
    // 값이 변화하며, 그렇지 않으면 항상 true 이다
    private boolean cosSvc_FSState;
    private WindowManager cosSvc_winManager;
    private ScheduledFuture<?> cosSvc_repeater;
    private Instant cosSvc_current;
    private DateTimeFormatter cosSvc_formatter;
    private Locale cosSvc_Locale;
    private StringBuilder cosSvc_FinalClockTextExceptSecond = new StringBuilder();
    private StringBuilder cosSvc_FinalClockText = new StringBuilder();
    private StringBuilder cosSvc_strBattLevelBuilder;
    private StringBuilder cosSvc_strBattBuilder;
    private StringBuilder cosSvc_strNetStateBuilder;
    private String cosSvc_ClockTextFormatted;
    private String cosSvc_ClockTextMax;
    private String cosSvc_ClockTextFormatted_notfs;
    private String cosSvc_ClockTextMax_notfs;
    private byte cosSvc_ClockPosition;
    private byte cosSvc_ClockPosition_notfs;
    private int[] cosSvc_gradientColors;
    private short cosSvc_HidingTimeLength;
    private short cosSvc_HidingTime;
    // Each bit have meaning as below
    // (10.onDestroy_called)
    // ( 9.isUsing_Network_State)
    // ( 8.bTouchEvent)
    // ( 7.overMin_need_update)
    // ( 6.isUsing_SecElement)
    // ( 5.thereAreOnlyString)
    // ( 4.isBattery_Discharging)
    // ( 3.isBattery_Charging)
    // ( 2.isUsing_Battery)
    // ( 1.InterruptHandler)
    private short cosSvc_Status;
    private short cosSvc_InitStatus;
    private short cosSvc_InitStatus_notfs;
    private byte cosSvc_second;
    private byte cosSvc_layoutAttachState;
    private View.OnLayoutChangeListener cosSvc_gradientRefresher;
    private ConnectivityManager cosSvc_connManager;
    private BroadcastReceiver cosSvc_receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // action null 여부 확인
            String action = intent.getAction();
            if(action == null) return;

            switch (action) {
                // 화면이 꺼지면 Idle 서비스로 전환.
                case Intent.ACTION_SCREEN_OFF:
                    startSvc_Idle();
                    break;
                // 시간이 변경되면 시간을 재설정 한다.
                case Intent.ACTION_DATE_CHANGED:
                case Intent.ACTION_TIMEZONE_CHANGED:
                case Intent.ACTION_TIME_CHANGED:
                    // 문자열만 존재하는 경우를 제외하고 시간을 갱신
                    // 5th bit check (thereAreOnlyString)
                    if((cosSvc_Status & 0b00_0001_0000) == 0) {
                        mHandler.removeMessages(0);
                        reloadCurrentTime(false);
                    }
                    break;
                // 배터리 상태 변경 시 변경 내용을 StringBuilder에 저장
                case Intent.ACTION_BATTERY_CHANGED:
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
                        cosSvc_Status &= 0b11_1111_0011;
                        // 3rd bit on (isBattery_Charging)
                        cosSvc_Status |= 0b00_0000_0100;
                        if (level == 100) {
                            // 4th bit on (isBattery_Discharging)
                            // 3,4 bit 둘다 켜져있으면 풀충전 상태로 간주
                            cosSvc_Status |= 0b00_0000_1000;
                        }
                    } else if (level < 16) {
                        // 3rd, 4th bit reset
                        cosSvc_Status &= 0b11_1111_0011;
                        // 4th bit on (isBattery_Discharging)
                        cosSvc_Status |= 0b00_0000_1000;
                    } else {
                        // 3rd, 4th bit reset
                        cosSvc_Status &= 0b11_1111_0011;
                    }
                    // 마지막 % 추가
                    cosSvc_strBattLevelBuilder.append(CHAR_PERCENT);
                    break;
                // 네트워크 상태가 변경 될 때 마다
                // 전체 네트워크를 검사하여 현재 활성화된 네트워크를 탐색
                case ConnectivityManager.CONNECTIVITY_ACTION:
                    // 총 발견된 네트워크 수를 저장
                    byte network_count = 0;
                    // 삼성의 다운로드 부스터 및 band LTE WiFi 사용 여부 판별
                    boolean bCellularHipri = false;

                    // 초기 값 지정
                    byte netState = COSSvc.NETSTATE_NONE;

                    // EXTRA_NO_CONNECTIVITY 값 확인 후 false 인 경우 상세 정보 확인
                    if(!intent.getBooleanExtra(ConnectivityManager.EXTRA_NO_CONNECTIVITY, false)) {
                        // API 21(LOLLIPOP) 이상에서는 NetworkCapabilities를 사용
                        // getActiveNetwork() 함수를 쓸수도 있지만
                        // 만약 사용자가 VPN 사용 시 getActiveNetwork()는
                        // VPN 네트워크를 반환하므로 아래와 같은 방법을 사용
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                            // 현재 존재하는 모든 네트워크를 먼저 검사
                            for (Network net : cosSvc_connManager.getAllNetworks()) {
                                NetworkCapabilities netCap = cosSvc_connManager.getNetworkCapabilities(net);
                                if (netCap == null) continue;

                                // 활성화된 네트워크 종류 확인 및 저장 후 네트워크 총 개수 1 증가
                                if (netCap.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) {
                                    if (netCap.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
                                        network_count++;
                                        // 삼성의 다운로드 부스터 및 band LTE WiFi 사용 여부 판별
                                        NetworkInfo cellularNetInfo = cosSvc_connManager.getNetworkInfo(net);
                                        if(cellularNetInfo != null && cellularNetInfo.getType() == ConnectivityManager.TYPE_MOBILE_HIPRI &&
                                                cellularNetInfo.isConnected()) {
                                            bCellularHipri = true;
                                        }
                                        netState += COSSvc.NETSTATE_CELLULAR;
                                    } else if (netCap.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                                        network_count++;
                                        netState += COSSvc.NETSTATE_WIFI;
                                    } else if (netCap.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) {
                                        network_count++;
                                        netState += COSSvc.NETSTATE_ETHERNET;
                                    }
                                }
                            }

                            // 만약 발견된 네트워크가 없다면 netState의 초기 값인
                            // 네트워크 없음 상태값이 유지되고
                            // 네트워크가 하나만 발견된 경우 해당 네트워크 값만 netState에 남는다

                            // 만약 발견된 네트워크 수가 2개 이상이면 현재 활성화된 네트워크를 탐색
                            if (network_count > 1) {
                                // 기본적으로 2개 이상이 탐지되면
                                // netState에 저장된 값은 1 / 2 / 4 중 하나가 되지 않는다
                                // (NETSTATE_CELLULAR(1) / WIFI(2) / ETHERNET(4))
                                //
                                // 그리고 숫자가 클 수록 더 높은 우선순위를 가진 네트워크로
                                // 특정 네트워크 x의 값보다 큰 경우
                                // 현재 활성화된 네트워크는 x라고 볼 수 있다
                                //
                                // NetworkInfo에서 Type 값이 TYPE_MOBILE_HIPRI로 나타나면
                                // 셀룰러 네트워크의 우선순위가 높아지지만
                                // Mobile DNS servers 로만 접속이 가능해진다고 한다
                                //
                                // 그런데 삼성의 다운로드 부스터 및 band LTE WiFi 사용 시
                                // 기존의 셀룰러 네트워크 NetworkInfo의 Type 값이
                                // TYPE_MOBILE_HIPRI로 변화한다
                                //
                                // 따라서 셀룰러 네트워크에 한해 NetworkInfo의 Type 값을 확인하여
                                // TYPE_MOBILE_HIPRI 여부를 저장한 bCellularHipri 값을 활용하여
                                // 다운로드 부스터 및 band LTE WiFi의 활성화 여부를 출력한다

                                // 만약 ethernet + 기타 네트워크가 활성화된 경우
                                // ethernet 사용 상태로 처리
                                if (netState > COSSvc.NETSTATE_ETHERNET) {
                                    netState = COSSvc.NETSTATE_ETHERNET;
                                }
                                // 만약 wifi + Cellular 네트워크가 활성화된 경우
                                else if (netState > COSSvc.NETSTATE_WIFI) {
                                    netState = COSSvc.NETSTATE_WIFI;
                                    // 셀룰러 네트워크 NetworkInfo Type 값이 TYPE_MOBILE_HIPRI이면
                                    if (bCellularHipri) {
                                        // 다운로드 부스터 및 band LTE WiFi 사용 상태
                                        netState = COSSvc.NETSTATE_CELLULAR_WIFI;
                                    }
                                }
                            } // if(network_count > 1)
                        } // LOLLIPOP 이상 API
                        // API 21(LOLLIPOP) 미만에서는 예전 방식인 NetworkInfo를 사용
                        else {
                            // 현재 존재하는 모든 네트워크 조사
                            for (NetworkInfo netinfo : cosSvc_connManager.getAllNetworkInfo()) {
                                if (netinfo == null) continue;

                                // 활성화된 네트워크 종류 확인 및 저장 후 네트워크 총 개수 1 증가
                                if (netinfo.isConnected()) {
                                    switch(netinfo.getType()) {
                                        case ConnectivityManager.TYPE_MOBILE_HIPRI:
                                            bCellularHipri = true;
                                        case ConnectivityManager.TYPE_MOBILE:
                                            network_count++;
                                            netState += COSSvc.NETSTATE_CELLULAR;
                                            break;
                                        case ConnectivityManager.TYPE_WIFI:
                                            network_count++;
                                            netState += COSSvc.NETSTATE_WIFI;
                                            break;
                                        case ConnectivityManager.TYPE_ETHERNET:
                                            network_count++;
                                            netState += COSSvc.NETSTATE_ETHERNET;
                                            break;
                                    }
                                }
                            }

                            // 만약 발견된 네트워크가 없다면 netState의 초기 값인
                            // 네트워크 없음 상태값이 유지되고
                            // 네트워크가 하나만 발견된 경우 해당 네트워크 값만 netState에 남는다

                            // 만약 발견된 네트워크 수가 2개 이상이면 현재 활성화된 네트워크를 탐색
                            if (network_count > 1) {
                                // 기본적으로 2개 이상이 탐지되면
                                // netState에 저장된 값은 1 / 2 / 4 중 하나가 되지 않는다
                                // (NETSTATE_CELLULAR(1) / WIFI(2) / ETHERNET(4))
                                //
                                // 그리고 숫자가 클 수록 더 높은 우선순위를 가진 네트워크로
                                // 특정 네트워크 x의 값보다 큰 경우
                                // 현재 활성화된 네트워크는 x라고 볼 수 있다

                                // 만약 ethernet + 기타 네트워크가 활성화된 경우
                                // ethernet 사용 상태로 처리
                                if (netState > COSSvc.NETSTATE_ETHERNET) {
                                    netState = COSSvc.NETSTATE_ETHERNET;
                                }
                                // 만약 wifi + Cellular 네트워크가 활성화된 경우
                                else if (netState > COSSvc.NETSTATE_WIFI) {
                                    netState = COSSvc.NETSTATE_WIFI;
                                    // 셀룰러 네트워크 NetworkInfo Type 값이 TYPE_MOBILE_HIPRI이면
                                    if (bCellularHipri) {
                                        // 다운로드 부스터 및 band LTE WiFi 사용 상태
                                        netState = COSSvc.NETSTATE_CELLULAR_WIFI;
                                    }
                                }
                            } // if(network_count > 1)
                        } // LOLLIPOP 미만 API
                    } // EXTRA_NO_CONNECTIVITY check

                    // 최종 획득한 상태 값을 기준으로 최종 문자열을 미리 저장
                    cosSvc_strNetStateBuilder.setLength(0);

                    switch(netState) {
                        case COSSvc.NETSTATE_NONE:
                            cosSvc_strNetStateBuilder.append(CHAR_NETSTATE_NONE);
                            break;
                        case COSSvc.NETSTATE_CELLULAR_WIFI:
                            cosSvc_strNetStateBuilder.append(CHAR_NETSTATE_WIFI);
                        case COSSvc.NETSTATE_CELLULAR:
                            cosSvc_strNetStateBuilder.append(CHAR_NETSTATE_CELLULAR);
                            break;
                        case COSSvc.NETSTATE_WIFI:
                            cosSvc_strNetStateBuilder.append(CHAR_NETSTATE_WIFI);
                            break;
                        case COSSvc.NETSTATE_ETHERNET:
                            cosSvc_strNetStateBuilder.append(CHAR_NETSTATE_ETHERNET);
                    }
                    break;
            }
        }
    };

    // 현재 시스템 시간을 불러와 저장
    void reloadCurrentTime(boolean bForceResetFormatter) {
        // 시계 구조 formatter 정의
        if(cosSvc_formatter == null || bForceResetFormatter) {
            // 풀스크린 상태에 따라 알맞는 시계 구조 포맷을 불러온다
            if (cosSvc_FSState) {
                cosSvc_formatter =
                        DateTimeFormatter.ofPattern(cosSvc_ClockTextFormatted)
                                .withLocale(cosSvc_Locale)
                                .withZone(ZoneId.systemDefault());
            } else {
                cosSvc_formatter =
                        DateTimeFormatter.ofPattern(cosSvc_ClockTextFormatted_notfs)
                                .withLocale(cosSvc_Locale)
                                .withZone(ZoneId.systemDefault());
            }
        }
        // 현재 시간
        cosSvc_current = Instant.now();
        // 초 정보 저장
        cosSvc_second = (byte) (cosSvc_current.getEpochSecond() % 60);
        // 초, 배터리를 제외한 모든 요소를 처리한 문자열을 재설정
        cosSvc_FinalClockTextExceptSecond.setLength(0);
        cosSvc_FinalClockTextExceptSecond.append(cosSvc_formatter.format(cosSvc_current));

        // 다음 업데이트에 필요한 초 단위 요소를 제외한 문자열을
        // 최종 문자열 빌더에 저장
        // cosSvc_FinalClockText 초기화
        cosSvc_FinalClockText.setLength(0);
        // cosSvc_FinalClockTextExceptSecond의 내용을
        // cosSvc_FinalClockText(최종 문자열 빌더)에 복사
        cosSvc_FinalClockText.append(cosSvc_FinalClockTextExceptSecond);

        // 시간 재설정 이후 초 단위 요소가 안 쓰인 경우
        // 바로 한번 시계 갱신을 위한 추가 처리
        // 6th bit check (isUsing_SecElement)
        if ((cosSvc_Status & 0b00_0010_0000) == 0)
            // 7th bit on (overMin_need_update)
            cosSvc_Status |= 0b00_0100_0000;


        // cosSvc_repeater는 이미 실행중인 상태이므로
        // 시간을 갱신하고 1개의 메세지를 전송하면
        // 처음 호출에 TextView에는 시간 갱신 당시
        // 저장된 시간이 나타나며
        // 저장된 시간이 1초 증가한다(저장된 시간 + 1초)
        //
        // ex) 15.498초에 시간이 갱신된 경우
        //     cosSvc_second에 15초로 저장한다
        //
        // 이 때 cosSvc_repeater에 의해 16.000초가 되는 순간
        // sendEmptyMessage를 실행하므로 시간이 알맞게 조절된다
        //
        // ex) 32.768초에 시간이 갱신된 경우 32초가 cosSvc_second에 저장되고
        //     여기서 한번 호출함으로써 32초 기준 시계를 표시하고
        //     저장된 시간은 33초가 된다
        //     그리고 cosSvc_repeater에 의해 33초가 되는 순간
        //     저장된 시간(33초) 기준 시계를 표시한다
        //
        // 또한 cosSvc_repeater가 아래 호출 보다 먼저 sendEmptyMessage를 하더라도
        // 총 2번 보내야 시간이 맞기 때문에 상관없다
        //
        // cosSvc_HidingTime 값이 0이 아닌 경우 시계는
        // 터치로 숨기기 상태이고, 이 상태로 msg 1개 날리는 경우
        // handleMessage 실행 도중에 숨기는 시간을 1초 줄여버리므로
        // 숨기는 시간을 1초 늘려준다
        if(cosSvc_HidingTime > 0) {
            cosSvc_HidingTime++;
        }
        mHandler.sendEmptyMessage(0);
    }

    // cosSvc_FSState 값 기준 작업
    void attachLayout() {
        // Layout Attach 상태에 따른 분기
        switch(cosSvc_layoutAttachState) {
            // 이미 Attach 된 상태이거나
            // Attach 진행 중인 경우
            // 그냥 종료
            case LAYOUT_IS_ATTACHED:
            case LAYOUT_IS_ATTACHING:
                return;
            // Detach 진행 중인 경우
            // 완료 될때까지 대기
            case LAYOUT_IS_DETACHING:
                while(cosSvc_layoutAttachState == LAYOUT_IS_DETACHING);
                break;
        }

        // 만약 cosSvc_TV, cosSvc_OutBoundLayout 중 하나라도 null이 된 경우
        // 서비스 강제 재시작
        if(cosSvc_TV == null || cosSvc_OutBoundLayout == null)
            startSvc_Idle();

        // Attach 진행 중 상태로 변경
        cosSvc_layoutAttachState = LAYOUT_IS_ATTACHING;

        // 현재 ClockText 설정 값을 기준으로 가능한
        // 최대 길이의 텍스트를 얻어와서 TextView에 집어 넣는다.
        // 추후 텍스트뷰 크기를 계산하여 OutBoundLayout의 고정 크기로 사용
        if(cosSvc_FSState)
            cosSvc_TV.setText(cosSvc_ClockTextMax);
        else
            cosSvc_TV.setText(cosSvc_ClockTextMax_notfs);

        // 현재 화면의 가로 길이 측정
        // 최소 가로 길이는 240 pixels으로 가정
        Point size = new Point(240, 0);
        Display dis;
        dis = cosSvc_winManager.getDefaultDisplay();
        dis.getSize(size);

        cosSvc_TV.measure(View.MeasureSpec.makeMeasureSpec(size.x, View.MeasureSpec.AT_MOST), 0);

        // 안드로이드 버젼 및 터치로 숨기기 또는
        // 롱 터치로 임시로 숨기기 기능 여부에 따라 다른 값을 사용
        int __flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
        int __type;
        // 터치 기능 사용 시
        // 8th bit check (bTouchEvent)
        if((cosSvc_Status & 0b00_1000_0000) != 0) {
            __flags |= WindowManager.LayoutParams.FLAG_SPLIT_TOUCH;
            __type = WindowManager.LayoutParams.TYPE_PRIORITY_PHONE;
        }
        else {
            __flags |= WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE;
            __type = WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            __type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;

        // 레이아웃 옵션
        WindowManager.LayoutParams layout;

        // OutBoundLayout를 최상단에 넣기 위한 layout 설정 값
        // 여기서 텍스트뷰 크기를 계산하여 고정된 크기를 사용
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            layout = new WindowManager.LayoutParams(
                    cosSvc_TV.getMeasuredWidth(), cosSvc_TV.getMeasuredHeight(),
                    __type, __flags, PixelFormat.TRANSLUCENT);
        else
            layout = new WindowManager.LayoutParams(
                    cosSvc_TV.getMeasuredWidth(), cosSvc_TV.getMeasuredHeight(),
                    __type, __flags, PixelFormat.TRANSLUCENT);

        // 시계위치 저장
        byte iClockPosition;
        // 저장된 상태가 풀스크린이거나 "풀스크린 상태에 따라 시계를 다르게 표기" 옵션을
        // 사용 안하는 경우 기존 위치 값을 사용
        if(cosSvc_FSState) {
            iClockPosition = cosSvc_ClockPosition;
        }
        // 만약 "풀스크린 상태에 따라 시계를 다르게 표기" 옵션을 사용하고
        // 저장된 풀스크린 상태가 풀스크린이 아닐 때
        // NotFS에 대응하는 위치 값을 사용
        else {
            iClockPosition = cosSvc_ClockPosition_notfs;
        }

        // 시계 위치에 따라 택스트뷰 및 OutBoundLayout 정렬(gravitiy 설정)
        switch (iClockPosition) {
            case 0:
                layout.gravity = Gravity.TOP | Gravity.START;
                cosSvc_TV.setGravity(Gravity.TOP | Gravity.START);
                // Gradient + Shadow 조합인 경우
                // TextView 2개를 겹치게 넣기 위해 RelativeLayout 사용
                if(cosSvc_TVGradient != null) {
                    cosSvc_TVGradient.setGravity(Gravity.TOP | Gravity.START);
                    ((RelativeLayout)cosSvc_OutBoundLayout).setGravity(Gravity.TOP | Gravity.START);
                }
                // 나머지 경우 TextView 1개만 쓰므로 LinearLayout 사용
                else
                    ((LinearLayout)cosSvc_OutBoundLayout).setGravity(Gravity.TOP | Gravity.START);
                break;
            case 1:
                layout.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
                cosSvc_TV.setGravity(Gravity.TOP | Gravity.CENTER_HORIZONTAL);
                if(cosSvc_TVGradient != null) {
                    cosSvc_TVGradient.setGravity(Gravity.TOP | Gravity.CENTER_HORIZONTAL);
                    ((RelativeLayout)cosSvc_OutBoundLayout).setGravity(Gravity.TOP | Gravity.CENTER_HORIZONTAL);
                }
                else
                    ((LinearLayout)cosSvc_OutBoundLayout).setGravity(Gravity.TOP | Gravity.CENTER_HORIZONTAL);
                break;
            case 2: default:
                layout.gravity = Gravity.TOP | Gravity.END;
                cosSvc_TV.setGravity(Gravity.TOP | Gravity.END);
                if(cosSvc_TVGradient != null) {
                    cosSvc_TVGradient.setGravity(Gravity.TOP | Gravity.END);
                    ((RelativeLayout)cosSvc_OutBoundLayout).setGravity(Gravity.TOP | Gravity.END);
                }
                else
                    ((LinearLayout)cosSvc_OutBoundLayout).setGravity(Gravity.TOP | Gravity.END);
                break;
            case 3:
                layout.gravity = Gravity.BOTTOM | Gravity.START;
                cosSvc_TV.setGravity(Gravity.BOTTOM | Gravity.START);
                if(cosSvc_TVGradient != null) {
                    cosSvc_TVGradient.setGravity(Gravity.BOTTOM | Gravity.START);
                    ((RelativeLayout)cosSvc_OutBoundLayout).setGravity(Gravity.BOTTOM | Gravity.START);
                }
                else
                    ((LinearLayout)cosSvc_OutBoundLayout).setGravity(Gravity.BOTTOM | Gravity.START);
                break;
            case 4:
                layout.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
                cosSvc_TV.setGravity(Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
                if(cosSvc_TVGradient != null) {
                    cosSvc_TVGradient.setGravity(Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
                    ((RelativeLayout)cosSvc_OutBoundLayout).setGravity(Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
                }
                else
                    ((LinearLayout)cosSvc_OutBoundLayout).setGravity(Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
                break;
            case 5:
                layout.gravity = Gravity.BOTTOM | Gravity.END;
                cosSvc_TV.setGravity(Gravity.BOTTOM | Gravity.END);
                if(cosSvc_TVGradient != null) {
                    cosSvc_TVGradient.setGravity(Gravity.BOTTOM | Gravity.END);
                    ((RelativeLayout)cosSvc_OutBoundLayout).setGravity(Gravity.BOTTOM | Gravity.END);
                }
                else
                    ((LinearLayout)cosSvc_OutBoundLayout).setGravity(Gravity.BOTTOM | Gravity.END);
                break;
        }

        // 화면 최상단에 OutBoundLayout부터 삽입
        cosSvc_winManager.addView((ViewGroup)cosSvc_OutBoundLayout, layout);

        // TextView는 width, height를 WRAP_CONTENT로 해서 cosSvc_OutBoundLayout 안에 넣음
        layout.width = WindowManager.LayoutParams.WRAP_CONTENT;
        layout.height = WindowManager.LayoutParams.WRAP_CONTENT;

        // cosSvc_OutBoundLayout에 TextView 삽입
        ((ViewGroup)cosSvc_OutBoundLayout).addView(cosSvc_TV, layout);
        if(cosSvc_TVGradient != null)
            ((ViewGroup)cosSvc_OutBoundLayout).addView(cosSvc_TVGradient, layout);

        // 텍스트뷰를 화면 위에 추가 한 뒤 최대 크기 계산을 위한 넣어둔 쓰래기값을 지움
        cosSvc_TV.setText("");

        // Attach 완료 상태로 변경
        cosSvc_layoutAttachState = LAYOUT_IS_ATTACHED;
    }

    void detachLayout() {
        // Layout Attach 상태에 따른 분기
        switch(cosSvc_layoutAttachState) {
            // 이미 Detach 된 상태이거나
            // Detach 진행 중인 경우
            // 그냥 종료
            case LAYOUT_IS_DETACHED:
            case LAYOUT_IS_DETACHING:
                return;
            // Attach 진행 중인 경우
            // 완료 될때까지 대기
            case LAYOUT_IS_ATTACHING:
                while(cosSvc_layoutAttachState == LAYOUT_IS_ATTACHING);
                break;
        }

        // 만약 cosSvc_TV, cosSvc_OutBoundLayout 중 하나라도 null이 된 경우
        // 서비스 강제 재시작
        if(cosSvc_TV == null || cosSvc_OutBoundLayout == null)
            startSvc_Idle();

        // Detach 진행 중 상태로 변경
        cosSvc_layoutAttachState = LAYOUT_IS_DETACHING;

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

            cosSvc_winManager.removeView(((ViewGroup)cosSvc_OutBoundLayout));
        }

        // Detach 완료 상태로 변경
        cosSvc_layoutAttachState = LAYOUT_IS_DETACHED;
    }

    @Override
    public IBinder onBind(Intent arg0) { return null; }

    // Idle 서비스 시작. (Main 상태 탈출)
    private void startSvc_Idle() {
        mCon.startService(new Intent(mCon, COSSvc_Idle.class));
        stopSelf();
    }

    @Override
    public void onDestroy() {
        // 서비스 종료 시작 후에 기타 작업이
        // 진행되는 것을 방지
        // 1st bit on (InterruptHandler)
        // 10th bit on (onDestroy_called)
        cosSvc_Status |= 0b10_0000_0001;

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

        // FSDetector를 비 활성화
        if(cosSvc_FSDetector != null)
            cosSvc_FSDetector.detach();

        // 부착한 뷰 제거
        detachLayout();

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
                        // 10th bit check (onDestroy_called)
                        if((cosSvc_Status & 0b10_0000_0000) != 0) return;

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
                        // 10th bit check (onDestroy_called)
                        if((cosSvc_Status & 0b10_0000_0000) != 0) return;

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
        // TextView 설정
        _subClass.initSettings(cosSvc_TV, cosSvc_TVGradient);
        cosSvc_FSMode = _subClass.getFSMode();
        if(cosSvc_FSMode != 2) cosSvc_FSState = true;
        cosSvc_Status = _subClass.getConfigStatus(cosSvc_FSState);
        cosSvc_Locale = _subClass.getLocale();

        // 저장된 문자열 저장
        cosSvc_ClockTextFormatted = _subClass.getClockTextFormatted();
        cosSvc_ClockTextFormatted_notfs = _subClass.getClockTextFormatted_notfs();
        cosSvc_ClockTextMax = _subClass.getClockTextMax();
        cosSvc_ClockTextMax_notfs = _subClass.getClockTextMax_notfs();

        // 시계 위치 저장
        cosSvc_ClockPosition = _subClass.getClockPosition();
        cosSvc_ClockPosition_notfs = _subClass.getClockPosition_notfs();

        // 시계 구조에 따라 변하는 상태 값 미리 저장
        cosSvc_InitStatus = _subClass.getConfigStatus(true);
        cosSvc_InitStatus_notfs = _subClass.getConfigStatus(false);

        // Get Window Manager
        cosSvc_winManager = ((WindowManager)getSystemService(Context.WINDOW_SERVICE));
        if(cosSvc_winManager == null) startSvc_Idle();

        // 레이아웃 화면 최상단에 넣는 작업
        attachLayout();

        // 시계를 터치 시 일정시간 동안 숨기는 기능 사용 시 필요한 설정 처리
        if(_subClass.getHideByTouch()) {
            // 숨기는 시간 설정 값을 미리 저장
            // 로직 보정을 위해 1감소한 값을 사용
            cosSvc_HidingTimeLength = (short)(_subClass.getHidingTime() - 1);

            // 가장 뷰 크기가 큰 cosSvc_OutBoundLayout에서 처리
            ((ViewGroup)cosSvc_OutBoundLayout).setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    // 10th bit check (onDestroy_called)
                    if((cosSvc_Status & 0b10_0000_0000) != 0) return;

                    // 하위 뷰 TextView의 텍스트를 초기화
                    cosSvc_TV.setText(STR_EMPTY);
                    if (cosSvc_TVGradient != null) cosSvc_TVGradient.setText(STR_EMPTY);

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
                    // 10th bit check (onDestroy_called)
                    if((cosSvc_Status & 0b10_0000_0000) != 0) return false;

                    // 화면을 끄고 켜는 경우에만 서비스 재시작한다고 토스트 메시지를 띄운다
                    Toast.makeText(COSSvc.this, R.string.pref_toast_hidetheclocktemporary, Toast.LENGTH_LONG).show();

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
                    cosSvc_TV.setText(STR_EMPTY);
                    if(cosSvc_TVGradient != null) cosSvc_TVGradient.setText(STR_EMPTY);

                    // 시계 숨기기
                    ((ViewGroup)cosSvc_OutBoundLayout).setVisibility(View.GONE);

                    return false;
                }
            });
        }

        // Full Screen Mode가 쓰이는지 확인 및 적용
        if(cosSvc_FSMode != 0) {
            // 풀스크린 디텍터를 생성 및 software 레이어로 설정
            cosSvc_FSDetector = new FSDetector(this);
            // FSDetector를 활성화
            cosSvc_FSDetector.attach();

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
        // 풀스크린 상태에 따라 시계를 다르게 표시하는 경우 notfs 전용 Status 값도 확인
        // 2nd bit check (isUsing_Battery)
        if((cosSvc_InitStatus & 0b00_0000_0010) != 0 ||
                (cosSvc_FSMode == 2 && (cosSvc_InitStatus_notfs & 0b00_0000_0010) != 0)) {
            filter.addAction(Intent.ACTION_BATTERY_CHANGED);
            // 배터리에서 사용할 StringBuilder
            cosSvc_strBattLevelBuilder = new StringBuilder();
            cosSvc_strBattBuilder = new StringBuilder();
        }

        // 네트워크 상태 값을 사용하면 네트워크 상태 변경 액션을
        // 분별하는 필터를 추가하거나 NetworkCallback을 등록
        // 풀스크린 상태에 따라 시계를 다르게 표시하는 경우 notfs 전용 Status 값도 확인
        // 9th bit check (isUsing_Network_State)
        if((cosSvc_InitStatus & 0b01_0000_0000) != 0 ||
                (cosSvc_FSMode == 2 && (cosSvc_InitStatus_notfs & 0b01_0000_0000) != 0)) {
            // ConnectivityManager 생성
            cosSvc_connManager = (ConnectivityManager)mCon
                    .getSystemService(Context.CONNECTIVITY_SERVICE);

            // StringBuilder 생성
            cosSvc_strNetStateBuilder = new StringBuilder();

            // Api 21(LOLLIPOP) 부터 Callback을 지원하지만
            // Callback이 network가 확정되기 전에(특히 wifi 연결 시)
            // 먼저 호출되어 버려서 네트워크 상태를 재대로 갱신하지 못한다
            // 따라서 BroadcastReceiver만 사용
            filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        }

        // 리시버 등록
        registerReceiver(cosSvc_receiver, filter);

        // 분 단위 이상의 요소를 모두 처리한 문자열을 저장하는 StringBuilder
        // 초 단위 요소가 있을 경우 각 요소에 대응되는 특수 값
        // (\uF000 ~ \uF003, 유니코드 Private Use Area)이 저장 되는 곳
        cosSvc_FinalClockTextExceptSecond = new StringBuilder();
        // 최종 시계 문자열을 만들기 위한 StringBuilder
        cosSvc_FinalClockText = new StringBuilder();

        // 초 단위 요소가 안쓰인 경우 처음에 시계가 출력되지 않으므로
        // 초기 한번 업데이트를 위해 7비트를 켜준다.
        // 6th bit check (isUsing_SecElement)
        if((cosSvc_Status & 0b00_0010_0000) == 0)
            // 7th bit on (overMin_need_update)
            cosSvc_Status |= 0b00_0100_0000;

        // 시계 구조 formatter 정의
        if(cosSvc_FSState) {
            cosSvc_formatter =
                    DateTimeFormatter.ofPattern(cosSvc_ClockTextFormatted)
                            .withLocale(cosSvc_Locale)
                            .withZone(ZoneId.systemDefault());
        }
        else {
            cosSvc_formatter =
                    DateTimeFormatter.ofPattern(cosSvc_ClockTextFormatted_notfs)
                            .withLocale(cosSvc_Locale)
                            .withZone(ZoneId.systemDefault());
        }

        // 현재 시간
        cosSvc_current = Instant.now();

		// 1초 마다 반복 처리하는 스케쥴러 등록
        // 단 2000ms - 현재 시간의 ms 한 만큼 딜레이를 가지고 시작한다
        // 즉, 1초 초과 ~ 2초 이하 만큼 기다린 뒤에 반복 작업을 시작하며
        // 최소 1초는 스케쥴러 등록 후 남은 작업을 위한 여유 시간으로 사용한다
        //
        // ex) 3.325초 에서 실행한 경우 5.000초 가 되는 순간부터
        //     1초마다 sendEmptyMessage를 호출한다
        //
        // Runnable을 서비스 자신으로 등록한다.
		if(cosSvc_repeater == null) {
            cosSvc_repeater = Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate
                    (this, 2000 - (cosSvc_current.getNano() / 1000000), 1000, TimeUnit.MILLISECONDS);
        }

        // 초 정보 저장
        cosSvc_second = (byte)(cosSvc_current.getEpochSecond() % 60);

        // 초, 배터리를 제외한 모든 요소를 처리한 문자열을 저장
        cosSvc_FinalClockTextExceptSecond.append(cosSvc_formatter.format(cosSvc_current));

        // cosSvc_FinalClockTextExceptSecond의 내용을
        // cosSvc_FinalClockText(최종 문자열 빌더)에 복사
        cosSvc_FinalClockText.append(cosSvc_FinalClockTextExceptSecond);

        // 처음 호출에 TextView에는 저장된 시간이 나타나며
        // 저장된 시간이 1초 증가한다(저장된 시간 + 1초)
        // 두번째 호출에 TextView에는 1초 증가한 시간이 나타나며
        // 또 저장된 시간이 1초 증가한다(저장된 시간 + 2초)
        //
        // ex) 11.572초에 서비스가 실행된 경우
        //     cosSvc_second에 11초로 저장한다
        //
        // 이 때 cosSvc_repeater 등록 시 1 ~ 2초 지연을 넣었으므로
        // 최초 cosSvc_repeater 작동 시기는 등록 시간 + 2초 이다
        //
        // ex) 3.325초에 서비스가 실행된 경우 5.000초가 되는 순간부터
        //     1초마다 sendEmptyMessage를 호출한다
        //
        // 따라서 미리 2번 호출하면
        // 다음에 cosSvc_repeater에 의해 호출되는 순간
        // TextView에는 저장된 시간에서 2초 증가한 시간이 나타나며
        // 시간이 알맞게 조절된다
        //
        // 또한 cosSvc_repeater가 아래 호출 보다 먼저 sendEmptyMessage를 하더라도
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
                // FSDetector를 비 활성화
                cosSvc_FSDetector.detach();

                // 서비스 실행 도중 서비스가 호출 되는 경우 onStartCommand가 실행될 때
                // cosSvc_FSDetector가 null이 아니면 이곳으로 다시 오는 것도 있고,
                // 추후 onDestroy에서 removeView를 할지 안할지를 null 값으로 판단하므로
                cosSvc_FSDetector = null;
            }
            // 설정 창이 아닌 경우 hasOnFullScreenListener 확인하여
            // OnFullScreenListener가 부착되지 않은 경우에만 처리
            else if(!cosSvc_FSDetector.hasOnFullScreenListener()) {
                // FSMode에 따라 다른 onFullScreenListener를 사용
                switch(cosSvc_FSMode) {
                    case 1:
                        // 풀스크린 상태에서만 시계 표시
                        cosSvc_FSDetector.setOnFullScreenListener(new OnFullScreenListener() {
                            @Override
                            public void fsChanged(Context context, boolean bIsFS) {
                                // 10th bit check (onDestroy_called)
                                if((cosSvc_Status & 0b10_0000_0000) != 0) return;

                                // 풀스크린 상태가 아닐 경우 Idle 서비스로 진입
                                if (!bIsFS) {
                                    startSvc_Idle();
                                }
                            }
                        });
                        break;
                    case 2:
                        // 풀스크린 상태에 따라 다른 시계 표시
                        cosSvc_FSDetector.setOnFullScreenListener(new OnFullScreenListener() {
                            @Override
                            public void fsChanged(Context context, boolean bFSState) {
                                // 10th bit check (onDestroy_called)
                                if((cosSvc_Status & 0b10_0000_0000) != 0) return;

                                // 풀스크린 상태가 설정에 저장된 값과 다를 경우
                                // 바뀐 풀스크린 상태를 저장하고
                                // 그에 맞는 시계 구조 및 위치를 불러온 뒤
                                // 시간이 지연되었을 가능성이 있으므로 시간을 재설정
                                if (cosSvc_FSState != bFSState) {
                                    cosSvc_FSState = bFSState;
                                    // 남아있는 Handler 예약 작업 지우기
                                    mHandler.removeMessages(0);
                                    // 1st bit on (InterruptHandler)
                                    cosSvc_Status |= 0b00_0000_0001;
                                    detachLayout();
                                    // 2nd bit (isUsing_Battery)
                                    // 5th bit (thereAreOnlyString)
                                    // 6th bit (isUsing_SecElement)
                                    // 9th bit (isUsing_Network_State)
                                    // 4개의 비트는 시계 구조에 따라 변화하므로 재설정
                                    cosSvc_Status &= 0b10_1100_1101;
                                    if(cosSvc_FSState) {
                                        cosSvc_Status |= cosSvc_InitStatus;
                                    }
                                    else {
                                        cosSvc_Status |= cosSvc_InitStatus_notfs;
                                    }
                                    attachLayout();
                                    // 1st bit off (InterruptHandler)
                                    cosSvc_Status &= 0b11_1111_1110;
                                    reloadCurrentTime(true);
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
        // 10th bit check (onDestroy_called)
        if((cosSvc_Status & 0b10_0000_0000) != 0) return;

        // 롱터치로 시계를 끈 경우 cosSvc_repeater가 null이 된다
        // 따라서 cosSvc_repeater 값이 null이 아닌 경우에만
        // 레이아웃 재설정 및 작업 시간이
        // 길어진 경우를 고려하여 시간도 재설정
        if(cosSvc_repeater != null) {
            // 남아있는 Handler 예약 작업 지우기
            mHandler.removeMessages(0);
            // 1st bit on (InterruptHandler)
            cosSvc_Status |= 0b00_0000_0001;
            detachLayout();
            attachLayout();
            // 1st bit off (InterruptHandler)
            cosSvc_Status &= 0b11_1111_1110;
            reloadCurrentTime(false);
        }
        super.onConfigurationChanged(newConfig);
    }

    // cosSvc_repeater로 인해 매초 또는 0.5초 마다 실행되는 부분
    public void run() {
        // 주기마다 mHandler에 Message 전송
        mHandler.sendEmptyMessage(0);
    }

    // 매 초마다 실행.
    @SuppressLint("HandlerLeak")
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            // 1st bit check (InterruptHandler)
            // 10th bit check (onDestroy_called)
            // msg null 검사
            if((cosSvc_Status & 0b10_0000_0001) != 0 || msg == null) return;

            // cosSvc_HidingTime 값이 0이면 시계 내용 업데이트
            if (cosSvc_HidingTime == 0) {
                // 실제로 문자열 업데이트가 이루어지는 곳
                // 초 단위가 쓰인경우 초, 배터리, 네트워크 상태
                // 문자열을 처리하고 시계를 갱신
                // 6th bit check (isUsing_SecElement)
                if((cosSvc_Status & 0b00_0010_0000) != 0) {
                    // 각 초 단위 요소의 심볼 위치를 저장할 변수
                    int index;

                    // 초(0~59) 처리
                    index = cosSvc_FinalClockText.indexOf(STR_SYMBOL_SECOND);
                    while(index != -1) {
                        cosSvc_FinalClockText.deleteCharAt(index);
                        cosSvc_FinalClockText.insert(index, cosSvc_second);
                        index = cosSvc_FinalClockText.indexOf(STR_SYMBOL_SECOND);
                    }
                    // 초(00~59) 처리
                    index = cosSvc_FinalClockText.indexOf(STR_SYMBOL_SECOND_FILLZERO);
                    while(index != -1) {
                        cosSvc_FinalClockText.deleteCharAt(index);
                        if(cosSvc_second < 10) cosSvc_FinalClockText.insert(index++, 0);
                        cosSvc_FinalClockText.insert(index, cosSvc_second);
                        index = cosSvc_FinalClockText.indexOf(STR_SYMBOL_SECOND_FILLZERO);
                    }

                    // 배터리 요소를 사용하는지 확인
                    // 2nd bit check (isUsing_Battery)
                    if((cosSvc_Status & 0b00_0000_0010) != 0) {
                        // 배터리가 완충/충전/방전 상태 중 하나라도 해당하면 문자열 미리 생성
                        // 3rd, 4th bit check (isBattery_Charging)(isBattery_Discharging)
                        if((cosSvc_Status & 0b00_0000_1100) != 0) {
                            // 처음에 배터리 잔량(XX%) 부터 복사
                            cosSvc_strBattBuilder.setLength(0);
                            cosSvc_strBattBuilder.append(cosSvc_strBattLevelBuilder);

                            // 완충 상태 처리
                            // 3rd, 4th bit check (isBattery_Charging)(isBattery_Discharging)
                            if((cosSvc_Status & 0b00_0000_1100) == 0b00_0000_1100) {
                                cosSvc_strBattBuilder.append(CHAR_BATTSTATE_FULL);
                            }
                            // 충전 중 상태 처리
                            // 3rd bit check (isBattery_Charging)
                            else if((cosSvc_Status & 0b00_0000_0100) != 0) {
                                if((cosSvc_second & 0x1) == 1) cosSvc_strBattBuilder.append(CHAR_BATTSTATE_CHARGING1);
                                else cosSvc_strBattBuilder.append(CHAR_BATTSTATE_CHARGING2);
                            }
                            // 배터리가 15퍼 미만인 경우 방전으로 처리
                            // 4th bit check (isBattery_Discharging)
                            else {
                                if((cosSvc_second & 0x1) == 1) cosSvc_strBattBuilder.append(CHAR_BATTSTATE_DISCHARGING1);
                                else cosSvc_strBattBuilder.append(CHAR_BATTSTATE_DISCHARGING2);
                            }
                        }

                        // 배터리 처리
                        // 최종 문자열을 알맞은 위치에 삽입
                        index = cosSvc_FinalClockText.indexOf(STR_SYMBOL_BATT);
                        while (index != -1) {
                            cosSvc_FinalClockText.deleteCharAt(index);
                            // 배터리가 완충, 충전, 방전 상태가 아니면 현재 배터리 잔량을 바로 표기
                            if ((cosSvc_Status & 0b00_0000_1100) == 0)
                                cosSvc_FinalClockText.insert(index, cosSvc_strBattLevelBuilder);
                            // 완충, 충전, 방전 상태에 따른 처리
                            else
                                cosSvc_FinalClockText.insert(index, cosSvc_strBattBuilder);
                            index = cosSvc_FinalClockText.indexOf(STR_SYMBOL_BATT);
                        }
                    }

                    // Network State를 사용하는지 확인
                    // 9th bit check (isUsing_Network_State)
                    if((cosSvc_Status & 0b01_0000_0000) != 0) {
                        // Network State 문자열을 알맞은 위치에 삽입
                        index = cosSvc_FinalClockText.indexOf(STR_SYMBOL_NETWORKSTATE);
                        while (index != -1) {
                            cosSvc_FinalClockText.deleteCharAt(index);
                            cosSvc_FinalClockText.insert(index, cosSvc_strNetStateBuilder);
                            index = cosSvc_FinalClockText.indexOf(STR_SYMBOL_NETWORKSTATE);
                        }
                    }

                    // 시스템 렉 발생으로 Message를 처리 못한 경우 대비
                    // mHandler 내 Message가 없는 경우에만 setText
                    if(!mHandler.hasMessages(0)) {
                        cosSvc_TV.setText(cosSvc_FinalClockText);
                        if(cosSvc_TVGradient != null) cosSvc_TVGradient.setText(cosSvc_FinalClockText);
                    }

                    // 다음 업데이트에 필요한 초 단위 요소를 제외한 문자열을
                    // 최종 문자열 빌더에 저장
                    // cosSvc_FinalClockText 초기화
                    cosSvc_FinalClockText.setLength(0);
                    // cosSvc_FinalClockTextExceptSecond의 내용을
                    // cosSvc_FinalClockText(최종 문자열 빌더)에 복사
                    cosSvc_FinalClockText.append(cosSvc_FinalClockTextExceptSecond);
                }
                // 시스템 렉 발생으로 Message를 처리 못한 경우 대비
                // mHandler 내 Message가 없는 경우에만 setText
                // + 초 단위가 안 쓰인경우 분 단위 이상의 요소는
                // 이미 cosSvc_FinalClockTextExceptSecond안에
                // 저장되어 있으므로 바로 시계를 갱신한다
                // 7th bit check (overMin_need_update)
                else if((cosSvc_Status & 0b00_0100_0000) != 0 && !mHandler.hasMessages(0)) {
                    cosSvc_TV.setText(cosSvc_FinalClockTextExceptSecond);
                    if(cosSvc_TVGradient != null) cosSvc_TVGradient.setText(cosSvc_FinalClockTextExceptSecond);
                    // 7th bit off (overMin_need_update)
                    cosSvc_Status &= 0b11_1011_1111;
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

                    // 다음 업데이트에 필요한 초 단위 요소를 제외한 문자열을
                    // 최종 문자열 빌더에 저장
                    // cosSvc_FinalClockText 초기화
                    cosSvc_FinalClockText.setLength(0);
                    // cosSvc_FinalClockTextExceptSecond의 내용을
                    // cosSvc_FinalClockText(최종 문자열 빌더)에 복사
                    cosSvc_FinalClockText.append(cosSvc_FinalClockTextExceptSecond);

                    // 초 단위 요소가 안쓰인 경우 분 단위는 바로 업데이트가 안되므로
                    // overMin_need_update를 켜서 숨기는 시간이 끝나자 마자 업데이트 하도록 한다
                    // 따라서 isUsing_SecElement가 false 이면 갱신하도록 한다
                    // 6th bit check (isUsing_SecElement)
                    if((cosSvc_Status & 0b00_0010_0000) == 0)
                        // 7th bit on (overMin_need_update)
                        cosSvc_Status |= 0b00_0100_0000;
                }
            }

            // 시계 요소가 하나도 안쓰였다면 1초를 더할 필요가 없다
            // 5th bit check (thereAreOnlyString)
            if((cosSvc_Status & 0b00_0001_0000) != 0) {
                return;
            }

            // 시간을 더하는 부분
            // 1초를 더한다
            cosSvc_second++;

            // 만약 1분 단위가 증가 하는 경우 현재 시간을 새로 얻어와서
            // formatter를 통해 1분 이상 시간 단위 영역의 문자열을 갱신한다
            if (cosSvc_second == 60) {
                // 저장해둔 Instant에 60초를 더함
                cosSvc_current = Instant.ofEpochSecond(cosSvc_current.getEpochSecond() + 60);
                // 혹시 cosSvc_formatter가 GC 된 경우 재생성
                if (cosSvc_formatter == null) {
                    if (cosSvc_FSState) {
                        cosSvc_formatter =
                                DateTimeFormatter.ofPattern(cosSvc_ClockTextFormatted)
                                        .withLocale(cosSvc_Locale)
                                        .withZone(ZoneId.systemDefault());
                    } else {
                        cosSvc_formatter =
                                DateTimeFormatter.ofPattern(cosSvc_ClockTextFormatted_notfs)
                                        .withLocale(cosSvc_Locale)
                                        .withZone(ZoneId.systemDefault());
                    }
                }
                // 초, 배터리를 제외한 모든 요소를 처리한 문자열을 갱신
                cosSvc_FinalClockTextExceptSecond.setLength(0);
                cosSvc_FinalClockTextExceptSecond.append(cosSvc_formatter.format(cosSvc_current));
                cosSvc_second = 0;

                // cosSvc_FinalClockText 초기화
                cosSvc_FinalClockText.setLength(0);
                // cosSvc_FinalClockTextExceptSecond의 내용을
                // cosSvc_FinalClockText(최종 문자열 빌더)에 복사
                cosSvc_FinalClockText.append(cosSvc_FinalClockTextExceptSecond);

                // 초 단위 요소가 안쓰인 경우에만 분 단위 이상 업데이트
                // 6th bit check (isUsing_SecElement)
                if ((cosSvc_Status & 0b00_0010_0000) == 0)
                    // 7th bit on (overMin_need_update)
                    cosSvc_Status |= 0b00_0100_0000;
            }
        }
    };
}
