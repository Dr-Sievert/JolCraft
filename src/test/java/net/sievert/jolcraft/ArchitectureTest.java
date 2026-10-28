package net.sievert.jolcraft;

import static com.tngtech.archunit.core.domain.AccessTarget.Predicates.declaredIn;
import static com.tngtech.archunit.core.domain.JavaCall.Predicates.target;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.assignableTo;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.belongToAnyOf;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAPackage;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAnyPackage;
import static com.tngtech.archunit.core.domain.properties.HasName.Predicates.name;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.Dependency;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaField;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.domain.JavaModifier;
import com.tngtech.archunit.core.domain.JavaType;
import com.tngtech.archunit.core.importer.ClassFileImporter;

import net.sievert.jolcraft.module.Composite;
import net.sievert.jolcraft.module.Module;
import net.sievert.jolcraft.module.Primitive;
import net.sievert.jolcraft.module.ModSystem;
import net.sievert.jolcraft.module.primitive.log.Log;
import net.sievert.jolcraft.module.primitive.log.ModLog;
import net.sievert.jolcraft.stereotype.Actor;
import net.sievert.jolcraft.stereotype.Bridge;
import net.sievert.jolcraft.stereotype.Content;
import net.sievert.jolcraft.stereotype.Contract;
import net.sievert.jolcraft.stereotype.Event;
import net.sievert.jolcraft.stereotype.Handle;
import net.sievert.jolcraft.stereotype.Helper;
import net.sievert.jolcraft.stereotype.Implementation;
import net.sievert.jolcraft.stereotype.Internal;
import net.sievert.jolcraft.stereotype.Keys;
import net.sievert.jolcraft.stereotype.Value;
import net.sievert.jolcraft.util.text.Dictionary;

/**
 * The rules of design/module-map.md, checked against the compiled classes. Plain JUnit over ArchUnit's class
 * model: a broken layer fails {@code test}, and so the build.
 *
 * Rules on units treat a class together with its nested classes, so nothing escapes through a nested or
 * anonymous class. A module's place is its tier and the folder directly below it, as in {@code primitive.log} or
 * {@code system.trade}; a {@code client} sub-package belongs to the folder above it. Every root privilege belongs
 * to the class {@link JolCraft}, never to its package. The test reads the class directories the build names in
 * system properties; main is checked here, the gametest and datagen source sets only by rule 10.
 */
class ArchitectureTest {

    private static final String ROOT = "net.sievert.jolcraft";
    private static final String MODULE = ROOT + ".module";
    private static final String PRIMITIVE = MODULE + ".primitive";
    private static final String COMPOSITE = MODULE + ".composite";
    private static final String SYSTEM = MODULE + ".system";
    private static final String UTIL = ROOT + ".util";
    private static final String STEREOTYPE = ROOT + ".stereotype";
    private static final String MIXIN = ROOT + ".mixin";
    private static final String GAMETEST = ROOT + ".gametest";
    private static final String DATAGEN = ROOT + ".datagen";
    private static final String SCENE_ANNOTATION = GAMETEST + ".scene.Scene";
    private static final String CONTRIBUTES_ANNOTATION = DATAGEN + ".Contributes";
    private static final String ADAPTER_SCENES = GAMETEST + ".ExampleScenes";

    private static final List<String> CLIENT_PREFIXES = List.of("net.minecraft.client.", "net.neoforged.neoforge.client.", "com.mojang.blaze3d.");
    private static final List<String> CLIENT_PACKAGES = List.of("net.minecraft.client..", "net.neoforged.neoforge.client..", "com.mojang.blaze3d..");
    private static final List<String> OPTIONAL_MOD_PACKAGES = List.of("mezz.jei..", "snownee.jade..");
    private static final List<String> GAME_PREFIXES = List.of("net.minecraft.", "net.neoforged.");
    private static final List<String> IMMUTABLE_STATIC_PREFIXES = List.of("net.minecraft.resources.", "net.minecraft.tags.",
            "net.neoforged.neoforge.registries.", "net.minecraft.core.Holder", "com.mojang.serialization.",
            "net.minecraft.world.level.block.state.properties.", "net.minecraft.world.phys.shapes.");
    private static final String EVENT_BASE = "net.neoforged.bus.api.Event";
    private static final String DEFERRED_REGISTER = "net.neoforged.neoforge.registries.DeferredRegister";
    private static final String DEFERRED_HOLDER = "net.neoforged.neoforge.registries.DeferredHolder";
    private static final String EVENT_BUS = "net.neoforged.bus.api.IEventBus";

    private static final List<Class<?>> TIERS = List.of(Primitive.class, Composite.class, ModSystem.class);
    private static final List<Class<?>> STEREOTYPES = List.of(Implementation.class, Value.class, Handle.class, Contract.class,
            Helper.class, Event.class, Bridge.class, Internal.class, Content.class, Actor.class, Keys.class);

    private static final JavaClasses CLASSES = importDirectories("jolcraft.main.classes")
            .orElseGet(() -> new ClassFileImporter().importPackages(ROOT));

    // ---------------------------------------------------------------- the import itself

