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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import kr.hsg.clockonscreen.config.COSPref;
import kr.hsg.clockonscreen.control.COSSvcController;
import kr.hsg.util.ContextLocaleUpdater;

public final class COSKeepSvc extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        final String action = intent.getAction();
        if (action == null) {
            return;
        }


        // action 값이 부팅(QuickBoot 포함) 및 앱 재설치가 아닌 경우 체크
        if (!(
                action.equals(Intent.ACTION_BOOT_COMPLETED) ||
                action.equals(Intent.ACTION_MY_PACKAGE_REPLACED) ||
                action.equals("android.intent.action.QUICKBOOT_POWERON") ||
                action.equals("com.htc.intent.action.QUICKBOOT_POWERON")
            ))
        {
            return;
        }

        COSPref cosPref = new COSPref(context);
        // 현재 Context의 Locale 업데이트
        new ContextLocaleUpdater(context).updateLocale(cosPref.getSavedLocale());

        // 서비스가 실행 중 상태이면 실행
        if (cosPref.isServiceRunning()) {
            final boolean result = new COSSvcController(context).startServiceFromBroadcastReceiver();
            if (!result) {
                Toast.makeText(context, R.string.toast_no_overlay_perm, Toast.LENGTH_LONG).show();
            }
        }

    }
}
