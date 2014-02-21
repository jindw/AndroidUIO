package org.xidea.android.impl.ui;

import android.app.Activity;
import android.content.Context;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;

/**
 * @author jindawei
 * 
 */
class GestureHelper {
	public interface FillingListener {
		boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX,
				float distanceY);

	}

	public interface HorizontalFillingListener extends FillingListener {
		void onRight2Left();
		void onLeft2Right();
	}

	public interface VerticalFillingListener extends FillingListener {
		void onBottom2Top();
		void onTop2Bottom();
	}

	public void setupFillingGesture(final FillingListener listener,
			Activity activity, View... views) {
		Context context = findContext(activity, views);
		if (context == null) {
			return;
		}
		final GestureDetector gd = new GestureDetector(context,
				createGestureListener(listener));
		OnTouchListener proxy = new View.OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				return gd.onTouchEvent(event);
			}
		};
		if (activity != null) {
			View view = activity.getWindow().getDecorView();
			view.setLongClickable(true);
			EventHelper.attachEvent(view, proxy);
		}
		if (views != null) {
			for (View view : views) {
				view.setLongClickable(true);
				EventHelper.attachEvent(view, proxy);
			}
		}
	}

	private SimpleOnGestureListener createGestureListener(
			final FillingListener listener) {
		return new GestureDetector.SimpleOnGestureListener() {

			HorizontalFillingListener hl;
			VerticalFillingListener vl;
			{
				if (listener instanceof HorizontalFillingListener) {
					hl = (HorizontalFillingListener) listener;
				}
				if (listener instanceof VerticalFillingListener) {
					vl = (VerticalFillingListener) listener;
				}
			}
			private float t1 = 0;
			private long MIN_TIME = 1000 / 2;
			private int MIN_OFFSET = 40;

			long actEvent1Time;
			long actEvent2Time;

			public boolean onScroll(MotionEvent e1, MotionEvent e2,
					float distanceX, float distanceY) {
				if (listener.onScroll(e1, e2, distanceX, distanceY)) {
					return true;
				}
				if (e1 != null && e2 != null) {
					long t2 = e2.getEventTime();
					if (t2 - t1 < MIN_TIME) {
						return false;
					}
					int offsetX = ((int) (e2.getX() - e1.getX())) / MIN_OFFSET;
					int offsetY = ((int) (e2.getY() - e1.getY())) / MIN_OFFSET;
					if (Math.abs(offsetY) > 0 || Math.abs(offsetX) > 0) {
						t1 = t2;
						return this.onFling(e1, e2, offsetX, offsetY);
					}
				}
				return false;
			}

			@Override
			public boolean onFling(MotionEvent e1, MotionEvent e2,
					float velocityX, float velocityY) {
				if (e1 == null || e2 == null) {
					return false;
				}
				long e1Time = e1.getEventTime();
				if (actEvent1Time == e1Time
						|| e1Time - actEvent2Time < MIN_OFFSET) {
					return false;
				}
				actEvent1Time = e1Time;
				actEvent2Time = e2.getEventTime();

				if (Math.abs(velocityX) > Math.abs(velocityY)) {// 水平移动
					if (hl != null) {
						if (velocityX > 0)
							hl.onLeft2Right();
						if (velocityX < 0)
							hl.onRight2Left();
					} else if (vl != null) {
						if (velocityY > 0)
							vl.onTop2Bottom();
						if (velocityY < 0)
							vl.onBottom2Top();
					}
				} else {
					if (vl != null) {
						if (velocityY > 0)
							vl.onTop2Bottom();
						if (velocityY < 0)
							vl.onBottom2Top();
					} else if (hl != null) {
						if (velocityX > 0)
							hl.onLeft2Right();
						if (velocityX < 0)
							hl.onRight2Left();
					}
				}
				return false;
			}

		};
	}

	private Context findContext(Activity activity, View... views) {
		Context context = null;
		if (activity != null) {
			context = activity;
		} else {
			for (View v : views) {
				if (v != null) {
					context = v.getContext();
					if (context != null) {
						break;
					}
				}
			}
		}
		return context;
	}
}
