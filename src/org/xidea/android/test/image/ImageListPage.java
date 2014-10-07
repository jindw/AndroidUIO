package org.xidea.android.test.image;

import org.xidea.android.UIO;
import org.xidea.android.test.DemoUtil;
import org.xidea.android.test.R;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;


public class ImageListPage extends Fragment{
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
							 Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.image_list_fragment, container, false);
		ListView listView = (ListView) v.findViewById(R.id.image_listView);
		listView.setAdapter(new ImageListAdapter(inflater.getContext()));
		return v;
	}
	private static class ImageListAdapter extends ArrayAdapter<String> {
		public ImageListAdapter(Context context) {
			super(context,-1,DemoUtil.getImageList());
		}
		@Override
		public View getView(final int position, View view, ViewGroup parent) {
			if (view == null) {
				view = (ImageView)LayoutInflater.from(getContext()).inflate(R.layout.image_item, parent,false);
			}
			String url = super.getItem(position);
			ImageView image = (ImageView)view;
			UIO.bind(image, url);
			return view;
		}

	}



}
