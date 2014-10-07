package org.xidea.android.test;

import org.xidea.android.test.image.ImagePage;
import org.xidea.android.test.network.NetworkPage;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

public class MainActivity extends FragmentActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main_activity);
		View imageTest = findViewById(R.id.btn_image_test);
		imageTest.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				open(new ImagePage());
			}
		});
		View networkTest = findViewById(R.id.btn_network_test);
		networkTest.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				open(new NetworkPage());
			}
		});
	}
	
	public void open(Fragment fragment){
		FragmentManager fm = this.getSupportFragmentManager();
		String name = fragment.getClass().getName();
		fm.beginTransaction().add(android.R.id.content,fragment).addToBackStack(name).commit();
	}

}
