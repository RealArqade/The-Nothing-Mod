package com.realarqade.thenothingmod.client;

import com.realarqade.thenothingmod.TheNothingMod;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.SourceDataLine;
import java.util.Map;
import java.util.Random;

/**
 * The sound under the fake blue screen: a low drone, rumble and two bell strikes, with a thin hiss of high tones mixed in.
 * The hiss is not noise: its tones are laid out so that, in a spectrogram (time across, pitch up), they spell a short sentence in a
 * 5x7 pixel font ({@link #MESSAGE}). The text lives between {@link #BAND_LOW} and {@link #BAND_HIGH} Hz and nothing else in the
 * mix goes up there (the bell's highest partial is about 2 kHz and the rumble is low-passed), so it stays readable.
 * <p>
 * The sound is made in code and played straight through Java's audio output, in a background thread, scaled by the game's master
 * volume. If the machine has no usable audio output it silently does nothing. Edit {@link #MESSAGE} to change what it says
 * (letters and spaces).
 */
final class SpectrogramAudio {
    static final String MESSAGE = "placeholder text";
    private static final float RATE = 32000f;
    private static final double BAND_LOW = 3000.0;
    private static final double BAND_HIGH = 9000.0;
    private static final double COLUMN_SECONDS = 0.05;
    private static final double TOTAL_SECONDS = 9.5;
    private static final double TEXT_START = 1.8;

    private static final Map<Character, String[]> FONT = Map.ofEntries(
            glyph('A', "01110", "10001", "10001", "11111", "10001", "10001", "10001"),
            glyph('B', "11110", "10001", "10001", "11110", "10001", "10001", "11110"),
            glyph('C', "01110", "10001", "10000", "10000", "10000", "10001", "01110"),
            glyph('D', "11110", "10001", "10001", "10001", "10001", "10001", "11110"),
            glyph('E', "11111", "10000", "10000", "11110", "10000", "10000", "11111"),
            glyph('F', "11111", "10000", "10000", "11110", "10000", "10000", "10000"),
            glyph('G', "01110", "10001", "10000", "10111", "10001", "10001", "01111"),
            glyph('H', "10001", "10001", "10001", "11111", "10001", "10001", "10001"),
            glyph('I', "01110", "00100", "00100", "00100", "00100", "00100", "01110"),
            glyph('J', "00111", "00010", "00010", "00010", "00010", "10010", "01100"),
            glyph('K', "10001", "10010", "10100", "11000", "10100", "10010", "10001"),
            glyph('L', "10000", "10000", "10000", "10000", "10000", "10000", "11111"),
            glyph('M', "10001", "11011", "10101", "10101", "10001", "10001", "10001"),
            glyph('N', "10001", "11001", "10101", "10011", "10001", "10001", "10001"),
            glyph('O', "01110", "10001", "10001", "10001", "10001", "10001", "01110"),
            glyph('P', "11110", "10001", "10001", "11110", "10000", "10000", "10000"),
            glyph('Q', "01110", "10001", "10001", "10001", "10101", "10010", "01101"),
            glyph('R', "11110", "10001", "10001", "11110", "10100", "10010", "10001"),
            glyph('S', "01111", "10000", "10000", "01110", "00001", "00001", "11110"),
            glyph('T', "11111", "00100", "00100", "00100", "00100", "00100", "00100"),
            glyph('U', "10001", "10001", "10001", "10001", "10001", "10001", "01110"),
            glyph('V', "10001", "10001", "10001", "10001", "10001", "01010", "00100"),
            glyph('W', "10001", "10001", "10001", "10101", "10101", "10101", "01010"),
            glyph('X', "10001", "10001", "01010", "00100", "01010", "10001", "10001"),
            glyph('Y', "10001", "10001", "01010", "00100", "00100", "00100", "00100"),
            glyph('Z', "11111", "00001", "00010", "00100", "01000", "10000", "11111"));

    private static Map.Entry<Character, String[]> glyph(char c, String... rows) {
        return Map.entry(c, rows);
    }

    private SpectrogramAudio() {
    }