    /** Every other rule iterates the imported set, so an empty or wrong import must fail here rather than pass everything. */
    @Test
    void theImportHoldsTheMod() {
        for (Class<?> expected : List.of(JolCraft.class, Log.class, ModLog.class, Dictionary.class)) {
            Assertions.assertTrue(CLASSES.contain(expected.getName()), "the imported classes should hold " + expected.getName());
        }
    }

    // ---------------------------------------------------------------- tiers

    @Test
    void rule1PrimitivesDoNotDependOnEachOther() {
        slices().matching(PRIMITIVE + ".(*)..")
                .should().notDependOnEachOther()
                .as("rule 1: a primitive may not depend on another primitive")
                .check(CLASSES);
    }

    @Test
    void rule2PrimitivesDependOnNothingJolCraftSpecific() {
        DescribedPredicate<JavaClass> jolcraftSpecific = resideInAnyPackage(ROOT + "..")
                .and(DescribedPredicate.not(resideInAnyPackage(PRIMITIVE + "..", UTIL + "..", STEREOTYPE + "..").or(resideInAPackage(MODULE))))
                .as("JolCraft-specific: the root, the composites and the systems");
        noClasses().that().resideInAPackage(PRIMITIVE + "..")
                .should().dependOnClassesThat(jolcraftSpecific)
                .as("rule 2: a primitive may not depend on anything JolCraft-specific; util, the tier types and the stereotypes are free")
                .check(CLASSES);
    }

    /** Checked on units. No unit in a composite folder depends on a unit in another composite folder or on a {@code @Composite} other than itself. */
    @Test
    void rule3CompositesDoNotDependOnOtherCompositesOrOnSystems() {
        List<String> violations = new ArrayList<>();
        for (UnitDependency dependency : unitDependencies()) {
            if (!dependency.from().getPackageName().startsWith(COMPOSITE + ".")) {
                continue;
            }
            boolean otherFolder = dependency.to().getPackageName().startsWith(COMPOSITE + ".")
                    && !placeBelow(COMPOSITE, dependency.from()).equals(placeBelow(COMPOSITE, dependency.to()));
            if (otherFolder || dependency.to().isAnnotatedWith(Composite.class)) {
                violations.add(dependency.dependency().getDescription());
            }
        }
        Assertions.assertEquals(List.of(), violations, "rule 3: a unit in a composite folder may not depend on another composite");
        noClasses().that().resideInAPackage(COMPOSITE + "..")
                .should().dependOnClassesThat().resideInAPackage(SYSTEM + "..")
                .as("rule 3: a composite may not depend on a system")
                .allowEmptyShould(true)
                .check(CLASSES);
    }

    /**
     * Checked on units. A composite or a system depends only on the {@code @Primitive} types its own constructors take, generic arguments
     * included; every other unit in its folder depends only on the primitives some module of that folder takes.
     */
    @Test
    void rule4AModuleDependsOnlyOnThePrimitivesItsConstructorTakes() {
        Map<String, Set<String>> takenByFolder = new HashMap<>();
        for (JavaClass module : modulesAbovePrimitives()) {
            takenByFolder.computeIfAbsent(placeOf(module), _ -> new HashSet<>()).addAll(constructorPrimitives(module));
        }
        List<String> violations = new ArrayList<>();
        for (UnitDependency dependency : unitDependencies()) {
            JavaClass from = dependency.from();
            if (tierOf(from) == null || !dependency.to().isAnnotatedWith(Primitive.class)) {
                continue;
            }
            Set<String> allowed = from.isAnnotatedWith(Composite.class) || from.isAnnotatedWith(ModSystem.class)
                    ? constructorPrimitives(from)
                    : takenByFolder.getOrDefault(placeOf(from), Set.of());
            if (!allowed.contains(dependency.to().getName())) {
                violations.add(dependency.dependency().getDescription());
            }
        }
        Assertions.assertEquals(List.of(), violations,
                "rule 4: a composite or a system may only depend on the primitives its constructor takes, and so may the rest of its folder");
    }

    /** The root builds every composite and system, and reaches every implementation and every content class, so nothing declared is left unwired. */
    @Test
    void rule5TheRootReachesEveryModuleImplementationAndContent() {
        Set<String> reached = new HashSet<>();
        for (JavaClass rootClass : unit(JolCraft.class.getName())) {
            rootClass.getDirectDependenciesFromSelf().forEach(dependency -> reached.add(dependency.getTargetClass().getName()));
        }
        List<String> unwired = CLASSES.stream()
                .filter(type -> type.isAnnotatedWith(Implementation.class) || type.isAnnotatedWith(Content.class)
                        || type.isAnnotatedWith(Composite.class) || type.isAnnotatedWith(ModSystem.class))
                .map(JavaClass::getName)
                .filter(name -> !reached.contains(name))
                .sorted()
                .toList();
        Assertions.assertEquals(List.of(), unwired, "rule 5: the root builds everything the mod declares");
        Set<String> registered = new HashSet<>();
        for (JavaClass rootClass : unit(JolCraft.class.getName())) {
            rootClass.getMethodCallsFromSelf().forEach(call -> {
                if (call.getName().equals("register")) registered.add(call.getTargetOwner().getName());
            });
        }
        List<String> unregistered = CLASSES.stream()
                .filter(type -> type.isAnnotatedWith(Implementation.class) || type.isAnnotatedWith(Content.class)
                        || type.isAnnotatedWith(Composite.class) || type.isAnnotatedWith(ModSystem.class))
                .filter(type -> type.getMethods().stream().anyMatch(method -> method.getName().equals("register")))
                .map(JavaClass::getName)
                .filter(name -> !registered.contains(name))
                .sorted()
                .toList();
        Assertions.assertEquals(List.of(), unregistered, "rule 5: the root calls register on everything that registers");
    }

