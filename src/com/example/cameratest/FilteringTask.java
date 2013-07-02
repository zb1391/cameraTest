package com.example.cameratest;

import java.io.ByteArrayOutputStream;
import java.lang.ref.WeakReference;
import java.nio.IntBuffer;
import java.util.LinkedList;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.hardware.Camera.Size;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.ImageView;

public class FilteringTask extends AsyncTask<Void,Void,Bitmap>{
	//private final WeakReference<ImageView> imageViewReference;
	public static final int DIFFERENCE = 0;
	public static final int BLUE = 1;
	private static final int THREADCOUNT=2;
	private static int filterType=0;
	private ImageView imageViewReference;
	private Canvas canvas;
	private Size previewSize;
	private Camera.Parameters parameters;
	
	//For MultiThreading
	//I need to fix this tho
	//I cant finish the doInBackground() until all threads finish processing
	//so i need to change something to handle this
	//i need to read more about threads first
	private LinkedList<int[]> PixelList;
	private ParallelListener mlistener;
	private int curThread;
	private boolean isComplete;
	public FilteringTask(ImageView view,Size size,Camera.Parameters p){
		imageViewReference=view;
		canvas = new Canvas();
		this.previewSize=size;
		parameters = p;
		PixelList = new LinkedList<int[]>();
		curThread=0;
		isComplete=false;
		mlistener = new ParallelListener(){
			@Override
			public void onThreadComplete() {
				// TODO Auto-generated method stub
				curThread++;
				if(curThread==THREADCOUNT){
					// TODO combine into one array
					isComplete=true;
				}
			}
			
		};
	}
	@Override
	protected Bitmap doInBackground(Void... arg0) {
		// TODO Auto-generated method stub
		Bitmap bm = Bitmap.createBitmap(previewSize.height, previewSize.width, Bitmap.Config.ARGB_8888);
		if(MainActivity.isListEmpty()==false){
			Log.e(null,"filtertype is "+filterType);
			/*byte[] data = MainActivity.popFromListHead();
			int start,finish;
			start=0;
			finish=0;
			for(int i=0;i<THREADCOUNT;i++){
				PixelList.add(new int[previewSize.width*previewSize.height/THREADCOUNT]);
				finish=finish+previewSize.height/THREADCOUNT;
				ParallelFilter thread = new ParallelFilter(i,start,finish,data,PixelList.getLast());
				thread.setParallelListener(mlistener);
				Thread t = new Thread(thread);
				t.run();
			}
			while(isComplete==false){};
			int[] allPixels = merge(PixelList);
			canvas.setBitmap(bm);
			canvas.drawBitmap(allPixels,0,bm.getHeight(),0,0,bm.getHeight(),bm.getWidth(),true,new Paint());
			return bm;
			*/
			if(filterType==DIFFERENCE)
				return differenceFilter(bm);
			else if(filterType==BLUE)
				return blueFilter2(bm);
		}
			
		
		return bm;
	}
	
	protected void onPostExecute(Bitmap bm){
	       if (imageViewReference != null && bm != null) {
	            
	                imageViewReference.setImageBitmap(bm);
	            
	        }
	}
	
	private Bitmap blueFilter2(Bitmap bm){
		int format = parameters.getPreviewFormat();
		byte[] data = MainActivity.popFromListHead();
		if(format == ImageFormat.NV21){
			YuvImage yuv = new YuvImage(data,format,previewSize.width,
					previewSize.height,null);
			Rect rect = new Rect( 0, 0, previewSize.width, previewSize.height );
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            yuv.compressToJpeg( rect, 100, outputStream );
            bm = BitmapFactory.decodeByteArray( outputStream.toByteArray(), 0, outputStream.size() );
        
        }
		 else if ( format == ImageFormat.JPEG || format == ImageFormat.RGB_565 ){
			 
             bm = BitmapFactory.decodeByteArray( data, 0, data.length );
         }
		Bitmap bitmap = bm.copy(bm.getConfig(), true);
		for(int i=0;i<bitmap.getWidth();i++)
			for(int j=0;j<bitmap.getHeight();j++){
				int pixel = bitmap.getPixel(i, j);
				//Extract Y-value from YUV data
				int Y = data[j*previewSize.width +i] & 0xff;
				if(Y>MainActivity.getBrightness())
					bitmap.setPixel(i, j, Color.BLACK);
				else
					if(Color.blue(pixel)>MainActivity.getBlue())
						bitmap.setPixel(i, j, Color.BLACK);
					/*else
						bitmap.setPixel(i, j, Color.BLUE);*/
			}
		return bitmap;
		
	}

