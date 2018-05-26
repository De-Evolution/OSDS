/*
 * Copyright (C) 2007 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.firstinspires.ftc.robotcore.internal.android.dx.cf.cst;

import org.firstinspires.ftc.robotcore.internal.android.dx.cf.iface.ParseException;
import org.firstinspires.ftc.robotcore.internal.android.dx.cf.iface.ParseObserver;
import org.firstinspires.ftc.robotcore.internal.android.dx.rop.cst.Constant;
import org.firstinspires.ftc.robotcore.internal.android.dx.rop.cst.CstDouble;
import org.firstinspires.ftc.robotcore.internal.android.dx.rop.cst.CstFieldRef;
import org.firstinspires.ftc.robotcore.internal.android.dx.rop.cst.CstFloat;
import org.firstinspires.ftc.robotcore.internal.android.dx.rop.cst.CstInteger;
import org.firstinspires.ftc.robotcore.internal.android.dx.rop.cst.CstInterfaceMethodRef;
import org.firstinspires.ftc.robotcore.internal.android.dx.rop.cst.CstLong;
import org.firstinspires.ftc.robotcore.internal.android.dx.rop.cst.CstMethodRef;
import org.firstinspires.ftc.robotcore.internal.android.dx.rop.cst.CstNat;
import org.firstinspires.ftc.robotcore.internal.android.dx.rop.cst.CstString;
import org.firstinspires.ftc.robotcore.internal.android.dx.rop.cst.CstType;
import org.firstinspires.ftc.robotcore.internal.android.dx.rop.cst.StdConstantPool;
import org.firstinspires.ftc.robotcore.internal.android.dx.rop.type.Type;
import org.firstinspires.ftc.robotcore.internal.android.dx.util.ByteArray;
import org.firstinspires.ftc.robotcore.internal.android.dx.util.Hex;

import java.util.BitSet;

/**
 * Parser for a constant pool embedded in a class file.
 */
public final class ConstantPoolParser {
    /**
     * {@code non-null;} the bytes of the constant pool
     */
    private final ByteArray bytes;

    /**
     * {@code non-null;} actual parsed constant pool contents
     */
    private final StdConstantPool pool;

    /**
     * {@code non-null;} byte offsets to each cst
     */
    private final int[] offsets;

    /**
     * -1 || &gt;= 10; the end offset of this constant pool in the
     * {@code byte[]} which it came from or {@code -1} if not
     * yet parsed
     */
    private int endOffset;

    /**
     * {@code null-ok;} parse observer, if any
     */
    private ParseObserver observer;

    /**
     * Constructs an instance.
     *
     * @param bytes {@code non-null;} the bytes of the file
     */
    public ConstantPoolParser(ByteArray bytes) {
        int size = bytes.getUnsignedShort(8); // constant_pool_count

        this.bytes = bytes;
        this.pool = new StdConstantPool(size);
        this.offsets = new int[size];
        this.endOffset = -1;
    }

    /**
     * Sets the parse observer for this instance.
     *
     * @param observer {@code null-ok;} the observer
     */
    public void setObserver(ParseObserver observer) {
        this.observer = observer;
    }

    /**
     * Gets the end offset of this constant pool in the {@code byte[]}
     * which it came from.
     *
     * @return {@code >= 10;} the end offset
     */
    public int getEndOffset() {
        parseIfNecessary();
        return endOffset;
    }

    /**
     * Gets the actual constant pool.
     *
     * @return {@code non-null;} the constant pool
     */
    public StdConstantPool getPool() {
        parseIfNecessary();
        return pool;
    }

    /**
     * Runs {@link #parse} if it has not yet been run successfully.
     */
    private void parseIfNecessary() {
        if (endOffset < 0) {
            parse();
        }
    }

