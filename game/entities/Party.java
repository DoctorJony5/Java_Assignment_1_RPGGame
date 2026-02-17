// Jonathan Deconde - 3196362
package game.entities;

import util.DynArray;

// Party container for companions
public class Party {
    private DynArray<PartyMember> members;
    private static final int MAX_PARTY_SIZE = 3;

    public Party() {
        this.members = new DynArray<>();
    }

    public boolean addMember(PartyMember member) {
        if (member == null || members.size() >= MAX_PARTY_SIZE) {
            return false;
        }
        members.add(member);
        return true;
    }

    public boolean removeMember(PartyMember member) {
        return members.remove(member);
    }

    public DynArray<PartyMember> getMembers() {
        return members;
    }

    public DynArray<PartyMember> getLivingMembers() {
        DynArray<PartyMember> living = new DynArray<>();
        for (int i = 0; i < members.size(); i++) {
            PartyMember member = members.get(i);
            if (member.isAlive()) {
                living.add(member);
            }
        }
        return living;
    }

    public boolean isPartyDefeated() {
        return getLivingMembers().size() == 0;
    }

    public int size() {
        return members.size();
    }

    public int getMaxSize() {
        return MAX_PARTY_SIZE;
    }
}
