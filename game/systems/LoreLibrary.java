// Jonathan Decondé - 3196362

package game.systems;

import game.items.LoreItem;
import util.DynArray;
import util.RNG;
import util.Logger;

public class LoreLibrary {
    private DynArray<LoreItem> allLore; // I do perhaps overuse Dynamic Arrays
    private RNG rng;
    private Logger logger;
    
    public LoreLibrary(RNG rng) {
        this.rng = rng;
        this.logger = Logger.getInstance();
        this.allLore = new DynArray<>();
        initializeLore();
    }

    // All of this here is inspired by greek mythology, specifically Daedalus
    // The floor found isn't important.
    private void initializeLore() {
        allLore.add(new LoreItem(
            "Cracked Stone Tablet",
            "An ancient tablet with faded inscriptions",
            "...Daedalus, master craftsman of Athens, built this labyrinth as penance...\n" +
            "His genius, once celebrated, became his curse when King Minos imprisoned\n" +
            "him within his own creation. The irony was not lost on the inventor...",
            1
        ));
        
        allLore.add(new LoreItem(
            "Weathered Journal Page",
            "A torn page from an explorer's journal",
            "Day 47: The walls shift when I'm not looking. I swear the corridor I mapped\n" +
            "yesterday no longer exists. Daedalus designed this place to confound even\n" +
            "himself. How many others have died here, their bones scattered in rooms\n" +
            "that no longer exist?",
            2
        ));
        
        allLore.add(new LoreItem(
            "Bronze Plaque",
            "A tarnished bronze plaque with Greek text",
            "To those who enter: Know that Daedalus wept as he built each trap, each\n" +
            "corridor designed to mislead. This labyrinth was meant to contain the\n" +
            "Minotaur, but it has become a prison for countless souls. May the gods\n" +
            "forgive what necessity demanded.",
            3
        ));
        
        allLore.add(new LoreItem(
            "Icarus's Feather",
            "A preserved feather, impossibly ancient",
            "Found near the ceiling of a great chamber. A reminder of ambition's price.\n" +
            "Icarus flew too close to the sun,  was it hubris or desperation? To escape\n" +
            "his father's masterpiece, he chose the sky. The wax melted, and he fell.\n" +
            "Freedom has always demanded sacrifice.",
            4
        ));
        
        allLore.add(new LoreItem(
            "The Minotaur's Horn",
            "A curved horn, still stained with ancient blood",
            "The beast that once prowled these halls was both monster and victim.\n" +
            "Born of curse and shame, Asterion knew only darkness and hunger.\n" +
            "Theseus slew the Minotaur, but the labyrinth remembers its first resident.\n" +
            "Some say on quiet nights, you can still hear it roar.",
            5
        ));
        
        allLore.add(new LoreItem(
            "Theseus's Thread",
            "A golden thread, never tarnishing",
            "The thread of Ariadne, given to Theseus to guide him back from the depths.\n" +
            "He found his way out, but left her behind on Naxos. Heroes are often\n" +
            "remembered for their victories, not their betrayals. This thread has\n" +
            "guided many, but saved few.",
            6
        ));
        
        allLore.add(new LoreItem(
            "Daedalus's Blueprint",
            "A fragment of the original labyrinth design",
            "Impossible geometry rendered in precise lines. The blueprint shows corridors\n" +
            "that fold into themselves, rooms that exist in multiple places at once.\n" +
            "Daedalus didn't just build a maze—he created a pocket of reality where\n" +
            "the rules of the Gods don't apply.",
            7
        ));
        
        allLore.add(new LoreItem(
            "Minos's Crown Fragment",
            "A piece of the tyrant king's crown",
            "King Minos demanded this labyrinth to hide his shame. But in his cruelty,\n" +
            "he created something far worse than the Minotaur—a place where time and\n" +
            "space mean nothing. Minos is long dead, but his legacy of suffering endures\n" +
            "in every stone of this cursed place.",
            8
        ));
        
        allLore.add(new LoreItem(
            "The Architect's Compass",
            "An ornate compass that always points nowhere",
            "Daedalus's own compass, left behind or lost. It spins endlessly, unable to\n" +
            "find true north in a place that defies direction. A tool of creation became\n" +
            "useless in its own creation. Perhaps that's fitting—the maker trapped by\n" +
            "what he made.",
            9
        ));
        
        allLore.add(new LoreItem(
            "The Final Testament",
            "Daedalus's last written words",
            "If you read this, you have come far. Too far, perhaps. I built this labyrinth\n" +
            "to be inescapable, and so it is—even death offers no exit. My bones rest\n" +
            "somewhere in these halls, in a room I can no longer remember how to reach.\n" +
            "Turn back, if you still can. There is no treasure here worth finding.\n" +
            "Only endless corridors and the echo of old mistakes.\n" +
            "                                                    - Daedalus",
            10
        ));
        
        logger.debug("LORE", "Initialized " + allLore.size() + " lore items");
    }
    
    public LoreItem getRandomLore(int currentFloor) {
        // Filter lore items appropriate for current floor
        DynArray<LoreItem> suitableLore = new DynArray<>();
        for (int i = 0; i < allLore.size(); i++) {
            LoreItem lore = allLore.get(i);
            // Include lore from current floor or earlier
            if (lore.getFloorFound() <= currentFloor + 2) {
                suitableLore.add(lore);
            }
        }
        
        if (suitableLore.size() == 0) {
            return null;
        }
        
        return suitableLore.get(rng.nextInt(suitableLore.size()));
    }
}
