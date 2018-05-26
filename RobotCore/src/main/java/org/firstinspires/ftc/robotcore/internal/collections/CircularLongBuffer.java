/*
Copyright (c) 2017 Robert Atkinson

All rights reserved.

Redistribution and use in source and binary forms, with or without modification,
are permitted (subject to the limitations in the disclaimer below) provided that
the following conditions are met:

Redistributions of source code must retain the above copyright notice, this list
of conditions and the following disclaimer.

Redistributions in binary form must reproduce the above copyright notice, this
list of conditions and the following disclaimer in the documentation and/or
other materials provided with the distribution.

Neither the name of Robert Atkinson nor the names of his contributors may be used to
endorse or promote products derived from this software without specific prior
written permission.

NO EXPRESS OR IMPLIED LICENSES TO ANY PARTY'S PATENT RIGHTS ARE GRANTED BY THIS
LICENSE. THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
"AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESSFOR A PARTICULAR PURPOSE
ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE
FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/
package org.firstinspires.ftc.robotcore.internal.collections;

import java.nio.LongBuffer;

/**
 * {@link CircularByteBuffer} is an auto-growable (with optional finite capacity) circular buffer of longs.
 * <p>
 * Not thread safe.
 */
@SuppressWarnings("WeakerAccess")
public class CircularLongBuffer {
    //----------------------------------------------------------------------------------------------
    // State
    //----------------------------------------------------------------------------------------------

    protected long[] buffer;
    protected int readIndex;
    protected int size;
    protected final int capacity;

    //----------------------------------------------------------------------------------------------
    // State
    //----------------------------------------------------------------------------------------------

    public CircularLongBuffer(int initialAllocation) {
        this(initialAllocation, Integer.MAX_VALUE);
    }

    public CircularLongBuffer(int initialAllocation, int capacity) {
        this.buffer = new long[initialAllocation];
        this.capacity = capacity;
        clear();
    }

    //----------------------------------------------------------------------------------------------
    // Accessing
    //----------------------------------------------------------------------------------------------

    public int size() {
        return size;
    }

    public boolean isEmpty() {
        return size == 0;
    }

    public int capacity() {
        return capacity;
    }

    public int remainingCapacity() {
        return capacity - size;
    }

    protected int allocated() {
        return buffer.length;
    }

    public long get(int index) {
        if (index >= size || index < 0) {
            throw new IndexOutOfBoundsException("get(" + index + ")");
        }
        return buffer[mod(readIndex + index)];
    }

    public long getFirst() {
        return get(0);
    }

    public long getLast() {
        return get(size - 1);
    }

    public void put(int index, long value) {
        if (index >= size || index < 0) {
            throw new IndexOutOfBoundsException("put(" + index + ")");
        }
        buffer[mod(readIndex + index)] = value;
    }

    //----------------------------------------------------------------------------------------------
    // Reading
    //----------------------------------------------------------------------------------------------

    public long removeFirst() {
        if (isEmpty()) {
            throw new IndexOutOfBoundsException("removeFirst");
        }

        long result = buffer[readIndex];
        readIndex = mod(readIndex + 1);
        size -= 1;
        return result;
    }

    public int read(long[] output) {
        return read(output, 0, output.length);
    }

    public int read(long[] output, int index, int countToRead) {
        if (countToRead < 0) {
            throw new IllegalArgumentException("count must be non-negative: " + countToRead);
        }

        // Read the first stretch from what's remaining in the trailing part of the buffer
        int countFirst = min(countToRead, size, allocated() - readIndex);
        readTo(output, index, countFirst);

        // Read the second after the wrap
        int countSecond = min(countToRead - countFirst, size);
        readTo(output, index + countFirst, countSecond);

        return countFirst + countSecond;
    }

    protected void readTo(long[] output, int index, int count) {
        if (count > 0) {
            System.arraycopy(buffer, readIndex, output, index, count);
            readIndex = mod(readIndex + count);
            size -= count;
        }
    }

    public int skip(int countToSkip) {
        if (countToSkip < 0) {
            throw new IllegalArgumentException("count must be non-negative: " + countToSkip);
        }

        // Read the first stretch from what's remaining in the trailing part of the buffer
        int countFirst = min(countToSkip, size, allocated() - readIndex);
        skipBy(countFirst);

        // Read the second after the wrap
        int countSecond = min(countToSkip - countFirst, size);
        skipBy(countSecond);

        return countFirst + countSecond;
    }

    protected void skipBy(int count) {
        if (count > 0) {
            readIndex = mod(readIndex + count);
            size -= count;
        }
    }

    //----------------------------------------------------------------------------------------------
    // Writing
    //----------------------------------------------------------------------------------------------

    public void addLast(long value) {
        int sizeNeeded = size + 1;
        if (sizeNeeded > capacity) {
            throw new IllegalStateException("full");
        }
        if (sizeNeeded > allocated()) {
            grow(sizeNeeded);
        }
        int indexWrite = mod(readIndex + size);
        buffer[indexWrite] = value;
        size += 1;
    }

    public int write(LongBuffer fromBuffer) {
        long[] array = fromBuffer.array();
        int index = fromBuffer.arrayOffset() + fromBuffer.position();
        int count = fromBuffer.remaining();
        return write(array, index, count);
    }

    public int write(long[] buf) {
        return write(buf, 0, buf.length);
    }

    public int write(long[] input, int indexFrom, int countToWrite) {
        if (countToWrite < 0) {
            throw new IllegalArgumentException("count must be non-negative: " + countToWrite);
        }

        int sizeNeeded = size + countToWrite;
        if (sizeNeeded > capacity) {
            sizeNeeded = capacity;
            countToWrite = Math.max(0, sizeNeeded - size);
        }
        if (sizeNeeded > allocated()) {
            grow(sizeNeeded);
        }

        // Small perf optimization; useful in the case where we commonly drain the buffer fully
        if (size == 0) {
            readIndex = 0;
        }

        // Find first free byte
        int indexWrite = readIndex + size;
        if (indexWrite < allocated()) {
            // Unused space is in two fragments
            int countFirst = min(countToWrite, allocated() - indexWrite);
            writeFrom(input, indexFrom, countFirst);
            int countSecond = min(countToWrite - countFirst, readIndex);
            writeFrom(input, indexFrom + countFirst, countSecond);
            return countFirst + countSecond;
        } else {
            // Unused space is in one fragment
            int count = min(countToWrite, allocated() - size);
            writeFrom(input, indexFrom, count);
            return count;
        }
    }

    protected void writeFrom(long[] input, int indexFrom, int count) {
        if (count > 0) {
            int indexTo = mod(readIndex + size);
            System.arraycopy(input, indexFrom, buffer, indexTo, count);
            size += count;
        }
    }

    protected void grow(int newSize) {
        // Read all the data to a new buffer.
        long[] bufferNew = new long[newSize];
        int countRead = read(bufferNew, 0, size);

        buffer = bufferNew;
        readIndex = 0;
        size = countRead;
    }

    //----------------------------------------------------------------------------------------------
    // Miscellaneous
    //----------------------------------------------------------------------------------------------

    /**
     * Discards all the bytes currently readable in the buffer
     */
    public void clear() {
        readIndex = 0;
        size = 0;
    }

    //----------------------------------------------------------------------------------------------
    // Utility
    //----------------------------------------------------------------------------------------------

    protected int mod(int p) {
        return (p < allocated()) ? p : (p - allocated());
    }

    protected int min(int a, int b) {
        return Math.min(a, b);
    }

    protected int min(int a, int b, int c) {
        return Math.min(a, Math.min(b, c));
    }
}
