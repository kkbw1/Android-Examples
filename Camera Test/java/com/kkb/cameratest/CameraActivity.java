package com.kkb.cameratest;

import android.Manifest;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.GestureDetector;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class CameraActivity extends AppCompatActivity implements SurfaceHolder.Callback,
        GestureDetector.OnGestureListener {
    private static final String TITLE = "CameraTest";
    private static final String TAG = "CameraTest";

    private Camera camera;
    private int degree = 0;
    private boolean previewRunning = false;
    private boolean ffCamera = false;	// Front-Facing Camera.

    private SurfaceView surfaceView;
    private SurfaceHolder surfaceHolder;
    private boolean surfaceViewCreate = false;
    private int app_width, app_height;

    GestureDetector mSurfaceGDetector;

    Button btnCap;

    //********************************************************************************************//
    //                                                                                            //
    //                                   Overriden Methods                                        //
    //                                                                                            //
    //********************************************************************************************//
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        ActionBar actionBar = getSupportActionBar();
        actionBar.hide();

        checkDiskPermission();

        InitializeComponent();

        mSurfaceGDetector = new GestureDetector(this, this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_takePic:
                camera.takePicture(null, null, mJpegPictureCallback);
                break;
            case R.id.menu_start:
                camera.startPreview();
                break;
            case R.id.menu_stop:
                camera.stopPreview();
                break;
            case R.id.menu_change:
    			cameraChange(camera);
                break;
            case R.id.menu_rotation:
                cameraRotation(camera);
                break;
            case R.id.menu_resolution:
//			    camera.stopPreview();
                cameraResolutionDialog();
                break;
            case R.id.menu_p_anti:
                createStringListDialog(camera.getParameters().getSupportedAntibanding(),
                        "Antibanding");
                break;
            case R.id.menu_p_color:
                createStringListDialog(camera.getParameters().getSupportedColorEffects(),
                        "ColorEffect");
                break;
            case R.id.menu_p_flash:
                createStringListDialog(camera.getParameters().getSupportedFlashModes(),
                        "FlashMode");
                break;
            case R.id.menu_p_focus:
                createStringListDialog(camera.getParameters().getSupportedFocusModes(),
                        "FocusMode");
                break;
            case R.id.menu_p_thumb:
                createSizeListDialog(camera.getParameters().getSupportedJpegThumbnailSizes(),
                        "JpegThumbNail Size");
                break;
            case R.id.menu_p_picformat:
                createIntegerListDialog(camera.getParameters().getSupportedPictureFormats(),
                        "Picture Format");
                break;
            case R.id.menu_p_picsize:
                createSizeListDialog(camera.getParameters().getSupportedPictureSizes(),
                        "Picture Size");
                break;
            case R.id.menu_p_preformat:
                createIntegerListDialog(camera.getParameters().getSupportedPreviewFormats(),
                        "Preview Format");
                break;
            case R.id.menu_p_prefpsrange:

                break;
            case R.id.menu_p_prefps:
                createIntegerListDialog(camera.getParameters().getSupportedPreviewFrameRates(),
                        "Preview FPS");
                break;
            case R.id.menu_p_presize:
                createSizeListDialog(camera.getParameters().getSupportedPreviewSizes(),
                        "Preview Size");
                break;
            case R.id.menu_p_scene:
                createStringListDialog(camera.getParameters().getSupportedSceneModes(),
                        "SceneMode");
                break;
            case R.id.menu_p_vidsize:

                break;
            case R.id.menu_p_white:
                createStringListDialog(camera.getParameters().getSupportedWhiteBalance(),
                        "WhiteBalance");
                break;
        }
        return true;
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        /* Check if this device has a camera */
        if (!this.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA))
        {
            // no camera on this device
            return;
        }

//        camera = Camera.open();
//        camera.lock();
//        if (camera != null) {
//            try {
//                camera.setPreviewDisplay(holder);
//                camera.setDisplayOrientation(degree);
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//            Camera.Parameters p = camera.getParameters();
//            p.setPictureSize(p.getPreviewSize().width, p.getPreviewSize().height);
//            camera.setParameters(p);
//        }

        // Checking if the system already get the access Camera Permission
        if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED)
        {

            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.CAMERA))
            {
                ActivityCompat.requestPermissions(this,
                        new String[]{android.Manifest.permission.CAMERA}, 50);
            }
            else {   // No explanation needed; request the permission
                ActivityCompat.requestPermissions(this,
                        new String[]{android.Manifest.permission.CAMERA}, 50);
            }
        }

        try {
            // open the camera
            camera = Camera.open();
        } catch (RuntimeException e) {
            // check for exceptions
            System.err.println(e);
            return;
        }

        int w = surfaceView.getWidth();
        int h = surfaceView.getHeight();

        // modify parameter
        Camera.Parameters param = camera.getParameters();
        param.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
