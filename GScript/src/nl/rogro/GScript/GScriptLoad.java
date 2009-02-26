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
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemSelectedListener;

public class GScriptLoad extends Activity {
	
	Button ButtonLoad;
	ListView ListViewFile;
	
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.loadfile);
		
		ArrayList<String> scriptFiles = new ArrayList<String>();

		File[] files = new File("/sdcard/").listFiles();
		for (File file : files)
		{
			if(file.isFile() && file.getName().contains(".sh"))
				scriptFiles.add(file.getName());
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
