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

import java.util.Locale;

import android.app.Notification;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.graphics.Typeface;
import android.hardware.display.DisplayManager;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.util.Log;
import android.util.TypedValue;
import android.view.Display;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

final class COSSvcSubFunc {
    private Context mCon;
    private SharedPreferences mPref;

    COSSvcSubFunc(Context context) {
        mCon = context.getApplicationContext();
        mPref = PreferenceManager.getDefaultSharedPreferences(mCon);
    }

    Byte initSettings(Context context, TextView cosSvc_TV, TextView cosSvc_TVGradient, ViewGroup cosSvc_OutBoundLayout) {
        // 언어 설정
        setContextLocale(context);

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
        if(mPref.getBoolean(mCon.getString(R.string.pref_fontShadow_key_string), false)) {
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

        // 현재 ClockText 설정 값을 기준으로 가능한
        // 최대 길이의 텍스트를 얻어와서 TextView에 집어 넣는다.
        // 추후 텍스트뷰 크기를 계산하여 OutBoundLayout의 고정 크기로 사용
        cosSvc_TV.setText(getMaxAreaOfClockText());

        // 현재 화면의 가로 길이 측정
        // 최소 가로 길이는 480 pixels으로 가정
        Point size = new Point(480, 0);
        Display dis;
        try {
            dis = ((WindowManager) mCon.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
            dis.getSize(size);
        } catch (NullPointerException e) {
        }

        cosSvc_TV.measure(View.MeasureSpec.makeMeasureSpec(size.x, View.MeasureSpec.AT_MOST), 0);

        // 안드로이드 버젼 및 터치로 숨기기 또는
        // 롱 터치로 임시로 숨기기 기능 여부에 따라 다른 값을 사용
        int __flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
        int __type;
        if (!(mPref.getBoolean(mCon.getString(R.string.pref_hideTheClock_key_string), false) || getHideTemporaryByLongTouch())) {
            __flags |= WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE;
            __type = WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY;
        }
        else
            __type = WindowManager.LayoutParams.TYPE_PRIORITY_PHONE;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O)
            __type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;

        // 레이아웃 옵션
        WindowManager.LayoutParams layout;

		// OutBoundLayout를 최상단에 넣기 위한 layout 설정 값
        // 여기서 텍스트뷰 크기를 계산하여 고정된 크기를 사용
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O)
            layout = new WindowManager.LayoutParams(
                    cosSvc_TV.getMeasuredWidth(), cosSvc_TV.getMeasuredHeight(),
                    __type, __flags, PixelFormat.TRANSLUCENT);
        else
            layout = new WindowManager.LayoutParams(
                    cosSvc_TV.getMeasuredWidth(), cosSvc_TV.getMeasuredHeight(),
                    __type, __flags, PixelFormat.TRANSLUCENT);

        // 시계위치 저장
        byte iClockPosition;
        // 만약 "풀스크린 상태에 따라 시계를 다르게 표기" 옵션을 사용하고
        // 저장된 풀스크린 상태가 풀스크린이 아닐 때
        // NotFS에 대응하는 위치 값을 사용
        boolean bUseFSMode2AndNotFSState = ("2".equals(mPref.getString(mCon.getString(R.string.pref_fsmode_list_key_string), "0")) && !mPref.getBoolean("fs_saved", false));
        if(bUseFSMode2AndNotFSState)
            iClockPosition = (byte)Integer.parseInt(mPref.getString(mCon.getString(R.string.pref_clockPosition_notfs_key_string), "2"));
        // 저장된 상태가 풀스크린이거나 "풀스크린 상태에 따라 시계를 다르게 표기" 옵션을
        // 사용 안하는 경우 기존 위치 값을 사용
        else
            iClockPosition = (byte)Integer.parseInt(mPref.getString(mCon.getString(R.string.pref_clockPosition_key_string), "2"));

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
        ((WindowManager)context.getSystemService(Context.WINDOW_SERVICE)).addView(cosSvc_OutBoundLayout, layout);

        // TextView는 width, height를 WRAP_CONTENT로 해서 cosSvc_OutBoundLayout 안에 넣음
        layout.width = WindowManager.LayoutParams.WRAP_CONTENT;
        layout.height = WindowManager.LayoutParams.WRAP_CONTENT;

        // cosSvc_OutBoundLayout에 TextView 삽입
        cosSvc_OutBoundLayout.addView(cosSvc_TV, layout);
        if(cosSvc_TVGradient != null)
            cosSvc_OutBoundLayout.addView(cosSvc_TVGradient, layout);
        // 텍스트뷰를 화면 위에 추가 한 뒤 최대 크기 계산을 위한 넣어둔 쓰래기값을 지움
        cosSvc_TV.setText("");

        // 추후 필요한 시계 구조 설정 값 저장
        String structure;
        if(bUseFSMode2AndNotFSState)
            structure = mPref.getString(mCon.getString(R.string.pref_clockText_notfs_key_string), mCon.getString(R.string.pref_clockText_default_string));
        else
            structure = mPref.getString(mCon.getString(R.string.pref_clockText_key_string), mCon.getString(R.string.pref_clockText_default_string));

        // COSSvc에서 필요한 설정 값 반환
        byte status = 0b00000000;
        // FSMode가 쓰이는지
        if(!mPref.getString(mCon.getString(R.string.pref_fsmode_list_key_string), "0").equals("0"))
            status |= 0b10000000; // 8th bit on (bFSModeEnabled)
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

    // 풀스크린 모드 값을 반환
    byte getFSMode() {
        return (byte)Integer.parseInt(mPref.getString(mCon.getString(R.string.pref_fsmode_list_key_string), "0"));
    }

    // 풀스크린 상태 저장 값을 반환
    // "풀스크린 상태에 따라 시계를 다르게 표시" 옵션에서
    // 현재 저장해둔 풀스크린 상태를 알기 위한 함수
    boolean getFSSaved() {
        return mPref.getBoolean("fs_saved", false);
    }
    // 풀스크린 상태 값을 저장
    // "풀스크린 상태에 따라 시계를 다르게 표시" 옵션에서
    // 실제로 풀스크린 상태를 보니 저장된 값과 다를 때
    // 바뀐 풀스크린 상태 값을 저장할 때 사용하는 함수
    void saveFS(boolean bIsFS) {
        mPref.edit().putBoolean("fs_saved", bIsFS).apply();
    }

    // Gradient + Shadow 조합 판별을 위해 쓰이는 함수
    boolean getShadowEnabled() {
        return mPref.getBoolean(mCon.getString(R.string.pref_fontShadow_key_string), false);
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
            mNotiBuilder = new Notification.Builder(mCon, mCon.getString(R.string.notification_channel_id));
        } else {
            mNotiBuilder = new Notification.Builder(mCon);
            if(android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN)
                mNotiBuilder.setPriority(Notification.PRIORITY_MIN);
        }

        mNotiBuilder.setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(mCon.getString(R.string.notification_channel_title))
                .setContentText(mCon.getString(R.string.text_svc_on))
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

    // 시계 구조 설정 값을 DateTimeFormatter에 알맞은 포멧으로 변환
    // * 프로젝트 경로의 COS_structure.txt 파일 참조
    String getClockTextFormat() {
        StringBuilder SB = new StringBuilder();

        // 시계 구조 저장
        char[] array;
        if("2".equals(mPref.getString(mCon.getString(R.string.pref_fsmode_list_key_string), "0")) && !mPref.getBoolean("fs_saved", false))
            array = mPref.getString(mCon.getString(R.string.pref_clockText_notfs_key_string), mCon.getString(R.string.pref_clockText_default_string)).toCharArray();
        else
            array = mPref.getString(mCon.getString(R.string.pref_clockText_key_string), mCon.getString(R.string.pref_clockText_default_string)).toCharArray();

        // 같은 종류의 값을 이어서 사용하는 경우 또는 택스트와 구성요소 사이 구분을 위한 변수
        // 0 - 문자열 / 1 - 년 / 2 - 분기 / 3 - 월 / 4 - 주차 / 5 - 일 / 6 - 요일
        // 7 - 오전,오후 / 8 - 시간1(h) / 9 - 시간2(H) / 10 - 분 / 11 - 초 / 12 - 초기 값
        byte state = 12;
        // for loop
        byte i = 0;
        byte len = (byte)(array.length - 1);
        // 변환작업
        while(i < len) {
            if(array[i] == '.') {
                if(array[i + 1] == 'z') {
                    if(state != 0) { SB.append('\''); state = 0; }
                    SB.append('\n');
                    i++;
                }
                else if(array[i + 1] == 'a') {
                    // 1.문자열을 쓰다가 구성요소로 오면 '(문자열 구분자) 추가
                    // 2.현재 구성요소와 같은 문자를 쓰는 구성요소를 이어서 쓰는 경우
                    // 사이에 Zero Width Space(유니코드 공식 문자) 추가
                    // 3.다른 구성요소에서 현재 구성요소를 추가한 경우
                    // 현재 구성요소 상태로 변환(이 밑으로 같은 형식으로 작성)
                    if(state == 0) { SB.append('\''); state = 1; }
                    else if(state == 1) { SB.append('\u200B'); }
                    else state = 1;
                    SB.append("uuuu"); // 년 (2018)
                    i++;
                }
                else if(array[i + 1] == 'b') {
                    if(state == 0) { SB.append('\''); state = 1; }
                    else if(state == 1) { SB.append('\u200B'); }
                    else state = 1;
                    SB.append("uu"); // 년 (18)
                    i++;
                }
                else if(array[i + 1] == 'c') {
                    if(state == 0) { SB.append('\''); state = 2; }
                    else if(state == 2) { SB.append('\u200B'); }
                    else state = 2;
                    SB.append('Q'); // 분기 (1 ~ 4)
                    i++;
                }
                else if(array[i + 1] == 'd') {
                    if(state == 0) { SB.append('\''); state = 2; }
                    else if(state == 2) { SB.append('\u200B'); }
                    else state = 2;
                    SB.append("QQQ"); // 분기 (Q1, Q2, Q3, Q4)
                    i++;
                }
                else if(array[i + 1] == 'e') {
                    if(state == 0) { SB.append('\''); state = 3; }
                    else if(state == 3) { SB.append('\u200B'); }
                    else state = 3;
                    SB.append('M'); // 월 (1 ~ 12)
                    i++;
                }
                else if(array[i + 1] == 'f') {
                    if(state == 0) { SB.append('\''); state = 3; }
                    else if(state == 3) { SB.append('\u200B'); }
                    else state = 3;
                    SB.append("MM"); // 월 (01 ~ 12)
                    i++;
                }
                else if(array[i + 1] == 'g') {
                    if(state == 0) { SB.append('\''); state = 3; }
                    else if(state == 3) { SB.append('\u200B'); }
                    else state = 3;
                    SB.append("MMM"); // 월 (Jan, Feb...) (1월, 2월...)
                    i++;
                }
                else if(array[i + 1] == 'h') {
                    if(state == 0) { SB.append('\''); state = 3; }
                    else if(state == 3) { SB.append('\u200B'); }
                    else state = 3;
                    SB.append("MMMM"); // 월 (January, February...) (1월, 2월...)
                    i++;
                }
                else if(array[i + 1] == 'i') {
                    if(state == 0) { SB.append('\''); state = 4; }
                    else if(state == 4) { SB.append('\u200B'); }
                    else state = 4;
                    SB.append('W'); // 주차 (1 ~ 5)
                    i++;
                }
                else if(array[i + 1] == 'j') {
                    if(state == 0) { SB.append('\''); state = 5; }
                    else if(state == 5) { SB.append('\u200B'); }
                    else state = 5;
                    SB.append('d'); // 일 (1 ~ 31)
                    i++;
                }
                else if(array[i + 1] == 'k') {
                    if(state == 0) { SB.append('\''); state = 5; }
                    else if(state == 5) { SB.append('\u200B'); }
                    else state = 5;
                    SB.append("dd"); // 일 (01 ~ 31)
                    i++;
                }
                else if(array[i + 1] == 'l') {
                    if(state == 0) { SB.append('\''); state = 6; }
                    else if(state == 6) { SB.append('\u200B'); }
                    else state = 6;
                    SB.append("EE"); // 요일 (Mon, Tus...) (월, 화...)
                    i++;
                }
                else if(array[i + 1] == 'm') {
                    if(state == 0) { SB.append('\''); state = 6; }
                    else if(state == 6) { SB.append('\u200B'); }
                    else state = 6;
                    SB.append("EEEE"); // 요일 (Monday, Tuesday...) (월요일, 화요일...)
                    i++;
                }
                else if(array[i + 1] == 'n') {
                    if(state == 0) { SB.append('\''); state = 7; }
                    else if(state == 7) { SB.append('\u200B'); }
                    else state = 7;
                    SB.append('a'); // "AM" or "PM", "오전" 또는 "오후"
                    i++;
                }
                else if(array[i + 1] == 'o') {
                    if(state == 0) { SB.append('\''); state = 8; }
                    else if(state == 8) { SB.append('\u200B'); }
                    else state = 8;
                    SB.append('h'); // 시간 (1 ~ 12)
                    i++;
                }
                else if(array[i + 1] == 'p') {
                    if(state == 0) { SB.append('\''); state = 8; }
                    else if(state == 8) { SB.append('\u200B'); }
                    else state = 8;
                    SB.append("hh"); // 시간 (01 ~ 12)
                    i++;
                }
                else if(array[i + 1] == 'q') {
                    if(state == 0) { SB.append('\''); state = 9; }
                    else if(state == 9) { SB.append('\u200B'); }
                    else state = 9;
                    SB.append('H'); // 시간 (0 ~ 23)
                    i++;
                }
                else if(array[i + 1] == 'r') {
                    if(state == 0) { SB.append('\''); state = 9; }
                    else if(state == 9) { SB.append('\u200B'); }
                    else state = 9;
                    SB.append("HH"); // 시간 (00 ~ 23)
                    i++;
                }
                else if(array[i + 1] == 's') {
                    if(state == 0) { SB.append('\''); state = 10; }
                    else if(state == 10) { SB.append('\u200B'); }
                    else state = 10;
                    SB.append('m'); // 분 (0 ~ 59)
                    i++;
                }
                else if(array[i + 1] == 't') {
                    if(state == 0) { SB.append('\''); state = 10; }
                    else if(state == 10) { SB.append('\u200B'); }
                    else state = 10;
                    SB.append("mm"); // 분 (00 ~ 59)
                    i++;
                }
                else if(array[i + 1] == 'u') {
                    if(state == 0) { SB.append('\''); state = 11; }
                    //else if(state == 11) { SB.append('\u200B'); }
                    else state = 11;
                    //SB.append('s');
                    // 초(0~59)에 대한 특수 값
                    SB.append('\uF002');
                    i++;
                }
                else if(array[i + 1] == 'v') {
                    if(state == 0) { SB.append('\''); state = 11; }
                    //else if(state == 11) { SB.append('\u200B'); }
                    else state = 11;
                    //SB.append("ss");
                    // 초(00~59)에 대한 특수 값
                    SB.append('\uF001');
                    i++;
                }
                else if(array[i + 1] == 'w') {
                    if(state != 0) { SB.append('\''); state = 0; }
                    // 배터리에 대한 특수 값
                    SB.append('\uF000');
                    i++;
                }
                else if(array[i + 1] == '.') {
                    if(state != 0) { SB.append('\''); state = 0; }
                    SB.append(".");
                    i++;
                }
            }
            // '문자는 DateTimeFormatter에서 문자열 구분용으로 쓰이므로
            // '문자가 입력된 경우 따로 ''로 변환해줘야 정상 출력
            else if(array[i] == '\'') {
                if(state != 0) { SB.append('\''); state = 0; }
                SB.append("\'\'");
            }
            // .와 다음줄 문자를 제외한 값을 처리
            else if(array[i] != '\n' && array[i] != '\r') {
                if(state != 0) { SB.append('\''); state = 0; }
                SB.append(array[i]);
            }
            i++;
        }
        // 마지막 문자 1개가 남았을 시 넣음
        if(i < len + 1 && array[i] != '.' && array[i] != '\n' && array[i] != '\r') {
            if(state != 0) { SB.append('\''); state = 0; }
            if(array[i] == '\'') SB.append("\'\'");
            else SB.append(array[i]);
        }
        // 마지막에 문자열 입력상태면 문자열 끝을 표시하기 위해 '추가
        if(state == 0) SB.append('\'');

        return SB.toString();
    }

    // 시계 구조 설정 값을 기준으로 시계 문자열의 최대 크기를 계산
    // * 프로젝트 경로의 COS_structure.txt 파일 참조
    private String getMaxAreaOfClockText() {
        StringBuilder SB = new StringBuilder();

        // 시계 구조 저장
        char[] array;
        if("2".equals(mPref.getString(mCon.getString(R.string.pref_fsmode_list_key_string), "0")) && !mPref.getBoolean("fs_saved", false))
            array = mPref.getString(mCon.getString(R.string.pref_clockText_notfs_key_string), mCon.getString(R.string.pref_clockText_default_string)).toCharArray();
        else
            array = mPref.getString(mCon.getString(R.string.pref_clockText_key_string), mCon.getString(R.string.pref_clockText_default_string)).toCharArray();

        // for loop
        byte i = 0;
        byte len = (byte)(array.length - 1);
        // 변환작업
        while(i < len) {
            if(array[i] == '.') {
                if(array[i + 1] == 'z') {
                    SB.append(" \n");
                    i++;
                }
                else if(array[i + 1] == 'a') {
                    SB.append("0000");
                    i++;
                }
                else if(array[i + 1] == 'b' || array[i + 1] == 'e' || array[i + 1] == 'f' || array[i + 1] == 'j' ||
                        array[i + 1] == 'k' || array[i + 1] == 'o' || array[i + 1] == 'p' || array[i + 1] == 'q' || array[i + 1] == 'r' ||
                        array[i + 1] == 's' || array[i + 1] == 't' || array[i + 1] == 'u' || array[i + 1] == 'v') {
                    SB.append("00");
                    i++;
                }
                else if(array[i + 1] == 'c' || array[i + 1] == 'i') {
                    SB.append('0');
                    i++;
                }
                else if(array[i + 1] == 'd') {
                    SB.append("W0");
                    i++;
                }
                else if(array[i + 1] == 'g' || array[i + 1] == 'l') {
                    SB.append("WWW");
                    i++;
                }
                else if(array[i + 1] == 'h' || array[i + 1] == 'm') {
                    SB.append("WWWWWWWWW");
                    i++;
                }
                else if(array[i + 1] == 'n') {
                    SB.append("뷁뷁");
                    i++;
                }
                else if(array[i + 1] == 'w') {
                    SB.append("100%◎");
                    i++;
                }
                else if(array[i + 1] == '.') {
                    SB.append('.');
                    i++;
                }
            }
            // .와 다음줄 문자를 제외한 값을 처리
            else if(array[i] != '\n' && array[i] != '\r') {
                SB.append(array[i]);
            }
            i++;
        }
        // 마지막 문자 1개가 남았을 시 넣음
        if(i < len + 1 && array[i] != '.' && array[i] != '\n' && array[i] != '\r') {
            SB.append(array[i]);
        }
        SB.append(' ');

        return SB.toString();
    }
}
