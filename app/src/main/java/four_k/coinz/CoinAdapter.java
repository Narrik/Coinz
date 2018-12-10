package four_k.coinz;

import android.content.Context;
import android.icu.text.DecimalFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

public class CoinAdapter extends ArrayAdapter<Coin> {
    private Context context;
    private ArrayList<Coin> coins;

    public CoinAdapter(Context context, ArrayList<Coin> coins) {
        super(context, 0, coins);
        this.context = context;
        this.coins = coins;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // Get the data item for this position
        Coin coin = getItem(position);
        // Check if an existing view is being reused, otherwise inflate the view
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.coin_list, parent, false);
        }
        // Lookup view for data population
        ImageView currencyIcon = convertView.findViewById(R.id.ivCurrencyIcon);
        TextView coinInfo = convertView.findViewById(R.id.tvCoinInfo);
        TextView goldInfo = convertView.findViewById(R.id.tvGoldinfo);
        CheckBox checkBox = convertView.findViewById(R.id.checkBox);
        // Populate the data into the template view using the data object
        DecimalFormat df = new DecimalFormat("#.##");
        String curCoin = df.format(coin.getValue())+" "+coin.getCurrency();
        String curGold = "worth "+ " "  +" gold";
        coinInfo.setText(curCoin);
        goldInfo.setText(curGold);
        // Set currency icon based on currency
        if (coin.getCurrency().equals("DOLR")) {
            currencyIcon.setImageDrawable(getContext().getDrawable(R.drawable.dolr_icon));
        } else if (coin.getCurrency().equals("QUID")) {
            currencyIcon.setImageDrawable(getContext().getDrawable(R.drawable.quid_icon));
        } else if (coin.getCurrency().equals("PENY")) {
            currencyIcon.setImageDrawable(getContext().getDrawable(R.drawable.peny_icon));
        } else if (coin.getCurrency().equals("SHIL")) {
            currencyIcon.setImageDrawable(getContext().getDrawable(R.drawable.shil_icon));
        }

        // Add on click listener for checkbox
        checkBox.setOnClickListener(v -> {
            final boolean isChecked = checkBox.isChecked();
                if (isChecked){
                    Toast.makeText(v.getContext(), coins.get(position).getValue()+" selected", Toast.LENGTH_SHORT).show();
                }
        });
        // Return the completed view to render on screen
        return convertView;
    }
}