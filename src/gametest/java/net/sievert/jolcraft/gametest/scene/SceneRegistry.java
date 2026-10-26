package net.sievert.jolcraft.gametest.scene;

import java.lang.annotation.ElementType;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Rotation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforgespi.language.ModFileScanData;
import org.jetbrains.annotations.NotNull;

import net.sievert.jolcraft.module.primitive.log.Log;
import net.sievert.jolcraft.util.text.Dictionary;

/**
 * Finds every {@link Scene} method in the test mod through NeoForge's class-scan data, checks its shape and its
 * values at once, registers one test function per method in the vanilla function registry, and one test instance
 * per method when the game asks for them at {@link RegisterGameTestsEvent}. Finding no scene at all is an error:
 * the adapter's own scenes always exist, so an empty scan means discovery is broken.
 */
public final class SceneRegistry {

    private record Entry(Identifier id, Method method, Scene scene, DeferredHolder<Consumer<GameTestHelper>, @NotNull Consumer<GameTestHelper>> function) {
    }

    private static final String PACKAGE_ROOT = SceneRegistry.class.getPackageName().replaceAll("\\.scene$", "");
    private static final String MODULE_PREFIX = "module/";

    private final String namespace;
    private final Log log;
    private final List<Entry> entries = new ArrayList<>();

    public SceneRegistry(String namespace, Log log) {
        this.namespace = namespace;
        this.log = log;
    }

    public void register(IEventBus modBus) {
        DeferredRegister<Consumer<GameTestHelper>> functions = DeferredRegister.create(BuiltInRegistries.TEST_FUNCTION, namespace);
        Map<String, Method> byPath = new HashMap<>();
        for (Method method : discover()) {
            Scene scene = method.getAnnotation(Scene.class);
            String path = pathOf(method, scene);
            Method other = byPath.put(path, method);
            if (other != null) {
                throw new IllegalStateException("scenes " + describe(other) + " and " + describe(method) + " share the id " + path);
            }
            Identifier id = Identifier.fromNamespaceAndPath(namespace, path);
            var function = functions.register(path, () -> helper -> run(method, id, scene.isolated(), helper));
            entries.add(new Entry(id, method, scene, function));
        }
        if (entries.isEmpty()) {
            throw new IllegalStateException("no @Scene found in " + namespace + ": scene discovery is broken");
        }
        functions.register(modBus);
        modBus.addListener(this::registerInstances);
        log.info("{} scenes: {}", entries.size(), entries.stream().map(entry -> entry.id().getPath()).toList());
    }

    private List<Method> discover() {
        ModFileScanData scan = ModList.get().getModFileById(namespace).getFile().getScanResult();
        List<Method> methods = new ArrayList<>();
        scan.getAnnotatedBy(Scene.class, ElementType.METHOD).forEach(data -> {
            String className = data.clazz().getClassName();
            String methodName = data.memberName();
            int descriptor = methodName.indexOf('(');
            if (descriptor >= 0) {
                methodName = methodName.substring(0, descriptor);
            }
            try {
                Class<?> owner = Class.forName(className, false, SceneRegistry.class.getClassLoader());
                for (Method method : owner.getDeclaredMethods()) {
                    if (method.getName().equals(methodName) && method.isAnnotationPresent(Scene.class)) {
                        validate(method);
                        methods.add(method);
                    }
                }
            } catch (ClassNotFoundException e) {
                throw new IllegalStateException("scene class " + className + " not loadable", e);
            }
        });
        methods.sort(Comparator.comparing(method -> pathOf(method, method.getAnnotation(Scene.class))));
        return methods;
    }

