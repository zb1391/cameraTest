package com.example.cameratest;

import java.util.Iterator;
import java.util.List;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.Paint;
import android.hardware.Camera;
import android.hardware.Camera.Size;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.ImageView;

public class FilterTask extends AsyncTask<Void, Void, FilterDraw>{

	private Bitmap bm;
	private Canvas canvas;
	private ImageView differenceView;
	private byte frame1[];
	private byte frame2[];
	private int difference[];
	private Camera mCamera;
	private FilterDraw fd;
	private DifferenceFilter listener;
	private boolean isInBackground = false;
	public FilterTask(Camera camera,byte[] data, ImageView differencev){
		mCamera=camera;
		frame2=data;
		canvas = new Canvas();
		Log.e(null,"finished creating ");
		differenceView = differencev;

		
        
	}
	
	public void setListener(DifferenceFilter f){
		listener=f;
	}
	
	public boolean isInBackground(){
		return isInBackground;
	}
	
	@Override
	protected FilterDraw doInBackground(Void... arg0) {
		isInBackground=true;
		Camera.Parameters parameters = mCamera.getParameters();
		Log.e(null,"got Parameters");
        List<Size> supportedPreviewSizes=parameters.getSupportedPreviewSizes();
        Iterator<Size> supportedPreviewSizesIterator=supportedPreviewSizes.iterator();
        while(supportedPreviewSizesIterator.hasNext()){
            Size tmpSize=supportedPreviewSizesIterator.next();
            Log.v("CameraTest","supportedPreviewSize.width = "+tmpSize.width+"supportedPreviewSize.height = "+tmpSize.height);
        }
        
		Size previewSize=mCamera.getParameters().getPreviewSize();
		int dataBufferSize=(int)(previewSize.height*previewSize.width*
                (ImageFormat.getBitsPerPixel(mCamera.getParameters().getPreviewFormat())/8.0));
		frame1 = MainActivity.getOldPreviewFrame();
		if(frame1==null){
			frame1 = new byte[dataBufferSize];
		}
		difference = new int[dataBufferSize];

		
		bm = Bitmap.createBitmap(previewSize.width, previewSize.height, Bitmap.Config.ARGB_8888);
		//canvas.setBitmap(bm);
		
		//differenceView.setImageBitmap(bm);
		
	
		if(frame2.length!=frame1.length){
			Log.e(null,"buffer sizes dont match");
		}
		else{
			difference = new int[frame1.length];
			int diff;
			Log.i(null,"Subtracting the two frames");
			for(int i =0; i<frame1.length;i++){
				diff= (Math.abs(frame2[i]-frame1[i]));
				difference[i]=Color.rgb(diff,diff,diff);
			}
			//canvas.drawBitmap(difference,0,bm.getWidth(),0,0,bm.getWidth(),bm.getHeight(),true,new Paint());
		}
		fd = new FilterDraw(canvas,bm,difference);
		return fd;
	}

	protected void onPostExecute(FilterDraw x){
		Log.e(null,"finished FilterTask!");
		listener.onFilterComplete();
		
	}
}
