package com.dappstp.dappstp.aspect.scraping.context;

public class ScrapingContextHolder {

    private static final ThreadLocal<ScrapingContext> contextHolder = new ThreadLocal<>();

    public static void setContext(ScrapingContext context) {
        contextHolder.set(context);
    }

    public static ScrapingContext getContext() {
        return contextHolder.get();
    }

    public static void clearContext() {
        contextHolder.remove();
    }
}