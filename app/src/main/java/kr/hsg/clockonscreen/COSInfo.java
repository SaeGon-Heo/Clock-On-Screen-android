/*
 * Copyright 2020 SaeGon Heo
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
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.provider.Settings;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public final class COSInfo extends Activity {
    Context mCon;
    AlertDialog OpenSourceDialog;
    Uri GitHubLink;

    // 서비스 실행
    void restartService() {
        Intent mSvc_Idle = new Intent(mCon, COSSvc_Idle.class);
        Intent mSvc = new Intent(mCon, COSSvc.class);

        // 오버레이 권한이 사용자에 의해 풀린 경우 확인하여 서비스를 종료하고 다시 켜도록 유도
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M
                && !Settings.canDrawOverlays(mCon)) {
            // 정보 엑티비티 이면 정보 엑티비티만 닫음. service_running 값은 여전히 true 이므로
            // Main Activity로 복귀하면서 오버레이 권한을 한번더 확인.
            if(!isFinishing()) finish();
            return;
        }

        mCon.stopService(mSvc);
        mCon.stopService(mSvc_Idle);
        mCon.startService(mSvc_Idle);
    }

    @Override
    public void onDestroy() {
        if(OpenSourceDialog != null && OpenSourceDialog.isShowing()) {
            OpenSourceDialog.dismiss();
        }

        super.onDestroy();
    }


    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mCon = getApplicationContext();

        setContentView(R.layout.activity_info);
        setTitle(R.string.action_info);

        // Add up button
        ActionBar ab = getActionBar();
        if (ab != null) ab.setDisplayHomeAsUpEnabled(true);

        // GitHub Uri 미리 생성
        GitHubLink = Uri.parse("https://github.com/SaeGon-Heo/Clock-On-Screen-android");

        // 버튼 터치 시 효과 (음영)
        View.OnTouchListener touchEffect = new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent event) {
                switch(event.getAction()) {
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        view.setBackgroundColor(0xfffafafa);
                        break;
                    case MotionEvent.ACTION_DOWN:
                        view.setBackgroundColor(0xffc0c0c0);
                        break;
                }
                return false;
            }
        };

        // 아래 측 TextView를 버튼으로 활용
        TextView btnGithubLink = findViewById(R.id.btn_github_link);
        TextView btnOpenSourceLibrary = findViewById(R.id.btn_open_source_library);

        // 버튼 터치 시 효과
        btnGithubLink.setOnTouchListener(touchEffect);
        btnOpenSourceLibrary.setOnTouchListener(touchEffect);

        // GitHub Link 버튼 클릭 시
        // GitHub Repo로 연결
        btnGithubLink.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setAction(Intent.ACTION_VIEW);
                intent.addCategory(Intent.CATEGORY_BROWSABLE);
                intent.setData(GitHubLink);
                startActivity(intent);
            }
        });

        // Open Source 버튼 클릭 시
        // 참고한 라이브러리 및 소스 코드를 볼 수 있는 Dialog를 표시
        btnOpenSourceLibrary.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(OpenSourceDialog == null) {
                    StringBuilder license = new StringBuilder();

                    /*
                     * try-with-resources 라는 방식을 쓰면 알아서 close가 된다
                     * 다만 API 19 이상 지원이여서 쓰지 않음
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(con.getInputStream()));)
                    {
                        StringBuilder sb = new StringBuilder();

                        String str = null;

                        while (null != (str = reader.readLine()))
                            sb = sb.append(str);

                        resStr = sb.toString();
                    }
                    */

                    InputStreamReader input = null;
                    BufferedReader reader = null;
                    try {
                        input = new InputStreamReader(getAssets().open("opensource.txt"));
                        reader = new BufferedReader(input);

                        // do reading, usually loop until end of file reading
                        String mLine = reader.readLine();
                        while (mLine != null) {
                            license.append(mLine);
                            license.append('\n');
                            mLine = reader.readLine();
                        }
                        reader.close();
                        input.close();
                    } catch (IOException e) {
                        try {
                            if (reader != null) reader.close();
                        } catch (IOException e1) { }
                        try {
                            if (input != null) input.close();
                        } catch (IOException e2) { }

                        if(!isFinishing()) finish();
                    }

                    OpenSourceDialog = new AlertDialog.Builder(COSInfo.this)
                            .setPositiveButton(android.R.string.ok, null)
                            .setIcon(android.R.drawable.ic_menu_help)
                            .setMessage(license.toString())
                            .create();

                    license.setLength(0);
                }

                if(!OpenSourceDialog.isShowing())
                    OpenSourceDialog.show();
            }
        });
    } // onCreate

    @Override
    protected void onResume() {
        super.onResume();
        // 서비스가 켜진상태면 재실행
        if(PreferenceManager.getDefaultSharedPreferences(mCon).getBoolean("service_running", false)) restartService();
    } // onResume

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Home button (As Up button) action
        if (item.getItemId() == android.R.id.home) {
            if(!isFinishing()) finish();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}