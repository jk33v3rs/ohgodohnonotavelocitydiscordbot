package com.keevers.chumpcode;

import com.keevers.logging.CustomLogger;

public class ChumpCodeChecker {
    private static final CustomLogger logger = CustomLogger.getLogger();

    public ChumpCodeChecker() {}

    public boolean isChumpCode(String value) {
        return switch (value) {
            case "NeverGonnaGive", "YouUpNever", "GonnaLetYou", "DownNeverGonna",
                 "RunAroundAnd", "DesertYouNever", "GonnaMakeYou", "CryNeverGonna",
                 "SayGoodbyeNever", "TellALie", "AndHurtYou" -> true;
            default -> false;
        };
    }

    public void printChumpCodeMessage() {
        logger.severe("chump code");
    }
}