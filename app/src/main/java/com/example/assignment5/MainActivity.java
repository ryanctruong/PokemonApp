package com.example.assignment5;

import androidx.appcompat.app.AppCompatActivity;

import android.content.ContentValues;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.androidnetworking.interfaces.JSONObjectRequestListener;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import com.androidnetworking.AndroidNetworking;
import com.androidnetworking.common.ANRequest;
import com.androidnetworking.common.Priority;
import com.androidnetworking.error.ANError;
import com.androidnetworking.interfaces.JSONArrayRequestListener;
import com.androidnetworking.interfaces.ParsedRequestListener;
import com.squareup.picasso.Picasso;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashSet;


public class MainActivity extends AppCompatActivity {

    TextView tvPokeNum, tvPokeWei, tvPokeHei, tvPokeBXP, tvPokeMove, tvPokeAbility, pokeName, pokeNum, pokeWei,
    pokeHei,pokeBaseXP, pokeMove, pokeAbility;

    Button searchBut, deleteBut, clearBut;
    EditText searchB;
    ImageView pokemonImg;
    ListView lv;
    SimpleCursorAdapter adapter;

    private final View.OnClickListener localListener = new View.OnClickListener(){
        @Override
        public void onClick(View v) {
            TextView tickerTextView = findViewById(R.id.searchBar);
            String ticker = tickerTextView.getText().toString();
            if(validResponse(ticker)){
                getInfo(ticker);
            } else{
                Toast.makeText(getApplicationContext(),"Incorrect Input", Toast.LENGTH_LONG).show();
            }

        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AndroidNetworking.initialize(getApplicationContext());
        setContentView(R.layout.activity_main);

        tvPokeNum = findViewById(R.id.tv_pokeNum);
        tvPokeWei = findViewById(R.id.tv_pokeWei);
        tvPokeHei = findViewById(R.id.tv_pokeHei);
        tvPokeBXP = findViewById(R.id.tv_pokeBXP);
        tvPokeMove = findViewById(R.id.tv_pokeMove);
        tvPokeAbility = findViewById(R.id.tv_pokeAbility);

        pokeName = findViewById(R.id.pokemonName);
        pokeNum = findViewById(R.id.pokeNumber);
        pokeWei = findViewById(R.id.pokeWeight);
        pokeHei = findViewById(R.id.pokeHeight);
        pokeBaseXP = findViewById(R.id.pokeBaseXP);
        pokeMove = findViewById(R.id.pokeMove);
        pokeAbility = findViewById(R.id.pokeAbility);

        searchBut = findViewById(R.id.searchButton);
        deleteBut = findViewById(R.id.deleteButton);
        clearBut = findViewById(R.id.clearButton);
        searchB = findViewById(R.id.searchBar);
        pokemonImg = findViewById(R.id.pokemonImage);
        lv = findViewById(R.id.listView);

        searchBut.setOnClickListener(localListener);
        deleteBut.setOnClickListener(delete_from_list);
        clearBut.setOnClickListener(clear_profile);

        lv.setOnItemClickListener(getProfile);
    }

    private void getInfo(String ticker){
        ANRequest req = AndroidNetworking.get("https://pokeapi.co/api/v2/pokemon/{ticker}")
                .addPathParameter("ticker", ticker)
                .setPriority(Priority.LOW)
                .build();

        req.getAsJSONObject(new JSONObjectRequestListener() {
            @Override
            public void onResponse(JSONObject response) {
                try {
                    String pokemon_name = response.getString("name");
                    pokeName.setText(firstLetter(pokemon_name));

                    String image_url = response.getJSONObject("sprites").
                            getJSONObject("other").getJSONObject("official-artwork").getString("front_default");
                    Picasso.get().load(image_url).into(pokemonImg);

                    String pokemon_id = response.getString("id");
                    tvPokeNum.setText(pokemon_id);

                    String pokemon_weight = response.getString("weight");
                    tvPokeWei.setText(pokemon_weight);

                    String pokemon_height = response.getString("height");
                    tvPokeHei.setText(pokemon_height);

                    String pokemon_basexp = response.getString("base_experience");
                    tvPokeBXP.setText(pokemon_basexp);

                    String pokemon_move = response.getJSONArray("moves").getJSONObject(0).getJSONObject("move").getString("name");
                    tvPokeMove.setText(pokemon_move);

                    String pokemon_ability = response.getJSONArray("abilities").getJSONObject(0).getJSONObject("ability").getString("name");
                    tvPokeAbility.setText(pokemon_ability);

                    ContentValues values = new ContentValues();

                    values.put(PokemonDB.COL1_NAME, tvPokeNum.getText().toString());

                    values.put(PokemonDB.COL2_NAME, pokeName.getText().toString());

                    getContentResolver().insert(PokemonDB.CONTENT_URI, values);

                    updateListUI();

                    Toast.makeText(getApplicationContext(),"Pokemon Found", Toast.LENGTH_LONG).show();
                } catch (JSONException e) {
                    throw new RuntimeException(e);
                }
            }

            @Override
            public void onError(ANError anError) {
                Toast.makeText(getApplicationContext(),"Pokemon Not Found", Toast.LENGTH_LONG).show();
            }
        });
    }

    public String firstLetter(String s){
        StringBuilder sb = new StringBuilder();
        for(int i = 0; i < s.length(); i++){
            if(i == 0){
                sb.append(Character.toUpperCase(s.charAt(i)));
            } else{
                sb.append(s.charAt(i));
            }
        }
        return sb.toString();
    }

    public boolean validResponse(String s){
        if(s.length() < 1){
            return false;
        }
        HashSet<Character> hs = new HashSet<>();
        hs.add('%'); hs.add('!');
        hs.add('&'); hs.add(';');
        hs.add('*'); hs.add(':');
        hs.add('('); hs.add('<');
        hs.add('@'); hs.add('>');
        hs.add(')');

        if(Character.isDigit(s.charAt(0))){
            return Integer.parseInt(s) <= 1010 && Integer.parseInt(s) > 0;
        }

        for(int i = 0; i < s.length(); i++){
            char a = s.charAt(i);
            if(hs.contains(a) || Character.isDigit(a)){
                return false;
            }
        }

        return true;
    }

    String[] fromColumns = {
            PokemonDB.COL1_NAME,
            PokemonDB.COL2_NAME,
    };

    int[] toViews = {
            R.id.tv_pokemon_national_number,
            R.id.tv_pokemon_name
    };

    public void updateListUI() {
        Cursor cursor = getContentResolver().query(PokemonDB.CONTENT_URI, null, null, null, null);
        adapter = new SimpleCursorAdapter(
                getApplicationContext(),
                R.layout.list_item_pokemon,
                cursor,
                fromColumns,
                toViews,
                0
        );

        lv.setAdapter(adapter);
    }

    View.OnClickListener delete_from_list = v -> {
        int deletedRows = getContentResolver().delete(PokemonDB.CONTENT_URI, null, null);

        if (deletedRows > 0) {
            Toast.makeText(getApplicationContext(), deletedRows + " Pokemon deleted", Toast.LENGTH_SHORT).show();
            updateListUI();
        } else {
            Toast.makeText(getApplicationContext(), "No Pokemon to delete", Toast.LENGTH_SHORT).show();
        }
    };


    View.OnClickListener clear_profile = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            searchB.setText("");
            pokeName.setText(R.string.blank);
            pokemonImg.setImageResource(R.drawable.ic_launcher_background);
            tvPokeNum.setText(R.string.blank);
            tvPokeWei.setText(R.string.blank);
            tvPokeHei.setText(R.string.blank);
            tvPokeBXP.setText(R.string.blank);
            tvPokeMove.setText(R.string.blank);
            tvPokeAbility.setText(R.string.blank);
        }
    };

    AdapterView.OnItemClickListener getProfile = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            Cursor cursor = (Cursor) parent.getItemAtPosition(position);
            String pokeID = cursor.getString(1);
            getInfo(pokeID);
        }
    };



}