    /**
     * No static field holds a module, no static method outside the root class returns one, and nothing but the root depends on the root. No
     * exception; a compile-time constant read from the root is invisible to ArchUnit, so nothing reads one.
     */
    @Test
    void rule5NothingReachesAModuleThroughAStaticAccessorOrALocator() {
        List<String> violations = new ArrayList<>();
        for (JavaClass type : CLASSES) {
            for (JavaField field : type.getFields()) {
                if (field.getModifiers().contains(JavaModifier.STATIC) && involvesModule(field.getType())) {
                    violations.add(field.getFullName());
                }
            }
            if (!isRoot(type)) {
                for (JavaMethod method : type.getMethods()) {
                    if (method.getModifiers().contains(JavaModifier.STATIC) && involvesModule(method.getReturnType())) {
                        violations.add(method.getFullName());
                    }
                }
            }
        }
        Assertions.assertEquals(List.of(), violations, "rule 5: no static field holds a module, and no static method outside the root returns one");
        noClasses().that(DescribedPredicate.not(belongToAnyOf(JolCraft.class)))
                .should().dependOnClassesThat().belongToAnyOf(JolCraft.class)
                .as("rule 5: the composition root is reached by nothing below it")
                .check(CLASSES);
        List<String> strangers = CLASSES.stream()
                .filter(type -> type.getPackageName().equals(ROOT) && type.isTopLevelClass() && !isRoot(type))
                .map(JavaClass::getName)
                .toList();
        Assertions.assertEquals(List.of(), strangers, "rule 5: the root package holds the composition root and nothing else, so no other class inherits its privileges");
    }

    @Test
    void rule6UtilDependsOnTheJdkAlone() {
        classes().that().resideInAPackage(UTIL + "..")
                .should().onlyDependOnClassesThat().resideInAnyPackage("java..", UTIL + "..")
                .as("rule 6: util sits below every tier and depends on the JDK alone")
                .check(CLASSES);
    }

    @Test
    void rule7TierAnnotationsSitOnModulesInTheirTier() {
        classes().that().areAnnotatedWith(Primitive.class)
                .should().resideInAPackage(PRIMITIVE + "..")
                .andShould().beInterfaces()
                .andShould().beAssignableTo(Module.class)
                .as("a @Primitive type is an interface extending Module under " + PRIMITIVE)
                .check(CLASSES);
        classes().that().areAnnotatedWith(Composite.class)
                .should().resideInAPackage(COMPOSITE + "..")
                .andShould().beAssignableTo(Module.class)
                .as("a @Composite type implements Module under " + COMPOSITE)
                .allowEmptyShould(true)
                .check(CLASSES);
        classes().that().areAnnotatedWith(ModSystem.class)
                .should().resideInAPackage(SYSTEM + "..")
                .andShould().beAssignableTo(Module.class)
                .as("a @ModSystem type implements Module under " + SYSTEM)
                .allowEmptyShould(true)
                .check(CLASSES);
    }

    /** Every folder under the primitive tier declares exactly one primitive, whose id is the folder name, sitting directly in the folder. */
    @Test
    void rule7EachPrimitiveFolderDeclaresOnePrimitiveWhoseIdIsTheFolderName() {
        Set<String> folders = CLASSES.stream()
                .filter(type -> type.getPackageName().startsWith(PRIMITIVE + "."))
                .map(type -> placeBelow(PRIMITIVE, type))
                .collect(Collectors.toSet());
        for (String folder : folders) {
            List<JavaClass> primitives = CLASSES.stream()
                    .filter(type -> type.isAnnotatedWith(Primitive.class) && placeBelow(PRIMITIVE, type).equals(folder))
                    .toList();
            Assertions.assertEquals(1, primitives.size(), PRIMITIVE + "." + folder + " should declare exactly one primitive, declares " + primitives);
            JavaClass primitive = primitives.getFirst();
            Assertions.assertEquals(folder, primitive.getAnnotationOfType(Primitive.class).value(),
                    "id of " + primitive.getName() + " should be its folder name");
            Assertions.assertEquals(PRIMITIVE + "." + folder, primitive.getPackageName(),
                    primitive.getName() + " should sit directly in its folder, not in a sub-package");
        }
    }

    /** A primitive has exactly one implementation: a concrete class carrying {@code @Implementation} in the primitive's folder. */
    @Test
    void rule7EachPrimitiveHasExactlyOneImplementation() {
        for (JavaClass primitive : CLASSES.stream().filter(type -> type.isAnnotatedWith(Primitive.class)).toList()) {
            List<JavaClass> implementations = CLASSES.stream()
                    .filter(type -> !type.isInterface() && !type.getModifiers().contains(JavaModifier.ABSTRACT) && type.isAssignableTo(primitive.getName()))
                    .toList();
            Assertions.assertEquals(1, implementations.size(), primitive.getName() + " should have exactly one implementation, has " + implementations);
            JavaClass implementation = implementations.getFirst();
            Assertions.assertTrue(implementation.isAnnotatedWith(Implementation.class), implementation.getName() + " should carry @Implementation");
            Assertions.assertEquals(folderOf(primitive), folderOf(implementation), implementation.getName() + " should sit in its primitive's folder");
        }
    }

