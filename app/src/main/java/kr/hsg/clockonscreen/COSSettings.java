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

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Locale;

public final class COSSettings extends PreferenceActivity
        implements Preference.OnPreferenceClickListener, Preference.OnPreferenceChangeListener {
    private Context mCon;
    private SharedPreferences mPref;

    private AlertDialog ClockTextDialog;
    private AlertDialog ClockTextNotFSDialog;
    private AlertDialog FontSizeDialog;
    private AlertDialog FontShadowShapeDialog;
    private AlertDialog ClockTransparencyDialog;
    private AlertDialog HideTheClockTimeDialog;
    private AlertDialog ResetSettingsDialog;
	private View ClockTextLayout;
    private View ClockTextNotFSLayout;
	private View FontSizeLayout;
    private View FontShadowShapeLayout;
    private View ClockTransparencyLayout;
	private View HideTheClockTimeLayout;
	private TextWatcher ClockTextTWatcher;
    private TextWatcher ClockTextNotFSTWatcher;
    private TextWatcher ClockTransparencyTWatcher;

    private StringBuilder ClockTextPreviewBuilder;
    private EditText etClockText;
    private TextView tvClockTextPreview;
    private ScrollView svClockTextPreview;
    private EditText etClockTextNotFS;
    private TextView tvClockTextNotFSPreview;
    private ScrollView svClockTextNotFSPreview;
    private EditText etFontSize;
    private EditText etFontShadowDx;
    private EditText etFontShadowDy;
    private EditText etFontShadowRadius;
    private EditText etClockTransparency;
    private SeekBar seekClockTransparency;
    private EditText etHideTheClockTime;

    private boolean ClockTransparencyEditTextFromUser = true;

    // 서비스 실행
    private void runService(int way, boolean stillInPrefActivity) {
        Intent mSvc_Idle = new Intent(mCon, COSSvc_Idle.class);
        Intent mSvc = new Intent(mCon, COSSvc.class);

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M
                && !Settings.canDrawOverlays(mCon)) {
            mCon.stopService(mSvc); mCon.stopService(mSvc_Idle);

            mPref.edit().putBoolean("service_running", false).apply();

            // FLAG_ACTIVITY_NEW_TASK 때문에 ApplicationContext에서도 실행
            mCon.startActivity(new Intent(mCon, COSMain.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            return;
        }

        // 0 = stop / 1 = start / 2 = restart
        // 여기서 0, 2만 쓰이므로 Main의 함수와는 달리 필요없는 부분 제거
        mCon.stopService(mSvc); mCon.stopService(mSvc_Idle);
        if(way == 2) {
            if(stillInPrefActivity) {
                // 설정 창임을 알리는 값을 추가
                mSvc.putExtra("PreferenceView", true);
                mCon.startService(mSvc);
            } else {
                mCon.startService(mSvc_Idle);
            }
        }
    }

    // make Preview and get Modified Line
    // return Modified Line
    // byte를 반환하므로 다른 곳에서 사용 시 주의
    // 여기서는 최대 80줄을 초과 할 수 없으므로 byte 사용
    // * 프로젝트 경로의 COS_structure.txt 파일 참조
    private byte refreshPreview(CharSequence data, int modified_end, StringBuilder SB) {
        char[] array = data.toString().toCharArray();
        boolean mEnglish = mPref.getBoolean(mCon.getString(R.string.pref_english_key_string), true);

        // for loop
        byte i = 0;
        byte len = (byte)(array.length - 1);
		// 현재 수정중인 위치의 라인 값 저장
        byte linecnt = 0;
        // 변환작업
        while(i < len) {
            if(array[i] == '.') {
                if(array[i + 1] == 'z') {
                    SB.append('\n');
                    if(i < (modified_end)) linecnt++;
                    i++;
                }
                else if(array[i + 1] == 'a') {
                    SB.append("2018");
                    i++;
                }
                else if(array[i + 1] == 'b') {
                    SB.append("18");
                    i++;
                }
                else if(array[i + 1] == 'c') {
                    SB.append('1');
                    i++;
                }
                else if(array[i + 1] == 'd') {
                    SB.append("Q1");
                    i++;
                }
                else if(array[i + 1] == 'e') {
                    SB.append('1');
                    i++;
                }
                else if(array[i + 1] == 'f') {
                    SB.append("01");
                    i++;
                }
                else if(array[i + 1] == 'g') {
                    // 일부 설정은 언어 설정에 따라 다른 출력을 나타냄
                    if(mEnglish) SB.append("Jan");
                    else SB.append("1월");
                    i++;
                }
                else if(array[i + 1] == 'h') {
                    // 일부 설정은 언어 설정에 따라 다른 출력을 나타냄
                    if(mEnglish) SB.append("January");
                    else SB.append("1월");
                    i++;
                }
                else if(array[i + 1] == 'i') {
                    SB.append('1');
                    i++;
                }
                else if(array[i + 1] == 'j') {
                    SB.append('1');
                    i++;
                }
                else if(array[i + 1] == 'k') {
                    SB.append("01");
                    i++;
                }
                else if(array[i + 1] == 'l') {
                    // 일부 설정은 언어 설정에 따라 다른 출력을 나타냄
                    if(mEnglish) SB.append("Mon");
                    else SB.append("월");
                    i++;
                }
                else if(array[i + 1] == 'm') {
                    // 일부 설정은 언어 설정에 따라 다른 출력을 나타냄
                    if(mEnglish) SB.append("Monday");
                    else SB.append("월요일");
                    i++;
                }
                else if(array[i + 1] == 'n') {
                    // 일부 설정은 언어 설정에 따라 다른 출력을 나타냄
                    if(mEnglish) SB.append("PM");
                    else SB.append("오후");
                    i++;
                }
                else if(array[i + 1] == 'o') {
                    SB.append('1');
                    i++;
                }
                else if(array[i + 1] == 'p') {
                    SB.append("01");
                    i++;
                }
                else if(array[i + 1] == 'q') {
                    SB.append("13");
                    i++;
                }
                else if(array[i + 1] == 'r') {
                    SB.append("13");
                    i++;
                }
                else if(array[i + 1] == 's') {
                    SB.append('1');
                    i++;
                }
                else if(array[i + 1] == 't') {
                    SB.append("01");
                    i++;
                }
                else if(array[i + 1] == 'u') {
                    SB.append('1');
                    i++;
                }
                else if(array[i + 1] == 'v') {
                    SB.append("01");
                    i++;
                }
                else if(array[i + 1] == 'w') {
                    SB.append("50%▲");
                    i++;
                }
                else if(array[i + 1] == 'x') {
                    SB.append('⇵');
                    i++;
                }
                else if(array[i + 1] == 'y') {
                    SB.append('M');
                    i++;
                }
                else if(array[i + 1] == '.') {
                    SB.append(".");
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

        return linecnt;
    }

    // 시계 구조 설정 값을 DateTimeFormatter에 알맞은 포맷으로 변환
    // * 프로젝트 경로의 COS_structure.txt 파일 참조
    static String getClockTextFormatted(String data) {
        StringBuilder SB = new StringBuilder();

        // 시계 구조 저장
        char[] array = data.toCharArray();

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
                else if(i < (len - 1) && array[i + 1] == 'w' && array[i + 2] == 'w') {
                    if(state != 0) { SB.append('\''); state = 0; }
                    // 배터리 전압(히든)에 대한 특수 값
                    SB.append('\uF005');
                    i += 2;
                }
                else if(array[i + 1] == 'w') {
                    if(state != 0) { SB.append('\''); state = 0; }
                    // 배터리에 대한 특수 값
                    SB.append('\uF000');
                    i++;
                }
                else if(array[i + 1] == 'x') {
                    if(state != 0) { SB.append('\''); state = 0; }
                    // 네트워크 상태에 대한 특수 값
                    SB.append('\uF003');
                    i++;
                }
                else if(array[i + 1] == 'y') {
                    if(state != 0) { SB.append('\''); state = 0; }
                    // 네트워크 상태(문자)에 대한 특수 값
                    SB.append('\uF004');
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
    static String getClockTextMax(String data, boolean mEnglish) {
        StringBuilder SB = new StringBuilder();

        // 시계 구조 저장
        char[] array = data.toCharArray();

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
                else if(array[i + 1] == 'b' || array[i + 1] == 'e' || array[i + 1] == 'f' ||
                        array[i + 1] == 'j' || array[i + 1] == 'k' || array[i + 1] == 'o' ||
                        array[i + 1] == 'p' || array[i + 1] == 'q' || array[i + 1] == 'r' ||
                        array[i + 1] == 's' || array[i + 1] == 't' || array[i + 1] == 'u' ||
                        array[i + 1] == 'v') {
                    SB.append("00");
                    i++;
                }
                else if(array[i + 1] == 'c' || array[i + 1] == 'i') {
                    SB.append('0');
                    i++;
                }
                else if(array[i + 1] == 'd') {
                    SB.append("Q0");
                    i++;
                }
                else if(array[i + 1] == 'g') {
                    if(mEnglish) SB.append("Www");
                    else SB.append("00월");
                    i++;
                }
                else if(array[i + 1] == 'h') {
                    if(mEnglish) SB.append("Wwwwwwwww");
                    else SB.append("00월");
                    i++;
                }
                else if(array[i + 1] == 'l') {
                    if(mEnglish) SB.append("Www");
                    else SB.append("뷁");
                    i++;
                }
                else if(array[i + 1] == 'm') {
                    if(mEnglish) SB.append("Wwwwwwwww");
                    else SB.append("뷁요일");
                    i++;
                }
                else if(array[i + 1] == 'n') {
                    if(mEnglish) SB.append("WM");
                    else SB.append("오뷁");
                    i++;
                }
                else if(i < (len - 1) && array[i + 1] == 'w' && array[i + 2] == 'w') {
                    SB.append("8888");
                    i += 2;
                }
                else if(array[i + 1] == 'w') {
                    SB.append("100%△");
                    i++;
                }
                else if(array[i + 1] == 'x') {
                    SB.append("≋⇵");
                    i++;
                }
                else if(array[i + 1] == 'y') {
                    SB.append("W+M");
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

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.pref_clockonscreen);
        setTitle(R.string.action_settings);

        mCon = getApplicationContext();
        mPref = PreferenceManager.getDefaultSharedPreferences(mCon);

        // 각 설정에 클릭리스너 등록
        findPreference(mCon.getString(R.string.pref_clockText_key_string)).setOnPreferenceClickListener(this);
        findPreference(mCon.getString(R.string.pref_clockText_notfs_key_string)).setOnPreferenceClickListener(this);
        findPreference(mCon.getString(R.string.pref_fontSize_key_string)).setOnPreferenceClickListener(this);
        findPreference(mCon.getString(R.string.pref_fontShadowShape_key_string)).setOnPreferenceClickListener(this);
        findPreference(mCon.getString(R.string.pref_clockTransparency_key_string)).setOnPreferenceClickListener(this);
        findPreference(mCon.getString(R.string.pref_hideTheClockTime_key_string)).setOnPreferenceClickListener(this);
        findPreference(mCon.getString(R.string.pref_resetSettings_key_string)).setOnPreferenceClickListener(this);

        // 색상 변경 및 기타 설정값이 변경되었을 때 서비스가 실행중이면 재실행하도록 설정
        findPreference(mCon.getString(R.string.pref_fsmode_list_key_string)).setOnPreferenceChangeListener(this);
        findPreference(mCon.getString(R.string.pref_font_key_string)).setOnPreferenceChangeListener(this);
        findPreference(mCon.getString(R.string.pref_fontAppearance_key_string)).setOnPreferenceChangeListener(this);
        findPreference(mCon.getString(R.string.pref_gradient_key_string)).setOnPreferenceChangeListener(this);
        findPreference(mCon.getString(R.string.pref_fontColor_key_string)).setOnPreferenceChangeListener(this);
        findPreference(mCon.getString(R.string.pref_gradientColor1_key_string)).setOnPreferenceChangeListener(this);
        findPreference(mCon.getString(R.string.pref_gradientColor2_key_string)).setOnPreferenceChangeListener(this);
        findPreference(mCon.getString(R.string.pref_gradientColor3_key_string)).setOnPreferenceChangeListener(this);
        findPreference(mCon.getString(R.string.pref_fontShadow_key_string)).setOnPreferenceChangeListener(this);
        findPreference(mCon.getString(R.string.pref_fontShadowColor_key_string)).setOnPreferenceChangeListener(this);
        findPreference(mCon.getString(R.string.pref_hideTheClock_key_string)).setOnPreferenceChangeListener(this);
        findPreference(mCon.getString(R.string.pref_longTouchToHide_key_string)).setOnPreferenceChangeListener(this);
        findPreference(mCon.getString(R.string.pref_background_key_string)).setOnPreferenceChangeListener(this);
        findPreference(mCon.getString(R.string.pref_backgroundColor_key_string)).setOnPreferenceChangeListener(this);
        findPreference(mCon.getString(R.string.pref_fixNotRefresh_key_string)).setOnPreferenceChangeListener(this);

        // 따로 처리해야 하는 설정
        findPreference(mCon.getString(R.string.pref_clockPosition_key_string)).setOnPreferenceChangeListener(this);
        findPreference(mCon.getString(R.string.pref_clockPosition_notfs_key_string)).setOnPreferenceChangeListener(this);
        findPreference(mCon.getString(R.string.pref_english_key_string)).setOnPreferenceChangeListener(this);

        // 현재 값을 표기하는 모든 설정에 대하여 현재 값 표기
        Resources res = mCon.getResources();
        String[] titles_clockposition = res.getStringArray(R.array.pref_clockposition_list);
        String[] titles_fsmode_list = res.getStringArray(R.array.pref_fsmode_list);
        String[] titles_font = res.getStringArray(R.array.pref_font_list);
        String[] titles_fontappearance = res.getStringArray(R.array.pref_fontappearance_list);
        String[] titles_gradient = res.getStringArray(R.array.pref_gradient_list);

        // 현재 값을 설정에 표기 및 변경된 값에 따라 연동되는 설정들의 활성화 여부를 조절
        int mGradient = Integer.parseInt(mPref.getString(mCon.getString(R.string.pref_gradient_key_string), "0"));

        switch(Integer.parseInt(mPref.getString(mCon.getString(R.string.pref_clockPosition_key_string), "2"))) {
            case 0: findPreference(mCon.getString(R.string.pref_clockPosition_key_string)).setSummary(mCon.getString(R.string.pref_prefix_current) + titles_clockposition[0]); break;
            case 1: findPreference(mCon.getString(R.string.pref_clockPosition_key_string)).setSummary(mCon.getString(R.string.pref_prefix_current) + titles_clockposition[1]); break;
            case 2: findPreference(mCon.getString(R.string.pref_clockPosition_key_string)).setSummary(mCon.getString(R.string.pref_prefix_current) + titles_clockposition[2]); break;
            case 3: findPreference(mCon.getString(R.string.pref_clockPosition_key_string)).setSummary(mCon.getString(R.string.pref_prefix_current) + titles_clockposition[3]); break;
            case 4: findPreference(mCon.getString(R.string.pref_clockPosition_key_string)).setSummary(mCon.getString(R.string.pref_prefix_current) + titles_clockposition[4]); break;
            case 5: findPreference(mCon.getString(R.string.pref_clockPosition_key_string)).setSummary(mCon.getString(R.string.pref_prefix_current) + titles_clockposition[5]); break;
        }
        switch(Integer.parseInt(mPref.getString(mCon.getString(R.string.pref_fsmode_list_key_string), "0"))) {
            case 0:
                findPreference(mCon.getString(R.string.pref_fsmode_list_key_string)).setSummary(mCon.getString(R.string.pref_prefix_current) + titles_fsmode_list[0]);
                findPreference(mCon.getString(R.string.pref_clockText_notfs_key_string)).setEnabled(false);
                findPreference(mCon.getString(R.string.pref_clockPosition_notfs_key_string)).setEnabled(false);
                break;
            case 1:
                findPreference(mCon.getString(R.string.pref_fsmode_list_key_string)).setSummary(mCon.getString(R.string.pref_prefix_current) + titles_fsmode_list[1] + mCon.getString(R.string.pref_fsmode_summary_postfix));
                findPreference(mCon.getString(R.string.pref_clockText_notfs_key_string)).setEnabled(false);
                findPreference(mCon.getString(R.string.pref_clockPosition_notfs_key_string)).setEnabled(false);
                break;
            case 2:
                findPreference(mCon.getString(R.string.pref_fsmode_list_key_string)).setSummary(mCon.getString(R.string.pref_prefix_current) + titles_fsmode_list[2] + mCon.getString(R.string.pref_fsmode_summary_postfix));
                findPreference(mCon.getString(R.string.pref_clockText_notfs_key_string)).setEnabled(true);
                findPreference(mCon.getString(R.string.pref_clockPosition_notfs_key_string)).setEnabled(true);
                break;
        }
        switch(Integer.parseInt(mPref.getString(mCon.getString(R.string.pref_clockPosition_notfs_key_string), "2"))) {
            case 0: findPreference(mCon.getString(R.string.pref_clockPosition_notfs_key_string)).setSummary(mCon.getString(R.string.pref_prefix_current) + titles_clockposition[0]); break;
            case 1: findPreference(mCon.getString(R.string.pref_clockPosition_notfs_key_string)).setSummary(mCon.getString(R.string.pref_prefix_current) + titles_clockposition[1]); break;
            case 2: findPreference(mCon.getString(R.string.pref_clockPosition_notfs_key_string)).setSummary(mCon.getString(R.string.pref_prefix_current) + titles_clockposition[2]); break;
            case 3: findPreference(mCon.getString(R.string.pref_clockPosition_notfs_key_string)).setSummary(mCon.getString(R.string.pref_prefix_current) + titles_clockposition[3]); break;
            case 4: findPreference(mCon.getString(R.string.pref_clockPosition_notfs_key_string)).setSummary(mCon.getString(R.string.pref_prefix_current) + titles_clockposition[4]); break;
            case 5: findPreference(mCon.getString(R.string.pref_clockPosition_notfs_key_string)).setSummary(mCon.getString(R.string.pref_prefix_current) + titles_clockposition[5]); break;
        }
        findPreference(mCon.getString(R.string.pref_fontSize_key_string)).setSummary(mCon.getString(R.string.pref_prefix_current) + Integer.toString(mPref.getInt(mCon.getString(R.string.pref_fontSize_key_string),18)));
        switch(Integer.parseInt(mPref.getString(mCon.getString(R.string.pref_font_key_string), "0"))) {
            case 0: findPreference(mCon.getString(R.string.pref_font_key_string)).setSummary(mCon.getString(R.string.pref_prefix_current) + titles_font[0]); break;
            case 1: findPreference(mCon.getString(R.string.pref_font_key_string)).setSummary(mCon.getString(R.string.pref_prefix_current) + mCon.getString(R.string.pref_font_serif_string)); break;
            case 2: findPreference(mCon.getString(R.string.pref_font_key_string)).setSummary(mCon.getString(R.string.pref_prefix_current) + mCon.getString(R.string.pref_font_monospace_string)); break;
            case 3: findPreference(mCon.getString(R.string.pref_font_key_string)).setSummary(mCon.getString(R.string.pref_prefix_current) + mCon.getString(R.string.pref_font_sans_serif_string)); break;
        }
        switch(Integer.parseInt(mPref.getString(mCon.getString(R.string.pref_fontAppearance_key_string), "0"))) {
            case 0: findPreference(mCon.getString(R.string.pref_fontAppearance_key_string)).setSummary(mCon.getString(R.string.pref_prefix_current) + titles_fontappearance[0]); break;
            case 1: findPreference(mCon.getString(R.string.pref_fontAppearance_key_string)).setSummary(mCon.getString(R.string.pref_prefix_current) + titles_fontappearance[1]); break;
            case 2: findPreference(mCon.getString(R.string.pref_fontAppearance_key_string)).setSummary(mCon.getString(R.string.pref_prefix_current) + titles_fontappearance[2]); break;
        }
        switch(mGradient) {
            case 0:
                findPreference(mCon.getString(R.string.pref_gradient_key_string)).setSummary(mCon.getString(R.string.pref_prefix_current) + titles_gradient[0]);
                findPreference(mCon.getString(R.string.pref_gradientColor1_key_string)).setEnabled(false);
                findPreference(mCon.getString(R.string.pref_gradientColor2_key_string)).setEnabled(false);
                findPreference(mCon.getString(R.string.pref_gradientColor3_key_string)).setEnabled(false);
                findPreference(mCon.getString(R.string.pref_fontColor_key_string)).setEnabled(true);
                break;
            case 1:
                findPreference(mCon.getString(R.string.pref_gradient_key_string)).setSummary(getString(R.string.pref_prefix_current) + titles_gradient[1]);
                findPreference(mCon.getString(R.string.pref_gradientColor1_key_string)).setEnabled(true);
                findPreference(mCon.getString(R.string.pref_gradientColor2_key_string)).setEnabled(true);
                findPreference(mCon.getString(R.string.pref_gradientColor3_key_string)).setEnabled(false);
                findPreference(getString(R.string.pref_fontColor_key_string)).setEnabled(false);
                break;
            case 2:
                findPreference(mCon.getString(R.string.pref_gradient_key_string)).setSummary(mCon.getString(R.string.pref_prefix_current) + titles_gradient[2]);
                findPreference(mCon.getString(R.string.pref_gradientColor1_key_string)).setEnabled(true);
                findPreference(mCon.getString(R.string.pref_gradientColor2_key_string)).setEnabled(true);
                findPreference(mCon.getString(R.string.pref_gradientColor3_key_string)).setEnabled(true);
                findPreference(mCon.getString(R.string.pref_fontColor_key_string)).setEnabled(false);
                break;
        }
        findPreference(mCon.getString(R.string.pref_fontShadowShape_key_string)).setSummary(mCon.getString(R.string.pref_prefix_current) + String.format(Locale.US, "%.0f", mPref.getFloat(mCon.getString(R.string.pref_fontShadowShape_dx_key_string), 2.0f)) + "x " +
                String.format(Locale.US, "%.0f", mPref.getFloat(mCon.getString(R.string.pref_fontShadowShape_dy_key_string), 2.0f)) + "y " +
                String.format(Locale.US, "%.0f", mPref.getFloat(mCon.getString(R.string.pref_fontShadowShape_radius_key_string), 3.0f)) + "r");
        findPreference(mCon.getString(R.string.pref_clockTransparency_key_string)).setSummary(mCon.getString(R.string.pref_prefix_current) + Integer.toString(mPref.getInt(mCon.getString(R.string.pref_clockTransparency_key_string), 255)) + mCon.getString(R.string.pref_clocktransparency_current_postfix));
        findPreference(mCon.getString(R.string.pref_hideTheClockTime_key_string)).setSummary(mCon.getString(R.string.pref_prefix_current) + Integer.toString(mPref.getInt(mCon.getString(R.string.pref_hideTheClockTime_key_string),10)) + mCon.getString(R.string.pref_suffix_second));
    } //onCreate

    @Override
    protected void onResume() {
        super.onResume();

        // 서비스 무조건 실행 + 설정 창이므로 FS모드 무시
        Toast.makeText(COSSettings.this, R.string.pref_toast_fsmode_ignored_on_prefactivity, Toast.LENGTH_SHORT).show();
        runService(2, true);
    } // onResume

    @Override
    protected void onPause() {
        super.onPause();
        // 서비스가 켜진상태면 FS모드 다시 적용하여 재실행 + 꺼진 상태면 꺼버리기
        if(mPref.getBoolean("service_running", false))
            runService(2, false);
        else
            runService(0, false);
    }

    // 열려있는 Dialog 닫기 및 TextWatcher 제거
    // 일부 private 오브젝트에 종속된 오브젝트들만 따로 null
    @Override
    public void onDestroy() {
		if(ClockTextDialog != null) {
		    if(ClockTextTWatcher != null && ClockTextLayout != null) {
                ((EditText) ClockTextLayout.findViewById(R.id.edittext_clocktext))
                        .removeTextChangedListener(ClockTextTWatcher);
            }
            if(ClockTextDialog.isShowing())
                ClockTextDialog.dismiss();
		}

        if(ClockTextNotFSDialog != null) {
            if(ClockTextNotFSTWatcher != null && ClockTextNotFSLayout != null) {
                ((EditText) ClockTextNotFSLayout.findViewById(R.id.edittext_clocktext))
                        .removeTextChangedListener(ClockTextNotFSTWatcher);
            }
            if(ClockTextNotFSDialog.isShowing())
                ClockTextNotFSDialog.dismiss();
        }

        if(FontSizeDialog != null && FontSizeDialog.isShowing()) {
            FontSizeDialog.dismiss();
        }

        if(FontShadowShapeDialog != null && FontShadowShapeDialog.isShowing()) {
            FontShadowShapeDialog.dismiss();
        }

        if(ClockTransparencyDialog != null) {
            if(ClockTransparencyTWatcher != null && ClockTransparencyLayout != null) {
                ((EditText) ClockTransparencyLayout.findViewById(R.id.edittext_clocktransparency))
                        .removeTextChangedListener(ClockTransparencyTWatcher);
            }
            if(ClockTransparencyDialog.isShowing())
                ClockTransparencyDialog.dismiss();
        }

        if(HideTheClockTimeDialog != null && HideTheClockTimeDialog.isShowing()) {
            HideTheClockTimeDialog.dismiss();
        }

		if(ResetSettingsDialog != null && ResetSettingsDialog.isShowing()) {
            ResetSettingsDialog.dismiss();
		}

        super.onDestroy();
    }

    // 일부 설정 값들의 기본 값이나 최대/최소 값은 언어에 따른 영향이 X
    // -> strings.xml에 넣지 않음
    @SuppressLint("SetTextI18n")
    @Override
    public boolean onPreferenceClick(Preference preference) {
        // 클릭된 설정 값 저장.
		String prefKey = preference.getKey();

        // 각 설정에 따라 알맞는 Dialog 생성 및 표시
        // 한번 생성한 Dialog는 계속 재활용
        if(prefKey.equals(mCon.getString(R.string.pref_clockText_key_string))) {
            // Dialog를 한번도 생성하지 않은 경우
			if(ClockTextDialog == null) {
                // 다이얼로그 내 들어갈 레이아웃 로드
				if(ClockTextLayout == null) {
				    try {
                        ClockTextLayout = ((LayoutInflater) mCon.getSystemService(Context.LAYOUT_INFLATER_SERVICE))
                                .inflate(R.layout.pref_dialog_clocktext, (ViewGroup) findViewById(R.id.layout_clocktext));
                    } catch(NullPointerException e) {
				        return false;
                    }
				}
		        
                // 레이아웃 내 시계 구성요소 목록을 표시하는 TextView 내용 업데이트
                ((TextView) ClockTextLayout.findViewById(R.id.textview_clocktext_components))
                        .setText(R.string.pref_dialog_clocktext_components);
                // 레이아웃에서 사용되는 edittext를 가져오고, NO_SUGGESTIONS 플래그 설정.
                etClockText = ClockTextLayout.findViewById(R.id.edittext_clocktext);
                etClockText.setInputType(InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);

                // 텍스트 변경 리스너
                // 시계 구조 문자열 변경시 미리보기를 실시간으로 출력하는 역할
                if(ClockTextTWatcher == null) {
                    if(ClockTextPreviewBuilder == null) ClockTextPreviewBuilder = new StringBuilder();
                    tvClockTextPreview = ClockTextLayout.findViewById(R.id.textview_clocktext_preview);
                    svClockTextPreview = ClockTextLayout.findViewById(R.id.scrollview_preview);

                    ClockTextTWatcher = new TextWatcher() {
                        @Override
                        public void afterTextChanged(Editable arg0) {}
                        @Override
                        public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                        @Override
                        public void onTextChanged(CharSequence s, int start, int before, int count) {
                            byte modified_line;
                            int height;
                            if (s.length() >= 0) {
                                // (전체)입력한 값에 대한 미리보기를 만들고 출력
                                // 미리보기를 만드는 도중 현재 입력중인 위치가 몇번째 라인인지 계산
                                modified_line = refreshPreview(s, start + count, ClockTextPreviewBuilder);
                                tvClockTextPreview.setText(ClockTextPreviewBuilder.toString());
                                ClockTextPreviewBuilder.setLength(0);
                                // 첫 실행시 뷰가 화면에 표시가 안된 상태여서 높이 및 라인 카운트가 0으로 반환
                                // -> 이 경우 높이 값을 0으로
                                try {
                                    height = tvClockTextPreview.getHeight() / tvClockTextPreview.getLineCount();
                                } catch (ArithmeticException ae) {
                                    height = 0;
                                }
                                // 라인 당 높이 값에 현재 입력중인 라인 위치 값을 곱하여
                                // 프리뷰를 현재 입력중인 라인 위치로 스크롤
                                svClockTextPreview.smoothScrollTo(0, height * modified_line);
                            }
                        }
                    };
                }
                etClockText.addTextChangedListener(ClockTextTWatcher);

                // 시계내용 설정 기존 값을 미리 저장 + TextWatcher 한번 여기서 작동되어 프리뷰 미리 작성
                etClockText.setText(mPref.getString(mCon.getString(R.string.pref_clockText_key_string), mCon.getString(R.string.pref_clockText_default_string)));
                etClockText.setSelection(etClockText.length());

                // 다이얼로그 빌드
                ClockTextDialog = new AlertDialog.Builder(this)
                    .setTitle(R.string.pref_clocktext_title)
                    .setView(ClockTextLayout)
                    .setPositiveButton(android.R.string.ok, null)
                    .setNegativeButton(android.R.string.cancel, null)
                    // 기본값 복원 버튼
                    .setNeutralButton(R.string.pref_dialog_btn_default, null)
                    .create();

                ClockTextDialog.show();
                // 기본값 복원 버튼(택스트를 기본값으로 변경)
                ClockTextDialog.getButton(DialogInterface.BUTTON_NEUTRAL).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        etClockText.setText(mCon.getString(R.string.pref_clockText_default_string));
                        etClockText.setSelection(etClockText.length());
                    }
                });
                // 확인 버튼
                ClockTextDialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        String strClockText = etClockText.getText().toString();

                        // 빈값인 상태로 OK 누른경우 기존 설정값을 불러오고 다이얼로그를 닫지 않음
                        if (strClockText.equals("")) {
                            etClockText.setText(mPref.getString(mCon.getString(R.string.pref_clockText_key_string), mCon.getString(R.string.pref_clockText_default_string)));
                            etClockText.setSelection(etClockText.length());
                            return;
                        }

                        // 시계 구조 값 및 시계 구조를 미리 DateTimeFormatter에 알맞은 포맷으로
                        // 변환해서 저장하고, 가능한 최대 길이의 문자열도 미리 구해서 저장한다.
                        mPref.edit()
                                .putString(mCon.getString(R.string.pref_clockText_key_string), strClockText)
                                .putString(mCon.getString(R.string.pref_clockTextFormatted_key_string),
                                        getClockTextFormatted(strClockText))
                                .putString(mCon.getString(R.string.pref_clockTextMax_key_string),
                                        getClockTextMax(strClockText, mPref.getBoolean(mCon.getString(R.string.pref_english_key_string), true))).apply();

                        // 변경 내역 적용을 위해 서비스 재시작
                        runService(2, true);
                        ClockTextDialog.dismiss();
                    }
                });
			}
            // Dialog가 위에서 만들어 졌거나 이미 있고 표시된 상태가 아닐경우 표시
			if(!ClockTextDialog.isShowing())
				ClockTextDialog.show();
        } else if(prefKey.equals(mCon.getString(R.string.pref_clockText_notfs_key_string))) {
            if(ClockTextNotFSDialog == null) {
                // 다이얼로그 내 들어갈 레이아웃 로드
                if(ClockTextNotFSLayout == null) {
                    try {
                        ClockTextNotFSLayout = ((LayoutInflater) mCon.getSystemService(Context.LAYOUT_INFLATER_SERVICE))
                                .inflate(R.layout.pref_dialog_clocktext, (ViewGroup) findViewById(R.id.layout_clocktext));
                    } catch(NullPointerException e) {
                        return false;
                    }
                }

                // 레이아웃 내 시계 구성요소 목록을 표시하는 TextView 내용 업데이트
                ((TextView) ClockTextNotFSLayout.findViewById(R.id.textview_clocktext_components))
                        .setText(R.string.pref_dialog_clocktext_components);
                // 레이아웃에서 사용되는 edittext를 가져오고, NO_SUGGESTIONS 플래그 설정.
                etClockTextNotFS = ClockTextNotFSLayout.findViewById(R.id.edittext_clocktext);
                etClockTextNotFS.setInputType(InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);

                // 텍스트 변경 리스너
                // 시계 구조 문자열 변경시 미리보기를 실시간으로 출력하는 역할
                if(ClockTextNotFSTWatcher == null) {
                    if(ClockTextPreviewBuilder == null) ClockTextPreviewBuilder = new StringBuilder();
                    tvClockTextNotFSPreview = ClockTextNotFSLayout.findViewById(R.id.textview_clocktext_preview);
                    svClockTextNotFSPreview = ClockTextNotFSLayout.findViewById(R.id.scrollview_preview);

                    ClockTextNotFSTWatcher = new TextWatcher() {
                        @Override
                        public void afterTextChanged(Editable arg0) {}
                        @Override
                        public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                        @Override
                        public void onTextChanged(CharSequence s, int start, int before, int count) {
                            byte modified_line;
                            int height;
                            if (s.length() >= 0) {
                                // (전체)입력한 값에 대한 미리보기를 만들고 출력
                                // 미리보기를 만드는 도중 현재 입력중인 위치가 몇번째 라인인지 계산
                                modified_line = refreshPreview(s, start + count, ClockTextPreviewBuilder);
                                tvClockTextNotFSPreview.setText(ClockTextPreviewBuilder.toString());
                                ClockTextPreviewBuilder.setLength(0);
                                // 첫 실행시 뷰가 화면에 표시가 안된 상태여서 높이 및 라인 카운트가 0으로 반환
                                // -> 이 경우 높이 값을 0으로
                                try {
                                    height = tvClockTextNotFSPreview.getHeight() / tvClockTextNotFSPreview.getLineCount();
                                } catch (ArithmeticException ae) {
                                    height = 0;
                                }
                                // 라인 당 높이 값에 현재 입력중인 라인 위치 값을 곱하여
                                // 프리뷰를 현재 입력중인 라인 위치로 스크롤
                                svClockTextNotFSPreview.smoothScrollTo(0, height * modified_line);
                            }
                        }
                    };
                }
                etClockTextNotFS.addTextChangedListener(ClockTextNotFSTWatcher);

                // 시계내용 설정 기존 값을 미리 저장 + TextWatcher 한번 여기서 작동되어 프리뷰 미리 작성
                etClockTextNotFS.setText(mPref.getString(mCon.getString(R.string.pref_clockText_notfs_key_string), mCon.getString(R.string.pref_clockText_default_string)));
                etClockTextNotFS.setSelection(etClockTextNotFS.length());

                // 다이얼로그 빌드
                ClockTextNotFSDialog = new AlertDialog.Builder(this)
                        .setTitle(R.string.pref_clocktext_title)
                        .setView(ClockTextNotFSLayout)
                        .setPositiveButton(android.R.string.ok,null)
                        .setNegativeButton(android.R.string.cancel, null)
                        // 기본값 복원 버튼
                        .setNeutralButton(R.string.pref_dialog_btn_default, null)
                        .create();

                ClockTextNotFSDialog.show();
                // 기본값 복원 버튼(택스트를 기본값으로 변경)
                ClockTextNotFSDialog.getButton(DialogInterface.BUTTON_NEUTRAL).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        etClockTextNotFS.setText(mCon.getString(R.string.pref_clockText_default_string));
                        etClockTextNotFS.setSelection(etClockTextNotFS.length());
                    }
                });
                // 확인 버튼
                ClockTextNotFSDialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        String strClockText = etClockTextNotFS.getText().toString();

                        // 빈값인 상태로 OK 누른경우 기존 설정값을 불러오고 다이얼로그를 닫지 않음
                        if (strClockText.equals("")) {
                            etClockTextNotFS.setText(mPref.getString(mCon.getString(R.string.pref_clockText_notfs_key_string), mCon.getString(R.string.pref_clockText_default_string)));
                            etClockTextNotFS.setSelection(etClockTextNotFS.length());
                            return;
                        }

                        // 시계 구조 값 및 시계 구조를 미리 DateTimeFormatter에 알맞은 포맷으로
                        // 변환해서 저장하고, 가능한 최대 길이의 문자열도 미리 구해서 저장한다.
                        mPref.edit()
                                .putString(mCon.getString(R.string.pref_clockText_notfs_key_string), strClockText)
                                .putString(mCon.getString(R.string.pref_clockTextFormatted_notfs_key_string),
                                        getClockTextFormatted(strClockText))
                                .putString(mCon.getString(R.string.pref_clockTextMax_notfs_key_string),
                                        getClockTextMax(strClockText, mPref.getBoolean(mCon.getString(R.string.pref_english_key_string), true))).apply();

                        // 변경 내역 적용을 위해 서비스 재시작
                        runService(2, true);
                        ClockTextNotFSDialog.dismiss();
                    }
                });
            }
            if(!ClockTextNotFSDialog.isShowing())
                ClockTextNotFSDialog.show();
        } else if(prefKey.equals(mCon.getString(R.string.pref_fontSize_key_string))) {
			if(FontSizeDialog == null) {
                // 다이얼로그 내 들어갈 레이아웃 로드
                if (FontSizeLayout == null) {
                    try {
                        FontSizeLayout = ((LayoutInflater) mCon.getSystemService(Context.LAYOUT_INFLATER_SERVICE))
                                .inflate(R.layout.pref_dialog_fontsize, (ViewGroup) findViewById(R.id.layout_fontsize));
                    } catch (NullPointerException e) {
                        return false;
                    }
                }
                // 레이아웃에서 설정 값을 가지는 EditText를 불러와 내용을 기존 설정 값으로 변경
                etFontSize = FontSizeLayout.findViewById(R.id.edittext_fontsize);
                etFontSize.setText(String.format(Locale.US, "%d", mPref.getInt(mCon.getString(R.string.pref_fontSize_key_string), 18)));
                etFontSize.setSelection(2);

                // 다이얼로그 빌드
                FontSizeDialog = new AlertDialog.Builder(this)
                        .setTitle(R.string.pref_dialog_fontsize_title)
                        .setView(FontSizeLayout)
                        .setPositiveButton(android.R.string.ok, null)
                        .setNegativeButton(android.R.string.cancel, null)
                        // 기본값 복원 버튼
                        .setNeutralButton(R.string.pref_dialog_btn_default, null)
                        .create();

                FontSizeDialog.show();
                // 기본값 복원 버튼(택스트를 기본값으로 변경만 함)
                FontSizeDialog.getButton(DialogInterface.BUTTON_NEUTRAL).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        etFontSize.setText("18");
                        etFontSize.setSelection(2);
                    }
                });
                // 확인 버튼
                FontSizeDialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        int valInt;
                        // 한개라도 값이 수정되었다면 수정된 값을 사용자가 확인 할 수 있도록 바로 저장 안하고 다이얼로그도 닫지 않음.
                        try {
                            valInt = Integer.parseInt(etFontSize.getText().toString());
                        } catch (NumberFormatException e) {
                            // 빈값이 입력된경우 기존 값으로 복구
                            etFontSize.setText(String.format(Locale.US, "%d", mPref.getInt(mCon.getString(R.string.pref_fontSize_key_string), 18)));
                            etFontSize.setSelection(2);
                            return;
                        }
                        // 최소값 미만, 최대값 초과시 최소, 최대값으로 변경
                        if (valInt > 45) {
                            etFontSize.setText("45");
                            etFontSize.setSelection(2);
                            return;
                        }
                        else if (valInt < 10) {
                            etFontSize.setText("10");
                            etFontSize.setSelection(2);
                            return;
                        }

                        // 변경된 값(valInt)을 문자열로 변환하여 저장
                        mPref.edit().putInt(mCon.getString(R.string.pref_fontSize_key_string), valInt).apply();
                        // 변경 내역 적용을 위해 서비스 재시작
                        runService(2, true);
                        // 변경된 값을 설정에 표기
                        findPreference(mCon.getString(R.string.pref_fontSize_key_string)).setSummary(mCon.getString(R.string.pref_prefix_current) + Integer.toString(valInt));
                        FontSizeDialog.dismiss();
                    }
                });
            }
            if(!FontSizeDialog.isShowing())
                FontSizeDialog.show();
        } else if(prefKey.equals(mCon.getString(R.string.pref_fontShadowShape_key_string))) {
            if(FontShadowShapeDialog == null) {
                // 다이얼로그 내 들어갈 레이아웃 로드
                if (FontShadowShapeLayout == null) {
                    try {
                        FontShadowShapeLayout = ((LayoutInflater) mCon.getSystemService(Context.LAYOUT_INFLATER_SERVICE))
                                .inflate(R.layout.pref_dialog_fontshadowshape, (ViewGroup) findViewById(R.id.layout_fontshadowshape));
                    } catch (NullPointerException e) {
                        return false;
                    }
                }

                // 레이아웃에서 설정 값을 가지는 EditText들을 불러와 내용을 기존 설정 값으로 변경
                etFontShadowDx = FontShadowShapeLayout.findViewById(R.id.edittext_shadowdx);
                etFontShadowDy = FontShadowShapeLayout.findViewById(R.id.edittext_shadowdy);
                etFontShadowRadius = FontShadowShapeLayout.findViewById(R.id.edittext_shadowradius);

                etFontShadowDx.setText(String.format(Locale.US, "%.0f", mPref.getFloat(mCon.getString(R.string.pref_fontShadowShape_dx_key_string), 2.0f)));
                etFontShadowDx.setSelection(etFontShadowDx.length());
                etFontShadowDy.setText(String.format(Locale.US, "%.0f", mPref.getFloat(mCon.getString(R.string.pref_fontShadowShape_dy_key_string), 2.0f)));
                etFontShadowDy.setSelection(etFontShadowDy.length());
                etFontShadowRadius.setText(String.format(Locale.US, "%.0f", mPref.getFloat(mCon.getString(R.string.pref_fontShadowShape_radius_key_string), 3.0f)));
                etFontShadowRadius.setSelection(etFontShadowRadius.length());

                // 다이얼로그 빌드
                FontShadowShapeDialog = new AlertDialog.Builder(this)
                        .setTitle(R.string.pref_dialog_fontshadowshape_title)
                        .setView(FontShadowShapeLayout)
                        .setPositiveButton(android.R.string.ok, null)
                        .setNegativeButton(android.R.string.cancel, null)
                        // 기본값 복원 버튼
                        .setNeutralButton(R.string.pref_dialog_btn_default, null)
                        .create();

                FontShadowShapeDialog.show();
                // 기본값 복원 버튼(택스트를 기본값으로 변경)
                FontShadowShapeDialog.getButton(DialogInterface.BUTTON_NEUTRAL).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        etFontShadowDx.setText("2");
                        etFontShadowDx.setSelection(1);
                        etFontShadowDy.setText("2");
                        etFontShadowDy.setSelection(1);
                        etFontShadowRadius.setText("3");
                        etFontShadowRadius.setSelection(1);
                    }
                });
                // 확인 버튼
                FontShadowShapeDialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        float valDx = 2.0f;
                        float valDy = 2.0f;
                        float valRadius = 3.0f;
                        int valueFixed = 0;
                        // Dx
                        try {
                            valDx = Float.parseFloat(etFontShadowDx.getText().toString());

                            // 최소값 미만, 최대값 초과시 입력란에 입력된 값을 최소, 최대값으로 변경.
                            if (valDx > 10.0f) {
                                etFontShadowDx.setText(String.format(Locale.US, "%.0f", 10.0f));
                                etFontShadowDx.setSelection(2);
                                valueFixed++;
                            } else if (valDx < -10.0f) {
                                etFontShadowDx.setText(String.format(Locale.US, "%.0f", -10.0f));
                                etFontShadowDx.setSelection(3);
                                valueFixed++;
                            }
                        }
                        // 빈값이 입력된경우 기존 값으로 복구
                        catch (NumberFormatException e) {
                            etFontShadowDx.setText(String.format(Locale.US, "%.0f", mPref.getFloat(mCon.getString(R.string.pref_fontShadowShape_dx_key_string), 2.0f)));
                            etFontShadowDx.setSelection(etFontShadowDx.length());
                            valueFixed++;
                        }

                        // Dy - Dx 와 동일한 구조
                        try {
                            valDy = Float.parseFloat(etFontShadowDy.getText().toString());

                            if (valDy > 10.0f) {
                                etFontShadowDy.setText(String.format(Locale.US, "%.0f", 10.0f));
                                etFontShadowDy.setSelection(2);
                                valueFixed++;
                            } else if (valDy < -10.0f) {
                                etFontShadowDy.setText(String.format(Locale.US, "%.0f", -10.0f));
                                etFontShadowDy.setSelection(3);
                                valueFixed++;
                            }
                        }
                        catch (NumberFormatException e) {
                            etFontShadowDy.setText(String.format(Locale.US, "%.0f", mPref.getFloat(mCon.getString(R.string.pref_fontShadowShape_dy_key_string), 2.0f)));
                            etFontShadowDy.setSelection(etFontShadowDy.length());
                            valueFixed++;
                        }

                        // Radius - Dx 와 동일한 구조
                        try {
                            valRadius = Float.parseFloat(etFontShadowRadius.getText().toString());

                            if (valRadius > 20.0f) {
                                etFontShadowRadius.setText(String.format(Locale.US, "%.0f", 20.0f));
                                etFontShadowRadius.setSelection(2);
                                valueFixed++;
                            } else if (valRadius < 1.0f) {
                                etFontShadowRadius.setText(String.format(Locale.US, "%.0f", 1.0f));
                                etFontShadowRadius.setSelection(1);
                                valueFixed++;
                            }
                        } catch (NumberFormatException e) {
                            etFontShadowRadius.setText(String.format(Locale.US, "%.0f", mPref.getFloat(mCon.getString(R.string.pref_fontShadowShape_radius_key_string), 3.0f)));
                            etFontShadowRadius.setSelection(etFontShadowRadius.length());
                            valueFixed++;
                        }
                        // 한개라도 값이 수정되었다면 수정된 값을 사용자가 확인 할 수 있도록 바로 저장 안하고 다이얼로그도 닫지 않음.
                        if(valueFixed > 0) return;

                        // 변경된 값들을 문자열로 변환하여 저장
                        mPref.edit()
                                .putFloat(mCon.getString(R.string.pref_fontShadowShape_dx_key_string), valDx)
                                .putFloat(mCon.getString(R.string.pref_fontShadowShape_dy_key_string), valDy)
                                .putFloat(mCon.getString(R.string.pref_fontShadowShape_radius_key_string), valRadius).apply();

                        // 변경된 값을 설정에 표기
                        findPreference(mCon.getString(R.string.pref_fontShadowShape_key_string)).setSummary(mCon.getString(R.string.pref_prefix_current) + String.format(Locale.US, "%.0f", mPref.getFloat(mCon.getString(R.string.pref_fontShadowShape_dx_key_string), 2.0f)) + "x " +
                                String.format(Locale.US, "%.0f", mPref.getFloat(mCon.getString(R.string.pref_fontShadowShape_dy_key_string), 2.0f)) + "y " +
                                String.format(Locale.US, "%.0f", mPref.getFloat(mCon.getString(R.string.pref_fontShadowShape_radius_key_string), 3.0f)) + "r");

                        // 변경 내역 적용을 위해 서비스 재시작
                        runService(2, true);

                        FontShadowShapeDialog.dismiss();
                    }
                });
            }
            if(!FontShadowShapeDialog.isShowing())
                FontShadowShapeDialog.show();
        } else if(prefKey.equals(mCon.getString(R.string.pref_clockTransparency_key_string))) {
            if(ClockTransparencyDialog == null) {
                // 다이얼로그 내 들어갈 레이아웃 로드
                if (ClockTransparencyLayout == null) {
                    try {
                        ClockTransparencyLayout = ((LayoutInflater) mCon.getSystemService(Context.LAYOUT_INFLATER_SERVICE))
                                .inflate(R.layout.pref_dialog_clocktransparency, (ViewGroup) findViewById(R.id.layout_clocktransparency));
                    } catch (NullPointerException e) {
                        return false;
                    }
                }
                // 레이아웃에서 사용되는 edittext 및 SeekBar를 가져옴.
                etClockTransparency = ClockTransparencyLayout.findViewById(R.id.edittext_clocktransparency);
                seekClockTransparency = ClockTransparencyLayout.findViewById(R.id.seekbar_clocktransparency);
                // 텍스트 변경 리스너
                // 투명도 값을 EditText에서 직접 입력할 때 입력 값에 따라 SeekBar 업데이트
                if(ClockTransparencyTWatcher == null) {
                    ClockTransparencyTWatcher = new TextWatcher() {
                        @Override
                        public void afterTextChanged(Editable arg0) {}
                        @Override
                        public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                        @Override
                        public void onTextChanged(CharSequence s, int start, int before, int count) {
                            // 사용자가 EditText를 직접 수정하는 경우에만 SeekBar를 업데이트.
                            // SeekBar를 조절하는 경우 setOnSeekBarChangeListener에서 EditText도
                            // 업데이트 한다. 그러나 EditText를 업데이트 하기전에
                            // ClockTransparencyEditTextFromUser 값을 false로 하고 업데이트 후 true로 하여,
                            // 텍스트 변경을 감지하더라도 SeekBar를 한번 더 업데이트 하지는 않음
                            if(ClockTransparencyEditTextFromUser) {
                                int newValue;
                                try {
                                    newValue = Integer.parseInt(s.toString());
                                    if(newValue > 255) {
                                        newValue = 255;
                                    }
                                }
                                catch (NumberFormatException e) {
                                    newValue = 0;
                                }
                                seekClockTransparency.setProgress(newValue);
                            }
                        }
                    };
                }
                etClockTransparency.addTextChangedListener(ClockTransparencyTWatcher);
                // edittext 내용을 기존 설정값으로 변경 -> TextWatcher로 인해 SeekBar도 초기 값으로 변경.
                etClockTransparency.setText(String.format(Locale.US, "%d", mPref.getInt(mCon.getString(R.string.pref_clockTransparency_key_string), 255)));
                etClockTransparency.setSelection(etClockTransparency.length());

                // SeekBar 값이 변경될 때 마다 EditText의 내용도 연동해서 변경
                seekClockTransparency.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {
                    }
                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {
                    }
                    @Override
                    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                        // 사용자가 조절한 경우에만 EditText를 같이 업데이트
                        if(fromUser) {
                            ClockTransparencyEditTextFromUser = false;
                            etClockTransparency.setText(String.format(Locale.US, "%d", progress));
                            etClockTransparency.setSelection(etClockTransparency.length());
                            ClockTransparencyEditTextFromUser = true;
                        }
                    }
                });

                // 다이얼로그 빌드
                ClockTransparencyDialog = new AlertDialog.Builder(this)
                        .setTitle(R.string.pref_dialog_clocktransparency_title)
                        .setView(ClockTransparencyLayout)
                        .setPositiveButton(android.R.string.ok, null)
                        .setNegativeButton(android.R.string.cancel, null)
                        // 기본값 복원 버튼
                        .setNeutralButton(R.string.pref_dialog_btn_default, null)
                        .create();

                ClockTransparencyDialog.show();
                // 기본값 복원 버튼(택스트를 기본값으로 변경만 함)
                ClockTransparencyDialog.getButton(DialogInterface.BUTTON_NEUTRAL).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        etClockTransparency.setText("255");
                        etClockTransparency.setSelection(3);
                    }
                });
                // 확인 버튼
                ClockTransparencyDialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        int valInt;
                        // 값이 수정되었다면 수정된 값을 사용자가 확인 할 수 있도록 바로 저장 안하고 다이얼로그도 닫지 않음.
                        try {
                            valInt = Integer.parseInt(etClockTransparency.getText().toString());
                        } catch (NumberFormatException e) {
                            // 빈값이 입력된경우 기존 값으로 복구하고 닫기
                            etClockTransparency.setText(String.format(Locale.US, "%d", mPref.getInt(mCon.getString(R.string.pref_clockTransparency_key_string), 255)));
                            etClockTransparency.setSelection(etClockTransparency.length());
                            return;
                        }
                        // 최대값 초과시 최대값으로 변경
                        if (valInt > 255) {
                            etClockTransparency.setText("255");
                            etClockTransparency.setSelection(3);
                            return;
                        }

                        // 변경된 값(valInt)을 문자열로 변환하여 저장
                        mPref.edit().putInt(mCon.getString(R.string.pref_clockTransparency_key_string), valInt).apply();
                        // 변경 내역 적용을 위해 서비스 재시작
                        runService(2, true);
                        // 변경된 값을 설정에 표기
                        findPreference(mCon.getString(R.string.pref_clockTransparency_key_string)).setSummary(mCon.getString(R.string.pref_prefix_current) + Integer.toString(valInt) + mCon.getString(R.string.pref_clocktransparency_current_postfix));
                        ClockTransparencyDialog.dismiss();
                    }
                });
            }
            if(!ClockTransparencyDialog.isShowing())
                ClockTransparencyDialog.show();
        } else if(prefKey.equals(mCon.getString(R.string.pref_hideTheClockTime_key_string))) {
            if(HideTheClockTimeDialog == null) {
                // 다이얼로그 내 들어갈 레이아웃 로드
                if (HideTheClockTimeLayout == null) {
                    try {
                        HideTheClockTimeLayout = ((LayoutInflater) mCon.getSystemService(Context.LAYOUT_INFLATER_SERVICE))
                                .inflate(R.layout.pref_dialog_hidetheclocktime, (ViewGroup) findViewById(R.id.layout_hidetheclocktime));
                    } catch(NullPointerException e) {
                        return false;
                    }
                }

                // 레이아웃에서 edittext 불러와 내용을 기존 설정값으로 변경
                etHideTheClockTime = HideTheClockTimeLayout.findViewById(R.id.edittext_hidetheclocktime);
                etHideTheClockTime.setText(String.format(Locale.US, "%d", mPref.getInt(mCon.getString(R.string.pref_hideTheClockTime_key_string), 10)));
                etHideTheClockTime.setSelection(etHideTheClockTime.length());

                // 다이얼로그 빌드
                HideTheClockTimeDialog = new AlertDialog.Builder(this)
                        .setTitle(R.string.pref_dialog_hidetheclocktime_title)
                        .setView(HideTheClockTimeLayout)
                        .setPositiveButton(android.R.string.ok, null)
                        .setNegativeButton(android.R.string.cancel, null)
                        // 기본값 복원 버튼
                        .setNeutralButton(R.string.pref_dialog_btn_default, null)
                        .create();

                HideTheClockTimeDialog.show();
                // 기본값 복원 버튼(택스트를 기본값으로 변경만 함)
                HideTheClockTimeDialog.getButton(DialogInterface.BUTTON_NEUTRAL).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        etHideTheClockTime.setText("10");
                        etHideTheClockTime.setSelection(2);
                    }
                });
                HideTheClockTimeDialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        int valInt;
                        // 한개라도 값이 수정되었다면 수정된 값을 사용자가 확인 할 수 있도록 바로 저장 안하고 다이얼로그도 닫지 않음.
                        try {
                            valInt = Integer.parseInt(etHideTheClockTime.getText().toString());
                        } catch (NumberFormatException e) {
                            // 빈값이 입력된경우 기존 값으로 복구하고 닫기
                            etHideTheClockTime.setText(String.format(Locale.US, "%d", mPref.getInt(mCon.getString(R.string.pref_hideTheClockTime_key_string), 10)));
                            etHideTheClockTime.setSelection(etHideTheClockTime.length());
                            return;
                        }
                        // 최소값 미만, 최대값 초과시 최소,최대값으로 변경
                        if (valInt > 600) {
                            etHideTheClockTime.setText("600");
                            etHideTheClockTime.setSelection(3);
                            return;
                        }
                        else if (valInt < 3) {
                            etHideTheClockTime.setText("3");
                            etHideTheClockTime.setSelection(1);
                            return;
                        }

                        // 변경된 값(valInt)을 문자열로 변환하여 저장
                        mPref.edit().putInt(mCon.getString(R.string.pref_hideTheClockTime_key_string), valInt).apply();
                        // 변경 내역 적용을 위해 서비스 재시작
                        runService(2, true);
                        // 변경된 값을 설정에 표기
                        findPreference(mCon.getString(R.string.pref_hideTheClockTime_key_string)).setSummary(mCon.getString(R.string.pref_prefix_current) + Integer.toString(valInt) + getString(R.string.pref_suffix_second));
                        HideTheClockTimeDialog.dismiss();
                    }
                });
            }
            if(!HideTheClockTimeDialog.isShowing())
                HideTheClockTimeDialog.show();
        } else if(prefKey.equals(mCon.getString(R.string.pref_resetSettings_key_string))) {
			if(ResetSettingsDialog == null) {
                ResetSettingsDialog = new AlertDialog.Builder(this)
                        .setPositiveButton(android.R.string.ok, null)
                        .setNegativeButton(android.R.string.cancel, null)
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .setMessage(R.string.pref_dialog_reset_summary)
                        .setTitle(R.string.pref_reset)
                        .create();

                ResetSettingsDialog.show();
                // 확인 버튼
                ResetSettingsDialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        // 서비스 종료
                        runService(0, false);
                        // 모든설정 초기화
                        mPref.edit().clear().apply();
                        ResetSettingsDialog.dismiss();
                        // 설정 초기화 토스트 띄우기
                        Toast.makeText(COSSettings.this, mCon.getString(R.string.pref_toast_reset_complete), Toast.LENGTH_SHORT).show();
                        // 메인 액티비티 실행
                        // FLAG_ACTIVITY_NEW_TASK 때문에 ApplicationContext에서도 실행
                        mCon.startActivity(new Intent(mCon, COSMain.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                    }
                });
			}
			if(!ResetSettingsDialog.isShowing())
				ResetSettingsDialog.show();
        }
        return false;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
		String prefKey = preference.getKey();

        if(prefKey.equals(mCon.getString(R.string.pref_clockPosition_key_string)) || prefKey.equals(mCon.getString(R.string.pref_clockPosition_notfs_key_string))) {
            // 아래 쪽으로 설정한 경우 패키지 설치 관리자(package installer)화면에서 설치 버튼이 안눌리는 현상이 존재. 이에 대한 경고를 띄움
            int newVal = Integer.parseInt((String)newValue);
            if(3 <= newVal) {
                Toast.makeText(COSSettings.this, R.string.pref_toast_clockposition_warning, Toast.LENGTH_LONG).show();
            }

            // 변경된 값을 설정에 표기
            String[] titles_clockposition = mCon.getResources().getStringArray(R.array.pref_clockposition_list);
            switch(newVal) {
                case 0: preference.setSummary(mCon.getString(R.string.pref_prefix_current) + titles_clockposition[0]); break;
                case 1: preference.setSummary(mCon.getString(R.string.pref_prefix_current) + titles_clockposition[1]); break;
                case 2: preference.setSummary(mCon.getString(R.string.pref_prefix_current) + titles_clockposition[2]); break;
                case 3: preference.setSummary(mCon.getString(R.string.pref_prefix_current) + titles_clockposition[3]); break;
                case 4: preference.setSummary(mCon.getString(R.string.pref_prefix_current) + titles_clockposition[4]); break;
                case 5: preference.setSummary(mCon.getString(R.string.pref_prefix_current) + titles_clockposition[5]); break;
            }

            // 변경 내역 적용을 위해 서비스 재시작
            runService(2, true);
            return true;
        } else if(prefKey.equals(mCon.getString(R.string.pref_fsmode_list_key_string))) {
            // 변경된 값을 설정에 표기 및 변경된 값에 따라 연동되는 설정들의 활성화 여부를 조절
            String[] titles_fsmode_list = mCon.getResources().getStringArray(R.array.pref_fsmode_list);
            switch(Integer.parseInt((String)newValue)) {
                case 0:
                    findPreference(mCon.getString(R.string.pref_fsmode_list_key_string)).setSummary(mCon.getString(R.string.pref_prefix_current) + titles_fsmode_list[0]);
                    findPreference(mCon.getString(R.string.pref_clockText_notfs_key_string)).setEnabled(false);
                    findPreference(mCon.getString(R.string.pref_clockPosition_notfs_key_string)).setEnabled(false);
                    break;
                case 1:
                    findPreference(mCon.getString(R.string.pref_fsmode_list_key_string)).setSummary(mCon.getString(R.string.pref_prefix_current) + titles_fsmode_list[1] + mCon.getString(R.string.pref_fsmode_summary_postfix));
                    findPreference(mCon.getString(R.string.pref_clockText_notfs_key_string)).setEnabled(false);
                    findPreference(mCon.getString(R.string.pref_clockPosition_notfs_key_string)).setEnabled(false);
                    break;
                case 2:
                    findPreference(mCon.getString(R.string.pref_fsmode_list_key_string)).setSummary(mCon.getString(R.string.pref_prefix_current) + titles_fsmode_list[2] + mCon.getString(R.string.pref_fsmode_summary_postfix));
                    findPreference(mCon.getString(R.string.pref_clockText_notfs_key_string)).setEnabled(true);
                    findPreference(mCon.getString(R.string.pref_clockPosition_notfs_key_string)).setEnabled(true);
                    break;
            }

            runService(2, true);
            return true;
        } else if(prefKey.equals(mCon.getString(R.string.pref_font_key_string))) {
            // 변경된 값을 설정에 표기
            String[] titles_font = mCon.getResources().getStringArray(R.array.pref_font_list);
            switch(Integer.parseInt((String)newValue)) {
                case 0: preference.setSummary(mCon.getString(R.string.pref_prefix_current) + titles_font[0]); break;
                case 1: preference.setSummary(mCon.getString(R.string.pref_prefix_current) + mCon.getString(R.string.pref_font_serif_string)); break;
                case 2: preference.setSummary(mCon.getString(R.string.pref_prefix_current) + mCon.getString(R.string.pref_font_monospace_string)); break;
                case 3: preference.setSummary(mCon.getString(R.string.pref_prefix_current) + mCon.getString(R.string.pref_font_sans_serif_string)); break;
            }

            runService(2, true);
            return true;
        } else if(prefKey.equals(mCon.getString(R.string.pref_fontAppearance_key_string))) {
            // 변경된 값을 설정에 표기
            String[] titles_fontappearance = mCon.getResources().getStringArray(R.array.pref_fontappearance_list);
            switch(Integer.parseInt((String)newValue)) {
                case 0: preference.setSummary(mCon.getString(R.string.pref_prefix_current) + titles_fontappearance[0]); break;
                case 1: preference.setSummary(mCon.getString(R.string.pref_prefix_current) + titles_fontappearance[1]); break;
                case 2: preference.setSummary(mCon.getString(R.string.pref_prefix_current) + titles_fontappearance[2]); break;
            }

            runService(2, true);
            return true;
        } else if(prefKey.equals(mCon.getString(R.string.pref_gradient_key_string))) {
            // 변경된 값을 설정에 표기 및 변경된 값에 따라 연동되는 설정들의 활성화 여부를 조절
            String[] titles_gradient = mCon.getResources().getStringArray(R.array.pref_gradient_list);
            int current = Integer.parseInt((String)newValue);
            switch(current) {
                case 0:
                    preference.setSummary(mCon.getString(R.string.pref_prefix_current) + titles_gradient[0]);
                    findPreference(mCon.getString(R.string.pref_gradientColor1_key_string)).setEnabled(false);
                    findPreference(mCon.getString(R.string.pref_gradientColor2_key_string)).setEnabled(false);
                    findPreference(mCon.getString(R.string.pref_gradientColor3_key_string)).setEnabled(false);
                    findPreference(mCon.getString(R.string.pref_fontColor_key_string)).setEnabled(true);
                    break;
                case 1:
                    preference.setSummary(mCon.getString(R.string.pref_prefix_current) + titles_gradient[1]);
                    findPreference(mCon.getString(R.string.pref_gradientColor1_key_string)).setEnabled(true);
                    findPreference(mCon.getString(R.string.pref_gradientColor2_key_string)).setEnabled(true);
                    findPreference(mCon.getString(R.string.pref_gradientColor3_key_string)).setEnabled(false);
                    findPreference(mCon.getString(R.string.pref_fontColor_key_string)).setEnabled(false);
                    break;
                case 2:
                    preference.setSummary(mCon.getString(R.string.pref_prefix_current) + titles_gradient[2]);
                    findPreference(mCon.getString(R.string.pref_gradientColor1_key_string)).setEnabled(true);
                    findPreference(mCon.getString(R.string.pref_gradientColor2_key_string)).setEnabled(true);
                    findPreference(mCon.getString(R.string.pref_gradientColor3_key_string)).setEnabled(true);
                    findPreference(mCon.getString(R.string.pref_fontColor_key_string)).setEnabled(false);
                    break;
            }

            runService(2, true);
            return true;
        } else if(prefKey.equals(mCon.getString(R.string.pref_english_key_string))) {
            // 변경된 값에 따른 언어가 적용 되었다는 토스트 알림
            if((boolean)newValue) {
                Toast.makeText(COSSettings.this, R.string.pref_toast_english_applied, Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(COSSettings.this, R.string.pref_toast_korean_applied, Toast.LENGTH_SHORT).show();
            }

            // 바뀐 언어에 맞는 현재 시계 구조로 가능한 최대 크기의 문자열 저장
            mPref.edit()
                    .putString(mCon.getString(R.string.pref_clockTextMax_key_string),
                            getClockTextMax(mPref.getString(mCon.getString(R.string.pref_clockText_key_string), mCon.getString(R.string.pref_clockText_default_string)), (boolean)newValue))
                    .putString(mCon.getString(R.string.pref_clockTextMax_notfs_key_string),
                            getClockTextMax(mPref.getString(mCon.getString(R.string.pref_clockText_notfs_key_string), mCon.getString(R.string.pref_clockText_default_string)), (boolean)newValue)).apply();

            // 지금까지의 액티비티 모두 제거 및 메인 액티비티 실행
            // FLAG_ACTIVITY_NEW_TASK 때문에 ApplicationContext에서도 실행
            mCon.startActivity(new Intent(mCon, COSMain.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            return true;
        } else if(prefKey.equals(mCon.getString(R.string.pref_fontColor_key_string)) || prefKey.equals(mCon.getString(R.string.pref_fontShadowColor_key_string)) || prefKey.equals(mCon.getString(R.string.pref_fontShadow_key_string)) ||
                prefKey.equals(mCon.getString(R.string.pref_hideTheClock_key_string)) || prefKey.equals(mCon.getString(R.string.pref_background_key_string)) || prefKey.equals(mCon.getString(R.string.pref_backgroundColor_key_string)) ||
                prefKey.equals(mCon.getString(R.string.pref_gradientColor1_key_string)) || prefKey.equals(mCon.getString(R.string.pref_gradientColor2_key_string)) || prefKey.equals(mCon.getString(R.string.pref_gradientColor3_key_string)) ||
                prefKey.equals(mCon.getString(R.string.pref_longTouchToHide_key_string)) || prefKey.equals(mCon.getString(R.string.pref_fixNotRefresh_key_string))) {
            // 기타 서비스 재시작만 필요한 설정이 변동된 경우
            // 변경 내역 적용을 위해 서비스 재시작
            runService(2, true);
            return true;
        }
        return false;
    }
}
