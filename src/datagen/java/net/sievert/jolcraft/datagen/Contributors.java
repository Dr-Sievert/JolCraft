package net.sievert.jolcraft.datagen;

import java.lang.annotation.ElementType;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import net.neoforged.fml.ModList;
import net.neoforged.neoforgespi.language.ModFileScanData;

import net.sievert.jolcraft.module.primitive.log.Log;

/** Every {@link Contributes} class of the datagen mod, found through the class scan and instantiated once. */
public final class Contributors {

    private static final String MODULE_ROOT = Contributors.class.getPackageName() + ".module.";
    private static final Set<String> TIERS = Set.of("primitive", "composite", "system");

    private final List<Object> all;

    private Contributors(List<Object> all) {
        this.all = all;
    }

    public static Contributors discover(String modId, Log log) {
        ModFileScanData scan = ModList.get().getModFileById(modId).getFile().getScanResult();
        List<Object> found = new ArrayList<>();
        scan.getAnnotatedBy(Contributes.class, ElementType.TYPE).forEach(data -> {
            String className = data.clazz().getClassName();
            try {
                Class<?> type = Class.forName(className, true, Contributors.class.getClassLoader());
                checkPlace(type);
                found.add(type.getDeclaredConstructor().newInstance());
            } catch (InvocationTargetException e) {
                throw new IllegalStateException("contributor " + className + " failed in its constructor", e.getCause());
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException("contributor " + className + " needs a public no-argument constructor", e);
            }
        });
        found.sort(Comparator.comparing(contributor -> contributor.getClass().getName()));
        log.info("{} contributors: {}", found.size(), found.stream().map(contributor -> contributor.getClass().getSimpleName()).toList());
        return new Contributors(found);
    }

    /** The contributors implementing {@code kind}, in a stable order. */
    public <T> List<T> of(Class<T> kind) {
        return all.stream().filter(kind::isInstance).map(kind::cast).toList();
    }

    /** {@code datagen.module.<tier>.<folder>}: a real tier, and the folder the annotation names. */
    private static void checkPlace(Class<?> type) {
        String folder = type.getAnnotation(Contributes.class).value();
        String packageName = type.getPackageName();
        if (packageName.startsWith(MODULE_ROOT)) {
            String[] below = packageName.substring(MODULE_ROOT.length()).split("[.]");
            if (below.length >= 2 && TIERS.contains(below[0]) && below[1].equals(folder)) {
                return;
            }
        }
        throw new IllegalStateException(type.getName() + " contributes to the module folder \"" + folder + "\" but does not sit in "
                + MODULE_ROOT + "<primitive|composite|system>." + folder);
    }
}
