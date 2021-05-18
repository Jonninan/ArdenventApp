package com.rmpav.android;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import com.salesforce.androidsdk.app.SalesforceSDKManager;
import com.salesforce.androidsdk.mobilesync.app.MobileSyncSDKManager;
import com.salesforce.androidsdk.rest.ApiVersionStrings;
import com.salesforce.androidsdk.rest.RestClient;
import com.salesforce.androidsdk.rest.RestClient.AsyncRequestCallback;
import com.salesforce.androidsdk.rest.RestRequest;
import com.salesforce.androidsdk.rest.RestResponse;
import com.salesforce.androidsdk.ui.SalesforceActivity;

import org.json.JSONArray;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Main activity
 */
public class SecondActivity extends SalesforceActivity implements View.OnClickListener {

	Button scanBtn;
	private RestClient client;
	private ArrayAdapter<String> listAdapter;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Setup theme
		boolean isDarkTheme = MobileSyncSDKManager.getInstance().isDarkTheme();
		setTheme(isDarkTheme ? R.style.SalesforceSDK_Dark : R.style.SalesforceSDK);
		MobileSyncSDKManager.getInstance().setViewNavigationVisibility(this);

		// Setup view
		setContentView(R.layout.secondmain);

		scanBtn = findViewById(R.id.scanBtn);
		scanBtn.setOnClickListener(this);
	}

	@Override
	public void onResume() {
		// Hide everything until we are logged in
		findViewById(R.id.root).setVisibility(View.INVISIBLE);

		// Create list adapter
		listAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, new ArrayList<String>());
		((ListView) findViewById(R.id.contacts_list)).setAdapter(listAdapter);

		super.onResume();
	}



	@Override
	public void onResume(RestClient client) {
		// Keeping reference to rest client
		this.client = client;

		// Show everything
		findViewById(R.id.root).setVisibility(View.VISIBLE);
	}

	/**
	 * Called when "Logout" button is clicked.
	 *
	 * @param v
	 */
	public void onLogoutClick(View v) {
		SalesforceSDKManager.getInstance().logout(this);
	}

	/**
	 * Called when "Clear" button is clicked.
	 *
	 * @param v
	 */
	public void onClearClick(View v) {
		listAdapter.clear();
	}

	/**
	 * Called when "Fetch Contacts" button is clicked.
	 *
	 * @param v
	 * @throws UnsupportedEncodingException
	 */
	/*public void onFetchContactsClick(View v) throws UnsupportedEncodingException {
		sendRequest("SELECT Name FROM Contact");
	}
	*/
	/**
	 * Called when "Fetch Items" button is clicked
	 *
	 * @param v
	 * @throws UnsupportedEncodingException
	 */
	public void onFetchEquipmentsClick(View v) throws UnsupportedEncodingException {
		sendRequest("SELECT Parent_Inventory__r.Name, Name, Barcode__c, Product_Type__c, createddate  " +
				"FROM Equipment__c ");
	}

	private void sendRequest(String soql) throws UnsupportedEncodingException {
		RestRequest restRequest = RestRequest.getRequestForQuery(ApiVersionStrings.getVersionNumber(this), soql);

		client.sendAsync(restRequest, new AsyncRequestCallback() {
			@Override
			public void onSuccess(RestRequest request, final RestResponse result) {
				result.consumeQuietly(); // consume before going back to main thread
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						try {
							listAdapter.clear();
							JSONArray records = result.asJSONObject().getJSONArray("records");
							for (int i = 0; i < records.length(); i++) {
								listAdapter.add("Name : "+records.getJSONObject(i).getString("Name")+
										"\nProduct Type : "+records.getJSONObject(i).getString("Product_Type__c")+
										"\nBar Code:"+records.getJSONObject(i).getString("Barcode__c"));
								//listAdapter.add(records.getJSONObject(i).getString("Name"));
								//listAdapter.add(records.getJSONObject(i).getString("Product_Type__c"));
								//listAdapter.add(records.getJSONObject(i).getString("Bar_Code__c"));
							}
						} catch (Exception e) {
							onError(e);
						}
					}
				});
			}

			@Override
			public void onError(final Exception exception) {
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						Toast.makeText(SecondActivity.this,
								SecondActivity.this.getString(R.string.sf__generic_error, exception.toString()),
								Toast.LENGTH_LONG).show();
					}
				});
			}
		});
	}

	public void onClick(View v) {
		scanCode();
	}

	private void scanCode() {

		IntentIntegrator integrator = new IntentIntegrator(this);
		integrator.setCaptureActivity(CaptureAct.class);
		integrator.setOrientationLocked(false);
		integrator.setDesiredBarcodeFormats(IntentIntegrator.ALL_CODE_TYPES);
		integrator.setPrompt("Scanning Code");
		integrator.initiateScan();
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		final Intent sendIntent = new Intent(Intent.ACTION_SEND);

		IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
		if (result != null) {
			if (result.getContents() != null) {
				final String barCode =  result.getContents();
				AlertDialog.Builder builder = new AlertDialog.Builder(this);
				builder.setMessage(result.getContents());
				builder.setTitle("Scanning Results");
				sendIntent.putExtra(Intent.EXTRA_TEXT, result.getContents());

				builder.setPositiveButton("Send", new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						//startActivity(Intent.createChooser(sendIntent, "https://test.salesforce.com/servlet/servlet.WebToLead?encoding=UTF-8"));
						//finish();
						try {
							insertRequest(barCode);
						} catch (UnsupportedEncodingException e) {
							e.printStackTrace();
						}
					}

				});
/*
				builder.setPositiveButton("Save", new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						//startActivity(Intent.createChooser(sendIntent, "https://test.salesforce.com/servlet/servlet.WebToLead?encoding=UTF-8"));
						//finish();


					}

				});
*/
				/*.setPositiveButton("Scan Again", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                });*/
				AlertDialog dialog = builder.create();
				dialog.show();
			} else {
				Toast.makeText(this, "No Results", Toast.LENGTH_LONG).show();
			}
		} else {
			super.onActivityResult(requestCode, resultCode, data);
		}


	}

	private void insertRequest(String barCode) throws UnsupportedEncodingException {
		Map createEquipment = new HashMap();
		createEquipment.put("BarCode__c", barCode);
		createEquipment.put("Status__c","New");
		createEquipment.put("Parent_Inventory__c","a0v09000000JgXuAAK");
		createEquipment.put("Product_Type__c","Ventilator");
		RestRequest restRequest = RestRequest.getRequestForCreate(ApiVersionStrings.getVersionNumber(this),"Equipment__c",createEquipment);
		//ApiVersionStrings.getVersionNumber(this), barCode

		client.sendAsync(restRequest, new AsyncRequestCallback() {

			@Override
			public void onError(Exception e) {
				//Toast.makeText(onClick.this, e.getMessage(), Toast.LENGTH_LONG).show();
			}

			@Override
			public void onSuccess(RestRequest request, RestResponse response) {
				try {
					//Log.d("APITest", "success entered");
				} catch (Exception e) {
					//showError(MainActivity.this, e);
				}
			}
		});
	}
}