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
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;

public final class FSDetector extends LinearLayout {
    private final static String LOG_TAG = "FSDetectorLog";

    private OnFullScreenListener onFullScreenListener;
    private WindowManager winManager;
    private Context ctx;
    private boolean bError;

    public FSDetector(Context context) {
        super(context);
        if (context == null) {
            bError = true;
            Log.e(LOG_TAG, "context is null!");
            return;
        }

        ctx = context;

        // Get Window Manager using default display context
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            DisplayManager dm = ((DisplayManager)context.getSystemService(Context.DISPLAY_SERVICE));
            Display dis = dm.getDisplay(Display.DEFAULT_DISPLAY);
            Context defaultDisContext = context.createDisplayContext(dis);
            winManager = ((WindowManager) defaultDisContext.getSystemService(Context.WINDOW_SERVICE));
        }
        else {
            winManager = ((WindowManager) context.getSystemService(Context.WINDOW_SERVICE));
        }

        if (winManager == null) {
            bError = true;
            Log.e(LOG_TAG, "Failed to get WindowManager!");
            return;
        }

        this.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
    }

    public boolean attach() {
        if (bError) return false;

        // create layout configuration to insert FSDetector into top screen
        int __type;
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            __type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        else
            __type = WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY;

        // Fix for the cannot click install on sideloaded apps bug
        // Dont fill width, set to left side with a few pixels wide
        WindowManager.LayoutParams layout = new WindowManager.LayoutParams(
                0,
                WindowManager.LayoutParams.MATCH_PARENT,
                __type,
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE |
                        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSPARENT);
        layout.gravity = Gravity.LEFT;

        // insert FSDetector into top screen
        winManager.addView(this, layout);

        return true;
    }

    public boolean detach() {
        if (bError) return false;

        // FSDetector를 화면 최상단에서 제거
        this.clearAnimation();
        winManager.removeView(this);

        return true;
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        if (bError) return;
        //Log.d(LOG_TAG, "onLayout called");

        if (changed) {
            //Log.d(LOG_TAG, "onLayout - changed: " + changed);
            onFSChanged();
        }
    }

    private void onFSChanged() {
        //Log.d(LOG_TAG, "FS changed");
        if (bError || !hasOnFullScreenListener()) {
            return;
        }

        // getLocationOnScreen에서 얻은 y 값이 0인 경우 풀스크린
        int loc[] = new int[2];
        this.getLocationOnScreen(loc);
        //Log.d(LOG_TAG, "y: " + location[1]);
        //Log.d(LOG_TAG, "screen FS: " + (location[1] == 0));

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