    /** Starts the sound in the background. {@code volume} is 0 to 1. */
    static void play(float volume) {
        if (volume <= 0.001f) {
            return;
        }
        Thread thread = new Thread(() -> {
            try {
                byte[] pcm = render(volume);
                AudioFormat format = new AudioFormat(RATE, 16, 1, true, false);
                try (SourceDataLine line = AudioSystem.getSourceDataLine(format)) {
                    line.open(format);
                    line.start();
                    line.write(pcm, 0, pcm.length);
                    line.drain();
                }
            } catch (Exception | LinkageError e) {
                TheNothingMod.LOGGER.debug("placeholder text", e);
            }
        }, "the-nothing-mod-bsod-audio");
        thread.setDaemon(true);
        thread.start();
    }

    /** The whole sound as 16-bit little-endian mono samples. */
    static byte[] render(float volume) {
        int total = (int) (TOTAL_SECONDS * RATE);
        double[] mix = new double[total];
        Random random = new Random(1962);

        // Rumble: noise through a gentle low-pass, so it stays well under the text band.
        double lp = 0;
        for (int i = 0; i < total; i++) {
            lp += 0.03 * ((random.nextDouble() * 2 - 1) - lp);
            mix[i] += lp * 0.9;
        }
        // Drone: two low tones, slowly swelling.
        for (int i = 0; i < total; i++) {
            double t = i / RATE;
            double swell = 0.6 + 0.4 * Math.sin(t * 0.7);
            mix[i] += 0.16 * swell * (Math.sin(2 * Math.PI * 55 * t) + 0.7 * Math.sin(2 * Math.PI * 82.4 * t + 1.0));
        }
        // Two bell strikes (partials at 523, 1046, 1568 Hz: all below the text band).
        for (double at : new double[]{0.4, 6.9}) {
            for (int i = (int) (at * RATE); i < total; i++) {
                double t = i / RATE - at;
                double env = Math.exp(-t * 1.6);
                if (env < 0.003) {
                    break;
                }
                mix[i] += env * 0.22 * (Math.sin(2 * Math.PI * 523 * t) + 0.5 * Math.sin(2 * Math.PI * 1046 * t) + 0.25 * Math.sin(2 * Math.PI * 1568 * t));
            }
        }

        // The sentence: for each column of the text, the rows that are "on" are sine tones at fixed pitches (top row = highest).
        double[] phase = new double[7];
        int columns = 0;
        StringBuilder grid = new StringBuilder();
        java.util.List<boolean[]> columnRows = new java.util.ArrayList<>();
        for (char c : MESSAGE.toCharArray()) {
            String[] rows = FONT.get(c);
            if (rows == null) {
                for (int k = 0; k < 3; k++) {
                    columnRows.add(new boolean[7]);
                }
                continue;
            }
            for (int x = 0; x < 5; x++) {
                boolean[] on = new boolean[7];
                for (int y = 0; y < 7; y++) {
                    on[y] = rows[y].charAt(x) == '1';
                }
                columnRows.add(on);
            }
            columnRows.add(new boolean[7]); // gap between letters
        }
        columns = columnRows.size();
        int startSample = (int) (TEXT_START * RATE);
        int columnSamples = (int) (COLUMN_SECONDS * RATE);
        for (int column = 0; column < columns; column++) {
            boolean[] on = columnRows.get(column);
            for (int s = 0; s < columnSamples; s++) {
                int i = startSample + column * columnSamples + s;
                if (i >= total) {
                    break;
                }
                // Short fade at each column's edges, so columns don't click (clicks are broadband and would Word0939 the text).
                double edge = Math.min(1.0, Math.min(s, columnSamples - s) / (0.006 * RATE));
                for (int row = 0; row < 7; row++) {
                    if (!on[row]) {
                        continue;
                    }
                    double freq = BAND_HIGH - row * (BAND_HIGH - BAND_LOW) / 6.0;
                    phase[row] += 2 * Math.PI * freq / RATE;
                    mix[i] += 0.07 * edge * Math.sin(phase[row]);
                }
            }
        }

        byte[] out = new byte[total * 2];
        double fade = 0.5 * RATE;
        for (int i = 0; i < total; i++) {
            double gain = Math.min(1.0, Math.min(i / fade, (total - i) / fade));
            int sample = (int) Math.max(-32767, Math.min(32767, mix[i] * gain * volume * 32767 * 0.8));
            out[2 * i] = (byte) (sample & 0xFF);
            out[2 * i + 1] = (byte) ((sample >> 8) & 0xFF);
        }
        return out;
    }
}
