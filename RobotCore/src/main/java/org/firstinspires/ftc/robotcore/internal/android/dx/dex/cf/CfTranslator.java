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

package org.firstinspires.ftc.robotcore.internal.android.dx.dex.cf;

import org.firstinspires.ftc.robotcore.internal.android.dex.util.ExceptionWithContext;
import org.firstinspires.ftc.robotcore.internal.android.dx.cf.code.ConcreteMethod;
import org.firstinspires.ftc.robotcore.internal.android.dx.cf.code.Ropper;
import org.firstinspires.ftc.robotcore.internal.android.dx.cf.direct.DirectClassFile;
import org.firstinspires.ftc.robotcore.internal.android.dx.cf.iface.Field;
import org.firstinspires.ftc.robotcore.internal.android.dx.cf.iface.FieldList;
import org.firstinspires.ftc.robotcore.internal.android.dx.cf.iface.Method;
import org.firstinspires.ftc.robotcore.internal.android.dx.cf.iface.MethodList;
import org.firstinspires.ftc.robotcore.internal.android.dx.dex.DexOptions;
import org.firstinspires.ftc.robotcore.internal.android.dx.dex.code.DalvCode;
import org.firstinspires.ftc.robotcore.internal.android.dx.dex.code.PositionList;
import org.firstinspires.ftc.robotcore.internal.android.dx.dex.code.RopTranslator;
import org.firstinspires.ftc.robotcore.internal.android.dx.dex.file.ClassDefItem;
import org.firstinspires.ftc.robotcore.internal.android.dx.dex.file.DexFile;
import org.firstinspires.ftc.robotcore.internal.android.dx.dex.file.EncodedField;
import org.firstinspires.ftc.robotcore.internal.android.dx.dex.file.EncodedMethod;
import org.firstinspires.ftc.robotcore.internal.android.dx.dex.file.FieldIdsSection;
import org.firstinspires.ftc.robotcore.internal.android.dx.dex.file.MethodIdsSection;
import org.firstinspires.ftc.robotcore.internal.android.dx.rop.annotation.Annotations;
import org.firstinspires.ftc.robotcore.internal.android.dx.rop.annotation.AnnotationsList;
import org.firstinspires.ftc.robotcore.internal.android.dx.rop.code.AccessFlags;
import org.firstinspires.ftc.robotcore.internal.android.dx.rop.code.DexTranslationAdvice;
import org.firstinspires.ftc.robotcore.internal.android.dx.rop.code.LocalVariableExtractor;
import org.firstinspires.ftc.robotcore.internal.android.dx.rop.code.LocalVariableInfo;
import org.firstinspires.ftc.robotcore.internal.android.dx.rop.code.RopMethod;
import org.firstinspires.ftc.robotcore.internal.android.dx.rop.code.TranslationAdvice;
import org.firstinspires.ftc.robotcore.internal.android.dx.rop.cst.Constant;
import org.firstinspires.ftc.robotcore.internal.android.dx.rop.cst.ConstantPool;
import org.firstinspires.ftc.robotcore.internal.android.dx.rop.cst.CstBaseMethodRef;
import org.firstinspires.ftc.robotcore.internal.android.dx.rop.cst.CstBoolean;
import org.firstinspires.ftc.robotcore.internal.android.dx.rop.cst.CstByte;
import org.firstinspires.ftc.robotcore.internal.android.dx.rop.cst.CstChar;
import org.firstinspires.ftc.robotcore.internal.android.dx.rop.cst.CstEnumRef;
import org.firstinspires.ftc.robotcore.internal.android.dx.rop.cst.CstFieldRef;
import org.firstinspires.ftc.robotcore.internal.android.dx.rop.cst.CstInteger;
import org.firstinspires.ftc.robotcore.internal.android.dx.rop.cst.CstInterfaceMethodRef;
import org.firstinspires.ftc.robotcore.internal.android.dx.rop.cst.CstMethodRef;
import org.firstinspires.ftc.robotcore.internal.android.dx.rop.cst.CstShort;
import org.firstinspires.ftc.robotcore.internal.android.dx.rop.cst.CstString;
import org.firstinspires.ftc.robotcore.internal.android.dx.rop.cst.CstType;
import org.firstinspires.ftc.robotcore.internal.android.dx.rop.cst.TypedConstant;
import org.firstinspires.ftc.robotcore.internal.android.dx.rop.type.Type;
import org.firstinspires.ftc.robotcore.internal.android.dx.rop.type.TypeList;
import org.firstinspires.ftc.robotcore.internal.android.dx.ssa.Optimizer;

