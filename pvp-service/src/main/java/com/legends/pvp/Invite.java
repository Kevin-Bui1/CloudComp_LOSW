package game.pvp;
public class Invite {
    private final String senderUsername, receiverUsername;
    private String senderParty, receiverParty;
    private boolean accepted;

    public Invite(String sender, String receiver) { this.senderUsername=sender; this.receiverUsername=receiver; }
    public void accept()  { accepted=true; }
    public void decline() { accepted=false; }
    public boolean isAccepted()           { return accepted; }
    public String getSenderUsername()     { return senderUsername; }
    public String getReceiverUsername()   { return receiverUsername; }
    public String getSenderParty()        { return senderParty; }
    public void   setSenderParty(String p){ senderParty=p; }
    public String getReceiverParty()      { return receiverParty; }
    public void   setReceiverParty(String p){ receiverParty=p; }
}
