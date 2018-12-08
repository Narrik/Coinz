package four_k.coinz;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.AbsListView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.util.ArrayList;

public class BankActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bank);
        // Construct the data source
        Coin coin1 = new Coin("re",4.3333232,"DOLR",34,34);
        Coin coin2 = new Coin("re",1.32333232,"SHIL",34,34);
        ArrayList<Coin> coinsInWallet = new ArrayList<>();
        coinsInWallet.add(coin1);
        coinsInWallet.add(coin2);
        // Create the adapter to convert the array to views
        CoinAdapter adapter = new CoinAdapter(this, coinsInWallet);
        // Attach the adapter to a ListView
        ListView listView = findViewById(R.id.list_view);
        listView.setAdapter(adapter);
        listView.setChoiceMode(AbsListView.CHOICE_MODE_MULTIPLE);
    }
}
