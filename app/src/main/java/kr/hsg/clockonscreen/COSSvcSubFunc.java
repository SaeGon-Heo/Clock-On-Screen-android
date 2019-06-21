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

import java.util.Locale;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.hardware.display.DisplayManager;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.util.Log;
import android.util.TypedValue;
import android.view.Display;
import android.widget.TextView;

final class COSSvcSubFunc {
    private Context mCon;
    private SharedPreferences mPref;

    COSSvcSubFunc(Context context) {
        mCon = context.getApplicationContext();
        mPref = PreferenceManager.getDefaultSharedPreferences(mCon);
    }

    void initSettings(TextView cosSvc_TV, TextView cosSvc_TVGradient) {
        // 폰트 사이즈 설정
        cosSvc_TV.setTextSize(TypedValue.COMPLEX_UNIT_DIP, mPref.getInt(mCon.getString(R.string.pref_fontSize_key_string), 18));
        // Gradient + Shadow 둘다 사용 시에만 cosSvc_TVGradient가 null이 아니도록 코딩.
        // Gradient + Shadow 둘다 사용 시 두개의 TextView를 이용하여 하나는 Shadow만 그리고,
        // 다른 하나는 Gradient를 그림자 없이 겹쳐서(FrameLayout) 그리도록 코딩.
        // 앞으로 (cosSvc_TVGradient != null) 조건을 사용하는 영역은 위와 같이 처리하기 위한 부분.
        if(cosSvc_TVGradient != null) {
            cosSvc_TVGradient.setTextSize(TypedValue.COMPLEX_UNIT_DIP, mPref.getInt(mCon.getString(R.string.pref_fontSize_key_string), 18));
        }
        // TextView에서 setTextColor으로 색상 설정 시 해당 색상의 Alpha 값이
        // TextView 내부 paint의 Alpha(투명도)를 덮어 씌운다.
        // 따라서 투명도 설정 값을 텍스트 컬러의 Alpha 영역에 적용 후 setTextColor 호출.
        int color = mPref.getInt(mCon.getString(R.string.pref_fontColor_key_string), 0xffffffff);
        int alpha = mPref.getInt(mCon.getString(R.string.pref_clockTransparency_key_string), 255);
        color &= ((alpha << 24) + 0xffffff);
        // 만약 Gradient + Shadow 조합인 경우 기본 TextView는 투명하게 하고
        // Gradient만 적용할 TextView에 색상 값을 넣어서 paint의 Alpha 값을 변경
        if(cosSvc_TVGradient != null) {
            cosSvc_TV.setTextColor(0x00ffffff);
            cosSvc_TVGradient.setTextColor(color);
        } else {
            cosSvc_TV.setTextColor(color);
        }

        // TextView의 배경 색 설정(단색)
        if(mPref.getBoolean(mCon.getString(R.string.pref_background_key_string),false))
            cosSvc_TV.setBackgroundColor(mPref.getInt(mCon.getString(R.string.pref_backgroundColor_key_string), 0x1fafafaf));

        // Shadow 설정 + dx, dy 값에 따른 TextView의 추가 공간 계산을 위한 변수 생성
        int leftPadding = 0, topPadding = 0, rightPadding = 0, bottomPadding = 0;
        if(getShadowEnabled()) {
            // 그림자 색상을 저장
            int shadowColor = mPref.getInt(mCon.getString(R.string.pref_fontShadowColor_key_string), 0xff000000);
            // 만약 Gradient + Shadow 조합인데
            // 시계 투명도 설정 값이 255(불투명)이면
            // 그림자 색상의 alpha 값을 254(투명도를 가짐)로 바꾼다.
            // https://developer.android.com/reference/android/graphics/Paint
            // 위 링크에서 setShadowLayer 함수 설명을 보면
            // The alpha of the shadow will be the paint's alpha
            // if the shadow color is opaque, or the alpha from the shadow color if not.
            // 와 같은 내용이 있다. 이를 활용한 것.
            if(cosSvc_TVGradient != null) {
                if(alpha == 255)
                    shadowColor &= 0xfeffffff;
                // 시계 투명도가 255가 아니면 해당 값을 그림자 색상의 alpha 값에 적용
                else
                    shadowColor &= ((alpha << 24) + 0xffffff);
            }

            float fRadius = mPref.getFloat(mCon.getString(R.string.pref_fontShadowShape_radius_key_string), 3.0f);
            float fDx = mPref.getFloat(mCon.getString(R.string.pref_fontShadowShape_dx_key_string), 2.0f);
            float fDy = mPref.getFloat(mCon.getString(R.string.pref_fontShadowShape_dy_key_string), 2.0f);
            cosSvc_TV.setShadowLayer(fRadius, fDx, fDy, shadowColor);

            // 그림자로 인한 추가 공간 계산
            if(fDx < 0) leftPadding -= fDx;
            else rightPadding += fDx;
            if(fDy < 0) topPadding -= fDy;
            else bottomPadding += fDy;
        }

        // 페인트 로드
        Paint tvPaint = cosSvc_TV.getPaint();
        Paint tvGradientPaint = null;
        if(cosSvc_TVGradient != null)
            tvGradientPaint = cosSvc_TVGradient.getPaint();

        // 폰트 외관 옵션
        switch(Integer.parseInt(mPref.getString(mCon.getString(R.string.pref_fontAppearance_key_string), "0"))) {
            case 0:
                // 기본 여분 공간을 좌우로 5 pixels 추가
                // 추가로 위에서 그림자를 사용한 경우 그림자로 인한 추가 여분 공간을 추가
                cosSvc_TV.setPadding(5 + leftPadding, topPadding, 5 + rightPadding, bottomPadding);
                // Gradient + Shadow 조합인 경우 Gradient 전용 TextView도 같은 여분공간 추가
                if(cosSvc_TVGradient != null)
                    cosSvc_TVGradient.setPadding(5 + leftPadding, topPadding, 5 + rightPadding, bottomPadding);
                break;
            case 1:
                cosSvc_TV.setPadding(5 + leftPadding, topPadding, 5 + rightPadding, bottomPadding);
                // 굵은 글씨
                tvPaint.setFakeBoldText(true);
                if(cosSvc_TVGradient != null) {
                    cosSvc_TVGradient.setPadding(5 + leftPadding, topPadding, 5 + rightPadding, bottomPadding);
                    tvGradientPaint.setFakeBoldText(true);
                }
                break;
            case 2:
                // 기울어진 글씨의 경우 우측으로 기울어지면서 가장 우측 부분이 잘리는 현상이 있으므로
                // 현재 폰트 사이즈를 픽셀 단위로 받아와서 해당 값의 20% 정도 우측에 추가 공간을 생성
                cosSvc_TV.setPadding(5 + leftPadding, topPadding, 5 + (int)cosSvc_TV.getTextSize() / 5 + rightPadding, bottomPadding);
                // 기울어진 글씨
                tvPaint.setTextSkewX(-0.25f);
                if(cosSvc_TVGradient != null) {
                    cosSvc_TVGradient.setPadding(5 + leftPadding, topPadding, 5 + (int)cosSvc_TV.getTextSize() / 5 + rightPadding, bottomPadding);
                    tvGradientPaint.setTextSkewX(-0.25f);
                }
                break;
        }

        // 폰트 종류 옵션
        // 안드로이드 기본 Typeface 3종류 중 하나를 사용하는 경우 해당 값을 적용
        // Gradient + Shadow 조합인 경우 Gradient에도 적용
        switch(Integer.parseInt(mPref.getString(mCon.getString(R.string.pref_font_key_string),  "0"))) {
            case 1:
                tvPaint.setTypeface(Typeface.SERIF);
                if(cosSvc_TVGradient != null)
                    tvGradientPaint.setTypeface(Typeface.SERIF);
                break;
            case 2:
                tvPaint.setTypeface(Typeface.MONOSPACE);
                if(cosSvc_TVGradient != null)
                    tvGradientPaint.setTypeface(Typeface.MONOSPACE);
                break;
            case 3:
                tvPaint.setTypeface(Typeface.SANS_SERIF);
                if(cosSvc_TVGradient != null)
                    tvGradientPaint.setTypeface(Typeface.SANS_SERIF);
                break;
        }
    }

