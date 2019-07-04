package com.kkb.touchtest;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.view.GestureDetectorCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity implements
        GestureDetector.OnGestureListener, GestureDetector.OnDoubleTapListener {

    private static final String DEBUG_TAG = "Gestures";
    private GestureDetectorCompat mDetector;

    MyGestureListener  mSimpleGestureListener;

    static Handler mHandler;

    public ArrayAdapter<String> MyArrayAdapter;
    private ListView MyListView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

//        // Instantiate the gesture detector with the
//        // application context and an implementation of
//        // GestureDetector.OnGestureListener
//        mDetector = new GestureDetectorCompat(this,this);
//        // Set the gesture detector as the double tap
//        // listener.
//        mDetector.setOnDoubleTapListener(this);

        MyArrayAdapter = new ArrayAdapter<String>(this, R.layout.list);
        MyListView = findViewById(R.id.listView1);
        MyListView.setAdapter(MyArrayAdapter);
        MyListView.setTranscriptMode(ListView.TRANSCRIPT_MODE_ALWAYS_SCROLL);
        MyListView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return mDetector.onTouchEvent(event);
            }
        });

        MyArrayAdapter.add("App Created");

        mHandler = new MyHandler(this);

        mSimpleGestureListener = new MyGestureListener(this, mHandler);
//        mDetector = new GestureDetectorCompat(this, mSimpleGestureListener);
        mDetector = new GestureDetectorCompat(this, mSimpleGestureListener);
    }

    //********************************************************************************************//
    //                                                                                            //
    //                                   Overriden Methods                                        //
    //                                                                                            //
    //********************************************************************************************//
    @Override
    public boolean onTouchEvent(MotionEvent event) {

        if (this.mDetector.onTouchEvent(event)) {
            return true;
        }
        return super.onTouchEvent(event);

//        int action = MotionEventCompat.getActionMasked(event);
//
//        switch(action) {
//            case (MotionEvent.ACTION_DOWN) :
//                Log.d(DEBUG_TAG,"Action was DOWN");
//                return true;
//            case (MotionEvent.ACTION_MOVE) :
//                Log.d(DEBUG_TAG,"Action was MOVE");
//                return true;
//            case (MotionEvent.ACTION_UP) :
//                Log.d(DEBUG_TAG,"Action was UP");
//                return true;
//            case (MotionEvent.ACTION_CANCEL) :
//                Log.d(DEBUG_TAG,"Action was CANCEL");
//                return true;
//            case (MotionEvent.ACTION_OUTSIDE) :
//                Log.d(DEBUG_TAG,"Movement occurred outside bounds " +
//                        "of current screen element");
//                return true;
//            default :
//                return super.onTouchEvent(event);
//        }
    }

    @Override
    public boolean onSingleTapConfirmed(MotionEvent e) {
        Log.d(DEBUG_TAG, "onSingleTapConfirmed: " + e.toString());
        return true;
    }

    @Override
    public boolean onDoubleTap(MotionEvent e) {
        Log.d(DEBUG_TAG, "onDoubleTap: " + e.toString());
        return true;
    }

    @Override
    public boolean onDoubleTapEvent(MotionEvent e) {
        Log.d(DEBUG_TAG, "onDoubleTapEvent: " + e.toString());
        return true;
    }

    @Override
    public boolean onDown(MotionEvent e) {
        Log.d(DEBUG_TAG,"onDown: " + e.toString());
        return true;
    }

    @Override
    public void onShowPress(MotionEvent e) {
        Log.d(DEBUG_TAG, "onShowPress: " + e.toString());
    }

    @Override
    public boolean onSingleTapUp(MotionEvent e) {
        Log.d(DEBUG_TAG, "onSingleTapUp: " + e.toString());
        return true;
    }

    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
        Log.d(DEBUG_TAG, "onScroll: "  + distanceX + ", " + distanceY + " " + e1.toString() + e2.toString());
        return true;
    }

    @Override
    public void onLongPress(MotionEvent e) {
        Log.d(DEBUG_TAG, "onLongPress: " + e.toString());
    }

    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
        Log.d(DEBUG_TAG, "onFling: " + velocityX + ", " + velocityY + " " + e1.toString() + e2.toString());

        if(velocityY < -2000)
        {
            ActionBar actionBar = getSupportActionBar();
            actionBar.hide();
        }
        else if(velocityY > 2000)
        {
            ActionBar actionBar = getSupportActionBar();
            actionBar.show();
        }

        return true;
    }

    //********************************************************************************************//
    //                                                                                            //
    //                                   Handlers and Callbacks                                   //
    //                                                                                            //
    //********************************************************************************************//
    private static class MyHandler extends Handler {
        MainActivity mainActivity;

        public MyHandler (MainActivity main) {
            mainActivity = main;
        }

        public void handleMessage(Message msg) {
            switch(msg.what) {
                case MyGestureListener.MESSAGE_DOWN:
                    mainActivity.MyArrayAdapter.add("Touch Down");
                    break;
                case MyGestureListener.MESSAGE_FLING:
                    mainActivity.MyArrayAdapter.add("Touch Fling " + msg.arg1 + ", " + msg.arg2);
                    break;
                case MyGestureListener.MESSAGE_DOUBLETAP:
                    mainActivity.MyArrayAdapter.add("Touch Double Tap");
                    break;
            }
        }
    }
}
