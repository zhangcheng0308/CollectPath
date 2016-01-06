package com.bbr0308.collectpath;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import android.os.Bundle;
import android.os.Environment;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

public class MainActivity extends Activity implements OnClickListener{
	private final String TAG = "CollectPKG_PATH";
	private final String mFileName = "CollectPKG_PATH.txt";
	private HashMap<String, String> mPkgAppMap = new HashMap<String, String>();
	private Button mBtn;
	private File mRootFile = Environment.getExternalStorageDirectory();;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		cleanPhoneStorage(mRootFile);
		mBtn = (Button) findViewById(R.id.btn);
		mBtn.setOnClickListener(this);
	}

	private List<String> initPackages() {
		PackageManager pm = getPackageManager();
		List<String> pkgNames = new ArrayList<String>();
		List<PackageInfo> l = pm.getInstalledPackages(0);
		int len = l.size();
		
		for (int i = 0; i < len; i++) {
			PackageInfo pi = l.get(i);
			if ((pi.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) == 0 && 
					((pi.applicationInfo.flags & ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) == 0)) {
				String pkgName = pi.packageName;
				if (!pkgName.equalsIgnoreCase("com.bbr0308.collectpath") && 
						!pkgName.equalsIgnoreCase("com.android.bbk.lockscreen3")) {
					pkgNames.add(pkgName);
					System.out.println("zhangcheng*****************pkg " + pi.packageName);
					mPkgAppMap.put(pi.packageName, (String) pi.applicationInfo.loadLabel(pm));	
				}	
			}
		}
		
		return pkgNames;
	}
	
	private void collectPath(FileWriter fw, File f) {
		File[] fs = f.listFiles();
		
		if (null != fs) {
			try {
				System.out.println("zhangcheng===============collectPath " + f.getAbsolutePath());
				int len = fs.length;
				for (int i = 0; i < len; i++) {
					File tmp = fs[i];
					if (tmp.isFile() && !tmp.getName().equals(mFileName)) {
						fw.write(tmp.getAbsolutePath());
						fw.write("\n");
					} else if (tmp.isDirectory()){
						fw.write(tmp.getAbsolutePath());
						fw.write("\n");
						collectPath(fw, tmp);
					}
				}
			} catch(IOException e) {
				e.printStackTrace();
			}
		}
	}

	@SuppressWarnings("unchecked")
	private void forceStopAllPackages() {
		ActivityManager mActivityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);  
		Method method;
		try {
			method = Class.forName("android.app.ActivityManager").getMethod("forceStopPackage", String.class);
			Set<String> l = (Set<String>) mPkgAppMap.keySet();
			for (String str : l) {
				method.invoke(mActivityManager, str);
			}
		} catch (NoSuchMethodException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	private void cleanPhoneStorage(File f) {
		File[] fs = f.listFiles();
		
		if (null != fs) {
			int len = fs.length;
			for (int i = 0; i < len; i++) {
				File tmp = fs[i];
				if (tmp.isFile()) {
					if (!tmp.getAbsolutePath().equals(Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + mFileName)) {
						tmp.delete();	
					}
				} else if (tmp.isDirectory()) {
					cleanPhoneStorage(tmp);
				}
			}
			
			f.delete();
		}
	}
	
	private void writePkgAppMap(FileWriter fw) {
		Set<Entry<String, String>> set = mPkgAppMap.entrySet();
		Iterator<Entry<String, String>> it = set.iterator();
		
		while (it.hasNext()) {
			Entry<String, String> entry = it.next();
			try {
				fw.write(entry.getKey() + "##" + entry.getValue());
				fw.write("==");
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	class CollectThread extends Thread{
		private Context mContext;
		
		public CollectThread(Context cxt) {
			// TODO Auto-generated constructor stub
			mContext = cxt;
		}
		
		@SuppressLint("Wakelock")
		@Override
		public void run() {
			// TODO Auto-generated method stub
			super.run();
			PowerManager powm = (PowerManager) mContext.getSystemService(Context.POWER_SERVICE);
			WakeLock wl = powm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "collectPath");
			wl.acquire();
			
			File uFile = new File(mRootFile.getAbsolutePath() + "/" + mFileName);
			FileWriter fw = null;
			
			try {
				fw = new FileWriter(uFile);
				List<String> l = initPackages();
				int len = l.size();
				
				for (int i = 0; i < len; i++) {
					try {
						forceStopAllPackages();
						String pkgName = l.get(i);
						System.out.println("zhangcheng>>>>>>>>>>>>>pkgName " + pkgName);
						Process p = Runtime.getRuntime().exec("monkey -p " + pkgName + " 1000");
						p.waitFor();
						fw.write(pkgName);
						fw.write("\n\n");
						collectPath(fw, mRootFile);
						fw.write("===================================================================\n");
						cleanPhoneStorage(mRootFile);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				
				writePkgAppMap(fw);
				fw.flush();
				fw.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			updateUI();
			wl.release();
		}
	}

	private void updateUI() {
		runOnUiThread(new Runnable() {

			@Override
			public void run() {
				// TODO Auto-generated method stub
				mBtn.setText(getText(R.string.end_collect));
				mBtn.setEnabled(true);
				ComponentName cn = new ComponentName(MainActivity.this, "com.bbr0308.collectpath.MainActivity");
				Intent i = new Intent();
				i.setComponent(cn);
				startActivity(i);
			}
			
		});
	}
	
	@Override
	public void onClick(View arg0) {
		// TODO Auto-generated method stub
		switch (arg0.getId()) {
			case R.id.btn:
				mBtn.setText(getText(R.string.collecting));
				mBtn.setEnabled(false);
				new CollectThread(this).start();
				break;
			default:
				break;
		}
	}
}