    byte getConfigStatus(boolean bFSState) {
        // 추후 연산에 필요한 시계 구조 설정 값 저장
        String structure;
        if(bFSState)
            structure = mPref.getString(mCon.getString(R.string.pref_clockText_key_string), mCon.getString(R.string.pref_clockText_default_string));
        else
            structure = mPref.getString(mCon.getString(R.string.pref_clockText_notfs_key_string), mCon.getString(R.string.pref_clockText_default_string));

        // COSSvc에서 필요한 설정 값 반환
        byte status = 0b00000000;
        // 터치 기능을 하나라도 사용하는 경우 저장
        if (mPref.getBoolean(mCon.getString(R.string.pref_hideTheClock_key_string), false) || getHideTemporaryByLongTouch())
            status |= 0b10000000; // 8th bit on (bTouchEvent)
        // 서비스에서 TextView 갱신이 필요한지 판단하는 값 중 하나로
        // 순수 문자열만 이루어진 경우를 저장
        // (= 시계 구조 설정 내 .a ~ .w 중 단 하나도 쓰이지 않은 경우)
        // .z는 다음 줄로, ..은 .으로 치환되는 둘 다 단순 문자열이므로 무시
        if(!structure.matches(".*\\.[a-w].*"))
            status |= 0b00010000; // 5th bit on (thereAreOnlyString)
        boolean useBatt = structure.contains(".w");
        boolean useSec = structure.contains(".u") || structure.contains(".v");
        // Batt 사용
        if(useBatt)
            status |= 0b00000010; // 2nd bit on (isUsing_Battery)
        // 초 단위 요소 사용
        if(useBatt || useSec) {
            status |= 0b00100000; // 6th bit on (isUsing_SecElement)
        }

        return status;
    }

