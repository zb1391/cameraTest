package com.example.cameratest;

import java.util.Iterator;
import java.util.List;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.Paint;
import android.hardware.Camera;
import android.hardware.Camera.PreviewCallback;
import android.hardware.Camera.Size;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.ImageView;

public class CameraPreview extends SurfaceView implements SurfaceHolder.Callback, Camera.PreviewCallback{

	public SurfaceHolder mHolder;
	private Camera mCamera;
	private Bitmap grayBitmap;
	private Camera.Parameters parameters;
    private int prevY;
    private int prevX;
    private byte[] oldPreviewFrame;
	private int[] frameDifference;
	public Canvas canvas;
	private Paint paint;
	private static int prevSize;
	
	private ImageView differenceImage;
	public CameraPreview(Context c, Camera cam){
		super(c);
		mCamera = cam;
		
		mHolder = getHolder();
		mHolder.addCallback(this);
        // deprecated setting, but required on Android versions prior to 3.0
        mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
		canvas = new Canvas();
		paint = new Paint();
	}


	
	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
		// TODO Auto-generated method stub
		if(mCamera!=null){
			mCamera.setPreviewCallback(new PreviewCallback(){

				@Override
				public void onPreviewFrame(byte[] data, Camera camera) {
					// TODO Auto-generated method stub
					if(mCamera!=null){
						Log.d(null,"onPreviewFrame");
					}
				}
				
			});
		}
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		// TODO Auto-generated method stub
		
		try{
			mCamera.setPreviewDisplay(mHolder);
		}
		catch(Exception e){
			Log.e("surface created","failed to set preview display");
		}
		
		

	}

	@Override
	public void surfaceDestroyed(SurfaceHolder arg0) {
		// TODO Auto-generated method stub
		 if ( mCamera != null )
         {
             mCamera.stopPreview();
             mCamera.release();
             mCamera = null;
         }
	}



	@Override
	public void onPreviewFrame(final byte[] data, Camera camera) {
		Log.d("preview","onPreviewFrame");
		/*FilterTask filter =new FilterTask(grayBitmap,canvas,differenceImage,oldPreviewFrame,data);
		filter.setListener(new DifferenceFilter(){

			@Override
			public void onFilterComplete() {
				// TODO Auto-generated method stub
				oldPreviewFrame = data;
			}
			
		});
		filter.execute();*/
	}

	

}
