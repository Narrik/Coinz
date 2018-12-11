package four_k.coinz;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.ArrayList;

public class MessageAdapter extends ArrayAdapter<Message> {
    private Context context;
    private ArrayList<Message> messages;
    private final String TAG = "Message Adapter";

    public MessageAdapter(Context context, ArrayList<Message> messages) {
        super(context, 0, messages);
        this.context = context;
        this.messages = messages;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        Message message = getItem(position);
        // Check if an existing view is being reused, otherwise inflate the view
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.message_list, parent, false);
        }
        // Lookup view for data population
        TextView sender = convertView.findViewById(R.id.tvSender);
        TextView messageText = convertView.findViewById(R.id.tvMessage);
        // Populate the data into the template view using the data object
        messageText.setText(message.getMessageText());
        sender.setText(message.getSender());
        // Return the completed view to render on screen
        return convertView;
    }
}