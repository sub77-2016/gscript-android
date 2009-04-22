package nl.rogro.GScript;

import java.io.File;
import java.io.FileWriter;

import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Bundle;
import android.os.Parcelable;
import android.view.*;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View.OnCreateContextMenuListener;
import android.widget.*;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;

public class GScript extends ListActivity {
	
	LayoutInflater mInflater;
	Cursor mCursor;
	SQLiteDatabase mDatabase;
	ScriptsAdapter mAdapter;
	ListView mListView;
	boolean CreateShortcut = false;

	public static final int mnuAddItem = Menu.FIRST + 1;
	public static final int mnuEditItem = Menu.FIRST + 2;
	public static final int mnuDeleteItem = Menu.FIRST + 3;
	public static final int mnuSaveItem = Menu.FIRST + 4;	
	public static final int mnuRunItem = Menu.FIRST + 5;
	public static final int mnuInfo = Menu.FIRST + 6;
	public static final int mnuExport = Menu.FIRST + 7;
	public static final int mnuImport = Menu.FIRST + 8;

	
	public static final String SCRIPT_KEY = "nl.rogro.GScript.GScript.ScriptId";
	
	@Override 
	public boolean onCreateOptionsMenu(Menu menu) {

		menu.setQwertyMode(true);
		MenuItem itmAdd = menu.add(0, mnuAddItem, 0, "Add script");
		{
			  itmAdd.setAlphabeticShortcut('a');
			  itmAdd.setIcon(R.drawable.gscript_add);
		}
		MenuItem itmInfo = menu.add(0, mnuInfo, 0, "Caution information");
		{
			  itmInfo.setAlphabeticShortcut('c');
			  itmInfo.setIcon(R.drawable.gscript_info);
		}
		/*
		MenuItem exportInfo = menu.add(0, mnuExport, 0, "Export to sdcard");
		{
			  itmInfo.setAlphabeticShortcut('e');
			  itmInfo.setIcon(R.drawable.gscript_info);
		}
		MenuItem importInfo = menu.add(0, mnuImport, 0, "Import from sdcard");
		{
			  itmInfo.setAlphabeticShortcut('i');
			  itmInfo.setIcon(R.drawable.gscript_info);
		}
		*/

		
		return super.onCreateOptionsMenu(menu);
	}

	/** when menu button option selected */
	@Override 
	public boolean onOptionsItemSelected(MenuItem item) {

		Toast toast;
		
		if(item.getItemId()==mnuAddItem)
		{
		    AddScript();
		}
		if(item.getItemId()==mnuInfo)
		{
			String CautionNotice = "";
			CautionNotice+="Caution:\n\n";
			CautionNotice+="Running scripts without knowing what they do could harm/damage your system. Handle with care...";
			
			toast = Toast.makeText(this, CautionNotice, Toast.LENGTH_LONG);
		    toast.show();
		}
		if(item.getItemId()==mnuExport)
		{
		}
		
		return super.onOptionsItemSelected(item);
	}	
	
