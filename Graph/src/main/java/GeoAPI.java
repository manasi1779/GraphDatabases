import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import org.apache.commons.httpclient.util.URIUtil;
import org.apache.http.HttpResponse;

import com.google.gson.Gson;


public class GeoAPI {

	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}
	
	public void getLocation(){
		try {
	        URL url = new URL(
	                "http://maps.googleapis.com/maps/api/geocode/json?address="
	                        + URIUtil.encodeQuery("Sayaji Hotel, Near balewadi stadium, pune") + "&sensor=true");
	        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
	        conn.setRequestMethod("GET");
	        conn.setRequestProperty("Accept", "application/json");

	        if (conn.getResponseCode() != 200) {
	            throw new RuntimeException("Failed : HTTP error code : " + conn.getResponseCode());
	        }
	        BufferedReader br = new BufferedReader(new InputStreamReader((conn.getInputStream())));

	        String output = "", full = "";
	        while ((output = br.readLine()) != null) {
	            System.out.println(output);
	            full += output;
	        }

	       /* PincodeVerify gson = new Gson().fromJson(full, PincodeVerify.class); 
	        HttpResponse response = new IsPincodeSupportedResponse(new PincodeVerifyConcrete(
	                gson.getResults().get(0).getFormatted_address(), 
	                gson.getResults().get(0).getGeometry().getLocation().getLat(),
	                gson.getResults().get(0).getGeometry().getLocation().getLng())) ;
	        try {
	            String address = response.getAddress();
	            Double latitude = response.getLatitude(), longitude = response.getLongitude();
	            if (address == null || address.length() <= 0) {
	                log.error("Address is null");
	            }
	        } catch (NullPointerException e) {
	            log.error("Address, latitude on longitude is null");
	        }*/
	        conn.disconnect();
	    } catch (MalformedURLException e) {
	        e.printStackTrace();
	    } catch (IOException e) {
	        e.printStackTrace();
	    }
	}

}
