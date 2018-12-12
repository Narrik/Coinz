package four_k.coinz;

public class Message {

    private String sender;
    private String messageText;

    @SuppressWarnings("unused")
    // Necessary due to firebase
    public Message() {}

    @SuppressWarnings("WeakerAccess")
    // Necessary due to firebase
    public Message(String sender, String messageText){
        this.sender = sender;
        this.messageText = messageText;
    }

    public String getSender() {
        return sender;
    }

    public String getMessageText() {
        return messageText;
    }

}
