package com.moutamid.currencyconverter;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;
import com.moutamid.currencyconverter.databinding.ActivityMainBinding;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    ActivityMainBinding binding;
    AdRequest adRequest;
    RequestQueue requestQueue;
    private InterstitialAd mInterstitialAd;
    private static final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        Constants.checkApp(this);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        requestQueue = VolleySingleton.getInstance(this).getRequestQueue();

        new Thread(() -> MobileAds.initialize(this, initializationStatus -> {
        })).start();

        adRequest = new AdRequest.Builder().build();
        AdView adView = new AdView(this);
        adView.setAdSize(getAdSize());
        adView.setAdUnitId(getString(R.string.BANNER_ID));
        binding.adContainerView.removeAllViews();
        binding.adContainerView.addView(adView);
        AdRequest adRequest = new AdRequest.Builder().build();
        adView.loadAd(adRequest);

        binding.currencyRate.setHasFixedSize(false);
        binding.currencyRate.setLayoutManager(new LinearLayoutManager(this));

        String[] world_currencies = getResources().getStringArray(R.array.world_currencies);
        ArrayAdapter<String> base = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, world_currencies);
        ArrayAdapter<String> convert = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, world_currencies);

        binding.baseList.setAdapter(base);
        binding.convertList.setAdapter(convert);

        binding.convert.setOnClickListener(v -> {
            if (valid()) {
                getData(binding.base.getEditText().getText().toString().trim(), binding.convertTo.getEditText().getText().toString().trim());
            }
        });

        InterstitialAd.load(this, getString(R.string.Interstitial_ID), adRequest,
                new InterstitialAdLoadCallback() {
                    @Override
                    public void onAdLoaded(@NonNull InterstitialAd interstitialAd) {
                        // The mInterstitialAd reference will be null until
                        // an ad is loaded.
                        mInterstitialAd = interstitialAd;
                        loadInterstitialAd();
                        Log.i(TAG, "onAdLoaded");
                    }

                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                        // Handle the error
                        Log.d(TAG, loadAdError.toString());
                        mInterstitialAd = null;
                    }
                });
    }

    private boolean valid() {
        if (binding.base.getEditText().getText().toString().isEmpty()) {
            Toast.makeText(this, "Base Currency is Empty", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (binding.base.getEditText().getText().toString().trim().length() < 3) {
            Toast.makeText(this, "Base Currency is not valid", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (binding.convertTo.getEditText().getText().toString().isEmpty()) {
            Toast.makeText(this, "Convert Currency is Empty", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (binding.convertTo.getEditText().getText().toString().trim().length() < 3) {
            Toast.makeText(this, "Convert Currency is not valid", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    private void loadInterstitialAd() {
        mInterstitialAd.setFullScreenContentCallback(new FullScreenContentCallback() {
            @Override
            public void onAdClicked() {
                // Called when a click is recorded for an ad.
                Log.d(TAG, "Ad was clicked.");
            }

            @Override
            public void onAdDismissedFullScreenContent() {
                // Called when ad is dismissed.
                // Set the ad reference to null so you don't show the ad a second time.
                Log.d(TAG, "Ad dismissed fullscreen content.");
                mInterstitialAd = null;
            }

            @Override
            public void onAdFailedToShowFullScreenContent(AdError adError) {
                // Called when ad fails to show.
                Log.e(TAG, "Ad failed to show fullscreen content.");
                mInterstitialAd = null;
            }

            @Override
            public void onAdImpression() {
                // Called when an impression is recorded for an ad.
                Log.d(TAG, "Ad recorded an impression.");
            }

            @Override
            public void onAdShowedFullScreenContent() {
                // Called when ad is shown.
                Log.d(TAG, "Ad showed fullscreen content.");
            }
        });
        if (mInterstitialAd != null) {
            mInterstitialAd.show(MainActivity.this);
        } else {
            Log.d("TAG", "The interstitial ad wasn't ready yet.");
        }
    }

    private void getData(String base, String convert) {
        String link = Constants.api(base, convert);
        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setCancelable(false);
        progressDialog.setMessage("Please Wait...");
        progressDialog.show();
        JsonObjectRequest objectRequest = new JsonObjectRequest(Request.Method.GET, link, null, response -> {
            progressDialog.dismiss();
            try {
                JSONObject data = response.getJSONObject("data");
                JSONObject code = data.getJSONObject(convert);
                double value = code.getDouble("value");
                loadData(value);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }, error -> {
            error.printStackTrace();
            progressDialog.dismiss();
            Toast.makeText(this, error.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
        });
        requestQueue.add(objectRequest);
    }

    private void loadData(double value) {
        ArrayList<CurrencyModel> list = new ArrayList<>();
        for (int i = 1; i <= 40; i++) {
            int baseAmount = i * 100;
            double convertedAmount = baseAmount * value;
            String base = binding.base.getEditText().getText().toString().toUpperCase(Locale.ROOT) + " " + String.format(Locale.getDefault(),"%,d.00", baseAmount);
            String convert = binding.convertTo.getEditText().getText().toString().toUpperCase(Locale.ROOT) + " " + String.format(Locale.getDefault(),"%,.2f", convertedAmount);
            list.add(new CurrencyModel(base, convert));
        }
        CurrencyAdapter adapter = new CurrencyAdapter(this, list);
        binding.currencyRate.setAdapter(adapter);
    }

    private AdSize getAdSize() {
        Display display = getWindowManager().getDefaultDisplay();
        DisplayMetrics outMetrics = new DisplayMetrics();
        display.getMetrics(outMetrics);
        float density = outMetrics.density;
        float adWidthPixels = binding.adContainerView.getWidth();
        if (adWidthPixels == 0) {
            adWidthPixels = outMetrics.widthPixels;
        }
        int adWidth = (int) (adWidthPixels / density);
        return AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(this, adWidth);
    }

}