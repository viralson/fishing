package pl.lowienie;

import org.bukkit.Sound;
import org.bukkit.entity.Player;

/** Dźwięki interfejsu – żeby klikanie było przyjemne. */
public final class Sfx {

    private Sfx() {}

    private static void play(Player p, Sound s, float vol, float pitch) { p.playSound(p.getLocation(), s, vol, pitch); }

    /** Otwarcie przystani. */
    public static void open(Player p) {
        play(p, Sound.ENTITY_FISHING_BOBBER_SPLASH, 0.4f, 1.2f);
        play(p, Sound.BLOCK_NOTE_BLOCK_CHIME, 0.4f, 2.0f);
    }

    /** Plusk wody. */
    public static void splash(Player p) { play(p, Sound.ENTITY_FISHING_BOBBER_SPLASH, 0.6f, 1.4f); }

    /** Sprzedaż / pieniądze. */
    public static void cash(Player p) {
        play(p, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.6f, 1.3f);
        play(p, Sound.BLOCK_NOTE_BLOCK_BELL, 0.5f, 1.6f);
    }

    /** Ulepszenie. */
    public static void upgrade(Player p) {
        play(p, Sound.BLOCK_SMITHING_TABLE_USE, 0.6f, 1.3f);
        play(p, Sound.ENTITY_PLAYER_LEVELUP, 0.4f, 1.8f);
    }

    public static void sound(Player p, Sound s, float vol, float pitch) { play(p, s, vol, pitch); }

    /** Zmiana strony. */
    public static void page(Player p) { play(p, Sound.ITEM_BOOK_PAGE_TURN, 0.8f, 1.2f); }

    /** Wybór kategorii / opcji. */
    public static void select(Player p) { play(p, Sound.BLOCK_NOTE_BLOCK_HAT, 0.6f, 1.6f); }

    /** Przełączanie sortowania / filtra. */
    public static void toggle(Player p) { play(p, Sound.UI_BUTTON_CLICK, 0.4f, 1.8f); }

    /** Wejście do podmenu. */
    public static void enter(Player p) { play(p, Sound.BLOCK_WOODEN_TRAPDOOR_OPEN, 0.5f, 1.4f); }

    /** Powrót. */
    public static void back(Player p) { play(p, Sound.BLOCK_WOODEN_TRAPDOOR_CLOSE, 0.5f, 1.4f); }

    /** Gwiazdka / przełącznik. */
    public static void star(Player p, boolean on) { play(p, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.9f, on ? 1.8f : 0.9f); }

    /** Odbiór przedmiotu. */
    public static void pickup(Player p) { play(p, Sound.ENTITY_ITEM_PICKUP, 0.7f, 1.2f); }

    /** Błąd / odmowa. */
    public static void deny(Player p) { play(p, Sound.BLOCK_NOTE_BLOCK_BASS, 0.7f, 0.7f); }
}
