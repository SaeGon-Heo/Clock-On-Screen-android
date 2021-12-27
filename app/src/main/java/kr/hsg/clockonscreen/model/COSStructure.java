/*
 * Copyright 2021 SaeGon Heo
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
package kr.hsg.clockonscreen.model;

import android.content.Context;

import kr.hsg.clockonscreen.R;
import kr.hsg.clockonscreen.config.COSPref;

public class COSStructure {
    private COSPref cosPref;

    public COSStructure(final Context ctx) {
        super();
        if (ctx == null) {
            throw new RuntimeException("Context is null!");
        }
        this.cosPref = new COSPref(ctx);
    }

    // 시계 구조 값 및 시계 구조를 미리 DateTimeFormatter에 알맞은 포맷으로
    // 변환해서 저장하고, 가능한 최대 길이의 문자열도 미리 구해서 저장한다.
    public void updateClockText(final String clockText) {
        mPref.edit()
                .putString(appCtx.getString(R.string.pref_clockText_key_string), strClockText)
                .putString(appCtx.getString(R.string.pref_clockTextFormatted_key_string),
                        getClockTextFormatted(strClockText))
                .putString(appCtx.getString(R.string.pref_clockTextMax_key_string),
                        getClockTextMax(strClockText, mPref.getBoolean(appCtx.getString(R.string.pref_english_key_string), true))).apply();
    }







    // make Preview and get Modified Line
    // return Modified Line
    // byte를 반환하므로 다른 곳에서 사용 시 주의
    // 여기서는 최대 80줄을 초과 할 수 없으므로 byte 사용
    // * 프로젝트 경로의 COS_structure.txt 파일 참조
    public int makePreview(CharSequence data, int modifiedEnd, StringBuilder SB) {
        char[] array = data.toString().toCharArray();
        boolean mEnglish = cosPref.isEnglish();

        // for loop
        byte i = 0;
        byte len = (byte)(array.length - 1);
        // 현재 수정중인 위치의 라인 값 저장
        byte linecnt = 0;
        // 변환작업
        while (i < len) {
            if (array[i] == '.') {
                switch (array[i + 1]) {
                    case 'z':
                        SB.append('\n');
                        if (i < modifiedEnd)
                            linecnt++;
                        i++;
                        break;
                    case 'a':
                        SB.append("2020");
                        i++;
                        break;
                    case 'b':
                        SB.append("20");
                        i++;
                        break;
                    case 'c':
                    case 'e':
                    case 'i':
                    case 'j':
                    case 'o':
                    case 's':
                    case 'u':
                        SB.append('1');
                        i++;
                        break;
                    case 'd':
                        SB.append("Q1");
                        i++;
                        break;
                    case 'f':
                    case 'k':
                    case 'p':
                    case 't':
                    case 'v':
                        SB.append("01");
                        i++;
                        break;
                    case 'g':
                        if (mEnglish)
                            SB.append("Jan");
                        else
                            SB.append("1월");
                        i++;
                        break;
                    case 'h':
                        if (mEnglish)
                            SB.append("January");
                        else
                            SB.append("1월");
                        i++;
                        break;
                    case 'l':
                        if (mEnglish)
                            SB.append("Mon");
                        else
                            SB.append("월");
                        i++;
                        break;
                    case 'm':
                        if (mEnglish)
                            SB.append("Monday");
                        else
                            SB.append("월요일");
                        i++;
                        break;
                    case 'n':
                        if (mEnglish)
                            SB.append("PM");
                        else
                            SB.append("오후");
                        i++;
                        break;
                    case 'q':
                    case 'r':
                        SB.append("13");
                        i++;
                        break;
                    case 'w':
                        SB.append("50%▲");
                        i++;
                        break;
                    case 'W':
                        SB.append("4000");
                        i++;
                        break;
                    case 'x':
                        SB.append('⇵');
                        i++;
                        break;
                    case 'X':
                        SB.append('M');
                        i++;
                        break;
                    case '.':
                        SB.append(".");
                        i++;
                        break;
                }
            }
            // .와 다음줄 문자를 제외한 값을 처리
            else if (array[i] != '\n' && array[i] != '\r') {
                SB.append(array[i]);
            }
            i++;
        }
        // 마지막 문자 1개가 남았을 시 넣음
        if (i < len + 1 && array[i] != '.' && array[i] != '\n' && array[i] != '\r') {
            SB.append(array[i]);
        }

        return linecnt;
    }

    // 시계 구조 설정 값을 DateTimeFormatter에 알맞은 포맷으로 변환
    // * 프로젝트 경로의 COS_structure.txt 파일 참조
    public static String getClockTextFormatted(String data) {
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
        while (i < len) {
            if (array[i] == '.') {
                switch (array[i + 1]) {
                    case 'z':
                        if (state != 0) {
                            SB.append('\'');
                            state = 0;
                        }
                        SB.append('\n');
                        i++;
                        break;
                    case 'a':
                        // 1.문자열을 쓰다가 구성요소로 오면 '(문자열 구분자) 추가
                        // 2.현재 구성요소와 같은 문자를 쓰는 구성요소를 이어서 쓰는 경우
                        // 사이에 Zero Width Space(유니코드 공식 문자) 추가
                        // 3.다른 구성요소에서 현재 구성요소를 추가한 경우
                        // 현재 구성요소 상태로 변환(이 밑으로 같은 형식으로 작성)
                        if (state == 0) {
                            SB.append('\'');
                            state = 1;
                        } else if (state == 1) {
                            SB.append('\u200B');
                        } else {
                            state = 1;
                        }
                        SB.append("uuuu"); // 년 (2020)
                        i++;
                        break;
                    case 'b':
                        if (state == 0) {
                            SB.append('\'');
                            state = 1;
                        } else if (state == 1) {
                            SB.append('\u200B');
                        } else {
                            state = 1;
                        }
                        SB.append("uu"); // 년 (20)
                        i++;
                        break;
                    case 'c':
                        if (state == 0) {
                            SB.append('\'');
                            state = 2;
                        } else if (state == 2) {
                            SB.append('\u200B');
                        } else {
                            state = 2;
                        }
                        SB.append('Q'); // 분기 (1 ~ 4)
                        i++;
                        break;
                    case 'd':
                        if (state == 0) {
                            SB.append('\'');
                            state = 2;
                        } else if (state == 2) {
                            SB.append('\u200B');
                        } else {
                            state = 2;
                        }
                        SB.append("QQQ"); // 분기 (Q1, Q2, Q3, Q4)
                        i++;
                        break;
                    case 'e':
                        if (state == 0) {
                            SB.append('\'');
                            state = 3;
                        } else if (state == 3) {
                            SB.append('\u200B');
                        } else {
                            state = 3;
                        }
                        SB.append('M'); // 월 (1 ~ 12)
                        i++;
                        break;
                    case 'f':
                        if (state == 0) {
                            SB.append('\'');
                            state = 3;
                        } else if (state == 3) {
                            SB.append('\u200B');
                        } else {
                            state = 3;
                        }
                        SB.append("MM"); // 월 (01 ~ 12)
                        i++;
                        break;
                    case 'g':
                        if (state == 0) {
                            SB.append('\'');
                            state = 3;
                        } else if (state == 3) {
                            SB.append('\u200B');
                        } else {
                            state = 3;
                        }
                        SB.append("MMM"); // 월 (Jan, Feb...) (1월, 2월...)
                        i++;
                        break;
                    case 'h':
                        if (state == 0) {
                            SB.append('\'');
                            state = 3;
                        } else if (state == 3) {
                            SB.append('\u200B');
                        } else {
                            state = 3;
                        }
                        SB.append("MMMM"); // 월 (January, February...) (1월, 2월...)
                        i++;
                        break;
                    case 'i':
                        if (state == 0) {
                            SB.append('\'');
                            state = 4;
                        } else if (state == 4) {
                            SB.append('\u200B');
                        } else {
                            state = 4;
                        }
                        SB.append('W'); // 주차 (1 ~ 5)
                        i++;
                        break;
                    case 'j':
                        if (state == 0) {
                            SB.append('\'');
                            state = 5;
                        } else if (state == 5) {
                            SB.append('\u200B');
                        } else {
                            state = 5;
                        }
                        SB.append('d'); // 일 (1 ~ 31)
                        i++;
                        break;
                    case 'k':
                        if (state == 0) {
                            SB.append('\'');
                            state = 5;
                        } else if (state == 5) {
                            SB.append('\u200B');
                        } else {
                            state = 5;
                        }
                        SB.append("dd"); // 일 (01 ~ 31)
                        i++;
                        break;
                    case 'l':
                        if (state == 0) {
                            SB.append('\'');
                            state = 6;
                        } else if (state == 6) {
                            SB.append('\u200B');
                        } else {
                            state = 6;
                        }
                        SB.append("EE"); // 요일 (Mon, Tus...) (월, 화...)
                        i++;
                        break;
                    case 'm':
                        if (state == 0) {
                            SB.append('\'');
                            state = 6;
                        } else if (state == 6) {
                            SB.append('\u200B');
                        } else {
                            state = 6;
                        }
                        SB.append("EEEE"); // 요일 (Monday, Tuesday...) (월요일, 화요일...)
                        i++;
                        break;
                    case 'n':
                        if (state == 0) {
                            SB.append('\'');
                            state = 7;
                        } else if (state == 7) {
                            SB.append('\u200B');
                        } else {
                            state = 7;
                        }
                        SB.append('a'); // "AM" or "PM", "오전" 또는 "오후"
                        i++;
                        break;
                    case 'o':
                        if (state == 0) {
                            SB.append('\'');
                            state = 8;
                        } else if (state == 8) {
                            SB.append('\u200B');
                        } else {
                            state = 8;
                        }
                        SB.append('h'); // 시간 (1 ~ 12)
                        i++;
                        break;
                    case 'p':
                        if (state == 0) {
                            SB.append('\'');
                            state = 8;
                        } else if (state == 8) {
                            SB.append('\u200B');
                        } else {
                            state = 8;
                        }
                        SB.append("hh"); // 시간 (01 ~ 12)
                        i++;
                        break;
                    case 'q':
                        if (state == 0) {
                            SB.append('\'');
                            state = 9;
                        } else if (state == 9) {
                            SB.append('\u200B');
                        } else {
                            state = 9;
                        }
                        SB.append('H'); // 시간 (0 ~ 23)
                        i++;
                        break;
                    case 'r':
                        if (state == 0) {
                            SB.append('\'');
                            state = 9;
                        } else if (state == 9) {
                            SB.append('\u200B');
                        } else {
                            state = 9;
                        }
                        SB.append("HH"); // 시간 (00 ~ 23)
                        i++;
                        break;
                    case 's':
                        if (state == 0) {
                            SB.append('\'');
                            state = 10;
                        } else if (state == 10) {
                            SB.append('\u200B');
                        } else {
                            state = 10;
                        }
                        SB.append('m'); // 분 (0 ~ 59)
                        i++;
                        break;
                    case 't':
                        if (state == 0) {
                            SB.append('\'');
                            state = 10;
                        } else if (state == 10) {
                            SB.append('\u200B');
                        } else {
                            state = 10;
                        }
                        SB.append("mm"); // 분 (00 ~ 59)
                        i++;
                        break;
                    case 'u':
                        if (state == 0) {
                            SB.append('\'');
                            state = 11;
//                        } else if (state == 11) {
//                            SB.append('\u200B');
                        } else {
                            state = 11;
                        }
                        //SB.append('s');
                        // 초(0~59)에 대한 특수 값
                        SB.append('\uF002');
                        i++;
                        break;
                    case 'v':
                        if (state == 0) {
                            SB.append('\'');
                            state = 11;
//                        } else if (state == 11) {
//                            SB.append('\u200B');
                        } else {
                            state = 11;
                        }
                        //SB.append("ss");
                        // 초(00~59)에 대한 특수 값
                        SB.append('\uF001');
                        i++;
                        break;
                    case 'w':
                        if (state != 0) {
                            SB.append('\'');
                            state = 0;
                        }
                        // 배터리에 대한 특수 값
                        SB.append('\uF000');
                        i++;
                        break;
                    case 'W':
                        if (state != 0) {
                            SB.append('\'');
                            state = 0;
                        }
                        // 배터리 전압에 대한 특수 값
                        SB.append('\uF005');
                        i++;
                        break;
                    case 'x':
                        if (state != 0) {
                            SB.append('\'');
                            state = 0;
                        }
                        // 네트워크 상태에 대한 특수 값
                        SB.append('\uF003');
                        i++;
                        break;
                    case 'X':
                        if (state != 0) {
                            SB.append('\'');
                            state = 0;
                        }
                        // 네트워크 상태(문자)에 대한 특수 값
                        SB.append('\uF004');
                        i++;
                        break;
                    case '.':
                        if (state != 0) {
                            SB.append('\'');
                            state = 0;
                        }
                        SB.append(".");
                        i++;
                        break;
                }
            }
            // '문자는 DateTimeFormatter에서 문자열 구분용으로 쓰이므로
            // '문자가 입력된 경우 따로 ''로 변환해줘야 정상 출력
            else if (array[i] == '\'') {
                if (state != 0) {
                    SB.append('\'');
                    state = 0;
                }
                SB.append("''");
            }
            // .와 다음줄 문자를 제외한 값을 처리
            else if (array[i] != '\n' && array[i] != '\r') {
                if (state != 0) {
                    SB.append('\'');
                    state = 0;
                }
                SB.append(array[i]);
            }
            i++;
        }
        // 마지막 문자 1개가 남았을 시 넣음
        if (i < len + 1 && array[i] != '.' && array[i] != '\n' && array[i] != '\r') {
            if (state != 0) {
                SB.append('\'');
                state = 0;
            }
            if (array[i] == '\'')
                SB.append("''");
            else
                SB.append(array[i]);
        }
        // 마지막에 문자열 입력상태면 문자열 끝을 표시하기 위해 '추가
        if (state == 0)
            SB.append('\'');

        return SB.toString();
    }

    // 시계 구조 설정 값을 기준으로 시계 문자열의 최대 크기를 계산
    // * 프로젝트 경로의 COS_structure.txt 파일 참조
    public static String getClockTextMax(String data, boolean mEnglish) {
        StringBuilder SB = new StringBuilder();

        // 시계 구조 저장
        char[] array = data.toCharArray();

        // for loop
        byte i = 0;
        byte len = (byte)(array.length - 1);
        // 변환작업
        while (i < len) {
            if (array[i] == '.') {
                switch (array[i + 1]) {
                    case 'z':
                        SB.append(" \n");
                        i++;
                        break;
                    case 'a':
                        SB.append("8888");
                        i++;
                        break;
                    case 'b':
                    case 'e':
                    case 'f':
                    case 'j':
                    case 'k':
                    case 'o':
                    case 'p':
                    case 'q':
                    case 'r':
                    case 's':
                    case 't':
                    case 'u':
                    case 'v':
                        SB.append("88");
                        i++;
                        break;
                    case 'c':
                    case 'i':
                        SB.append('8');
                        i++;
                        break;
                    case 'd':
                        SB.append("Q8");
                        i++;
                        break;
                    case 'g':
                        if (mEnglish)
                            SB.append("Www");
                        else
                            SB.append("00월");
                        i++;
                        break;
                    case 'h':
                        if (mEnglish)
                            SB.append("Wwwwwwwww");
                        else
                            SB.append("00월");
                        i++;
                        break;
                    case 'l':
                        if (mEnglish)
                            SB.append("Www");
                        else
                            SB.append("뷁");
                        i++;
                        break;
                    case 'm':
                        if (mEnglish)
                            SB.append("Wwwwwwwww");
                        else
                            SB.append("뷁요일");
                        i++;
                        break;
                    case 'n':
                        if (mEnglish)
                            SB.append("WM");
                        else
                            SB.append("오뷁");
                        i++;
                        break;
                    case 'w':
                        SB.append("100%△");
                        i++;
                        break;
                    case 'W':
                        SB.append("8888");
                        i++;
                        break;
                    case 'x':
                        SB.append("⌂≋⇵");
                        i++;
                        break;
                    case 'X':
                        SB.append("EWM");
                        i++;
                        break;
                    case '.':
                        SB.append('.');
                        i++;
                        break;
                }
            }
            // .와 다음줄 문자를 제외한 값을 처리
            else if (array[i] != '\n' && array[i] != '\r') {
                SB.append(array[i]);
            }
            i++;
        }
        // 마지막 문자 1개가 남았을 시 넣음
        if (i < len + 1 && array[i] != '.' && array[i] != '\n' && array[i] != '\r') {
            SB.append(array[i]);
        }
        SB.append(' ');

        return SB.toString();
    }

}
