package com.terasoltechnologies.cashfreepgdev;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.gocashfree.cashfreesdk.CFPaymentActivity;
import com.gocashfree.cashfreesdk.CFPaymentService;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;

public class MainActivity extends AppCompatActivity {

    ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button click_pay = findViewById(R.id.click_pay);

        click_pay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String amount = "600";
                progressDialog = ProgressDialog.show(MainActivity.this, "Processing...", " Please Wait...", true, false);
                new createOrder().execute(user_id, user_token, loan_id, amount, emi_id);
            }
        });
    }

    @SuppressLint("StaticFieldLeak")
    class createOrder extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... strings) {
            try {
                String url_parameter = "amount=" + URLEncoder.encode(strings[3], "UTF-8");
                Log.d("CashFreePGLOG", url_parameter);

                URL api = new URL("http://example.com/create_repayment.php");

                HttpURLConnection conn = (HttpURLConnection) api.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                conn.setUseCaches(false);
                conn.setDoOutput(true);

                BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(conn.getOutputStream(), StandardCharsets.UTF_8));
                writer.write(url_parameter);
                writer.flush();
                writer.close();

                if (conn.getResponseCode() == 200) {
                    String line;
                    StringBuilder response = new StringBuilder();
                    BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    while ((line = br.readLine()) != null) {
                        response.append(line);
                    }
                    return response.toString();
                } else {
                    return "301";
                }
            } catch (Exception e) {
                e.printStackTrace();
                return "301";
            }
        }

        @Override
        protected void onPostExecute(String response) {
            Log.d("CashFreePGLOG", response);
            progressDialog.dismiss();
            if (!response.equals("301")) {
                try {
                    JSONObject response_data = new JSONObject(response);
                    if (response_data.getString("status").equals("200")) {
                        Log.d("CashFreePGLog", response_data.getString("data"));

                        response_data = new JSONObject(response_data.getString("data"));

                        Map<String, String> map = new HashMap<String, String>();
                        map.put("appId", "Your APP ID from Cash Free");
                        map.put("orderId", response_data.getString("order_id"));
                        map.put("orderAmount", response_data.getString("amount"));
                        map.put("customerPhone", "customerPhone");
                        map.put("customerEmail", "customerEmail");
                        map.put("notifyUrl", "http://example.com/create_repayment_pg_response.php");

                        CFPaymentService.getCFPaymentServiceInstance().doPayment(MainActivity.this, map, response_data.getString("token"), "TEST");
                    } else {
                        Toast.makeText(getApplicationContext(), response_data.getString("message"), Toast.LENGTH_LONG).show();
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                    Toast.makeText(getApplicationContext(), "Sorry! I was not able to process the data! Contact support team", Toast.LENGTH_LONG).show();
                }
            } else {
                Toast.makeText(getApplicationContext(), "Oh no! I cannot connect to internet! Confirm it please.", Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        //Same request code for all payment APIs.
        Log.d("CashFreePGLOG", "ReqCode : " + CFPaymentService.REQ_CODE);
        Log.d("CashFreePGLOG", "API Response : ");
        //Prints all extras. Replace with app logic.
        if (data != null) {
            Bundle bundle = data.getExtras();
            if (bundle != null)
                for (String key : bundle.keySet()) {
                    if (bundle.getString(key) != null) {
                        Log.d("CashFreePGLOG", key + " : " + bundle.getString(key));
                    }
                }
        }
    }
}
