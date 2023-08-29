package org.springframework.mns.listener.api;

import com.aliyun.mns.client.CloudQueue;
import com.aliyun.mns.model.Message;

import java.util.List;

/**
 * A message listener that is aware of the Channel on which the message was received.
 *
 * @author jiaheng
 */
@FunctionalInterface
public interface ChannelAwareMessageListener extends MessageListener {

    /**
     * Callback for processing a received MNS message.
     * <p>Implementors are supposed to process the given Message,
     * typically sending reply messages through the given Session.
     * @param message the received MNS message (never <code>null</code>)
     * @param channel the underlying MNS Queue (never <code>null</code>)
     * @throws Exception Any.
     */
    void onMessage(Message message, CloudQueue cloudQueue) throws Exception; // NOSONAR

    @Override
    default void onMessage(Message message) {
        throw new IllegalStateException("Should never be called for a ChannelAwareMessageListener");
    }

    @SuppressWarnings("unused")
    default void onMessageBatch(List<Message> messages, CloudQueue cloudQueue) {
        throw new UnsupportedOperationException("This listener does not support message batches");
    }

}
