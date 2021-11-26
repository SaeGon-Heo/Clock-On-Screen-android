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
package kr.hsg.clockonscreen.control;

import android.content.Context;
import android.os.Build;
import android.provider.Settings;

import kr.hsg.clockonscreen.config.COSPref;
import kr.hsg.clockonscreen.dialog.ReqOverlayPermDialog;

public final class COSPermController {
    private ReqOverlayPermDialog reqOverlayPermDialog;

    private final COSPref cosPref;
    private final Context ctx;

    public COSPermController(final Context ctx) {
        if (ctx == null) {
            throw new RuntimeException("Context is null!");
        }
        this.ctx = ctx;
        this.cosPref = new COSPref(ctx);
    }

    public boolean hasOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (Settings.canDrawOverlays(ctx.getApplicationContext())) {
                return true;
            }
            cosPref.setServiceRunningOff();
            return false;
        }
        return true;
    }

    // API 23(Marshmallow)이후 오버레이 권한을 켜도록 하는 Dialog 출력
    public void openReqOverlayPermDialog() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return;
        }

        if (reqOverlayPermDialog == null) {
            reqOverlayPermDialog = new ReqOverlayPermDialog(ctx);
        }

        reqOverlayPermDialog.open();
    }

    public void closeReqOverlayPermDialog() {
        if (reqOverlayPermDialog != null) {
            reqOverlayPermDialog.close();
        }
    }

    public void closeAnyOpenedDialogs() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            closeReqOverlayPermDialog();
        }
    }

}