    /** Everything that would otherwise surface only when the scene runs: shape, visibility, construction, values. */
    private static void validate(Method method) {
        String where = describe(method);
        Class<?> owner = method.getDeclaringClass();
        boolean shape = method.getReturnType() == void.class && method.getParameterCount() == 1 && method.getParameterTypes()[0] == TestScene.class;
        if (!shape) {
            throw new IllegalStateException(where + " must be void and take exactly one TestScene");
        }
        if (!Modifier.isPublic(method.getModifiers()) || !Modifier.isPublic(owner.getModifiers())) {
            throw new IllegalStateException(where + " and its class must be public");
        }
        if (owner.isMemberClass() && !Modifier.isStatic(owner.getModifiers())) {
            throw new IllegalStateException(where + ": a nested scene class must be static");
        }
        if (!Modifier.isStatic(method.getModifiers())) {
            if (Modifier.isAbstract(owner.getModifiers())) {
                throw new IllegalStateException(where + ": the class is abstract and the method is not static");
            }
            try {
                owner.getConstructor();
            } catch (NoSuchMethodException e) {
                throw new IllegalStateException(where + ": the class needs a public no-argument constructor, or the method must be static");
            }
        }
        Scene scene = method.getAnnotation(Scene.class);
        if (scene.maxTicks() < 1 || scene.setupTicks() < 0) {
            throw new IllegalStateException(where + ": maxTicks must be at least 1 and setupTicks at least 0");
        }
        if (Identifier.tryParse(scene.structure()) == null) {
            throw new IllegalStateException(where + ": structure \"" + scene.structure() + "\" is not a valid identifier");
        }
    }

    private void registerInstances(RegisterGameTestsEvent event) {
        Holder<TestEnvironmentDefinition<?>> shared = event.registerEnvironment(Identifier.fromNamespaceAndPath(namespace, Dictionary.DEFAULT));
        for (Entry entry : entries) {
            Scene scene = entry.scene();
            Holder<TestEnvironmentDefinition<?>> environment = scene.isolated()
                    ? event.registerEnvironment(entry.id())
                    : shared;
            TestData<Holder<TestEnvironmentDefinition<?>>> data = new TestData<>(environment, Identifier.parse(scene.structure()),
                    scene.maxTicks(), scene.setupTicks(), scene.required(), Rotation.NONE, false, 1, 1, scene.skyAccess(), 0); // one attempt: the game-test server judges the first attempt only, so retries would misreport
            event.registerTest(entry.id(), new FunctionGameTestInstance(entry.function().getKey(), data));
        }
    }

    /** Runs one scene. Anything thrown becomes a test failure with its message; an Error would otherwise take the server down. */
    private void run(Method method, Identifier id, boolean isolated, GameTestHelper helper) {
        TestScene scene = new TestScene(helper, log, id.getPath(), isolated);
        try {
            Object target = Modifier.isStatic(method.getModifiers()) ? null : method.getDeclaringClass().getConstructor().newInstance();
            method.invoke(target, scene);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException runtime) throw runtime;
            throw new IllegalStateException(cause);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("cannot run scene " + id, e);
        }
    }

    /**
     * The scene's id: its package below the gametest root with slashes (a leading {@code module/} dropped), then the
     * class and the method in snake case; or the name the annotation gives.
     */
    static String pathOf(Method method, Scene scene) {
        if (!scene.name().isEmpty()) {
            return scene.name();
        }
        String packageName = method.getDeclaringClass().getPackageName();
        String below = packageName.equals(PACKAGE_ROOT) ? "" : packageName.substring(PACKAGE_ROOT.length() + 1).replace('.', '/') + "/";
        if (below.startsWith(MODULE_PREFIX)) {
            below = below.substring(MODULE_PREFIX.length());
        }
        return below + snake(method.getDeclaringClass().getSimpleName()) + "/" + snake(method.getName());
    }

    private static String describe(Method method) {
        return "@Scene " + method.getDeclaringClass().getName() + "#" + method.getName();
    }

    private static String snake(String camel) {
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < camel.length(); i++) {
            char c = camel.charAt(i);
            if (Character.isUpperCase(c) && i > 0) {
                out.append('_');
            }
            out.append(Character.toLowerCase(c));
        }
        return out.toString().toLowerCase(Locale.ROOT);
    }
}
