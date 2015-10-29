package org.xidea.android.impl.ui;

import java.lang.ref.WeakReference;

import org.xidea.android.Callback;
import org.xidea.android.UIO;

import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

class UIUtil {
	static final UIUtil INSTANCE = new UIUtil();
	private static WeakReference<Toast> currentToast = null;


	static Callback.Cancelable showTips(CharSequence message, int duration) {

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

//	public static int px2dip(Context context, float pxValue) {
//		final float scale = UIO.getApplication().getResources().getDisplayMetrics().density;
//		return (int) (pxValue / scale + 0.5f);
//	}

}
