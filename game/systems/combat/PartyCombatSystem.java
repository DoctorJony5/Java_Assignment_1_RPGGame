// Jonathan Deconde - 3196362
package game.systems.combat;

import game.entities.Party;
import game.entities.PartyMember;
import game.entities.Player;
import game.entities.monsters.Monster;
import game.entities.monsters.MonsterAbility;
import game.ui.View;
import util.ConsoleColor;
import util.DynArray;
import util.Logger;
import util.RNG;

// Prototype party combat system (player + companions vs single monster)
public class PartyCombatSystem {
    private Player player;
    private Party party;
    private Monster monster;
    private View view;
    private RNG rng;
    private Logger logger;

    private boolean playerDefending;
    private boolean monsterDefending;
    private DynArray<Boolean> companionDefending;

    public PartyCombatSystem(Player player, Party party, Monster monster, View view) {
        this.player = player;
        this.party = party;
        this.monster = monster;
        this.view = view;
        this.rng = new RNG();
        this.logger = Logger.getInstance();
        this.playerDefending = false;
        this.monsterDefending = false;
        this.companionDefending = new DynArray<>();

        for (int i = 0; i < party.size(); i++) {
            companionDefending.add(false);
        }
    }

    public void start() {
        logger.info("PARTY_COMBAT", "Party combat starting");
        view.clear();
        view.printLine(ConsoleColor.BRIGHT_RED + "=== PARTY COMBAT ===" + ConsoleColor.RESET);
        view.printLine(player.getName() + " and allies face " + monster.getName() + "!\n");
        view.pressAnyKey();

        while (player.isAlive() && !party.isPartyDefeated() && monster.isAlive()) {
            runRound();
        }

        if (!monster.isAlive()) {
            view.printColoredLine("Victory! The party defeated " + monster.getName() + "!",
                    ConsoleColor.BRIGHT_GREEN);
        } else {
            view.printColoredLine("Defeat... The party has fallen.", ConsoleColor.BRIGHT_RED);
        }
        view.pressAnyKey();
    }

    private void runRound() {
        DynArray<CombatActor> turnOrder = buildTurnOrder();

        for (int i = 0; i < turnOrder.size(); i++) {
            CombatActor actor = turnOrder.get(i);
            if (!monster.isAlive() || (!player.isAlive() && party.isPartyDefeated())) {
                break;
            }

            switch (actor.type) {
                case PLAYER -> playerTurn();
                case COMPANION -> companionTurn(actor.index);
                case MONSTER -> monsterTurn();
            }
        }
    }

    private DynArray<CombatActor> buildTurnOrder() {
        DynArray<CombatActor> actors = new DynArray<>();

        actors.add(new CombatActor(ActorType.PLAYER, -1, player.getSpeed()));

        for (int i = 0; i < party.getMembers().size(); i++) {
            PartyMember member = party.getMembers().get(i);
            if (member.isAlive()) {
                actors.add(new CombatActor(ActorType.COMPANION, i, member.getSpeed()));
            }
        }

        if (monster.isAlive()) {
            actors.add(new CombatActor(ActorType.MONSTER, -1, monster.getSpeed()));
        }

        // Simple selection sort by speed (descending)
        for (int i = 0; i < actors.size(); i++) {
            int best = i;
            for (int j = i + 1; j < actors.size(); j++) {
                if (actors.get(j).speed > actors.get(best).speed) {
                    best = j;
                }
            }
            if (best != i) {
                CombatActor tmp = actors.get(i);
                actors.set(i, actors.get(best));
                actors.set(best, tmp);
            }
        }

        return actors;
    }

    private void playerTurn() {
        if (!player.isAlive()) {
            return;
        }

        view.clear();
        showPartyStatus();
        view.printLine("\nYour turn:");
        view.printLine("[1] Attack");
        view.printLine("[2] Defend");
        view.printLine("[3] Rest");
        view.print("Choice > ");

        String input = view.readLine().trim();
        switch (input) {
            case "1" -> playerAttack();
            case "2" -> playerDefend();
            case "3" -> playerRest();
            default -> {
                view.showError("Invalid choice, defaulting to Attack.");
                playerAttack();
            }
        }
    }

    private void playerAttack() {
        int weaponDamage = player.getEquippedWeapon() != null ? player.getEquippedWeapon().getDamage() : 0;
        int baseDamage = 6 + player.getStrength() + weaponDamage + rng.nextInt(6);
        if (monsterDefending) {
            baseDamage = (int) (baseDamage * 0.75);
            monsterDefending = false;
        }
        monster.takeDamage(baseDamage);
        view.printColoredLine(player.getName() + " attacks for " + baseDamage + " damage!",
                ConsoleColor.BRIGHT_GREEN);
        view.pressAnyKey();
    }

