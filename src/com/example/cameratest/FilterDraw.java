package com.example.cameratest;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;

public class FilterDraw {
	public Canvas canvas;
	public Bitmap bm;
	public int[] difference;
	
	public FilterDraw(Canvas c,Bitmap b, int[] d){
		canvas=c;
		bm=b;
		difference=d;
	}
	
	public void draw(){
		canvas.drawBitmap(difference,0,bm.getWidth(),0,0,bm.getWidth(),bm.getHeight(),true,new Paint());

	}
	

}
