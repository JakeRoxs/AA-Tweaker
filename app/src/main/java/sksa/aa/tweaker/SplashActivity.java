package sksa.aa.tweaker;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import sksa.aa.tweaker.Utils.Version;

import static sksa.aa.tweaker.MainActivity.runSuWithCmd;

public class SplashActivity extends AppCompatActivity {


    Context context;
    String newVersionName;

    private static final String actualVersion = BuildConfig.VERSION_NAME;
    private static final String BASE_URL = "https://api.github.com/repos/shmykelsa/AA-Tweaker/releases/latest";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_splash);

        final Intent intent = new Intent(this, MainActivity.class);

        String CountUsers = runSuWithCmd(
                getApplicationInfo().dataDir + "/sqlite3 -batch /data/data/com.google.android.gms/databases/phenotype.db " +
                        "'SELECT COUNT(DISTINCT USER) FROM ApplicationTags WHERE user !=\"\";'").getInputStreamLog();
        int UserCount = 1;

        try {
            // Sometimes output returns empty string
            UserCount = Integer.parseInt(CountUsers);
        } catch (Exception e) { }

        intent.putExtra("users" , UserCount);
        final NoRootDialog noRootDialog = new NoRootDialog();
        final StreamLogs isDeviceRooted =  runSuWithCmd("echo 1");

        copyAssets();

        SharedPreferences sharedPreferences = getSharedPreferences("MainActivity", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean("aa_speed_hack", false);
        editor.putBoolean("aa_six_tap", false);
        editor.putBoolean("aa_startup_policy", false);
        editor.putBoolean("aa_patched_apps", false);
        editor.putBoolean("aa_message_autoread", false);
        editor.putBoolean("aa_battery_outline", false);
        editor.putBoolean("force_ws", false);
        editor.putBoolean("force_no_ws", false);
        editor.putBoolean("aa_hun_ms", false);
        editor.putBoolean("aa_media_hun", false);
        editor.putBoolean("multi_display", false);
        editor.putBoolean("battery_saver_warning", false);
        editor.putBoolean("kill_telemetry", false);
        editor.putBoolean("calendar_aa_tweak", false);
        editor.putBoolean("aa_bitrate_usb", false);
        editor.putBoolean("aa_bitrate_wireless", false);
        editor.putBoolean("coolwalk_daynight_tweak", false);
        editor.putBoolean("aa_activate_coolwalk", false);
        editor.putBoolean("aa_activate_declinesms", false);
        editor.putBoolean("aa_activate_assistant_tips", false);
        editor.putBoolean("aa_new_seekbar", false);
        editor.putBoolean("uxprototype_tweak", false);
        editor.putBoolean("aa_material_you", false);
        editor.commit();

        requestLatest();



        final Button continueButton = findViewById(R.id.proceed_button);
        continueButton.setEnabled(false);
        Log.v("sksa.aa.tweaker", "Engaging countdown");
        new CountDownTimer(5000, 10) {
            public void onTick(long millisUntilFinished) {
                int secondsRemaining = (int) ( 1 + (millisUntilFinished/1000));
                continueButton.setText(getString(R.string.proceed) + " (" + secondsRemaining + ")");
            }

            @Override
            public void onFinish() {
                continueButton.setEnabled(true);
                continueButton.setText(R.string.proceed);
            }
        }.start();

        continueButton.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if (isDeviceRooted.getInputStreamLog().equals("1")) {
                            if (newVersionName != null) {
                                intent.putExtra("NewVersionName", newVersionName);
                            }
                            startActivity(intent);
                            finish();
                        } else {
                            noRootDialog.show(getSupportFragmentManager(), "NoRootDialog");
                        }
                    }
                });
    }


    private void copyFile(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[1024];
        int read;
        while((read = in.read(buffer)) != -1){
            out.write(buffer, 0, read);
        }
    }

    private void copyAssets() {
        String path = getApplicationInfo().dataDir;
        File file = new File(path, "sqlite3");
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Log.v("sksa.aa.tweaker", "\n--  Copy sqlite3 to data directory  --");
                }
            });
            InputStream in;
            OutputStream out;
            try {
                in = this.getResources().openRawResource(R.raw.sqlite3);

                String outDir = getApplicationInfo().dataDir;

                File outFile = new File(outDir, "sqlite3");

                out = new FileOutputStream(outFile);
                copyFile(in, out);
                in.close();
                out.flush();
                out.close();
            } catch (IOException e) {
                Log.e("sksa.aa.tweaker", "Failed to copy asset file: sqlite3", e);
            }
            Log.v("sksa.aa.tweaker", runSuWithCmd("chmod 777 " + path + "/sqlite3").getStreamLogsWithLabels());

    }

    public String requestLatest() {

        RequestQueue queue = Volley.newRequestQueue(this.getApplicationContext());

        final JsonObjectRequest jsonObjectRequest = new JsonObjectRequest
                (Request.Method.GET, BASE_URL, null, new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            String fetchedVersion = response.getString("tag_name");
                            Version actualCheck = new Version(actualVersion);
                            Version newCheck = new Version(fetchedVersion.substring(1));


                            if (actualCheck.compareTo(newCheck) == -1) {
                                newVersionName = fetchedVersion.substring(1);
                            }


                        } catch (JSONException e) {
                            newVersionName = null;
                            e.printStackTrace();
                        }

                    }
                }, new Response.ErrorListener() {


                    @Override
                    public void onErrorResponse(VolleyError error) {
                        newVersionName = null;
                    }
                });


        queue.add(jsonObjectRequest);
        return this.newVersionName;
    }




}