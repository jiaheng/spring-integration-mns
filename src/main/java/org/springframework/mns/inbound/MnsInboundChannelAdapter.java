package org.springframework.mns.inbound;

import com.aliyun.mns.client.CloudQueue;
import com.aliyun.mns.client.MNSClient;
import com.aliyun.mns.model.Message;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.StopWatch;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.integration.context.OrderlyShutdownCapable;
import org.springframework.integration.endpoint.MessageProducerSupport;
import org.springframework.mns.listener.api.ChannelAwareMessageListener;
import org.springframework.mns.support.MnsHeaders;

import java.util.List;

@Slf4j
public class MnsInboundChannelAdapter extends MessageProducerSupport
        implements OrderlyShutdownCapable {

    private static final int DEGAULT_POLLING_WAIT_SECONDS = 30;
    private static final int DEFAULT_NUMBER_OF_CONSUMER = 1;
    private static final List<Integer> DEFAULT_RETRY_INTERVAL = ImmutableList.of(60);

    private final MNSClient mnsClient;
    private final String queueName;
    private final ChannelAwareMessageListener listener = new Listener();

    private int numberOfConsumer = DEFAULT_NUMBER_OF_CONSUMER;
    private TaskExecutor taskExecutor = new SimpleAsyncTaskExecutor();

    private int pollingWaitSeconds = DEGAULT_POLLING_WAIT_SECONDS;
    private CloudQueue cloudQueue;

    List<Integer> retryInterval = DEFAULT_RETRY_INTERVAL;

    private volatile boolean running = false;

    public MnsInboundChannelAdapter(final MNSClient mnsClient, final String queueName) {
        this.mnsClient = mnsClient;
        this.queueName = queueName;
    }

    @Override
    protected void onInit() {
        logger.info("Initializing MnsInboundChannerlAdapter {} with queueName: {}, Polling time; {}s");
        super.onInit();
        this.cloudQueue = mnsClient.getQueueRef(queueName);
    }

    @Override
    protected void doStart() {
        this.running = true;
        for (int i = 0; i < this.numberOfConsumer; i++) {
            taskExecutor.execute(() -> this.consumer(this.listener));
        }
    }

    @Override
    protected void doStop() {
        this.running = false;
    }

    @Override
    public int beforeShutdown() {
        this.stop();
        return 0;
    }

    @Override
    public int afterShutdown() {
        return 0;
    }

    private void consumer(ChannelAwareMessageListener listener) {
        while (running) {
            Message message = this.popMessage();

            if (message == null) {
                continue;
            }

            try {
                listener.onMessage(message, this.cloudQueue);
                // handle ack
            } catch (Exception e) {
                log.error("Error invoke listener.onMessage", e);
                // handle nack
            }
        }
    }

    private Message popMessage() {
        Message message;
        try {
            StopWatch stopWatch = new StopWatch();
            if (log.isTraceEnabled()) {
                stopWatch.start();
            }

            message = cloudQueue.popMessage(pollingWaitSeconds);

            if (message == null) {
                return null;
            }

            if (log.isTraceEnabled()) {
                stopWatch.stop();
                log.trace("Receive messageId[{}] from queue[{}], cost[{}ms]",
                        message.getMessageId(), queueName, stopWatch.getTime());
            }
        } catch (RuntimeException e) {
            log.error("Error receiving message from queue[{}]", queueName, e);
            return null;
        }
        return message;
    }

    protected class Listener implements ChannelAwareMessageListener {

        @Override
        public void onMessage(final Message message, final CloudQueue cloudQueue) {
            org.springframework.messaging.Message<String> messagingMessage = this.createMessage(message, cloudQueue);
            MnsInboundChannelAdapter.this.sendMessage(messagingMessage);
        }

        private org.springframework.messaging.Message<String> createMessage(final Message message, final CloudQueue cloudQueue) {
            String payload = message.getMessageBody();

            ImmutableMap.Builder<String, Object> headerBuilder = new ImmutableMap.Builder<>();

            headerBuilder.put(MnsHeaders.DEQUEUE_COUNT, message.getDequeueCount())
                    .put(MnsHeaders.PRIORITY, message.getPriority());

            return MnsInboundChannelAdapter.this.getMessageBuilderFactory()
                    .withPayload(payload)
                    .copyHeaders(headerBuilder.build())
                    .build();
        }
    }

}
