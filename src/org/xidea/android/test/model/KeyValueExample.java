package org.xidea.android.test.model;

import java.util.Map;
import java.util.Set;

import org.xidea.android.KeyValueStorage;

public interface KeyValueExample extends KeyValueStorage<KeyValueExample> {

    
	/**
	 * 设置字符串 
	 */
    public String getName();
    public void setName(String name);
    
	/**
	 * 设置名为age，类型为int属性，默认值为0 
	 */
	public int getAge();
	public void setAge(int age);

	/**
	 * 设置复杂数据类型
	 * @return
	 */
	@DefaultValue(jsonValue = "{}")
	public Map<String,Integer> getMapValue();
	public void setMapValue( Map<String,Integer> map);
	

	/**
	 * 设置字符串集合
	 * @return
	 */
	@DefaultValue(jsonValue = "[]")
	public Set<String> getSetValue();
	public void setSet( Set<String> set);
	
	/** 
	 * 设置一个bool值，setter 可以返回 当前对象，方便连续调用， 如： 
	 * myStorage.beginTransaction().setCacheLength(65536).setCacheEnable(false).commit();
	 */
	public boolean getCacheEnable();
	public KeyValueExample setCacheEnable(boolean b);
	
	/**
	 * 设置一个默认值为1024的属性 
	 */
	@DefaultValue(1024)
	public int getCacheLength();
	public KeyValueExample setCacheLength(int value);
}
