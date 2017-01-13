package com.mcc.ocr; /**
 * This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 * Jacopo Bufalino - Aalto University - 2016/2017 
 *  
 * 
 * 
 * */


import android.util.Log;
import android.webkit.MimeTypeMap;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.List;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

public class OCRConnectionManager {
    private static final String TAG = "OCRConnectionManager";
	public static final int OCR_WRONG_CREDENTIALS = 1;
	public static final int OCR_CONNECTION_OK = 0;
	public static final int OCR_FIELDS_MISSING = 2;
	public static final int OCR_INITIALIZE_MISSING = -5;
	public static long dataexchanged = 0;
private static String baseUrl = null;
private static String session = null;

public static void Initialize(String baseurl){
	baseUrl = baseurl;
	/* Make sure to trust all certificates */
	TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {
		public X509Certificate[] getAcceptedIssuers() {
			return new X509Certificate[1];
		}

		public void checkClientTrusted(X509Certificate[] certs, String authType) {

		}

		public void checkServerTrusted(X509Certificate[] certs, String authType) {

		}
	} };

	/* Ignore differences between given hostname and certificate hostname */
	HostnameVerifier hv = new HostnameVerifier() {

		@Override
		public boolean verify(String hostname, SSLSession session) {
			// TODO Auto-generated method stub
			return true;
		}
	};

	/* Install the all-trusting trust manager */

	SSLContext sc = null;
	try {
		sc = SSLContext.getInstance("SSL");
	} catch (NoSuchAlgorithmException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}