    /**
     * Does the actual parsing.
     */
    private void parse() {
        determineOffsets();

        if (observer != null) {
            observer.parsed(bytes, 8, 2,
                    "constant_pool_count: " + Hex.u2(offsets.length));
            observer.parsed(bytes, 10, 0, "\nconstant_pool:");
            observer.changeIndent(1);
        }

        /*
         * Track the constant value's original string type. True if constants[i] was
         * a CONSTANT_Utf8, false for any other type including CONSTANT_string.
         */
        BitSet wasUtf8 = new BitSet(offsets.length);

        for (int i = 1; i < offsets.length; i++) {
            int offset = offsets[i];
            if ((offset != 0) && (pool.getOrNull(i) == null)) {
                parse0(i, wasUtf8);
            }
        }

        if (observer != null) {
            for (int i = 1; i < offsets.length; i++) {
                Constant cst = pool.getOrNull(i);
                if (cst == null) {
                    continue;
                }
                int offset = offsets[i];
                int nextOffset = endOffset;
                for (int j = i + 1; j < offsets.length; j++) {
                    int off = offsets[j];
                    if (off != 0) {
                        nextOffset = off;
                        break;
                    }
                }
                String human = wasUtf8.get(i)
                        ? Hex.u2(i) + ": utf8{\"" + cst.toHuman() + "\"}"
                        : Hex.u2(i) + ": " + cst.toString();
                observer.parsed(bytes, offset, nextOffset - offset, human);
            }

            observer.changeIndent(-1);
            observer.parsed(bytes, endOffset, 0, "end constant_pool");
        }
    }

    /**
     * Populates {@link #offsets} and also completely parse utf8 constants.
     */
    private void determineOffsets() {
        int at = 10; // offset from the start of the file to the first cst
        int lastCategory;

        for (int i = 1; i < offsets.length; i += lastCategory) {
            offsets[i] = at;
            int tag = bytes.getUnsignedByte(at);
            try {
                switch (tag) {
                    case ConstantTags.CONSTANT_Integer:
                    case ConstantTags.CONSTANT_Float:
                    case ConstantTags.CONSTANT_Fieldref:
                    case ConstantTags.CONSTANT_Methodref:
                    case ConstantTags.CONSTANT_InterfaceMethodref:
                    case ConstantTags.CONSTANT_NameAndType: {
                        lastCategory = 1;
                        at += 5;
                        break;
                    }
                    case ConstantTags.CONSTANT_Long:
                    case ConstantTags.CONSTANT_Double: {
                        lastCategory = 2;
                        at += 9;
                        break;
                    }
                    case ConstantTags.CONSTANT_Class:
                    case ConstantTags.CONSTANT_String: {
                        lastCategory = 1;
                        at += 3;
                        break;
                    }
                    case ConstantTags.CONSTANT_Utf8: {
                        lastCategory = 1;
                        at += bytes.getUnsignedShort(at + 1) + 3;
                        break;
                    }
                    case ConstantTags.CONSTANT_MethodHandle: {
                        throw new ParseException("MethodHandle not supported");
                    }
                    case ConstantTags.CONSTANT_MethodType: {
                        throw new ParseException("MethodType not supported");
                    }
                    case ConstantTags.CONSTANT_InvokeDynamic: {
                        throw new ParseException("InvokeDynamic not supported");
                    }
                    default: {
                        throw new ParseException("unknown tag byte: " + Hex.u1(tag));
                    }
                }
            } catch (ParseException ex) {
                ex.addContext("...while preparsing cst " + Hex.u2(i) + " at offset " + Hex.u4(at));
                throw ex;
            }
        }

        endOffset = at;
    }

