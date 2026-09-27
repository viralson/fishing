package pl.lowienie;

import org.bukkit.Material;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.bukkit.Material.*;
import static pl.lowienie.Rarity.*;
import static pl.lowienie.Species.When;
import static pl.lowienie.Species.Where;

/** Rejestr wszystkich 50 gatunków. */
public final class Fishes {

    private final Map<String, Species> byId = new LinkedHashMap<>();
    private final Map<Rarity, List<Species>> byRarity = new EnumMap<>(Rarity.class);

    public Fishes() {
        // ── Pospolite ─────────────────────────────
        add("karp", "Karp", POSPOLITA, COD, Where.ANY, When.ANY, 30, 80, 0.5, 9, 25, "Klasyk każdego stawu.");
        add("leszcz", "Leszcz", POSPOLITA, COD, Where.ANY, When.ANY, 25, 60, 0.3, 4, 20, "Płaski i leniwy.");
        add("plotka", "Płotka", POSPOLITA, COD, Where.RIVER, When.ANY, 15, 35, 0.1, 0.8, 18, "Czerwone oczka, srebrne łuski.");
        add("ukleja", "Ukleja", POSPOLITA, COD, Where.RIVER, When.DAY, 10, 20, 0.05, 0.2, 16, "Pływa tuż pod powierzchnią.");
        add("okon", "Okoń", POSPOLITA, COD, Where.RIVER, When.ANY, 15, 45, 0.1, 2, 26, "Kolczasta płetwa grzbietowa.");
        add("dorsz", "Dorsz", POSPOLITA, COD, Where.OCEAN, When.ANY, 35, 100, 0.8, 12, 30, "Król smażalni nad morzem.");
        add("sledz", "Śledź", POSPOLITA, COD, Where.OCEAN, When.ANY, 20, 40, 0.1, 0.6, 22, "Pływa w ogromnych ławicach.");
        add("makrela", "Makrela", POSPOLITA, SALMON, Where.OCEAN, When.DAY, 25, 50, 0.3, 1.5, 28, "Szybka i pasiasta.");
        add("karas", "Karaś", POSPOLITA, COD, Where.SWAMP, When.ANY, 15, 40, 0.2, 1.8, 24, "Przetrwa nawet w mule.");
        add("sardynka", "Sardynka", POSPOLITA, TROPICAL_FISH, Where.WARM, When.ANY, 10, 25, 0.05, 0.2, 22, "Mała, ale smaczna.");
        add("stynka", "Stynka", POSPOLITA, COD, Where.COLD, When.ANY, 12, 30, 0.05, 0.3, 26, "Pachnie świeżym ogórkiem.");

        // ── Niepospolite ──────────────────────────
        add("szczupak", "Szczupak", NIEPOSPOLITA, SALMON, Where.ANY, When.ANY, 50, 130, 1.5, 20, 90, "Drapieżnik z paszczą pełną zębów.");
        add("sandacz", "Sandacz", NIEPOSPOLITA, COD, Where.RIVER, When.NIGHT, 40, 100, 1, 12, 110, "Poluje o zmroku.");
        add("lin", "Lin", NIEPOSPOLITA, COD, Where.SWAMP, When.ANY, 25, 60, 0.4, 5, 85, "Złotozielony mieszkaniec bagien.");
        add("pstrag", "Pstrąg", NIEPOSPOLITA, SALMON, Where.RIVER, When.DAY, 25, 70, 0.3, 6, 100, "Lubi zimną, czystą wodę.");
        add("losos", "Łosoś", NIEPOSPOLITA, SALMON, Where.OCEAN, When.ANY, 50, 120, 2, 25, 120, "Wraca do rzeki, w której się urodził.");
        add("dorada", "Dorada", NIEPOSPOLITA, TROPICAL_FISH, Where.WARM, When.ANY, 25, 60, 0.5, 4, 105, "Złota brew nad okiem.");
        add("fladra", "Flądra", NIEPOSPOLITA, COD, Where.OCEAN, When.ANY, 25, 60, 0.4, 3, 95, "Oczy po jednej stronie głowy.");
        add("wegorz", "Węgorz", NIEPOSPOLITA, COD, Where.SWAMP, When.NIGHT, 50, 130, 0.5, 6, 130, "Śliski jak mydło.");
        add("sum", "Sum", NIEPOSPOLITA, COD, Where.RIVER, When.NIGHT, 80, 200, 5, 60, 140, "Wąsaty olbrzym z dna.");
        add("blazenek", "Błazenek", NIEPOSPOLITA, TROPICAL_FISH, Where.WARM, When.DAY, 6, 12, 0.02, 0.1, 115, "Mieszka w ukwiale.");

        // ── Rzadkie ───────────────────────────────
        add("tunczyk", "Tuńczyk", RZADKA, SALMON, Where.OCEAN, When.ANY, 100, 250, 20, 250, 380, "Torpeda oceanu.");
        add("jesiotr", "Jesiotr", RZADKA, COD, Where.RIVER, When.ANY, 100, 250, 10, 100, 450, "Żywa skamieniałość.");
        add("miecznik", "Miecznik", RZADKA, SALMON, Where.OCEAN, When.DAY, 150, 300, 40, 300, 520, "Nos jak szpada.");
        add("rozdymka", "Rozdymka", RZADKA, PUFFERFISH, Where.OCEAN, When.ANY, 10, 40, 0.1, 2, 400, "Nie jedz jej na surowo!");
        add("glowacica", "Głowacica", RZADKA, SALMON, Where.COLD, When.ANY, 60, 150, 3, 35, 480, "Królowa górskich potoków.");
        add("ksiezycowa", "Ryba księżycowa", RZADKA, TROPICAL_FISH, Where.OCEAN, When.NIGHT, 100, 300, 50, 900, 600, "Wypływa przy pełni.");
        add("slepczyk", "Ślepczyk jaskiniowy", RZADKA, COD, Where.CAVE, When.ANY, 8, 20, 0.02, 0.1, 420, "Nigdy nie widział słońca.");
        add("pirania", "Pirania", RZADKA, TROPICAL_FISH, Where.WARM, When.ANY, 15, 35, 0.3, 3, 450, "Uważaj na palce.");
        add("burzowy_pstrag", "Burzowy pstrąg", RZADKA, SALMON, Where.ANY, When.STORM, 30, 80, 1, 8, 550, "Wyskakuje z wody przy piorunach.");
        add("deszczowka", "Deszczówka", RZADKA, TROPICAL_FISH, Where.ANY, When.RAIN, 15, 40, 0.2, 1.5, 400, "Pojawia się tylko w deszczu.");

        // ── Epickie ───────────────────────────────
        add("marlin", "Marlin błękitny", EPICKA, SALMON, Where.OCEAN, When.ANY, 200, 450, 80, 700, 1600, "Najszybsza ryba świata.");
        add("mlot", "Rekin młot", EPICKA, COD, Where.OCEAN, When.ANY, 250, 500, 150, 450, 2000, "Głowa jak młot kowalski.");
        add("arapaima", "Arapaima", EPICKA, SALMON, Where.WARM, When.ANY, 150, 400, 50, 200, 1800, "Oddycha powietrzem.");
        add("lodowa", "Lodowa ryba", EPICKA, COD, Where.COLD, When.NIGHT, 30, 70, 1, 5, 2200, "Ma przezroczystą krew.");
        add("elektryk", "Węgorz elektryczny", EPICKA, COD, Where.SWAMP, When.STORM, 100, 250, 10, 20, 2600, "Kopie na 600 woltów.");
        add("latimeria", "Latimeria", EPICKA, COD, Where.CAVE, When.ANY, 100, 200, 40, 90, 2400, "Uznana za wymarłą 66 mln lat.");
        add("zlota_rybka", "Złota rybka", EPICKA, TROPICAL_FISH, Where.ANY, When.DAY, 5, 20, 0.01, 0.3, 1500, "Podobno spełnia życzenia.");
        add("duch", "Ryba-duch", EPICKA, TROPICAL_FISH, Where.ANY, When.NIGHT, 20, 60, 0.2, 2, 2000, "Przenika przez sieci.");

        // ── Legendarne ────────────────────────────
        add("bialy_rekin", "Wielki biały rekin", LEGENDARNA, COD, Where.OCEAN, When.ANY, 400, 650, 700, 2200, 9000, "Postrach wszystkich mórz.");
        add("krol_jesiotr", "Królewski jesiotr", LEGENDARNA, COD, Where.RIVER, When.ANY, 300, 700, 300, 1500, 10000, "Nosi koronę z łusek.");
        add("krysztalowy", "Kryształowy karp", LEGENDARNA, PRISMARINE_CRYSTALS, Where.CAVE, When.ANY, 40, 90, 3, 15, 14000, "Świeci w ciemności jaskiń.");
        add("teczowa", "Tęczowa ryba", LEGENDARNA, TROPICAL_FISH, Where.ANY, When.RAIN, 30, 80, 1, 6, 12000, "Mieni się wszystkimi kolorami.");
        add("ognisty_losos", "Ognisty łosoś", LEGENDARNA, SALMON, Where.WARM, When.DAY, 60, 150, 5, 30, 13000, "Woda wokół niego paruje.");
        add("ksiezycowy_wegorz", "Księżycowy węgorz", LEGENDARNA, COD, Where.ANY, When.NIGHT, 100, 300, 5, 40, 11000, "Srebrny jak światło księżyca.");

        // ── Mityczne ──────────────────────────────
        add("smocza", "Smocza ryba", MITYCZNA, SALMON, Where.ANY, When.STORM, 200, 600, 100, 900, 90000, "Rodzi się z uderzenia pioruna.");
        add("posejdon", "Ryba Posejdona", MITYCZNA, HEART_OF_THE_SEA, Where.OCEAN, When.ANY, 150, 400, 50, 500, 120000, "Dar władcy mórz.");
        add("kolakant", "Pradawny kolakant", MITYCZNA, NAUTILUS_SHELL, Where.CAVE, When.ANY, 150, 300, 80, 200, 110000, "Starszy niż dinozaury.");
        add("gwiezdna", "Gwiezdna płetwa", MITYCZNA, AMETHYST_SHARD, Where.ANY, When.NIGHT, 50, 150, 2, 20, 100000, "Spadła z nieba do wody.");
        add("krol_karpi", "Złoty Król Karpi", MITYCZNA, GOLD_INGOT, Where.ANY, When.ANY, 100, 250, 30, 120, 80000, "Legenda każdego wędkarza.");

        for (Rarity r : Rarity.values()) byRarity.putIfAbsent(r, new ArrayList<>());
    }

    private void add(String id, String name, Rarity r, Material m, Where w, When t,
                     double minCm, double maxCm, double minKg, double maxKg, double price, String desc) {
        Species s = new Species(id, name, r, m, w, t, minCm, maxCm, minKg, maxKg, price, desc);
        byId.put(id, s);
        byRarity.computeIfAbsent(r, k -> new ArrayList<>()).add(s);
    }

    public Species get(String id) { return id == null ? null : byId.get(id.toLowerCase()); }

    public List<Species> all() { return Collections.unmodifiableList(new ArrayList<>(byId.values())); }

    public List<Species> of(Rarity r) { return byRarity.get(r); }

    public int size() { return byId.size(); }
}
