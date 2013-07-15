package com.example.cameratest;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.hardware.Camera.PreviewCallback;
import android.hardware.Camera.Size;
import android.media.CamcorderProfile;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.Toast;


public class MainActivity extends Activity
{
    private SurfaceView surfaceView = null;
    private SurfaceHolder surfaceHolder = null;
    private Camera camera;
    private int imageCount = 0;
    private static byte[] oldPreviewFrame;
    private ImageView differenceView;
    
    
    private Size previewSize;
    private byte frame1[];
    private int difference[];
    private Bitmap bm;
    private static LinkedList<byte[]> frames = new LinkedList<byte[]>();;
    private int framerate;
    private double count;
    
    private Button button;
    
    private SeekBar Brightness,Blue;
    private TextView BValue,BlueValue;
    private static int bval,blueval;
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate( savedInstanceState );
        
        setContentView( R.layout.activity_main );
        MainActivity.oldPreviewFrame=null;
        
        surfaceView = (SurfaceView) findViewById( R.id.surfaceview01 );
        differenceView = (ImageView) findViewById(R.id.differenceimage);
        surfaceHolder = surfaceView.getHolder();
        surfaceHolder.setType( SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS );
        surfaceHolder.addCallback( surfaceCallback );

        Log.e( getLocalClassName(), "END: onCreate" );
        CamcorderProfile profile = CamcorderProfile.get(CamcorderProfile.QUALITY_LOW);
        framerate = profile.videoFrameRate;
        count=0;
        
        //Create Button
        button = (Button)findViewById(R.id.CaptureButton);
        button.setOnClickListener(new OnClickListener(){

			@Override
			public void onClick(View arg0) {
				// TODO Auto-generated method stub
				//if(arg0.getId()==R.id.CaptureButton)
					FilteringTask.setFilter();
					
					
				/*int dataBufferSize=(int)(previewSize.height*previewSize.width*
	                    (ImageFormat.getBitsPerPixel(camera.getParameters().getPreviewFormat())/8.0));
					frames = new LinkedList<byte[]>();
					frames.add(new byte[dataBufferSize]);*/
			}
        	
        });
        
        //Create SeekBar
        bval=100;
        BValue = (TextView)findViewById(R.id.BrightnessVal);
        BValue.setText("Y: "+bval);
        Brightness = (SeekBar)findViewById(R.id.Brightness);
        Brightness.setProgress(bval);
        Brightness.setOnSeekBarChangeListener( new SeekBarListener(R.id.Brightness));
        
        blueval=100;
        BlueValue = (TextView)findViewById(R.id.BlueVal);
        BlueValue.setText("B: "+blueval);
        Blue = (SeekBar)findViewById(R.id.Blue);
        Blue.setProgress(blueval);
        Blue.setOnSeekBarChangeListener(new SeekBarListener(R.id.Blue));
        
        
    }

    @Override
    public void onConfigurationChanged( Configuration newConfig )
    {
        super.onConfigurationChanged( newConfig );
    }

    SurfaceHolder.Callback surfaceCallback = new SurfaceHolder.Callback() {
        private Canvas canvas;

		public void surfaceCreated( SurfaceHolder holder )
        {
            camera = Camera.open();
            imageCount = 0;
            
            try {
                camera.setPreviewDisplay( surfaceHolder );
            } catch ( Throwable t )
            {
                Log.e( "surfaceCallback", "Exception in setPreviewDisplay()", t );
            }
            previewSize=camera.getParameters().getPreviewSize();
    		int dataBufferSize=(int)(previewSize.height*previewSize.width*
                    (ImageFormat.getBitsPerPixel(camera.getParameters().getPreviewFormat())/8.0));
    		frame1 = new byte[dataBufferSize];
    		difference = new int[dataBufferSize];

    		
    		frames.addLast(new byte[dataBufferSize]);
            Log.e( getLocalClassName(), "END: surfaceCreated" );
            //camera.getParameters().setPreviewFormat(ImageFormat.RGB_565);
        }

        public void surfaceChanged( SurfaceHolder holder, int format, int width, int height )
        {
            if ( camera != null )
            {
            	
                camera.setPreviewCallback( new PreviewCallback() {

                    public void onPreviewFrame( final byte[] data, Camera camera ) {
                    	//Log.d("onPreviewFrame","onPreviewFrame");
                    	if(count==0){
	                		if(data.length!=frame1.length){
	                			Log.e(null,"buffer sizes dont match");
	                		}
	                		else{
	                			frames.addLast(data);
	                			FilteringTask task = new FilteringTask(differenceView,previewSize,camera.getParameters());
	                			task.execute();
	                		}
                    	}
                    	count++;
                    	//it gets slower and slower with the more image processing i do,
                    	//so i need to change the framerate on the filtering task
                    	//for just differenceFilter framerate/2 works well
                    	//for Y-filter framerate
                    	TextView total = (TextView) findViewById(R.id.ThreadInfo);
                    	total.setText("RunTime: "+FilteringTask.total);
                    	if(count>(double)framerate*.1){
                    		count=0;
                    		if(frames.size()>40){
                    			Toast.makeText(getApplicationContext(), "Refreshing filter queue", Toast.LENGTH_SHORT).show();
                    			byte[] last = frames.getLast();
                    			frames = new LinkedList<byte[]>();
                    			frames.add(last);
                    		}
                    	}
                    	
                    }
                    
                });
                Camera.Parameters parameters = camera.getParameters();
                if ( parameters != null )
                {

                    camera.startPreview();
                }
            }
        }

        public void surfaceDestroyed(SurfaceHolder holder) {
            if ( camera != null )
            {
                camera.stopPreview();
                camera.release();
                camera = null;
            }
            Log.e( getLocalClassName(), "END: surfaceDestroyed" );
        }
    };

    //What makes most sense is to create a class that extends SeekBar
    //have it take a have it get take in the SeekBar,TextView,and int reference
    //and then addListener(new Listener(){});
    //and have the listener change those values
    //im not thinking straight anymore
    private class MySeekBar extends SeekBar{
    	
		public MySeekBar(Context context) {
			super(context);
			// TODO Auto-generated constructor stub
		}
    	
    }
    private class SeekBarListener implements OnSeekBarChangeListener{
    	int v;
    	public SeekBarListener(int v){
    		this.v=v;
    	}
    	public void onProgressChanged(SeekBar seekBar, int progress,boolean fromUser){
    		// TODO Auto-generated method stub
    		if(v==R.id.Brightness){
	    		BValue.setText("Y: "+progress);
	    		bval=progress;
    		}
    		else if(v==R.id.Blue){
    			BlueValue.setText("B: "+progress);
    			blueval=progress;
    		}
    	}
    	public void onStartTrackingTouch(SeekBar seekBar){
        
    	}
    	public void onStopTrackingTouch(SeekBar seekBar){
    		// TODO Auto-generated method stub
    	}
    }
    
    
    public static int getBrightness(){
    	return bval;
    }
    
    public static int getBlue(){
    	return blueval;
    }
    
    public static void setOldPreviewFrame(byte[] frame){
    	MainActivity.oldPreviewFrame=frame;
    }
    public static byte[] getOldPreviewFrame(){
    	return MainActivity.oldPreviewFrame;
    }
    
    public static byte[] getFromListHead(){
    	return frames.getFirst();
    }
    
    public static byte[] popFromListHead(){
    	return frames.pop();
    }
    
    public static boolean isListEmpty(){
    	Log.d(null,"LIST IS EMPTY");
    	if(frames.size()>1)
    		return false;
    	else
    		return true;
    }
}
