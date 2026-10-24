package net.sievert.jolcraft.module;

import org.jetbrains.annotations.NotNull;

import java.lang.annotation.Annotation;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Finds the one tier annotation a module carries, on the class itself or on a type it extends or implements. */
final class ModuleId {

    private static final ClassValue<String> IDS = new ClassValue<>() {
        @Override
        protected String computeValue(@NotNull Class<?> type) {
            return find(type);
        }
    };

    private ModuleId() {
    }

    static String of(Class<?> type) {
        return IDS.get(type);
    }

    private static String find(Class<?> type) {
        Set<Class<?>> seen = new LinkedHashSet<>();
        Deque<Class<?>> pending = new ArrayDeque<>();
        pending.add(type);
        while (!pending.isEmpty()) {
            Class<?> next = pending.poll();
            if (!seen.add(next)) continue;
            if (next.getSuperclass() != null) pending.add(next.getSuperclass());
            pending.addAll(List.of(next.getInterfaces()));
        }
        List<Class<?>> carriers = new ArrayList<>();
        String id = null;
        for (Class<?> candidate : seen) {
            String own = idOf(candidate);
            if (own == null) continue;
            carriers.add(candidate);
            id = own;
        }
        if (carriers.isEmpty()) {
            throw new IllegalStateException(type.getName() + " carries no tier annotation and extends or implements no type that does");
        }
        if (carriers.size() > 1) {
            throw new IllegalStateException(type.getName() + " has more than one tier annotation, on " + carriers);
        }
        return id;
    }

    private static String idOf(Class<?> type) {
        for (Annotation annotation : type.getDeclaredAnnotations()) {
            if (annotation instanceof Primitive primitive) return primitive.value();
            if (annotation instanceof Composite composite) return composite.value();
            if (annotation instanceof ModSystem system) return system.value();
        }
        return null;
    }
}
