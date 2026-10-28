package github.naivg.offlineskininjector;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.lang.instrument.ClassFileTransformer;
import java.security.ProtectionDomain;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Rewrites the authlib texture lookup methods so they return our own textures.
 *
 * <p>Only the concrete overrides in {@code YggdrasilMinecraftSessionService} and
 * {@code MinecraftServicesSessionService} are patched; the interface methods are abstract and
 * never called directly by the game.
 */
public final class AuthlibTransformer implements ClassFileTransformer {

    private static final Set<String> TARGETS = new HashSet<String>(Arrays.asList(
            "com/mojang/authlib/yggdrasil/YggdrasilMinecraftSessionService",
            "com/mojang/authlib/services/MinecraftServicesSessionService"));

    private static final String HOOKS = "github/naivg/offlineskininjector/Hooks";

    private static final String LEGACY_DESC = "(Lcom/mojang/authlib/GameProfile;Z)Ljava/util/Map;";
    private static final String PACKED_DESC = "(Lcom/mojang/authlib/GameProfile;)Lcom/mojang/authlib/properties/Property;";
    private static final String UNPACK_DESC = "(Lcom/mojang/authlib/properties/Property;)Lcom/mojang/authlib/minecraft/MinecraftProfileTextures;";

    @Override
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
                            ProtectionDomain protectionDomain, byte[] classfileBuffer) {
        if (className == null || !TARGETS.contains(className)) {
            return null;
        }
        try {
            return patch(classfileBuffer, loader);
        } catch (Throwable t) {
            // Never break the game: if the transform fails, run the original code.
            System.out.println("[OfflineSkinInjector] transform failed for " + className + ": " + t);
            return null;
        }
    }

    private static byte[] patch(byte[] original, final ClassLoader loader) {
        ClassReader reader = new ClassReader(original);
        // COMPUTE_FRAMES is required: the injected early return adds a branch target.
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES) {
            @Override
            protected String getCommonSuperClass(String type1, String type2) {
                try {
                    ClassLoader cl = loader != null ? loader : AuthlibTransformer.class.getClassLoader();
                    Class<?> c1 = Class.forName(type1.replace('/', '.'), false, cl);
                    Class<?> c2 = Class.forName(type2.replace('/', '.'), false, cl);
                    if (c1.isAssignableFrom(c2)) {
                        return type1;
                    }
                    if (c2.isAssignableFrom(c1)) {
                        return type2;
                    }
                    if (c1.isInterface() || c2.isInterface()) {
                        return "java/lang/Object";
                    }
                    do {
                        c1 = c1.getSuperclass();
                    } while (c1 != null && !c1.isAssignableFrom(c2));
                    return c1 == null ? "java/lang/Object" : c1.getName().replace('.', '/');
                } catch (Throwable t) {
                    return "java/lang/Object";
                }
            }
        };

        reader.accept(new org.objectweb.asm.ClassVisitor(Opcodes.ASM9, writer) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String descriptor,
                                             String signature, String[] exceptions) {
                MethodVisitor delegate = super.visitMethod(access, name, descriptor, signature, exceptions);
                if ("getTextures".equals(name) && LEGACY_DESC.equals(descriptor)) {
                    return new EarlyReturn(delegate, "shouldOverride", "legacyTextures",
                            "(Ljava/lang/Object;)Ljava/util/Map;", null);
                }
                if ("getPackedTextures".equals(name) && PACKED_DESC.equals(descriptor)) {
                    return new EarlyReturn(delegate, "shouldOverride", "packedTextures",
                            "(Ljava/lang/Object;)Ljava/lang/Object;",
                            "com/mojang/authlib/properties/Property");
                }
                if ("unpackTextures".equals(name) && UNPACK_DESC.equals(descriptor)) {
                    return new EarlyReturn(delegate, "isOurProperty", "ourTextures",
                            "(Ljava/lang/Object;)Ljava/lang/Object;",
                            "com/mojang/authlib/minecraft/MinecraftProfileTextures");
                }
                return delegate;
            }
        }, 0);
        return writer.toByteArray();
    }

    /**
     * Injects {@code if (Hooks.shouldOverride(profile)) return Hooks.legacyTextures(profile);}
     * at the very beginning of the method.
     *
     * @param guard     the {@code Hooks} predicate deciding whether to take over
     * @param hook      the {@code Hooks} method producing the replacement value
     * @param hookDesc  the descriptor of that hook method
     * @param castTo    the type the hook result has to be cast to, or {@code null} when the
     *                  hook already returns the exact type the method declares
     */
    private static final class EarlyReturn extends MethodVisitor {
        private final String guard;
        private final String hook;
        private final String hookDesc;
        private final String castTo;

        EarlyReturn(MethodVisitor delegate, String guard, String hook, String hookDesc, String castTo) {
            super(Opcodes.ASM9, delegate);
            this.guard = guard;
            this.hook = hook;
            this.hookDesc = hookDesc;
            this.castTo = castTo;
        }

        @Override
        public void visitCode() {
            super.visitCode();
            Label original = new Label();

            mv.visitVarInsn(Opcodes.ALOAD, 1);
            mv.visitMethodInsn(Opcodes.INVOKESTATIC, HOOKS, guard, "(Ljava/lang/Object;)Z", false);
            mv.visitJumpInsn(Opcodes.IFEQ, original);

            mv.visitVarInsn(Opcodes.ALOAD, 1);
            mv.visitMethodInsn(Opcodes.INVOKESTATIC, HOOKS, hook, hookDesc, false);
            if (castTo != null) {
                mv.visitTypeInsn(Opcodes.CHECKCAST, castTo);
            }
            mv.visitInsn(Opcodes.ARETURN);

            mv.visitLabel(original);
        }
    }
}
