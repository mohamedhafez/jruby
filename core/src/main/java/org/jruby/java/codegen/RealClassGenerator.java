/*
 **** BEGIN LICENSE BLOCK *****
 * Version: EPL 2.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Eclipse Public
 * License Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/epl-v20.html
 *
 * Software distributed under the License is distributed on an "AS
 * IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * rights and limitations under the License.
 *
 * Copyright (C) 2013-2015 Charles O Nutter <headius@headius.com>
 *
 * Alternatively, the contents of this file may be used under the terms of
 * either of the GNU General Public License Version 2 or later (the "GPL"),
 * or the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
 * in which case the provisions of the GPL or the LGPL are applicable instead
 * of those above. If you wish to allow use of your version of this file only
 * under the terms of either the GPL or the LGPL, and not to allow others to
 * use your version of this file under the terms of the EPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the EPL, the GPL or the LGPL.
 ***** END LICENSE BLOCK *****/

package org.jruby.java.codegen;

import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.jruby.Ruby;
import org.jruby.RubyBasicObject;
import org.jruby.RubyClass;
import org.jruby.RubyModule;
import org.jruby.ast.executable.RuntimeCache;
import org.jruby.compiler.impl.SkinnyMethodAdapter;
import org.jruby.compiler.util.BasicObjectStubGenerator;
import org.jruby.internal.runtime.methods.DynamicMethod;
import org.jruby.javasupport.JavaUtil;
import org.jruby.runtime.Block;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.ClassDefiningClassLoader;
import org.jruby.util.ClassDefiningJRubyClassLoader;
import org.jruby.util.Loader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.Type;

import static org.jruby.util.CodegenUtils.ci;
import static org.jruby.util.CodegenUtils.getBoxType;
import static org.jruby.util.CodegenUtils.p;
import static org.jruby.util.CodegenUtils.params;
import static org.jruby.util.CodegenUtils.prettyParams;
import static org.jruby.util.CodegenUtils.sig;
import static org.objectweb.asm.Opcodes.ACC_FINAL;
import static org.objectweb.asm.Opcodes.ACC_PRIVATE;
import static org.objectweb.asm.Opcodes.ACC_PUBLIC;
import static org.objectweb.asm.Opcodes.ACC_STATIC;
import static org.objectweb.asm.Opcodes.ACC_SUPER;
import static org.objectweb.asm.Opcodes.V1_6;

/**
 * On fly .class generator (used for Ruby interface impls).
 *
 * @author headius
 */
public abstract class RealClassGenerator {

    private static final boolean DEBUG = false;

    private static final int V_BC = V1_6; // version used for generated byte-code

    //public static Map<String, List<Method>> buildSimpleToAllMap(Class[] interfaces, String[] superTypeNames)
    //    throws SecurityException {
    //    return buildSimpleToAllMap(interfaces, superTypeNames, null);
    //}

    static Map<String, List<Method>> buildSimpleToAllMap(Class[] interfaces, String[] superTypeNames, RubyClass implClass)
        throws SecurityException {
        final LinkedHashMap<String, List<Method>> simpleToAll = new LinkedHashMap<>();
        // we're use the map's order to work-around bug when there's too getters for a property :
        // getFoo and isFoo in which case we make sure getFoo will come after isFoo in the map
        // so that the installed "foo" alias always triggers getFoo regardless of getMethods order
        for (int i = 0; i < interfaces.length; i++) {
            superTypeNames[i] = p(interfaces[i]);
            for ( Method method : interfaces[i].getMethods() ) {
                final String name = method.getName();
                if ( Modifier.isStatic(method.getModifiers()) ) continue;
                if ( implClass != null ) { // only override default methods if present in implementing class
                    if ( ! Modifier.isAbstract(method.getModifiers()) && ! implClass.getMethods().containsKey(name) ) {
                        continue;
                    }
                }
                List<Method> methods = simpleToAll.get(name);
                if (methods == null) {
                    simpleToAll.put(name, methods = new ArrayList<Method>(6));

                    if ( name.startsWith("is") && name.length() > 2 ) {
                        final String getName = "get" + name.substring(2);
                        List<Method> getMethods = simpleToAll.get(getName);
                        if ( getMethods != null ) { // remove and re-add so that getFoo is after isFoo
                            simpleToAll.remove(getName);
                            simpleToAll.put(getName, getMethods);
                        }
                    }
                }
                methods.add(method);
            }
        }
        return simpleToAll;
    }

