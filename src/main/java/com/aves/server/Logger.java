package com.aves.server;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;

public class Logger {
    public static java.util.logging.Logger getLOGGER() {
        return LOGGER;
    }

    private final static java.util.logging.Logger LOGGER = java.util.logging.Logger.getLogger("com.aves.logger");
    private static AtomicInteger errorCount = new AtomicInteger();
    private static AtomicInteger warningCount = new AtomicInteger();

    static {
        java.util.logging.Logger.getLogger("org.apache.http.wire").setLevel(Level.SEVERE);
        java.util.logging.Logger.getLogger("org.apache.http.headers").setLevel(Level.SEVERE);
        System.setProperty("org.apache.commons.logging.Log", "org.apache.commons.logging.impl.SimpleLog");
        System.setProperty("org.apache.commons.logging.simplelog.showdatetime", "true");
        System.setProperty("org.apache.commons.logging.simplelog.log.httpclient.wire", "ERROR");
        System.setProperty("org.apache.commons.logging.simplelog.log.org.apache.http", "ERROR");
        System.setProperty("org.apache.commons.logging.simplelog.log.org.apache.http.headers", "ERROR");

        for (Handler handler : LOGGER.getHandlers()) {
            handler.setFormatter(new BotFormatter());
        }
    }

    public static void debug(String msg) {
        LOGGER.fine(msg);
    }

    public static void debug(String format, Object... args) {
        LOGGER.fine(String.format(format, args));
    }

    public static void info(String msg) {
        LOGGER.info(msg);
    }

    public static void info(String format, Object... args) {
        LOGGER.info(String.format(format, args));
    }

    public static void error(String msg) {
        errorCount.incrementAndGet();
        LOGGER.severe(msg);
    }

    public static void error(String format, Object... args) {
        errorCount.incrementAndGet();
        LOGGER.severe(String.format(format, args));
    }

    public static void warning(String msg) {
        warningCount.incrementAndGet();
        LOGGER.warning(msg);
    }

    public static void warning(String format, Object... args) {
        warningCount.incrementAndGet();
        LOGGER.warning(String.format(format, args));
    }

    public static int getErrorCount() {
        return errorCount.get();
    }

    public static int getWarningCount() {
        return warningCount.get();
    }

    public static Level getLevel() {
        return LOGGER.getLevel();
    }

    static class BotFormatter extends Formatter {
        private static final DateFormat df = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");

        @Override
        public String format(LogRecord record) {
            StringBuilder builder = new StringBuilder();
            builder.append(df.format(new Date(record.getMillis()))).append(" - ");
            // builder.append("[").append(record.getSourceClassName()).append(".");
            // builder.append(record.getSourceMethodName()).append("] - ");
            builder.append("[").append(record.getLevel()).append("] - ");
            builder.append(formatMessage(record));
            builder.append("\n");
            return builder.toString();
        }

        @Override
        public String getHead(Handler h) {
            return super.getHead(h);
        }

        @Override
        public String getTail(Handler h) {
            return super.getTail(h);
        }
    }
}
