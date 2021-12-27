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
import android.content.Intent;
import android.os.Build;

import kr.hsg.clockonscreen.COSSvc;
import kr.hsg.clockonscreen.COSSvc_Idle;

public final class COSSvcController {
    private final COSPermController cosPermController;
    private final Context ctx;

    public COSSvcController(final Context ctx) {
        if (ctx == null) {
            throw new RuntimeException("Context is null!");
        }
        this.ctx = ctx;
        this.cosPermController = new COSPermController(ctx);
    }

    private boolean hasOverlayPermission() {
        if (!cosPermController.hasOverlayPermission()) {
            cosPermController.openReqOverlayPermDialog();
            return false;
        }

        return true;
    }

    public void restartService() {
        if (!hasOverlayPermission()) {
            return;
        }

        Intent mSvc_Idle = new Intent(ctx, COSSvc_Idle.class);
        Intent mSvc = new Intent(ctx, COSSvc.class);

        ctx.stopService(mSvc);
        ctx.stopService(mSvc_Idle);

        ctx.startService(mSvc_Idle);
    }

    public void restartServiceInSetting() {
        if (!hasOverlayPermission()) {
            return;
        }

        Intent mSvc_Idle = new Intent(ctx, COSSvc_Idle.class);
        Intent mSvc = new Intent(ctx, COSSvc.class);

        ctx.stopService(mSvc);
        ctx.stopService(mSvc_Idle);

        mSvc.putExtra("PreferenceView", true);
        ctx.startService(mSvc);
    }

    public void startService() {
        if (!hasOverlayPermission()) {
            return;
        }

        Intent mSvc_Idle = new Intent(ctx, COSSvc_Idle.class);

        ctx.startService(mSvc_Idle);
    }

    // return false if overlay permission is not granted
    public boolean startServiceFromBroadcastReceiver() {
        if (!cosPermController.hasOverlayPermission()) {
            return false;
        }

        Intent mSvc_Idle = new Intent(ctx, COSSvc_Idle.class);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ctx.startForegroundService(mSvc_Idle);
            return true;
        }

        ctx.startService(mSvc_Idle);
        return true;
    }

    public void stopService() {
        Intent mSvc_Idle = new Intent(ctx, COSSvc_Idle.class);
        Intent mSvc = new Intent(ctx, COSSvc.class);

        ctx.stopService(mSvc);
        ctx.stopService(mSvc_Idle);
    }

}
