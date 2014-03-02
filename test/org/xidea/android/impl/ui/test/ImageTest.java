package org.xidea.android.impl.ui.test;

import java.io.ByteArrayInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.List;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.xidea.android.UIO;
import org.xidea.android.Callback;
import org.xidea.android.impl.AsynTask.AsynImpl;
import org.xidea.android.impl.http.HttpSupport;
import org.xidea.android.impl.AsynTask;
import org.xidea.android.impl.io.IOUtil;
import org.xidea.android.impl.ui.GifDecoder;
import org.xidea.android.test.BaseTest;

import android.util.Log;
import android.widget.ImageView;

import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class ImageTest extends BaseTest {

	@Test
	public void testBind() throws InterruptedException {
		ImageView view = new ImageView(UIO.getApplication());
		Log.e("A", "AAAAAA");
		System.out.println("!!!!");
		UIO.bind(view,
				"http://mat1.gtimg.com/news/news2013/LOGO.jpg");
		UIO.bind(view,
				"http://mat1.gtimg.com/news/news2013/LOGO.jpg");

		Thread.sleep(1000 * 10);

		assertRegexp("queue info\\:count\\:1;memery\\:[\\d\\.]+M");
		// Assert.assertEquals(expected, actual)
	}

	/*
 */
	@Test
	public void testGifAnimateTest() throws Exception {
		String[] urls = new String[] {
				"http://t3.baidu.com/it/u=2226890413,714921982&fm=23&gp=0.jpg",
				"http://c.baidu.com/c.gif?t=3&q=gif&p=0&pn=0",
				"http://img0.bdstatic.com/img/image/shitu/feimg/uploading.gif",
				"http://image.baidu.com/static/flash/mark.gif",
				"http://list.image.baidu.com/t/loading_circle.gif",
				"http://t1.baidu.com/it/u=2510408151,4029331558&fm=21&gp=0.jpg",
				"http://t1.baidu.com/it/u=3666727830,2597641321&fm=21&gp=0.jpg",
//				"http://t2.baidu.com/it/u=18441288,1802779444&fm=21&gp=0.jpg",
//				"http://t2.baidu.com/it/u=2059852255,434043336&fm=21&gp=0.jpg",
//				"http://t1.baidu.com/it/u=4119389101,1045196322&fm=21&gp=0.jpg",
//				"http://t3.baidu.com/it/u=123672868,590272345&fm=21&gp=0.jpg",
//				"http://t1.baidu.com/it/u=751501906,1513400462&fm=90&gp=0.jpg",
//				"http://t1.baidu.com/it/u=868096369,118416722&fm=21&gp=0.jpg",
//				"http://t2.baidu.com/it/u=2003090483,1177643471&fm=21&gp=0.jpg",
//				"http://t1.baidu.com/it/u=1675500229,1792835852&fm=90&gp=0.jpg",
//				"http://t2.baidu.com/it/u=2435728351,1133472933&fm=21&gp=0.jpg",
//				"http://t2.baidu.com/it/u=3172049661,1644909044&fm=21&gp=0.jpg",
//				"http://t2.baidu.com/it/u=916472775,647385562&fm=21&gp=0.jpg",
//				"http://t1.baidu.com/it/u=3853475039,2712324899&fm=21&gp=0.jpg",
//				"http://t1.baidu.com/it/u=1371571849,2211902582&fm=21&gp=0.jpg",
//				"http://t1.baidu.com/it/u=1183339533,1579368537&fm=21&gp=0.jpg",
//				"http://t2.baidu.com/it/u=915720671,1439985539&fm=21&gp=0.jpg",
//				"http://t1.baidu.com/it/u=174467275,4226842929&fm=21&gp=0.jpg",
//				"http://t3.baidu.com/it/u=4279124819,2643602830&fm=21&gp=0.jpg",
//				"http://t3.baidu.com/it/u=2820363020,3957482827&fm=21&gp=0.jpg",
//				"http://t2.baidu.com/it/u=214126949,426233786&fm=21&gp=0.jpg",
//				"http://t1.baidu.com/it/u=2017815537,2703642145&fm=21&gp=0.jpg",
//				"http://t1.baidu.com/it/u=2045248104,1139323035&fm=21&gp=0.jpg",
//				"http://t3.baidu.com/it/u=1689558378,377731691&fm=21&gp=0.jpg",
//				"http://t1.baidu.com/it/u=3427546413,1994813791&fm=21&gp=0.jpg",
//				"http://t2.baidu.com/it/u=646635970,483694629&fm=21&gp=0.jpg",
//				"http://t2.baidu.com/it/u=2047508977,679613856&fm=21&gp=0.jpg",
//				"http://t1.baidu.com/it/u=1255042358,1012271191&fm=21&gp=0.jpg",
//				"http://t1.baidu.com/it/u=3880300553,523725302&fm=21&gp=0.jpg",
//				"http://t3.baidu.com/it/u=2759664814,3628493402&fm=21&gp=0.jpg",
//				"http://t1.baidu.com/it/u=571489751,705305938&fm=21&gp=0.jpg",
//				"http://t1.baidu.com/it/u=2272275214,634515608&fm=21&gp=0.jpg",
//				"http://t2.baidu.com/it/u=2139998914,823110948&fm=21&gp=0.jpg",
//				"http://t2.baidu.com/it/u=2279561824,30340506&fm=21&gp=0.jpg",
//				"http://t1.baidu.com/it/u=2101442185,2623744203&fm=21&gp=0.jpg",
//				"http://t2.baidu.com/it/u=1789774978,199918158&fm=21&gp=0.jpg",
//				"http://t1.baidu.com/it/u=4055043197,1319184491&fm=21&gp=0.jpg",
//				"http://t3.baidu.com/it/u=1015903864,1834180729&fm=21&gp=0.jpg",
//				"http://t3.baidu.com/it/u=1409846906,2076781884&fm=11&gp=0.jpg",
//				"http://t3.baidu.com/it/u=1660091059,2059386169&fm=21&gp=0.jpg",
//				"http://t2.baidu.com/it/u=3369704113,26706246&fm=21&gp=0.jpg",
//				"http://t1.baidu.com/it/u=3424095261,3981003988&fm=21&gp=0.jpg",
//				"http://t1.baidu.com/it/u=3527665895,4114666283&fm=21&gp=0.jpg",
//				"http://t1.baidu.com/it/u=1844105655,525723588&fm=21&gp=0.jpg",
//				"http://t3.baidu.com/it/u=3608893144,1896388526&fm=21&gp=0.jpg",
//				"http://t2.baidu.com/it/u=2038089406,3130714573&fm=90&gp=0.jpg",
//				"http://t3.baidu.com/it/u=227808380,3136453026&fm=21&gp=0.jpg",
//				"http://t3.baidu.com/it/u=544852674,1121700014&fm=21&gp=0.jpg",
//				"http://t2.baidu.com/it/u=3534601112,3562169268&fm=90&gp=0.jpg",
//				"http://t3.baidu.com/it/u=3631268234,4279292350&fm=21&gp=0.jpg",
//				"http://t3.baidu.com/it/u=965775989,4255257907&fm=21&gp=0.jpg",
//				"http://t2.baidu.com/it/u=607246621,2781320268&fm=21&gp=0.jpg",
//				"http://t1.baidu.com/it/u=3232809279,3814487482&fm=21&gp=0.jpg",
//				"http://t3.baidu.com/it/u=3975366746,2723572669&fm=21&gp=0.jpg",
//				"http://t3.baidu.com/it/u=2431520077,2630337896&fm=21&gp=0.jpg",
//				"http://t3.baidu.com/it/u=1015261745,1617304452&fm=21&gp=0.jpg",
//				"http://t2.baidu.com/it/u=520581490,3874074331&fm=21&gp=0.jpg",
//				"http://t1.baidu.com/it/u=2523532388,1000133050&fm=21&gp=0.jpg",
//				"http://t1.baidu.com/it/u=3083964795,2767895881&fm=21&gp=0.jpg",
//				"http://t1.baidu.com/it/u=1298313360,3511843891&fm=21&gp=0.jpg",
//				"http://t1.baidu.com/it/u=4041996275,3908920454&fm=21&gp=0.jpg",
//				"http://t2.baidu.com/it/u=1641129289,3501117151&fm=21&gp=0.jpg",
//				"http://t1.baidu.com/it/u=3082263183,4215508978&fm=21&gp=0.jpg",
//				"http://t1.baidu.com/it/u=3870077888,2613879155&fm=21&gp=0.jpg",
//				"http://t2.baidu.com/it/u=2890111626,2830420160&fm=21&gp=0.jpg",
//				"http://t1.baidu.com/it/u=576798798,4044405895&fm=21&gp=0.jpg",
//				"http://t1.baidu.com/it/u=352888724,3669233413&fm=21&gp=0.jpg",
//				"http://t3.baidu.com/it/u=1502267477,3016356409&fm=21&gp=0.jpg",
//				"http://t3.baidu.com/it/u=3812457574,1236883868&fm=21&gp=0.jpg",
//				"http://t2.baidu.com/it/u=590545345,4084026187&fm=21&gp=0.jpg",
//				"http://t3.baidu.com/it/u=344451164,3464606628&fm=21&gp=0.jpg",
//				"http://t2.baidu.com/it/u=3418086983,3327970137&fm=21&gp=0.jpg",
//				"http://t1.baidu.com/it/u=2323648978,3904476708&fm=21&gp=0.jpg",
//				"http://t3.baidu.com/it/u=3443294897,4109616778&fm=21&gp=0.jpg",
//				"http://t1.baidu.com/it/u=3701112687,1840448557&fm=21&gp=0.jpg",
//				"http://t3.baidu.com/it/u=224511217,3777152656&fm=21&gp=0.jpg",
//				"http://t1.baidu.com/it/u=3634318279,2369246232&fm=21&gp=0.jpg",
//				"http://t2.baidu.com/it/u=2419687306,3185888872&fm=21&gp=0.jpg",
//				"http://t2.baidu.com/it/u=3393363359,1405243185&fm=21&gp=0.jpg",
//				"http://t3.baidu.com/it/u=3535321849,2088000626&fm=21&gp=0.jpg",
//				"http://t2.baidu.com/it/u=2609477595,3720579245&fm=21&gp=0.jpg",
//				"http://t3.baidu.com/it/u=518359795,3386058844&fm=21&gp=0.jpg",
//				"http://t2.baidu.com/it/u=227662709,4183083678&fm=21&gp=0.jpg",
//				"http://t1.baidu.com/it/u=883637497,219186752&fm=21&gp=0.jpg",
//				"http://t1.baidu.com/it/u=120836520,664015236&fm=21&gp=0.jpg",
//				"http://t3.baidu.com/it/u=652509824,3944862727&fm=21&gp=0.jpg",
//				"http://t2.baidu.com/it/u=1044099978,1368997474&fm=21&gp=0.jpg",
//				"http://t2.baidu.com/it/u=707144795,1550545655&fm=21&gp=0.jpg",
//				"http://t1.baidu.com/it/u=4253340586,3127027860&fm=21&gp=0.jpg",
//				"http://t3.baidu.com/it/u=2226890413,714921982&fm=21&gp=0.jpg",
//				"http://t3.baidu.com/it/u=42129468,1103441825&fm=21&gp=0.jpg",
//				"http://t3.baidu.com/it/u=2820363020,3957482827&fm=21&gp=0.jpg",
//				"http://t2.baidu.com/it/u=214126949,426233786&fm=21&gp=0.jpg",
//				"http://t1.baidu.com/it/u=2017815537,2703642145&fm=21&gp=0.jpg",
//				"http://t1.baidu.com/it/u=2045248104,1139323035&fm=21&gp=0.jpg",
//				"http://t3.baidu.com/it/u=1689558378,377731691&fm=21&gp=0.jpg",
//				"http://t1.baidu.com/it/u=3427546413,1994813791&fm=21&gp=0.jpg",
//				"http://t2.baidu.com/it/u=646635970,483694629&fm=21&gp=0.jpg",
//				"http://t2.baidu.com/it/u=2047508977,679613856&fm=21&gp=0.jpg",
//				"http://t1.baidu.com/it/u=1255042358,1012271191&fm=21&gp=0.jpg",
//				"http://t1.baidu.com/it/u=3880300553,523725302&fm=21&gp=0.jpg",
//				"http://t3.baidu.com/it/u=2759664814,3628493402&fm=21&gp=0.jpg",
//				"http://t1.baidu.com/it/u=571489751,705305938&fm=21&gp=0.jpg",
//				"http://t1.baidu.com/it/u=2272275214,634515608&fm=21&gp=0.jpg",
//				"http://t2.baidu.com/it/u=2139998914,823110948&fm=21&gp=0.jpg",
//				"http://t2.baidu.com/it/u=2279561824,30340506&fm=21&gp=0.jpg",
//				"http://t1.baidu.com/it/u=2101442185,2623744203&fm=21&gp=0.jpg",
//				"http://t2.baidu.com/it/u=1789774978,199918158&fm=21&gp=0.jpg",
//				"http://t1.baidu.com/it/u=4055043197,1319184491&fm=21&gp=0.jpg",
//				"http://t3.baidu.com/it/u=1015903864,1834180729&fm=21&gp=0.jpg",
//				"http://t3.baidu.com/it/u=1409846906,2076781884&fm=11&gp=0.jpg",
//				"http://t3.baidu.com/it/u=1660091059,2059386169&fm=21&gp=0.jpg",
//				"http://t2.baidu.com/it/u=3369704113,26706246&fm=21&gp=0.jpg",
//				"http://t1.baidu.com/it/u=3424095261,3981003988&fm=21&gp=0.jpg",
//				"http://t1.baidu.com/it/u=3527665895,4114666283&fm=21&gp=0.jpg",
//				"http://t1.baidu.com/it/u=1844105655,525723588&fm=21&gp=0.jpg",
//				"http://t3.baidu.com/it/u=3608893144,1896388526&fm=21&gp=0.jpg",
//				"http://t2.baidu.com/it/u=2038089406,3130714573&fm=90&gp=0.jpg",
//				"http://t3.baidu.com/it/u=227808380,3136453026&fm=21&gp=0.jpg",
//				"http://t3.baidu.com/it/u=544852674,1121700014&fm=21&gp=0.jpg",
//				"http://t2.baidu.com/it/u=3534601112,3562169268&fm=90&gp=0.jpg",
//				"http://t3.baidu.com/it/u=3631268234,4279292350&fm=21&gp=0.jpg",
//				"http://t3.baidu.com/it/u=965775989,4255257907&fm=21&gp=0.jpg",
//				"http://t2.baidu.com/it/u=607246621,2781320268&fm=21&gp=0.jpg",
//				"http://t1.baidu.com/it/u=3232809279,3814487482&fm=21&gp=0.jpg",
//				"http://t3.baidu.com/it/u=3975366746,2723572669&fm=21&gp=0.jpg",
//				"http://t3.baidu.com/it/u=2431520077,2630337896&fm=21&gp=0.jpg",
//				"http://t3.baidu.com/it/u=1015261745,1617304452&fm=21&gp=0.jpg",
//				"http://t2.baidu.com/it/u=520581490,3874074331&fm=21&gp=0.jpg",
//				"http://t1.baidu.com/it/u=2523532388,1000133050&fm=21&gp=0.jpg",
//				"http://t1.baidu.com/it/u=3083964795,2767895881&fm=21&gp=0.jpg",
//				"http://t1.baidu.com/it/u=1298313360,3511843891&fm=21&gp=0.jpg",
//				"http://t1.baidu.com/it/u=4041996275,3908920454&fm=21&gp=0.jpg",
//				"http://t2.baidu.com/it/u=1641129289,3501117151&fm=21&gp=0.jpg",
//				"http://t1.baidu.com/it/u=3082263183,4215508978&fm=21&gp=0.jpg",
//				"http://t1.baidu.com/it/u=3870077888,2613879155&fm=21&gp=0.jpg",
//				"http://t2.baidu.com/it/u=2890111626,2830420160&fm=21&gp=0.jpg",
//				"http://t1.baidu.com/it/u=576798798,4044405895&fm=21&gp=0.jpg",
//				"http://t1.baidu.com/it/u=352888724,3669233413&fm=21&gp=0.jpg",
//				"http://t3.baidu.com/it/u=1502267477,3016356409&fm=21&gp=0.jpg",
//				"http://t3.baidu.com/it/u=3812457574,1236883868&fm=21&gp=0.jpg",
//				"http://t2.baidu.com/it/u=590545345,4084026187&fm=21&gp=0.jpg",
//				"http://t3.baidu.com/it/u=344451164,3464606628&fm=21&gp=0.jpg",
//				"http://t2.baidu.com/it/u=3418086983,3327970137&fm=21&gp=0.jpg",
//				"http://t1.baidu.com/it/u=2323648978,3904476708&fm=21&gp=0.jpg",
//				"http://t3.baidu.com/it/u=3443294897,4109616778&fm=21&gp=0.jpg",
//				"http://t1.baidu.com/it/u=3701112687,1840448557&fm=21&gp=0.jpg",
//				"http://t3.baidu.com/it/u=224511217,3777152656&fm=21&gp=0.jpg",
//				"http://t1.baidu.com/it/u=3634318279,2369246232&fm=21&gp=0.jpg",
//				"http://t2.baidu.com/it/u=2419687306,3185888872&fm=21&gp=0.jpg",
//				"http://t2.baidu.com/it/u=3393363359,1405243185&fm=21&gp=0.jpg",
//				"http://t3.baidu.com/it/u=3535321849,2088000626&fm=21&gp=0.jpg",
//				"http://t2.baidu.com/it/u=2609477595,3720579245&fm=21&gp=0.jpg",
//				"http://t3.baidu.com/it/u=518359795,3386058844&fm=21&gp=0.jpg",
//				"http://t2.baidu.com/it/u=227662709,4183083678&fm=21&gp=0.jpg",
//				"http://t1.baidu.com/it/u=883637497,219186752&fm=21&gp=0.jpg",
//				"http://t1.baidu.com/it/u=120836520,664015236&fm=21&gp=0.jpg",
//				"http://t3.baidu.com/it/u=652509824,3944862727&fm=21&gp=0.jpg",
//				"http://t2.baidu.com/it/u=1044099978,1368997474&fm=21&gp=0.jpg",
//				"http://t2.baidu.com/it/u=707144795,1550545655&fm=21&gp=0.jpg",
//				"http://t1.baidu.com/it/u=4253340586,3127027860&fm=21&gp=0.jpg",
//				"http://t3.baidu.com/it/u=2226890413,714921982&fm=21&gp=0.jpg",
//				"http://t3.baidu.com/it/u=42129468,1103441825&fm=21&gp=0.jpg",
//				"http://img0.bdstatic.com/img/image/ertonghua_200x66.jpg",
//				"http://img0.bdstatic.com/img/image/verlogo.gif",
//				"http://t3.baidu.com/it/u=1625525513,4273792612&fm=23&gp=0.jpg",
//				"http://t1.baidu.com/it/u=4253340586,3127027860&fm=23&gp=0.jpg",
//				"http://t3.baidu.com/it/u=42129468,1103441825&fm=21&gp=0.jpg",
//				"http://t1.baidu.com/it/u=4065430292,437124983&fm=11&gp=0.jpg",
//				"http://t1.baidu.com/it/u=875026196,135118555&fm=90&gp=0.jpg",
//				"http://t2.baidu.com/it/u=2636605022,2354391240&fm=90&gp=0.jpg"
		// "http://upload.wikimedia.org/wikipedia/commons/thumb/d/d3/Newtons_cradle_animation_book_2.gif/200px-Newtons_cradle_animation_book_2.gif"
		// ,"http://think.bigchief.it/wp-content/files/2012/08/Gif-ball.gif"
		};
		for (final String url : urls) {
			UIO.get(new Callback<InputStream>() {

				@Override
				public void callback(InputStream in) {
					try {
						byte[] result = IOUtil.loadBytesAndClose(in);

						GifDecoder gd = new GifDecoder(
								new ByteArrayInputStream(result));
						System.out.println(gd.getWidth() + "," + gd.getHeight()
								+ "#" + gd.getDelay());
						System.out.println("#" + gd.isGif89() + "/"
								+ gd.isAnimate());
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}

				@Override
				public void error(Throwable ex, boolean callbackError) {
					ex.printStackTrace();
				}

			}, url);
		}

		Robolectric.unPauseMainLooper();
		end();

	}

	public void end() throws Exception {

		List<AsynTask> taskList = getTaskList("taskList");
		List<AsynTask> runningTaskList = getTaskList("runningTaskList");
		// List<AsynTask> pauseTaskList = getTaskList("pauseTaskList");
		while (true) {
			// System.err.println(taskList+"/"+runningTaskList+"/"+pauseTaskList);
			Thread.sleep(200);
			if (runningTaskList.isEmpty() && taskList.isEmpty()/*
																 * &&
																 * pauseTaskList
																 * .isEmpty()
																 */) {
				break;
			}
		}
		Thread.sleep(500);
	}

	private List<AsynTask> getTaskList(String name)
			throws NoSuchFieldException, IllegalAccessException {
		Field field = HttpSupport.class.getDeclaredField("asyn");
		field.setAccessible(true);
		Object asyn = field.get(HttpSupport.INSTANCE);
		field = AsynImpl.class.getDeclaredField(name);
		field.setAccessible(true);
		List<AsynTask> runningTaskList = (List<AsynTask>) field.get(asyn);
		return runningTaskList;
	}
}
