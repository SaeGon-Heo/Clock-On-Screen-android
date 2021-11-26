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
package kr.hsg.clockonscreen.config;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import java.util.Locale;

import kr.hsg.clockonscreen.COSSettings;
import kr.hsg.clockonscreen.R;
import kr.hsg.clockonscreen.dialog.AppInfoDialog;

public final class COSPref {
    private AppInfoDialog appInfoDialog;

    private final SharedPreferences pref;
    private final Context ctx;

    public COSPref(final Context ctx) {
        if (ctx == null) {
            throw new RuntimeException("Context is null!");
        }
        this.ctx = ctx;
        pref = PreferenceManager.getDefaultSharedPreferences(ctx.getApplicationContext());
        if (pref == null) {
            throw new RuntimeException("Failed to get SharedPreferences!");
        }
    }

    public boolean isServiceRunning() {
        return pref.getBoolean("service_running", false);
    }

    public void setServiceRunningOff() {
        pref.edit().putBoolean("service_running", false).apply();
    }

    public void setServiceRunningOn() {
        pref.edit().putBoolean("service_running", true).apply();
    }

    public Locale getSavedLocale() {
        if (pref.getBoolean(
                ctx.getApplicationContext().getString(R.string.pref_english_key_string),
                true))
        {
            return Locale.US;
        }
        return Locale.KOREA;
    }

    public void initPreferencesIfNeed() {
        if (!pref.getBoolean("init_pref", true)) {
            return;
        }

        // 사용중인 주 언어 확인 및 반영
        String currentLocale = Locale.getDefault().toString();
        final boolean isEnglish = !currentLocale.equals("ko_KR") && !currentLocale.equals("ko");

        final Context appCtx = ctx.getApplicationContext();

        // 기본 시계 구조에 따라 포맷 문자열 저장
        // TODO getClockText 함수 변경 반영
        pref.edit()
                .putBoolean(appCtx.getString(R.string.pref_english_key_string), isEnglish)
                .putString(appCtx.getString(R.string.pref_clockText_key_string), appCtx.getString(R.string.pref_clockText_default_string))
                .putString(appCtx.getString(R.string.pref_clockText_notfs_key_string), appCtx.getString(R.string.pref_clockText_default_string))
                .putString(appCtx.getString(R.string.pref_clockTextMax_key_string),
                        COSSettings.getClockTextMax(appCtx.getString(R.string.pref_clockText_default_string), isEnglish))
                .putString(appCtx.getString(R.string.pref_clockTextMax_notfs_key_string),
                        COSSettings.getClockTextMax(appCtx.getString(R.string.pref_clockText_default_string), isEnglish))
                .putString(appCtx.getString(R.string.pref_clockTextFormatted_key_string),
                        COSSettings.getClockTextFormatted(appCtx.getString(R.string.pref_clockText_default_string)))
                .putString(appCtx.getString(R.string.pref_clockTextFormatted_notfs_key_string),
                        COSSettings.getClockTextFormatted(appCtx.getString(R.string.pref_clockText_default_string))).apply();

        openAppInfoDialog();
    }

    public void markInitPreferencesIsDone() {
        pref.edit().putBoolean("init_pref", false).apply();
    }

    public void openAppInfoDialog() {
        if (appInfoDialog == null) {
            appInfoDialog = new AppInfoDialog(ctx);
        }

        appInfoDialog.open();
    }

    public void closeAppInfoDialog() {
        if (appInfoDialog != null) {
            appInfoDialog.close();
        }
    }

    public void closeAnyOpenedDialogs() {
        closeAppInfoDialog();
    }
}