    // NOTE: assuming this is only used for interface-impl generation from: Java.newInterfaceImpl
    public static Class createOldStyleImplClass(Class[] superTypes, RubyClass rubyClass, Ruby ruby, String name, ClassDefiningClassLoader classLoader) {
        String[] superTypeNames = new String[superTypes.length];

        // interfaces now do have a convention that they only override an interface default method
        // if a Ruby method (stub) is present in the implementing Ruby class :
        Map<String, List<Method>> simpleToAll = buildSimpleToAllMap(superTypes, superTypeNames, rubyClass);

        Class newClass = defineOldStyleImplClass(ruby, name, superTypeNames, simpleToAll, classLoader);

        return newClass;
    }

    // NOTE: only used for interface class generation from ... Java.generateRealClass
    public static Class createRealImplClass(Class superClass, Class[] interfaces, RubyClass rubyClass, Ruby ruby, String name) {
        String[] superTypeNames = new String[interfaces.length];

        // interfaces now do have a convention that they only override an interface default method
        // if a Ruby method (stub) is present in the implementing Ruby class :
        Map<String, List<Method>> simpleToAll = buildSimpleToAllMap(interfaces, superTypeNames, rubyClass);

        Class newClass = defineRealImplClass(ruby, name, superClass, superTypeNames, simpleToAll);

        // Confirm all interfaces got implemented
        for (Class ifc : interfaces) {
            assert ifc.isAssignableFrom(newClass);
        }

        return newClass;
    }

    /**
     * This variation on defineImplClass uses all the classic type coercion logic
     * for passing args and returning results.
     *
     * @param ruby
     * @param name
     * @param superTypeNames
     * @param simpleToAll
     * @return
     */
    public static Class defineOldStyleImplClass(final Ruby ruby, final String name,
        final String[] superTypeNames, final Map<String, List<Method>> simpleToAll,
        final ClassDefiningClassLoader classLoader) {

        Class newClass;
        synchronized (classLoader) {
            // try to load the specified name; only if that fails, try to define the class
            try {
                newClass = classLoader.loadClass(name);
            }
            catch (ClassNotFoundException ex) {
                ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
                String pathName = name.replace('.', '/');

                // construct the class, implementing all supertypes
                cw.visit(V_BC, ACC_PUBLIC | ACC_SUPER, pathName, null, p(Object.class), superTypeNames);
                cw.visitSource(pathName + ".gen", null);

                // fields needed for dispatch and such
                cw.visitField(ACC_STATIC | ACC_FINAL | ACC_PRIVATE, "$runtimeCache", ci(RuntimeCache.class), null, null).visitEnd();
                cw.visitField(ACC_PRIVATE | ACC_FINAL, "$self", ci(IRubyObject.class), null, null).visitEnd();

                // create static init
                SkinnyMethodAdapter clinitMethod = new SkinnyMethodAdapter(cw, ACC_PUBLIC | ACC_STATIC, "<clinit>", sig(void.class), null, null);

                // create constructor
                SkinnyMethodAdapter initMethod = new SkinnyMethodAdapter(cw, ACC_PUBLIC, "<init>", sig(void.class, IRubyObject.class), null, null);
                initMethod.aload(0);
                initMethod.invokespecial(p(Object.class), "<init>", sig(void.class));

                // store the wrapper
                initMethod.aload(0);
                initMethod.aload(1);
                initMethod.putfield(pathName, "$self", ci(IRubyObject.class));

                // end constructor
                initMethod.voidreturn();
                initMethod.end();

                int cacheSize = 0;

                final HashSet<String> implementedNames = new HashSet<String>();

                // for each simple method name, implement the complex methods, calling the simple version
                for (Map.Entry<String, List<Method>> entry : simpleToAll.entrySet()) {
                    final String simpleName = entry.getKey();
                    final List<Method> methods = entry.getValue();
                    Set<String> nameSet = JavaUtil.getRubyNamesForJavaName(simpleName, methods);

                    implementedNames.clear();

                    for (int i = 0; i < methods.size(); i++) {
                        final Method method = methods.get(i);
                        final Class[] paramTypes = method.getParameterTypes();
                        final Class returnType = method.getReturnType();

                        String fullName = simpleName + prettyParams(paramTypes);
                        if (implementedNames.contains(fullName)) continue;
                        implementedNames.add(fullName);

                        // indices for temp values
                        final int baseIndex = calcBaseIndex(paramTypes, 1);

                        SkinnyMethodAdapter mv = new SkinnyMethodAdapter(
                                cw, ACC_PUBLIC, simpleName, sig(returnType, paramTypes), null, null);
                        mv.start();
                        mv.line(1);

                        switch ( simpleName ) {
                            // TODO: this code should really check if a Ruby equals method is implemented or not.
                            case "equals" :
                                if ( defineDefaultEquals(2, mv, paramTypes, returnType) ) ;
                                else defineOldStyleBody(mv, pathName, simpleName, paramTypes, returnType, baseIndex, cacheSize++, nameSet);
                                break;
                            case "hashCode" :
                                if ( defineDefaultHashCode(3, mv, paramTypes, returnType) ) ;
                                else defineOldStyleBody(mv, pathName, simpleName, paramTypes, returnType, baseIndex, cacheSize++, nameSet);
                                break;
                            case "toString" :
                                if ( defineDefaultToString(4, mv, paramTypes, returnType) ) ;
                                else defineOldStyleBody(mv, pathName, simpleName, paramTypes, returnType, baseIndex, cacheSize++, nameSet);
                                break;
                            case "__ruby_object" :
                                if ( paramTypes.length == 0 && returnType == IRubyObject.class ) {
                                    mv.aload(0);
                                    mv.getfield(pathName, "$self", ci(IRubyObject.class));
                                    mv.areturn();
                                    break;
                                }
                            default : // cacheIndex = cacheSize++;
                                defineOldStyleBody(mv, pathName, simpleName, paramTypes, returnType, baseIndex, cacheSize++, nameSet);
                                break;
                        }

                        mv.end();
                    }
                }

                // end setup method
                clinitMethod.newobj(p(RuntimeCache.class));
                clinitMethod.dup();
                clinitMethod.invokespecial(p(RuntimeCache.class), "<init>", sig(void.class));
                clinitMethod.dup();
                clinitMethod.ldc(cacheSize);
                clinitMethod.invokevirtual(p(RuntimeCache.class), "initMethodCache", sig(void.class, int.class));
                clinitMethod.putstatic(pathName, "$runtimeCache", ci(RuntimeCache.class));
                clinitMethod.voidreturn();
                clinitMethod.end();

                // end class
                cw.visitEnd();

                // create the class
                final byte[] bytecode = cw.toByteArray();
                newClass = classLoader.defineClass(name, bytecode);
                if ( DEBUG ) writeClassFile(name, bytecode);
            }
        }

        return newClass;
    }

