/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Codename One through http://www.codenameone.com/ if you
 * need additional information or have any questions.
 */
package com.codename1.maven.processors;

import com.codename1.maven.annotations.ProcessorContext;
import org.objectweb.asm.*;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.*;
import java.util.zip.*;

/** Enhances entity state and field access without reflection or runtime proxies. */
final class OrmEnhancer {
    private static final String MANAGED="com/codename1/impl/orm/ManagedEntity";
    private static final String STATE="com/codename1/impl/orm/EntityState";
    private static final String STATE_DESC="L"+STATE+";";
    private static final String MANIFEST="META-INF/cn1/orm-enhanced-dependencies.list";
    /// The raw accessors for non-public persistent fields. Unlike the
    /// association bridges they do nothing but read or write the field: the
    /// generated model is their only caller and it needs the stored value.
    static final String GET="__cn1OrmGet_",SET="__cn1OrmSet_";
    private OrmEnhancer() {}

    /// The non-public persistent fields of every entity, by declaring class:
    /// internal name -> field name -> descriptor.
    static Map<String,Map<String,String>> hiddenFields(Map<String,OrmAnnotationProcessor.EntityClass> entities) {
        Map<String,Map<String,String>> result=new HashMap<String,Map<String,String>>();
        for(OrmAnnotationProcessor.EntityClass entity:entities.values()) for(String[] field:entity.hiddenFields) {
            Map<String,String> fields=result.get(field[0]);
            if(fields==null) { fields=new HashMap<String,String>();result.put(field[0],fields); }
            fields.put(field[1],field[2]);
        }
        return result;
    }

    /// The classes the processor generates for its entities, by internal name.
    /// They are the only callers of the raw accessors, and the only classes
    /// whose own field access is never redirected through association bridges.
    static Set<String> generatedClasses(Map<String,OrmAnnotationProcessor.EntityClass> entities) {
        Set<String> result=new HashSet<String>();
        for(OrmAnnotationProcessor.EntityClass entity:entities.values()) {
            String name=entity.binaryName.replace('.','/');
            result.add(name+"Cn1Model");result.add(name+"Cn1BackendModel");
            result.add(name+"Cn1Dao");result.add(name+"Cn1BackendDao");
            if(entity.daoBinaryName!=null) result.add(entity.daoBinaryName.replace('.','/'));
        }
        return result;
    }

    /// Writes a copy of every class that declares a non-public persistent field,
    /// with those fields made public, into a fresh temporary directory, for the
    /// generated sources to compile against. Answers null when no entity has
    /// such a field. The caller deletes the directory.
    ///
    /// Only javac sees these copies. The bytecode it emits is the same GETFIELD
    /// and PUTFIELD it would emit for a public field, and [#enhance] then
    /// rewrites each one into a call to an accessor the real class gets.
    static File publicStubs(Map<String,OrmAnnotationProcessor.EntityClass> entities,ProcessorContext ctx) throws IOException {
        final Map<String,Map<String,String>> hidden=hiddenFields(entities);
        if(hidden.isEmpty()) return null;
        File stubs=Files.createTempDirectory("cn1-orm-stubs").toFile();
        boolean done=false;
        try {
            for(final Map.Entry<String,Map<String,String>> owner:hidden.entrySet()) {
                byte[] original=classBytes(owner.getKey(),ctx);
                if(original==null) throw new IOException("Cannot find the class file of "+owner.getKey().replace('/','.'));
                ClassWriter writer=new ClassWriter(0);
                new ClassReader(original).accept(new ClassVisitor(Opcodes.ASM9,writer) {
                    @Override public FieldVisitor visitField(int access,String name,String descriptor,String signature,Object value) {
                        if(owner.getValue().containsKey(name) && (access&Opcodes.ACC_STATIC)==0)
                            access=(access&~(Opcodes.ACC_PRIVATE|Opcodes.ACC_PROTECTED))|Opcodes.ACC_PUBLIC;
                        return super.visitField(access,name,descriptor,signature,value);
                    }
                },0);
                File out=new File(stubs,owner.getKey()+".class");
                Files.createDirectories(out.getParentFile().toPath());
                Files.write(out.toPath(),writer.toByteArray());
            }
            done=true;
            return stubs;
        } finally {
            if(!done) org.apache.commons.io.FileUtils.deleteDirectory(stubs);
        }
    }

