package org.xidea.android.test.network;

import java.util.HashMap;

import org.json.JSONObject;
import org.xidea.android.UIO;
import org.xidea.android.Callback;
import org.xidea.android.impl.DebugLog;
import org.xidea.android.test.DemoUtil;
import org.xidea.android.test.R;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;


public class NetworkPage extends Fragment {
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}


	private void setupUserInfo( User user) {
		View root = this.getView();
		final TextView nameView = (TextView) root.findViewById(R.id.text_username);
		final ImageView avatarView = (ImageView) root.findViewById(R.id.image_avatar);
		final TextView emailView = (TextView) root.findViewById(R.id.text_email);
		if(user !=null){
			UIO.bind(avatarView, user.avatar);
			nameView.setText(user.name);
			emailView.setText(user.email);
		}else{
			UIO.bind(avatarView, null);
			nameView.setText("");
			emailView.setText("");
		}
	}
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {

		View root = inflater.inflate(R.layout.network_fragment, container, false);
		Button callbackButton = (Button) root.findViewById(R.id.btn_network_callback);
		Button prepareButton = (Button) root.findViewById(R.id.btn_network_prepare);
		Button cacheCallbackButton = (Button) root.findViewById(R.id.btn_network_cachecallback);
		callbackButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				String url = DemoUtil.getUserURL();;
				UIO.get(new Callback<User>() {
					@Override
					public void callback(User user) {
						setupUserInfo( user);
					}
					@Override
					public void error(Throwable ex, boolean callbackError) {
						DebugLog.warn(ex.getLocalizedMessage());
					}

				}, url);
			}
		});

		prepareButton.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				String url = DemoUtil.getUserURL();;
				UIO.get(new Callback.PrepareCallback<JSONObject,User>() {
					@Override
					public Object prepare(JSONObject rawData) {
						User user = new User();
						user.name = "username:"+rawData.optString("name");
						user.avatar =rawData.optString("avatar");
						user.email = "email:"+rawData.optString("email");
						return user;
					}
					@Override
					public void callback(User user) {
						setupUserInfo(user);
					}

					@Override
					public void error(Throwable ex, boolean callbackError) {
						DebugLog.warn(ex.getLocalizedMessage());
					}


				}, url);
			}
		});
		cacheCallbackButton.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				String url = DemoUtil.getUserURL();
				UIO.get(new Callback.CacheCallback<User>() {
					
					@Override
					public boolean cache(User cacheData) {
						if (cacheData == null) {
							UIO.showTips("没有任何缓存数据! 全新的请求！");
						} else {
							UIO.showTips("存在缓存数据，先显示缓存数据！");
							setupUserInfo(cacheData);
						}
						return false;
					}

					@Override
					public void callback(User networkResult) {
						if (networkResult == null) {
							UIO.showLongTips("数据未更新，继续显示原来内容！");
						} else {
							UIO.showLongTips("有新数据，更新显示：");
							setupUserInfo(networkResult);
						}
					}

					@Override
					public void error(Throwable ex, boolean callbackError) {
						DebugLog.warn(ex.getLocalizedMessage());
					}

				}, url);
			}
		});

		return root;
	}

}
