package org.fkjava.weixin.service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpClient.Version;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.Charset;

import org.fkjava.weixin.domain.AccessToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

@Service // 把对象放入Spring容器里面
public class TokenManager {

	private static final Logger LOG = LoggerFactory.getLogger(TokenManager.class);

	public String getToken() {
		// 在微信的公众号没有认证通过之前，先使用开发者工具里面的测试号来进行测试
		
//		String appId = "wxfbe4d402fd892349";
//		String appSecret = "8d655e3b09599766fe93b8c00c9aa4ee";
		String appId = "wxd900194bf71fb1f1";
		String appSecret = "b23094b6cd4e9a900507f1e4d584c3f8";
		String url = "https://api.weixin.qq.com/cgi-bin/token"//
				+ "?grant_type=client_credential"//
				+ "&appid=" + appId//
				+ "&secret=" + appSecret;

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
			LOG.trace("获取令牌的返回：\n{}", json);

			if (json.indexOf("errcode") > 0) {
				// 出现了问题
				throw new RuntimeException("获取令牌出现问题：" + json);
			}
			ObjectMapper mapper = new ObjectMapper();
			AccessToken at = mapper.readValue(json, AccessToken.class);

			// 返回令牌
			return at.getAccessToken();
		} catch (Exception e) {
			// 不处理异常，直接包异常封装以后再抛出去
			throw new RuntimeException("获取令牌出现问题：" + e.getLocalizedMessage(), e);
		}
	}
}