    private static void defineOldStyleBody(SkinnyMethodAdapter mv, final String pathName,
        final String simpleName, final Class[] paramTypes, final Class returnType,
        final int baseIndex, final int cacheIndex, final Set<String> nameSet) {

        final int selfIndex = baseIndex;
        final int rubyIndex = selfIndex + 1;

        mv.line(5);

        // prepare temp locals
        mv.aload(0);
        mv.getfield(pathName, "$self", ci(IRubyObject.class));
        mv.astore(selfIndex);
        mv.aload(selfIndex);
        mv.invokeinterface(p(IRubyObject.class), "getRuntime", sig(Ruby.class));
        mv.astore(rubyIndex);

        // get method from cache
        mv.getstatic(pathName, "$runtimeCache", ci(RuntimeCache.class));
        mv.aload(selfIndex);
        mv.ldc(cacheIndex);
        for (String eachName : nameSet) {
            mv.ldc(eachName);
        }
        mv.invokevirtual(p(RuntimeCache.class), "searchWithCache",
            sig(DynamicMethod.class, params(IRubyObject.class, int.class, String.class, nameSet.size())));

        // get current context
        mv.aload(rubyIndex);
        mv.invokevirtual(p(Ruby.class), "getCurrentContext", sig(ThreadContext.class));

        // load self, class, and name
        mv.aloadMany(selfIndex, selfIndex);
        mv.invokeinterface(p(IRubyObject.class), "getMetaClass", sig(RubyClass.class));
        mv.ldc(simpleName);

        // coerce arguments
        coerceArgumentsToRuby(mv, paramTypes, rubyIndex);

        // load null block
        mv.getstatic(p(Block.class), "NULL_BLOCK", ci(Block.class));

        // invoke method
        mv.line(13);
        mv.invokevirtual(p(DynamicMethod.class), "call", sig(IRubyObject.class, ThreadContext.class, IRubyObject.class, RubyModule.class, String.class, IRubyObject[].class, Block.class));

        coerceResultAndReturn(mv, returnType);
    }

