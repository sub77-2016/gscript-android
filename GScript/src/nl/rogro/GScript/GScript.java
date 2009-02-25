package nl.rogro.GScript;

import java.io.DataOutputStream;
import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Bundle;
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

	public static final int mnuAddItem = Menu.FIRST + 1;
	public static final int mnuEditItem = Menu.FIRST + 2;
	public static final int mnuDeleteItem = Menu.FIRST + 3;
	public static final int mnuRunItem = Menu.FIRST + 4;
	public static final int mnuInfo = Menu.FIRST + 5;
	
	@Override 
	public boolean onCreateOptionsMenu(Menu menu) {

		menu.setQwertyMode(true);
		MenuItem itmAdd = menu.add(0, mnuAddItem, 0, "Add script");
		{
			  itmAdd.setAlphabeticShortcut('a');
		}
		MenuItem itmInfo = menu.add(0, mnuInfo, 0, "Caution information");
		{
			  itmInfo.setAlphabeticShortcut('c');
		}
		
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
		
		return super.onOptionsItemSelected(item);
	}	
	@Override 
	public boolean onContextItemSelected(MenuItem item) 
    {
		AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
		int itemId = Integer.valueOf(String.valueOf(info.position));
		
		if(item.getItemId()==mnuRunItem)
		{
		    ExecuteScript(itemId);
		}
		if(item.getItemId()==mnuDeleteItem)
		{
			DeleteScript(itemId);
		}
		if(item.getItemId()==mnuEditItem)
		{
			EditScript(itemId);
		}
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
		//updateEmptyText();
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
        setContentView(R.layout.main);
        
        mListView = (ListView) findViewById(android.R.id.list);
        mInflater = getLayoutInflater();
        
        DatabaseHelper hlp = new DatabaseHelper(this);                 
        mDatabase = hlp.getWritableDatabase(); 

        refreshCursor();
        
        mListView.setOnCreateContextMenuListener(
        		new OnCreateContextMenuListener()
        		{
					public void onCreateContextMenu(ContextMenu menu, View v,
							ContextMenuInfo menuInfo) {
						menu.add(0, mnuRunItem, 0, "Run");
						menu.add(0, mnuEditItem, 0, "Edit");
						menu.add(0, mnuDeleteItem, 0, "Delete");
						menu.add(0, 999, 0, "Cancel");

					}
        		}
        );
        mListView.setOnItemClickListener(
        		new OnItemClickListener()
        		{
					public void onItemClick(AdapterView<?> parent, View view,
							int position, long id) {
						ExecuteScript(position);
					}
        		}
     	);
	}
	
	void AddScript()
	{
		Intent i = new Intent(GScript.this, GScriptAdd.class);
		startActivity(i);
	}
	
	void EditScript(int ScriptNr)
	{
		mCursor.moveToPosition(ScriptNr);
		int ScriptId = mCursor.getInt(0);
		refreshCursor();
		
		GScriptEdit.EditScriptId = ScriptId;
		
		Intent i = new Intent(GScript.this, GScriptEdit.class);
		startActivity(i);
	}
	
	void DeleteScript(int ScriptNr)
	{
		//Delete selected script
		mCursor.moveToPosition(ScriptNr);
		int ScriptId = mCursor.getInt(0);
		
		mDatabase.execSQL("DELETE FROM scripts WHERE _id = "+ScriptId);
		refreshCursor();
	}
	
	void ExecuteScript(int ScriptNr)
	{
		//Execute selected script
		Toast toast; 

		try 
		{

		mCursor.moveToPosition(ScriptNr);
		
		String Script = mCursor.getString(2); 
		short Su = mCursor.getShort(3);
		
		toast = Toast.makeText(this, "Script is being executed in the background:\n\n"+ Script, Toast.LENGTH_LONG);
	    toast.show();

		Script += "\nexit\n";	    
		
		refreshCursor();
			
		Process process;
				
		if(Su==1)
		{
			process = Runtime.getRuntime().exec("su");			
		} else {
			process = Runtime.getRuntime().exec("sh");		
		}

		DataOutputStream os = new DataOutputStream(process.getOutputStream());
		os.writeBytes(Script);
		os.flush();

		} catch (Exception e) {

			toast = Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG);
		    toast.show();
		    
		}
		
	}
	
}