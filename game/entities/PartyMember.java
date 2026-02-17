// Jonathan Deconde - 3196362
package game.entities;

import game.ElementType;
import game.systems.combat.Ability;
import util.DynArray;
import util.RNG;

// Party member for the enhanced party combat system
public class PartyMember {
    public enum Role {
        TANK,
        DPS,
        SUPPORT
    }

    private String name;
    private Role role;
    private ElementType affinity;
    private int level;
    private int health;
    private int maxHealth;
    private int stamina;
    private int maxStamina;
    private int strength;
    private int resilience;
    private int speed;
    private int intelligence;
    private int luck;
    private boolean alive;
    private DynArray<Ability> abilities;
    private RNG rng;

    public PartyMember(String name, Role role, int level) {
        this.name = name;
        this.role = role;
        this.level = Math.max(1, level);
        this.rng = new RNG();
        this.abilities = new DynArray<>();
        initializeStats();
        initializeAbilities();
    }

    private void initializeStats() {
        this.maxHealth = switch (role) {
            case TANK -> 140 + level * 8;
            case DPS -> 100 + level * 6;
            case SUPPORT -> 110 + level * 6;
        };
        this.health = maxHealth;
        this.maxStamina = 80 + level * 4;
        this.stamina = maxStamina;

        this.strength = switch (role) {
            case TANK -> 10 + level * 2;
            case DPS -> 14 + level * 3;
            case SUPPORT -> 8 + level * 2;
        };
        this.resilience = switch (role) {
            case TANK -> 12 + level * 2;
            case DPS -> 6 + level * 1;
            case SUPPORT -> 8 + level * 1;
        };
        this.speed = switch (role) {
            case TANK -> 8 + level;
            case DPS -> 12 + level;
            case SUPPORT -> 10 + level;
        };
        this.intelligence = 8 + level;
        this.luck = 5 + (level / 2);
        this.alive = true;
        this.affinity = ElementType.NEUTRAL;
    }

    private void initializeAbilities() {
        abilities.add(new Ability("Strike", "Basic attack", 10, 6, 12, false));

        switch (role) {
            case TANK -> {
                abilities.add(new Ability("Guard", "Reduce damage this turn", 12, 0, 0, true));
                abilities.add(new Ability("Shield Bash", "Stagger the enemy", 16, 10, 18, false));
            }
            case DPS -> {
                abilities.add(new Ability("Quick Slash", "Fast damage", 12, 8, 16, false));
                abilities.add(new Ability("Power Cut", "Heavy damage", 18, 14, 26, false));
            }
            case SUPPORT -> {
                abilities.add(new Ability("Bolster", "Defensive stance", 10, 0, 0, true));
                abilities.add(new Ability("Arcane Bolt", "Magical attack", 16, 12, 22, false));
            }
        }
    }

    public Ability chooseAbility() {
        // Simple AI: prefer offensive when stamina allows, else rest
        for (int i = 0; i < abilities.size(); i++) {
            Ability ability = abilities.get(i);
            if (!ability.isDefensive() && stamina >= ability.getStaminaCost()) {
                return ability;
            }
        }
        return abilities.get(0);
    }

    public int calculateDamage(Ability ability) {
        int base = ability.calculateDamage(rng);
        return base + strength;
    }

    public void takeDamage(int amount) {
        int reduced = Math.max(1, amount - resilience);
        health -= reduced;
        if (health <= 0) {
            health = 0;
            alive = false;
        }
    }

    public void heal(int amount) {
        health = Math.min(maxHealth, health + Math.max(0, amount));
    }

    public void restoreStamina(int amount) {
        stamina = Math.min(maxStamina, stamina + Math.max(0, amount));
    }

    public void useStamina(int amount) {
        stamina = Math.max(0, stamina - amount);
    }

    public String getName() {
        return name;
    }

    public Role getRole() {
        return role;
    }

    public int getLevel() {
        return level;
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

    public int getSpeed() {
        return speed;
    }

    public boolean isAlive() {
        return alive;
    }

    public DynArray<Ability> getAbilities() {
        return abilities;
    }
}