	private Bitmap blueFilter(Bitmap bm){
		byte[] data = MainActivity.popFromListHead();
		int numPixels = previewSize.width*previewSize.height;
		// the buffer we fill up which we then fill the bitmap with
		IntBuffer intBuffer = IntBuffer.allocate(numPixels);
		// If you're reusing a buffer, next line imperative to refill from the start,
		// if not good practice
		intBuffer.position(0);
		final byte alpha = (byte) 255;
		
		// Get each pixel, one at a time
		for (int y = 0; y < previewSize.height; y++) {
		    for (int x = 0; x < previewSize.width; x++) {
		        // Get the Y value, stored in the first block of data
		        // The logical "AND 0xff" is needed to deal with the signed issue
		        int Y = data[y*previewSize.width + x] & 0xff;

		        // Get U and V values, stored after Y values, one per 2x2 block
		        // of pixels, interleaved. Prepare them as floats with correct range
		        // ready for calculation later.
		        int xby2 = x/2;
		        int yby2 = y/2;
		        float U = (float)(data[numPixels + 2*xby2 + yby2*previewSize.width] & 0xff) - 128.0f;
		        float V = (float)(data[numPixels + 2*xby2 + 1 + yby2*previewSize.width] & 0xff) - 128.0f;
		        // Do the YUV -> RGB conversion
		        float Yf = 1.164f*((float)Y) - 16.0f;
		        int R = (int)(Yf + 1.596f*V);
		        int G = (int)(Yf - 0.813f*V - 0.391f*U);
		        int B = (int)(Yf            + 2.018f*U);

		        // Clip rgb values to 0-255
		        R = R < 0 ? 0 : R > 255 ? 255 : R;
		        G = G < 0 ? 0 : G > 255 ? 255 : G;
		        B = B < 0 ? 0 : B > 255 ? 255 : B;

		        // Put that pixel in the buffer
		        intBuffer.put(alpha*16777216 + R*65536 + G*256 + B);
		    }
		}
		// Get buffer ready to be read
		intBuffer.flip();

		// Push the pixel information from the buffer onto the bitmap.
		bm.copyPixelsFromBuffer(intBuffer);
		canvas.setBitmap(bm);
		canvas.drawBitmap(intBuffer.array(),0,bm.getHeight(),0,0,bm.getHeight(),bm.getWidth(),true,new Paint());
		return bm;
	}
	
	private Bitmap differenceFilter(Bitmap bm){
		byte frame1[] = MainActivity.popFromListHead();
		byte frame2[] = MainActivity.getFromListHead();
		if(frame1.length!=frame2.length){
			Log.e(null,"ERROR buffer sizes dont match");
			return null;
		}
		else{
			int difference[] = new int[frame1.length];
			Log.i(null,"Subtracting the two frames");
			int diff;
			for(int i =0; i<frame1.length;i++){
				diff= (Math.abs(frame2[i]-frame1[i]));
				difference[i]=Color.rgb(diff,diff,diff);
			}
			canvas.setBitmap(bm);
			canvas.drawBitmap(difference,0,bm.getHeight(),0,0,bm.getHeight(),bm.getWidth(),true,new Paint());
			return bm;
		
		}
	}
	
	public static void setFilter(int type){
			filterType=type;
	}
	
	public static void setFilter(){
		if(filterType==DIFFERENCE)
			filterType=BLUE;
		else if(filterType==BLUE)
			filterType=DIFFERENCE;
		Log.d(null,"filterType is now "+filterType);
	}
	
	private interface ParallelListener{
		public void onThreadComplete();
	}
	
	private class ParallelFilter implements Runnable{
		private int id;
		private Bitmap bm;
		private int start;
		private int finish;
		private byte[] data;
		private int[] pixels;
		private ParallelListener listener;
		public ParallelFilter(int id,int start, int finish,byte[] data,int[] pixels){
			this.id=id;
			this.data=data;
			this.start=start;
			this.finish=finish;
			this.pixels=pixels;
		}
		
		public void setParallelListener(ParallelListener listener){
			this.listener=listener;
		}
		
		@Override
		public void run() {
			// TODO Auto-generated method stub
			int format = parameters.getPreviewFormat();
			YuvImage yuv = new YuvImage(data,format,previewSize.width,
					(finish-start),null);
			Rect rect = new Rect( 0, 0, previewSize.width, (finish-start) );
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            yuv.compressToJpeg( rect, 100, outputStream );
            bm = BitmapFactory.decodeByteArray( outputStream.toByteArray(), 0, outputStream.size() );
       
			Bitmap bitmap = bm.copy(bm.getConfig(), true);
			for(int i=0;i<bm.getWidth();i++){
				for(int j=start;j<finish;j++){
					int pixel = bitmap.getPixel(i, j);
					//Extract Y-value from YUV data
					int Y = data[j*previewSize.width +i] & 0xff;
					if(Y>MainActivity.getBrightness())
						bitmap.setPixel(i, j, Color.BLACK);
					else
						if(Color.blue(pixel)>MainActivity.getBlue())
							bitmap.setPixel(i, j, Color.BLACK);
				}
			}
			bitmap.getPixels(pixels, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());
			//now i have the pixels for for part of the bitmap(0-bm.width,start-finish)
			//i need to decide what to do with these pixels from the sub threads
			//once all processing finishes, the FilteringTask needs to store them
			//i think parallelFilter should take in a pixel array with the contructor
			//this array will be from a linked list created by filtering task
			//then the thread will update this node of the int[] linked list
			
			//once all threads finish i combine them
			//so do something like thread.onComplete()
			listener.onThreadComplete();
		}
		
	}
	
	public int[] merge(LinkedList<int[]> arrays) {
	    int size = 0;
	    for (int i=0;i<arrays.size();i++ )
	    	size+=arrays.get(i).length;

	        int[] res = new int[size];

	        int destPos = 0;
	        for ( int i = 0; i < arrays.size(); i++ ) {
	            if ( i > 0 ) 
	            	destPos += arrays.get(i-1).length;
	            int length = arrays.get(i).length;
	            System.arraycopy(arrays.get(i), 0, res, destPos, length);
	        }

	        return res;
	}
}