	@Override 
	public boolean onContextItemSelected(MenuItem item) 
    {
		AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
		int itemId = Integer.valueOf(String.valueOf(info.position));

		mCursor.moveToPosition(itemId);
		int ScriptId = mCursor.getInt(0);
		
		switch(item.getItemId())
		{
		case mnuRunItem:
			ExecuteScript(ScriptId);
			break;
		case mnuDeleteItem:
			DeleteScript(ScriptId);
			break;
		case mnuEditItem:
			EditScript(ScriptId);
			break;
		case mnuSaveItem:
			SaveScript(ScriptId);
			break;
		}

		refreshCursor();
		
    	return super.onContextItemSelected(item);
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

	/** Cursor adapter */
	public class ScriptsAdapter extends CursorAdapter {

		public ScriptsAdapter(Context context, Cursor c) {
			super(context, c);
		}

		@Override
		public void bindView(View view, Context context, Cursor cursor) {

			TextView ScriptNameText = (TextView) view.findViewById(R.id.txtTitle);
			TextView ScriptDescText = (TextView) view.findViewById(R.id.txtDescription);

			String ScriptName = cursor.getString(1);
			short SU = cursor.getShort(3);
			
			String ScriptDesc = "Needs su: ";

			if(SU==0)
			{
				ScriptDesc += "false";
			} else {
				ScriptDesc += "true";
			}
			
			ScriptNameText.setText(ScriptName);
			ScriptDescText.setText(ScriptDesc);
		}

		@Override
		public View newView(Context context, Cursor cursor, ViewGroup parent) {
			View view = mInflater.inflate(R.layout.row, null);
			bindView(view, context, cursor);
			return view;
		}
	}

	private void refreshCursor() {
		mCursor = mDatabase.query(false, "scripts", new String[] { "_id", "name", "script", "su" }, null, null, null, null, null, null);
		mAdapter = new ScriptsAdapter(this, mCursor);
		setListAdapter(mAdapter);
	}

	@Override
	protected void onResume()
	{
		refreshCursor();
		super.onResume();
	}
	
	@Override
    protected void onCreate(Bundle savedInstanceState)
	{
        super.onCreate(savedInstanceState);
        
		final Intent intent = getIntent();
        final String action = intent.getAction();

        //Launched as ACTION_CREATE_SHORTCUT
        if (Intent.ACTION_CREATE_SHORTCUT.equals(action)) {
        	CreateShortcut = true;
        } else {
        	CreateShortcut = false;
        }
        //intent has SCRIPT_KEY so just launch the specified script        
        if (intent.getIntExtra(SCRIPT_KEY, 0)!=0)
        {
        	ExecuteScript(intent.getIntExtra(SCRIPT_KEY, 0));
        	setResult(RESULT_OK, intent);
        	finish();
        }
        
        setContentView(R.layout.main);
        
        mListView = (ListView) findViewById(android.R.id.list);
        mInflater = getLayoutInflater();
        
        DatabaseHelper hlp = new DatabaseHelper(this);                 
        mDatabase = hlp.getWritableDatabase(); 

        refreshCursor();
        
        //Listview Context
        mListView.setOnCreateContextMenuListener(
        		new OnCreateContextMenuListener()
        		{
					public void onCreateContextMenu(ContextMenu menu, View v,
							ContextMenuInfo menuInfo) {
						if(!CreateShortcut)
						{
							menu.add(0, mnuRunItem, 0, "Run");
						} else {
							menu.add(0, mnuRunItem, 0, "Select");
						}
						menu.add(0, mnuEditItem, 0, "Edit");
						menu.add(0, mnuDeleteItem, 0, "Delete");
						menu.add(0, mnuSaveItem, 0, "Save to SD");
						menu.add(0, 999, 0, "Cancel");

					}
        		}
        );
        
        //Listview clicked
        mListView.setOnItemClickListener(
        		new OnItemClickListener()
        		{
					public void onItemClick(AdapterView<?> parent, View view,
							int position, long id) {
						
						//Execute the selected script
						mCursor.moveToPosition(position);
						int ScriptId = mCursor.getInt(0);
						
						ExecuteScript(ScriptId);
						
						refreshCursor();
					}
        		}
     	);
	}
	
	/**Add a new script -> GScriptAdd*/
	void AddScript()
	{
		Intent i = new Intent(GScript.this, GScriptAdd.class);
		startActivity(i);
	}
	
	/**Edit a script -> GScriptEdit*/
	void EditScript(int ScriptId)
	{
		//Edit selected script
		GScriptEdit.EditScriptId = ScriptId;
		Intent i = new Intent(GScript.this, GScriptEdit.class);
		startActivity(i);
	}
	
	/**Delete script*/
	void DeleteScript(int ScriptId)
	{
		//Delete selected script
		mDatabase.execSQL("DELETE FROM scripts WHERE _id = " + ScriptId);
		refreshCursor();
	}
	
	/**Execute a script*/
	void ExecuteScript(int ScriptId)
	{
		if(!CreateShortcut)
		{
			//Execute selected script
			Intent i = new Intent(GScript.this, GScriptExec.class);
			i.putExtra(SCRIPT_KEY, ScriptId);
			startActivity(i);
		} else {
			//Create a shortcut to the specified script
        	ShortcutToScript(ScriptId);
            finish();
            return;
		}
	}
	
	/** Save script to sd card */
	void SaveScript(int ScriptId)
	{
		try
		{
		
		mCursor = mDatabase.query(false, "scripts", new String[] { "_id",
				"name", "script", "su" }, "_id="+ScriptId, null, null, null, null, null);
		
		mCursor.moveToFirst();
		String ScriptName = mCursor.getString(1);
		String Script = mCursor.getString(2);
		
		
		//check if folder exists
		File gscript_folder = new File("/sdcard/gscript/");
		
		if(!gscript_folder.exists())
		{
			gscript_folder.mkdir();
		}			
		
		
		FileWriter fileOutput = new FileWriter("/sdcard/gscript/" + ScriptName + ".sh");
		fileOutput.write(Script);
		fileOutput.flush();
		fileOutput.close();
		
		} catch (Exception ex)
		{
			Toast toast = Toast.makeText(this, "Error while trying to save:\n\n" + ex.getMessage(), Toast.LENGTH_LONG);
			toast.show();
		}
	
	}
	
	/**Create shortcut for script*/
	void ShortcutToScript(int ScriptId)
	{

		mCursor = mDatabase.query(false, "scripts", new String[] { "_id",
				"name", "script", "su" }, "_id="+ScriptId, null, null, null, null, null);
		
		mCursor.moveToFirst();
		String ScriptName = mCursor.getString(1);
		refreshCursor();
		
		Intent executeIntent = new Intent(Intent.ACTION_MAIN);
		executeIntent.setClassName(this, this.getClass().getName());
		executeIntent.putExtra(SCRIPT_KEY, ScriptId);

		Intent intentShortcut = new Intent();

		intentShortcut.putExtra(Intent.EXTRA_SHORTCUT_INTENT, executeIntent);
		intentShortcut.putExtra(Intent.EXTRA_SHORTCUT_NAME, ScriptName);
		Parcelable iconResource = Intent.ShortcutIconResource.fromContext(this,
				R.drawable.gscript_shortcut);
		intentShortcut.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE,
				iconResource);

		setResult(RESULT_OK, intentShortcut);
		
		
	}
}