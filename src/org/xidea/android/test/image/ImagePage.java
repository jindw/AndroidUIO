package org.xidea.android.test.image;

import org.xidea.android.Callback;
import org.xidea.android.DrawableFactory;
import org.xidea.android.UIO;
import org.xidea.android.impl.ui.BoxDrawableFactory;
import org.xidea.android.test.MainActivity;
import org.xidea.android.test.R;

import android.graphics.Bitmap;
import android.graphics.Movie;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;


public class ImagePage extends Fragment{

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View view = inflater.inflate(org.xidea.android.test.R.layout.image_fragment, container, false);
		
		ImageView image_bind_simple = (ImageView) view.findViewById(R.id.image_bind_simple);
		UIO.bind(image_bind_simple, "http://upload.wikimedia.org/wikipedia/commons/thumb/d/d3/Newtons_cradle_animation_book_2.gif/400px-Newtons_cradle_animation_book_2.gif");

		ImageView image_bind_gif = (ImageView) view.findViewById(R.id.image_bind_gif);
		UIO.bind(image_bind_gif, "http://www.baidu.com/img/baidu_jgylogo3.gif");
		
		ImageView image_bind_drawable_factory = (ImageView) view.findViewById(R.id.image_bind_drawable_factory);
		UIO.bind(image_bind_drawable_factory, "http://www.baidu.com/img/baidu_jgylogo3.gif",drawableFactory, R.drawable.failed);

		ImageView difficult_imageview = (ImageView) view.findViewById(R.id.image_bind_callback);
		UIO.bind(difficult_imageview, "http://www.baidu.com/img/baidu_jgylogo3.gif", drawableFactory, R.drawable.failed, 
				new Callback<Drawable>() {
					@Override
					public void callback(Drawable result) {
						if (result != null) {
							UIO.showTips("获取图片成功.");
						}else {
							UIO.showTips("获取图片失败.");
						}
					}

					@Override
					public void error(Throwable ex, boolean callbackError) {
						UIO.showTips("获取图片失败.");
						ex.printStackTrace();
					}
				});
		view.findViewById(R.id.btn_image_list).setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				((MainActivity)getActivity()).open(new ImageListPage());
			}
		});
		
		return view;
	}
	
	
	static DrawableFactory drawableFactory = new BoxDrawableFactory(20){
		{
			this.setBorder(4, 0xFFFF00FF);
		}
		@Override
		public Bitmap prepare(Bitmap bitmap) {
			return super.prepare(bitmap);
		}

		@Override
		public Movie prepare(Movie movie) {
			return super.prepare(movie);
		}
		@Override
		public Drawable createDrawable(Bitmap bitmap) {
			return super.createDrawable(bitmap);
		}

		@Override
		public Drawable createDrawable(Movie movie) {
			return super.createDrawable(movie);
		}

		@Override
		public Drawable getLoadingDrawable(View imageView) {
			return super.getLoadingDrawable(imageView);
		}

		@Override
		public int getSize(Bitmap bm) {
			return super.getSize(bm);
		}

		@Override
		public int getSize(Movie m) {
			return super.getSize(m);
		}
		
	};

}
