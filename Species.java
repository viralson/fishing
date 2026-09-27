package pl.lowienie;

import org.bukkit.Material;

/**
 * Gatunek ryby.
 * @param where gdzie żyje (biom)
 * @param when  kiedy bierze (pora / pogoda)
 */
public record Species(String id, String name, Rarity rarity, Material mat, Where where, When when,
                      double minCm, double maxCm, double minKg, double maxKg, double price, String desc) {

    public enum Where {
        ANY("Wszędzie"), OCEAN("Ocean"), RIVER("Rzeka"), SWAMP("Bagno"), COLD("Zimne wody"), WARM("Ciepłe wody"), CAVE("Jaskinie (Y<45)");
        public final String label;
        Where(String l) { label = l; }
    }

    public enum When {
        ANY("Zawsze"), DAY("W dzień"), NIGHT("W nocy"), RAIN("W deszczu"), STORM("W burzy");
        public final String label;
        When(String l) { label = l; }
    }

    /** Nazwa w kolorze rzadkości. */
    public String display() { return rarity.c() + name + "</" + rarity.hex + ">"; }

    public boolean edible() {
        return mat == Material.COD || mat == Material.SALMON || mat == Material.TROPICAL_FISH || mat == Material.PUFFERFISH;
    }
}
