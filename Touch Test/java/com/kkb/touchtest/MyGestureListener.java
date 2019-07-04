package com.kkb.touchtest;

import android.content.Context;
import android.os.Handler;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;

public class MyGestureListener extends GestureDetector.SimpleOnGestureListener {

    public static final int MESSAGE_DOWN = 1;
    public static final int MESSAGE_FLING = 2;
    public static final int MESSAGE_DOUBLETAP = 3;

    private static final String DEBUG_TAG = "Gestures";
    Context mainContext;
    private final Handler mHandler;

    public MyGestureListener(Context context1, Handler handler) {
        mainContext = context1;
        mHandler = handler;
    }

    @Override
    public boolean onDown(MotionEvent event) {
        Log.d(DEBUG_TAG,"onDown: " + event.toString());

        mHandler.obtainMessage(MESSAGE_DOWN).sendToTarget();

        return true;
    }

    @Override
    public boolean onFling(MotionEvent event1, MotionEvent event2,
                           float velocityX, float velocityY) {
        Log.d(DEBUG_TAG, "onFling: " + event1.toString() + event2.toString());

        mHandler.obtainMessage(MESSAGE_FLING, (int)velocityX, (int)velocityY).sendToTarget();

        return true;
    }

    @Override
    public boolean onDoubleTap(MotionEvent e) {
        Log.d(DEBUG_TAG, "onDouble: " + e.toString());

        mHandler.obtainMessage(MESSAGE_DOUBLETAP).sendToTarget();

        return true;
    }
}