    private void playerDefend() {
        playerDefending = true;
        view.printColoredLine(player.getName() + " braces for impact!", ConsoleColor.BRIGHT_CYAN);
        view.pressAnyKey();
    }

    private void playerRest() {
        player.restoreStamina((int) (player.getMaxStamina() * 0.25));
        view.printColoredLine(player.getName() + " recovers stamina.", ConsoleColor.BRIGHT_CYAN);
        view.pressAnyKey();
    }

    private void companionTurn(int index) {
        PartyMember member = party.getMembers().get(index);
        if (!member.isAlive()) {
            return;
        }

        Ability chosen = member.chooseAbility();
        if (chosen.isDefensive()) {
            companionDefending.set(index, true);
            view.printColoredLine(member.getName() + " takes a defensive stance!", ConsoleColor.BRIGHT_CYAN);
            view.pressAnyKey();
            return;
        }

        if (member.getStamina() < chosen.getStaminaCost()) {
            member.restoreStamina(10);
            view.printColoredLine(member.getName() + " regains stamina.", ConsoleColor.BRIGHT_CYAN);
            view.pressAnyKey();
            return;
        }

        member.useStamina(chosen.getStaminaCost());
        int damage = member.calculateDamage(chosen);
        if (monsterDefending) {
            damage = (int) (damage * 0.75);
            monsterDefending = false;
        }
        monster.takeDamage(damage);
        view.printColoredLine(member.getName() + " used " + chosen.getName() + " for " + damage + " damage!",
                ConsoleColor.BRIGHT_GREEN);
        view.pressAnyKey();
    }

    private void monsterTurn() {
        if (!monster.isAlive()) {
            return;
        }

        MonsterAbility ability = monster.chooseAbility();
        if (ability == null) {
            monster.restoreStamina();
            view.printColoredLine(monster.getName() + " recovers stamina.", ConsoleColor.BRIGHT_CYAN);
            view.pressAnyKey();
            return;
        }

        ability.use();
        monster.useStamina(ability.getStaminaCost());
        int damage = ability.calculateDamage(rng) + monster.getDamage();

        Target target = pickTarget();
        if (target.type == ActorType.PLAYER) {
            if (playerDefending) {
                damage = (int) (damage * 0.7);
                playerDefending = false;
            }
            player.takeDamage(damage);
            view.printColoredLine(monster.getName() + " hit " + player.getName() + " for " + damage + " damage!",
                    ConsoleColor.BRIGHT_RED);
            view.pressAnyKey();
            return;
        }

        PartyMember member = party.getMembers().get(target.index);
        if (companionDefending.get(target.index)) {
            damage = (int) (damage * 0.7);
            companionDefending.set(target.index, false);
        }
        member.takeDamage(damage);
        view.printColoredLine(monster.getName() + " hit " + member.getName() + " for " + damage + " damage!",
                ConsoleColor.BRIGHT_RED);
        view.pressAnyKey();
    }

    private Target pickTarget() {
        DynArray<Target> candidates = new DynArray<>();
        if (player.isAlive()) {
            candidates.add(new Target(ActorType.PLAYER, -1));
        }
        for (int i = 0; i < party.getMembers().size(); i++) {
            if (party.getMembers().get(i).isAlive()) {
                candidates.add(new Target(ActorType.COMPANION, i));
            }
        }

        int index = rng.nextInt(candidates.size());
        return candidates.get(index);
    }

    private void showPartyStatus() {
        view.printLine("\nEnemy: " + monster.getName() + " HP " + monster.getHealth() + "/" + monster.getMaxHealth());
        view.printLine("Player: " + player.getName() + " HP " + player.getHealth() + "/" + player.getMaxHealth());

        for (int i = 0; i < party.getMembers().size(); i++) {
            PartyMember member = party.getMembers().get(i);
            view.printLine("Ally: " + member.getName() + " HP " + member.getHealth() + "/" + member.getMaxHealth());
        }
    }

    private enum ActorType {
        PLAYER,
        COMPANION,
        MONSTER
    }

    private static class CombatActor {
        private ActorType type;
        private int index;
        private int speed;

        CombatActor(ActorType type, int index, int speed) {
            this.type = type;
            this.index = index;
            this.speed = speed;
        }
    }

    private static class Target {
        private ActorType type;
        private int index;

        Target(ActorType type, int index) {
            this.type = type;
            this.index = index;
        }
    }
}
