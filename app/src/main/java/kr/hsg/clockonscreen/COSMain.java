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
package kr.hsg.clockonscreen;

import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.widget.ImageView;
import android.widget.TextView;

import kr.hsg.clockonscreen.config.COSPref;
import kr.hsg.clockonscreen.control.COSPermController;
import kr.hsg.clockonscreen.control.COSSvcController;
import kr.hsg.util.ContextLocaleUpdater;

public final class COSMain extends Activity {
    COSPref cosPref;
    COSSvcController cosSvcController;
    COSPermController cosPermController;

    @Override
    public void onDestroy() {
        cosPermController.closeAnyOpenedDialogs();
        cosPref.closeAnyOpenedDialogs();
        super.onDestroy();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        cosPref = new COSPref(this);
        cosSvcController = new COSSvcController(this);
        cosPermController = new COSPermController(this);

        // 현재 Context의 Locale 업데이트
        new ContextLocaleUpdater(this).updateLocale(cosPref.getSavedLocale());

        // 메인화면 출력
        setContentView(R.layout.activity_main);

        // Add up button
        ActionBar ab = getActionBar();
        if (ab != null) ab.setDisplayHomeAsUpEnabled(true);

        // 오버레이 권한 체크 및 없으면 켜달라고 요구하기
        if (!cosPermController.hasOverlayPermission()) {
            cosPermController.openReqOverlayPermDialog();
        }

        // 첫 실행 또는 설정 초기화를 실행한 경우 초기 값 설정
        cosPref.initPreferencesIfNeed();

        // 서비스 실행상태 시 안내문 및 아이콘 변경
        if (cosPref.isServiceRunning()) {
            updateSvcStateUiOn();
        }

        // 시작 / 중지버튼 클릭 이벤트 및 터치 효과 등록
        final ImageView iv = findViewById(R.id.btn_svc_toggle);
        addClickEvent(iv);
        addTouchEffect(iv);
    } // onCreate

    private void updateSvcStateUiOn() {
        final TextView tv = findViewById(R.id.tv_svc_state);
        final ImageView iv = findViewById(R.id.btn_svc_toggle);

        iv.setImageResource(R.mipmap.ic_svc_on);
        tv.setText(R.string.text_svc_on);
        tv.setTextColor(Color.GREEN);
    }

    private void updateSvcStateUiOff() {
        final TextView tv = findViewById(R.id.tv_svc_state);
        final ImageView iv = findViewById(R.id.btn_svc_toggle);

        iv.setImageResource(R.mipmap.ic_svc_off);
        tv.setText(R.string.text_svc_off);
        tv.setTextColor(Color.RED);
    }

    private void addClickEvent(final ImageView iv) {
        // 설정값에 따라 서비스 상태를 토글
        iv.setOnClickListener(v -> {
            if (cosPref.isServiceRunning()) {
                cosSvcController.stopService();
                updateSvcStateUiOff();
                cosPref.setServiceRunningOff();
                return;
            }

            cosSvcController.startService();
            updateSvcStateUiOn();
            cosPref.setServiceRunningOn();
        });
    }

    @SuppressLint("ClickableViewAccessibility")
    private void addTouchEffect(final ImageView iv) {
        iv.setOnTouchListener((view, event) -> {
            final int action = event.getAction();

            if (action == MotionEvent.ACTION_UP ||
                    action == MotionEvent.ACTION_CANCEL)
            {
                view.setBackgroundColor(0xfffafafa);
            }
            else if (action == MotionEvent.ACTION_DOWN) {
                view.setBackgroundResource(R.mipmap.ic_svc_background);
            }

            return false;
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 서비스가 켜진상태면 재실행
        if (cosPref.isServiceRunning()) {
            cosSvcController.restartService();
        }
    } // onResume

    // 메인 화면에만 메뉴를 로드
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        final int id = item.getItemId();

        if (id == R.id.action_settings) {
            startActivity(new Intent(this, COSSettings.class));
            return true;
        }
        else if (id == R.id.action_info) {
            startActivity(new Intent(this, COSInfo.class));
            return true;
        }
        else if (id == android.R.id.home) {
            if (!isFinishing()) finish();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}