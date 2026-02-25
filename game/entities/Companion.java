package game.entities;

import game.ElementType;
import util.RNG;

public class Companion {
    public enum Role {
        VANGUARD,
        ROGUE,
        MAGE,
        HEALER,
        SUPPORT
    }

    public enum CompanionAction {
        ATTACK,
        HEAL,
        SUPPORT,
        HOLD,
        RETREAT
    }

    private String name;
    private Role role;
    private ElementType affinity;
    private int level;
    private int health;
    private int maxHealth;
    private int stamina;
    private int maxStamina;
    private int attack;
    private int defense;
    private int speed;
    private boolean alive;
    private boolean engaged;
    private int loyalty;
    private int cohesion;
    private String disengageReason;

    public Companion(String name, Role role, ElementType affinity, int level) {
        this.name = name;
        this.role = role;
        this.affinity = affinity;
        this.level = Math.max(1, level);
        this.maxHealth = 70 + (this.level * 8);
        this.health = maxHealth;
        this.maxStamina = 70 + (this.level * 5);
        this.stamina = maxStamina;
        this.attack = 10 + (this.level * 3);
        this.defense = 5 + (this.level * 2);
        this.speed = 8 + (this.level * 2);
        this.alive = true;
        this.engaged = true;
        this.loyalty = 70;
        this.cohesion = 70;
        this.disengageReason = "";
        applyRoleModifiers();
    }

    private void applyRoleModifiers() {
        switch (role) {
            case VANGUARD -> {
                maxHealth += 30;
                health = maxHealth;
                defense += 6;
                attack += 2;
                speed -= 1;
            }
            case ROGUE -> {
                speed += 6;
                attack += 4;
                defense -= 1;
            }
            case MAGE -> {
                attack += 7;
                maxStamina += 20;
                stamina = maxStamina;
                maxHealth -= 10;
                health = Math.max(1, maxHealth);
                cohesion += 5;
            }
            case HEALER -> {
                attack -= 2;
                defense += 1;
                maxStamina += 25;
                stamina = maxStamina;
                loyalty += 8;
            }
            case SUPPORT -> {
                maxStamina += 15;
                stamina = maxStamina;
                defense += 2;
                speed += 2;
                cohesion += 10;
            }
        }

        loyalty = Math.max(20, Math.min(100, loyalty));
        cohesion = Math.max(20, Math.min(100, cohesion));
    }

    public int performAttack(RNG rng) {
        if (!engaged || !alive) {
            return 0;
        }

        int variance = Math.max(2, attack / 4);
        int base = attack + rng.nextInt(-variance, variance + 1);
        int roleBonus = switch (role) {
            case VANGUARD -> 3;
            case ROGUE -> rng.nextInt(0, 6);
            case MAGE -> 5;
            case HEALER -> -1;
            case SUPPORT -> 1;
        };
        return Math.max(1, base + roleBonus);
    }

    public int trySupportHeal(double playerHealthRatio) {
        if (!alive || !engaged || !canHeal()) {
            return 0;
        }

        int cost = role == Role.HEALER ? 14 : 12;
        if (stamina < cost) {
            return 0;
        }

        double healThreshold = role == Role.HEALER ? 0.80 : 0.55;
        if (playerHealthRatio > healThreshold) {
            return 0;
        }

        stamina -= cost;
        return role == Role.HEALER ? 20 : 12;
    }

    public boolean canHeal() {
        return role == Role.HEALER || role == Role.SUPPORT;
    }

    public boolean canUseOffense() {
        return role != Role.HEALER || stamina > 10;
    }

    public CompanionAction chooseAction(double playerHealthRatio, int monsterThreat) {
        if (!alive || !engaged) {
            return CompanionAction.HOLD;
        }
        if (shouldDisengage(monsterThreat, false, cohesion)) {
            return CompanionAction.RETREAT;
        }
        if (canHeal() && playerHealthRatio <= (role == Role.HEALER ? 0.80 : 0.55)) {
            return role == Role.HEALER ? CompanionAction.HEAL : CompanionAction.SUPPORT;
        }
        if (canUseOffense()) {
            return CompanionAction.ATTACK;
        }
        return CompanionAction.HOLD;
    }

    public boolean shouldDisengage(int monsterThreat, boolean bossEncounter, int partyAverageCohesion) {
        if (!alive || !engaged) {
            return false;
        }

        int moralePower = (level * 12) + loyalty + cohesion + (partyAverageCohesion / 2);
        int bossPenalty = bossEncounter ? 30 : 0;
        int threatScore = monsterThreat + bossPenalty;

        if (threatScore > moralePower) {
            disengageReason = bossEncounter
                    ? "Overwhelmed by boss pressure (low morale)"
                    : "Morale broke under current threat";
            return true;
        }
        return false;
    }

    public void disengage(String reason) {
        this.engaged = false;
        this.disengageReason = reason == null ? "Disengaged" : reason;
    }

    public void reengage() {
        this.engaged = true;
        this.disengageReason = "";
    }

    public String getMoraleSummary() {
        String state = engaged ? "Ready" : "Disengaged";
        return String.format("%s [%s] L:%d C:%d (%s)", name, role.name(), loyalty, cohesion, state);
    }

    public void takeDamage(int rawDamage) {
        if (!engaged) {
            return;
        }
        int reduced = Math.max(1, rawDamage - defense);
        health = Math.max(0, health - reduced);
        alive = health > 0;
    }

    public void heal(int amount) {
        if (amount <= 0 || !alive) {
            return;
        }
        health = Math.min(maxHealth, health + amount);
    }

    public void restoreStamina(int amount) {
        stamina = Math.min(maxStamina, stamina + Math.max(0, amount));
    }

    public void fullRestoreStamina() {
        stamina = maxStamina;
    }

    public boolean isAlive() {
        return alive;
    }

    public boolean isEngaged() {
        return engaged;
    }

    public String getName() {
        return name;
    }

    public Role getRole() {
        return role;
    }

    public int getHealth() {
        return health;
    }

    public int getMaxHealth() {
        return maxHealth;
    }

    public int getStamina() {
        return stamina;
    }

    public int getAttack() {
        return attack;
    }

    public int getSpeed() {
        return speed;
    }

    public int getLoyalty() {
        return loyalty;
    }

    public int getCohesion() {
        return cohesion;
    }

    public String getDisengageReason() {
        return disengageReason;
    }

    public void adjustLoyalty(int delta) {
        loyalty = Math.max(0, Math.min(100, loyalty + delta));
    }

    public void adjustCohesion(int delta) {
        cohesion = Math.max(0, Math.min(100, cohesion + delta));
    }
}
