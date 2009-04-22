package nl.rogro.GScript;

import java.io.File;
import java.util.ArrayList;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;

public class GScriptLoad extends Activity {
	
	Button ButtonLoad;
	ListView ListViewFile;
	
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.loadfile);
		
		ArrayList<String> scriptFiles = new ArrayList<String>();

		
		try
		{

			//check if folder exists
			File gscript_folder = new File("/sdcard/gscript/");
			
			if(!gscript_folder.exists())
			{
				gscript_folder.mkdir();
			}			
			
		File[] files = new File("/sdcard/gscript/").listFiles();
		for (File file : files)
		{
			if(file.isFile() && file.getName().contains(".sh"))
				scriptFiles.add(file.getName());
		}
		} catch (Exception ex)
		{
			//Error while trying to load from /sdcard/gscript/
			Toast toast = Toast.makeText(this,
					"GScript:\n\nError while looking for scripts in /sdcard/gscript/\n\n" + ex,
					Toast.LENGTH_LONG);
			toast.show();
		}
		
		final ArrayAdapter<String> fileList = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, scriptFiles);
		
		ListViewFile = (ListView)findViewById(R.id.ListViewFiles);
		ListViewFile.setAdapter(fileList);
		
		ListViewFile.setOnItemClickListener(
				new OnItemClickListener()
				{
					public void onItemClick(AdapterView<?> arg0, View arg1,
							int arg2, long arg3) {
						String selectedFile = fileList.getItem(arg2);

						Intent intentLoad = new Intent();
						intentLoad.putExtra("FileToLoad", selectedFile);
						setResult(android.app.Activity.RESULT_OK, intentLoad);
						finish();
						
					}
				}
		);
		
		Button ButtonCancel = (Button)findViewById(R.id.ButtonFileCancel);
		ButtonCancel.setOnClickListener(
				new OnClickListener()
				{
					public void onClick(View v) {
						setResult(android.app.Activity.RESULT_CANCELED);
						finish();
					}
				}
		);
			
	}
}
