package org.fkjava.weixin.repository;

import org.fkjava.weixin.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

//Spring Data会自动根据此主键生成实例
//并且这个实例会放入Spring的容器里面，可以随时获取使用
@Repository
// 继承JpaRepository以后，会得到大部分的CRUD操作方法，基本上不需要写代码可以完成数据库的操作
// <User, String> 第一个参数指定哪个类的对象（对应哪个表），第二个参数指定主键的数据类型（@Id注解）
public interface UserRepository extends JpaRepository<User, String> {

	// Spring Data会自动根据这个方法名生成一个查询语句
	// select * from wx_user where open_id = ?
	// 并且还会自动把查询得到的结果，转换为User对象
	User findByOpenId(String openId);
}
