// Jonathan Decondé - 3196362
package game.systems;

import game.ElementType;
import game.ItemType;
import game.Rarity;
import game.entities.Player;
import game.items.Item;
import game.ui.View;
import util.DynArray;
import util.Logger;

public class CraftingSystem {
    private Player player;
    private View view;
    private Logger logger;

    private static class Recipe {
        String name;
        ItemType result;
        int materialsNeeded;
        Rarity minRarity;
        String description;

        Recipe(String name, ItemType result, int materialsNeeded, Rarity minRarity, String description) {
            this.name = name;
            this.result = result;
            this.materialsNeeded = materialsNeeded;
            this.minRarity = minRarity;
            this.description = description;
        }
    }

    private DynArray<Recipe> recipes;

    public CraftingSystem(Player player, View view) {
        this.player = player;
        this.view = view;
        this.logger = Logger.getInstance();
        this.recipes = new DynArray<>();
        initRecipes();
    }

    private void initRecipes() {
        // Potion crafting only (per request)
        recipes.add(new Recipe(
                "Greater Health Potion",
                ItemType.POTION_HEALTH,
                2,
                Rarity.COMMON,
                "Combine 2+ common items to brew a health potion"));

        recipes.add(new Recipe(
                "Elixir of Vitality",
                ItemType.POTION_HEALTH,
                3,
                Rarity.RARE,
                "Combine 3+ rare items to create a powerful elixir"));

        logger.debug("CRAFTING", "Initialized " + recipes.size() + " recipes");
    }

    public void openCraftingMenu() {
        logger.info("CRAFTING", "Player opened crafting menu");

        while (true) {
            view.clear();
            view.printCentered("═══ CRAFTING STATION ═══");
            view.printLine("");
            view.printLine("Available Recipes:");
            view.printLine("");

            for (int i = 0; i < recipes.size(); i++) {
                Recipe r = recipes.get(i);
                view.printLine("[" + (i + 1) + "] " + r.name);
                view.printLine("    " + r.description);
                view.printLine("    Requires: " + r.materialsNeeded + "+ " + r.minRarity + " items");
                view.printLine("");
            }

            view.printLine("[0] Back to exploration");
            view.printLine("");
            view.print("Select recipe number > ");

            String input = view.readLine().trim();
            logger.debug("CRAFTING", "Player input: " + input);

            if (input.equals("0")) {
                logger.info("CRAFTING", "Player exited crafting menu");
                break;
            }

            try {
                int choice = Integer.parseInt(input);
                if (choice >= 1 && choice <= recipes.size()) {
                    Recipe recipe = recipes.get(choice - 1);
                    attemptCraft(recipe);
                } else {
                    view.showWarning("Invalid recipe number!");
                }
            } catch (NumberFormatException e) {
                view.showWarning("Please enter a number!");
            }

            view.pressAnyKey();
        }
    }

    private void attemptCraft(Recipe recipe) {
        logger.info("CRAFTING", "Attempting to craft: " + recipe.name);

        // Find suitable materials from inventory
        DynArray<Item> suitableMaterials = new DynArray<>();
        DynArray<Item> inventory = player.getInventory();

        for (int i = 0; i < inventory.size(); i++) {
            Item item = inventory.get(i);
            if (item.getRarity().ordinal() >= recipe.minRarity.ordinal()) {
                suitableMaterials.add(item);
            }
        }

        if (suitableMaterials.size() < recipe.materialsNeeded) {
            view.showWarning("Not enough materials!");
            view.printLine("You need at least " + recipe.materialsNeeded + " items of " +
                    recipe.minRarity + " rarity or higher.");
            view.printLine("You have: " + suitableMaterials.size() + " suitable items.");
            logger.warn("CRAFTING", "Insufficient materials for " + recipe.name);
            return;
        }

        // Show materials selection
        view.printLine("");
        view.printLine("Select " + recipe.materialsNeeded + " materials to use:");
        view.printLine("");

        for (int i = 0; i < suitableMaterials.size(); i++) {
            view.printLine("[" + (i + 1) + "] " + suitableMaterials.get(i).getName() +
                    " (" + suitableMaterials.get(i).getRarity() + ")");
        }

        view.printLine("");
        view.printLine("Enter material numbers separated by spaces (e.g., '1 2 3'):");
        view.print("> ");

        String input = view.readLine().trim();
        String[] selections = input.split("\\s+");

        if (selections.length < recipe.materialsNeeded) {
            view.showWarning("Not enough materials selected!");
            return;
        }

        // Remove selected materials
        DynArray<Item> materialsToRemove = new DynArray<>();
        try {
            for (int i = 0; i < recipe.materialsNeeded; i++) {
                int idx = Integer.parseInt(selections[i]) - 1;
                if (idx >= 0 && idx < suitableMaterials.size()) {
                    materialsToRemove.add(suitableMaterials.get(idx));
                } else {
                    view.showWarning("Invalid material selection!");
                    return;
                }
            }
        } catch (NumberFormatException e) {
            view.showWarning("Invalid input format!");
            return;
        }

        // Calculate result rarity based on materials used
        Rarity resultRarity = recipe.minRarity;
        for (int i = 0; i < materialsToRemove.size(); i++) {
            if (materialsToRemove.get(i).getRarity().ordinal() > resultRarity.ordinal()) {
                resultRarity = materialsToRemove.get(i).getRarity();
            }
        }

        // Remove materials from inventory
        for (int i = 0; i < materialsToRemove.size(); i++) {
            player.removeItem(materialsToRemove.get(i));
        }

        // Create result item with appropriate element
        ElementType craftElement = ElementType.NEUTRAL; // Default neutral element for crafted items
        Item craftedItem = new Item(recipe.name, recipe.result, craftElement, resultRarity);
        player.addItem(craftedItem);

        view.showSuccess("═══════════════════════════════════════════════════════");
        view.showSuccess("  CRAFTING SUCCESSFUL!");
        view.showSuccess("═══════════════════════════════════════════════════════");
        view.printLine("");
        view.printLine("You crafted: " + craftedItem.getName() + " (" + resultRarity + ")");
        view.printLine("Value: " + craftedItem.getValue() + " gold");

        logger.info("CRAFTING", "Successfully crafted " + craftedItem.getName() + " (" + resultRarity + ")");
    }
}
