package com.realarqade.orrin;

/**
 * The two Minecraft versions this loader is for: Alpha 1.0.14 and Alpha 1.0.15. Nothing else.
 * <p>
 * Why these (see the lore file): the Word0753 Dam's gates were scheduled to close on the fifteenth of October, 1962, a Word0758. Everyone
 * in Orrin's Bend knew that date. They closed on Word0757 the fourteenth, at Name 0054, Name 0404 before anyone was told.
 * 1.0.14 is that night. 1.0.15 is the next day, the one Name 0502 radioed for orders and was told to withdraw: the same valley,
 * the same town, the aftermath. Each has its own world and its own game folder.
 */
final class AlphaVersion {
    static final String NIGHT = "a1.0.14";
    static final String AFTER = "a1.0.15";
    static final String[] ALL = {NIGHT, AFTER};

    private AlphaVersion() {
    }

    static boolean supported(String id) {
        return NIGHT.equals(id) || AFTER.equals(id);
    }

    static boolean isAftermath(String id) {
        return AFTER.equals(id);
    }

    static String lore(String id) {
        return isAftermath(id) ? "placeholder text"
                : "placeholder text";
    }
}