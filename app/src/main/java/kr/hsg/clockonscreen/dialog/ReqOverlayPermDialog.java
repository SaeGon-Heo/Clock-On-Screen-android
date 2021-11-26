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

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;

import kr.hsg.clockonscreen.R;

public final class ReqOverlayPermDialog extends BaseDialog {
    private final Context ctx;

    public ReqOverlayPermDialog(final Context ctx) {
        super();
        if (ctx == null) {
            throw new RuntimeException("Context is null!");
        }
        this.ctx = ctx;
    }

    @Override
    @TargetApi(Build.VERSION_CODES.M)
    public void buildAndOpen() {
        if (hasDialog()) {
            open();
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(ctx)
                .setPositiveButton(android.R.string.ok, null)
                .setIcon(R.mipmap.ic_launcher)
                .setTitle(R.string.app_request_overlay_permission_title)
                .setCancelable(false);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            builder.setMessage(R.string.app_request_overlay_permission_content_android_R);
        }
        else {
            builder.setMessage(R.string.app_request_overlay_permission_content);
        }

        AlertDialog dialog = builder.create();

        dialog.show();
        dialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener(v -> {
            ctx.startActivity(
                    new Intent(
                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:" + ctx.getApplicationContext().getPackageName())
                    )
            );
            close();
        });

        updateDialog(dialog);
    }
}
