// Jonathan Decondé - 3196362
package game.systems;

import game.entities.NPC;
import game.entities.Player;
import game.items.Item;
import game.ui.View;
import util.DynArray;
import util.Logger;

/**
 * NPCInteractionSystem - Handles player interactions with NPCs
 * 
 * Manages:
 * - NPC greeting and dialogue
 * - Shop browsing and trading
 * - Quest acceptance and completion
 * - Party recruitment (future)
 * 
 * This is the controller layer between NPCs and the View
 */
public class NPCInteractionSystem {
    private Player player;
    private View view;
    private Logger logger;
    private NPCManager npcManager;

    public NPCInteractionSystem(Player player, View view) {
        this.player = player;
        this.view = view;
        this.logger = Logger.getInstance();
        this.npcManager = NPCManager.getInstance();
        logger.debug("NPC_SYSTEM", "NPCInteractionSystem initialized");
    }

    /**
     * Main interaction handler - call when player enters SHOP room
     */
    public void interactWithNPC(NPC npc) {
        if (npc == null) return;

        logger.info("NPC_SYSTEM", "Player interacting with NPC: " + npc.getName());
        npc.interact();  // Mark as discovered

        boolean continueInteracting = true;
        while (continueInteracting) {
            view.clear();
            view.printCentered("═══ " + npc.getTitle() + " ═══");
            view.printLine("");
            view.printLine(npc.getName() + ": " + npc.getGreeting());
            view.printLine("");

            // Show options based on NPC type
            switch (npc.getType()) {
                case MERCHANT, BLACKSMITH -> {
                    continueInteracting = showShopMenu(npc);
                }
                case HEALER -> {
                    continueInteracting = showHealerMenu(npc);
                }
                case QUESTGIVER -> {
                    continueInteracting = showQuestMenu(npc);
                }
                case RECRUITER -> {
                    continueInteracting = showRecruiterMenu(npc);
                }
            }
        }

        // Farewell
        view.clear();
        view.printLine(npc.getName() + ": " + npc.getFarewell());
        view.pressAnyKey();
    }

    /**
     * Show shop menu for merchants/blacksmiths
     */
    private boolean showShopMenu(NPC npc) {
        view.printLine("[1] Browse items for sale");
        view.printLine("[2] Sell item to " + npc.getName());
        view.printLine("[3] Leave shop");
        view.printLine("");
        view.print("Choice > ");

        try {
            String input = view.readLine().trim();

            switch (input) {
                case "1" -> {
                    showShopBrowse(npc);
                    return true;
                }
                case "2" -> {
                    showSellItems(npc);
                    return true;
                }
                case "3" -> {
                    return false;
                }
                default -> {
                    view.showError("Invalid choice!");
                    return true;
                }
            }
        } catch (Exception e) {
            logger.error("NPC_SYSTEM", "Error in shop menu", e);
            return false;
        }
    }

    /**
     * Show available items for purchase
     */
    private void showShopBrowse(NPC npc) {
        DynArray<Item> inventory = npc.getShopInventory();

        if (inventory.size() == 0) {
            view.clear();
            view.printLine(npc.getName() + ": I'm out of stock right now. Check back later!");
            view.pressAnyKey();
            return;
        }

        view.clear();
        view.printCentered("═══ " + npc.getName() + "'s Shop ═══");
        view.printLine("");

        for (int i = 0; i < inventory.size(); i++) {
            Item item = inventory.get(i);
            view.printLine(String.format("[%d] %s (Value: %d gold) [%s]",
                    i + 1, item.getName(), item.getValue(), item.getRarity().toString()));
        }
        view.printLine("[0] Back");
        view.printLine("");
        view.print("Select item (or 0 to back) > ");

        try {
            String input = view.readLine().trim();
            int choice = Integer.parseInt(input);

            if (choice == 0) return;
            if (choice > 0 && choice <= inventory.size()) {
                int itemCost = inventory.get(choice - 1).getValue();
                
                if (player.getGold() >= itemCost) {
                    Item purchased = npc.sellToPlayer(choice - 1);
                    player.subtractGold(itemCost);
                    player.addItem(purchased);
                    
                    view.clear();
                    view.printColoredLine("✓ Purchased " + purchased.getName() + " for " + itemCost + " gold!", 
                        util.ConsoleColor.BRIGHT_GREEN);
                    view.pressAnyKey();
                } else {
                    view.clear();
                    view.printColoredLine("✖ You don't have enough gold!", util.ConsoleColor.BRIGHT_RED);
                    view.pressAnyKey();
                }
            }
        } catch (Exception e) {
            logger.error("NPC_SYSTEM", "Error buying from shop", e);
        }
    }

