package nl.rogro.GScript;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
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
	String processScript;
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

		EditTextTerm.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				AutoClose = false;
			}
		});

		ButtonExecClose = (Button) findViewById(R.id.ButtonExecClose);
		ButtonExecClose.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				executeThread.stop();
				setResult(Activity.RESULT_OK);
				finish();
			}
		});

		try {

			String Script = eCursor.getString(2);
			processScript = Script;

			short Su = eCursor.getShort(3);

			processName = "sh";

			switch (Su) {
			case 0:
				processName = "sh";
				break;
			case 1:
				processName = "su";
				break;
			}

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
				EditTextTerm.setText(msg.obj.toString() + "\n");
				EditTextTerm.scrollTo(0, EditTextTerm.getLineCount()
						* EditTextTerm.getLineHeight());
			}
			if (msg.what == ExecuteFinished) {

				EditTextTerm.setText(EditTextTerm.getText().toString()
						+ "\nScript finished!");
				EditTextTerm.scrollTo(0, EditTextTerm.getLineCount()
						* EditTextTerm.getLineHeight());

				AutoCloseTimer.schedule(new AutoCloseTimerTask(), 1000, 1000);
			}
			if (msg.what == ExecuteUpdateButton) {
				ButtonExecClose.setText(msg.obj.toString());
			}

			super.handleMessage(msg);
		}

	};

	class ExecuteThread extends Thread {

		public void run() {
			Execute();
		}

		void Execute() {

			messageHandler.sendMessage(Message.obtain(messageHandler,
					ExecuteResponse, "Script executing started..."));

			Process process = null;
			DataOutputStream outputstream = null;
			DataInputStream inputstream = null;

			try {

				process = Runtime.getRuntime().exec(processName);
				outputstream = new DataOutputStream(process.getOutputStream());
				inputstream = new DataInputStream(process.getInputStream());

				outputstream.writeBytes(processScript + " \n");
				outputstream.writeBytes("exit\n");
				outputstream.flush();

				String output = "";

				try {
					String line;
					while ((line = inputstream.readLine()) != null) {
						output += line + "\n";
						messageHandler.sendMessage(Message.obtain(
								messageHandler, ExecuteResponse, output));
					}

				} catch (IOException e) {
					messageHandler.sendMessage(Message.obtain(messageHandler,
							ExecuteResponse,
							"Error while trying to read input stream..."));
				}

				process.waitFor();

			} catch (Exception e) {
				messageHandler.sendMessage(Message.obtain(messageHandler,
						ExecuteResponse, "Error while trying to execute.."));
			} finally {
				try {
					if (outputstream != null)
						outputstream.close();
					if (inputstream != null)
						inputstream.close();
				} catch (IOException e) {
				}
				
				process.destroy();
				
				messageHandler.sendMessage(Message.obtain(messageHandler,
						ExecuteFinished, ""));
			}

		}

	}
}