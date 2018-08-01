package org.apache.geode.redis.internal;

public class ZSetRangeException extends Exception {
    private static final long serialVersionUID = 4707944288714910949L;

    public ZSetRangeException() {
        super();
    }

    public ZSetRangeException(String message) {
        super(message);
    }

    public ZSetRangeException(Throwable cause) {
        super(cause);
    }

    public ZSetRangeException(String message, Throwable cause) {
        super(message, cause);
    }
}