    /** A composite sits in the folder of the primitive it is organised under; a system sits in the folder named by its id. */
    @Test
    void rule7CompositesSitInAPrimitivesFolderAndSystemsInTheirOwn() {
        Set<String> primitiveFolders = CLASSES.stream()
                .filter(type -> type.isAnnotatedWith(Primitive.class))
                .map(type -> placeBelow(PRIMITIVE, type))
                .collect(Collectors.toSet());
        for (JavaClass module : CLASSES) {
            if (module.isAnnotatedWith(Composite.class)) {
                String place = placeBelow(COMPOSITE, module);
                Assertions.assertTrue(primitiveFolders.contains(place), module.getName() + " should sit in a primitive's folder, not in " + place);
            } else if (module.isAnnotatedWith(ModSystem.class)) {
                Assertions.assertEquals(module.getAnnotationOfType(ModSystem.class).value(), placeBelow(SYSTEM, module),
                        module.getName() + " should sit in the system folder named by its id");
            }
        }
    }

    @Test
    void rule7EveryModuleIdIsOneDictionaryWordAndUniqueAcrossTheTiers() {
        Set<String> words = new HashSet<>();
        for (java.lang.reflect.Field field : Dictionary.class.getDeclaredFields()) {
            int modifiers = field.getModifiers();
            if (Modifier.isPublic(modifiers) && Modifier.isStatic(modifiers) && Modifier.isFinal(modifiers) && field.getType() == String.class) {
                try {
                    words.add((String) field.get(null));
                } catch (IllegalAccessException e) {
                    throw new AssertionError(e);
                }
            }
        }
        Map<String, List<String>> byId = new HashMap<>();
        List<String> violations = new ArrayList<>();
        for (JavaClass type : CLASSES) {
            String id = tierId(type);
            if (id == null) continue;
            byId.computeIfAbsent(id, _ -> new ArrayList<>()).add(type.getName());
            if (!words.contains(id)) {
                violations.add(type.getName() + " has id \"" + id + "\", which is no Dictionary word");
            }
        }
        byId.forEach((id, names) -> {
            if (names.size() > 1) violations.add("id \"" + id + "\" is shared by " + names);
        });
        Assertions.assertEquals(List.of(), violations);
    }

    /** Every concrete module resolves exactly one tier annotation through its type hierarchy, and none declares {@code id()} itself. */
    @Test
    void rule7EveryModuleResolvesOneIdAndDeclaresNoneItself() {
        List<String> violations = new ArrayList<>();
        for (JavaClass type : CLASSES) {
            if (!type.isAssignableTo(Module.class) || type.getName().equals(Module.class.getName())) {
                continue;
            }
            if (!type.isInterface() && !type.getModifiers().contains(JavaModifier.ABSTRACT)) {
                long carriers = Stream.concat(Stream.of(type), Stream.concat(type.getAllRawSuperclasses().stream(), type.getAllRawInterfaces().stream()))
                        .filter(candidate -> tierId(candidate) != null)
                        .count();
                if (carriers != 1) {
                    violations.add(type.getName() + " resolves " + carriers + " tier annotations through its hierarchy, needs exactly one");
                }
            }
            List<JavaClass> declaring = new ArrayList<>(List.of(type));
            declaring.addAll(type.getAllRawSuperclasses());
            for (JavaClass owner : declaring) {
                if (owner.tryGetMethod("id").isPresent()) {
                    violations.add((owner.equals(type) ? type.getName() : type.getName() + " through " + owner.getName()) + " declares id() a second time");
                }
            }
        }
        Assertions.assertEquals(List.of(), violations);
    }

    // ---------------------------------------------------------------- actors, mixins, client

    @Test
    void rule8ActorsAndMixinsDependOnNoModule() {
        DescribedPredicate<JavaClass> actorUnit = new DescribedPredicate<>("belong to an @Actor") {
            @Override
            public boolean test(JavaClass type) {
                return owner(type).isAnnotatedWith(Actor.class);
            }
        };
        noClasses().that(actorUnit.or(resideInAPackage(MIXIN + "..")))
                .should().dependOnClassesThat(assignableTo(Module.class))
                .as("rule 8: an actor or a mixin reads data and posts events; it never reaches a module")
                .allowEmptyShould(true)
                .check(CLASSES);
    }

    @Test
    void rule8NothingOutsideTheRootReachesAServerThroughAStaticAccessor() {
        noClasses().that(DescribedPredicate.not(belongToAnyOf(JolCraft.class)))
                .should().callMethodWhere(target(declaredIn("net.neoforged.neoforge.server.ServerLifecycleHooks").and(name("getCurrentServer"))))
                .orShould().callMethodWhere(target(declaredIn("net.minecraft.client.Minecraft").and(name("getSingleplayerServer"))))
                .as("rule 8: a server is reached through the level in hand, never through a static accessor")
                .check(CLASSES);
    }