//        param.setPreviewSize(param.getPreviewSize().width, param.getPreviewSize().height);
        camera.setParameters(param);
        try {
            camera.setPreviewDisplay(surfaceHolder);
            camera.startPreview();
        } catch (Exception e) {
            // check for exceptions
            System.err.println(e);
            return;
        }

        w = param.getPreviewSize().width;
        h = param.getPreviewSize().height;
        Toast.makeText(CameraActivity.this, w + "x" + h, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        if(surfaceViewCreate == false)
        {
            app_width = width;
            app_height = height;
            surfaceViewCreate = true;
        }

        surfaceViewSizeChange(camera, surfaceView, app_width, app_height);

        camera.startPreview();
        previewRunning = true;
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        if(camera != null) {
            cameraClose(camera);
            camera = null;
        }
    }

    //------------------------------  Surface Gesture Events  --------------------------//
    @Override
    public boolean onDown(MotionEvent e) {
        return true;
    }

    @Override
    public void onShowPress(MotionEvent e) {

    }

    @Override
    public boolean onSingleTapUp(MotionEvent e) {
        if(previewRunning)
        {
            camera.autoFocus(mAutoFocus);
        }

        return true;
    }

    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
        return true;
    }

    @Override
    public void onLongPress(MotionEvent e) {

    }

    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
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
    //                                   Listeners and Callbacks                                  //
    //                                                                                            //
    //********************************************************************************************//
    private View.OnTouchListener surfaceTocuhListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
//        if(previewRunning)
//        {
//            camera.autoFocus(mFocus);
//        }
//
//        Toast.makeText(CameraActivity.this, app_width + "," + app_height,
//                Toast.LENGTH_SHORT).show();
//
//        ActionBar actionBar = getSupportActionBar();
//        if(actionBar.isShowing()) {
//            actionBar.hide();
//        }
//        else {
//            actionBar.show();
//        }

            return mSurfaceGDetector.onTouchEvent(event);

