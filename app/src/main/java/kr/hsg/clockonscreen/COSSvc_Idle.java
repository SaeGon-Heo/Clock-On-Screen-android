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

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.IBinder;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;

public final class COSSvc_Idle extends Service {
    private Context mCon;
    private FSDetector cosSvc_FSDetector_Idle;
    private BroadcastReceiver cosSvc_BReceiver;
    private byte cosSvc_FSMode;

    @Override
    public IBinder onBind(Intent arg0) { return null; }

    // Main 서비스 시작. (Idle 상태 탈출)
    private void startSvc() {
        mCon.startService(new Intent(mCon, COSSvc.class));
        stopSelf();
    }

    @Override
    public void onDestroy() {
        // 서비스 종료 과정
        cosSvc_BReceiver.clearAbortBroadcast();
        unregisterReceiver(cosSvc_BReceiver);

        if(cosSvc_FSDetector_Idle != null) {
            cosSvc_FSDetector_Idle.clearAnimation();
            ((WindowManager)getSystemService(Context.WINDOW_SERVICE)).removeView(cosSvc_FSDetector_Idle);
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
        startForeground(220, _subClass.getNotification(true));

        // 풀스크린 모드 값 저장
        cosSvc_FSMode = COSSvcSubFunc.getFSMode(this);
        // 풀스크린에서만 시계 표시 모드를 사용하는 경우 풀스크린 디텍터 추가
        if(cosSvc_FSMode == 1) {
            cosSvc_FSDetector_Idle = new FSDetector(this);
            cosSvc_FSDetector_Idle.setLayerType(View.LAYER_TYPE_SOFTWARE, null);

            int __type = WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY;
            if(android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                __type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
            }

            // Fix for the cannot click install on sideloaded apps bug
            // Dont fill width, set to left side with a few pixels wide
            WindowManager.LayoutParams layout = new WindowManager.LayoutParams(
                    1, WindowManager.LayoutParams.MATCH_PARENT,
                    __type,
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE |
                            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                    PixelFormat.TRANSPARENT);
            layout.gravity = Gravity.LEFT;
            ((WindowManager)getSystemService(Context.WINDOW_SERVICE)).addView(cosSvc_FSDetector_Idle, layout);

            cosSvc_FSDetector_Idle.setOnFullScreenListener(new OnFullScreenListener() {
                @Override
                public void fsChanged(Context context, boolean bIsFS) {
                    // 풀스크린이고 절전모드(DOZE)도 아니면서 화면이 켜진 상태면 서비스 실행
                    if (bIsFS && new COSSvcSubFunc(COSSvc_Idle.this).getIsScreenOnAndNotDOZEState())
                        startSvc();
                }
            });
        } else {
            if(_subClass.getIsScreenOnAndNotDOZEState()) startSvc();
        }

        cosSvc_BReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                // 화면 켜짐 action이 들어오고, 풀스크린에서만 시계 표시 모드가 아니면서
                // 절전모드(DOZE)도 아닌 경우 서비스 실행
                if(action != null && action.equals("android.intent.action.SCREEN_ON") &&
                        cosSvc_FSMode != 1 &&
                        new COSSvcSubFunc(COSSvc_Idle.this).getIsScreenOnAndNotDOZEState()) {
                    startSvc();
                }
            }
        };
        // 화면 켜짐 액션만 분별하는 필터 생성 및 리시버 등록
        registerReceiver(cosSvc_BReceiver, new IntentFilter("android.intent.action.SCREEN_ON"));
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        // 서비스 유지
        return START_STICKY;
    }
}