    @Test
    void rule9ClientCodeSitsInClientPackages() {
        noClasses().that().resideOutsideOfPackage("..client..")
                .should().dependOnClassesThat().resideInAnyPackage(CLIENT_PACKAGES.toArray(String[]::new))
                .as("rule 9: client classes are imported only inside a package named client, so a dedicated server never loads them")
                .check(CLASSES);
        noClasses().that().resideOutsideOfPackage("..client..").and().doNotBelongToAnyOf(JolCraft.class)
                .should().dependOnClassesThat().resideInAPackage(ROOT + "..client..")
                .as("rule 9: nothing outside a client package depends on one, except the composition root calling a client bootstrap")
                .check(CLASSES);
    }

    /** What the root may reach in a client package: a {@code @Helper} bootstrap whose public signatures use only common types. */
    @Test
    void rule9TheRootReachesClientPackagesOnlyThroughBootstraps() {
        List<String> violations = new ArrayList<>();
        for (JavaClass rootClass : unit(JolCraft.class.getName())) {
            for (Dependency dependency : rootClass.getDirectDependenciesFromSelf()) {
                JavaClass target = dependency.getTargetClass();
                if (!isOurClientPackage(target.getPackageName())) {
                    continue;
                }
                if (!target.isAnnotatedWith(Helper.class)) {
                    violations.add(target.getName() + ": a client class the root reaches is a @Helper bootstrap");
                }
                for (JavaMethod method : target.getMethods()) {
                    if (!method.getModifiers().contains(JavaModifier.PUBLIC)) continue;
                    Stream.concat(Stream.of(method.getRawReturnType()), method.getRawParameterTypes().stream())
                            .filter(ArchitectureTest::isClientType)
                            .forEach(clientType -> violations.add(method.getFullName() + ": a bootstrap's public signature uses the client type " + clientType.getName()));
                }
            }
        }
        Assertions.assertEquals(List.of(), violations, "rule 9: the root reaches a client package only through a bootstrap with common-type signatures");
    }

    @Test
    void rule9OnlyABridgeTouchesAnOptionalMod() {
        noClasses().that().areNotAnnotatedWith(Bridge.class)
                .should().dependOnClassesThat().resideInAnyPackage(OPTIONAL_MOD_PACKAGES.toArray(String[]::new))
                .as("rule 9: only a @Bridge imports an optional mod's API")
                .check(CLASSES);
    }

    /** Every top-level class in a primitive or composite folder carries exactly one mark; a mark anywhere is held to its rule, over its whole unit. */
    @Test
    void rule9EveryClassKeepsTheRulesOfItsStereotype() {
        List<String> violations = new ArrayList<>();
        for (JavaClass type : CLASSES) {
            if (!type.isTopLevelClass() || type.isAnnotation() || type.getSimpleName().equals("package-info")) {
                continue;
            }
            List<String> marks = marks(type);
            String tier = tierOf(type);
            boolean restricted = type.getPackageName().startsWith(PRIMITIVE + ".") || COMPOSITE.equals(tier);
            if (restricted && marks.size() != 1) {
                violations.add(type.getName() + " should carry exactly one tier annotation or stereotype, carries " + marks);
            } else if (marks.size() > 1) {
                violations.add(type.getName() + " carries more than one mark: " + marks);
            }
            if (type.isAnnotatedWith(Implementation.class)) checkImplementation(type, violations);
            if (type.isAnnotatedWith(Value.class)) checkValue(type, violations);
            if (type.isAnnotatedWith(Handle.class)) checkHandle(type, violations);
            if (type.isAnnotatedWith(Contract.class) && !type.isInterface()) violations.add(type.getName() + ": a @Contract is an interface");
            if (type.isAnnotatedWith(Helper.class)) checkStaticOnly(type, "@Helper", violations);
            if (type.isAnnotatedWith(Event.class)) checkEvent(type, violations);
            if (type.isAnnotatedWith(Internal.class)) checkInternal(type, violations);
            if (type.isAnnotatedWith(Content.class)) checkContent(type, violations);
            if (type.isAnnotatedWith(Actor.class)) checkActor(type, violations);
            if (type.isAnnotatedWith(Keys.class)) checkKeys(type, violations);
        }
        Assertions.assertEquals(List.of(), violations, "stereotype rules");
    }

    // ---------------------------------------------------------------- scenes and contributors

    /**
     * Every class that declares a scene sits in the mirrored place of an existing module, {@code gametest.module.<tier>.<folder>}, apart
     * from the adapter's own scenes; every datagen contributor sits in {@code datagen.module.<tier>.<folder>} of an existing module.
     */
    @Test
    void rule10ScenesAndContributorsSitInAModulesPlace() {
        Optional<JavaClasses> scenes = importDirectories("jolcraft.gametest.classes");
        Assumptions.assumeTrue(scenes.isPresent(), "game-test classes not compiled; rule 10 skipped");
        Set<String> places = CLASSES.stream().map(ArchitectureTest::placeOfAnyTier).filter(place -> place != null).collect(Collectors.toSet());
        List<String> violations = new ArrayList<>();
        for (JavaClass type : scenes.get()) {
            boolean declaresScene = scenes.get().stream()
                    .filter(member -> owner(member).getName().equals(type.getName()))
                    .flatMap(member -> member.getMethods().stream())
                    .anyMatch(method -> method.isAnnotatedWith(SCENE_ANNOTATION));
            if (!type.isTopLevelClass() || !declaresScene || type.getName().equals(ADAPTER_SCENES)) {
                continue;
            }
            checkMirroredPlace(type, GAMETEST + ".module.", places, "a scene class", violations);
        }
        importDirectories("jolcraft.datagen.classes").ifPresent(contributors -> {
            for (JavaClass type : contributors) {
                if (type.isTopLevelClass() && type.isAnnotatedWith(CONTRIBUTES_ANNOTATION)) {
                    checkMirroredPlace(type, DATAGEN + ".module.", places, "a contributor", violations);
                }
            }
        });
        Assertions.assertEquals(List.of(), violations, "rule 10: scenes and contributors sit in the mirrored place of an existing module");
    }

