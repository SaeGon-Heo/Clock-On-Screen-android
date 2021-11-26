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

public abstract class BaseDialog {
    private AlertDialog dialog;

    BaseDialog() {}

    public void open() {
        if (dialog == null) {
            buildAndOpen();
        }

        if (!dialog.isShowing()) {
            dialog.show();
        }
    }

    public void close() {
        if (dialog != null && dialog.isShowing()) {
            dialog.dismiss();
        }
    }

    public void updateDialog(AlertDialog dialog) {
        this.dialog = dialog;
    }

    public boolean hasDialog() {
        return (this.dialog != null);
    }

    public abstract void buildAndOpen();

}
