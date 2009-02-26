package nl.rogro.GScript;

import java.io.DataOutputStream;
import java.io.FileWriter;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Bundle;
import android.widget.Toast;

public class GScriptExec extends Activity {
	SQLiteDatabase mDatabase;

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
		
		Toast toast; 
		
		final Intent intent = getIntent();
        int ScriptId = intent.getIntExtra(GScript.SCRIPT_KEY, 0);
        
		DatabaseHelper hlp = new DatabaseHelper(this);                 
        mDatabase = hlp.getWritableDatabase(); 

        Cursor eCursor = mDatabase.query(false, "scripts", new String[] { "_id", "name", "script", "su" }, "_id="+String.valueOf(ScriptId), null, null, null, null, null);

        if(eCursor.getCount()==0)
        {
    		toast = Toast.makeText(this, "GScript:\n\nSpecified script can not be found", Toast.LENGTH_LONG);
    	    toast.show();
        	
        	finish();	
        }
        
        
        eCursor.moveToFirst();

		try 
		{
		String Script = eCursor.getString(2); 
		short Su = eCursor.getShort(3);
		
		toast = Toast.makeText(this, "Executing GScript:\n\n" + eCursor.getString(1), Toast.LENGTH_LONG);
	    toast.show();
	
		Process process;

		String processName = "sh";
		
		switch(Su)
		{
		case 0:
			processName = "sh";
			break;
		case 1:
			processName = "su";
			break;			
		}
		
		if(Su==1)
		{
			process = Runtime.getRuntime().exec(processName);			
		} else {
			process = Runtime.getRuntime().exec(processName);		
		}
		
		//Create temporary file		
		FileWriter fileOutput = new FileWriter("/sdcard/gscript_tmp.sh");
		fileOutput.write(Script);
		fileOutput.flush();
		fileOutput.close();
		
		//Execute file
		DataOutputStream streamOutput = new DataOutputStream(process.getOutputStream());
		streamOutput.writeBytes(processName + " /sdcard/gscript_tmp.sh \n");
		streamOutput.writeBytes("exit \n");
		streamOutput.flush();
		
		} catch (Exception e) {
			toast = Toast.makeText(this, "Error: \n\n" + e.getMessage(), Toast.LENGTH_LONG);
		    toast.show();
		}

		setResult(android.app.Activity.RESULT_OK);
		finish();
	
	}
}
