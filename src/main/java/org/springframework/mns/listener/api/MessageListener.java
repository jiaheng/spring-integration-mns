package org.springframework.mns.listener.api;

import com.aliyun.mns.model.Message;

import java.util.List;

/**
 * Listener interface to receive asynchronous delivery of MNS Messages.
 *
 * @author jiaheng
 */
@FunctionalInterface
public interface MessageListener {

    void onMessage(Message message);

    default void onMessageBatch(List<Message> messages) {
        throw new UnsupportedOperationException("This listener does not suppoer message batches");
    }

}