    /**
     * This variation on defineImplClass uses all the classic type coercion logic
     * for passing args and returning results.
     *
     * @param runtime
     * @param name
     * @param superTypeNames
     * @param simpleToAll
     * @return
     */
    public static Class defineRealImplClass(final Ruby runtime, final String name,
        final Class superClass, final String[] superTypeNames,
        final Map<String, List<Method>> simpleToAll) {

        final ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        final String pathName = name.replace('.', '/');

        boolean isRubyHierarchy = RubyBasicObject.class.isAssignableFrom(superClass);

        // construct the class, implementing all supertypes
        if (isRubyHierarchy) {
            // Ruby hierarchy...just extend it
            cw.visit(V_BC, ACC_PUBLIC | ACC_SUPER, pathName, null, p(superClass), superTypeNames);
        }
        else {
            // Non-Ruby hierarchy; add IRubyObject
            String[] plusIRubyObject = new String[superTypeNames.length + 1];
            plusIRubyObject[0] = p(IRubyObject.class);
            System.arraycopy(superTypeNames, 0, plusIRubyObject, 1, superTypeNames.length);

            cw.visit(V_BC, ACC_PUBLIC | ACC_SUPER, pathName, null, p(superClass), plusIRubyObject);
        }
        cw.visitSource(pathName + ".gen", null);

        // fields needed for dispatch and such
        cw.visitField(ACC_STATIC | ACC_FINAL | ACC_PRIVATE, "$runtimeCache", ci(RuntimeCache.class), null, null).visitEnd();

        // create static init
        SkinnyMethodAdapter clinitMethod = new SkinnyMethodAdapter(cw, ACC_PUBLIC | ACC_STATIC, "<clinit>", sig(void.class), null, null);

        // create constructor
        SkinnyMethodAdapter initMethod = new SkinnyMethodAdapter(cw, ACC_PUBLIC, "<init>", sig(void.class, Ruby.class, RubyClass.class), null, null);

        if (isRubyHierarchy) {
            // superclass is in the Ruby object hierarchy; invoke typical Ruby superclass constructor
            initMethod.aloadMany(0, 1, 2);
            initMethod.invokespecial(p(superClass), "<init>", sig(void.class, Ruby.class, RubyClass.class));
        }
        else {
            // superclass is not in Ruby hierarchy; store objects and call no-arg super constructor
            cw.visitField(ACC_FINAL | ACC_PRIVATE, "$ruby", ci(Ruby.class), null, null).visitEnd();
            cw.visitField(ACC_FINAL | ACC_PRIVATE, "$rubyClass", ci(RubyClass.class), null, null).visitEnd();

            initMethod.aloadMany(0, 1);
            initMethod.putfield(pathName, "$ruby", ci(Ruby.class));
            initMethod.aloadMany(0, 2);
            initMethod.putfield(pathName, "$rubyClass", ci(RubyClass.class));

            // only no-arg super constructor supported right now
            initMethod.aload(0);
            initMethod.invokespecial(p(superClass), "<init>", sig(void.class));
        }
        initMethod.voidreturn();
        initMethod.end();

        if (isRubyHierarchy) { // override toJava
            SkinnyMethodAdapter toJavaMethod = new SkinnyMethodAdapter(cw, ACC_PUBLIC, "toJava", sig(Object.class, Class.class), null, null);
            toJavaMethod.aload(0);
            toJavaMethod.areturn();
            toJavaMethod.end();
        }
        else { // decorate with stubbed IRubyObject methods
            BasicObjectStubGenerator.addBasicObjectStubsToClass(cw);

            // add getRuntime and getMetaClass impls based on captured fields
            SkinnyMethodAdapter getRuntimeMethod = new SkinnyMethodAdapter(cw, ACC_PUBLIC, "getRuntime", sig(Ruby.class), null, null);
            getRuntimeMethod.aload(0);
            getRuntimeMethod.getfield(pathName, "$ruby", ci(Ruby.class));
            getRuntimeMethod.areturn();
            getRuntimeMethod.end();

            SkinnyMethodAdapter getMetaClassMethod = new SkinnyMethodAdapter(cw, ACC_PUBLIC, "getMetaClass", sig(RubyClass.class), null, null);
            getMetaClassMethod.aload(0);
            getMetaClassMethod.getfield(pathName, "$rubyClass", ci(RubyClass.class));
            getMetaClassMethod.areturn();
            getMetaClassMethod.end();
        }

        int cacheSize = 0;

        final HashSet<String> implementedNames = new HashSet<String>();

        // for each simple method name, implement the complex methods, calling the simple version
        for (Map.Entry<String, List<Method>> entry : simpleToAll.entrySet()) {
            final String simpleName = entry.getKey();
            final List<Method> methods = entry.getValue();
            Set<String> nameSet = JavaUtil.getRubyNamesForJavaName(simpleName, methods);

            implementedNames.clear();

            for (int i = 0; i < methods.size(); i++) {
                final Method method = methods.get(i);
                final Class[] paramTypes = method.getParameterTypes();
                final Class returnType = method.getReturnType();

                String fullName = simpleName + prettyParams(paramTypes);
                if (implementedNames.contains(fullName)) continue;
                implementedNames.add(fullName);

                // indices for temp values
                final int baseIndex = calcBaseIndex(paramTypes, 1);

                SkinnyMethodAdapter mv = new SkinnyMethodAdapter(
                        cw, ACC_PUBLIC, simpleName, sig(returnType, paramTypes), null, null);
                mv.start();
                mv.line(1);

                switch ( simpleName ) { // cacheIndex = cacheSize++;

                    case "equals" :
                        if ( paramTypes.length == 1 && paramTypes[0] == Object.class && returnType == Boolean.TYPE ) {
                            defineRealEqualsWithFallback(mv, pathName, simpleName, paramTypes, returnType, baseIndex, cacheSize++, nameSet);
                        }
                        else defineRealBody(mv, pathName, simpleName, paramTypes, returnType, baseIndex, cacheSize++, nameSet);
                        break;
                    case "hashCode" :
                        if ( paramTypes.length == 0 && returnType == Integer.TYPE ) {
                            defineRealHashCodeWithFallback(mv, pathName, simpleName, paramTypes, returnType, baseIndex, cacheSize++, nameSet);
                        }
                        else defineRealBody(mv, pathName, simpleName, paramTypes, returnType, baseIndex, cacheSize++, nameSet);
                        break;
                    case "toString" :
                        if ( paramTypes.length == 0 && returnType == String.class ) {
                            defineRealToStringWithFallback(mv, pathName, simpleName, paramTypes, returnType, baseIndex, cacheSize++, nameSet);
                        }
                        else defineRealBody(mv, pathName, simpleName, paramTypes, returnType, baseIndex, cacheSize++, nameSet);
                        break;
                    default :
                        defineRealBody(mv, pathName, simpleName, paramTypes, returnType, baseIndex, cacheSize++, nameSet);
                        break;
                }

                mv.end();
            }
        }

        // end setup method
        clinitMethod.newobj(p(RuntimeCache.class));
        clinitMethod.dup();
        clinitMethod.invokespecial(p(RuntimeCache.class), "<init>", sig(void.class));
        clinitMethod.dup();
        clinitMethod.ldc(cacheSize);
        clinitMethod.invokevirtual(p(RuntimeCache.class), "initMethodCache", sig(void.class, int.class));
        clinitMethod.putstatic(pathName, "$runtimeCache", ci(RuntimeCache.class));
        clinitMethod.voidreturn();
        clinitMethod.end();

        // end class
        cw.visitEnd();

        // first try to find the class
        Class newClass = null;
        for(Loader loader : runtime.getInstanceConfig().getExtraLoaders()) {
            try {
                newClass = loader.loadClass(name);
                break;
            }
            catch(ClassNotFoundException ignored) {
            }
        }

        final ClassDefiningJRubyClassLoader loader;
        if (superClass.getClassLoader() instanceof ClassDefiningJRubyClassLoader) {
            loader = new ClassDefiningJRubyClassLoader(superClass.getClassLoader());
        } else {
            loader = new ClassDefiningJRubyClassLoader(runtime.getJRubyClassLoader());
        }

        if (newClass == null) {
            try {
                newClass = loader.loadClass(name);
            }
            catch (ClassNotFoundException ignored) {
            }
        }

        // create the class
        if (newClass == null) {
            final byte[] bytecode = cw.toByteArray();
            MultiClassLoader multiClassLoader = new MultiClassLoader(superClass.getClassLoader());
            for(Loader cLoader : runtime.getInstanceConfig().getExtraLoaders()) {
                multiClassLoader.addClassLoader(cLoader.getClassLoader());
            }
            try {
                newClass = new ClassDefiningJRubyClassLoader(multiClassLoader).defineClass(name, bytecode);
            }
            catch(Error ignored) {
            }
            if (newClass == null) {
                newClass = loader.defineClass(name, bytecode);
            }
            if ( DEBUG ) writeClassFile(name, bytecode);
        }

        return newClass;
    }

