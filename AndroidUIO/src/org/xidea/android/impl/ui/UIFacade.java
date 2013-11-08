package org.xidea.android.impl.ui;

import java.lang.ref.WeakReference;
import java.util.HashMap;

import android.app.Activity;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.Toast;

import org.xidea.android.Callback;
import org.xidea.android.impl.ApplicationState;


public class UIFacade {
	private static final UIFacade INSTANCE = new UIFacade();
	private int androidbaseReservedId = 0xFFFFFFFF;//android.R.id.custom;
	private WeakReference<Toast> currentToast = null;
	public void longTips(CharSequence message) {
		showTips(message,Toast.LENGTH_LONG);
	}
	public void shortTips(String message) {
		showTips(message,Toast.LENGTH_SHORT);
	}
	private void showTips(CharSequence message,int duration) {
		Toast t =  Toast.makeText(ApplicationState.getInstance().getApplication(),message,duration);
		t.show();
		currentToast = new WeakReference<Toast>(t);
	}
	public void cancelToast() {
		if(currentToast!=null){
			Toast toast = currentToast.get();
			if(toast != null){
				toast.cancel();
			}
			
		}
	}
	private static class KV{Object key;Object value;}
	private static class MAP extends HashMap<Object,Object>{private static final long serialVersionUID = 8165191196953713719L;};
	public void setTag(View view,Object key,Object value){
		final int id = androidbaseReservedId;
		Object old = view == null ? null : view.getTag(id);
		if(old == null){
			KV kv = new KV();
			kv.key = key;
			kv.value = value;
			view.setTag(id,kv);
		}else if(old instanceof KV){
			KV kv = (KV) old;
			if(key == null ? kv.key == null :key.equals(kv.key)){
				kv.value = value;
			}else{
				MAP map = new MAP();
				map.put(kv.key, kv.value);
				map.put(key, value);
				view.setTag(id,map);
			}
		}else if(old instanceof MAP){
			((MAP)old).put(key,value);
		}else{
			throw new IllegalStateException("请通过commonUtil.putTag管理tag。 ");
		}
		
		
	}
	public Object getTag(View view,Object key){
		int id =  androidbaseReservedId;
		Object old = view == null ? null : view.getTag(id);
        if(old != null){
            if(old instanceof KV){
                KV kv = (KV) old;
                if(kv.key.equals(key)){
                    return kv.value;
                }
            }else if(old instanceof MAP){
                return ((MAP)old).get(key);
            }
        }
		return null;
	}

	public void attachTopBottomGesture(final Callback<Boolean> fromTopCallback,Activity activity, View[] views){
		new GestureHelper().setupFillingGesture(new GestureHelper.VerticalFillingListener(){

			@Override
			public boolean onScroll(MotionEvent e1, MotionEvent e2,
					float distanceX, float distanceY) {
				return false;
			}

			@Override
			public void onBottom2Top() {
				fromTopCallback.callback(false);
			}

			@Override
			public void onTop2Bottom() {
				fromTopCallback.callback(true);
			}
			
		}, activity, views);
	}
	public void attachLeftRightGesture(final Callback<Boolean> fromLeftCallback, Activity activity, View[] views){
		new GestureHelper().setupFillingGesture(new GestureHelper.HorizontalFillingListener(){

			@Override
			public boolean onScroll(MotionEvent e1, MotionEvent e2,
					float distanceX, float distanceY) {
				return false;
			}

			@Override
			public void onRight2Left() {
				fromLeftCallback.callback(false);
			}

			@Override
			public void onLeft2Right() {
				fromLeftCallback.callback(true);
			}
			
		}, activity, views);
		
	}
	public void attachEvent(View view, OnTouchListener listener) {
		EventHelper.attachEvent(view, listener);
	}
	public static UIFacade getInstance() {
		return INSTANCE;
	}
}
