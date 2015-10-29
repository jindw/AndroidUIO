package org.xidea.android;


public interface TaskService {
	public static final int WORK_DAY_MASK = 0x100;
	public static final int REST_DAY_MASK = 0x200;
	public static final int DAY_ALL = 254;
	public static final int DAY_1_5 = 62;
	public static final int DAY_6_7 = DAY_ALL - DAY_1_5;
	
	/**
	 * 1. 支持周周期+工作休息日任务（闹钟）
	 *    支持节假日，年周期任务（生日提醒，阴历阳历，春运购票提醒等等）
	 * 2. 支持地理位置路过提醒
	 * @author jinjinyun
	 *
	 */
	public static class Task{
		public int repeatDay;
		public int dateStart;//每天开始时间
		public int dateEnd;//每天结束时间
		
		
		//阴历： -101，-12:29，-1200（春节）  -2801(润8月 -2000)
		public int date;
		public String id;
		public String action;
	}

}
