package org.springframework.mns.support;

public abstract class MnsHeaders {

    public static final String PREFIX = "mns_";

    public static final String CLOUD_QUEUE = PREFIX + "cloudQueue";

    public static final String RECEIPT_HANDLER = PREFIX + "receiptHandler";

    public static final String PRIORITY = PREFIX + "priority";

    public static final String DEQUEUE_COUNT = PREFIX + "dequeueCount";

}
