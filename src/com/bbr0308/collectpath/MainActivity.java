package com.bbr0308.collectpath;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import com.example.testfile.R;

import android.os.Bundle;
import android.os.Environment;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

public class MainActivity extends Activity {
	private HashMap<String, String> mPkgAppMap = new HashMap<String, String>();
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		File rootFile = Environment.getExternalStorageDirectory();
		cleanPhoneStorage(rootFile);
		
		new CollectThread(this, rootFile).start();
	}

	List<String> initPackages() {
		PackageManager pm = getPackageManager();
		List<String> pkgNames = new ArrayList<String>();
		List<PackageInfo> l = pm.getInstalledPackages(0);
		int len = l.size();
		
		for (int i = 0; i < len; i++) {
			PackageInfo pi = l.get(i);
			if ((pi.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) == 0 && 
					((pi.applicationInfo.flags & ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) == 0)) {
				pkgNames.add(pi.packageName);
				System.out.println("zhangcheng*****************pkg " + pi.packageName);
				mPkgAppMap.put(pi.packageName, (String) pi.applicationInfo.loadLabel(pm));	
			}
		}
		
		return pkgNames;
	}
	
	void collectPath(FileWriter fw, File f) {
		File[] fs = f.listFiles();
		
		if (null != fs) {
			try {
				System.out.println("zhangcheng===============collectPath " + f.getAbsolutePath());
				int len = fs.length;
				for (int i = 0; i < len; i++) {
					File tmp = fs[i];
					if (tmp.isFile()) {
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
	
	void cleanPhoneStorage(File f) {
		File[] fs = f.listFiles();
		
		if (null != fs) {
			System.out.println("zhangcheng===============cleanPhoneStorage " + f.getAbsolutePath());
			int len = fs.length;
			for (int i = 0; i < len; i++) {
				File tmp = fs[i];
				if (tmp.isFile()) {
					if (!tmp.getAbsolutePath().equals(Environment.getExternalStorageDirectory().getAbsolutePath() + "/collect.txt")) {
						tmp.delete();	
					}
				} else if (tmp.isDirectory()) {
					cleanPhoneStorage(tmp);
				}
			}
			
			f.delete();
		}
	}
	
	void writePkgAppMap(FileWriter fw) {
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
		private File mRootFile;
		
		public CollectThread(Context cxt, File f) {
			// TODO Auto-generated constructor stub
			mContext = cxt;
			mRootFile = f;
		}
		
		@SuppressLint("Wakelock")
		@Override
		public void run() {
			// TODO Auto-generated method stub
			super.run();
			PowerManager powm = (PowerManager) mContext.getSystemService(Context.POWER_SERVICE);
			WakeLock wl = powm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "collectPath");
			wl.acquire();
			
			File uFile = new File(mRootFile.getAbsolutePath() + "/collect.txt");
			FileWriter fw = null;
			
			try {
				fw = new FileWriter(uFile);
				List<String> l = initPackages();
				int len = l.size();
				
				for (int i = 0; i < len; i++) {
					try {
						String pkgName = l.get(i);
						System.out.println("zhangcheng>>>>>>>>>>>>>pkgName " + pkgName);
						Process p = Runtime.getRuntime().exec("monkey -p " + pkgName + " 1000");
						p.waitFor();
						fw.write(pkgName);
						fw.write("\n");
						collectPath(fw, mRootFile);
						fw.write("===================================================================");
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
			
			wl.release();
		}
	}
}
