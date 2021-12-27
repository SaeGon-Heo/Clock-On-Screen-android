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
package kr.hsg.clockonscreen.dialog;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;

import kr.hsg.clockonscreen.R;
import kr.hsg.clockonscreen.config.COSPref;
import kr.hsg.clockonscreen.control.COSSvcController;
import kr.hsg.clockonscreen.model.COSStructure;

public class ClockTextDialog extends BaseDialog {
    private final Activity activity;
    private final Context appCtx;
    private final COSPref cosPref;
    private final COSSvcController cosSvcController;
    private final COSStructure cosStructure;

    private View layout;
    private EditText et;
    private TextWatcher tw;
    private StringBuilder preview;
    private TextView tv;
    private ScrollView sv;

    public ClockTextDialog(final Activity activity) {
        super();
        if (activity == null) {
            throw new RuntimeException("Activity is null!");
        }
        this.activity = activity;
        this.appCtx = activity.getApplicationContext();
        this.cosPref = new COSPref(activity);
        this.cosSvcController = new COSSvcController(activity);
        this.cosStructure = new COSStructure(activity);
    }

    @Override
    public void buildAndOpen() {
        if (hasDialog()) {
            open();
            return;
        }

        if (layout == null) {
            try {
                layout = ((LayoutInflater)activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE))
                        .inflate(R.layout.pref_dialog_clocktext, activity.findViewById(R.id.layout_clocktext));
            } catch (NullPointerException e) {
                return;
            }
        }

        // 레이아웃 내 시계 구성요소 목록을 표시하는 TextView 내용 업데이트
        ((TextView)layout.findViewById(R.id.textview_clocktext_components))
                .setText(R.string.pref_dialog_clocktext_components);

        // 레이아웃에서 사용되는 edittext를 가져오고, NO_SUGGESTIONS 플래그 설정.
        et = layout.findViewById(R.id.edittext_clocktext);
        et.setInputType(InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);

        // 텍스트 변경 리스너
        // 시계 구조 문자열 변경시 미리보기를 실시간으로 출력하는 역할
        if (tw == null) {
            updateTextWatcher();
        }
        et.addTextChangedListener(tw);

        // 시계내용 설정 기존 값을 미리 저장 + TextWatcher 한번 여기서 작동되어 프리뷰 미리 작성
        et.setText(cosPref.getClockText());
        et.setSelection(et.length());

        // 다이얼로그 빌드
        AlertDialog dialog = new AlertDialog.Builder(activity)
                .setTitle(R.string.pref_clocktext_title)
                .setView(layout)
                .setPositiveButton(android.R.string.ok, null)
                .setNegativeButton(android.R.string.cancel, null)
                // 기본값 복원 버튼
                .setNeutralButton(R.string.pref_dialog_btn_default, null)
                .create();

        dialog.show();
        // 기본값 복원 버튼(택스트를 기본값으로 변경)
        dialog.getButton(DialogInterface.BUTTON_NEUTRAL)
                .setOnClickListener(v -> {
                    et.setText(appCtx.getString(R.string.pref_clockText_default_string));
                    et.setSelection(et.length());
                });
        // 확인 버튼
        dialog.getButton(DialogInterface.BUTTON_POSITIVE)
                .setOnClickListener(v -> {
                    final String clockText = et.getText().toString();

                    // 빈값인 상태로 OK 누른경우 기존 설정값을 불러오고 다이얼로그를 닫지 않음
                    if (clockText.equals("")) {
                        et.setText(cosPref.getClockText());
                        et.setSelection(et.length());
                        return;
                    }

                    cosStructure.updateClockText(clockText);
                    cosSvcController.restartServiceInSetting();
                    close();
                });

        updateDialog(dialog);
    }

    private void updateTextWatcher() {
        if (layout == null) {
            return;
        }

        if (preview == null) {
            preview = new StringBuilder();
        }
        if (tv == null) {
            tv = layout.findViewById(R.id.textview_clocktext_preview);
        }
        if (sv == null) {
            sv = layout.findViewById(R.id.scrollview_preview);
        }

        tw = new TextWatcher() {
            @Override
            public void afterTextChanged(Editable arg0) {}
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() < 0) {
                    return;
                }

                // (전체)입력한 값에 대한 미리보기를 만들고 출력
                // 미리보기를 만드는 도중 현재 입력중인 위치가 몇번째 라인인지 계산
                final int modifiedLine = cosStructure.makePreview(s, start + count, preview);
                tv.setText(preview.toString());
                preview.setLength(0);
                // 라인 당 높이 값에 현재 입력중인 라인 위치 값을 곱하여
                // 프리뷰를 현재 입력중인 라인 위치로 스크롤
                // 첫 실행시 뷰가 화면에 표시가 안된 상태여서
                // 높이 및 라인 카운트가 0으로 반환되므로 이 경우 높이 0으로 스크롤
                try {
                    sv.smoothScrollTo(0, tv.getHeight() / tv.getLineCount() * modifiedLine);
                } catch (ArithmeticException e) {
                    sv.smoothScrollTo(0, 0);
                }
            }
        };
    }
}
