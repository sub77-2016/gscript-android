package nl.rogro.GScript;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Timer;
import java.util.TimerTask;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class GScriptExec extends Activity {
	SQLiteDatabase mDatabase;
	ExecuteThread executeThread = new ExecuteThread();

	int ExecuteResponse = 999;
	int ExecuteFinished = 1000;
	int ExecuteUpdateButton = 1001;
	
	String processName;
	String GScript_TempFile;
	Button ButtonExecClose;
	EditText EditTextTerm;
	
	/** Database */
	protected static class DatabaseHelper extends SQLiteOpenHelper {
		public DatabaseHelper(Context context) {
			super(context, "gscript.sqlite", null, 1);
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
			String ddlScripts = "create table scripts (_id integer primary key, name text, script text, su short);";
			db.execSQL(ddlScripts);
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		}
	}

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.execitem);

		
		Toast toast;
		GScript_TempFile = this.getCacheDir() + "/gscript_tmp.sh";		
		
		final Intent intent = getIntent();
		int ScriptId = intent.getIntExtra(GScript.SCRIPT_KEY, 0);

		DatabaseHelper hlp = new DatabaseHelper(this);
		mDatabase = hlp.getWritableDatabase();

		Cursor eCursor = mDatabase.query(false, "scripts", new String[] {
				"_id", "name", "script", "su" }, "_id="
				+ String.valueOf(ScriptId), null, null, null, null, null);

		if (eCursor.getCount() == 0) {
			toast = Toast.makeText(this,
					"GScript:\n\nSpecified script can not be found",
					Toast.LENGTH_LONG);
			toast.show();

			finish();
		}
		
		eCursor.moveToFirst();

		EditTextTerm = (EditText) findViewById(R.id.EditTextTerm);
		
		EditTextTerm.setOnClickListener(
				new OnClickListener()
				{
					public void onClick(View v) {
						AutoClose=false;
					}
				}
		);
		
		ButtonExecClose = (Button)findViewById(R.id.ButtonExecClose);
		ButtonExecClose.setOnClickListener(
				new OnClickListener()
				{
					public void onClick(View v) {
						executeThread.stop();
						setResult(Activity.RESULT_OK);
						finish();
					}
				}
		);
		
		try {

			String Script = eCursor.getString(2);
			short Su = eCursor.getShort(3);

			processName = "sh";

			switch (Su) 
			{
			case 0:
				processName = "sh";
				break;
			case 1:
				processName = "su";
				break;
			}

			// Create temporary file
			
			FileWriter fileOutput = new FileWriter(GScript_TempFile);
			fileOutput.write(Script);
			fileOutput.flush();
			fileOutput.close();

			executeThread = new ExecuteThread();
			executeThread.GScript_TempFile = GScript_TempFile;
			executeThread.start();

		} catch (Exception e) {
			EditTextTerm.setText("Error: \n\n" + e.getMessage());
		}		

	}

	Boolean AutoClose = true;
	int AutoCloseSeconds = 3;
	Timer AutoCloseTimer = new Timer();
	class AutoCloseTimerTask extends TimerTask
	{
		@Override
		public void run() 
		{
			if(AutoClose)
			{
			AutoCloseSeconds--;
			messageHandler.sendMessage(Message.obtain(messageHandler, ExecuteUpdateButton, "Close ( Auto closing in " + AutoCloseSeconds + " seconds )")); 
			
			if(AutoCloseSeconds==0)
			{
				executeThread.stop();
				setResult(Activity.RESULT_OK);
				finish();
			}
			} else {
				messageHandler.sendMessage(Message.obtain(messageHandler, ExecuteUpdateButton, "Close ( Auto closing canceled )"));
				this.cancel();
			}
		}
	}
	
	private Handler messageHandler = new Handler() {
	    
		@Override
		public void handleMessage(Message msg) 
		{
	    	  if(msg.what==ExecuteResponse)
	    	  {
				EditTextTerm.setText(msg.obj.toString() + "\n");
				EditTextTerm.scrollTo(0, EditTextTerm.getLineCount()*EditTextTerm.getLineHeight());
	    	  }
	    	  if(msg.what==ExecuteFinished)
	    	  {

	    		EditTextTerm.setText(EditTextTerm.getText().toString()+"\nScript finished!");
				EditTextTerm.scrollTo(0, EditTextTerm.getLineCount()*EditTextTerm.getLineHeight());
				
				AutoCloseTimer.schedule(new AutoCloseTimerTask(), 1000, 1000);
	    	  }
	    	  if(msg.what==ExecuteUpdateButton)
	    	  {
	    		ButtonExecClose.setText(msg.obj.toString());
	    	  }
		 	  
	    	  super.handleMessage(msg);
		}

	  };	
	
	class ExecuteThread extends Thread {

	public String GScript_TempFile;
		
	public void run() {
		Execute();
	}

	void Execute() {
		try {
			messageHandler.sendMessage(Message.obtain(messageHandler, ExecuteResponse, "Thread started")); 
			
			Class<?> execClass = Class.forName("android.os.Exec");
			Method createSubprocess = execClass.getMethod("createSubprocess",
					String.class, String.class, String.class, int[].class);
			Method waitFor = execClass.getMethod("waitFor", int.class);

			
			int[] pid = new int[1];
			FileDescriptor fd = (FileDescriptor) createSubprocess.invoke(null,
					"/system/bin/" + processName, this.GScript_TempFile, null, pid);

			FileInputStream in = new FileInputStream(fd);
			BufferedReader reader = new BufferedReader(
					new InputStreamReader(in));
			
			String output = "";
			
			try {
				String line;
				while ((line = reader.readLine()) != null) {
					output += line + "\n";
					messageHandler.sendMessage(Message.obtain(messageHandler, ExecuteResponse, output)); 
				}

			} catch (IOException e) {
			}

			waitFor.invoke(null, pid[0]);

			messageHandler.sendMessage(Message.obtain(messageHandler, ExecuteFinished, "")); 
			
			FileOutputStream out = new FileOutputStream(fd);
			BufferedWriter writer = new BufferedWriter(
					new OutputStreamWriter(out));
			
			writer.write("exit \n");
						
			writer.close();
			reader.close();
			
			out.close();
			in.close();
			
			
		} catch (ClassNotFoundException e) {
			throw new RuntimeException(e.getMessage());
		} catch (SecurityException e) {
			throw new RuntimeException(e.getMessage());
		} catch (NoSuchMethodException e) {
			throw new RuntimeException(e.getMessage());
		} catch (IllegalArgumentException e) {
			throw new RuntimeException(e.getMessage());
		} catch (IllegalAccessException e) {
			throw new RuntimeException(e.getMessage());
		} catch (InvocationTargetException e) {
			throw new RuntimeException(e.getMessage());
		} catch (IOException e) {
		}

	}

}
}