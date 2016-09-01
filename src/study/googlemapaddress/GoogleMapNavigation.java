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

import com.google.android.gms.location.LocationListener;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.GoogleMap.OnInfoWindowClickListener;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
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

public class GoogleMapNavigation extends Activity implements LocationListener{
	
	private GoogleMap mGoogleMap;
	
	private EditText et_lbs_keyword;
	private Button btn_lbs_searchKeyword;
	private TextView tvLatLng;	
	
	private String mapAddressUrl = "";					// 查詢網址	
	private String formatted_address = "";				// 地址
	private LatLng latlngAddress = new LatLng(0, 0);	// 地址座標
	private LatLng latlngLocation = new LatLng(0, 0);	// 本地座標
	private GmsLocationUtil mGmsLocationUtil;			// Google Play Service Locaton APIs
    private int locationUpdateCount = 1;
    private boolean firstSetMapLocation = false;
	
	private Handler mHandler;
	protected static final int REFRESH_DATA = 0x00000001;
	private Dialog dialog;    

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.google_map_navigation);
		
		// 設定本頁面為直向
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);    
		
		// Google Play Service Locaton APIs
		mGmsLocationUtil = new GmsLocationUtil(this);
		
		// Google Map
		mGoogleMap = ((MapFragment)getFragmentManager().findFragmentById(R.id.frg_lbs_map)).getMap();
		mGoogleMap.setMyLocationEnabled(true);
		mGoogleMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
		
	    // 介面顯示設定
	    UiSettings uis = mGoogleMap.getUiSettings();
	    uis.setZoomControlsEnabled(true); 		// 顯示縮放按鈕
	    uis.setCompassEnabled(true); 			// 顯示指北針
	    uis.setMyLocationButtonEnabled(true); 	// 顯示自己位置按鈕
	    uis.setScrollGesturesEnabled(true); 	// 開啟地圖捲動手勢
	    uis.setZoomGesturesEnabled(true); 		// 開啟地圖縮放手勢
	    uis.setTiltGesturesEnabled(true); 		// 開啟地圖傾斜手勢
	    uis.setRotateGesturesEnabled(true); 	// 開啟地圖旋轉手勢
		
		// 判定是否有開啟定位
		if(isOpenGps()){
			startUpdateLocation();
		}else{
			Toast.makeText(GoogleMapNavigation.this, "請打開定位功能", Toast.LENGTH_LONG).show();
		}
			
		tvLatLng = (TextView) findViewById(R.id.tvLatLng);
		et_lbs_keyword = (EditText) findViewById(R.id.et_lbs_keyword);		
		et_lbs_keyword.setText("屏東縣恆春鎮白沙路23號");	
		btn_lbs_searchKeyword = (Button) findViewById(R.id.btn_lbs_searchKeyword); // 關鍵字搜尋
		
		// 關鍵字搜尋
		btn_lbs_searchKeyword.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				
				if(haveInternet()){
				
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
			}
		});
		
		// 簡易型 Marker，點擊後直接進入導航
		mGoogleMap.setOnInfoWindowClickListener(new OnInfoWindowClickListener() {

			 @ Override
			public void onInfoWindowClick(Marker marker) {
				 
				// 判定 Google Map 是否安裝
				if (isAvilible(GoogleMapNavigation.this, "com.google.android.apps.maps")) {

					// 導航座標設定
					String vDirectionUrl = "https://maps.google.com/maps?f=d" 
							+ "&saddr="	+ latlngLocation.latitude + "," + latlngLocation.longitude 
							+ "&daddr=" + latlngAddress.latitude + "," + latlngAddress.longitude 
							+ "&hl=tw";

					// 關閉本頁 activity
					finish();

					// 在 Google 地圖 App 顯示導航
					Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(vDirectionUrl));
					intent.setClassName("com.google.android.apps.maps",
							"com.google.android.maps.MapsActivity");
					startActivity(intent);

				// 如果 Google Map 未安裝，轉跳到 Google Play Store 下載頁面
				} else {
					Toast.makeText(GoogleMapNavigation.this, "您尚未安裝 Google Map", Toast.LENGTH_LONG).show();
					Uri uri = Uri.parse("market://details?id=com.google.android.apps.maps");
					Intent intent = new Intent(Intent.ACTION_VIEW, uri);
					GoogleMapNavigation.this.startActivity(intent);
				}
			}
		});		
	}
	
    private void stopUpdateLocation() {
        mGmsLocationUtil.stopUpdateLocation(this);
    }
    
    private void startUpdateLocation() {
        mGmsLocationUtil.startUpdateLocation(this);
    }
    
    @Override
    protected void onStart() {
        super.onStart();
        mGmsLocationUtil.connect();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mGmsLocationUtil.isConnected() && mGmsLocationUtil.isRequestLocationUpdates()) {
            startUpdateLocation();
        }
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        stopUpdateLocation();
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        mGmsLocationUtil.disConnect();
    }
    
    @Override
    public void onLocationChanged(Location location) {
    	
    	latlngLocation = new LatLng(location.getLatitude(), location.getLongitude());
    	
    	if(firstSetMapLocation != true){
            moveMap(latlngLocation, 18.0F);
            firstSetMapLocation = true;
    	}
    	
    	Log.e("latlngLocation", latlngLocation.toString());
    	tvLatLng.setText("latlngLocation, count: " + locationUpdateCount++ + "\n" + latlngLocation.toString() 
    		+ "\n" + "formatted_address: " + formatted_address + "\n" + latlngAddress.toString());
    }
	
	/**
	 * 用關鍵字搜尋地標
	 * 使用 Google Maps JavaScript API
	 * 
	 * @param keyword
	 */
	private void searchKeyword(String keyword) {
		try {
						
			String unitStr = URLEncoder.encode(keyword, "utf8");  //字體要utf8編碼			
			StringBuilder sb = new StringBuilder("https://maps.googleapis.com/maps/api/geocode/json");
			sb.append("?address=" + unitStr);
			sb.append("&key=AIzaSyCAWEE5Zowaenzf4qSYIvAP8ph3mcv0B6Q");
			sb.append("&language=zh-TW");
			mapAddressUrl = sb.toString();
			Log.e("mapAddressUrl", mapAddressUrl);
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
//							Log.e("strResult", strResult);
							
							// String to JSONObject
							obj = new JSONObject(strResult);
							
							// status
							String status = obj.getString("status");
							
							// 如果  "status" 是 "OK" 代表查詢成功
							if (status.equals("OK")) {
								
								// results
								String stringResults = obj.getString("results");
								JSONArray arrayResults = new JSONArray(stringResults);
								JSONObject objectResults = arrayResults.getJSONObject(0);
								
								// formatted_address
								formatted_address = objectResults.getString("formatted_address");
								
								// geometry
								String stringGeometry = objectResults.getString("geometry");
								JSONObject objectGeometry = new JSONObject(stringGeometry);
								
								// location
								String stringLocation = objectGeometry.getString("location");
								JSONObject objectLocation = new JSONObject(stringLocation);						
								latlngAddress = new LatLng(objectLocation.getDouble("lat"), objectLocation.getDouble("lng"));
								Log.e("latlngAddress", latlngAddress.toString());
								
						        // Clears all the existing markers
								mGoogleMap.clear();
								
								// 簡易型 Marker
								Marker melbourne = mGoogleMap.addMarker(new MarkerOptions()
				                          .position(latlngAddress)
				                          .title(formatted_address));
								melbourne.showInfoWindow();
								
								// 移動地圖到搜尋解果座標
								moveMap(latlngAddress, 18.0F);

						    	Log.e("latlngLocation", latlngLocation.toString());
						    	tvLatLng.setText("latlngLocation, count: " + locationUpdateCount++ + "\n" + latlngLocation.toString() 
						    		+ "\n" + "formatted_address: " + formatted_address + "\n" + latlngAddress.toString());
						    	
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
	
	/**
	 * 移動地圖到指定位置
	 * 
	 * @param place
	 * 			座標
	 * 
	 * @param zoom
	 * 			縮放比
	 * 
	 * @return none
	 */
	private void moveMap(LatLng place, float zoom) {
	    // 建立地圖攝影機的位置物件
	    CameraPosition cameraPosition = 
	            new CameraPosition.Builder()
	            .target(place)
	            .zoom(zoom)
	            .build();
	 
	    // 使用動畫的效果移動地圖
	    mGoogleMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
	}
	
	/**
	 * 檢查手機上是否安裝了指定的軟件
	 * @param 	context
	 * @param 	packageName：應用包名
	 * @return
	 */
    private boolean isAvilible(Context context, String packageName){ 
        // 獲取packagemanager 
        final PackageManager packageManager = context.getPackageManager();
        // 獲取所有已安裝程序的包信息 
        List<PackageInfo> packageInfos = packageManager.getInstalledPackages(0);
        // 用於存儲所有已安裝程序的包名 
        List<String> packageNames = new ArrayList<String>();
        // 從pinfo中將包名字逐一取出，壓入pName list中 
        if(packageInfos != null){ 
            for(int i = 0; i < packageInfos.size(); i++){ 
                String packName = packageInfos.get(i).packageName; 
                packageNames.add(packName); 
            } 
        } 
        // 判斷packageNames中是否有目標程序的包名，有TRUE，沒有FALSE 
        return packageNames.contains(packageName);
    }
	
    // 判斷是否有網路
    private boolean haveInternet() {
    	boolean result = false;
    	ConnectivityManager connManager = (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
    	NetworkInfo info = connManager.getActiveNetworkInfo();
    	if (info == null || !info.isConnected()) {
    		result = false;
    	} else {
    		if (!info.isAvailable()) {
    			result = false;
    		} else {
    			result = true;
    		}
    	}
    	return result;
    }
	
	/**
	  * 判斷GPS是否開啟，GPS或者AGPS開啟一個就認為是開啟的
	  */
	private boolean isOpenGps() {

		LocationManager locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
		// 通過GPS衛星定位，定位級別可以精確到街（通過24顆衛星定位，在室外和空曠的地方定位準確、速度快）
		boolean gps = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
		// 通過WLAN或移動網路(3G/2G)確定的位置（也稱作AGPS，輔助GPS定位。主要用於在室內或遮蓋物（建築群或茂密的深林等）密集的地方定位）
		boolean network = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
		if (gps || network) {
			return true;
		}
		return false;
	}
}