    private static void defineRealBody(SkinnyMethodAdapter mv, final String pathName,
        final String simpleName, final Class[] paramTypes, final Class returnType,
        final int baseIndex, final int cacheIndex, final Set<String> nameSet) {

        final int rubyIndex = baseIndex + 1;

        mv.line(5);

        // prepare temp locals
        mv.aload(0);
        mv.invokeinterface(p(IRubyObject.class), "getRuntime", sig(Ruby.class));
        mv.astore(rubyIndex);

        // get method from cache
        mv.getstatic(pathName, "$runtimeCache", ci(RuntimeCache.class));
        mv.aload(0);
        mv.ldc(cacheIndex);
        for (String eachName : nameSet) {
            mv.ldc(eachName);
        }
        mv.invokevirtual(p(RuntimeCache.class), "searchWithCache",
                sig(DynamicMethod.class, params(IRubyObject.class, int.class, String.class, nameSet.size())));

        // get current context
        mv.aload(rubyIndex);
        mv.invokevirtual(p(Ruby.class), "getCurrentContext", sig(ThreadContext.class));

        // load self, class, and name
        mv.aloadMany(0, 0);
        mv.invokeinterface(p(IRubyObject.class), "getMetaClass", sig(RubyClass.class));
        mv.ldc(simpleName);

        // coerce arguments
        coerceArgumentsToRuby(mv, paramTypes, rubyIndex);

        // load null block
        mv.getstatic(p(Block.class), "NULL_BLOCK", ci(Block.class));

        // invoke method
        mv.line(13);
        mv.invokevirtual(p(DynamicMethod.class), "call", sig(IRubyObject.class, ThreadContext.class, IRubyObject.class, RubyModule.class, String.class, IRubyObject[].class, Block.class));

        coerceResultAndReturn(mv, returnType);
    }

