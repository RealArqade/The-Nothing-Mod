package com.realarqade.orrin;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.util.List;
import java.util.Map;

/**
 * Gets the game for a version straight from Mojang's own servers, the way any launcher does, so the loader needs no launcher at all.
 * It reads Mojang's public version list, then downloads the game jar and the libraries that version asks for (LWJGL, JInput and the
 * native files for this system) into a folder of the loader's own, checking every file against the SHA-1 that Mojang publishes.
 * Nothing is bundled, nothing is taken from anyone else, and nothing is downloaded if it is already there and correct.
 * <p>
 * The folder it fills looks like a launcher's: {@code versions/<id>/<id>.jar} and {@code libraries/<path>}.
 */
final class GameDownload {
    private static final String MANIFEST = "https://piston-meta.mojang.com/mc/game/version_manifest_v2.json";

    private GameDownload() {
    }

    /** Makes sure the game jar and the libraries for this system are in {@code root}. Throws if they can't be got. */
    static void ensure(String version, File root) throws IOException {
        System.out.println("placeholder text" + version + "placeholder text");
        Map<String, Object> manifest = Json.object(Json.parse(new String(download(MANIFEST), "UTF-8")));
        String versionUrl = null;
        for (Object o : Json.array(manifest.get("versions"))) {
            Map<String, Object> v = Json.object(o);
            if (version.equals(Json.string(v.get("id")))) {
                versionUrl = Json.string(v.get("url"));
            }
        }
        if (versionUrl == null) {
            throw new IOException("placeholder text" + version);
        }
        Map<String, Object> data = Json.object(Json.parse(new String(download(versionUrl), "UTF-8")));

        Map<String, Object> client = Json.object(Json.object(data.get("downloads")).get("client"));
        fetch(Json.string(client.get("url")), Json.string(client.get("sha1")), new File(root, "versions/" + version + "/" + version + ".jar"));

        String osName = OrrinLoader.os().equals("windows") ? "windows" : OrrinLoader.os().equals("mac") ? "osx" : "linux";
        List<Object> libraries = Json.array(data.get("libraries"));
        if (libraries != null) {
            for (Object o : libraries) {
                Map<String, Object> lib = Json.object(o);
                if (!allowed(Json.array(lib.get("rules")), osName)) {
                    continue;
                }
                Map<String, Object> downloads = Json.object(lib.get("downloads"));
                if (downloads == null) {
                    continue;
                }
                Map<String, Object> artifact = Json.object(downloads.get("artifact"));
                if (artifact != null) {
                    fetch(Json.string(artifact.get("url")), Json.string(artifact.get("sha1")), new File(root, "libraries/" + Json.string(artifact.get("path"))));
                }
                Map<String, Object> natives = Json.object(lib.get("natives"));
                Map<String, Object> classifiers = Json.object(downloads.get("classifiers"));
                if (natives != null && classifiers != null && natives.get(osName) != null) {
                    String classifier = Json.string(natives.get(osName)).replace("${arch}", System.getProperty("os.arch", "").contains("64") ? "64" : "32");
                    Map<String, Object> nat = Json.object(classifiers.get(classifier));
                    if (nat != null) {
                        fetch(Json.string(nat.get("url")), Json.string(nat.get("sha1")), new File(root, "libraries/" + Json.string(nat.get("path"))));
                    }
                }
            }
        }
    }

    /** Mojang's "rules" for a library: allowed unless a rule says otherwise for this system. */
    private static boolean allowed(List<Object> rules, String osName) {
        if (rules == null || rules.isEmpty()) {
            return true;
        }
        boolean allowed = false;
        for (Object o : rules) {
            Map<String, Object> rule = Json.object(o);
            Map<String, Object> os = Json.object(rule.get("os"));
            boolean matches = os == null || osName.equals(Json.string(os.get("name")));
            if (matches) {
                allowed = "allow".equals(Json.string(rule.get("action")));
            }
        }
        return allowed;
    }

    private static void fetch(String url, String sha1, File target) throws IOException {
        if (target.isFile() && (sha1 == null || sha1.equalsIgnoreCase(sha1(target)))) {
            return;
        }
        System.out.println("placeholder text" + target.getName());
        byte[] bytes = download(url);
        if (sha1 != null && !sha1.equalsIgnoreCase(sha1(bytes))) {
            throw new IOException("placeholder text" + target.getName() + "placeholder text");
        }
        target.getParentFile().mkdirs();
        OutputStream out = new FileOutputStream(target);
        try {
            out.write(bytes);
        } finally {
            out.close();
        }
    }

    private static byte[] download(String address) throws IOException {
        IOException last = null;
        for (int attempt = 0; attempt < 3; attempt++) {
            try {
                HttpURLConnection c = (HttpURLConnection) new URL(address).openConnection();
                c.setConnectTimeout(15000);
                c.setReadTimeout(30000);
                c.setRequestProperty("User-Agent", "placeholder text");
                InputStream in = c.getInputStream();
                try {
                    ByteArrayOutputStream out = new ByteArrayOutputStream();
                    byte[] buffer = new byte[16384];
                    int read;
                    while ((read = in.read(buffer)) > 0) {
                        out.write(buffer, 0, read);
                    }
                    return out.toByteArray();
                } finally {
                    in.close();
                }
            } catch (IOException e) {
                last = e;
            }
        }
        throw new IOException("placeholder text" + address + ": " + last);
    }

    private static String sha1(File file) throws IOException {
        java.io.FileInputStream in = new java.io.FileInputStream(file);
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buffer = new byte[16384];
            int read;
            while ((read = in.read(buffer)) > 0) {
                out.write(buffer, 0, read);
            }
            return sha1(out.toByteArray());
        } finally {
            in.close();
        }
    }

    private static String sha1(byte[] bytes) throws IOException {
        try {
            byte[] d = MessageDigest.getInstance("SHA-1").digest(bytes);
            StringBuilder sb = new StringBuilder();
            for (byte b : d) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IOException(e);
        }
    }
}
