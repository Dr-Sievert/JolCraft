package net.sievert.jolcraft.module.composite.stat;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.UUIDUtil;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.SavedData;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

import net.sievert.jolcraft.module.Composite;
import net.sievert.jolcraft.module.Module;
import net.sievert.jolcraft.module.primitive.data.Data;
import net.sievert.jolcraft.module.primitive.data.DataKey;
import net.sievert.jolcraft.module.primitive.log.Log;
import net.sievert.jolcraft.module.primitive.stat.Stat;
import net.sievert.jolcraft.module.primitive.stat.StatKey;
import net.sievert.jolcraft.util.text.Dictionary;
import org.jetbrains.annotations.NotNull;

/**
 * Statistics for a player who may be offline: a brew that finishes while its brewer is away, a bounty paid out
 * overnight. A module declares a {@link Book} while it is built, naming the statistics it may award through it;
 * {@link #award} gives the statistic at once to a player who is online and otherwise writes the award into the
 * book, a server-wide saved data, from which the player's next login grants it. Built from {@code stat},
 * {@code data} and {@code log}; the root attaches it to the game bus once, so it hears every login.
 */
@Composite(Dictionary.LEDGER)
public final class Ledger implements Module {

    private final Stat stat;
    private final Data data;
    private final Log log;
    private final List<Book> books = new ArrayList<>();
    private boolean registered;

    public Ledger(Stat stat, Data data, Log log) {
        this.stat = stat;
        this.data = data;
        this.log = log.tagged(this);
    }

    /** Declares a book; only while modules are built. */
    public void declare(Book book) {
        if (registered) {
            throw new IllegalStateException(book + " was declared after the ledger was attached to the game bus");
        }
        if (books.stream().anyMatch(other -> other.id().equals(book.id()))) {
            throw new IllegalStateException(book + " was declared twice");
        }
        data.declare(book.data());
        books.add(book);
    }

    /** Attaches the login listener to the game bus. The root calls it once, after every module is built. */
    public void register(IEventBus gameBus) {
        if (registered) {
            throw new IllegalStateException("the ledger was attached to the game bus twice");
        }
        registered = true;
        gameBus.addListener(PlayerEvent.PlayerLoggedInEvent.class, event -> {
            if (event.getEntity() instanceof ServerPlayer player) {
                grant(player);
            }
        });
    }

    /** How many books are declared; the root logs it with the primitives' counts. */
    public int declared() {
        return books.size();
    }

    /**
     * Awards the statistic now when the player is online, else writes the award into the book for the player's next
     * login. The statistic must be one the book names, so a written award can always be resolved.
     */
    public void award(MinecraftServer server, Book book, UUID player, StatKey key, int amount) {
        if (!books.contains(book)) {
            throw new IllegalArgumentException(book + " was never declared through the ledger; a module declares its books while it is built");
        }
        if (!book.stats().containsKey(key.id())) {
            throw new IllegalArgumentException(key + " is not a statistic of the book " + book + "; a book names what it may award");
        }
        if (amount < 1) {
            throw new IllegalArgumentException("an award is at least one, not " + amount);
        }
        ServerPlayer online = server.getPlayerList().getPlayer(player);
        if (online != null) {
            stat.award(online, key, amount);
            return;
        }
        book.data().get(server).add(new Awards.Entry(player, key.id(), amount));
        log.debug("{} +{} waits for {} in {}", key, amount, player, book);
    }

    /** How many awards wait in the book for the player. */
    public int waiting(MinecraftServer server, Book book, UUID player) {
        return book.data().get(server).waitingFor(player).size();
    }

    private void grant(ServerPlayer player) {
        MinecraftServer server = player.level().getServer();
        for (Book book : books) {
            for (Awards.Entry entry : book.data().get(server).take(player.getUUID())) {
                StatKey key = book.stats().get(entry.stat());
                if (key == null) {
                    log.warn("{} +{} waited for {} but is no statistic of {} any more; dropped", entry.stat(), entry.amount(), player.getName().getString(), book);
                    continue;
                }
                try {
                    stat.award(player, key, entry.amount());
                    log.debug("{} +{} granted to {} from {}", key, entry.amount(), player.getName().getString(), book);
                } catch (RuntimeException e) {
                    log.error("{} +{} for {} from {} could not be granted and is dropped: {}", key, entry.amount(), player.getName().getString(), book, e.toString());
                }
            }
        }
    }

    /** One book: its id names the saved data, and the statistics it may award are listed by id. */
    public record Book(Identifier id, DataKey<Awards> data, Map<Identifier, StatKey> stats) {

        public static Book of(Identifier id, StatKey... stats) {
            if (stats.length == 0) {
                throw new IllegalArgumentException("the book " + id + " names no statistic; a book awards at least one");
            }
            Map<Identifier, StatKey> byId = new LinkedHashMap<>();
            for (StatKey stat : stats) {
                if (byId.putIfAbsent(stat.id(), stat) != null) {
                    throw new IllegalArgumentException(stat + " is named twice in the book " + id);
                }
            }
            return new Book(id, DataKey.of(id, Awards::new, Awards.CODEC).serverWide(), Map.copyOf(byId));
        }

        @Override
        public @NotNull String toString() {
            return id.toString();
        }
    }

    /** The awards waiting in a book, written with the server. */
    public static final class Awards extends SavedData {

        /** One waiting award: for whom, which statistic, how much. */
        public record Entry(UUID player, Identifier stat, int amount) {
            static final Codec<Entry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                    UUIDUtil.CODEC.fieldOf(Dictionary.PLAYER).forGetter(Entry::player),
                    Identifier.CODEC.fieldOf(Dictionary.STAT).forGetter(Entry::stat),
                    Codec.INT.fieldOf(Dictionary.AMOUNT).forGetter(Entry::amount)).apply(instance, Entry::new));
        }

        public static final Codec<Awards> CODEC = Entry.CODEC.listOf().fieldOf(Dictionary.AWARDS).codec()
                .xmap(Awards::new, awards -> List.copyOf(awards.entries));

        private final List<Entry> entries = new ArrayList<>();

        public Awards() {
        }

        private Awards(List<Entry> saved) {
            entries.addAll(saved);
        }

        void add(Entry entry) {
            entries.add(entry);
            setDirty();
        }

        /** The awards waiting for the player, in the order they were written. */
        public List<Entry> waitingFor(UUID player) {
            return entries.stream().filter(entry -> entry.player().equals(player)).toList();
        }

        /** Takes the player's awards out of the book. */
        List<Entry> take(UUID player) {
            List<Entry> due = waitingFor(player);
            if (!due.isEmpty()) {
                entries.removeIf(entry -> entry.player().equals(player));
                setDirty();
            }
            return due;
        }

        /** How many awards wait, for anyone. */
        public int size() {
            return entries.size();
        }
    }
}
