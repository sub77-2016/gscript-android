package nl.rogro.GScript;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

public class GScriptAdd extends Activity {

	SQLiteDatabase mDatabase;
	EditText EditTextName;
	EditText EditTextScript;
	CheckBox CheckboxSu;
	protected static final int ACTIVITY_LOAD_FILE = 9999;
	
	//Activity load file
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if(requestCode==ACTIVITY_LOAD_FILE && resultCode==Activity.RESULT_OK)
		{
			String loadFileName = "";
			
			try {

			loadFileName = "/sdcard/" + data.getStringExtra("FileToLoad");
			String Script = "";
			
  	        BufferedReader in = new BufferedReader(new FileReader(loadFileName));
  	        String str;
  	        
  	        while ((str = in.readLine()) != null) {
  	        	Script += str;
  	        }
  	        in.close();
			
			EditTextScript.setText(Script.toString());
			

			} catch (Exception ex) {
				Toast toast = Toast.makeText(this, "Error while loading script file " + loadFileName, Toast.LENGTH_LONG);
				toast.show();
			}
		}
	}
	
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
		setContentView(R.layout.additem);

        DatabaseHelper hlp = new DatabaseHelper(this);                 
        mDatabase = hlp.getWritableDatabase(); 

        EditTextName = (EditText)findViewById(R.id.AddName);
        EditTextScript = (EditText)findViewById(R.id.AddScript);
        CheckboxSu = (CheckBox)findViewById(R.id.AddSu);
        
		Button btnCancel = (Button) findViewById(R.id.ButtonAddCancel);
		btnCancel.setOnClickListener(new OnClickListener() {

			public void onClick(View v) {
				setResult(android.app.Activity.RESULT_CANCELED);
				finish();
			}

		});
		
		Button btnLoadFile = (Button) findViewById(R.id.ButtonAddLoad);
		btnLoadFile.setOnClickListener(
				new OnClickListener()
				{
					public void onClick(View v) {
		                Intent i = new Intent(GScriptAdd.this, GScriptLoad.class);
		                startActivityForResult(i, ACTIVITY_LOAD_FILE);
					}
				}
		);
		
		
		Button btnSave = (Button) findViewById(R.id.ButtonAddSave);
		btnSave.setOnClickListener(

				new OnClickListener()
				{
					public void onClick(View v) {
						
						String Name = EditTextName.getText().toString();
						String Script = EditTextScript.getText().toString();
						
						boolean SU = CheckboxSu.isChecked();
						short NeedsSU = 0;

						if(SU)
						{
							NeedsSU = 1;
						}
						
				        ContentValues insertValues = new ContentValues();
				        insertValues.put("name", Name);
				        insertValues.put("script", Script);
				        insertValues.put("su", NeedsSU);
				        
				        mDatabase.insert("scripts", "", insertValues);

						Toast toast = Toast.makeText(GScriptAdd.this, "New script added", Toast.LENGTH_LONG);
					    toast.show();
				        
				        setResult(android.app.Activity.RESULT_OK);
						finish();
					}
				}
		);
		
	}

}
