package org.xidea.android.test.network;

import java.util.List;

/**
 * 用户信息数据模型
 */
public class User {
	//性别常量定义
	public enum Gender {
		 Unknow,Male,Female
	}
	//用户名
	public String name;
	//性别
	public Gender gender = Gender.Unknow;
	//头像
	public String avatar;
	//邮箱
	public String email;
	//好友列表（测试复杂数据集合）
	public List<User> friends;
}
