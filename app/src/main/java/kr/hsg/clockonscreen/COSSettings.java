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

import android.app.ActionBar;
import android.app.Activity;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Toast;

import kr.hsg.clockonscreen.config.COSPref;
import kr.hsg.clockonscreen.control.COSPermController;
import kr.hsg.clockonscreen.control.COSSvcController;

public final class COSSettings extends Activity {
    COSPref cosPref;
    COSSvcController cosSvcController;
    COSPermController cosPermController;
    COSFragmentPref cosFragmentPref;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle(R.string.action_settings);

        // Add up button
        ActionBar ab = getActionBar();
        if (ab != null) ab.setDisplayHomeAsUpEnabled(true);

        cosPref = new COSPref(this);
        cosSvcController = new COSSvcController(this);
        cosPermController = new COSPermController(this);

        cosFragmentPref = new COSFragmentPref();
        getFragmentManager().beginTransaction()
                .replace(android.R.id.content, cosFragmentPref)
                .commit();
    } //onCreate

    @Override
    protected void onResume() {
        super.onResume();

        // 서비스 무조건 실행 + 설정 창이므로 FS모드 무시
        Toast.makeText(
                COSSettings.this,
                R.string.pref_toast_fsmode_ignored_on_prefactivity,
                Toast.LENGTH_SHORT
        ).show();

        if (cosPermController.hasOverlayPermission()) {
            cosSvcController.restartServiceInSetting();
            return;
        }
        cosSvcController.stopService();
        cosPermController.openReqOverlayPermDialog();
    } // onResume

    @Override
    protected void onPause() {
        super.onPause();
        // 설정 창을 나가므로 보통 방식으로 서비스 재시작 또는 끄기
        if (cosPref.isServiceRunning()) {
            cosSvcController.restartService();
            return;
        }
        cosSvcController.stopService();
    }

    // TODO 열려있는 Dialog 닫기 및 TextWatcher 제거
    // TODO 일부 private 오브젝트에 종속된 오브젝트들만 따로 null
    @Override
    public void onDestroy() {
        cosPref.closeAnyOpenedDialogs();
        cosPermController.closeAnyOpenedDialogs();
        // TODO cosFragmentPref.destroy();

        if (cosFragmentPref != null) {
            getFragmentManager().beginTransaction()
                    .remove(cosFragmentPref)
                    .commit();
        }

        super.onDestroy();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            if (!isFinishing()) finish();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

}