/**
 * Static method that turns {@code byte[]}s containing Java
 * classfiles into {@link ClassDefItem} instances.
 */
public class CfTranslator {
    /**
     * set to {@code true} to enable development-time debugging code
     */
    private static final boolean DEBUG = false;

    /**
     * This class is uninstantiable.
     */
    private CfTranslator() {
        // This space intentionally left blank.
    }

    /**
     * Takes a {@code byte[]}, interprets it as a Java classfile, and
     * translates it into a {@link ClassDefItem}.
     *
     * @param cf         {@code non-null;} the class file
     * @param bytes      {@code non-null;} contents of the file
     * @param cfOptions  options for class translation
     * @param dexOptions options for dex output
     * @param dexFile    {@code non-null;} dex output
     * @return {@code non-null;} the translated class
     */
    public static ClassDefItem translate(DirectClassFile cf, byte[] bytes,
                                         CfOptions cfOptions, DexOptions dexOptions, DexFile dexFile) {
        try {
            return translate0(cf, bytes, cfOptions, dexOptions, dexFile);
        } catch (RuntimeException ex) {
            String msg = "...while processing " + cf.getFilePath();
            throw ExceptionWithContext.withContext(ex, msg);
        }
    }

    /**
     * Performs the main act of translation. This method is separated
     * from {@link #translate} just to keep things a bit simpler in
     * terms of exception handling.
     *
     * @param cf         {@code non-null;} the class file
     * @param bytes      {@code non-null;} contents of the file
     * @param cfOptions  options for class translation
     * @param dexOptions options for dex output
     * @param dexFile    {@code non-null;} dex output
     * @return {@code non-null;} the translated class
     */
    private static ClassDefItem translate0(DirectClassFile cf, byte[] bytes,
                                           CfOptions cfOptions, DexOptions dexOptions, DexFile dexFile) {

        OptimizerOptions.loadOptimizeLists(cfOptions.optimizeListFile,
                cfOptions.dontOptimizeListFile);

        // Build up a class to output.

        CstType thisClass = cf.getThisClass();
        int classAccessFlags = cf.getAccessFlags() & ~AccessFlags.ACC_SUPER;
        CstString sourceFile = (cfOptions.positionInfo == PositionList.NONE) ? null :
                cf.getSourceFile();
        ClassDefItem out =
                new ClassDefItem(thisClass, classAccessFlags,
                        cf.getSuperclass(), cf.getInterfaces(), sourceFile);

        Annotations classAnnotations =
                AttributeTranslator.getClassAnnotations(cf, cfOptions);
        if (classAnnotations.size() != 0) {
            out.setClassAnnotations(classAnnotations, dexFile);
        }

        FieldIdsSection fieldIdsSection = dexFile.getFieldIds();
        MethodIdsSection methodIdsSection = dexFile.getMethodIds();
        processFields(cf, out, dexFile);
        processMethods(cf, cfOptions, dexOptions, out, dexFile);

        // intern constant pool method, field and type references
        ConstantPool constantPool = cf.getConstantPool();
        int constantPoolSize = constantPool.size();

        for (int i = 0; i < constantPoolSize; i++) {
            Constant constant = constantPool.getOrNull(i);
            if (constant instanceof CstMethodRef) {
                methodIdsSection.intern((CstBaseMethodRef) constant);
            } else if (constant instanceof CstInterfaceMethodRef) {
                methodIdsSection.intern(((CstInterfaceMethodRef) constant).toMethodRef());
            } else if (constant instanceof CstFieldRef) {
                fieldIdsSection.intern((CstFieldRef) constant);
            } else if (constant instanceof CstEnumRef) {
                fieldIdsSection.intern(((CstEnumRef) constant).getFieldRef());
            }
        }

        return out;
    }

