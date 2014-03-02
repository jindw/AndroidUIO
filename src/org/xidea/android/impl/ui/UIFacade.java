package org.xidea.android.impl.ui;

import java.lang.ref.WeakReference;
import java.util.HashMap;

import org.xidea.android.Callback;
import org.xidea.android.UIO;

import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Toast;

public class UIFacade {
	private static final UIFacade INSTANCE = new UIFacade();
	private static WeakReference<Toast> currentToast = null;

	public Callback.Cancelable longTips(CharSequence message) {
		return showTips(message, Toast.LENGTH_LONG);
	}

	public Callback.Cancelable shortTips(CharSequence message) {
		return showTips(message, Toast.LENGTH_SHORT);
	}

	private Callback.Cancelable showTips(CharSequence message, int duration) {

		return new ToastCancelable(message, duration);
	}

	private static class ToastCancelable implements Callback.Cancelable,
			Runnable {

		private WeakReference<Toast> ref;
		private int duration;
		private CharSequence message;

		public ToastCancelable(CharSequence message, int duration) {
			this.message = message;
			this.duration = duration;
			if (Looper.getMainLooper() == Looper.myLooper()) {
				run();
			} else {
				ref = new WeakReference<Toast>(null);
				new Handler(Looper.getMainLooper()).post(this);
			}
		}

		public void run() {
			if (message != null) {
				Toast t = Toast.makeText(UIO.getApplication(), message,
						duration);
				t.show();
				ref = new WeakReference<Toast>(t);
				currentToast = ref;
			}
		}

		@Override
		public void cancel() {
			message = null;
			Toast toast = ref.get();
			if (toast != null) {
				ref.clear();
				toast.cancel();
			}
		}

		@Override
		public boolean isCanceled() {
			Toast toast = ref.get();
			return toast == null;
		}

	}

	public void cancelToast() {
		if (currentToast != null) {
			Toast toast = currentToast.get();
			if (toast != null) {
				toast.cancel();
			}

		}
	}

	// private int androidbaseReservedId = 0xFFFFFFFF;//android.R.id.custom;
	// private static class KV{Object key;Object value;}
	// private static class MAP extends HashMap<Object,Object>{private static
	// final long serialVersionUID = 8165191196953713719L;};
	// public void setTag(View view,Object key,Object value){
	// final int id = androidbaseReservedId;
	// Object old = view == null ? null : view.getTag(id);
	// if(old == null){
	// KV kv = new KV();
	// kv.key = key;
	// kv.value = value;
	// view.setTag(id,kv);
	// }else if(old instanceof KV){
	// KV kv = (KV) old;
	// if(key == null ? kv.key == null :key.equals(kv.key)){
	// kv.value = value;
	// }else{
	// MAP map = new MAP();
	// map.put(kv.key, kv.value);
	// map.put(key, value);
	// view.setTag(id,map);
	// }
	// }else if(old instanceof MAP){
	// ((MAP)old).put(key,value);
	// }else{
	// throw new IllegalStateException("请通过commonUtil.putTag管理tag。 ");
	// }
	//
	//
	// }
	// public Object getTag(View view,Object key){
	// int id = androidbaseReservedId;
	// Object old = view == null ? null : view.getTag(id);
	// if(old != null){
	// if(old instanceof KV){
	// KV kv = (KV) old;
	// if(kv.key.equals(key)){
	// return kv.value;
	// }
	// }else if(old instanceof MAP){
	// return ((MAP)old).get(key);
	// }
	// }
	// return null;
	// }

	public static UIFacade getInstance() {
		return INSTANCE;
	}
}
