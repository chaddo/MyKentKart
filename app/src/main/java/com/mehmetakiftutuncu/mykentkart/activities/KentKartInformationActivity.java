package com.mehmetakiftutuncu.mykentkart.activities;

import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.nfc.NfcAdapter;
import android.nfc.tech.NfcA;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.mehmetakiftutuncu.mykentkart.R;
import com.mehmetakiftutuncu.mykentkart.models.KentKart;
import com.mehmetakiftutuncu.mykentkart.models.KentKartInformation;
import com.mehmetakiftutuncu.mykentkart.tasks.GetKentKartInformationTask;
import com.mehmetakiftutuncu.mykentkart.utilities.Constants;
import com.mehmetakiftutuncu.mykentkart.utilities.Data;
import com.mehmetakiftutuncu.mykentkart.utilities.Log;
import com.mehmetakiftutuncu.mykentkart.utilities.NFCUtils;
import com.mehmetakiftutuncu.mykentkart.utilities.NetworkUtils;
import com.mehmetakiftutuncu.mykentkart.utilities.StringUtils;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;

import fr.nicolaspomepuy.discreetapprate.AppRate;
import fr.nicolaspomepuy.discreetapprate.RetryPolicy;
import ru.vang.progressswitcher.ProgressWidget;

public class KentKartInformationActivity extends ActionBarActivity implements GetKentKartInformationTask.OnKentKartInformationReadyListener {
    private enum States {PROGRESS, ERROR, SUCCESS}

    private States state;

    private ProgressWidget progressWidget;

    private RelativeLayout lastUseSectionLayout;
    private RelativeLayout lastLoadSectionLayout;

    private TextView balanceTextView;
    private TextView lastUseAmountTextView;
    private TextView lastUseTimeTextView;
    private TextView connectedTransportTextView;
    private TextView lastLoadAmountTextView;
    private TextView lastLoadTimeTextView;

    private String kentKartName;
    private String kentKartNumber;
    private String kentKartRegionCode;
    private boolean isStartedWithNfc = false;

    private NfcAdapter nfcAdapter;

