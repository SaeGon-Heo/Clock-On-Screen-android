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

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;

import kr.hsg.clockonscreen.R;
import kr.hsg.clockonscreen.config.COSPref;

public final class AppInfoDialog extends BaseDialog {
    private final Context ctx;

    public AppInfoDialog(final Context ctx) {
        super();
        if (ctx == null) {
            throw new RuntimeException("Context is null!");
        }
        this.ctx = ctx;
    }

    @Override
    public void buildAndOpen() {
        if (hasDialog()) {
            open();
            return;
        }

        StringBuilder MsgBuilder = new StringBuilder();
        final Context appCtx = ctx.getApplicationContext();

        MsgBuilder.append(appCtx.getString(R.string.app_info_content));
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            MsgBuilder.append(appCtx.getString(R.string.app_info_content_oreo_suffix));
        }

        // Dialog 생성
        AlertDialog dialog = new AlertDialog.Builder(ctx)
                .setPositiveButton(android.R.string.ok, null)
                .setIcon(R.mipmap.ic_launcher)
                .setTitle(R.string.action_info)
                .setMessage(MsgBuilder.toString())
                .setCancelable(false)
                .create();

        dialog.show();
        dialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener(v -> {
            new COSPref(ctx).markInitPreferencesIsDone();
            close();
        });

        updateDialog(dialog);
    }
}
