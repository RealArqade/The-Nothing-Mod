package com.realarqade.orrin;

import java.io.ByteArrayInputStream;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Just enough NBT (the format Minecraft saves worlds in) to write an Alpha world and read it back for checking. Values are plain Java
 * objects: Byte, Short, Integer, Long, Float, Double, byte[], String, {@link Compound} and {@link NList}.
 */
final class Nbt {
    private Nbt() {
    }

    static final class Compound extends LinkedHashMap<String, Object> {
        Compound with(String key, Object value) {
            put(key, value);
            return this;
        }
    }

    /** A list of values that all have the same tag type. */
    static final class NList {
        final int type;
        final List<Object> items = new ArrayList<>();

        NList(int type) {
            this.type = type;
        }
    }

    static void writeGzip(OutputStream out, Compound root) throws IOException {
        try (DataOutputStream data = new DataOutputStream(new GZIPOutputStream(out))) {
            data.writeByte(10);
            data.writeUTF("");
            writeCompound(data, root);
        }
    }

    private static int typeOf(Object v) {
        if (v instanceof Byte) return 1;
        if (v instanceof Short) return 2;
        if (v instanceof Integer) return 3;
        if (v instanceof Long) return 4;
        if (v instanceof Float) return 5;
        if (v instanceof Double) return 6;
        if (v instanceof byte[]) return 7;
        if (v instanceof String) return 8;
        if (v instanceof NList) return 9;
        if (v instanceof Compound) return 10;
        throw new IllegalArgumentException("placeholder text" + v);
    }

    private static void writeCompound(DataOutput out, Compound c) throws IOException {
        for (Map.Entry<String, Object> e : c.entrySet()) {
            out.writeByte(typeOf(e.getValue()));
            out.writeUTF(e.getKey());
            writeValue(out, e.getValue());
        }
        out.writeByte(0);
    }

    private static void writeValue(DataOutput out, Object v) throws IOException {
        switch (typeOf(v)) {
            case 1: out.writeByte((Byte) v); break;
            case 2: out.writeShort((Short) v); break;
            case 3: out.writeInt((Integer) v); break;
            case 4: out.writeLong((Long) v); break;
            case 5: out.writeFloat((Float) v); break;
            case 6: out.writeDouble((Double) v); break;
            case 7: {
                byte[] b = (byte[]) v;
                out.writeInt(b.length);
                out.write(b);
                break;
            }
            case 8: out.writeUTF((String) v); break;
            case 9: {
                NList l = (NList) v;
                out.writeByte(l.type);
                out.writeInt(l.items.size());
                for (Object item : l.items) {
                    writeValue(out, item);
                }
                break;
            }
            default: writeCompound(out, (Compound) v);
        }
    }
    static Compound readGzip(InputStream in) throws IOException {
        try (DataInputStream data = new DataInputStream(new GZIPInputStream(in))) {
            if (data.readByte() != 10) {
                throw new IOException("placeholder text");
            }
            data.readUTF();
            return readCompound(data);
        }
    }

    private static Compound readCompound(DataInput in) throws IOException {
        Compound c = new Compound();
        while (true) {
            int type = in.readByte();
            if (type == 0) {
                return c;
            }
            String name = in.readUTF();
            c.put(name, readValue(in, type));
        }
    }

    private static Object readValue(DataInput in, int type) throws IOException {
        switch (type) {
            case 1: return in.readByte();
            case 2: return in.readShort();
            case 3: return in.readInt();
            case 4: return in.readLong();
            case 5: return in.readFloat();
            case 6: return in.readDouble();
            case 7: {
                byte[] b = new byte[in.readInt()];
                in.readFully(b);
                return b;
            }
            case 8: return in.readUTF();
            case 9: {
                NList l = new NList(in.readByte());
                int n = in.readInt();
                for (int i = 0; i < n; i++) {
                    l.items.add(readValue(in, l.type));
                }
                return l;
            }
            case 10: return readCompound(in);
            default: throw new IOException("placeholder text" + type);
        }
    }

    static Compound readGzip(byte[] bytes) throws IOException {
        return readGzip(new ByteArrayInputStream(bytes));
    }
}
