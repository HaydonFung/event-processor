package org.fkjava.weixin;

import org.fkjava.weixin.domain.InMessage;
import org.fkjava.weixin.domain.event.EventInMessage;
import org.fkjava.weixin.processors.EventMessageProcessor;
import org.fkjava.weixin.service.JsonRedisSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.util.xml.StaxUtils;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;

// 实现ApplicationContextAware接口的目的：为了让当前对象能够得到Spring容器本身，能够通过Spring的容器来找到里面的Bean
@SpringBootApplication
public class EventProcessorApplication //
		implements ApplicationContextAware
		// 为了让非WEB应用能够一直等待信息的到来，必须实现CommandLineRunner接口
		, CommandLineRunner//
		, DisposableBean {

	private static final Logger LOG = LoggerFactory.getLogger(EventProcessorApplication.class);
	private ApplicationContext ctx;
	// 运行器的监视器，将会在这个监视器上等待停止的通知
	private final Object runnerMonitor = new Object();

	// 此程序中，run方法必须实现，否则无法收到消息！
	@Override
	public void run(String... args) throws Exception {
		// 实现CommandLineRunner接口，在Spring Boot项目启动之初执行的
		// 等待退出通知
		Thread t = new Thread(() -> {
			synchronized (runnerMonitor) {
				try {
					runnerMonitor.wait();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		});
		t.start();
	}

	@Override
	public void destroy() throws Exception {
		// 实现DisposableBean接口，在Spring容器销毁的时候执行的
		// 发送退出通知
		synchronized (runnerMonitor) {
			runnerMonitor.notify();
		}
	}

	// 这个方法，会在当前实例创建之后，由Spring自己调用，而Spring会把它本身传入进来
	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		ctx = applicationContext;
	}

	@Bean()
	public XmlMapper xmlMapper() {
		XmlMapper mapper = new XmlMapper(StaxUtils.createDefensiveInputFactory());
		return mapper;
	}

	// RedisTemplate是一个模板，用于访问数据库的！
	@Bean // 把对象放入容器里面
	public RedisTemplate<String, ? extends InMessage> inMessageTemplate(//
			// 获取Redis的连接工厂，这个配置是由Spring Boot自动完成，只需要这里说需要，然后就有了！
			// 为了让Spring Boot能够完成自动化配置，必须有spring.redis前缀的配置参数。
			@Autowired RedisConnectionFactory connectionFactory) {

		RedisTemplate<String, ? extends InMessage> template = new RedisTemplate<>();
		template.setConnectionFactory(connectionFactory);
		// 使用序列化程序完成对象的序列化和反序列化，可以自定义
		// 序列化程序负责Java对象和其他格式的数据相互转换。
		// JSON是一种纯文本的格式，非常方便在网络上传输。
		template.setValueSerializer(jsonRedisSerializer());

		return template;
	}

	@Bean
	public JsonRedisSerializer<InMessage> jsonRedisSerializer() {
		return new JsonRedisSerializer<InMessage>();
	}

	@Bean
	public RedisMessageListenerContainer messageListenerContainer(//
			@Autowired RedisConnectionFactory connectionFactory) {
		RedisMessageListenerContainer c = new RedisMessageListenerContainer();

		// 设置连接工厂，设置连接工厂以后才能连接到消息队列
		c.setConnectionFactory(connectionFactory);

		// 订阅event消息，当队列中有event消息我们就可以收到
		ChannelTopic topic = new ChannelTopic("fengzhe_event");

		// 使用匿名内部类实现一个监听器
//		MessageListener listener = new MessageListener() {
//
//			@Override
//			public void onMessage(Message message, byte[] pattern) {
//				
//			}
//		};

		//
		MessageListener listener = (message, pattern) -> {
//			byte[] channel = message.getChannel();// 通道名称
			byte[] body = message.getBody();// 消息内容

			// 把消息转换为Java对象
			JsonRedisSerializer<InMessage> serializer = jsonRedisSerializer();
			InMessage msg = serializer.deserialize(body);

			// 强制转换，然后根据消息的事件类型，执行不同的业务
			EventInMessage event = (EventInMessage) msg;
			String eventType = event.getEvent();// 获取事件类型
			eventType = eventType.toLowerCase();// 转换为小写

			// 调用消息的处理器，进行具体的消息处理
			String beanName = eventType + "MessageProcessor";
			EventMessageProcessor mp = (EventMessageProcessor) ctx.getBean(beanName);
			if (mp == null) {
				LOG.error("事件 {} 没有找到对应的处理器", eventType);
			} else {
				mp.onMessage(event);
			}
		};
		c.addMessageListener(listener, topic);

		return c;
	}

	public static void main(String[] args) {
		SpringApplication.run(EventProcessorApplication.class, args);
	}
}