    /**
     * Processes the fields of the given class.
     *
     * @param cf      {@code non-null;} class being translated
     * @param out     {@code non-null;} output class
     * @param dexFile {@code non-null;} dex output
     */
    private static void processFields(
            DirectClassFile cf, ClassDefItem out, DexFile dexFile) {
        CstType thisClass = cf.getThisClass();
        FieldList fields = cf.getFields();
        int sz = fields.size();

        for (int i = 0; i < sz; i++) {
            Field one = fields.get(i);
            try {
                CstFieldRef field = new CstFieldRef(thisClass, one.getNat());
                int accessFlags = one.getAccessFlags();

                if (AccessFlags.isStatic(accessFlags)) {
                    TypedConstant constVal = one.getConstantValue();
                    EncodedField fi = new EncodedField(field, accessFlags);
                    if (constVal != null) {
                        constVal = coerceConstant(constVal, field.getType());
                    }
                    out.addStaticField(fi, constVal);
                } else {
                    EncodedField fi = new EncodedField(field, accessFlags);
                    out.addInstanceField(fi);
                }

                Annotations annotations =
                        AttributeTranslator.getAnnotations(one.getAttributes());
                if (annotations.size() != 0) {
                    out.addFieldAnnotations(field, annotations, dexFile);
                }
                dexFile.getFieldIds().intern(field);
            } catch (RuntimeException ex) {
                String msg = "...while processing " + one.getName().toHuman() +
                        " " + one.getDescriptor().toHuman();
                throw ExceptionWithContext.withContext(ex, msg);
            }
        }
    }

    /**
     * Helper for {@link #processFields}, which translates constants into
     * more specific types if necessary.
     *
     * @param constant {@code non-null;} the constant in question
     * @param type     {@code non-null;} the desired type
     */
    private static TypedConstant coerceConstant(TypedConstant constant,
                                                Type type) {
        Type constantType = constant.getType();

        if (constantType.equals(type)) {
            return constant;
        }

        switch (type.getBasicType()) {
            case Type.BT_BOOLEAN: {
                return CstBoolean.make(((CstInteger) constant).getValue());
            }
            case Type.BT_BYTE: {
                return CstByte.make(((CstInteger) constant).getValue());
            }
            case Type.BT_CHAR: {
                return CstChar.make(((CstInteger) constant).getValue());
            }
            case Type.BT_SHORT: {
                return CstShort.make(((CstInteger) constant).getValue());
            }
            default: {
                throw new UnsupportedOperationException("can't coerce " +
                        constant + " to " + type);
            }
        }
    }

