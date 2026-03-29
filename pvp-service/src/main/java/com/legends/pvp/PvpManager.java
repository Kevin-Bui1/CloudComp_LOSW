package game.pvp;
import ui.login.IUserDB;
import java.util.List;
public class PvpManager {
    private final IUserDB userDB;
    public PvpManager(IUserDB userDB) { this.userDB=userDB; }

    public boolean canInvite(String inviter, String opponent) {
        if (opponent==null||opponent.isBlank()||inviter.equals(opponent)) return false;
        if (!userDB.usernameExists(opponent)) return false;
        return userDB.hasSavedParty(inviter) && userDB.hasSavedParty(opponent);
    }
    public List<String> getSavedParties(String username) { return userDB.getSavedParties(username); }
    public void recordMatchResult(String winner, String loser) { userDB.recordPvpResult(winner, loser); }
}
