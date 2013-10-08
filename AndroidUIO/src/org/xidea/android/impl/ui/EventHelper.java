package org.xidea.android.impl.ui;

import java.util.ArrayList;

import android.view.MotionEvent;
import android.view.View;

/**
 * 防止多处事件添加时相互覆盖
 * @author jindawei
 *
 */
class EventHelper {

	private static Object KEY = new Object();
	static class EventProxy  implements View.OnTouchListener,View.OnClickListener{
		ArrayList<View.OnTouchListener> touch;
		ArrayList<View.OnClickListener> click;
		@Override
		public boolean onTouch(View v, MotionEvent event) {
			boolean f = false;
			for(View.OnTouchListener l: touch){
				f = l.onTouch(v, event) || f;
			}
			return f;
		}

		@Override
		public void onClick(View v) {
			for(View.OnClickListener l: click){
				l.onClick( v) ;
			}
		}
	}
	public static void attachEvent(View view,View.OnTouchListener l){
		EventProxy listenerMap = requireProxy(view);
		if(listenerMap.touch == null){
			listenerMap.touch = new ArrayList<View.OnTouchListener>();
			view.setOnTouchListener(listenerMap);
		}
		listenerMap.touch.add(l);
	}
	public static void attachEvent(View view,View.OnClickListener l){
		EventProxy listenerMap = requireProxy(view);
		if(listenerMap.click == null){
			listenerMap.click = new ArrayList<View.OnClickListener>();
			view.setOnClickListener(listenerMap);
		}
		listenerMap.click.add(l);
	}
	private static EventProxy requireProxy(View view) {
		UIFacade CommonUtil = UIFacade.getInstance();
		EventProxy listenerMap = (EventProxy)CommonUtil.getTag(view,KEY);
		if(listenerMap == null){
			listenerMap = new EventProxy();
			CommonUtil.setTag(view,KEY,listenerMap);
		}
		return listenerMap;
	}

}