    private static void defineRealBodyWithFallback(SkinnyMethodAdapter mv, final String pathName,
        final String simpleName, final Class[] paramTypes, final Class returnType,
        final int baseIndex, final int cacheIndex, final Set<String> nameSet) {

        final int rubyIndex = baseIndex + 1;

        //mv.line(5);

        // prepare temp locals
        mv.aload(0);
        mv.invokeinterface(p(IRubyObject.class), "getRuntime", sig(Ruby.class));
        mv.astore(rubyIndex);

        // get method from cache
        mv.getstatic(pathName, "$runtimeCache", ci(RuntimeCache.class));
        mv.aload(0);
        mv.ldc(cacheIndex);
        for (String eachName : nameSet) {
            mv.ldc(eachName);
        }
        mv.invokevirtual(p(RuntimeCache.class), "searchWithCacheNoMethodMissing",
                sig(DynamicMethod.class, params(IRubyObject.class, int.class, String.class, nameSet.size())));
        final int methodIndex = baseIndex + 2;
        mv.astore(methodIndex);

        Label fallback = new Label();
        mv.aload(methodIndex);
        mv.ifnull(fallback);

        mv.aload(methodIndex); // method (!= null)

        // get current context
        mv.aload(rubyIndex);
        mv.invokevirtual(p(Ruby.class), "getCurrentContext", sig(ThreadContext.class));

        // load self, class, and name
        mv.aloadMany(0, 0);
        mv.invokeinterface(p(IRubyObject.class), "getMetaClass", sig(RubyClass.class));
        mv.ldc(simpleName);

        // coerce arguments
        coerceArgumentsToRuby(mv, paramTypes, rubyIndex);

        // load null block
        mv.getstatic(p(Block.class), "NULL_BLOCK", ci(Block.class));

        // invoke method
        //mv.line(13);
        mv.invokevirtual(p(DynamicMethod.class), "call", sig(IRubyObject.class, ThreadContext.class, IRubyObject.class, RubyModule.class, String.class, IRubyObject[].class, Block.class));

        coerceResultAndReturn(mv, returnType);

        // fallback (default) impl :
        mv.label(fallback);
        switch ( simpleName ) {
            case "equals" : objectEquals(-1, mv); break;
            case "hashCode" : objectHashCode(-1, mv); break;
            case "toString" : objectToString(-1, mv); break;
            default : throw new UnsupportedOperationException(simpleName);
        }
    }

