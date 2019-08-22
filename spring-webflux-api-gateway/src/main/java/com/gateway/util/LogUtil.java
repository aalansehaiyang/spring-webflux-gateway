package com.gateway.util;

import org.slf4j.Logger;

/**
 * @author onlyone
 */
public class LogUtil {

    public static void logRecord(Logger log, String message) {
        log.error(message);
    }

    public static void logRecord(Logger log, String message, int traceId) {
        log.error(message + " [" + traceId + "]");
    }
}