    private KentKartInformation kentKartInformation;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_kentkart_information);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
        }

        Bundle args = getIntent().getExtras();
        if (args != null) {
            kentKartName       = args.getString(Constants.KENT_KART_NAME);
            kentKartNumber     = args.getString(Constants.KENT_KART_NUMBER);
            kentKartRegionCode = args.getString(Constants.KENT_KART_REGION_CODE);
            isStartedWithNfc   = args.getBoolean(Constants.HAS_NFC, false);
        }

        ActionBar actionBar = getSupportActionBar();
        actionBar.setHomeButtonEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);
        updateTitle(actionBar);

        progressWidget = (ProgressWidget) findViewById(R.id.progressWidget_kentKartInformation);

        lastUseSectionLayout = (RelativeLayout) findViewById(R.id.relativeLayout_kentKartInformationActivity_section_lastUse);
        lastLoadSectionLayout = (RelativeLayout) findViewById(R.id.relativeLayout_kentKartInformationActivity_section_lastLoad);

        balanceTextView = (TextView) findViewById(R.id.textView_kentKartInformationActivity_balance);
        lastUseAmountTextView = (TextView) findViewById(R.id.textView_kentKartInformationActivity_lastUseAmount);
        lastUseTimeTextView = (TextView) findViewById(R.id.textView_kentKartInformationActivity_lastUseTime);
        connectedTransportTextView = (TextView) findViewById(R.id.textView_kentKartInformationActivity_connectedTransport);
        lastLoadAmountTextView = (TextView) findViewById(R.id.textView_kentKartInformationActivity_lastLoadAmount);
        lastLoadTimeTextView = (TextView) findViewById(R.id.textView_kentKartInformationActivity_lastLoadTime);

        TextView balanceTLTextView = (TextView) findViewById(R.id.textView_kentKartInformationActivity_balanceTL);
        TextView lastUseTLTextView = (TextView) findViewById(R.id.textView_kentKartInformationActivity_lastUseAmountTL);
        TextView lastLoadTLTextView = (TextView) findViewById(R.id.textView_kentKartInformationActivity_lastLoadAmountTL);
        Typeface typeface = Typeface.createFromAsset(getAssets(), "TL.ttf");
        balanceTLTextView.setTypeface(typeface);
        lastUseTLTextView.setTypeface(typeface);
        lastLoadTLTextView.setTypeface(typeface);

        nfcAdapter = NFCUtils.get(getApplicationContext()).getAdapter();

        if (savedInstanceState != null) {
            restoreInstanceState(savedInstanceState);
        }

        handleIntent(getIntent());

        AppRate
            .with(this)
            .initialLaunchCount(3)
            .text(R.string.rate_app)
            .retryPolicy(RetryPolicy.INCREMENTAL)
            .checkAndShow();
    }

    @Override
    public void onResume() {
        super.onResume();

        if (nfcAdapter != null) {
            PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(), 0, new Intent(getApplicationContext(), KentKartInformationActivity.class).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
            IntentFilter intentFilter = new IntentFilter(NfcAdapter.ACTION_TECH_DISCOVERED);

            nfcAdapter.enableForegroundDispatch(this, pendingIntent, new IntentFilter[]{intentFilter}, new String[][]{new String[]{NfcA.class.getName()}});
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        if (nfcAdapter != null) {
            nfcAdapter.disableForegroundDispatch(this);
        }
    }

    @Override
    public void onNewIntent(Intent intent) {
        handleIntent(intent);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        // Save current state
        outState.putString(Constants.STATE, state.toString());

        // Save KentKart name and number
        outState.putString(Constants.KENT_KART_NAME, kentKartName);
        outState.putString(Constants.KENT_KART_NUMBER, kentKartNumber);
        outState.putString(Constants.KENT_KART_REGION_CODE, kentKartRegionCode);

        // Save KentKart information
        if (kentKartInformation != null) {
            outState.putParcelable(Constants.KENT_KART_INFORMATION, kentKartInformation);
        }
    }

    private void restoreInstanceState(Bundle savedState) {
        if (savedState != null) {
            // Restore current state
            changeState(States.valueOf(savedState.getString(Constants.STATE)));

            // Restore KentKart name and number
            kentKartName       = savedState.getString(Constants.KENT_KART_NAME);
            kentKartNumber     = savedState.getString(Constants.KENT_KART_NUMBER);
            kentKartRegionCode = savedState.getString(Constants.KENT_KART_REGION_CODE);

            // Restore KentKart information
            kentKartInformation = savedState.getParcelable(Constants.KENT_KART_INFORMATION);
            if (kentKartInformation != null) {
                showKentKartInformationResult(kentKartNumber, kentKartInformation);
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                goToKentKartList();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        goToKentKartList();
    }

    @Override
    public void onKentKartInformationReady(String kentKartNumber, KentKartInformation kentKartInformation) {
        showKentKartInformationResult(kentKartNumber, kentKartInformation);
    }

    private void showKentKartInformationResult(String kentKartNumber, KentKartInformation kentKartInformation) {
        if (StringUtils.isEmpty(kentKartNumber)) {
            Log.error(this, "Failed to get KentKart information, kentKartNumber is empty!");
            changeState(States.ERROR);
        } else if (kentKartNumber.length() != 11) {
            Log.error(this, "Failed to get KentKart information, kentKartNumber is invalid! kentKartNumber: " + kentKartNumber);
            changeState(States.ERROR);
        } else if (kentKartInformation == null) {
            Log.error(this, "Failed to get KentKart information, kentKartInformation is empty!");
            Toast.makeText(getApplicationContext(), getString(R.string.kentKartInformationActivity_blocked), Toast.LENGTH_LONG).show();
            changeState(States.ERROR);
        } else {
            this.kentKartInformation = kentKartInformation;

            balanceTextView.setText(String.valueOf(kentKartInformation.balance));

            boolean isLastUseAmountFound = kentKartInformation.lastUseAmount != -1;
            boolean isLastUseTimeFound = kentKartInformation.lastUseTime != -1;
            boolean isLastLoadAmountFound = kentKartInformation.lastLoadAmount != -1;
            boolean isLastLoadTimeFound = kentKartInformation.lastLoadTime != -1;

            if (!isLastUseAmountFound && !isLastUseTimeFound) {
                lastUseSectionLayout.setVisibility(View.GONE);
            } else {
                if (isLastUseAmountFound) {
                    lastUseAmountTextView.setText(String.valueOf(kentKartInformation.lastUseAmount));
                } else {
                    lastUseAmountTextView.setVisibility(View.GONE);
                }

                if (isLastUseTimeFound) {
                    lastUseTimeTextView.setText(new SimpleDateFormat(Constants.DATE_TIME_FORMAT).format(new Date(kentKartInformation.lastUseTime)));
                } else {
                    lastUseTimeTextView.setVisibility(View.GONE);
                }

                SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                if (preferences.getBoolean(Constants.PREFERENCE_CONNECTED_TRANSPORT_ENABLED, false)) {
                    String durationPreference = preferences.getString(Constants.PREFERENCE_CONNECTED_TRANSPORT_DURATION, "90");
                    long connectedTransportDuration = !durationPreference.isEmpty() ? Long.parseLong(durationPreference) * 60 * 1000 : -1;
                    long difference = System.currentTimeMillis() - kentKartInformation.lastUseTime;

                    if (difference >= 0 && difference < connectedTransportDuration && kentKartInformation.lastLoadAmount > 0) {
                        String differenceMinutes = String.valueOf(difference / (60 * 1000));
                        connectedTransportTextView.setText(getString(R.string.kentKartInformationActivity_connectedTransport_duration, differenceMinutes));
                    } else {
                        connectedTransportTextView.setVisibility(View.GONE);
                    }
                } else {
                    connectedTransportTextView.setVisibility(View.GONE);
                }
            }

            if (!isLastLoadAmountFound && !isLastLoadTimeFound) {
                lastLoadSectionLayout.setVisibility(View.GONE);
            } else {
                if (isLastLoadAmountFound) {
                    lastLoadAmountTextView.setText(String.valueOf(kentKartInformation.lastLoadAmount));
                } else {
                    lastLoadAmountTextView.setVisibility(View.GONE);
                }

                if (isLastLoadTimeFound) {
                    lastLoadTimeTextView.setText(new SimpleDateFormat(Constants.DATE_TIME_FORMAT).format(new Date(kentKartInformation.lastLoadTime)));
                } else {
                    lastLoadTimeTextView.setVisibility(View.GONE);
                }
            }

            changeState(States.SUCCESS);
        }
    }

    private void goToKentKartList() {
        if (isStartedWithNfc) {
            Intent intent = new Intent(getApplicationContext(), KentKartListActivity.class);
            startActivity(intent);
        }
        finish();
    }

    private void handleIntent(Intent intent) {
        if (intent != null && state == null) {
            String action = intent.getAction();

            if (NfcAdapter.ACTION_TECH_DISCOVERED.equals(action)) {
                // An NFC tag has been read
                byte[] tagId = intent.getByteArrayExtra(NfcAdapter.EXTRA_ID);

                String id = StringUtils.generateNfcId(tagId);

                Log.info(this, "Read tag id: " + Arrays.toString(tagId) + ", generated KentKart NFC id: " + id);

                KentKart loadedKentKart = Data.loadKentKart(id);

                if (loadedKentKart != null) {
                    // Found a saved KentKart
                    String message = "Going to get info about KentKart... kentKart: " + loadedKentKart;
                    Log.info(this, message);

                    kentKartName = loadedKentKart.name;
                    kentKartNumber = loadedKentKart.number;
                    kentKartRegionCode = loadedKentKart.regionCode;
                    isStartedWithNfc = true;

                    changeState(States.PROGRESS);
                } else {
                    // This is a new KentKart
                    Log.info(this, "New KentKart with id: " + id);

                    Intent i = new Intent(getApplicationContext(), KentKartEditActivity.class);
                    i.putExtra(Constants.KENT_KART_NFC_ID, id);
                    i.putExtra(Constants.HAS_NFC, true);
                    startActivity(i);
                    finish();
                }
            } else {
                // User came to this activity from UI
                changeState(States.PROGRESS);
            }
        }
    }

    private void updateTitle(ActionBar actionBar) {
        if (!StringUtils.isEmpty(kentKartName) && !StringUtils.isEmpty(kentKartNumber)) {
            actionBar.setTitle(kentKartName);
            actionBar.setSubtitle(KentKart.getFormattedNumber(kentKartNumber));
        }
    }

    private void showProgressLayout() {
        if (progressWidget != null) {
            progressWidget.showProgress(true);
        }
    }

    private void showContentLayout() {
        if (progressWidget != null) {
            progressWidget.showContent(true);
        }
    }

    private void showErrorLayout() {
        if (progressWidget != null) {
            progressWidget.showError(true);
        }
    }

    private void changeState(States state) {
        /* Either
         *   state is null and new state is not, meaning that this is the first time activity is running and a state is being set
         * Or
         *   state is not null and a different state came, meaning that a state change is actually needed
         */
        boolean shouldChangeState = (this.state == null && state != null) || (this.state != null && !this.state.equals(state));

        if (shouldChangeState) {
            this.state = state;

            switch (state) {
                case PROGRESS:
                    showProgressLayout();
                    updateTitle(getSupportActionBar());
                    if (!NetworkUtils.isConnectedToInternet(getApplicationContext())) {
                        Toast.makeText(getApplicationContext(), getString(R.string.kentKartInformationActivity_offline), Toast.LENGTH_LONG).show();
                        changeState(States.ERROR);
                    } else {
                        new GetKentKartInformationTask(kentKartNumber, kentKartRegionCode, this).execute();
                    }
                    break;
                case ERROR:
                    showErrorLayout();
                    break;
                case SUCCESS:
                    showContentLayout();
                    break;
            }
        }
    }
}