    private static void defineRealEqualsWithFallback(SkinnyMethodAdapter mv, final String pathName,
        final String simpleName, final Class[] paramTypes, final Class returnType,
        final int baseIndex, final int cacheIndex, final Set<String> nameSet) {
        defineRealBodyWithFallback(mv, pathName, simpleName, paramTypes, returnType, baseIndex, cacheIndex, nameSet);
    }

    private static void defineRealHashCodeWithFallback(SkinnyMethodAdapter mv, final String pathName,
        final String simpleName, final Class[] paramTypes, final Class returnType,
        final int baseIndex, final int cacheIndex, final Set<String> nameSet) {
        defineRealBodyWithFallback(mv, pathName, simpleName, paramTypes, returnType, baseIndex, cacheIndex, nameSet);
    }

    private static void defineRealToStringWithFallback(SkinnyMethodAdapter mv, final String pathName,
        final String simpleName, final Class[] paramTypes, final Class returnType,
        final int baseIndex, final int cacheIndex, final Set<String> nameSet) {
        defineRealBodyWithFallback(mv, pathName, simpleName, paramTypes, returnType, baseIndex, cacheIndex, nameSet);
    }

    private static boolean defineDefaultEquals(final int line, SkinnyMethodAdapter mv,
        final Class[] paramTypes, final Class returnType) {

        if ( paramTypes.length == 1 && paramTypes[0] == Object.class && returnType == Boolean.TYPE ) {
            objectEquals(line, mv);
            return true;
        }
        return false;
    }

    private static void objectEquals(final int line, SkinnyMethodAdapter mv) {
        if ( line > 0 ) mv.line(line);
        mv.aload(0);
        mv.aload(1);
        mv.invokespecial(p(Object.class), "equals", sig(Boolean.TYPE, params(Object.class)));
        mv.ireturn();
    }

    private static boolean defineDefaultHashCode(final int line, SkinnyMethodAdapter mv,
        final Class[] paramTypes, final Class returnType) {

        if ( paramTypes.length == 0 && returnType == Integer.TYPE ) {
            objectHashCode(line, mv);
            return true;
        }
        return false;
    }

    private static void objectHashCode(final int line, SkinnyMethodAdapter mv) {
        if ( line > 0 ) mv.line(line);
        mv.aload(0);
        mv.invokespecial(p(Object.class), "hashCode", sig(Integer.TYPE));
        mv.ireturn();
    }

    private static boolean defineDefaultToString(final int line, SkinnyMethodAdapter mv,
        final Class[] paramTypes, final Class returnType) {

        if ( paramTypes.length == 0 && returnType == String.class ) {
            objectToString(line, mv);
            return true;
        }
        return false;
    }

    private static void objectToString(final int line, SkinnyMethodAdapter mv) {
        if ( line > 0 ) mv.line(line);
        mv.aload(0);
        mv.invokespecial(p(Object.class), "toString", sig(String.class));
        mv.areturn();
    }

