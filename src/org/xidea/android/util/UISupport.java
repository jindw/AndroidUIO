package org.xidea.android.util;

import org.xidea.android.Callback;
import org.xidea.android.DrawableFactory;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.widget.ImageView;

/**
 * @see org.xidea.android.impl.ui.UISupportImpl
 * @author jinjinyun
 *
 */
public interface UISupport {
	public abstract void bind(ImageView view, String url,
			DrawableFactory factory, Object fallbackResource,
			Callback<Drawable> callback);

	public Callback.Cancelable longTips(CharSequence message) ;

	public Callback.Cancelable shortTips(CharSequence message) ;
	public abstract void clear(Context activity);
	public abstract void clearAll();
}