//        if(mSurfaceGDetector.onTouchEvent(event))
//        {
//            return true;
//        }
//        return super.onTouchEvent(event);
        }
    };

    private Camera.AutoFocusCallback mAutoFocus = new Camera.AutoFocusCallback() {
        @Override
        public void onAutoFocus(boolean success, Camera camera) {
            if(success)
                Toast.makeText(CameraActivity.this, "AutoFoucs",
                        Toast.LENGTH_SHORT).show();
        }
    };

    private Camera.PictureCallback mJpegPictureCallback = new Camera.PictureCallback() {
        @Override
        public void onPictureTaken(byte[] data, Camera camera) {

            String sd_dcim_path = Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_DCIM).getAbsolutePath();

            String path;

            int fileNumber = 0;
            File file;

            while(true)
            {
                String num = String.valueOf(fileNumber);
                path = sd_dcim_path + "/" + currentDate() + "_" + num + ".jpg";
                file = new File(path);
                if(file.exists())
                    fileNumber++;
                else
                    break;
            }

            try {
                FileOutputStream fos = new FileOutputStream(file);
                fos.write(data);
                fos.flush();
                fos.close();
            } catch (Exception e) {
                System.err.println(e);
                Toast.makeText(CameraActivity.this, "File Save Error.",
                        Toast.LENGTH_SHORT).show();
                return;
            }

            Intent intent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
            Uri uri = Uri.parse("file://" + path);
            intent.setData(uri);
            sendBroadcast(intent);

            Toast.makeText(CameraActivity.this, "File Save Success.",
                    Toast.LENGTH_SHORT).show();
            camera.startPreview();
        }
    };

    //********************************************************************************************//
    //                                                                                            //
    //                               User Defined Sub-routines                                    //
    //                                                                                            //
    //********************************************************************************************//
    private void checkDiskPermission () {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "No Disk Permissions" , Toast.LENGTH_LONG).show();
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 0);
        }
        else
        {
            Toast.makeText(this, "Has Disk Permissions" , Toast.LENGTH_LONG).show();
        }
    }

    private void InitializeComponent() {
        surfaceView = findViewById(R.id.surfaceView1);
        surfaceView.setOnTouchListener(surfaceTocuhListener);
        surfaceHolder = surfaceView.getHolder();
        surfaceHolder.addCallback(CameraActivity.this);
        // deprecated setting, but required on Android versions prior to 3.0
        surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

        btnCap = findViewById(R.id.btnCap);
        btnCap.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Capture a photo image
                camera.takePicture(null, null, mJpegPictureCallback);
                Toast.makeText(CameraActivity.this, "Capture", Toast.LENGTH_SHORT).show();;
            }
        });
    }

    private String currentDate() {
        String currentDate;

        Calendar cal = Calendar.getInstance();
        String year = String.valueOf(cal.get(Calendar.YEAR));
        String month = String.valueOf(cal.get(Calendar.MONTH) + 1);
        String day = String.valueOf(cal.get(Calendar.DAY_OF_MONTH));
        currentDate = year + "_" + month + "_" + day;

        return currentDate;
    }

    private void surfaceViewSizeChange(Camera camera, SurfaceView sv, int w, int h) {
        Camera.Parameters p = camera.getParameters();

        float ratio = (float)p.getPreviewSize().width / (float)p.getPreviewSize().height;
        int change_width = Math.round(h * ratio);
        if(change_width > app_width)
            change_width = app_width;
        int change_height = h;

        ViewGroup.LayoutParams params = sv.getLayoutParams();
        params.width = change_width;
        params.height = change_height;
        sv.setLayoutParams(params);
    }

    private void cameraChange(Camera camera) {
        if(Camera.getNumberOfCameras() > 1)
        {
            if(camera != null)
                cameraClose(camera);

            if(ffCamera == true) 	// if FFCamera On.
            {
                int camId = cameraId(Camera.CameraInfo.CAMERA_FACING_BACK);
                camera = Camera.open(camId);
                camera.lock();
                ffCamera = false;
            }
            else 					// if FFCamera Off.
            {
                int camId = cameraId(Camera.CameraInfo.CAMERA_FACING_FRONT);
                camera = Camera.open(camId);
                camera.lock();
                ffCamera = true;
            }

            try {
                camera.setPreviewDisplay(surfaceHolder);
                camera.setDisplayOrientation(degree);
            } catch (IOException e) {
                System.err.println(e);
            }

            Camera.Parameters p = camera.getParameters();
//			Log.d("camera", p.flatten());
            String width_p = String.valueOf(p.getPreviewSize().width);
            String height_p = String.valueOf(p.getPreviewSize().height);
            setTitle(TITLE + ": " + width_p + "&" + height_p);

            camera.startPreview();
            previewRunning = true;
        }
        else
        {
            Toast.makeText(this, "Number of Camera is 1 or 0.", Toast.LENGTH_SHORT).show();
        }
    }

    private int cameraId(int camera_Info) {
        int getId = 0;
        int count = Camera.getNumberOfCameras();

        Camera.CameraInfo info = new Camera.CameraInfo();
        for(int camId = 0; camId < count; camId++ ) {
            Camera.getCameraInfo(camId, info);
            if(info.facing == camera_Info) {
                getId = camId;
                break;
            }
        }

        return getId;
    }

    private void cameraRotation(Camera camera) {
        degree += 90;
        if(degree >= 360)
            degree = 0;

        if(camera != null) {
            camera.stopPreview();
            camera.setDisplayOrientation(degree);
            camera.startPreview();
        }
    }

    private void cameraClose(Camera camera) {
        camera.stopPreview();
        previewRunning = false;
        camera.unlock();
        camera.release();
    }

    private void cameraResolutionDialog() {
        final List<Camera.Size> previewSizes = PreviewSizesAtPictureSizes(camera);
        final List<Camera.Size> pictureSizes = camera.getParameters().getSupportedPictureSizes();
        final ArrayList<String> pictureSizes_string = new ArrayList<String>();

        for(Camera.Size size : pictureSizes)
        {
            String width = String.valueOf(size.width);
            String height = String.valueOf(size.height);

            pictureSizes_string.add(width + "&" + height);
        }

        ArrayAdapter<String> size_adapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_list_item_1, pictureSizes_string);

        AlertDialog.Builder builder;
        builder = new AlertDialog.Builder(this);
        builder.setTitle("Camera Resolution");
        builder.setAdapter(size_adapter, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                camera.stopPreview();
                previewRunning = false;

                Camera.Size pre_size = previewSizes.get(which);
                Camera.Size pic_size = pictureSizes.get(which);

                Camera.Parameters p = camera.getParameters();
                p.setPreviewSize(pre_size.width, pre_size.height);
                p.setPictureSize(pic_size.width, pic_size.height);

                String width_pic = String.valueOf(p.getPreviewSize().width);
                String height_pic = String.valueOf(p.getPreviewSize().height);
                String width_pre = String.valueOf(p.getPictureSize().width);
                String height_pre = String.valueOf(p.getPictureSize().height);
                setTitle(TITLE + ": " + width_pic + "&" + height_pic
                        + "//" + width_pre + "&" + height_pre);

                camera.setParameters(p);

                surfaceViewSizeChange(camera, surfaceView, app_width, app_height);

                camera.startPreview();
                previewRunning = true;
            }
        });

        Dialog dialog = builder.create();
        dialog.show();
    }

    private List<Camera.Size> PreviewSizesAtPictureSizes(Camera mCamera) {

        if (mCamera == null) {
            return null;
        }

        List<Camera.Size> pictureSizes = mCamera.getParameters().getSupportedPictureSizes();
        List<Camera.Size> previewSizes = mCamera.getParameters().getSupportedPreviewSizes();
        List<Camera.Size> previewSizesAtOptimizedPicture = new ArrayList<Camera.Size>();
        for(int i = 0; i < pictureSizes.size(); i++) {
            previewSizesAtOptimizedPicture.add(null);
        }

        boolean pic_descend = false;
        boolean pre_descend = false;
        Camera.Size pic_size;
        Camera.Size pre_size;
        double pic_ratio = 0;
        double pre_ratio = 0;
        final double Tolerance = 0.05;

        int width_first = pictureSizes.get(0).width;
        int width_last = pictureSizes.get(pictureSizes.size() - 1).width;

        if(width_first > width_last) 	// Descending
        {
            pic_descend = true;
            pre_descend = true;
        }
//	    else if(width_first < width_last)	//Ascending
//	    {
//	    	pic_descend = false;
//	    	pre_descend = false;
//	    }
        else
        {
            Log.d(TAG, "PreviewSizesAtPictureSizes Error Descending or Ascending.");
        }

        //Check High Resolution -> Low Resolution
        if(pic_descend == true && pre_descend == true)
        {
            for(int index_pic = 0; index_pic < pictureSizes.size(); index_pic++) {
                pic_size = pictureSizes.get(index_pic);
                pic_ratio = (double)pic_size.width / (double)pic_size.height;

                for(int index_pre = 0; index_pre < previewSizes.size(); index_pre++) {
                    pre_size = previewSizes.get(index_pre);
                    pre_ratio = (double)pre_size.width / (double)pre_size.height;
                    if(Math.abs(pic_ratio - pre_ratio) < Tolerance) {
                        previewSizesAtOptimizedPicture.set(index_pic, pre_size);
                        break;
                    }
                }
            }
        }
        else if(!pic_descend == false && pre_descend == false)
        {
            for(int index_pic = pictureSizes.size() - 1; index_pic >= 0; --index_pic) {
                pic_size = pictureSizes.get(index_pic);
                pic_ratio = (double)pic_size.width / (double)pic_size.height;

                for(int index_pre = previewSizes.size() - 1; index_pre >= 0; --index_pre) {
                    pre_size = previewSizes.get(index_pre);
                    pre_ratio = (double)pre_size.width / (double)pre_size.height;
                    if(Math.abs(pic_ratio - pre_ratio) < Tolerance) {
                        previewSizesAtOptimizedPicture.set(index_pic, pre_size);
                        break;
                    }
                }
            }
        }

        return previewSizesAtOptimizedPicture;
    }

    private void createStringListDialog(List<String> list, String title) {
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_list_item_1, list);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(title);
        builder.setAdapter(adapter, null);
        builder.setPositiveButton("Back", null);
        builder.create().show();
    }

    private void createIntegerListDialog(List<Integer> list, String title) {
        ArrayAdapter<Integer> adapter = new ArrayAdapter<Integer>(this,
                android.R.layout.simple_list_item_1, list);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(title);
        builder.setAdapter(adapter, null);
        builder.setPositiveButton("Back", null);
        builder.create().show();
    }

    private void createSizeListDialog(List<Camera.Size> list, String title) {
        ArrayList<String> list_string = new ArrayList<String>();
        for(Camera.Size size : list)
        {
            String width = String.valueOf(size.width);
            String height = String.valueOf(size.height);

            list_string.add(width + "&" + height);
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_list_item_1, list_string);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(title);
        builder.setAdapter(adapter, null);
        builder.setPositiveButton("Back", null);
        builder.create().show();
    }
}
