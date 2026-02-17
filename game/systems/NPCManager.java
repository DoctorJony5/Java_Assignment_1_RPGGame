// Jonathan Decondé - 3196362
package game.systems;

import game.entities.NPC;
import game.entities.NPC.Quest;
import game.items.Item;
import util.DynArray;

/**
 * NPCManager - Manages all NPCs in the game world
 * 
 * Handles:
 * - NPC registration and tracking
 * - Shop/trading with NPCs
 * - Quest assignment and completion
 * - NPC state persistence
 * 
 * FUTURE: Should be integrated with save/load system
 */
public class NPCManager {
    private DynArray<NPC> npcRegistry;  // All NPCs that exist
    private static NPCManager instance;  // Singleton pattern

    private NPCManager() {
        this.npcRegistry = new DynArray<>();
        initializeDefaultNPCs();
    }

    /**
     * Get singleton instance
     */
    public static NPCManager getInstance() {
        if (instance == null) {
            instance = new NPCManager();
        }
        return instance;
    }

    /**
     * Create default NPCs for the game
     * FUTURE: These should be data-driven from config files
     */
    private void initializeDefaultNPCs() {
        // Create some default merchants/NPCs
        
        // Thoren the Merchant - buys/sells weapons and armor
        NPC merchant = new NPC("Thoren", NPC.NPCType.MERCHANT, "The Wandering Merchant", 
            "A seasoned trader with goods from every corner of the realm.");
        merchant.setShopGold(1000);
        npcRegistry.add(merchant);

        // Elara the Healer - sells potions and restores HP
        NPC healer = new NPC("Elara", NPC.NPCType.HEALER, "The Herbalist", 
            "Expert in the art of healing. Her potions are renowned.");
        healer.setShopGold(800);
        npcRegistry.add(healer);

        // Grox the Blacksmith - upgrades equipment
        NPC blacksmith = new NPC("Grox", NPC.NPCType.BLACKSMITH, "Master of Forge and Steel", 
            "A dwarf of unmatched skill. Your weapons will be legendary.");
        blacksmith.setShopGold(1200);
        npcRegistry.add(blacksmith);

        // Gaia the Questgiver - assigns tasks
        NPC questgiver = new NPC("Gaia", NPC.NPCType.QUESTGIVER, "The Oracle", 
            "A mysterious figure who always seems to know what needs doing.");
        npcRegistry.add(questgiver);
    }

    /**
     * Register a new NPC
     */
    public void registerNPC(NPC npc) {
        if (npc != null) {
            npcRegistry.add(npc);
        }
    }

    /**
     * Get NPC by name
     */
    public NPC getNPCByName(String name) {
        for (int i = 0; i < npcRegistry.size(); i++) {
            NPC npc = npcRegistry.get(i);
            if (npc.getName().equalsIgnoreCase(name)) {
                return npc;
            }
        }
        return null;
    }

    /**
     * Get all NPCs of a specific type
     */
    public DynArray<NPC> getNPCsByType(NPC.NPCType type) {
        DynArray<NPC> result = new DynArray<>();
        for (int i = 0; i < npcRegistry.size(); i++) {
            NPC npc = npcRegistry.get(i);
            if (npc.getType() == type) {
                result.add(npc);
            }
        }
        return result;
    }

    /**
     * Get all available merchants
     */
    public DynArray<NPC> getMerchants() {
        return getNPCsByType(NPC.NPCType.MERCHANT);
    }

    /**
     * Get all available quest givers
     */
    public DynArray<NPC> getQuestgivers() {
        return getNPCsByType(NPC.NPCType.QUESTGIVER);
    }

    /**
     * Get random NPC of specific type
     */
    public NPC getRandomNPC(NPC.NPCType type) {
        DynArray<NPC> matches = getNPCsByType(type);
        if (matches.size() == 0) return null;
        
        int index = (int) (Math.random() * matches.size());
        return matches.get(index);
    }

    /**
     * Get all NPCs
     */
    public DynArray<NPC> getAllNPCs() {
        return npcRegistry;
    }

    /**
     * Get number of NPCs
     */
    public int getNPCCount() {
        return npcRegistry.size();
    }

    /**
     * Reset all NPC state (for new run)
     */
    public void resetAllNPCs() {
        for (int i = 0; i < npcRegistry.size(); i++) {
            NPC npc = npcRegistry.get(i);
            // Reset state as needed (clear shop, reset quests, etc.)
            // Currently NPCs persist across runs, but this can be changed
        }
    }

    /**
     * Print all registered NPCs (debug)
     */
    public String debugPrintNPCs() {
        StringBuilder sb = new StringBuilder();
        sb.append("===== REGISTERED NPCs =====\n");
        for (int i = 0; i < npcRegistry.size(); i++) {
            NPC npc = npcRegistry.get(i);
            sb.append(String.format("[%d] %s (%s)\n", i + 1, npc.getName(), npc.getType().toString()));
        }
        sb.append("Total: ").append(npcRegistry.size()).append("\n");
        return sb.toString();
    }
}
