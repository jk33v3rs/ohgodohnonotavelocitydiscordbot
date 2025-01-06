package com.keevers.chumpcode;

import org.slf4j.Logger;

public class ChumpCodeChecker {
    private final Logger logger;

    public ChumpCodeChecker(Logger logger) {
        this.logger = logger;
    }

    public boolean isChumpCode(String value) {
        return value.equals("NeverGonnaGive") || value.equals("YouUpNever") || value.equals("GonnaLetYou") || value.equals("DownNeverGonna") ||
               value.equals("RunAroundAnd") || value.equals("DesertYouNever") || value.equals("GonnaMakeYou") || value.equals("CryNeverGonna") ||
               value.equals("SayGoodbyeNever") || value.equals("TellALie") || value.equals("AndHurtYou");
    }

    public void printChumpCodeMessage() {
        logger.error("chump code");
    }
}