    /**
     * Processes the methods of the given class.
     *
     * @param cf         {@code non-null;} class being translated
     * @param cfOptions  {@code non-null;} options for class translation
     * @param dexOptions {@code non-null;} options for dex output
     * @param out        {@code non-null;} output class
     * @param dexFile    {@code non-null;} dex output
     */
    private static void processMethods(DirectClassFile cf, CfOptions cfOptions,
                                       DexOptions dexOptions, ClassDefItem out, DexFile dexFile) {
        CstType thisClass = cf.getThisClass();
        MethodList methods = cf.getMethods();
        int sz = methods.size();

        for (int i = 0; i < sz; i++) {
            Method one = methods.get(i);
            try {
                CstMethodRef meth = new CstMethodRef(thisClass, one.getNat());
                int accessFlags = one.getAccessFlags();
                boolean isStatic = AccessFlags.isStatic(accessFlags);
                boolean isPrivate = AccessFlags.isPrivate(accessFlags);
                boolean isNative = AccessFlags.isNative(accessFlags);
                boolean isAbstract = AccessFlags.isAbstract(accessFlags);
                boolean isConstructor = meth.isInstanceInit() ||
                        meth.isClassInit();
                DalvCode code;

                if (isNative || isAbstract) {
                    // There's no code for native or abstract methods.
                    code = null;
                } else {
                    ConcreteMethod concrete =
                            new ConcreteMethod(one, cf,
                                    (cfOptions.positionInfo != PositionList.NONE),
                                    cfOptions.localInfo);

                    TranslationAdvice advice;

                    advice = DexTranslationAdvice.THE_ONE;

                    RopMethod rmeth = Ropper.convert(concrete, advice, methods);
                    RopMethod nonOptRmeth = null;
                    int paramSize;

                    paramSize = meth.getParameterWordCount(isStatic);

                    String canonicalName
                            = thisClass.getClassType().getDescriptor()
                            + "." + one.getName().getString();

                    if (cfOptions.optimize &&
                            OptimizerOptions.shouldOptimize(canonicalName)) {
                        if (DEBUG) {
                            System.err.println("Optimizing " + canonicalName);
                        }

                        nonOptRmeth = rmeth;
                        rmeth = Optimizer.optimize(rmeth,
                                paramSize, isStatic, cfOptions.localInfo, advice);

                        if (DEBUG) {
                            OptimizerOptions.compareOptimizerStep(nonOptRmeth,
                                    paramSize, isStatic, cfOptions, advice, rmeth);
                        }

                        if (cfOptions.statistics) {
                            CodeStatistics.updateRopStatistics(
                                    nonOptRmeth, rmeth);
                        }
                    }

                    LocalVariableInfo locals = null;

                    if (cfOptions.localInfo) {
                        locals = LocalVariableExtractor.extract(rmeth);
                    }

                    code = RopTranslator.translate(rmeth, cfOptions.positionInfo,
                            locals, paramSize, dexOptions);

                    if (cfOptions.statistics && nonOptRmeth != null) {
                        updateDexStatistics(cfOptions, dexOptions, rmeth, nonOptRmeth, locals,
                                paramSize, concrete.getCode().size());
                    }
                }

                // Preserve the synchronized flag as its "declared" variant...
                if (AccessFlags.isSynchronized(accessFlags)) {
                    accessFlags |= AccessFlags.ACC_DECLARED_SYNCHRONIZED;

                    /*
                     * ...but only native methods are actually allowed to be
                     * synchronized.
                     */
                    if (!isNative) {
                        accessFlags &= ~AccessFlags.ACC_SYNCHRONIZED;
                    }
                }

                if (isConstructor) {
                    accessFlags |= AccessFlags.ACC_CONSTRUCTOR;
                }

                TypeList exceptions = AttributeTranslator.getExceptions(one);
                EncodedMethod mi =
                        new EncodedMethod(meth, accessFlags, code, exceptions);

                if (meth.isInstanceInit() || meth.isClassInit() ||
                        isStatic || isPrivate) {
                    out.addDirectMethod(mi);
                } else {
                    out.addVirtualMethod(mi);
                }

                Annotations annotations =
                        AttributeTranslator.getMethodAnnotations(one);
                if (annotations.size() != 0) {
                    out.addMethodAnnotations(meth, annotations, dexFile);
                }

                AnnotationsList list =
                        AttributeTranslator.getParameterAnnotations(one);
                if (list.size() != 0) {
                    out.addParameterAnnotations(meth, list, dexFile);
                }
                dexFile.getMethodIds().intern(meth);
            } catch (RuntimeException ex) {
                String msg = "...while processing " + one.getName().toHuman() +
                        " " + one.getDescriptor().toHuman();
                throw ExceptionWithContext.withContext(ex, msg);
            }
        }
    }

    /**
     * Helper that updates the dex statistics.
     */
    private static void updateDexStatistics(CfOptions cfOptions, DexOptions dexOptions,
                                            RopMethod optRmeth, RopMethod nonOptRmeth,
                                            LocalVariableInfo locals, int paramSize, int originalByteCount) {
        /*
         * Run rop->dex again on optimized vs. non-optimized method to
         * collect statistics. We have to totally convert both ways,
         * since converting the "real" method getting added to the
         * file would corrupt it (by messing with its constant pool
         * indices).
         */

        DalvCode optCode = RopTranslator.translate(optRmeth,
                cfOptions.positionInfo, locals, paramSize, dexOptions);
        DalvCode nonOptCode = RopTranslator.translate(nonOptRmeth,
                cfOptions.positionInfo, locals, paramSize, dexOptions);

        /*
         * Fake out the indices, so code.getInsns() can work well enough
         * for the current purpose.
         */

        DalvCode.AssignIndicesCallback callback =
                new DalvCode.AssignIndicesCallback() {
                    public int getIndex(Constant cst) {
                        // Everything is at index 0!
                        return 0;
                    }
                };

        optCode.assignIndices(callback);
        nonOptCode.assignIndices(callback);

        CodeStatistics.updateDexStatistics(nonOptCode, optCode);
        CodeStatistics.updateOriginalByteCount(originalByteCount);
    }
}
