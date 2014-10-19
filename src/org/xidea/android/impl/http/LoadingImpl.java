package org.xidea.android.impl.http;

import java.util.LinkedHashMap;
import java.util.WeakHashMap;

import org.xidea.android.Callback.Cancelable;
import org.xidea.android.Callback.Loading;
import org.xidea.android.UIO;
import org.xidea.android.impl.AsynTask;
import org.xidea.android.impl.ui.UIFacade;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.graphics.CornerPathEffect;
import android.graphics.PathEffect;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.RectShape;
import android.os.Looper;
import android.os.Message;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.ProgressBar;
import android.widget.TextView;

public class LoadingImpl {
	private static Dialog dialog = null;
	private static WeakHashMap<AsynTask, String> tasks = new WeakHashMap<AsynTask, String>();
	private static AsynTask currentLoading = null;
	private static TextView dialogContent;

	private static android.os.Handler main = new android.os.Handler(
			Looper.getMainLooper());

	public static void showDialog(final AsynTask task, Loading loading) {
		final String message = loading == null ? null : loading.value();
		if (message != null) {
			if (Looper.myLooper() != Looper.getMainLooper()) {
				main.post(new Runnable() {
					@Override
					public void run() {
						showDialog(task, message);
					}
				});
			} else {
				showDialog(task, message);
			}
		}
	}

	private static void showDialog(final AsynTask task, final String message) {
		Dialog dialog = requireLoadingDialog();
		currentLoading = task;
		if (!tasks.containsKey(task)) {
			tasks.put(task, message);
		}
		dialogContent.setText(message);
		dialog.show();

	}

	public static void cancleUI(final AsynTask key) {
		if (dialog != null && tasks.containsKey(key)) {
			if (Looper.myLooper() != Looper.getMainLooper()) {
				main.post(new Runnable() {
					@Override
					public void run() {
						cancleUI(key);
					}
				});
			} else {
				tasks.remove(key);
				if (tasks.isEmpty()) {
					dialog.dismiss();
					dialog = null;
				} else {
					AsynTask lastKey = null;
					// get the last key
					for (AsynTask i : tasks.keySet()) {
						lastKey = i;
					}
					currentLoading = lastKey;
					updateDialog();
				}
			}
		}
	}

	private static void updateDialog() {
		String msg = tasks.get(currentLoading);
		if (msg != null) {
			dialogContent.setText(msg);
		}
	}

	private static Dialog requireLoadingDialog() {
		if (dialog == null) {
			dialog = new Dialog(UIO.getApplication());
		    dialog.getWindow().setType(WindowManager.LayoutParams.TYPE_TOAST);  
			dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
			dialogContent = setupDialogView();
			dialog.setCanceledOnTouchOutside(false);
			dialog.setOnCancelListener(new OnCancelListener() {
				@Override
				public void onCancel(DialogInterface dialog) {
					if (currentLoading != null && !currentLoading.isCanceled()) {
						currentLoading.cancel();
					}
				}
			});
		}
		return dialog;

	}

	private static TextView setupDialogView() {
		Context context = UIO.getApplication();

		ShapeDrawable drawable = new ShapeDrawable(new RectShape());
		// drawable.getPaint().setColor(Color.BLACK);
		PathEffect effect = new CornerPathEffect(UIFacade.dip2px(5));
		drawable.getPaint().setPathEffect(effect);

		LinearLayout layout = new LinearLayout(context);
		layout.setOrientation(LinearLayout.HORIZONTAL);
		LayoutParams params = new LayoutParams(LayoutParams.WRAP_CONTENT,
				LayoutParams.WRAP_CONTENT);
		layout.setGravity(Gravity.CENTER);
		layout.setLayoutParams(params);
		layout.setBackground(drawable);

		ProgressBar bar = new ProgressBar(context, null,
				android.R.attr.progressBarStyle);
		params = new LayoutParams(UIFacade.dip2px(35), UIFacade.dip2px(35));
		params.setMargins(UIFacade.dip2px(20), UIFacade.dip2px(20),
				UIFacade.dip2px(10), UIFacade.dip2px(20));
		bar.setLayoutParams(params);
		layout.addView(bar);

		TextView textView = new TextView(context);
		params = new LayoutParams(LayoutParams.WRAP_CONTENT,
				LayoutParams.WRAP_CONTENT);
		params.setMargins(UIFacade.dip2px(10), UIFacade.dip2px(20),
				UIFacade.dip2px(40), UIFacade.dip2px(20));
		textView.setLayoutParams(params);
		textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17);
		layout.addView(textView);
		dialog.setContentView(layout);
		return textView;
	}

}
