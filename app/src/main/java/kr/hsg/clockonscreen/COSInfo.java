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
package kr.hsg.clockonscreen;

import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.widget.TextView;

import kr.hsg.clockonscreen.config.COSPref;
import kr.hsg.clockonscreen.control.COSSvcController;
import kr.hsg.clockonscreen.dialog.OpenSourceDialog;

public final class COSInfo extends Activity {
    COSPref cosPref;
    COSSvcController cosSvcController;

    OpenSourceDialog openSourceDialog;

    @Override
    public void onDestroy() {
        if (openSourceDialog != null) {
            openSourceDialog.close();
        }

        super.onDestroy();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        cosPref = new COSPref(this);
        cosSvcController = new COSSvcController(this);

        setContentView(R.layout.activity_info);
        setTitle(R.string.action_info);

        // Add up button
        ActionBar ab = getActionBar();
        if (ab != null) ab.setDisplayHomeAsUpEnabled(true);

        // 아래 측 TextView를 버튼으로 활용
        final TextView btnGithubLink = findViewById(R.id.btn_github_link);
        final TextView btnOpenSourceLibrary = findViewById(R.id.btn_open_source_library);

        // 버튼 터치 시 효과 추가
        addTouchEffect(btnGithubLink);
        addTouchEffect(btnOpenSourceLibrary);

        // GitHub Link 버튼 클릭 시
        // GitHub Repo로 연결
        btnGithubLink.setOnClickListener(v -> {
            Intent intent = new Intent();
            intent.setAction(Intent.ACTION_VIEW);
            intent.addCategory(Intent.CATEGORY_BROWSABLE);
            intent.setData(Uri.parse("https://github.com/SaeGon-Heo/Clock-On-Screen-android"));
            startActivity(intent);
        });

        // Open Source 버튼 클릭 시
        // 참고한 라이브러리 및 소스 코드를 볼 수 있는 Dialog를 표시
        openSourceDialog = new OpenSourceDialog(this);
        btnOpenSourceLibrary.setOnClickListener(view -> openSourceDialog.open());
    } // onCreate

    @SuppressLint("ClickableViewAccessibility")
    private void addTouchEffect(final TextView tv) {
        tv.setOnTouchListener((view, event) -> {
            final int action = event.getAction();

            if (action == MotionEvent.ACTION_UP ||
                    action == MotionEvent.ACTION_CANCEL)
            {
                view.setBackgroundColor(0xfffafafa);
            }
            else if (action == MotionEvent.ACTION_DOWN) {
                view.setBackgroundColor(0xffc0c0c0);
            }

            return false;
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (cosPref.isServiceRunning()) {
            cosSvcController.restartService();
        }
    } // onResume

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Home button (As Up button) action
        if (item.getItemId() == android.R.id.home) {
            if (!isFinishing()) finish();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}