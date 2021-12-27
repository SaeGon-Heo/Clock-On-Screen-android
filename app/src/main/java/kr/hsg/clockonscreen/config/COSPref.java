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

import kr.hsg.clockonscreen.COSFragmentPref;
import kr.hsg.clockonscreen.R;
import kr.hsg.clockonscreen.dialog.AppInfoDialog;

public final class COSPref {

    private static final String KEY_SVC_RUNNING = "service_running";
    private static final String KEY_INIT_PREF = "init_pref";

    private AppInfoDialog appInfoDialog;

    private final SharedPreferences pref;
    private final Context ctx;
    private final Context appCtx;

    public COSPref(final Context ctx) {
        if (ctx == null) {
            throw new RuntimeException("Context is null!");
        }
        this.ctx = ctx;
        this.appCtx = ctx.getApplicationContext();

        pref = PreferenceManager.getDefaultSharedPreferences(ctx.getApplicationContext());
        if (pref == null) {
            throw new RuntimeException("Failed to get SharedPreferences!");
        }
    }

    public boolean isServiceRunning() {
        return pref.getBoolean(KEY_SVC_RUNNING, false);
    }

    public void setServiceRunningOff() {
        pref.edit().putBoolean(KEY_SVC_RUNNING, false).apply();
    }

    public void setServiceRunningOn() {
        pref.edit().putBoolean(KEY_SVC_RUNNING, true).apply();
    }

    public boolean isEnglish() {
        return pref.getBoolean(
                ctx.getApplicationContext()
                        .getString(R.string.pref_english_key_string),
                true
        );
    }

    public Locale getSavedLocale() {
        if (isEnglish()) {
            return Locale.US;
        }
        return Locale.KOREA;
    }

    public void initPreferencesIfNeed() {
        if (!pref.getBoolean(KEY_INIT_PREF, true)) {
            return;
        }

        // 사용중인 주 언어 확인 및 반영
        final String currentLocale = Locale.getDefault().toString();
        final boolean isEnglish = !currentLocale.equals("ko_KR") && !currentLocale.equals("ko");

        // 기본 시계 구조에 따라 포맷 문자열 저장
        // TODO getClockText 함수 변경 반영
        pref.edit()
                .putBoolean(appCtx.getString(R.string.pref_english_key_string), isEnglish)
                .putString(appCtx.getString(R.string.pref_clockText_key_string), appCtx.getString(R.string.pref_clockText_default_string))
                .putString(appCtx.getString(R.string.pref_clockText_notfs_key_string), appCtx.getString(R.string.pref_clockText_default_string))
                .putString(appCtx.getString(R.string.pref_clockTextMax_key_string),
                        COSFragmentPref.getClockTextMax(appCtx.getString(R.string.pref_clockText_default_string), isEnglish))
                .putString(appCtx.getString(R.string.pref_clockTextMax_notfs_key_string),
                        COSFragmentPref.getClockTextMax(appCtx.getString(R.string.pref_clockText_default_string), isEnglish))
                .putString(appCtx.getString(R.string.pref_clockTextFormatted_key_string),
                        COSFragmentPref.getClockTextFormatted(appCtx.getString(R.string.pref_clockText_default_string)))
                .putString(appCtx.getString(R.string.pref_clockTextFormatted_notfs_key_string),
                        COSFragmentPref.getClockTextFormatted(appCtx.getString(R.string.pref_clockText_default_string))).apply();

        openAppInfoDialog();
    }

    public void markInitPreferencesIsDone() {
        pref.edit().putBoolean(KEY_INIT_PREF, false).apply();
    }

    public String getClockText() {
        return pref.getString(appCtx.getString(R.string.pref_clockText_key_string), appCtx.getString(R.string.pref_clockText_default_string));
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
