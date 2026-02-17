// Jonathan Decondé - 3196362
package game.entities;

import game.items.Item;
import util.DynArray;
import util.RNG;

/**
 * NPC - Non-player character that can be interacted with in SHOP rooms or other locations.
 * 
 * Framework for:
 * - Merchants (buy/sell items)
 * - Questgivers (assign tasks/rewards)
 * - Recruiters (potential party members)
 * - Dialogue system (branching conversations)
 * 
 * FUTURE: This is a skeleton for expansion into full NPC/party systems
 */
public class NPC {
    public enum NPCType {
        MERCHANT,      // Trades items
        BLACKSMITH,    // Upgrades equipment
        HEALER,        // Restores HP/status
        QUESTGIVER,    // Assigns objectives
        RECRUITER      // Can join party (future)
    }

    private String name;
    private NPCType type;
    private String title;           // "The Blacksmith", "Merchant of Dreams", etc.
    private String description;      // Flavor text
    private DynArray<Item> shop;     // Items for sale (if merchant)
    private int shopGold;            // Gold this NPC has (for buying player items)
    private DynArray<Quest> availableQuests; // Quests to offer
    private DynArray<Quest> completedQuests;  // Quests player has completed

    // NPC state
    private boolean discovered;      // Has player met this NPC?
    private boolean questCompleted;  // Has active quest been finished?
    private String lastDialogue;     // Most recent dialogue line

    public NPC(String name, NPCType type, String title, String description) {
        this.name = name;
        this.type = type;
        this.title = title;
        this.description = description;
        this.shop = new DynArray<>();
        this.availableQuests = new DynArray<>();
        this.completedQuests = new DynArray<>();
        this.shopGold = 500;         // Starting gold for trading
        this.discovered = false;
        this.questCompleted = false;
        this.lastDialogue = "";
    }

    // ===== SHOP SYSTEM =====
    /**
     * Add item to merchant's shop
     */
    public void addItemToShop(Item item) {
        if (type == NPCType.MERCHANT || type == NPCType.BLACKSMITH) {
            shop.add(item);
        }
    }

    /**
     * Get all items available for sale
     */
    public DynArray<Item> getShopInventory() {
        return shop;
    }

    /**
     * Try to sell item to NPC
     * @param item The item to sell
     * @param price What the player wants for it
     * @return true if NPC buys it, false if they refuse/can't afford
     */
    public boolean buyFromPlayer(Item item, int price) {
        if (shopGold >= price) {
            shopGold -= price;
            shop.add(item);  // NPC gains the item
            return true;
        }
        return false;
    }

    /**
     * Try to buy item from NPC
     * @param itemIndex Index in NPC's shop
     * @return Item if successful, null if out of stock
     */
    public Item sellToPlayer(int itemIndex) {
        if (itemIndex >= 0 && itemIndex < shop.size()) {
            Item item = shop.get(itemIndex);
            shop.remove(itemIndex);
            shopGold += item.getValue();  // NPC gains the money
            return item;
        }
        return null;
    }

    // ===== QUEST SYSTEM =====
    /**
     * Add quest to this NPC's available quests
     */
    public void addQuest(Quest quest) {
        availableQuests.add(quest);
    }

    /**
     * Get available quests from this NPC
     */
    public DynArray<Quest> getAvailableQuests() {
        return availableQuests;
    }

    /**
     * Accept quest from NPC
     */
    public Quest acceptQuest(int questIndex) {
        if (questIndex >= 0 && questIndex < availableQuests.size()) {
            Quest quest = availableQuests.get(questIndex);
            return quest;  // Return accepted quest
        }
        return null;
    }

    /**
     * Mark quest as completed
     */
    public void completeQuest(Quest quest) {
        completedQuests.add(quest);
        questCompleted = true;
    }

    // ===== DIALOGUE SYSTEM =====
    /**
     * Get dialogue based on NPC type and state
     * FUTURE: Could expand into full branching dialogue trees with choices
     */
    public String getGreeting() {
        return switch (type) {
            case MERCHANT -> "Welcome, traveler! Browse my wares.";
            case BLACKSMITH -> "I can forge and repair your equipment. Show me what you've got.";
            case HEALER -> "The road is harsh. I can mend your wounds... for a price.";
            case QUESTGIVER -> "I have work for those brave enough to take it.";
            case RECRUITER -> "You look like you could use an extra sword arm.";
        };
    }

    /**
     * Get farewell dialogue
     */
    public String getFarewell() {
        return switch (type) {
            case MERCHANT -> "Come back anytime! I'll have fresh stock.";
            case BLACKSMITH -> "May your blade stay sharp and your armor stay strong.";
            case HEALER -> "Go forth, and try to stay alive this time.";
            case QUESTGIVER -> "Good luck. Don't disappoint me.";
            case RECRUITER -> "Let's see what you're made of in combat.";
        };
    }

    /**
     * Get response when player completes quest
     */
    public String getQuestCompleteDialogue() {
        return "Excellent work! Here's your reward as promised.";
    }

    // ===== SIMPLE INTERACTION FLOW =====
    /**
     * Start interaction with NPC
     * FUTURE: This should branch into full dialogue/action trees
     */
    public void interact() {
        discovered = true;
        lastDialogue = getGreeting();
    }

    /**
     * Show NPC status/summary
     */
    public String getSummary() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("═══ %s ═══\n", name));
        sb.append(String.format("Title: %s\n", title));
        sb.append(String.format("Type: %s\n", type.toString()));
        sb.append(String.format("Description: %s\n", description));
        
        if (type == NPCType.MERCHANT || type == NPCType.BLACKSMITH) {
            sb.append(String.format("Shop Items: %d\n", shop.size()));
            sb.append(String.format("Gold Available: %d\n", shopGold));
        }
        
        if (type == NPCType.QUESTGIVER) {
            sb.append(String.format("Available Quests: %d\n", availableQuests.size()));
            sb.append(String.format("Completed Quests: %d\n", completedQuests.size()));
        }
        
        return sb.toString();
    }

    // ===== GETTERS/SETTERS =====
    public String getName() { return name; }
    public NPCType getType() { return type; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public boolean isDiscovered() { return discovered; }
    public String getLastDialogue() { return lastDialogue; }
    public int getShopGold() { return shopGold; }
    public void setShopGold(int gold) { this.shopGold = gold; }

    // ===== QUEST INNER CLASS =====
    /**
     * Represents a quest that can be assigned to the player
     */
    public static class Quest {
        private String name;
        private String description;
        private String objective;
        private int rewardGold;
        private int rewardXP;
        private Item rewardItem;
        private boolean completed;

        public Quest(String name, String description, String objective, 
                     int rewardGold, int rewardXP, Item rewardItem) {
            this.name = name;
            this.description = description;
            this.objective = objective;
            this.rewardGold = rewardGold;
            this.rewardXP = rewardXP;
            this.rewardItem = rewardItem;
            this.completed = false;
        }

        // Getters
        public String getName() { return name; }
        public String getDescription() { return description; }
        public String getObjective() { return objective; }
        public int getRewardGold() { return rewardGold; }
        public int getRewardXP() { return rewardXP; }
        public Item getRewardItem() { return rewardItem; }
        public boolean isCompleted() { return completed; }
        public void setCompleted(boolean completed) { this.completed = completed; }
    }
}
