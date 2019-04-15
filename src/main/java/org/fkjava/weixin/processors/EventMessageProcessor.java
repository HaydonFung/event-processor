package org.fkjava.weixin.processors;

import org.fkjava.weixin.domain.event.EventInMessage;

public interface EventMessageProcessor {

	void onMessage(EventInMessage event);

}
