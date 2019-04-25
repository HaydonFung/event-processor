package org.fkjava.weixin.service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpClient.Version;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.Charset;

import org.fkjava.weixin.domain.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * 微信的代理接口，所有跟微信服务器的主动通讯（我们请求微信），都通过此类里面的方法进行封装
 * 
 * @author lwq
 *
 */
@Service
public class WeiXinProxy {

	private static final Logger LOG = LoggerFactory.getLogger(WeiXinProxy.class);
	@Autowired
	private TokenManager tokenManager;

	/**
	 * 利用OpenID到微信的服务器里面获取用户信息
	 * 
	 * @param openId
	 * @return
	 */
	public User getWxUser(String openId) {
		String accessToken = this.tokenManager.getToken();

		String url = "https://api.weixin.qq.com/cgi-bin/user/info"//
				+ "?access_token=" + accessToken//
				+ "&openid=" + openId//
				+ "&lang=zh_CN";

		// 1.创建HttpClient对象
		// 在Java 11才内置了HttpClient，如果是早期JDK需要使用第三方的jar文件
		HttpClient client = HttpClient.newBuilder()//
				.version(Version.HTTP_1_1)// 设置HTTP 1.1的协议版本
				.build();

		// 2.创建HttpRequest对象
		HttpRequest request = HttpRequest.newBuilder(URI.create(url))//
				.GET()// 以GET方式发送请求
				.build();

		// 3.调用远程接口，返回JSON
		// BodyHandlers里面包含了许多内置的请求体、响应体的处理程序，ofString意思是使用String方式返回
		// Charset.forName("UTF-8")指定字符编码
		try {
			HttpResponse<String> response = client.send(request, BodyHandlers.ofString(Charset.forName("UTF-8")));

			// 4.把返回的JSON转换为Java对象
			String json = response.body();// 响应体
			
			LOG.trace("获取用户信息的返回：\n{}", json);

			if (json.indexOf("errcode") > 0) {
				// 出现了问题
				throw new RuntimeException("获取用户信息出现问题：" + json);
			}
			ObjectMapper mapper = new ObjectMapper();
			User user = mapper.readValue(json, User.class);

			return user;
		} catch (Exception e) {
			// 不处理异常，直接包异常封装以后再抛出去
			throw new RuntimeException("获取令牌出现问题：" + e.getLocalizedMessage(), e);
		}
	}

	/**
	 * 通过客服接口，发送文本信息给指定的用户
	 * 
	 * @param openId 接收者
	 * @param string 文本信息的内容
	 */
	public void sendText(String openId, String string) {
	}
}
