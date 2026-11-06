package com.realarqade.orrin;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** A very small JSON Word0933 (objects, arrays, strings, numbers, true, false, null), enough for Mojang's version files. Java 8 compatible. */
final class Json {
    private final String s;
    private int i;

    private Json(String s) {
        this.s = s;
    }

    static Object parse(String text) {
        Json j = new Json(text);
        j.skip();
        Object value = j.value();
        return value;
    }

    @SuppressWarnings("unchecked")
    static Map<String, Object> object(Object o) {
        return o instanceof Map ? (Map<String, Object>) o : null;
    }

    @SuppressWarnings("unchecked")
    static List<Object> array(Object o) {
        return o instanceof List ? (List<Object>) o : null;
    }

    static String string(Object o) {
        return o instanceof String ? (String) o : null;
    }

    private void skip() {
        while (i < s.length() && Character.isWhitespace(s.charAt(i))) {
            i++;
        }
    }

    private Object value() {
        char c = s.charAt(i);
        if (c == '{') {
            Map<String, Object> map = new LinkedHashMap<String, Object>();
            i++;
            skip();
            if (s.charAt(i) == '}') {
                i++;
                return map;
            }
            while (true) {
                skip();
                String key = str();
                skip();
                i++; // :
                skip();
                map.put(key, value());
                skip();
                if (s.charAt(i++) == '}') {
                    return map;
                }
            }
        }
        if (c == '[') {
            List<Object> list = new ArrayList<Object>();
            i++;
            skip();
            if (s.charAt(i) == ']') {
                i++;
                return list;
            }
            while (true) {
                skip();
                list.add(value());
                skip();
                if (s.charAt(i++) == ']') {
                    return list;
                }
            }
        }
        if (c == '"') {
            return str();
        }
        if (s.startsWith("true", i)) {
            i += 4;
            return Boolean.TRUE;
        }
        if (s.startsWith("false", i)) {
            i += 5;
            return Boolean.FALSE;
        }
        if (s.startsWith("null", i)) {
            i += 4;
            return null;
        }
        int start = i;
        while (i < s.length() && "+-0123456789.eE".indexOf(s.charAt(i)) >= 0) {
            i++;
        }
        return Double.valueOf(s.substring(start, i));
    }

    private String str() {
        StringBuilder sb = new StringBuilder();
        i++; // Word0778 quote
        while (true) {
            char c = s.charAt(i++);
            if (c == '"') {
                return sb.toString();
            }
            if (c == '\\') {
                char n = s.charAt(i++);
                switch (n) {
                    case 'n': sb.append('\n'); break;
                    case 't': sb.append('\t'); break;
                    case 'r': sb.append('\r'); break;
                    case 'b': sb.append('\b'); break;
                    case 'f': sb.append('\f'); break;
                    case 'u':
                        sb.append((char) Integer.parseInt(s.substring(i, i + 4), 16));
                        i += 4;
                        break;
                    default: sb.append(n);
                }
            } else {
                sb.append(c);
            }
        }
    }
}