	try {
		sc.init(null, trustAllCerts, new SecureRandom());
	} catch (KeyManagementException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
	HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
	HttpsURLConnection.setDefaultHostnameVerifier(hv);
}

public static int Connect(String username,String password){
	if (baseUrl == null){
		return OCR_INITIALIZE_MISSING;
	}
	String postData = "username="+username+"&password="+password;
	URL destinationURL = null;
    HttpsURLConnection conn = null;
    String page = baseUrl+"/login";
	try {
	    byte[] postDataBytes = postData.toString().getBytes("UTF-8");
		destinationURL = new URL(page);
		
		conn = (HttpsURLConnection) destinationURL.openConnection();
		conn.setInstanceFollowRedirects(false);
		HttpsURLConnection.setFollowRedirects(false);
		conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        conn.setRequestProperty("Content-Length", String.valueOf(postDataBytes.length));
		conn.setRequestMethod("POST");
		conn.setConnectTimeout(2000);			
        conn.setDoOutput(true);
        conn.getOutputStream().write(postDataBytes);
	} catch (IOException e) {
		System.out.println("Exiting due to error: Malformed URL Exception.");
	}
     
	/*Check wrong credentials*/
	String redirection = conn.getHeaderField("Location");
	if (redirection != null){
		if (redirection.equals(page))
			return OCRConnectionManager.OCR_WRONG_CREDENTIALS;
	}

	/*Check correct credentials*/
	if (conn.getHeaderField("Set-Cookie") != null){
	session = conn.getHeaderField("Set-Cookie").split(";")[0];
		//dataexchanged+= Long.parseLong(conn.getHeaderField("Content-Length"));
		//dataexchanged+= Long.parseLong(conn.getRequestProperty("Content-Length"));
	return OCR_CONNECTION_OK;
	}
	/*Otherwise there are some fields missing sent through JSON*/
	return OCR_FIELDS_MISSING;
	
}

public static JSONObject OCRemote(List<File> files){
	String charset = "UTF-8";
	String boundary = "----WebKitFormBoundaryFP52Q9lpFIkoWyUM"; 
	String CRLF = "\r\n";

	HttpsURLConnection connection = null;
	try {
		connection = (HttpsURLConnection) new URL(baseUrl+"/remote_ocr").openConnection();
	} catch (MalformedURLException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	} catch (IOException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
	connection.setDoOutput(true);
	connection.setInstanceFollowRedirects(false);
	HttpsURLConnection.setFollowRedirects(false);
	connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
    connection.setRequestProperty("Cookie", session);
	try (
	    OutputStream output = connection.getOutputStream();
	    PrintWriter writer = new PrintWriter(new OutputStreamWriter(output, charset), true);
	) {

	    // Send text files.
		for (File textFile : files){
	    writer.append("--" + boundary).append(CRLF);
	    writer.append("Content-Disposition: form-data; name=\"file[]\"; filename=\"" + textFile.getName() + "\"").append(CRLF);
	    writer.append("Content-Type: "+getMimeType(textFile)+"; charset=" + charset).append(CRLF); // Text file itself must be saved in this charset!
	    writer.append(CRLF).flush();
	    
	    //Files.copy(textFile.toPath(), output);
			dataexchanged += textFile.length();
	   InputStream in = new FileInputStream(textFile);
	   int ch;
	   while((ch = in.read()) != -1)
		    output.write(ch);
	    output.flush(); // Important before continuing with writer!
	    writer.append(CRLF).flush(); // CRLF is important! It indicates end of boundary.
		}

	    // End of multipart/form-data.
	    writer.append("--" + boundary + "--").append(CRLF).flush();
	} catch (IOException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
	dataexchanged += Long.parseLong(connection.getHeaderField("Content-Length"));

	 return GetJson(connection.getHeaderField("Location"));
}

    private static String getExtension(final File file) {
        String suffix = "";
        String name = file.getName();
        final int idx = name.lastIndexOf(".");
        if (idx > 0) {
            suffix = name.substring(idx + 1);
        }
        return suffix;
    }

    public static String getMimeType(final File file) {
        String extension = getExtension(file);
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
    }

private static JSONObject GetJson(String path){
	URL destinationURL = null;
    HttpURLConnection conn = null;
    Log.i(TAG, "path="+path);
	try {
		destinationURL = new URL(path);
		
		conn = (HttpURLConnection) destinationURL.openConnection();
		conn.setInstanceFollowRedirects(false);
		HttpURLConnection.setFollowRedirects(false);
		conn.setRequestMethod("GET");
		conn.setConnectTimeout(2000);			
        conn.setRequestProperty("Cookie", session);
        conn.setDoOutput(false);
	} catch (IOException e) {
		System.out.println("Exiting due to error: Malformed URL Exception.");
	}
   
    int ch;
    StringBuilder sb = new StringBuilder();
    try {
		while((ch = conn.getInputStream().read()) != -1)
		    sb.append((char)ch);
	} catch (IOException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
String jsn = sb.toString();
    try {
		dataexchanged += Long.parseLong(conn.getHeaderField("Content-Length"));
		//dataexchanged+= Long.parseLong(conn.getRequestProperty("Content-Length"));
		return new JSONObject(jsn);
	} catch (JSONException e) {
		return null;

	}
}

public static File getImage(String thumb_id){
	//35.186.221.63

			URL destinationURL = null;
	        HttpsURLConnection conn = null;

			try {
				destinationURL = new URL(baseUrl + "/image/"+thumb_id);
				conn = (HttpsURLConnection) destinationURL.openConnection();
				conn.setInstanceFollowRedirects(false);
				HttpsURLConnection.setFollowRedirects(false);
				//System.out.println(conn.getURL().toString());
				conn.setRequestMethod("GET");
				conn.setConnectTimeout(2000);			
		        conn.setRequestProperty("Cookie", session);
		        conn.setDoOutput(false);
			} catch (IOException e) {
				System.out.println("Exiting due to error: Malformed URL Exception.");
			}
	       
	        int ch;
File tmp_file = null;
try {
	tmp_file = File.createTempFile("dasdas", "sdivb");
} catch (IOException e1) {
	e1.printStackTrace();
}
	        StringBuilder sb = new StringBuilder();
	        try {
	        	OutputStream out =
					      new FileOutputStream(tmp_file.getAbsolutePath());
				while((ch = conn.getInputStream().read()) != -1)
				    out.write(ch);
				out.flush();
				out.close();
				dataexchanged += Long.parseLong(conn.getHeaderField("Content-Length"));
//				dataexchanged += Long.parseLong(conn.getRequestProperty("Content-Length"));
				return tmp_file;
			} catch (IOException e) {
				e.printStackTrace();
			}


	
	return null;
}

public static JSONObject OCRHistory(String user_id){
	
	URL destinationURL = null;
    HttpsURLConnection conn = null;

	try {
		destinationURL = new URL(baseUrl+"/history_api/"+user_id);
		
		conn = (HttpsURLConnection) destinationURL.openConnection();
		conn.setInstanceFollowRedirects(false);
		HttpsURLConnection.setFollowRedirects(false);
		conn.setRequestMethod("GET");
		conn.setConnectTimeout(2000);			
        conn.setRequestProperty("Cookie", session);
        conn.setDoOutput(false);
	} catch (IOException e) {
		System.out.println("Exiting due to error: Malformed URL Exception.");
	}
   
    int ch;
    StringBuilder sb = new StringBuilder();
    try {
		while((ch = conn.getInputStream().read()) != -1)
		    sb.append((char)ch);
	} catch (IOException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
String jsn = sb.toString();
    try {
		return new JSONObject(jsn);
	} catch (JSONException e) {
		return null;

	}
	
}
}
