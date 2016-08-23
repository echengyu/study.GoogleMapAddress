package study.googlemapaddress;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class GoogleMapNavigation extends Activity {
	
	private EditText et_lbs_keyword;
	private Button btn_lbs_searchKeyword;
	private TextView tvLatLng;
	private String mapAddressUrl = "";
	private Double lat, lng; 
	private String latlng;	
	private Handler mHandler;
	protected static final int REFRESH_DATA = 0x00000001;
	private Dialog dialog;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.google_map_navigation);
		
		tvLatLng = (TextView) findViewById(R.id.tvLatLng);
		et_lbs_keyword = (EditText) findViewById(R.id.et_lbs_keyword);		
		et_lbs_keyword.setText("屏東縣恆春鎮白沙路23號");	
		btn_lbs_searchKeyword = (Button) findViewById(R.id.btn_lbs_searchKeyword); // 關鍵字搜尋
		
		// 關鍵字搜尋
		btn_lbs_searchKeyword.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				
				// 關閉鍵盤
				InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
				imm.hideSoftInputFromWindow(et_lbs_keyword.getWindowToken(), 0); // et_setting_name為獲取焦點的EditText
				// 搜尋關鍵字
				String keyword = et_lbs_keyword.getText().toString();
				if (!TextUtils.isEmpty(keyword)) {
					searchKeyword(keyword);
				} else {
					Toast.makeText(GoogleMapNavigation.this, getString(R.string.please_enter_an_address), Toast.LENGTH_LONG).show();
				}				
			}
		});
	}
	
	/**
	 * 用關鍵字搜尋地標
	 * 
	 * @param keyword
	 */
	private void searchKeyword(String keyword) {
		try {
						
			String unitStr = URLEncoder.encode(keyword, "utf8");  //字體要utf8編碼			
			StringBuilder sb = new StringBuilder("https://maps.googleapis.com/maps/api/geocode/json");
			sb.append("?address=" + unitStr);
			sb.append("&key=AIzaSyCAWEE5Zowaenzf4qSYIvAP8ph3mcv0B6Q");			
			mapAddressUrl = sb.toString();
			dialog = ProgressDialog.show(GoogleMapNavigation.this, null, getString(R.string.loading_wait),true);
			storeRegIdinServerread();
			
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
			Log.i("searchKeyword", "Exception:" + e);
		}
	}
	
	@SuppressLint("HandlerLeak")
	private void storeRegIdinServerread() {
		// TODO Auto-generated method stub

		mHandler = new Handler() {
			@Override
			public void handleMessage(Message msg) {
				switch (msg.what) {
				// 顯示網路上抓取的資料
				case REFRESH_DATA:
					String strResult = null;
					if (msg.obj instanceof String)
						strResult = (String) msg.obj;
					
					// 印出網路回傳的文字
					if (strResult != null) {
						JSONObject obj;
						try {
							Log.e("strResult", strResult);
							
							// String to JSONObject
							obj = new JSONObject(strResult);
							
							// Get "status" to JSONObject
							String status = obj.getString("status");
							
							// 如果  "status" 是 "OK" 代表查詢成功
							if (status.equals("OK")) {
								
								// Get "results" to JSONArray
								String stringResults = obj.getString("results");
								JSONArray arrayResults = new JSONArray(stringResults);
								JSONObject objectResults = arrayResults.getJSONObject(0);
								String stringGeometry = objectResults.getString("geometry");
								JSONObject objectGeometry = new JSONObject(stringGeometry);
								String stringLocation = objectGeometry.getString("location");
								JSONObject objectLocation = new JSONObject(stringLocation);
								
								lat = objectLocation.getDouble("lat");
								lng = objectLocation.getDouble("lng");
								Log.e("location.Double", lat+","+lng);
								
								// location to String
								latlng = objectLocation.get("lat")+","+objectLocation.get("lng");

								tvLatLng.setText(latlng);
							}else{
								tvLatLng.setText(getString(R.string.no_results));
							}

						} catch (JSONException e) {
							e.printStackTrace();
						}
					}
					break;
				}
				dialog.dismiss();
			}
		};
		
		Thread t = new Thread(new sendPostRunnable());
		t.start();
	}

	class sendPostRunnable implements Runnable {

		@Override
		public void run() {

			mHandler.obtainMessage(REFRESH_DATA, sendPostDataToInternetread(mapAddressUrl)).sendToTarget();
		}
	}
	
	private String sendPostDataToInternetread(String httpPostUrl) {

		/* 建立HTTP Post連線 */

		HttpPost httpRequest = new HttpPost(httpPostUrl);

		/*
		 * Post運作傳送變數必須用NameValuePair[]陣列儲存
		 */
		List<NameValuePair> params = new ArrayList<NameValuePair>();

		// params.add(new BasicNameValuePair("data", strTxt));        

		try {
			/* 發出HTTP request */

			httpRequest.setEntity(new UrlEncodedFormEntity(params, HTTP.UTF_8));
			/* 取得HTTP response */
			HttpResponse httpResponse = new DefaultHttpClient().execute(httpRequest);
			/* 若狀態碼為200 ok */
			if (httpResponse.getStatusLine().getStatusCode() == 200) {
				/* 取出回應字串 */
				String strResult = EntityUtils.toString(httpResponse.getEntity());
				// 回傳回應字串
				return strResult;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
}