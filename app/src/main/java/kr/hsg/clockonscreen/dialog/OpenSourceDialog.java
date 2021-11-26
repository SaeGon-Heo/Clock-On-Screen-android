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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class OpenSourceDialog extends BaseDialog {
    private final Context ctx;

    public OpenSourceDialog(final Context ctx) {
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

        StringBuilder license = new StringBuilder();
        try (
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(
                                ctx.getAssets().open("opensource.txt")
                        )
                )
        ) {
            while (true) {
                final String line = reader.readLine();
                if (line == null) break;
                license.append(line);
                license.append('\n');
            }
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        AlertDialog dialog = new AlertDialog.Builder(ctx)
                .setPositiveButton(android.R.string.ok, null)
                .setIcon(android.R.drawable.ic_menu_help)
                .setMessage(license.toString())
                .create();

        updateDialog(dialog);
        dialog.show();
    }
}