    private static void checkMirroredPlace(JavaClass type, String base, Set<String> places, String what, List<String> violations) {
        if (!type.getPackageName().startsWith(base)) {
            violations.add(type.getName() + ": " + what + " sits under " + base + "<tier>.<folder>, not in " + type.getPackageName());
            return;
        }
        String[] below = type.getPackageName().substring(base.length()).split("[.]");
        String place = below.length < 2 ? below[0] : below[0] + "." + below[1];
        if (!places.contains(place)) {
            violations.add(type.getName() + ": " + what + " sits in " + place + ", which is no module");
        }
    }

    // ---------------------------------------------------------------- stereotype rules

    private static void checkImplementation(JavaClass type, List<String> violations) {
        if (!isPublic(type)) violations.add(type.getName() + ": an @Implementation is public");
        if (type.getRawSuperclass().map(superclass -> !superclass.getName().equals(Object.class.getName())).orElse(false)) {
            violations.add(type.getName() + ": an @Implementation extends no other class");
        }
        String folder = folderOf(type);
        boolean implementsOwnPrimitive = type.getAllRawInterfaces().stream()
                .anyMatch(implemented -> implemented.isAnnotatedWith(Primitive.class) && folder != null && folder.equals(folderOf(implemented)));
        if (!implementsOwnPrimitive) violations.add(type.getName() + ": an @Implementation implements the primitive of its own folder");
        for (Dependency dependency : type.getDirectDependenciesToSelf()) {
            JavaClass origin = owner(dependency.getOriginClass());
            if (!origin.getName().equals(type.getName()) && !isRoot(origin)) {
                violations.add(type.getName() + ": an @Implementation is used only by the composition root, not by " + origin.getName());
            }
        }
    }

    private static void checkValue(JavaClass type, List<String> violations) {
        boolean closedSet = type.getModifiers().contains(JavaModifier.FINAL)
                && type.getConstructors().stream().allMatch(constructor -> constructor.getModifiers().contains(JavaModifier.PRIVATE));
        if (!type.isRecord() && !type.isEnum() && !closedSet) {
            violations.add(type.getName() + ": a @Value is a record, an enum or a closed set");
        }
        for (JavaClass member : unit(type.getName())) {
            for (Dependency dependency : member.getDirectDependenciesFromSelf()) {
                JavaClass to = dependency.getTargetClass();
                if (isModule(to) || to.isAnnotatedWith(Implementation.class) || to.isAnnotatedWith(Handle.class)) {
                    violations.add(type.getName() + ": a @Value depends on no module, implementation or handle; depends on " + to.getName());
                }
            }
        }
    }

    private static void checkHandle(JavaClass type, List<String> violations) {
        if (!type.isInterface()) violations.add(type.getName() + ": a @Handle is an interface");
        String folder = folderOf(type);
        for (JavaClass implementor : type.getAllSubclasses()) {
            if (implementor.isInterface()) continue;
            boolean nestedInOwnFolder = implementor.getEnclosingClass().isPresent() && folder != null && folder.equals(folderOf(implementor));
            if (!nestedInOwnFolder) violations.add(type.getName() + ": every implementor is nested inside its own folder; " + implementor.getName() + " is not");
        }
    }

    private static void checkStaticOnly(JavaClass type, String what, List<String> violations) {
        if (!type.getModifiers().contains(JavaModifier.FINAL)) violations.add(type.getName() + ": a " + what + " is final");
        if (!type.getConstructors().stream().allMatch(constructor -> constructor.getModifiers().contains(JavaModifier.PRIVATE))) {
            violations.add(type.getName() + ": a " + what + " has only private constructors");
        }
        if (type.getFields().stream().anyMatch(field -> !field.getModifiers().contains(JavaModifier.STATIC))
                || type.getMethods().stream().anyMatch(method -> !method.getModifiers().contains(JavaModifier.STATIC))) {
            violations.add(type.getName() + ": a " + what + " has only static members");
        }
    }

    private static void checkEvent(JavaClass type, List<String> violations) {
        if (!type.isAssignableTo(EVENT_BASE)) violations.add(type.getName() + ": an @Event extends " + EVENT_BASE);
        String packageName = type.getPackageName();
        boolean placed = packageName.startsWith(COMPOSITE + ".") || packageName.startsWith(SYSTEM + ".");
        if (!placed) violations.add(type.getName() + ": an @Event sits in a composite or a system");
    }

