package com.cinebook;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** CineBook CLI entry point. */
public class App {

    private static final Logger log = LoggerFactory.getLogger(App.class);

    public static void main(String[] args) {
        log.info("CineBook v1.0 starting...");
        System.out.println("================================");
        System.out.println("       CineBook  v1.0");
        System.out.println("================================");
        System.out.println(" Starting up... (scaffold only)");
        System.out.println("================================");
    }
}