    /**
     * Parses the constant for the given index if it hasn't already been
     * parsed, also storing it in the constant pool. This will also
     * have the side effect of parsing any entries the indicated one
     * depends on.
     *
     * @param idx which constant
     * @return {@code non-null;} the parsed constant
     */
    private Constant parse0(int idx, BitSet wasUtf8) {
        Constant cst = pool.getOrNull(idx);
        if (cst != null) {
            return cst;
        }

        int at = offsets[idx];

        try {
            int tag = bytes.getUnsignedByte(at);
            switch (tag) {
                case ConstantTags.CONSTANT_Utf8: {
                    cst = parseUtf8(at);
                    wasUtf8.set(idx);
                    break;
                }
                case ConstantTags.CONSTANT_Integer: {
                    int value = bytes.getInt(at + 1);
                    cst = CstInteger.make(value);
                    break;
                }
                case ConstantTags.CONSTANT_Float: {
                    int bits = bytes.getInt(at + 1);
                    cst = CstFloat.make(bits);
                    break;
                }
                case ConstantTags.CONSTANT_Long: {
                    long value = bytes.getLong(at + 1);
                    cst = CstLong.make(value);
                    break;
                }
                case ConstantTags.CONSTANT_Double: {
                    long bits = bytes.getLong(at + 1);
                    cst = CstDouble.make(bits);
                    break;
                }
                case ConstantTags.CONSTANT_Class: {
                    int nameIndex = bytes.getUnsignedShort(at + 1);
                    CstString name = (CstString) parse0(nameIndex, wasUtf8);
                    cst = new CstType(Type.internClassName(name.getString()));
                    break;
                }
                case ConstantTags.CONSTANT_String: {
                    int stringIndex = bytes.getUnsignedShort(at + 1);
                    cst = parse0(stringIndex, wasUtf8);
                    break;
                }
                case ConstantTags.CONSTANT_Fieldref: {
                    int classIndex = bytes.getUnsignedShort(at + 1);
                    CstType type = (CstType) parse0(classIndex, wasUtf8);
                    int natIndex = bytes.getUnsignedShort(at + 3);
                    CstNat nat = (CstNat) parse0(natIndex, wasUtf8);
                    cst = new CstFieldRef(type, nat);
                    break;
                }
                case ConstantTags.CONSTANT_Methodref: {
                    int classIndex = bytes.getUnsignedShort(at + 1);
                    CstType type = (CstType) parse0(classIndex, wasUtf8);
                    int natIndex = bytes.getUnsignedShort(at + 3);
                    CstNat nat = (CstNat) parse0(natIndex, wasUtf8);
                    cst = new CstMethodRef(type, nat);
                    break;
                }
                case ConstantTags.CONSTANT_InterfaceMethodref: {
                    int classIndex = bytes.getUnsignedShort(at + 1);
                    CstType type = (CstType) parse0(classIndex, wasUtf8);
                    int natIndex = bytes.getUnsignedShort(at + 3);
                    CstNat nat = (CstNat) parse0(natIndex, wasUtf8);
                    cst = new CstInterfaceMethodRef(type, nat);
                    break;
                }
                case ConstantTags.CONSTANT_NameAndType: {
                    int nameIndex = bytes.getUnsignedShort(at + 1);
                    CstString name = (CstString) parse0(nameIndex, wasUtf8);
                    int descriptorIndex = bytes.getUnsignedShort(at + 3);
                    CstString descriptor = (CstString) parse0(descriptorIndex, wasUtf8);
                    cst = new CstNat(name, descriptor);
                    break;
                }
                case ConstantTags.CONSTANT_MethodHandle: {
                    throw new ParseException("MethodHandle not supported");
                }
                case ConstantTags.CONSTANT_MethodType: {
                    throw new ParseException("MethodType not supported");
                }
                case ConstantTags.CONSTANT_InvokeDynamic: {
                    throw new ParseException("InvokeDynamic not supported");
                }
                default: {
                    throw new ParseException("unknown tag byte: " + Hex.u1(tag));
                }
            }
        } catch (ParseException ex) {
            ex.addContext("...while parsing cst " + Hex.u2(idx) +
                    " at offset " + Hex.u4(at));
            throw ex;
        } catch (RuntimeException ex) {
            ParseException pe = new ParseException(ex);
            pe.addContext("...while parsing cst " + Hex.u2(idx) +
                    " at offset " + Hex.u4(at));
            throw pe;
        }

        pool.set(idx, cst);
        return cst;
    }

    /**
     * Parses a utf8 constant.
     *
     * @param at offset to the start of the constant (where the tag byte is)
     * @return {@code non-null;} the parsed value
     */
    private CstString parseUtf8(int at) {
        int length = bytes.getUnsignedShort(at + 1);

        at += 3; // Skip to the data.

        ByteArray ubytes = bytes.slice(at, at + length);

        try {
            return new CstString(ubytes);
        } catch (IllegalArgumentException ex) {
            // Translate the exception
            throw new ParseException(ex);
        }
    }
}
