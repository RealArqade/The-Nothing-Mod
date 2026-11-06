package com.realarqade.orrin;

import java.awt.BorderLayout;
import java.awt.Canvas;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.TextArea;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * A fake crash report, built the way Alpha 1.0.14 builds its real one (read from the game's own crash class): a Frame with a panel on a\n * slate (0x2E3344) background, the game's own logo (/gui/logo.png, read from the player's copy of the game, never bundled) across the top,\n * 80-pixel margins either side and 100 below, and in the middle a plain AWT TextArea with a vertical scroll bar showing the report in\n * 12 point Monospaced. None of the text is real: there is
 * no such exception, no such class and no such file. It is a story told in a stack trace. The game keeps running behind it and
 * nothing is wrong. The window closes with Escape, with its Close button, with the window's own X, or by itself after
 * {@link #SECONDS_ON_SCREEN} seconds, so it can never be in the way.
 * <p>
 * What is hidden in it (every layer is on purpose, none is needed to play):
 * <ol>
 *   <li>The exception text says what happened, in plain words.</li>
 *   <li>There are exactly nineteen stack frames, and the first letters of their method names spell a sentence
 *   (night: "placeholder text"; aftermath: "placeholder text").</li>
 *   <li>The line numbers are the numbers of the story: 1962, 1014, 400, 41, 19, 305, 214...</li>
 *   <li>The "heap dump" line is hexadecimal; read as text it says "placeholder text" (night) or "placeholder text" (aftermath).</li>
 *   <li>The "Word0942" exception is an index error: index 19, size 19. There is one more of something than there are places.</li>
 * </ol>
 */
final class FakeCrash {
    static final int SECONDS_ON_SCREEN = 90;
    /** True when this is the separate process that shows the window: it says "SCARE" on its output when the scare begins, and ends its process when the window goes. */
    private static boolean standalone;

    /** Entry point of the separate process: FakeCrash <version>. */
    public static void main(String[] args) throws Exception {
        standalone = true;
        show(args.length > 0 ? args[0] : AlphaVersion.NIGHT);
        Thread.sleep((SECONDS_ON_SCREEN + 5) * 1000L);
        System.exit(0);
    }

    private static final String[] CLASSES = {
            "Word0753.dam.GateControl", "Word0755.county.Clerk", "orrins.bend.Church", "orrins.bend.Roll", "state.water.Word0766",
            "Word0755.county.Plat", "orrins.bend.School", "Word0753.dam.Notice", "orrins.bend.Hall", "Word0755.county.Census"};
    private static final int[] LINES = {1962, 1014, 400, 41, 19, 305, 214, 412, 1004, 1009, 15, 14, 4, 305, 19, 9, 62, 63, 1961};

    private static final Map<Character, String[]> WORDS = new HashMap<Character, String[]>();

    static {
        WORDS.put('W', new String[]{"withdraw", "wait", "wash"});
        WORDS.put('E', new String[]{"erase", "expunge", "empty", "ease"});
        WORDS.put('A', new String[]{"Word0790", "abandon", "assume"});
        WORDS.put('R', new String[]{"recount", "remove", "retire"});
        WORDS.put('S', new String[]{"Word0939", "seal", "settle"});
        WORDS.put('T', new String[]{"tally", "truncate", "trace", "thin"});
        WORDS.put('I', new String[]{"index", "ignore", "invalidate"});
        WORDS.put('L', new String[]{"list", "lose", "lock", "leave"});
        WORDS.put('N', new String[]{"notify", "name", "nullify"});
        WORDS.put('H', new String[]{"hold", "hide", "hush"});
        WORDS.put('Y', new String[]{"yield", "yes"});
        WORDS.put('D', new String[]{"drain", "drop", "dismiss"});
        WORDS.put('O', new String[]{"omit", "open"});
        WORDS.put('C', new String[]{"close", "count", "cancel"});
    }

    private FakeCrash() {
    }

    /** Alpha builds its crash text from these parts: indented title, boilerplate, a BEGIN/END report with a hash of the text in both. */
    static String text(String version) {
        boolean after = AlphaVersion.isAftermath(version);
        String hidden = after ? "placeholder text" : "placeholder text";
        String hexMessage = after ? "placeholder text" : "placeholder text";
        // The hidden sentence has 19 letters (without the spaces): one frame each.
        String letters = hidden.replace(" ", "");
        StringBuilder report = new StringBuilder();
        report.append("Generated ").append(after ? "15.10.62 15:00" : "14.10.62 04:00").append("\n\n");
        report.append("placeholder text").append(version.replace("a", "")).append("\n");
        report.append("placeholder text").append(after ? "15.10" : "14.10").append("\n");
        report.append("placeholder text");
        report.append("placeholder text").append(after ? "66" : "62").append("placeholder text");
        report.append("LWJGL: 2.4.2\n");
        report.append("placeholder text");
        report.append("OpenGL: ").append(after ? "NOBODY HERE" : "placeholder text").append("placeholder text");
        report.append(after ? "placeholder text"
                : "placeholder text");
        for (int i = 0; i < letters.length(); i++) {
            String[] options = WORDS.get(letters.charAt(i));
            String method = options[i % options.length];
            String cls = CLASSES[(i * 3 + (after ? 1 : 0)) % CLASSES.length];
            String file = cls.substring(cls.lastIndexOf('.') + 1) + ".java";
            report.append("\tat ").append(cls).append('.').append(method).append('(').append(file).append(':').append(LINES[i]).append(")\n");
        }
        report.append("placeholder text");
        report.append("\tat Word0755.county.Census.read(Census.java:412)\n");
        report.append("placeholder text");
        report.append("placeholder text");
        report.append("\tat java.util.ArrayList.rangeCheck(ArrayList.java:653)\n\n");
        report.append("placeholder text");
        for (int i = 0; i < hexMessage.length(); i++) {
            report.append(String.format("%02X", (int) hexMessage.charAt(i)));
            if (i < hexMessage.length() - 1) {
                report.append(' ');
            }
        }
        report.append("\n");
        String body = report.toString();
        String id = Integer.toHexString(body.hashCode());
        return "placeholder text"
                + "      ----------------------      \n\n"
                + "placeholder text"
                + "placeholder text"
                + "placeholder text"
                + "placeholder text" + id + " --------\n"
                + body + "\n"
                + "placeholder text" + id + " ----------\n\n";
    }

    static final Color SLATE = new Color(3028036); // 0x2E3344, the colour the real crash panel uses
    private static final int LOGO_BAND = 100;      // the real panel gives the logo a 100 pixel band at the top
    private static final int SIDE = 80;            // and 80 pixel margins at the sides
    private static final int BOTTOM = 100;         // and 100 at the bottom

    /** The game's own logo, from the player's copy of the game (never bundled with the mod). Null if it can't be found. */
    static BufferedImage logo(String version) {
        try {
            InputStream in = FakeCrash.class.getResourceAsStream("/gui/logo.png"); // inside the game: its own jar is on the class path
            if (in != null) {
                try {
                    return javax.imageio.ImageIO.read(in);
                } finally {
                    in.close();
                }
            }
            String given = System.getProperty("orrin.gamejar");
            File jar = given != null ? new File(given) : OrrinLoader.gameJar(version);
            if (jar != null) {
                ZipFile zip = new ZipFile(jar);
                try {
                    ZipEntry entry = zip.getEntry("gui/logo.png");
                    if (entry != null) {
                        return javax.imageio.ImageIO.read(zip.getInputStream(entry));
                    }
                } finally {
                    zip.close();
                }
            }
        } catch (Exception e) {
            // fall through: no logo
        }
        return null;
    }

    /** The logo band, as the real one draws it: the picture centred, 32 pixels down, cut off by the band. */
    private static void paintLogo(Graphics g, BufferedImage logo, int width) {
        if (logo != null) {
            g.drawImage(logo, width / 2 - logo.getWidth() / 2, 32, null);
        } else {
            g.setColor(new Color(0xC9CBD8));
            g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 40));
            g.drawString("MINECRAFT", width / 2 - 110, 70);
        }
    }

    /** Draws the report to an image the way the window looks, for checking without Word0778 a window (854 x 480 is Alpha's own size). */
    static BufferedImage render(String version, int width, int height) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        java.awt.Graphics2D g = image.createGraphics();
        g.setColor(SLATE);
        g.fillRect(0, 0, width, height);
        java.awt.Shape clip = g.getClip();
        g.setClip(0, 0, width, LOGO_BAND);
        paintLogo(g, logo(version), width);
        g.setClip(clip);
        int px = SIDE;
        int py = LOGO_BAND;
        int pw = width - 2 * SIDE;
        int ph = height - LOGO_BAND - BOTTOM;
        g.setColor(Color.WHITE);
        g.fillRect(px, py, pw, ph);
        g.setClip(px, py, pw - 17, ph);
        g.setColor(Color.BLACK);
        g.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        int y = py + 15;
        for (String line : text(version).split("\n", -1)) {
            g.drawString(line.replace("\t", "        "), px + 4, y);
            y += 15;
        }
        g.setClip(clip);
        g.setColor(new Color(0xF0F0F0));
        g.fillRect(px + pw - 17, py, 17, ph);
        g.setColor(new Color(0xCDCDCD));
        g.fillRect(px + pw - 16, py + 18, 15, ph / 4);
        g.dispose();
        return image;
    }

    /** The real crash panel's logo component: a Canvas with a 100 pixel band. */
    private static final class LogoCanvas extends Canvas {
        private final BufferedImage logo;

        LogoCanvas(BufferedImage logo) {
            this.logo = logo;
            setPreferredSize(new Dimension(100, LOGO_BAND));
            setMinimumSize(new Dimension(100, LOGO_BAND));
        }

        public void paint(Graphics g) {
            super.paint(g);
            paintLogo(g, logo, getWidth());
        }
    }

    private static Canvas spacer(int size) {
        Canvas c = new Canvas();
        c.setPreferredSize(new Dimension(size, size));
        c.setMinimumSize(new Dimension(size, size));
        return c;
    }

    // ------------------------------------------------------------------ the scare

    static final String SCARE_TEXT = "placeholder text";
    /** -Word0940.calm=on: no white flashes, no shaking, the sound at a third of the volume. The scare still happens. */
    static final boolean CALM = "on".equals(System.getProperty("orrin.calm"));
    /** Ticks (30 per second) the scare lasts: as long as the sound (set when the sound is loaded), 150 if it can't be. */
    static int scareTicks = 150;

    /** One frame of the scare. Used by the window and by the picture check. */
    static void paintScare(java.awt.Graphics2D g, int w, int h, int tick) {
        java.util.Random r = new java.util.Random(tick * 7919L + 17L);
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, w, h);
        float progress = Math.min(1f, tick / (float) scareTicks);
        // The letters, drawn off screen so slices of them can be torn sideways.
        BufferedImage letters = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        java.awt.Graphics2D lg = letters.createGraphics();
        int size = Math.max(24, w / 9 + (int) (progress * w / 14));
        lg.setFont(new Font(Font.SANS_SERIF, Font.BOLD, size));
        int tw = lg.getFontMetrics().stringWidth(SCARE_TEXT);
        while (tw > w * 0.94 && size > 12) { // always fits the window, however big it grows
            size -= 2;
            lg.setFont(new Font(Font.SANS_SERIF, Font.BOLD, size));
            tw = lg.getFontMetrics().stringWidth(SCARE_TEXT);
        }
        int tx = (w - tw) / 2;
        int ty = h / 2 + size / 3;
        lg.setColor(new Color(0x5A0000));
        lg.drawString(SCARE_TEXT, tx + 5, ty + 5);
        lg.setColor(new Color(0xC40000));
        lg.drawString(SCARE_TEXT, tx, ty);
        // Drips from the bottoms of the letters.
        lg.setColor(new Color(0x8A0000));
        for (int i = 0; i < 40; i++) {
            int x = tx + r.nextInt(Math.max(1, tw));
            int len = 6 + r.nextInt(size / 2 + 1) + (int) (progress * size / 2);
            lg.fillRect(x, ty + 4, 2 + r.nextInt(2), len);
        }
        lg.dispose();
        // Tear: rows of the lettering slide sideways by a different amount every frame.
        int rowH = 4;
        int shake = CALM ? 0 : (int) (3 + 14 * progress);
        for (int y = 0; y < h; y += rowH) {
            int dx = r.nextInt(7) == 0 ? (r.nextInt(2 * 40 + 1) - 40) : 0;
            dx += CALM ? 0 : r.nextInt(2 * shake + 1) - shake;
            g.drawImage(letters, dx, y, dx + w, y + rowH, 0, y, w, y + rowH, null);
        }
        // A pale ghost of the words, offset, and static over everything.
        g.setComposite(java.awt.AlphaComposite.getInstance(java.awt.AlphaComposite.SRC_OVER, 0.18f));
        g.drawImage(letters, -w / 40, 0, null);
        g.setComposite(java.awt.AlphaComposite.SrcOver);
        for (int i = 0; i < (CALM ? 120 : 420); i++) {
            int shade = 70 + r.nextInt(150);
            g.setColor(new Color(shade, shade, shade, 150));
            g.fillRect(r.nextInt(w), r.nextInt(h), 1 + r.nextInt(8), 1 + r.nextInt(2));
        }
        for (int i = 0; i < 4; i++) {
            g.setColor(r.nextBoolean() ? new Color(120, 0, 0, 120) : new Color(255, 255, 255, 40));
            g.fillRect(0, r.nextInt(h), w, 2 + r.nextInt(5));
        }
        // Two quick flashes in the whole thing, none when calm.
        if (!CALM && (tick == 0 || tick == 1 || tick == 38 || tick == 39)) {
            g.setColor(tick < 2 ? Color.WHITE : new Color(200, 0, 0));
            g.fillRect(0, 0, w, h);
        }
        // Fade out at the end.
        if (tick > scareTicks - 25) {
            g.setColor(new Color(0, 0, 0, Math.min(255, (tick - (scareTicks - 25)) * 11)));
            g.fillRect(0, 0, w, h);
        }
    }

    /** A picture of one frame of the scare, for checking what it looks like. */
    static BufferedImage renderScare(int w, int h, int tick) {
        BufferedImage image = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        java.awt.Graphics2D g = image.createGraphics();
        paintScare(g, w, h, tick);
        g.dispose();
        return image;
    }

    /**
     * The scream: "Horror scream" by jusatti890 on Pixabay (see CREDITS.md), already slowed, filtered and echoed so it sounds like it
     * is underwater and cut to its first two screams (see DEV_NOTES.md for the ffmpeg line). It ships inside the loader jar as a 44.1 kHz mono
     * 16-bit WAV. Returns null if it is not there, and the made-up sound below is used instead.
     */
    private static byte[] recordedScream(float volume) {
        try {
            InputStream in = FakeCrash.class.getResourceAsStream("/orrin/scream_underwater.wav");
            if (in == null) {
                return null;
            }
            javax.sound.sampled.AudioInputStream audio = javax.sound.sampled.AudioSystem.getAudioInputStream(new java.io.BufferedInputStream(in));
            java.io.ByteArrayOutputStream bytes = new java.io.ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int read;
            while ((read = audio.read(buffer)) > 0) {
                bytes.write(buffer, 0, read);
            }
            byte[] pcm = bytes.toByteArray();
            for (int i = 0; i + 1 < pcm.length; i += 2) {
                int s = (short) ((pcm[i] & 0xFF) | (pcm[i + 1] << 8));
                s = (int) (s * volume);
                pcm[i] = (byte) (s & 0xFF);
                pcm[i + 1] = (byte) ((s >> 8) & 0xFF);
            }
            scareTicks = Math.max(150, Math.min(600, (int) (pcm.length / 2 / 44100.0 * 30) + 15));
            return pcm;
        } catch (Exception e) {
            return null;
        }
    }

    /** One resonant band-pass filter (a "formant"): it is what makes a buzzing wave sound like a mouth. */
    private static final class Formant {
        private final double b0, b1, b2, a1, a2;
        private double x1, x2, y1, y2;

        Formant(double freq, double q, double rate) {
            double w = 2 * Math.PI * freq / rate;
            double alpha = Math.sin(w) / (2 * q);
            double a0 = 1 + alpha;
            b0 = alpha / a0;
            b1 = 0;
            b2 = -alpha / a0;
            a1 = -2 * Math.cos(w) / a0;
            a2 = (1 - alpha) / a0;
        }

        double process(double x) {
            double y = b0 * x + b1 * x1 + b2 * x2 - a1 * y1 - a2 * y2;
            x2 = x1;
            x1 = x;
            y2 = y1;
            y1 = y;
            return y;
        }
    }

    /**
     * The sound, about 5 seconds, mono, 44.1 kHz:
     * a sudden sub-bass impact with a burst of noise at the very start; a distorted human scream (a buzzing wave through three vowel
     * filters, its pitch tearing downwards with a shake in it, Word0900 noise on top) that opens at once; a cluster of dissonant
     * low tones swelling underneath; a second, smaller impact at 2 seconds and a last one at 3.4.
     */
    static byte[] scareSound(float volume) {
        byte[] recorded = recordedScream(volume);
        if (recorded != null) {
            return recorded;
        }
        final double rate = 44100.0;
        int total = (int) (5.0 * rate);
        double[] mix = new double[total];
        java.util.Random r = new java.util.Random(1962);

        // Impacts: a falling sine (the thump) plus noise that dies quickly.
        double[][] hits = {{0.0, 1.0}, {2.0, 0.6}, {3.4, 0.5}};
        for (double[] hit : hits) {
            for (int i = (int) (hit[0] * rate); i < total; i++) {
                double t = i / rate - hit[0];
                double env = Math.exp(-t * 5.0);
                if (env < 0.002) {
                    break;
                }
                double thump = Math.sin(2 * Math.PI * (70 * Math.exp(-t * 6) + 28) * t);
                double noise = (r.nextDouble() * 2 - 1) * Math.exp(-t * 18);
                mix[i] += hit[1] * (1.1 * env * thump + 0.9 * noise);
            }
        }

        // The scream: a buzzing wave (many harmonics) through "ah" filters, pitch falling and shaking.
        Formant f1 = new Formant(780, 6, rate);
        Formant f2 = new Formant(1250, 8, rate);
        Formant f3 = new Formant(2600, 10, rate);
        Formant breath = new Formant(3200, 1.5, rate);
        double phase = 0;
        for (int i = 0; i < total; i++) {
            double t = i / rate;
            if (t > 4.4) {
                break;
            }
            double glide = 900 - 520 * Math.min(1.0, t / 3.2);
            double shake = 1 + 0.04 * Math.sin(2 * Math.PI * 7.5 * t) + 0.02 * (r.nextDouble() - 0.5);
            phase += 2 * Math.PI * glide * shake / rate;
            double src = 0;
            for (int h = 1; h <= 18; h++) {
                src += Math.sin(phase * h) / h;
            }
            double voiced = f1.process(src) * 1.0 + f2.process(src) * 0.9 + f3.process(src) * 0.5;
            double air = breath.process(r.nextDouble() * 2 - 1) * 0.5;
            double env = Math.min(1.0, t / 0.04) * (t < 3.6 ? 1.0 : Math.max(0, (4.4 - t) / 0.8));
            mix[i] += 1.6 * env * Math.tanh(2.5 * (voiced + air));
        }

        // Dissonant low cluster, swelling: a minor second and a tritone, with a slow wobble.
        double[] cluster = {110.0, 116.5, 155.6, 164.8};
        for (int i = 0; i < total; i++) {
            double t = i / rate;
            double swell = Math.min(1.0, t / 2.5) * Math.min(1.0, (5.0 - t) / 0.6);
            double s = 0;
            for (double f : cluster) {
                s += Math.signum(Math.sin(2 * Math.PI * f * t + 0.3 * Math.sin(t * 5)));
            }
            mix[i] += 0.11 * swell * s;
        }

        // Normalise so the peak is the same however the parts add up, then apply the volume.
        double peak = 0.0001;
        for (double v : mix) {
            peak = Math.max(peak, Math.abs(v));
        }
        byte[] out = new byte[total * 2];
        for (int i = 0; i < total; i++) {
            double v = Math.tanh(1.4 * mix[i] / peak) / Math.tanh(1.4) * 0.95 * volume;
            int s = (int) (v * 32767);
            out[2 * i] = (byte) (s & 0xFF);
            out[2 * i + 1] = (byte) ((s >> 8) & 0xFF);
        }
        return out;
    }

    /** For testing the sound on its own: plays it and waits until it is over. */
    static void playScareSoundAndWait() throws InterruptedException {
        playScareSound();
        Thread.sleep(16000);
    }

    private static void playScareSound() {
        Thread thread = new Thread(new Runnable() {
            public void run() {
                try {
                    byte[] pcm = scareSound(CALM ? 0.3f : 1.0f);
                    javax.sound.sampled.AudioFormat format = new javax.sound.sampled.AudioFormat(44100f, 16, 1, true, false);
                    javax.sound.sampled.SourceDataLine line = javax.sound.sampled.AudioSystem.getSourceDataLine(format);
                    line.open(format);
                    line.start();
                    line.write(pcm, 0, pcm.length);
                    line.drain();
                    line.close();
                } catch (Throwable t) {
                    // no sound device: the picture still happens
                }
            }
        }, "orrin-scare-sound");
        thread.setDaemon(true);
        thread.start();
    }

    /** The scare itself: the crash window is replaced by the lettering for about five seconds, then the window goes away. The game is untouched. */
    private static void scare(final Frame frame) {
        final Canvas canvas = new Canvas() {
            public void paint(Graphics g) {
                // painting is driven by the timer below
            }

            public void update(Graphics g) {
            }
        };
        frame.removeAll();
        frame.setLayout(new BorderLayout());
        frame.add(canvas, BorderLayout.CENTER);
        frame.validate();
        scareSound(0f); // loads the recording, which sets how long the scare lasts
        playScareSound();
        final int[] tick = {0};
        final java.util.Timer timer = new java.util.Timer("orrin-scare", true);
        timer.schedule(new java.util.TimerTask() {
            public void run() {
                if (tick[0] >= scareTicks || !frame.isDisplayable()) {
                    timer.cancel();
                    frame.dispose();
                    return;
                }
                try {
                    int w = Math.max(1, canvas.getWidth());
                    int h = Math.max(1, canvas.getHeight());
                    BufferedImage image = renderScare(w, h, tick[0]++);
                    Graphics g = canvas.getGraphics();
                    if (g != null) {
                        g.drawImage(image, 0, 0, null);
                        g.dispose();
                    }
                } catch (Throwable t) {
                    timer.cancel();
                    frame.dispose();
                }
            }
        }, 0, 33);
    }

    /** Shows the report in a window that closes itself. Call from any thread. */
    static void show(final String version) {
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                try {
                    final Frame frame = new Frame("Minecraft");
                    frame.setBackground(SLATE);
                    frame.setLayout(new BorderLayout());
                    TextArea area = new TextArea(text(version), 0, 0, TextArea.SCROLLBARS_VERTICAL_ONLY);
                    area.setFont(new Font("Monospaced", Font.PLAIN, 12));
                    area.setEditable(false);
                    frame.add(new LogoCanvas(logo(version)), BorderLayout.NORTH);
                    frame.add(spacer(SIDE), BorderLayout.EAST);
                    frame.add(spacer(SIDE), BorderLayout.WEST);
                    frame.add(spacer(BOTTOM), BorderLayout.SOUTH);
                    frame.add(area, BorderLayout.CENTER);
                    KeyAdapter escape = new KeyAdapter() {
                        public void keyPressed(KeyEvent e) {
                            if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                                frame.dispose();
                            }
                        }
                    };
                    area.addKeyListener(escape);
                    frame.addKeyListener(escape);
                    frame.addWindowListener(new WindowAdapter() {
                        public void windowClosing(WindowEvent e) {
                            frame.dispose();
                        }

                        public void windowClosed(WindowEvent e) {
                            if (standalone) {
                                System.exit(0);
                            }
                        }
                    });
                    frame.setSize(854, 480 + 28);
                    frame.setLocationRelativeTo(null);
                    frame.setAlwaysOnTop(true);
                    frame.setVisible(true);
                    if (standalone) {
                        System.out.println("WINDOW"); // tells the game it is time to save and close
                        System.out.flush();
                    }
                    final TextArea shown = area;
                    final java.util.Timer timer = new java.util.Timer("orrin-fakecrash-close", true);
                    // A while later the report starts to add to itself, one line at a time...
                    final String[] more = {"placeholder text", "placeholder text", "placeholder text",
                            "placeholder text", "placeholder text", "placeholder text"};
                    for (int i = 0; i < more.length; i++) {
                        final String line = more[i];
                        timer.schedule(new java.util.TimerTask() {
                            public void run() {
                                java.awt.EventQueue.invokeLater(new Runnable() {
                                    public void run() {
                                        if (frame.isDisplayable()) {
                                            shown.append(line);
                                            shown.setCaretPosition(shown.getText().length());
                                        }
                                    }
                                });
                            }
                        }, (long) (8000 + i * 1700 + (i == 5 ? 700 : 0)));
                    }
                    // ...and then the scare. The window still closes with Escape or its X at any moment.
                    timer.schedule(new java.util.TimerTask() {
                        public void run() {
                            java.awt.EventQueue.invokeLater(new Runnable() {
                                public void run() {
                                    if (frame.isDisplayable()) {
                                        if (standalone) {
                                            System.out.println("SCARE");
                                            System.out.flush();
                                        }
                                        scare(frame);
                                    }
                                }
                            });
                        }
                    }, 20500L);
                    // Whatever happens, the window is gone after this long (the game is never closed by any of this).
                    timer.schedule(new java.util.TimerTask() {
                        public void run() {
                            frame.dispose();
                        }
                    }, SECONDS_ON_SCREEN * 1000L);
                } catch (Throwable t) {
                    // No display, no window. Never let this get in the way of the game.
                }
            }
        });
    }

    /**
     * After the given number of seconds, shows the crash window in a process of its own. As soon as that window is up, this process
     * (the game) saves the world and closes, so the game's window goes away and only the crash window is left. If that process can't be
     * started, the window is shown here instead and the game is left alone. -Word0940.fakecrash.close=off keeps the game open.
     */
    static void schedule(final String version, final long seconds) {
        Thread thread = new Thread(new Runnable() {
            public void run() {
                try {
                    Thread.sleep(seconds * 1000L);
                } catch (InterruptedException e) {
                    return;
                }
                try {
                    File self = new File(FakeCrash.class.getProtectionDomain().getCodeSource().getLocation().toURI());
                    java.util.List<String> command = new ArrayList<String>();
                    command.add(new File(System.getProperty("java.home"), "bin/java").getPath());
                    String gameJar = GameControl.gameJarPath();
                    if (gameJar != null) {
                        command.add("-Word0940.gamejar=" + gameJar);
                    }
                    command.add("-Word0940.calm=" + (CALM ? "on" : "off"));
                    command.add("-cp");
                    command.add(self.getAbsolutePath());
                    command.add("com.realarqade.orrin.FakeCrash");
                    command.add(version);
                    Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
                    java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(process.getInputStream()));
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (line.equals("WINDOW") && !"off".equals(System.getProperty("orrin.fakecrash.close"))) {
                            GameControl.saveAndExit();
                        }
                    }
                } catch (Throwable t) {
                    show(version); // no separate process: show it here, and leave the game alone
                }
            }
        }, "orrin-fakecrash");
        thread.setDaemon(true);
        thread.start();
    }
}