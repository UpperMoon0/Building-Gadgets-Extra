package com.nstut.buildinggadgetsextra.common;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/** Stops encoding before an oversized template can grow an unbounded staging buffer. */
public final class BoundedByteArrayOutputStream extends OutputStream {
    private final int limit;
    private final ByteArrayOutputStream output = new ByteArrayOutputStream();

    public BoundedByteArrayOutputStream(int limit) {
        if (limit < 0) throw new IllegalArgumentException("Negative byte limit");
        this.limit = limit;
    }

    @Override public void write(int value) throws IOException {
        reserve(1);
        output.write(value);
    }

    @Override public void write(byte[] bytes, int offset, int length) throws IOException {
        if (offset < 0 || length < 0 || offset > bytes.length - length) throw new IndexOutOfBoundsException();
        reserve(length);
        output.write(bytes, offset, length);
    }

    private void reserve(int count) throws IOException {
        if (count > limit - output.size()) throw new IOException("Template exceeds encoded byte budget");
    }

    public byte[] toByteArray() { return output.toByteArray(); }
}