    /**
     * Show player items for sale to NPC
     */
    private void showSellItems(NPC npc) {
        DynArray<Item> inventory = player.getInventory();

        if (inventory.size() == 0) {
            view.clear();
            view.printLine("You have no items to sell!");
            view.pressAnyKey();
            return;
        }

        view.clear();
        view.printCentered("═══ Sell to " + npc.getName() + " ═══");
        view.printLine("");

        for (int i = 0; i < Math.min(10, inventory.size()); i++) {  // Show first 10 items
            Item item = inventory.get(i);
            int sellPrice = (int) (item.getValue() * 0.75);  // 75% of value
            view.printLine(String.format("[%d] %s (Sell for: %d gold) [%s]",
                    i + 1, item.getName(), sellPrice, item.getRarity().toString()));
        }
        if (inventory.size() > 10) {
            view.printLine("... and " + (inventory.size() - 10) + " more items");
        }
        view.printLine("[0] Back");
        view.printLine("");
        view.print("Select item to sell > ");

        try {
            String input = view.readLine().trim();
            int choice = Integer.parseInt(input);

            if (choice == 0) return;
            if (choice > 0 && choice <= inventory.size()) {
                Item toSell = inventory.get(choice - 1);
                int sellPrice = (int) (toSell.getValue() * 0.75);

                if (npc.buyFromPlayer(toSell, sellPrice)) {
                    player.removeItem(toSell);
                    player.addGold(sellPrice);
                    
                    view.clear();
                    view.printColoredLine("✓ Sold " + toSell.getName() + " for " + sellPrice + " gold!", 
                        util.ConsoleColor.BRIGHT_GREEN);
                    view.pressAnyKey();
                } else {
                    view.clear();
                    view.printColoredLine("✖ " + npc.getName() + " doesn't want that item.", 
                        util.ConsoleColor.BRIGHT_RED);
                    view.pressAnyKey();
                }
            }
        } catch (Exception e) {
            logger.error("NPC_SYSTEM", "Error selling to shop", e);
        }
    }

    /**
     * Show healer menu
     */
    private boolean showHealerMenu(NPC npc) {
        view.printLine("[1] Restore HP (" + 100 + " gold)");
        view.printLine("[2] Cure status effects (" + 50 + " gold)");
        view.printLine("[3] Leave");
        view.printLine("");
        view.print("Choice > ");

        try {
            String input = view.readLine().trim();

            switch (input) {
                case "1" -> {
                    if (player.getGold() >= 100) {
                        player.subtractGold(100);
                        player.heal(player.getMaxHealth());
                        view.clear();
                        view.printColoredLine("✓ You feel rejuvenated!", util.ConsoleColor.BRIGHT_GREEN);
                        view.pressAnyKey();
                    } else {
                        view.clear();
                        view.printColoredLine("✖ Not enough gold!", util.ConsoleColor.BRIGHT_RED);
                        view.pressAnyKey();
                    }
                    return true;
                }
                case "2" -> {
                    // TODO: Implement status cure
                    view.clear();
                    view.printLine("Status curing not yet implemented.");
                    view.pressAnyKey();
                    return true;
                }
                case "3" -> {
                    return false;
                }
                default -> {
                    view.showError("Invalid choice!");
                    return true;
                }
            }
        } catch (Exception e) {
            logger.error("NPC_SYSTEM", "Error in healer menu", e);
            return false;
        }
    }

    /**
     * Show quest menu
     */
    private boolean showQuestMenu(NPC npc) {
        view.printLine("[1] View available quests");
        view.printLine("[2] Leave");
        view.printLine("");
        view.print("Choice > ");

        try {
            String input = view.readLine().trim();

            switch (input) {
                case "1" -> {
                    showQuests(npc);
                    return true;
                }
                case "2" -> {
                    return false;
                }
                default -> {
                    view.showError("Invalid choice!");
                    return true;
                }
            }
        } catch (Exception e) {
            logger.error("NPC_SYSTEM", "Error in quest menu", e);
            return false;
        }
    }

    /**
     * Show available quests
     */
    private void showQuests(NPC npc) {
        DynArray<NPC.Quest> quests = npc.getAvailableQuests();

        if (quests.size() == 0) {
            view.clear();
            view.printLine("No quests available right now.");
            view.pressAnyKey();
            return;
        }

        // TODO: Implement quest browsing UI
        view.clear();
        view.printLine("Quests would be displayed here.");
        view.printLine("(Quest system framework ready, UI coming soon)");
        view.pressAnyKey();
    }

    /**
     * Show recruiter menu
     */
    private boolean showRecruiterMenu(NPC npc) {
        view.printLine("[1] Learn more about joining");
        view.printLine("[2] Leave");
        view.printLine("");
        view.print("Choice > ");

        try {
            String input = view.readLine().trim();

            switch (input) {
                case "1" -> {
                    view.clear();
                    view.printLine("Party system coming soon!");
                    view.printLine("Companions will be a major feature in the future.");
                    view.pressAnyKey();
                    return true;
                }
                case "2" -> {
                    return false;
                }
                default -> {
                    view.showError("Invalid choice!");
                    return true;
                }
            }
        } catch (Exception e) {
            logger.error("NPC_SYSTEM", "Error in recruiter menu", e);
            return false;
        }
    }
}
