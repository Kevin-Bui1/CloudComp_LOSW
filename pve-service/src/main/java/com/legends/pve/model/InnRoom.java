package com.legends.pve.model;

import java.util.ArrayList;
import java.util.List;

/**
 * InnRoom — the rest stop between dungeon floors.
 *
 * When entered it:
 *   1. Fully heals and restores mana for every hero in the party.
 *   2. Returns an available item catalogue and recruitable hero list
 *      as part of the InnResult, so the client can present choices.
 *
 * Purchases and recruitment are separate API calls handled by PveController
 * (POST /api/pve/{userId}/inn/buy and POST /api/pve/{userId}/inn/recruit)
 * so the REST API stays stateless — each action is its own request.
 */
public class InnRoom extends Room {

    /** Item catalogue: { name, cost, hpBonus, manaBonus, attackBonus, defenseBonus } */
    public static final Object[][] ITEMS = {
        { "Bread",       200, 20,  0,  0, 0 },
        { "Potion",      350, 50,  0,  0, 0 },
        { "Elixir",      500, 80, 40,  0, 0 },
        { "Power Shard", 400,  0,  0, 10, 0 },
        { "Iron Shield", 400,  0,  0,  0, 8 },
    };

    /** Heroes available for recruitment at this inn (scaled by floor). */
    private final List<Hero> recruitableHeroes;

    public InnRoom(int floor) {
        super(floor);
        this.recruitableHeroes = generateRecruits(floor);
    }

    /**
     * Heals the whole party and returns a summary of what was restored.
     * Also returns the item catalogue and recruitable heroes so the
     * client can display the full inn UI in one call.
     */
    @Override
    public String enter(Party party) {
        StringBuilder sb = new StringBuilder("Welcome to the Inn!\n");

        // Heal all heroes
        for (Hero h : party.getHeroes()) {
            int hpGained   = h.getMaxHp()   - h.getHp();
            int manaGained = h.getMaxMana()  - h.getMana();
            h.setHp(h.getMaxHp());
            h.setMana(h.getMaxMana());
            sb.append(h.getName())
              .append(" restored ").append(hpGained).append(" HP and ")
              .append(manaGained).append(" mana.\n");
        }

        // List shop
        sb.append("SHOP: ");
        for (Object[] item : ITEMS) {
            sb.append(item[0]).append("(").append(item[1]).append("g) ");
        }
        sb.append("\n");

        // List recruits
        if (!recruitableHeroes.isEmpty()) {
            sb.append("RECRUITS: ");
            for (Hero r : recruitableHeroes) {
                sb.append(r.getName()).append("[").append(r.getHeroClass()).append("] ");
            }
        }

        return sb.toString().trim();
    }

    public List<Hero>     getRecruitableHeroes() { return recruitableHeroes; }
    public Object[][]     getItems()              { return ITEMS; }

    // ── Helpers ──────────────────────────────────────────────────────────

    private List<Hero> generateRecruits(int floor) {
        List<Hero> recruits = new ArrayList<>();
        // 2 recruits always available; stats scale with floor
        String[] classes = { "WARRIOR", "MAGE", "ORDER", "CHAOS" };
        String[] names   = { "Aldric", "Syra", "Torven", "Lirien", "Bram" };
        for (int i = 0; i < 2; i++) {
            String heroClass = classes[(floor + i) % classes.length];
            String name      = names[(floor + i) % names.length];
            Hero h = new Hero(name, heroClass);
            // Give recruits slightly better stats at higher floors
            int bonus = (floor / 5);
            h.setAttack(h.getAttack() + bonus);
            h.setDefense(h.getDefense() + bonus);
            recruits.add(h);
        }
        return recruits;
    }
}