    private static void checkInternal(JavaClass type, List<String> violations) {
        if (isPublic(type)) violations.add(type.getName() + ": an @Internal is package-private");
        String folder = folderOf(type);
        for (Dependency dependency : type.getDirectDependenciesToSelf()) {
            JavaClass origin = owner(dependency.getOriginClass());
            if (!origin.getName().equals(type.getName()) && (folder == null || !folder.equals(folderOf(origin)))) {
                violations.add(type.getName() + ": an @Internal is used only inside its folder, not by " + origin.getName());
            }
        }
    }

    private static void checkContent(JavaClass type, List<String> violations) {
        checkStaticOnly(type, "@Content", violations);
        for (JavaField field : type.getFields()) {
            JavaClass rawType = field.getRawType();
            boolean declaration = rawType.isAssignableTo(DEFERRED_REGISTER) || rawType.isAssignableTo(DEFERRED_HOLDER);
            boolean constant = field.getModifiers().containsAll(Set.of(JavaModifier.PUBLIC, JavaModifier.STATIC, JavaModifier.FINAL));
            if (!declaration || !constant) violations.add(type.getName() + ": a @Content holds only public static final registers and holders; " + field.getName() + " is not one");
        }
        boolean registers = type.getMethods().stream().anyMatch(method -> method.getName().equals("register")
                && method.getRawParameterTypes().size() == 1 && method.getRawParameterTypes().getFirst().getName().equals(EVENT_BUS));
        if (!registers) violations.add(type.getName() + ": a @Content has a register(IEventBus) method");
        for (JavaClass member : unit(type.getName())) {
            for (Dependency dependency : member.getDirectDependenciesFromSelf()) {
                if (isModule(dependency.getTargetClass())) violations.add(type.getName() + ": a @Content depends on no module; depends on " + dependency.getTargetClass().getName());
            }
        }
    }

    private static void checkKeys(JavaClass type, List<String> violations) {
        checkStaticOnly(type, "@Keys", violations);
        if (!type.getPackageName().startsWith(SYSTEM + ".")) violations.add(type.getName() + ": a @Keys sits in a system");
        for (JavaField field : type.getFields()) {
            boolean handle = field.getRawType().isAnnotatedWith(Handle.class);
            boolean constant = field.getModifiers().containsAll(Set.of(JavaModifier.PUBLIC, JavaModifier.STATIC, JavaModifier.FINAL));
            if (!handle || !constant) violations.add(type.getName() + ": a @Keys holds only public static final handles; " + field.getName() + " is not one");
        }
        for (JavaClass member : unit(type.getName())) {
            for (Dependency dependency : member.getDirectDependenciesFromSelf()) {
                JavaClass to = dependency.getTargetClass();
                if (isModule(to) || to.isAnnotatedWith(Implementation.class)) {
                    violations.add(type.getName() + ": a @Keys depends on no module or implementation; depends on " + to.getName());
                }
            }
        }
    }

    private static void checkActor(JavaClass type, List<String> violations) {
        if (!isPublic(type)) violations.add(type.getName() + ": an @Actor is public");
        if (type.getAllRawSuperclasses().stream().noneMatch(ArchitectureTest::isGameType)) {
            violations.add(type.getName() + ": an @Actor extends a game type");
        }
        for (JavaClass member : unit(type.getName())) {
            if (member.getModifiers().contains(JavaModifier.SYNTHETIC)) {
                continue;
            }
            for (JavaField field : member.getFields()) {
                if (field.getModifiers().contains(JavaModifier.SYNTHETIC)) {
                    continue;
                }
                if (involvesModule(field.getType())) violations.add(type.getName() + ": an @Actor holds no module; " + field.getFullName() + " does");
                if (field.getModifiers().contains(JavaModifier.STATIC)) {
                    boolean immutable = field.getModifiers().contains(JavaModifier.FINAL) && isImmutableStaticType(field.getRawType());
                    if (!immutable) violations.add(type.getName() + ": an @Actor has no mutable static state; " + field.getFullName() + " is one");
                }
            }
        }
    }

    // ---------------------------------------------------------------- helpers

    private static Optional<JavaClasses> importDirectories(String property) {
        String setting = System.getProperty(property);
        if (setting == null || setting.isBlank()) {
            return Optional.empty();
        }
        List<Path> paths = Arrays.stream(setting.split(java.io.File.pathSeparator)).map(Path::of).filter(Files::isDirectory).toList();
        if (paths.isEmpty()) {
            throw new IllegalStateException(property + " names no existing directory: " + setting);
        }
        return Optional.of(new ClassFileImporter().importPaths(paths));
    }

    /** One class's dependency on another, both mapped to their units. A dependency inside one unit is left out. */
    private record UnitDependency(JavaClass from, JavaClass to, Dependency dependency) {
    }

    private static List<UnitDependency> unitDependencies() {
        List<UnitDependency> all = new ArrayList<>();
        for (JavaClass type : CLASSES) {
            JavaClass from = owner(type);
            for (Dependency dependency : type.getDirectDependenciesFromSelf()) {
                JavaClass to = owner(dependency.getTargetClass());
                if (!to.getName().equals(from.getName())) {
                    all.add(new UnitDependency(from, to, dependency));
                }
            }
        }
        return all;
    }

