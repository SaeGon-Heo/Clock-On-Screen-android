// Original: https://github.com/pvyParts/Android-Helpers/blob/master/FullScreenDetector/Screendetect.java
//
// This is a full screen state detector by using LinearLayout
//
// --- Require ---
// Add permission below in manifest
// <uses-permission android:name="android.permission.SYSTEM_ALERT_WINDOW" />
// And add it in Activity or Service too
// <service
//            android:name=".TestService"
//            android:exported="false"
//            android:permission="android.permission.SYSTEM_ALERT_WINDOW">
// </service>
//
// Must initialize using ui thread context
// Note that "Min Api Level" is 11

 /* --- Usage ---
	private FSDetector detector;
    <onCreate>
    // activate detector
	detector = new FSDetector(this);
    detector.attach();

    // set Listener
    detector.setOnFullScreenListener(new OnFullScreenListener() {
        @Override
        public void fsChanged(Context context, boolean bIsFS) {
            if(bIsFS) {
                // write your code
            }
        }
    });

    <onDestroy>
    // deactivate detector
    if(detector != null)
        detector.detach();
*/

package kr.hsg.clockonscreen;

import android.content.Context;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.os.Build;
import android.view.Display;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;

import java.lang.reflect.InvocationTargetException;

public final class FSDetector extends LinearLayout {
    //private String FLAG = "FSDetectorLog";
    private OnFullScreenListener OnFullScreenListener;
    private WindowManager winManager;
    private Context mCon;
    private boolean bError;
    private boolean bAttachedOnFullScreenListener;

    public FSDetector(Context context) {
        super(context);
        mCon = context;
        if(mCon == null) {
            bError = true;
            // Log.e(FLAG, "context error");
        }
        this.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
    }

    public boolean attach() {
        if(bError) return false;

        // FSDetector를 최상단에 넣기 위한 layout 설정 값
        int __type;
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            __type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        else
            __type = WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY;
        // Fix for the cannot click install on sideloaded apps bug
        // Dont fill width, set to left side with a few pixels wide
        WindowManager.LayoutParams layout = new WindowManager.LayoutParams(
                1, WindowManager.LayoutParams.MATCH_PARENT,
                __type,
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE |
                        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSPARENT);
        layout.gravity = Gravity.LEFT;

        // 화면 최상단에 FSDetector를 삽입
        if(winManager == null)
            winManager = ((WindowManager)mCon.getSystemService(Context.WINDOW_SERVICE));
        winManager.addView(this, layout);

        return true;
    }

    public boolean detach() {
        if(bError) return false;

        // FSDetector를 화면 최상단에서 제거
        this.clearAnimation();

        if(winManager == null)
            winManager = ((WindowManager)mCon.getSystemService(Context.WINDOW_SERVICE));
        winManager.removeView(this);

        return true;
    }

    // this does the magic
    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        if(bError) return;
        //this.setBackgroundColor(0xafffffff);
        if (changed) {
            //Log.d(FLAG, "screen - " + r + " x " + b);
            onFSChanged(b);
        }
    }

    // do some math... and update the listeners ect ect
    private void onFSChanged(int bottom) {
        //Log.d(FLAG, "FS changed");
        if(OnFullScreenListener != null) {
            Point size = new Point(0, 0);
            int height;

            // soft key 제외 영역 계산
            if(winManager == null)
                winManager = ((WindowManager)mCon.getSystemService(Context.WINDOW_SERVICE));
            Display dis = winManager.getDefaultDisplay();
            dis.getSize(size);
            height = size.y;

            // soft key 포함 영역 계산
            if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                dis.getRealSize(size);
            } else {
                try {
                    //size.x = (Integer) Display.class.getMethod("getRawWidth").invoke(dis);
                    size.y = (Integer) Display.class.getMethod("getRawHeight").invoke(dis);
                } catch (IllegalAccessException e) {} catch (InvocationTargetException e) {} catch (NoSuchMethodException e) {}
            }

            // LinearLayout의 bottom (세로 픽셀 크기)과 측정된 화면 크기 Point의 y값(세로)이 같으면 풀스크린 상태
            //Log.d(FLAG, "screen FS " + (bottom == height || bottom == size.y) + ". scrSize:" + size.x + "/" + size.y + ", Bottom:" + bottom);
            OnFullScreenListener.fsChanged(mCon, bottom == height || bottom == size.y);
        }
    }

    public void setOnFullScreenListener
            (OnFullScreenListener listener) {
        this.OnFullScreenListener = listener;
        this.bAttachedOnFullScreenListener = true;
    }

    public boolean hasOnFullScreenListener() {
        return this.bAttachedOnFullScreenListener;
    }

    public interface OnFullScreenListener {
        void fsChanged(Context context, boolean bFSState);
    }
}