    private static void writeClassFile(final String name, final byte[] bytecode) {
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(name + ".class");
            fos.write(bytecode);
        }
        catch (IOException ex) { ex.printStackTrace(); }
        finally {
            try { if ( fos != null ) fos.close(); } catch (Exception e) {}
        }
    }

    public static void coerceArgumentsToRuby(SkinnyMethodAdapter mv, Class[] paramTypes, int rubyIndex) {
        // load arguments into IRubyObject[] for dispatch
        if (paramTypes.length != 0) {
            mv.pushInt(paramTypes.length);
            mv.anewarray(p(IRubyObject.class));

            // TODO: make this do specific-arity calling
            for (int i = 0, argIndex = 1; i < paramTypes.length; i++) {
                Class paramType = paramTypes[i];
                mv.dup();
                mv.pushInt(i);
                // convert to IRubyObject
                if (paramTypes[i].isPrimitive()) {
                    mv.aload(rubyIndex);
                    if (paramType == byte.class || paramType == short.class || paramType == char.class || paramType == int.class) {
                        mv.iload(argIndex++);
                        mv.invokestatic(p(JavaUtil.class), "convertJavaToRuby", sig(IRubyObject.class, Ruby.class, int.class));
                    } else if (paramType == long.class) {
                        mv.lload(argIndex);
                        argIndex += 2; // up two slots, for long's two halves
                        mv.invokestatic(p(JavaUtil.class), "convertJavaToRuby", sig(IRubyObject.class, Ruby.class, long.class));
                    } else if (paramType == float.class) {
                        mv.fload(argIndex++);
                        mv.invokestatic(p(JavaUtil.class), "convertJavaToRuby", sig(IRubyObject.class, Ruby.class, float.class));
                    } else if (paramType == double.class) {
                        mv.dload(argIndex);
                        argIndex += 2; // up two slots, for long's two halves
                        mv.invokestatic(p(JavaUtil.class), "convertJavaToRuby", sig(IRubyObject.class, Ruby.class, double.class));
                    } else if (paramType == boolean.class) {
                        mv.iload(argIndex++);
                        mv.invokestatic(p(JavaUtil.class), "convertJavaToRuby", sig(IRubyObject.class, Ruby.class, boolean.class));
                    }
                } else if (!IRubyObject.class.isAssignableFrom(paramType)) {
                    mv.aload(rubyIndex);
                    mv.aload(argIndex++);
                    mv.invokestatic(p(JavaUtil.class), "convertJavaToUsableRubyObject", sig(IRubyObject.class, Ruby.class, Object.class));
                } else {
                    mv.aload(argIndex++);
                }
                mv.aastore();
            }
        } else {
            mv.getstatic(p(IRubyObject.class), "NULL_ARRAY", ci(IRubyObject[].class));
        }
    }

    public static void coerceResultAndReturn(SkinnyMethodAdapter mv, Class returnType) {
        // if we expect a return value, unwrap it
        if (returnType != void.class) {
            // TODO: move the bulk of this logic to utility methods
            if (returnType.isPrimitive()) {
                if (returnType == boolean.class) {
                    mv.getstatic(p(Boolean.class), "TYPE", ci(Class.class));
                    mv.invokeinterface(p(IRubyObject.class), "toJava", sig(Object.class, Class.class));
                    mv.checkcast(p(Boolean.class));
                    mv.invokevirtual(p(Boolean.class), "booleanValue", sig(boolean.class));
                    mv.ireturn();
                } else {
                    mv.getstatic(p(getBoxType(returnType)), "TYPE", ci(Class.class));
                    mv.invokeinterface(p(IRubyObject.class), "toJava", sig(Object.class, Class.class));
                    if (returnType == byte.class) {
                        mv.checkcast(p(Number.class));
                        mv.invokevirtual(p(Number.class), "byteValue", sig(byte.class));
                        mv.ireturn();
                    } else if (returnType == short.class) {
                        mv.checkcast(p(Number.class));
                        mv.invokevirtual(p(Number.class), "shortValue", sig(short.class));
                        mv.ireturn();
                    } else if (returnType == char.class) {
                        mv.checkcast(p(Character.class));
                        mv.invokevirtual(p(Character.class), "charValue", sig(char.class));
                        mv.ireturn();
                    } else if (returnType == int.class) {
                        mv.checkcast(p(Number.class));
                        mv.invokevirtual(p(Number.class), "intValue", sig(int.class));
                        mv.ireturn();
                    } else if (returnType == long.class) {
                        mv.checkcast(p(Number.class));
                        mv.invokevirtual(p(Number.class), "longValue", sig(long.class));
                        mv.lreturn();
                    } else if (returnType == float.class) {
                        mv.checkcast(p(Number.class));
                        mv.invokevirtual(p(Number.class), "floatValue", sig(float.class));
                        mv.freturn();
                    } else if (returnType == double.class) {
                        mv.checkcast(p(Number.class));
                        mv.invokevirtual(p(Number.class), "doubleValue", sig(double.class));
                        mv.dreturn();
                    }
                }
            } else {
                if (!IRubyObject.class.isAssignableFrom(returnType)) {
                    mv.ldc(Type.getType(returnType));
                    mv.invokeinterface(
                        p(IRubyObject.class), "toJava", sig(Object.class, Class.class));
                    mv.checkcast(p(returnType));
                }
                mv.areturn();
            }
        } else {
            mv.voidreturn();
        }
    }

    public static int calcBaseIndex(final Class[] params, int baseIndex) {
        for (Class paramType : params) {
            if (paramType == double.class || paramType == long.class) {
                baseIndex += 2;
            } else {
                baseIndex += 1;
            }
        }
        return baseIndex;
    }

}
