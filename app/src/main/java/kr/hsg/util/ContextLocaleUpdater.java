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
package kr.hsg.util;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;

import java.util.Locale;

public class ContextLocaleUpdater {
    private final Context ctx;

    public ContextLocaleUpdater(final Context ctx) {
        if (ctx == null) {
            throw new RuntimeException("Context is null!");
        }
        this.ctx = ctx;
    }

    public void updateLocale(Locale locale) {
        Configuration config = new Configuration();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            config.setLocale(locale);
        }
        else {
            config.locale = locale;
        }
        Resources res = ctx.getResources();
        res.updateConfiguration(config, res.getDisplayMetrics());
    }
}
