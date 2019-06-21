/*
 *  Dummy Layout for detecting the statusbar vissibility on GB and below
 *
 *  Created by pvyParts
 *
 *  Modified by Saegon Heo (to work on more devices)
 *
 *  NOTE** May or may-not be a stupid way of doing it but hey it works....
 *
 *  ON SDK 11 and above use OnSystemUiVisibilityChangeListener()
 *
 *  FS_Bool True if the StatusBar is hiden eg Fullscreen App is Running
 *
 *	OnFullScreenListener fired if layout size changes by over-riding the onLayout()
 */

 /* --- Usage ---
    <In Class>
	private FSDetector detector;
    <onCreate>
	detector = new FSDetector(this);
    detector.setLayerType(View.LAYER_TYPE_SOFTWARE, null);

    int __type = WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY;
    if(android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
        __type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
    }

    // Fix for the cannot click install on sideloaded apps bug
    // Dont fill width, set to left side with a few pixels wide
    WindowManager.LayoutParams layout = new WindowManager.LayoutParams(
            1, WindowManager.LayoutParams.MATCH_PARENT,
            __type,
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE |
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSPARENT);
    layout.gravity = Gravity.LEFT;
    ((WindowManager)getSystemService(Context.WINDOW_SERVICE)).addView(detector, layout);

    detector.setOnFullScreenListener(new OnFullScreenListener() {
        @Override
        public void fsChanged(Context context, boolean bIsFS) {
            if(bIsFS) {
                // TODO write your code
            }
        }
    });

    <onDestroy>
    if(detector != null) {
        detector.clearAnimation();
        ((WindowManager)getSystemService(Context.WINDOW_SERVICE)).removeView(detector);
    }
*/
package kr.hsg.clockonscreen;

import android.content.Context;
import android.graphics.Point;
import android.os.Build;
import android.view.Display;
import android.view.WindowManager;
import android.widget.LinearLayout;

import java.lang.reflect.InvocationTargetException;

public final class FSDetector extends LinearLayout {
    //private String FLAG = "FSDetector";
    private OnFullScreenListener OnFullScreenListener;
    private Context mCon;
    private boolean bAttachedOnFullScreenListener;

    public FSDetector(Context context) {
        super(context);
        mCon = context.getApplicationContext();
    }

    // this does the magic
    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
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
            Display dis;
            try {
                dis = ((WindowManager) mCon.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
            } catch (NullPointerException e) {
                return;
            }
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
        void fsChanged(Context context, boolean bIsFS);
    }
}