    void setContextLocale(Context ActivityOrSvcContext) {
        // 언어 설정
        Locale locale = getLocale();

        Configuration config = new Configuration();
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN_MR1){
            config.setLocale(locale);
        } else {
            config.locale = locale;
        }
        ActivityOrSvcContext.getResources().updateConfiguration(config, ActivityOrSvcContext.getResources().getDisplayMetrics());
    }

    // 현재 화면이 켜져있고 절전모드가 아니라면 참을 반환
    // (삼성 Always On Display 사용시 화면은 켜져있는 것으로 인식되나 절전모드(DOZE)를 사용)
    boolean getIsScreenOnAndNotDOZEState() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT_WATCH) {
            DisplayManager dm = (DisplayManager)mCon.getSystemService(Context.DISPLAY_SERVICE);

            try {
                for (Display display : dm.getDisplays()) {
                    if (display.getDisplayId() == Display.DEFAULT_DISPLAY &&
                            display.getState() == Display.STATE_ON) {
                        return true;
                    }
                }
            } catch (NullPointerException e) {}
            return false;
        } else {
            try {
                return ((PowerManager) mCon.getSystemService(Context.POWER_SERVICE)).isScreenOn();
            } catch (NullPointerException e) {
                return false;
            }
        }
    }

    // 현재 언어 설정 값을 불러옴
    Locale getLocale() {
        if(mPref.getBoolean(mCon.getString(R.string.pref_english_key_string), true)) {
            return Locale.US;
        } else {
            return Locale.KOREA;
        }
    }

    // DateTimeFormatter에 알맞은 포맷으로 변환된 문자열을 반환
    String getClockTextFormatted() {
        return mPref.getString(mCon.getString(R.string.pref_clockTextFormatted_key_string), mCon.getString(R.string.pref_clockText_reset_notify_string));
    }

    String getClockTextFormatted_notfs() {
        return mPref.getString(mCon.getString(R.string.pref_clockTextFormatted_notfs_key_string), mCon.getString(R.string.pref_clockText_reset_notify_string));
    }

    // 시계 문자열의 최대 크기를 계산해 둔 것을 반환
    String getClockTextMax() {
        return mPref.getString(mCon.getString(R.string.pref_clockTextMax_key_string), mCon.getString(R.string.pref_clockText_reset_notify_string));
    }

    String getClockTextMax_notfs() {
        return mPref.getString(mCon.getString(R.string.pref_clockTextMax_notfs_key_string), mCon.getString(R.string.pref_clockText_reset_notify_string));
    }

    // 풀스크린 상태에 따른 시계 위치 값 반환
    byte getClockPosition() {
        return (byte)Integer.parseInt(mPref.getString(mCon.getString(R.string.pref_clockPosition_key_string), "2"));
    }

    byte getClockPosition_notfs() {
        return (byte)Integer.parseInt(mPref.getString(mCon.getString(R.string.pref_clockPosition_notfs_key_string), "2"));
    }

    // 풀스크린 모드 값을 반환
    byte getFSMode() {
        return (byte)Integer.parseInt(PreferenceManager.getDefaultSharedPreferences(mCon).getString(mCon.getString(R.string.pref_fsmode_list_key_string), "0"));
    }

    // Gradient + Shadow 조합 판별을 위해 쓰이는 함수
    boolean getShadowEnabled() {
        return mPref.getBoolean(mCon.getString(R.string.pref_fontShadow_key_string), true);
    }

    // 터치로 숨기기 기능 활성화 여부 반환
    boolean getHideByTouch() {
        return mPref.getBoolean(mCon.getString(R.string.pref_hideTheClock_key_string), false);
    }

    // 숨기는 시간 값 반환
    short getHidingTime() {
        return (short)mPref.getInt(mCon.getString(R.string.pref_hideTheClockTime_key_string), 10);
    }

    boolean getHideTemporaryByLongTouch() {
        return mPref.getBoolean(mCon.getString(R.string.pref_longTouchToHide_key_string), false);
    }

    // Notification 을 만들고 반환
    Notification getNotification(boolean isIdle) {
        Notification.Builder mNotiBuilder;

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            // 알람 채널 관리자 생성
            NotificationManager mNotificationManager = (NotificationManager)mCon.getSystemService(Context.NOTIFICATION_SERVICE);

            // 알림 채널이 존재하는지 확인
            NotificationChannel mChannelExist = mNotificationManager.getNotificationChannel(mCon.getString(R.string.notification_channel_id));

            // 알림 채널이 존재하지 않으면 생성
            if(mChannelExist == null) {
                // Configure the notification channel.
                NotificationChannel mChannel = new NotificationChannel(mCon.getString(R.string.notification_channel_id),
                        mCon.getString(R.string.notification_channel_title), NotificationManager.IMPORTANCE_LOW);
                mChannel.setDescription(mCon.getString(R.string.notification_channel_des));
                mChannel.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);

                // Create channel
                mNotificationManager.createNotificationChannel(mChannel);
            }
            // 알림 빌더 생성
            mNotiBuilder = new Notification.Builder(mCon, mCon.getString(R.string.notification_channel_id));
        } else {
            // 알림 빌더 생성
            mNotiBuilder = new Notification.Builder(mCon);
            if(android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN)
                mNotiBuilder.setPriority(Notification.PRIORITY_MIN);
        }

        // 알림 터치 시 앱 메인화면 띄우기 위한 PendingIntent
        PendingIntent mPendingIntent = PendingIntent.getActivity(
                mCon,
                0,
                new Intent(mCon, COSMain.class),
                PendingIntent.FLAG_UPDATE_CURRENT
        );

        // 알림 설정
        mNotiBuilder.setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(mCon.getString(R.string.notification_channel_title))
                .setContentText(mCon.getString(R.string.text_svc_on))
                .setContentIntent(mPendingIntent)
                .setOngoing(true);
        if(isIdle) {
            mNotiBuilder.setContentText(mCon.getString(R.string.text_svc_idle));
        }

        // 안드로이드 버전에 따라 분기
        if(android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN)
            return mNotiBuilder.build();
        else
            return mNotiBuilder.getNotification();
    }

    // Gradient 색상 값을 반환
    int[] getGradientColors() {
        int current = Integer.parseInt(mPref.getString(mCon.getString(R.string.pref_gradient_key_string), "0"));
        if(current == 1) {
            int[] colors = {0, 0};
            colors[0] = mPref.getInt(mCon.getString(R.string.pref_gradientColor1_key_string), 0xffff6609);
            colors[1] = mPref.getInt(mCon.getString(R.string.pref_gradientColor2_key_string), 0xff09ffff);
            return colors;
        } else if(current == 2) {
            int[] colors = {0, 0, 0};
            colors[0] = mPref.getInt(mCon.getString(R.string.pref_gradientColor1_key_string), 0xffff6609);
            colors[1] = mPref.getInt(mCon.getString(R.string.pref_gradientColor2_key_string), 0xff09ffff);
            colors[2] = mPref.getInt(mCon.getString(R.string.pref_gradientColor3_key_string), 0xff0909ff);
            return colors;
        }

        // Gradient 미사용 시 null 반환
        return null;
    }
}
