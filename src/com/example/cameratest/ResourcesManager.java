package com.example.cameratest;

import android.app.Activity;
import android.content.Context;

public class ResourcesManager {
	private static final ResourcesManager INSTANCE = new ResourcesManager();
	public Activity activity;
	public Context context;
	
	public static void prepareManager(Activity a, Context c){
		getInstance().activity=a;
		getInstance().context=c;
	}
	
	public static ResourcesManager getInstance(){
		return INSTANCE;
	}
}