    /// The current bytes of a class: the output directory first, as javac and
    /// the runtime would find it, then the compile classpath.
    private static byte[] classBytes(String internalName,ProcessorContext ctx) throws IOException {
        String name=internalName+".class";
        File own=new File(ctx.getOutputClassDir(),name);
        if(own.isFile()) return Files.readAllBytes(own.toPath());
        for(String path:ctx.getCompileClasspath()) {
            File file=new File(path);
            if(file.isDirectory()) {
                File candidate=new File(file,name);
                if(candidate.isFile()) return Files.readAllBytes(candidate.toPath());
            } else if(file.isFile() && file.getName().endsWith(".jar")) {
                ZipFile zip=new ZipFile(file);
                try {
                    ZipEntry entry=zip.getEntry(name);
                    if(entry!=null) {
                        InputStream in=zip.getInputStream(entry);
                        try { return read(in); } finally { in.close(); }
                    }
                } finally { zip.close(); }
            }
        }
        return null;
    }
    static Set<String> prepare(ProcessorContext ctx) throws IOException {
        Path manifest=outputPath(ctx.getOutputClassDir(),MANIFEST);
        Set<String> removed=new HashSet<String>();
        if(!Files.isRegularFile(manifest)) return removed;
        for(Map.Entry<String,String> entry:readManifest(manifest.toFile()).entrySet()) {
            String name=entry.getKey();Path output=outputPath(ctx.getOutputClassDir(),name);
            // A compiler may have replaced an old dependency overlay with an app class.
            // Only remove the exact bytes this enhancer previously copied.
            if(Files.isRegularFile(output) && digest(Files.readAllBytes(output)).equals(entry.getValue())) {
                Files.delete(output);
                removed.add(name.substring(0,name.length()-6).replace('/','.'));
            }
        }
        Files.delete(manifest);return removed;
    }
    static void enhance(Map<String,OrmAnnotationProcessor.EntityClass> entities,ProcessorContext ctx) throws IOException {
        final Map<String,OrmAnnotationProcessor.EntityClass> owners=new HashMap<String,OrmAnnotationProcessor.EntityClass>();
        for(OrmAnnotationProcessor.EntityClass entity:entities.values()) if(needsState(entity)) owners.put(entity.binaryName.replace('.','/'),entity);
        // Accessors declared by mapped superclasses must use the same state as their entities.
        for(OrmAnnotationProcessor.EntityClass entity:entities.values()) for(OrmAnnotationProcessor.RelationField relation:entity.relations) {
            String declaring=relation.declaringType.replace('.','/');
            if(entities.containsKey(relation.declaringType)) continue;
            OrmAnnotationProcessor.EntityClass owner=owners.get(declaring);
            if(owner==null) { owner=new OrmAnnotationProcessor.EntityClass();owner.binaryName=relation.declaringType;owners.put(declaring,owner); }
            boolean found=false;
            for(OrmAnnotationProcessor.RelationField existing:owner.relations) if(existing.field.equals(relation.field)) {
                if(existing.index!=relation.index) throw new IOException("Inherited association has incompatible state layouts: "+relation.declaringType+"."+relation.field);
                found=true;
            }
            if(!found) owner.relations.add(relation);
        }
        final Map<String,Map<String,String>> hidden=hiddenFields(entities);
        final Set<String> generated=generatedClasses(entities);
        Path manifest=outputPath(ctx.getOutputClassDir(),MANIFEST);
        Map<String,byte[]> classes=new LinkedHashMap<String,byte[]>();
        collect(ctx.getOutputClassDir(),ctx.getOutputClassDir(),classes);
        Set<String> own=new HashSet<String>(classes.keySet()),copied=new TreeSet<String>();
        if(Files.isRegularFile(manifest)) copied.addAll(readManifest(manifest.toFile()).keySet());
        own.removeAll(copied);
        // The classpath too when an entity there needs weaving: an entity shared
        // by the client and the server lives in a module both depend on, and the
        // woven copy is written over it into this module's output.
        if(!owners.isEmpty() || !hidden.isEmpty()) for(String path:ctx.getCompileClasspath()) {
            File file=new File(path);
            if(file.isDirectory()) collect(file,file,classes);
            else if(file.isFile() && file.getName().endsWith(".jar")) {
                ZipFile zip=new ZipFile(file);
                try {
                    Enumeration<? extends ZipEntry> entries=zip.entries();
                    while(entries.hasMoreElements()) {
                        ZipEntry entry=entries.nextElement();String name=entry.getName();
                        if(!candidate(name) || classes.containsKey(name)) continue;
                        InputStream in=zip.getInputStream(entry);
                        try { classes.put(name,read(in)); } finally { in.close(); }
                    }
                } finally { zip.close(); }
            }
        }
        final Map<String,byte[]> all=classes;
        for(Map.Entry<String,byte[]> entry:classes.entrySet()) {
            String actual=new ClassReader(entry.getValue()).getClassName()+".class";
            if(!actual.equals(entry.getKey()) || actual.contains("..") || actual.startsWith("/")) throw new IOException("Invalid class entry: "+entry.getKey());
            byte[] result=transform(entry.getValue(),owners,hidden,generated,new Resolver(all,hidden));
            if(result!=null) {
                Path output=outputPath(ctx.getOutputClassDir(),entry.getKey());
                Files.createDirectories(output.getParent());Files.write(output,result);
                if(!own.contains(entry.getKey())) copied.add(entry.getKey());
            }
        }
        Files.createDirectories(manifest.getParent());
        List<String> records=new ArrayList<String>();
        for(String name:copied) records.add(name+"\t"+digest(Files.readAllBytes(outputPath(ctx.getOutputClassDir(),name))));
        Files.write(manifest,records,java.nio.charset.StandardCharsets.UTF_8);
    }
    static boolean needsState(OrmAnnotationProcessor.EntityClass entity) {
        // Match generated EntityModel.requiresSession(), including mappings
        // that have managed values but no association accessors to weave.
        if(!entity.relations.isEmpty() || !entity.embedded.isEmpty() || !entity.indexes.isEmpty()
                || entity.generation!=0 || entity.idFields.size()>1 || entity.hierarchyRoot!=null) return true;
        for(OrmAnnotationProcessor.PersistedField field:entity.fields)
            if(field.version || field.unique || field.converter!=null) return true;
        for(String callback:entity.callbacks) if(callback!=null) return true;
        return false;
    }
    private static Path outputPath(File directory,String name) throws IOException {
        // Reject non-portable archive paths even when building on a different OS.
        if(name.length()==0 || name.contains("..") || name.startsWith("/")
                || name.indexOf('\\')>=0 || name.indexOf(':')>=0) {
            throw new IOException("Invalid ORM enhancement path: "+name);
        }
        // Canonicalization also resolves existing symlinks. Use Path.startsWith,
        // not a string prefix, so a sibling such as classes-escape is excluded.
        Path root=realPath(directory.toPath());
        Path output=realPath(root.resolve(name));
        if(!output.startsWith(root) || output.equals(root)) {
            throw new IOException("ORM enhancement path escapes output directory: "+name);
        }
        return output;
    }
    private static Path realPath(Path path) throws IOException {
        Path absolute=path.toAbsolutePath().normalize();
        Path existing=absolute;
        // Output files and their parent directories may not exist yet. Resolve
        // the nearest existing ancestor, including any symlinks, then append
        // the missing suffix. NIO avoids File's stale canonical-path cache.
        while(!Files.exists(existing,LinkOption.NOFOLLOW_LINKS)) existing=existing.getParent();
        return existing.toRealPath().resolve(existing.relativize(absolute)).normalize();
    }
    private static Map<String,String> readManifest(File manifest) throws IOException {
        Map<String,String> entries=new LinkedHashMap<String,String>();
        for(String line:Files.readAllLines(manifest.toPath(),java.nio.charset.StandardCharsets.UTF_8)) {
            int separator=line.indexOf('\t');
            if(separator<0) throw new IOException("Invalid ORM enhancement manifest");
            String name=line.substring(0,separator),hash=line.substring(separator+1);
            if(!candidate(name) || name.contains("..") || name.startsWith("/") || name.indexOf('\\')>=0 || !hash.matches("[0-9a-f]{64}")) throw new IOException("Invalid ORM enhancement manifest");
            entries.put(name,hash);
        }
        return entries;
    }
    private static String digest(byte[] bytes) throws IOException {
        try {
            byte[] hash=java.security.MessageDigest.getInstance("SHA-256").digest(bytes);
            StringBuilder value=new StringBuilder();
            for(byte b:hash) { value.append(Character.forDigit((b>>>4)&15,16));value.append(Character.forDigit(b&15,16)); }
            return value.toString();
        } catch(java.security.NoSuchAlgorithmException error) { throw new IOException("SHA-256 is unavailable",error); }
    }
    private static boolean candidate(String path) {
        return path.endsWith(".class") && !path.startsWith("java/") && !path.startsWith("javax/")
            && !path.startsWith("META-INF/");
    }
    private static void collect(File root,File directory,Map<String,byte[]> classes) throws IOException {
        File[] files=directory.listFiles();if(files==null) return;
        for(File file:files) {
            if(file.isDirectory()) collect(root,file,classes);
            else {
                String name=root.toPath().relativize(file.toPath()).toString().replace(File.separatorChar,'/');
                if(candidate(name) && !classes.containsKey(name)) classes.put(name,Files.readAllBytes(file.toPath()));
            }
        }
    }
    private static byte[] read(InputStream in) throws IOException {
        ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] buffer=new byte[8192];int count;
        while((count=in.read(buffer))!=-1) out.write(buffer,0,count);return out.toByteArray();
    }
    static byte[] transform(byte[] original,final Map<String,OrmAnnotationProcessor.EntityClass> owners) {
        Map<String,Map<String,String>> none=Collections.emptyMap();
        Set<String> generated=new HashSet<String>();
        for(String owner:owners.keySet()) {
            generated.add(owner+"Cn1Model");generated.add(owner+"Cn1BackendModel");
            generated.add(owner+"Cn1Dao");generated.add(owner+"Cn1BackendDao");
        }
        return transform(original,owners,none,generated,new Resolver(Collections.<String,byte[]>emptyMap(),none));
    }

    static byte[] transform(byte[] original,final Map<String,OrmAnnotationProcessor.EntityClass> owners,
                            final Map<String,Map<String,String>> hidden,Set<String> generatedClasses,final Resolver resolver) {
        ClassReader reader=new ClassReader(original);
        final String className=reader.getClassName();
        if(generatedClasses.contains(className)) return redirectHidden(reader,resolver);
        final boolean serializer=className.endsWith("Cn1Mapper");
        final OrmAnnotationProcessor.EntityClass entity=owners.get(className);
        final Map<String,String> accessors=hidden.get(className);
        final boolean[] changed={false},already={false};
        final ClassWriter writer=new ClassWriter(reader,ClassWriter.COMPUTE_MAXS);
        reader.accept(new ClassVisitor(Opcodes.ASM9,writer) {
            @Override public void visit(int version,int access,String name,String signature,String superclass,String[] interfaces) {
                List<String> list=new ArrayList<String>(Arrays.asList(interfaces));already[0]=list.contains(MANAGED);
                if(entity!=null && !already[0]) { list.add(MANAGED);changed[0]=true; }
                super.visit(version,access,name,signature,superclass,list.toArray(new String[0]));
            }
            @Override public MethodVisitor visitMethod(int access,String name,String descriptor,String signature,String[] exceptions) {
                if(entity!=null && (name.startsWith("__cn1OrmRead_") || name.startsWith("__cn1OrmWrite_") || name.startsWith("__cn1OrmSerialize_"))) { changed[0]=true;return null; }
                // Raw accessors from an earlier build are always dropped and,
                // for the fields still hidden, written again below: a field that
                // has since become public, transient or gone must not keep one.
                if(name.startsWith(GET) || name.startsWith(SET)) { changed[0]=true;return null; }
                MethodVisitor method=super.visitMethod(access,name,descriptor,signature,exceptions);
                if(name.startsWith("__cn1Orm")) return method;
                return new MethodVisitor(Opcodes.ASM9,method) {
                    @Override public void visitMethodInsn(int opcode,String owner,String name,String descriptor,boolean isInterface) {
                        boolean write=name.startsWith("__cn1OrmWrite_");
                        boolean read=name.startsWith("__cn1OrmRead_") || name.startsWith("__cn1OrmSerialize_");
                        if(opcode==Opcodes.INVOKESTATIC && !isInterface && (read || write)) {
                            Type[] args=Type.getArgumentTypes(descriptor);Type result=Type.getReturnType(descriptor);
                            if(args.length==(write?2:1) && args[0].getDescriptor().equals("L"+owner+";")
                                    && (write?result.getSort()==Type.VOID:result.getSort()==Type.OBJECT || result.getSort()==Type.ARRAY)) {
                                // Recover the field operation, then apply current metadata.
                                // This also removes stale calls when the last relation is gone.
                                String prefix=write?"__cn1OrmWrite_":name.startsWith("__cn1OrmRead_")?"__cn1OrmRead_":"__cn1OrmSerialize_";
                                String field=name.substring(prefix.length());
                                if(field.length()>0) {
                                    visitFieldInsn(write?Opcodes.PUTFIELD:Opcodes.GETFIELD,owner,field,write?args[1].getDescriptor():result.getDescriptor());
                                    changed[0]=true;return;
                                }
                            }
                        }
                        super.visitMethodInsn(opcode,owner,name,descriptor,isInterface);
                    }
                    @Override public void visitFieldInsn(int opcode,String owner,String field,String desc) {
                        OrmAnnotationProcessor.EntityClass target=owners.get(owner);
                        if(target!=null && (opcode==Opcodes.GETFIELD || opcode==Opcodes.PUTFIELD)) {
                            for(OrmAnnotationProcessor.RelationField relation:target.relations) if(relation.field.equals(field)) {
                                String sig=opcode==Opcodes.GETFIELD?"(L"+owner+";)"+desc:"(L"+owner+";"+desc+")V";
                                super.visitMethodInsn(Opcodes.INVOKESTATIC,owner,"__cn1Orm"+(opcode==Opcodes.GETFIELD?(serializer?"Serialize_":"Read_"):"Write_")+field,sig,false);
                                changed[0]=true;return;
                            }
                        }
                        super.visitFieldInsn(opcode,owner,field,desc);
                    }
                };
            }
            @Override public void visitEnd() {
                if(entity!=null && !already[0]) {
                    writer.visitField(Opcodes.ACC_PRIVATE|Opcodes.ACC_TRANSIENT,"__cn1OrmState",STATE_DESC,null,null).visitEnd();
                    MethodVisitor get=writer.visitMethod(Opcodes.ACC_PUBLIC,"__cn1OrmState","()"+STATE_DESC,null,null);
                    get.visitCode();get.visitVarInsn(Opcodes.ALOAD,0);get.visitFieldInsn(Opcodes.GETFIELD,className,"__cn1OrmState",STATE_DESC);get.visitInsn(Opcodes.ARETURN);get.visitMaxs(0,0);get.visitEnd();
                    MethodVisitor set=writer.visitMethod(Opcodes.ACC_PUBLIC,"__cn1OrmState","("+STATE_DESC+")V",null,null);
                    set.visitCode();set.visitVarInsn(Opcodes.ALOAD,0);set.visitVarInsn(Opcodes.ALOAD,1);set.visitFieldInsn(Opcodes.PUTFIELD,className,"__cn1OrmState",STATE_DESC);set.visitInsn(Opcodes.RETURN);set.visitMaxs(0,0);set.visitEnd();
                }
                if(entity!=null) {
                    changed[0]=true;
                    for(OrmAnnotationProcessor.RelationField relation:entity.relations) {
                        bridge(writer,className,relation,0);bridge(writer,className,relation,1);bridge(writer,className,relation,2);
                    }
                }
                if(accessors!=null) {
                    changed[0]=true;
                    for(Map.Entry<String,String> field:new TreeMap<String,String>(accessors).entrySet()) {
                        accessor(writer,className,field.getKey(),field.getValue(),false);
                        accessor(writer,className,field.getKey(),field.getValue(),true);
                    }
                }
                super.visitEnd();
            }
        },0);
        return changed[0]?writer.toByteArray():null;
    }
    /// `public static T __cn1OrmGet_f(Owner o)` or
    /// `public static void __cn1OrmSet_f(Owner o, T v)`, synthetic so that no
    /// source can call them.
    private static void accessor(ClassWriter writer,String owner,String field,String desc,boolean write) {
        Type type=Type.getType(desc);
        String signature=write?"(L"+owner+";"+desc+")V":"(L"+owner+";)"+desc;
        MethodVisitor method=writer.visitMethod(Opcodes.ACC_PUBLIC|Opcodes.ACC_STATIC|Opcodes.ACC_SYNTHETIC,
                (write?SET:GET)+field,signature,null,null);
        method.visitCode();method.visitVarInsn(Opcodes.ALOAD,0);
        if(write) method.visitVarInsn(type.getOpcode(Opcodes.ILOAD),1);
        method.visitFieldInsn(write?Opcodes.PUTFIELD:Opcodes.GETFIELD,owner,field,desc);
        method.visitInsn(write?Opcodes.RETURN:type.getOpcode(Opcodes.IRETURN));
        method.visitMaxs(0,0);method.visitEnd();
    }

    /// Rewrites a generated class's GETFIELD and PUTFIELD on a non-public
    /// persistent field into a call to that field's accessor. The stack shape
    /// is the same before and after -- the object, then the value for a write
    /// -- so nothing around the instruction changes.
    private static byte[] redirectHidden(ClassReader reader,final Resolver resolver) {
        if(resolver.isEmpty()) return null;
        final boolean[] changed={false};
        ClassWriter writer=new ClassWriter(reader,0);
        reader.accept(new ClassVisitor(Opcodes.ASM9,writer) {
            @Override public MethodVisitor visitMethod(int access,String name,String descriptor,String signature,String[] exceptions) {
                return new MethodVisitor(Opcodes.ASM9,super.visitMethod(access,name,descriptor,signature,exceptions)) {
                    @Override public void visitFieldInsn(int opcode,String owner,String field,String desc) {
                        if(opcode==Opcodes.GETFIELD || opcode==Opcodes.PUTFIELD) {
                            String declaring=resolver.declaring(owner,field);
                            if(declaring!=null) {
                                boolean write=opcode==Opcodes.PUTFIELD;
                                super.visitMethodInsn(Opcodes.INVOKESTATIC,declaring,(write?SET:GET)+field,
                                        write?"(L"+declaring+";"+desc+")V":"(L"+declaring+";)"+desc,false);
                                changed[0]=true;return;
                            }
                        }
                        super.visitFieldInsn(opcode,owner,field,desc);
                    }
                };
            }
        },0);
        return changed[0]?writer.toByteArray():null;
    }

    /// Answers which class declares the field an instruction names. javac
    /// names the static type of the expression, not the declaring class, so
    /// `e.id` on an entity whose id lives in a mapped superclass says the
    /// entity; this walks up to the class that holds the accessor.
    static final class Resolver {
        private final Map<String,byte[]> classes;
        private final Map<String,Map<String,String>> hidden;
        Resolver(Map<String,byte[]> classes,Map<String,Map<String,String>> hidden) { this.classes=classes;this.hidden=hidden; }
        boolean isEmpty() { return hidden.isEmpty(); }
        String declaring(String owner,final String field) {
            String current=owner;
            for(int depth=0;current!=null && depth<64;depth++) {
                Map<String,String> fields=hidden.get(current);
                if(fields!=null && fields.containsKey(field)) return current;
                byte[] bytes=classes.get(current+".class");
                if(bytes==null) return null;
                ClassReader reader=new ClassReader(bytes);
                final boolean[] declares={false};
                reader.accept(new ClassVisitor(Opcodes.ASM9) {
                    @Override public FieldVisitor visitField(int access,String name,String descriptor,String signature,Object value) {
                        if(name.equals(field)) declares[0]=true;
                        return null;
                    }
                },ClassReader.SKIP_CODE|ClassReader.SKIP_DEBUG|ClassReader.SKIP_FRAMES);
                // A class that declares a field of that name and is not in the
                // table holds a different, accessible field.
                if(declares[0]) return null;
                current=reader.getSuperName();
            }
            return null;
        }
    }

    private static void bridge(ClassWriter writer,String owner,OrmAnnotationProcessor.RelationField relation,int operation) {
        boolean write=operation==1;
        String desc=relation.descriptor;
        String signature=write?"(L"+owner+";"+desc+")V":"(L"+owner+";)"+desc;
        MethodVisitor method=writer.visitMethod(Opcodes.ACC_PUBLIC|Opcodes.ACC_STATIC|Opcodes.ACC_SYNTHETIC,
                "__cn1Orm"+(write?"Write_":operation==2?"Serialize_":"Read_")+relation.field,signature,null,null);
        method.visitCode();method.visitVarInsn(Opcodes.ALOAD,0);method.visitLdcInsn(relation.index);
        method.visitMethodInsn(Opcodes.INVOKESTATIC,STATE,write?"beforeWrite":operation==2?"beforeSerialization":"beforeRead","(L"+MANAGED+";I)V",false);
        method.visitVarInsn(Opcodes.ALOAD,0);
        if(write) method.visitVarInsn(Opcodes.ALOAD,1);
        method.visitFieldInsn(write?Opcodes.PUTFIELD:Opcodes.GETFIELD,owner,relation.field,desc);
        method.visitInsn(write?Opcodes.RETURN:Opcodes.ARETURN);method.visitMaxs(0,0);method.visitEnd();
    }
}
