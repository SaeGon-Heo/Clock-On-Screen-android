/*
 * Copyright 2020 SaeGon Heo
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

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.ImageView;
import android.widget.TextView;
import android.content.res.Configuration;
import android.graphics.Color;

import java.util.Locale;

public final class COSMain extends Activity {
    Context mCon;
    AlertDialog PermissionDialog;
    AlertDialog AppInfoDialog;

    // 서비스 실행
    void runService(int way) {
        Intent mSvc_Idle = new Intent(this, COSSvc_Idle.class);
        Intent mSvc = new Intent(this, COSSvc.class);

        // 오버레이 권한이 사용자에 의해 풀린 경우 확인하여 서비스를 종료하고 다시 켜도록 유도
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M
                && !Settings.canDrawOverlays(mCon)) {
            // service_running 값을 false 으로 저장 및 서비스 중지 상태로 버튼을 전환
            stopService(mSvc); stopService(mSvc_Idle);

            // 서비스 실행 상태 꺼짐으로 변경 및 서비스 켜기/끄기 버튼 상태를 끄기로 변경
            PreferenceManager.getDefaultSharedPreferences(mCon).edit().putBoolean("service_running", false).apply();

            ((ImageView)findViewById(R.id.btn_svc_toggle)).setImageResource(R.mipmap.ic_svc_off);
            TextView tv = findViewById(R.id.tv_svc_state);
            tv.setText(R.string.text_svc_off);
            tv.setTextColor(Color.RED);

            // 다시 오버레이 권한을 켜도록 유도
            induceSetOverlayPermission();

            return;
        }

        // 0 = stop / 1 = start / 2 = restart
        if(way != 1) { stopService(mSvc); stopService(mSvc_Idle); }
        if(way != 0) startService(mSvc_Idle);
    }

    // API 23(Marshmallow)이후 오버레이 권한을 켜도록 하는 Dialog 출력
    @TargetApi(Build.VERSION_CODES.M)
    public void induceSetOverlayPermission() {
        if(PermissionDialog == null) {
            PermissionDialog = new AlertDialog.Builder(this)
                    .setPositiveButton(android.R.string.ok, null)
                    .setIcon(R.mipmap.ic_launcher)
                    .setMessage(R.string.app_request_overlay_permission_content)
                    .setTitle(R.string.app_request_overlay_permission_title)
                    .setCancelable(false)
                    .create();

            PermissionDialog.show();
            PermissionDialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + mCon.getPackageName())));
                    PermissionDialog.dismiss();
                }
            });
        }

        // show dialog
        if(!PermissionDialog.isShowing())
            PermissionDialog.show();
    }

    @Override
    public void onDestroy() {
        if (PermissionDialog != null && PermissionDialog.isShowing()) {
            PermissionDialog.dismiss();
        }
        if (AppInfoDialog != null && AppInfoDialog.isShowing()) {
            AppInfoDialog.dismiss();
        }
        super.onDestroy();
    }

    // 초기 설정 실행
    private void initSettings() {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(mCon);

        // 사용중인 언어 확인
        String currentLocale = Locale.getDefault().toString();

        // 사용중인 언어 값 및 사용중인 언어에 따라
        // 기본 시계 구조로 가능한 최대 크기의 문자열 저장
        boolean isEnglish = true;
        if(currentLocale.equals("ko_KR") || currentLocale.equals("ko")) {
            isEnglish = false;
        }

        // 기본 시계 구조에 따라 포맷 문자열 저장
        pref.edit()
                .putBoolean(mCon.getString(R.string.pref_english_key_string), isEnglish)
                .putString(mCon.getString(R.string.pref_clockText_key_string), mCon.getString(R.string.pref_clockText_default_string))
                .putString(mCon.getString(R.string.pref_clockText_notfs_key_string), mCon.getString(R.string.pref_clockText_default_string))
                .putString(mCon.getString(R.string.pref_clockTextMax_key_string),
                        COSSettings.getClockTextMax(mCon.getString(R.string.pref_clockText_default_string), isEnglish))
                .putString(mCon.getString(R.string.pref_clockTextMax_notfs_key_string),
                        COSSettings.getClockTextMax(mCon.getString(R.string.pref_clockText_default_string), isEnglish))
                .putString(mCon.getString(R.string.pref_clockTextFormatted_key_string),
                        COSSettings.getClockTextFormatted(mCon.getString(R.string.pref_clockText_default_string)))
                .putString(mCon.getString(R.string.pref_clockTextFormatted_notfs_key_string),
                        COSSettings.getClockTextFormatted(mCon.getString(R.string.pref_clockText_default_string))).apply();

        if(AppInfoDialog == null) {
            StringBuilder MsgBuilder = new StringBuilder();

            MsgBuilder.append(mCon.getString(R.string.app_info_content));
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                MsgBuilder.append(mCon.getString(R.string.app_info_content_oreo_suffix));
            }

            // Dialog 생성
            AppInfoDialog = new AlertDialog.Builder(this)
                    .setPositiveButton(android.R.string.ok, null)
                    .setIcon(R.mipmap.ic_launcher)
                    .setTitle(R.string.action_info)
                    .setMessage(MsgBuilder.toString())
                    .setCancelable(false)
                    .create();
            MsgBuilder.setLength(0);

            AppInfoDialog.show();
            AppInfoDialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    PreferenceManager.getDefaultSharedPreferences(mCon).edit().putBoolean("init_settings", false).apply();
                    AppInfoDialog.dismiss();
                }
            });
        }

        if(!AppInfoDialog.isShowing())
            AppInfoDialog.show();

    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mCon = getApplicationContext();

        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(mCon);

        // 현재 Context의 언어 설정
        Locale locale;
        String currentLocale = Locale.getDefault().toString();
        if (pref.getBoolean(mCon.getString(R.string.pref_english_key_string), !(currentLocale.equals("ko_KR") || currentLocale.equals("ko"))))
            locale = Locale.US;
        else
            locale = Locale.KOREA;

        Configuration config = new Configuration();
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN_MR1) {
            config.setLocale(locale);
        } else {
            config.locale = locale;
        }
        getResources().updateConfiguration(config, getResources().getDisplayMetrics());

        // 메인화면 출력
        setContentView(R.layout.activity_main);

        // Add up button
        ActionBar ab = getActionBar();
        if (ab != null) ab.setDisplayHomeAsUpEnabled(true);

        // 오버레이 권한 확인 및 오버레이 권한이 풀린 경우 다시 켜도록 유도
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M
                && !Settings.canDrawOverlays(mCon))
            induceSetOverlayPermission();

        // 첫실행 또는 설정 초기화 실행 후 초기 값을 설정
        if (pref.getBoolean("init_settings", true))
            initSettings();

        // 서비스 상태에 따라 글자색 및 이미지를 바꾸기 위해 선언
        TextView tv = findViewById(R.id.tv_svc_state);
        ImageView iv = findViewById(R.id.btn_svc_toggle);

        // 서비스 실행상태시 안내문 및 아이콘 변경
        if (pref.getBoolean("service_running", false)) {
            iv.setImageResource(R.mipmap.ic_svc_on);
            tv.setText(R.string.text_svc_on);
            tv.setTextColor(Color.GREEN);
        }

        // 시작 / 중지버튼
        iv.setOnClickListener(new View.OnClickListener() {
            // 설정값에 따라 서비스를 시작 / 중지
            @Override
            public void onClick(View v) {
                SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                TextView tv = findViewById(R.id.tv_svc_state);
                ImageView iv = findViewById(R.id.btn_svc_toggle);

                if (pref.getBoolean("service_running", false)) {
                    // 서비스 상태에 따라 글자색 및 이미지를 바꾸기 위해 선언
                    iv.setImageResource(R.mipmap.ic_svc_off);
                    tv.setText(R.string.text_svc_off);
                    tv.setTextColor(Color.RED);
                    pref.edit().putBoolean("service_running", false).apply();
                    runService(0);
                } else {
                    iv.setImageResource(R.mipmap.ic_svc_on);
                    tv.setText(R.string.text_svc_on);
                    tv.setTextColor(Color.GREEN);
                    pref.edit().putBoolean("service_running", true).apply();
                    runService(1);
                }
            }
        });

        // 버튼 터치 시 효과
        iv.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        view.setBackgroundColor(0xfffafafa);
                        break;
                    case MotionEvent.ACTION_DOWN:
                        view.setBackgroundResource(R.mipmap.ic_svc_background);
                        break;
                }
                return false;
            }
        });
    } // onCreate

    @Override
    protected void onResume() {
        super.onResume();
        // 서비스가 켜진상태면 재실행
        if(PreferenceManager.getDefaultSharedPreferences(mCon).getBoolean("service_running", false)) runService(2);
    } // onResume

    // 메인 화면에만 메뉴를 로드
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    // 메뉴 버튼 별 행동
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_settings:
                startActivity(new Intent(this, COSSettings.class));
                return true;
            case R.id.action_info:
                startActivity(new Intent(this, COSInfo.class));
                return true;
            // Home button (As Up button) action
            case android.R.id.home:
                if(!isFinishing()) finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}