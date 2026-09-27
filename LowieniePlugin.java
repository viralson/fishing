package pl.lowienie;

import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class LowieniePlugin extends JavaPlugin {

    private Store store;
    private Fishes fishes;
    private FishItems items;
    private Market market;
    private Quests quests;
    private Fishing fishing;
    private Hotspots hotspots;
    private Frenzy frenzy;
    private Tournament tournament;
    private Creatures creatures;
    private Gui gui;
    private FishGui fishGui;
    private InternalEco internal;
    private Eco eco;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        fishes = new Fishes();
        items = new FishItems(this);
        internal = new InternalEco(this);
        eco = internal;
        store = new Store(this);
        store.loadGlobal();
        market = new Market(this);
        quests = new Quests(this);
        fishing = new Fishing(this);
        hotspots = new Hotspots(this);
        frenzy = new Frenzy(this);
        tournament = new Tournament(this);
        creatures = new Creatures(this);
        gui = new Gui(this);
        fishGui = new FishGui(this);
        store.loadOnline();

        FishCommand cmd = new FishCommand(this);
        PluginCommand pc = getCommand("ryby");
        if (pc != null) {
            pc.setExecutor(cmd);
            pc.setTabCompleter(cmd);
        }
        getServer().getPluginManager().registerEvents(new FishListener(this), this);

        var sch = getServer().getScheduler();
        // ekonomia tick później – EssentialsX i inne rejestrują się z opóźnieniem
        sch.runTask(this, () -> {
            if (getServer().getPluginManager().getPlugin("Vault") != null) {
                VaultEco v = VaultEco.create(this);
                if (v != null) eco = v;
            }
            getLogger().info("Ekonomia: " + eco.name());
        });
        sch.runTaskTimer(this, fishing::tick, 1L, 1L);
        sch.runTaskTimer(this, () -> {
            for (Player p : getServer().getOnlinePlayers()) {
                if (p.getOpenInventory().getTopInventory().getHolder(false) instanceof Menu m && m.ticker() != null) m.ticker().run();
            }
        }, 6L, 6L);
        sch.runTaskTimer(this, () -> { hotspots.particles(); creatures.tick(); }, 10L, 10L);
        sch.runTaskTimer(this, () -> { frenzy.tick(); tournament.tick(); }, 20L, 20L);
        sch.runTaskTimer(this, hotspots::spawnTick, 200L, 400L);
        sch.runTaskTimer(this, () -> {
            market.recover();
            frenzy.maybeAuto();
            store.autosave();
        }, 1200L, 1200L);

        getLogger().info("Łowienie włączone – " + fishes.size() + " gatunków ryb.");
    }

    @Override
    public void onDisable() {
        if (fishing != null) fishing.cancelAll();
        if (creatures != null) creatures.removeAll();
        if (tournament != null && tournament.active()) tournament.stop(false);
        if (frenzy != null) frenzy.stop();
        if (hotspots != null) hotspots.clear();
        for (Player p : getServer().getOnlinePlayers())
            if (p.getOpenInventory().getTopInventory().getHolder(false) instanceof Menu) p.closeInventory();
        if (store != null) store.saveAll();
    }

    public Store store() { return store; }
    public Fishes fishes() { return fishes; }
    public FishItems items() { return items; }
    public Market market() { return market; }
    public Quests quests() { return quests; }
    public Fishing fishing() { return fishing; }
    public Hotspots hotspots() { return hotspots; }
    public Frenzy frenzy() { return frenzy; }
    public Tournament tournament() { return tournament; }
    public Creatures creatures() { return creatures; }
    public Gui gui() { return gui; }
    public FishGui fishGui() { return fishGui; }
    public Eco eco() { return eco; }
    public InternalEco internalEco() { return internal; }
}
