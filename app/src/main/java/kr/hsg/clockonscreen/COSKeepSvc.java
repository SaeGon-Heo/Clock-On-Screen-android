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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.provider.Settings;

public final class COSKeepSvc extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Context mCon = context.getApplicationContext();
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(mCon);

        // 서비스가 실행 중 상태로 저장되어 있는지 확인
        if (pref.getBoolean("service_running", false)) {
            // 오버레이 권한 확인
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M
                    && !Settings.canDrawOverlays(mCon)) {
                pref.edit().putBoolean("service_running", false).apply();
				return;
            }

            // action 값이 null인지 확인
            String strAction = intent.getAction();
            if(strAction == null)
				return;

            // 부팅(QuickBoot 포함) 및 앱 재설치 시 서비스 실행
            if(strAction.equals(Intent.ACTION_BOOT_COMPLETED) || strAction.equals(Intent.ACTION_MY_PACKAGE_REPLACED) ||
            strAction.equals("android.intent.action.QUICKBOOT_POWERON") || strAction.equals("com.htc.intent.action.QUICKBOOT_POWERON")) {
                Intent mSvc_Idle = new Intent(mCon, COSSvc_Idle.class);
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O)
                    mCon.startForegroundService(mSvc_Idle);
                else
                    mCon.startService(mSvc_Idle);
            }
        }
    }
}
