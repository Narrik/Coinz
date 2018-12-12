package four_k.coinz;

import android.content.Context;
import android.icu.text.DecimalFormat;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

@SuppressWarnings("unused, FieldCanBeLocal")
public class CoinAdapter extends ArrayAdapter<Coin> {
    private Context context;
    private ArrayList<Coin> coins;
    private final String TAG = "Coin Adapter";
    private boolean[] checkBoxState;
    private HashMap<Coin, Boolean> checkedForCoin = new HashMap<>();
    private Map userInfo;


    CoinAdapter(Context context, ArrayList<Coin> coins) {
        super(context, 0, coins);
        this.context = context;
        this.coins = coins;
    }

    @Override
    @NonNull
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        // Get current user
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        // If there is no user, don't continue
        if (currentUser == null) {
            Log.d(TAG, "Cannot load bank if user is not logged in");
        }
        // Access our database
        FirebaseFirestore database = FirebaseFirestore.getInstance();
        FirebaseFirestoreSettings settings = new FirebaseFirestoreSettings.Builder()
                .setTimestampsInSnapshotsEnabled(true)
                .build();
        database.setFirestoreSettings(settings);
        // Get current user information
        if (currentUser != null) {
            DocumentReference userData = database.collection("Users").document(currentUser.getUid());
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
            DecimalFormat df = new DecimalFormat("0.00");
            // Set coin icon based on currency
            if (coin != null) {
                switch (coin.getCurrency()) {
                    case "DOLR":
                        currencyIcon.setImageDrawable(getContext().getDrawable(R.drawable.dolr_icon));
                        break;
                    case "QUID":
                        currencyIcon.setImageDrawable(getContext().getDrawable(R.drawable.quid_icon));
                        break;
                    case "PENY":
                        currencyIcon.setImageDrawable(getContext().getDrawable(R.drawable.peny_icon));
                        break;
                    case "SHIL":
                        currencyIcon.setImageDrawable(getContext().getDrawable(R.drawable.shil_icon));
                        break;
                }
                // Show value and currency of coin
                String curCoin = df.format(coin.getValue()) + " " + coin.getCurrency();
                coinInfo.setText(curCoin);
                // Show GOLD value based on exchange rates
                userData.get().addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        // Get user to info to get exchange rates
                        userInfo = task.getResult().getData();
                        // Get coin value in gold by multiplying coin's value and it's currency exchange rate
                        String coinValue = df.format(coin.getValue() * Double.parseDouble(userInfo.get(coin.getCurrency()).toString()));
                        goldInfo.setText("= " + coinValue + " GOLD");
                    } else {
                        Log.d(TAG, "Get failed with " + task.getException());
                    }
                });

                checkBoxState = new boolean[coins.size()];
                checkBox.setChecked(checkBoxState[position]);
                checkBox.setOnClickListener(v -> {
                    if (((CheckBox) v).isChecked()) {
                        checkBoxState[position] = true;
                        isChecked(position, true);
                    } else {
                        checkBoxState[position] = false;
                        isChecked(position, false);
                    }
                });

                //if country is in checkedForCountry then set the checkBox to true
                if (checkedForCoin.get(coin) != null) {
                    checkBox.setChecked(checkedForCoin.get(coin));
                }
                //Set tag to all checkBox
                checkBox.setTag(coin.getValue());
                // Return the completed view to render on screen
            }
        }
        return convertView;
    }

    private void isChecked(int position, boolean flag) {
        checkedForCoin.put(this.coins.get(position), flag);
    }


    public LinkedList<String> getSelectedCoins() {
        LinkedList<String> selectedCoins = new LinkedList<>();
        for (Map.Entry<Coin, Boolean> pair : checkedForCoin.entrySet()) {
            if (pair.getValue()) {
                selectedCoins.add(pair.getKey().getId());
            }
        }
        return selectedCoins;
    }

    public Double getSelectedCoinsGoldValue() {
        Double totalGoldValue = 0d;
        for (Map.Entry<Coin, Boolean> pair : checkedForCoin.entrySet()) {
            if (pair.getValue()) {
                totalGoldValue += pair.getKey().getValue() * Double.parseDouble(userInfo.get(pair.getKey().getCurrency()).toString());
            }
        }
        return totalGoldValue;
    }
}