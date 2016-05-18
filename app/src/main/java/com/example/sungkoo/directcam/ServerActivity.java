package com.example.sungkoo.directcam;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Message;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.Toast;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class ServerActivity extends Activity {
    Socket  socket;
    private static String TAG = "CAMERA";
    private static int count;
    private Context mContext = this;
    private Camera mCamera;
    private CameraPreview mPreview;
    public static String mediapath;
    public static boolean ack= false;

    ServerSocket    serverSocket= null;
    Socket          clientSocket= null;

    BufferedOutputStream bos;// = new BufferedOutputStream(clientSocket.getOutputStream());
    DataOutputStream outputStream;// = new DataOutputStream(bos);

    byte[]  b1, b2;

    android.os.Handler handler = new android.os.Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            ack=false;
            mCamera.takePicture(null, null, mPicture);


        }

    };

    Thread  thread;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        socket= SelectActivity.socket;

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_server);
        mContext = this;

        // 카메라 사용여부 체크
        if(!checkCameraHardware(getApplicationContext())){
            finish();
        }

        // 카메라 인스턴스 생성
        mCamera = getCameraInstance();

        // 프리뷰창을 생성하고 액티비티의 레아이웃으로 지정
        mPreview = new CameraPreview(this, mCamera);
        FrameLayout preview = (FrameLayout) findViewById(R.id.camera_preview);
        preview.addView(mPreview);
        mCamera.startPreview();


        try {
            bos = new BufferedOutputStream(socket.getOutputStream());
            outputStream = new DataOutputStream(bos);
        }catch(IOException e){

        }

        thread= new Thread(new Runnable(){
            @Override
            public void run() {
                try {
                    Thread.sleep(500);
                }catch(InterruptedException e){
                }
                handler.sendEmptyMessage(1);

                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                }

                while(true) {
                   try {
                       //Thread.sleep(100);

                       while(ServerActivity.ack==false){

                           Thread.sleep(50);
                       }

                       handler.sendEmptyMessage(1);

                       b2=b1;
                       b1=null;
                       try {
                           outputStream.writeInt(b2.length);
                           outputStream.write(b2, 0, b2.length);
                           outputStream.flush();
                       }catch(IOException e){

                       }



                   } catch (InterruptedException e) {
                        break;
                    }

                }
            }
        });
        thread.start();
        ack= true;
    }

    /**
     * 카메라 사용여부 가능 체크
     * @param context
     * @return
     */
    private boolean checkCameraHardware(Context context) {
        if (context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)) {
            Log.i(TAG, "Number of available camera : " + Camera.getNumberOfCameras());
            return true;
        } else {
            Toast.makeText(context, "No camera found!", Toast.LENGTH_SHORT).show();
            return false;
        }
    }

    /**
     * 카메라 인스턴스 호출
     * @return
     */
    public Camera getCameraInstance(){
        try{
            // open() 의 매개변수로 int 값을 받을 수 도 있는데, 일반적으로 0이 후면 카메라, 1이 전면 카메라를 의미합니다.
            mCamera = Camera.open();
        }catch(Exception e){
            Log.i(TAG,"Error : Using Camera");
            e.printStackTrace();
        }
        return mCamera;
    }

    private Camera.PictureCallback mPicture = new Camera.PictureCallback() {
        @Override
        public void onPictureTaken(byte[] data, Camera camera) {
            b1= data;

/*
            try {
                outputStream.writeInt(data.length);
                outputStream.write(data, 0, data.length);
                outputStream.flush();

            }catch (IOException e){
            }

*/
            mCamera.startPreview();
            ack= true;

        }
    };

}