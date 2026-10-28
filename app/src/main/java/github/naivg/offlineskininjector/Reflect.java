package github.naivg.offlineskininjector;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

/**
 * Reflection helpers for authlib classes loaded by the game.
 */
final class Reflect {

    private Reflect() {
    }

    static Class<?> findClass(String name, ClassLoader loader) throws ClassNotFoundException {
        ClassLoader cl = loader != null ? loader : Reflect.class.getClassLoader();
        return Class.forName(name, false, cl);
    }

    /** Returns the enum constant with the given name, or {@code null}. */
    static Object enumConstant(Class<?> enumClass, String name) {
        Object[] constants = enumClass.getEnumConstants();
        if (constants == null) {
            return null;
        }
        for (Object constant : constants) {
            if (name.equals(((Enum<?>) constant).name())) {
                return constant;
            }
        }
        return null;
    }

    static Constructor<?> constructor(Class<?> owner, Class<?>... params) throws NoSuchMethodException {
        return owner.getConstructor(params);
    }

    static Method method(Class<?> owner, String name, Class<?>... params) throws NoSuchMethodException {
        return owner.getMethod(name, params);
    }

    /**
     * Reads a record-style accessor ({@code value()}) or a bean getter ({@code getValue()}).
     * Returns {@code null} when neither exists.
     */
    static String readString(Object target, String... candidates) {
        for (String candidate : candidates) {
            try {
                Method m = target.getClass().getMethod(candidate);
                Object value = m.invoke(target);
                if (value != null) {
                    return String.valueOf(value);
                }
            } catch (Throwable ignored) {
                // Try the next candidate.
            }
        }
        return null;
    }
}
