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
	public static int THREADCOUNT=1;
	private static int filterType=0;
	public static long total=0;
	private ImageView imageViewReference;
	private Canvas canvas;
	private Size previewSize;
	private Camera.Parameters parameters;
	
	private static byte[] SI=null;
	private static byte[] BG=null;
	private final int Fth=6;
	
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
		if(SI==null){
			SI = new byte[MainActivity.getFromListHead().length];
		}
		if(BG==null){
			BG = new byte[MainActivity.getFromListHead().length];
		}
		
		//For MultiThreading (doesnt work)
		/*
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
			
		};*/
	}
	@Override
	protected Bitmap doInBackground(Void... arg0) {
		// TODO Auto-generated method stub
		Bitmap bm = Bitmap.createBitmap(previewSize.height, previewSize.width, Bitmap.Config.ARGB_8888);
		if(MainActivity.isListEmpty()==false){
			Log.e(null,"filtertype is "+filterType);
			
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
	
	private void parallelStuff(){
		Bitmap bm = Bitmap.createBitmap(previewSize.height, previewSize.width, Bitmap.Config.ARGB_8888);
		byte[] data = MainActivity.popFromListHead();
		int start,finish;
		start=0;
		finish=0;
		long startTime = System.currentTimeMillis();
		for(int i=0;i<THREADCOUNT;i++){
			//PixelList.add(new int[previewSize.width*previewSize.height/THREADCOUNT]);
			finish=finish+previewSize.height/THREADCOUNT;
			ParallelFilter thread = new ParallelFilter(i,start,finish,data);
			thread.setParallelListener(mlistener);
			Thread t = new Thread(thread);
			t.run();
			Log.d("THREADS","started thread "+i);
		}
		while(isComplete==false){};
		Log.d(null,"finished all threads");
		int[] allPixels = merge(PixelList);
		canvas.setBitmap(bm);
		canvas.drawBitmap(allPixels,0,bm.getHeight(),0,0,bm.getHeight(),bm.getWidth(),true,new Paint());
		long endTime = System.currentTimeMillis();
		total = endTime-startTime;
		Log.i(null,"TOTAL TIME: "+total);
	}
	

	
	private Bitmap blueFilter2(Bitmap bm){
		long starttime = System.currentTimeMillis();
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
		long finishtime = System.currentTimeMillis();
		total = finishtime- starttime;
		return bitmap;
		
	}


	
	private Bitmap differenceFilter(Bitmap bm){
		long starttime = System.currentTimeMillis();
		byte frame1[] = MainActivity.popFromListHead();
		byte frame2[] = MainActivity.getFromListHead();
		if(frame1.length!=frame2.length){
			Log.e(null,"ERROR buffer sizes dont match");
			return null;
		}
		else{
			int difference[] = new int[frame1.length];
			int bdifference[] = new int[frame1.length];
			Log.i(null,"Subtracting the two frames");
			int diff;
			for(int i =0; i<frame1.length;i++){
				//Find FD[i]
				diff= (Math.abs(frame2[i]-frame1[i]));
				difference[i]=Color.rgb(diff,diff,diff);
				
				//Find FDM[i]
				if(Color.red(difference[i])>MainActivity.getBrightness())
					difference[i]=Color.WHITE;
				else
					difference[i]=Color.BLACK;
				
				//Update SI[i]
				if(difference[i]==Color.WHITE)
					SI[i]++;
				else
					SI[i]=0;
				
				//Update BG[i]
				if(SI[i]==Fth)
					BG[i]=frame2[i];
				
				//find BD
				bdifference[i]=Math.abs(frame2[i]-BG[i]);
				
				//find BDM
				if(Color.red(bdifference[i])>MainActivity.getBrightness())
					bdifference[i]=Color.WHITE;
				else
					bdifference[i]=Color.BLACK;
				
			}
			canvas.setBitmap(bm);
			canvas.drawBitmap(difference,0,bm.getHeight(),0,0,bm.getHeight(),bm.getWidth(),true,new Paint());
			long finishtime = System.currentTimeMillis();
			total = finishtime-starttime;
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
		private ParallelListener listener;
		public ParallelFilter(int id,int start, int finish,byte[] data){
			this.id=id;
			this.data=data;
			this.start=start;
			this.finish=finish;
			
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
            Log.d("THREADS","thread "+id+" finished decode byte array");
			Bitmap bitmap = bm.copy(bm.getConfig(), true);
			
			PixelList.add(id, new int[bitmap.getWidth()*bitmap.getHeight()]);
			
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
			Log.d("THREADS",id+" bitmap size "+(bitmap.getWidth()*bitmap.getHeight()));
			Log.d("THREADS",id+" pixel size "+PixelList.get(id).length);
			bitmap.getPixels(PixelList.get(id), 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());
			//now i have the pixels for for part of the bitmap(0-bm.width,start-finish)
			//i need to decide what to do with these pixels from the sub threads
			//once all processing finishes, the FilteringTask needs to store them
			//i think parallelFilter should take in a pixel array with the contructor
			//this array will be from a linked list created by filtering task
			//then the thread will update this node of the int[] linked list
			
			//once all threads finish i combine them
			//so do something like thread.onComplete()
			Log.d("THREADS","thread "+id+" finished processing");
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