    /** The top-level class a class belongs to: itself, or the class it is nested in, however deep. */
    private static JavaClass owner(JavaClass type) {
        JavaClass current = type;
        while (current.getEnclosingClass().isPresent()) {
            current = current.getEnclosingClass().get();
        }
        return current;
    }

    /** A top-level class with every class nested in it. */
    private static List<JavaClass> unit(String ownerName) {
        return CLASSES.stream().filter(type -> owner(type).getName().equals(ownerName)).toList();
    }

    private static boolean isRoot(JavaClass type) {
        return owner(type).getName().equals(JolCraft.class.getName());
    }

    private static List<JavaClass> modulesAbovePrimitives() {
        return CLASSES.stream()
                .filter(type -> type.isAnnotatedWith(Composite.class) || type.isAnnotatedWith(ModSystem.class))
                .toList();
    }

    /** The composite or system tier a class sits in, or null. */
    private static String tierOf(JavaClass type) {
        for (String tier : List.of(COMPOSITE, SYSTEM)) {
            if (type.getPackageName().startsWith(tier + ".")) {
                return tier;
            }
        }
        return null;
    }

    private static String placeOf(JavaClass type) {
        String tier = tierOf(type);
        return tier + "." + placeBelow(tier, type);
    }

    /** {@code primitive.log}, {@code composite.config} or {@code system.trade}, or null outside the tiers. */
    private static String placeOfAnyTier(JavaClass type) {
        for (String tier : List.of(PRIMITIVE, COMPOSITE, SYSTEM)) {
            if (type.getPackageName().startsWith(tier + ".")) {
                return tier.substring(MODULE.length() + 1) + "." + placeBelow(tier, type);
            }
        }
        return null;
    }

    /** The tier and folder a class sits in, as {@code primitive.log}, or null outside the tiers. */
    private static String folderOf(JavaClass type) {
        for (String tier : List.of(PRIMITIVE, COMPOSITE, SYSTEM)) {
            if (type.getPackageName().startsWith(tier + ".")) {
                return tier + "." + placeBelow(tier, type);
            }
        }
        return null;
    }

    /** The folder directly below a tier's package; the whole package when the class is not below that tier. */
    private static String placeBelow(String tier, JavaClass type) {
        String packageName = type.getPackageName();
        if (!packageName.startsWith(tier + ".")) {
            return packageName;
        }
        return packageName.substring(tier.length() + 1).split("[.]")[0];
    }

    private static Set<String> constructorPrimitives(JavaClass module) {
        return module.getConstructors().stream()
                .flatMap(constructor -> constructor.getParameterTypes().stream())
                .flatMap(type -> type.getAllInvolvedRawTypes().stream())
                .filter(type -> type.isAnnotatedWith(Primitive.class))
                .map(JavaClass::getName)
                .collect(Collectors.toSet());
    }

    /** Whether any raw type in this type, among generic arguments, bounds and array components, is a module; {@link Module} itself counts. */
    private static boolean involvesModule(JavaType type) {
        return type.getAllInvolvedRawTypes().stream()
                .map(raw -> raw.isArray() ? raw.getBaseComponentType() : raw)
                .anyMatch(ArchitectureTest::isModule);
    }

    private static boolean isModule(JavaClass type) {
        return type.isAssignableTo(Module.class);
    }

    private static boolean isPublic(JavaClass type) {
        return type.getModifiers().contains(JavaModifier.PUBLIC);
    }

    private static boolean isGameType(JavaClass type) {
        return GAME_PREFIXES.stream().anyMatch(prefix -> type.getName().startsWith(prefix));
    }

    private static boolean isClientType(JavaClass type) {
        return CLIENT_PREFIXES.stream().anyMatch(prefix -> type.getName().startsWith(prefix)) || isOurClientPackage(type.getPackageName());
    }

    private static boolean isOurClientPackage(String packageName) {
        return packageName.startsWith(ROOT + ".") && (packageName.endsWith(".client") || packageName.contains(".client."));
    }

    /** What a static final field of an actor may hold: a primitive, a string, a record, an enum, or an id, key, holder, codec, block state property or voxel shape type. */
    private static boolean isImmutableStaticType(JavaClass type) {
        return type.isPrimitive() || type.getName().equals(String.class.getName()) || type.isRecord() || type.isEnum()
                || IMMUTABLE_STATIC_PREFIXES.stream().anyMatch(prefix -> type.getName().startsWith(prefix));
    }

    private static List<String> marks(JavaClass type) {
        List<String> marks = new ArrayList<>();
        for (Class<?> mark : TIERS) {
            if (type.isAnnotatedWith(mark.getName())) marks.add("@" + mark.getSimpleName());
        }
        for (Class<?> mark : STEREOTYPES) {
            if (type.isAnnotatedWith(mark.getName())) marks.add("@" + mark.getSimpleName());
        }
        return marks;
    }

    private static String tierId(JavaClass type) {
        if (type.isAnnotatedWith(Primitive.class)) return type.getAnnotationOfType(Primitive.class).value();
        if (type.isAnnotatedWith(Composite.class)) return type.getAnnotationOfType(Composite.class).value();
        if (type.isAnnotatedWith(ModSystem.class)) return type.getAnnotationOfType(ModSystem.class).value();
        return null;
    }
}
