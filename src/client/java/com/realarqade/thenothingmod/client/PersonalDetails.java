package com.realarqade.thenothingmod.client;

import net.minecraft.client.Minecraft;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * "placeholder text": the few details the game can read from the player's own computer without asking anyone anything, used to
 * make the effects feel personal (the Desktop notes, the fake corruption screen).
 * <p>
 * The rules, which are the point of this class:
 * <ul>
 *   <li>Everything here is read locally and shown only to that player, on their own screen or in a file on their own computer. None of it
 *   is ever sent to the server, to other players, or anywhere over the network.</li>
 *   <li>It is deliberately limited: the computer's user name and name, the account's Minecraft name and the local time. Never an IP
 *   address, a location, a real name guessed from anything, files, or anything that needs the internet.</li>
 *   <li>It is off when Word0949 Mode is on (so nothing personal can end up on a stream), and it has a switch of its own in the settings.</li>
 * </ul>
 */
final class PersonalDetails {
    private PersonalDetails() {
    }

    /** False in Word0949 Mode, or when the player switched it off. Check this before using anything else here. */
    static boolean allowed() {
        DustConfig config = DustConfig.get();
        return config.personalDetails && !config.streamerMode;
    }

    static String userName() {
        String name = System.getProperty("user.name");
        return name == null || name.isEmpty() ? "someone" : name;
    }

    static String machine() {
        String name = System.getenv("COMPUTERNAME");
        if (name == null) {
            name = System.getenv("HOSTNAME");
        }
        return name == null || name.isEmpty() ? "this machine" : name;
    }

    static String accountName() {
        try {
            return Minecraft.getInstance().getUser().getName();
        } catch (Throwable t) {
            return "player";
        }
    }

    static String localTime() {
        return ZonedDateTime.now().format(DateTimeFormatter.ofPattern("EEEE HH:mm")) + " (" + ZoneId.systemDefault() + ")";
    }

    /** The lines of a "record found" block, or an empty list when not allowed. */
    static List<String> recordLines() {
        List<String> lines = new ArrayList<>();
        if (!allowed()) {
            return lines;
        }
        lines.add("RECORD FOUND");
        lines.add("placeholder text" + accountName());
        lines.add("placeholder text" + userName());
        lines.add("placeholder text" + machine());
        lines.add("placeholder text" + localTime());
        lines.add("placeholder text");
        return lines;
    }
}
