package org.fkjava.weixin.processors.impl;

import org.fkjava.weixin.domain.event.EventInMessage;
import org.fkjava.weixin.processors.EventMessageProcessor;
import org.springframework.stereotype.Service;

// 把对象加入Spring容器，并且根据事件处理查找Bean的规则给一个名字
@Service("subscribeMessageProcessor")
public class SubscribeEventMessageProcessor implements EventMessageProcessor {

	@Override
	public void onMessage(EventInMessage event) {
		System.out.println("处理关注消息： " + event);
	}
}
