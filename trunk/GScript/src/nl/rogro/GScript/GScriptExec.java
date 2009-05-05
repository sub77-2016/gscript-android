package nl.rogro.GScript;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
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

	String processName = "";
	String processScript = "";
	Button ButtonExecClose = null;
	EditText EditTextTerm = null;
	
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
		
		EditTextTerm.setVerticalScrollBarEnabled(true);

		EditTextTerm.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				AutoClose = false;
			}
		});

		ButtonExecClose = (Button) findViewById(R.id.ButtonExecClose);
		ButtonExecClose.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {

				executeThread.stop();
				
				try
				{
					process.destroy();
				} catch(Exception e){}
				
				setResult(Activity.RESULT_OK);
				finish();
			}
		});

		try {

			String Script = eCursor.getString(2);
			processScript = Script;

			short Su = eCursor.getShort(3);

			eCursor.close();
			mDatabase.close();
			hlp.close();
			
			processName = "sh";

			switch (Su) {
			case 0:
				processName = "sh";
				break;
			case 1:
				processName = "su";
				break;
			}

			scriptOutput = "";

			EditTextTerm.setDrawingCacheEnabled(false);
			EditTextTerm.setText("Script execution started.");
			
			executeThread = new ExecuteThread();
			executeThread.start();

		} catch (Exception e) {
			EditTextTerm.setText("Error: \n\n" + e.getMessage());
		}

	}

	Boolean AutoClose = true;
	int AutoCloseSeconds = 3;
	Timer AutoCloseTimer = new Timer();

	class AutoCloseTimerTask extends TimerTask {
		@Override
		public void run() {
			if (AutoClose) {
				AutoCloseSeconds--;
				messageHandler.sendMessage(Message.obtain(messageHandler,
						ExecuteUpdateButton, "Close ( Auto closing in "
								+ AutoCloseSeconds + " seconds )"));

				if (AutoCloseSeconds == 0) {
					executeThread.stop();

					try
					{
						process.destroy();
					} catch(Exception e){}					
					
					setResult(Activity.RESULT_OK);
					finish();
				}
			} else {
				messageHandler
						.sendMessage(Message.obtain(messageHandler,
								ExecuteUpdateButton,
								"Close ( Auto closing canceled )"));
				this.cancel();
			}
		}
	}

	private Handler messageHandler = new Handler() {

		@Override
		public void handleMessage(Message msg) {
			
			if (msg.what == ExecuteResponse) {

				if(scriptOutput.length() > 500)
				{
					scriptOutput = scriptOutput.substring(scriptOutput.length()-500, scriptOutput.length());
				}
				
				String termText = scriptOutput;

				if(EditTextTerm.getText().toString()!=termText)
					EditTextTerm.setText(termText);
				
			}
			
			if (msg.what == ExecuteFinished) {

				EditTextTerm.setText(EditTextTerm.getText().toString()
						+ "\nScript finished!");

				AutoCloseTimer.schedule(new AutoCloseTimerTask(), 1000, 1000);
			}
			
			if (msg.what == ExecuteUpdateButton) {
				ButtonExecClose.setText(msg.obj.toString());
			}

			super.handleMessage(msg);
		}

	};

	DataOutputStream stdin = null;
	DataInputStream stdout = null;
	DataInputStream stderr = null;
	
	String scriptOutput = "";
	Boolean scriptRunning = false;
	Boolean stdoutFinished = false;
	Boolean stderrFinished = false;

	Thread stdoutThread = null;
	Thread stderrThread = null;
	Thread stdinThread = null;

	Process process = null;
	
	class ExecuteThread extends Thread {

		public void run() {
			super.setPriority(MIN_PRIORITY);
			Execute();
		}

		void Execute() {

			try {
			
				process = Runtime.getRuntime().exec(processName);
				
				stdin = new DataOutputStream(process.getOutputStream());
				stdout = new DataInputStream(process.getInputStream());
				stderr = new DataInputStream(process.getErrorStream());				
				
				stdinThread = new Thread()
				{
					public void run()
					{
						super.setPriority(MIN_PRIORITY);
						
						while(scriptRunning)
						{
							try
							{
								
							super.sleep(200);
								
							messageHandler.sendMessage(Message.obtain(messageHandler,
									ExecuteResponse, ""));
							} catch(Exception e) {}
						}
					}
				};
				
				stdoutThread = new Thread(){
					public void run()
					{						
						super.setPriority(MIN_PRIORITY);
						
						try
						{
							String line;
							while((line = stdout.readLine())!=null)
							{
								super.sleep(10);
								scriptOutput+=line+"\n";
							}
							stdoutFinished = true;
							
						} catch(Exception e){}
					}
				};
				stderrThread = new Thread(){
					public void run()
					{
						super.setPriority(MIN_PRIORITY);
						
						try
						{
						String line;
						while((line = stderr.readLine())!=null)
						{
							super.sleep(10);
							scriptOutput+="stderr: " + line+"\n";
						}
						
						stderrFinished = true;
						
						} catch(Exception e){}
					}
				};

				scriptRunning=true;
				
				stdoutThread.start();
				stderrThread.start();
				stdinThread.start();
				
				stdin.writeBytes(processScript + " \n");
				stdin.writeBytes("exit\n");
				
				stdin.flush();

				process.waitFor();
				
				while(!stdoutFinished || !stderrFinished)
				{
				}
				
				stderr.close();
				stdout.close();
				stdin.close();
				
				stdoutThread = null;
				stderrThread = null;
				stdinThread = null;
				
				messageHandler.sendMessage(Message.obtain(messageHandler,
						ExecuteResponse, ""));
				
				scriptRunning = false;

				process.destroy();				

				messageHandler.sendMessage(Message.obtain(messageHandler,
						ExecuteFinished, ""));
				
			} catch (Exception e) {
				messageHandler.sendMessage(Message.obtain(messageHandler,
						ExecuteResponse, "Error while trying to execute.."));
			}
		}

	}
}