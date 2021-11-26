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
// You must have to initialize this using ui thread context

 /* --- Usage ---
	private FSDetector detector;
    <onCreate>
    // activate detector
	detector = new FSDetector(this);
    detector.attach();

    // set Listener
    detector.setOnFullScreenListener(new OnFullScreenListener() {
        @Override
        public void fsChanged(Context context, boolean bFSState) {
            if (bFSState) {
                // write your code
            }
        }
    });

    <onDestroy>
    // deactivate detector
    if (detector != null)
        detector.detach();
*/

package kr.hsg.util;

import android.content.Context;
import android.graphics.PixelFormat;
import android.hardware.display.DisplayManager;
import android.os.Build;
import android.view.Display;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;

public final class FSDetector extends LinearLayout {
    private OnFullScreenListener onFullScreenListener;

    private final WindowManager winManager;
    private final Context ctx;

    public FSDetector(Context ctx) {
        super(ctx);
        if (ctx == null) {
            throw new RuntimeException("Context is null!");
        }

        this.ctx = ctx;

        winManager = getWinManager(ctx);
        if (winManager == null) {
            throw new RuntimeException("Failed to get WindowManager!");
        }

        this.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
    }

    private WindowManager getWinManager(Context ctx) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            DisplayManager dm = ((DisplayManager)ctx.getSystemService(Context.DISPLAY_SERVICE));
            Display dis = dm.getDisplay(Display.DEFAULT_DISPLAY);
            Context defaultDisContext = ctx.createDisplayContext(dis);

            return ((WindowManager) defaultDisContext.getSystemService(Context.WINDOW_SERVICE));
        }

        return ((WindowManager) ctx.getSystemService(Context.WINDOW_SERVICE));
    }

    public boolean attach() {
        // create layout configuration to insert FSDetector into top screen
        int type;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        }
        else {
            type = WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY;
        }

        // Fix for the cannot click install on sideloaded apps bug
        // Dont fill width, set to left side with a few pixels wide
        WindowManager.LayoutParams layout = new WindowManager.LayoutParams(
                0,
                WindowManager.LayoutParams.MATCH_PARENT,
                type,
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE |
                        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSPARENT);
        layout.gravity = Gravity.LEFT;

        // insert FSDetector into top screen
        winManager.addView(this, layout);

        return true;
    }

    public boolean detach() {
        // FSDetector를 화면 최상단에서 제거
        this.clearAnimation();
        winManager.removeView(this);

        return true;
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        if (changed) {
            onFSChanged();
        }
    }

    private void onFSChanged() {
        if (!hasOnFullScreenListener()) {
            return;
        }

        // getLocationOnScreen에서 얻은 y 값이 0인 경우 풀스크린
        int loc[] = new int[2];
        this.getLocationOnScreen(loc);

        onFullScreenListener.fsChanged(ctx, loc[1] == 0);
    }

    public void setOnFullScreenListener(OnFullScreenListener listener) {
        this.onFullScreenListener = listener;
    }

    public boolean hasOnFullScreenListener() {
        return this.onFullScreenListener != null;
    }

    public interface OnFullScreenListener {
        void fsChanged(Context context, boolean bFSState);
    }
}