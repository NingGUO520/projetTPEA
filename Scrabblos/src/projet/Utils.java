package projet;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Base64.Encoder;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class Utils {
	
	
	
	
	public static List<String> readFile(String file) throws IOException{
        InputStream ips  =  new FileInputStream(file);
        InputStreamReader ir = new InputStreamReader(ips);
        BufferedReader br = new BufferedReader(ir);
        String line;
        List<String> result = new ArrayList<String>();
        while( (line = br.readLine()) != null) {
        	result.add(line);
        }
        return result;
    }
	
	public static String getHexKey(PublicKey pk) {
		byte [] array = pk.toString().getBytes();
		Encoder encoder = Base64.getEncoder();
		String s = encoder.encodeToString(array);
		byte[] decoded = Base64.getDecoder().decode(s);
		String res = String.format("%040x", new BigInteger(1, decoded));
		return res.substring(res.length()-64, res.length());
	}
	
	private static byte[] getSHA(String input) throws NoSuchAlgorithmException 
    {   
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        return md.digest(input.getBytes(StandardCharsets.UTF_8));  
    } 
    
	public static String bytesToHex(byte[] bytes) {
		StringBuilder sb = new StringBuilder();
		for(Byte b : bytes) {
			sb.append(String.format("%02x", b));
			
		}
		return sb.toString();
	}
    
    public static String hash(String input) throws NoSuchAlgorithmException {
    	return bytesToHex(getSHA(input));
    }
    
    public static String toBinaryString(String s) throws UnsupportedEncodingException {
    	byte [] bytes =  s.getBytes("UTF-8");
    	String res = "";
    	for(byte b : bytes) {
    		res+=Integer.toBinaryString(b);
    	}
    	return res;
    }
    
    public static Map<String, Object> jsonToMap(Object json) throws JSONException {

        if(json instanceof JSONObject)
            return _jsonToMap_((JSONObject)json) ;

        else if (json instanceof String)
        {
            JSONObject jsonObject = new JSONObject((String)json) ;
            return _jsonToMap_(jsonObject) ;
        }
        return null ;
    }


   private static Map<String, Object> _jsonToMap_(JSONObject json) throws JSONException {
        Map<String, Object> retMap = new HashMap<String, Object>();

        if(json != JSONObject.NULL) {
            retMap = toMap(json);
        }
        return retMap;
    }


    private static Map<String, Object> toMap(JSONObject object) throws JSONException {
        Map<String, Object> map = new HashMap<String, Object>();

        Iterator<String> keysItr = object.keys();
        while(keysItr.hasNext()) {
            String key = keysItr.next();
            Object value = object.get(key);

            if(value instanceof JSONArray) {
                value = toList((JSONArray) value);
            }

            else if(value instanceof JSONObject) {
                value = toMap((JSONObject) value);
            }
            map.put(key, value);
        }
        return map;
    }


    public static List<Object> toList(JSONArray array) throws JSONException {
        List<Object> list = new ArrayList<Object>();
        for(int i = 0; i < array.length(); i++) {
            Object value = array.get(i);
            if(value instanceof JSONArray) {
                value = toList((JSONArray) value);
            }

            else if(value instanceof JSONObject) {
                value = toMap((JSONObject) value);
            }
            list.add(value);
        }
        return list;
    }
    
    public static Map<String, Integer> sortByValue(Map<String, Integer> unsortMap, final boolean order)
    {
        List<Entry<String, Integer>> list = new LinkedList<>(unsortMap.entrySet());

        // Sorting the list based on values
        list.sort((o1, o2) -> order ? o1.getValue().compareTo(o2.getValue()) == 0
                ? o1.getKey().compareTo(o2.getKey())
                : o1.getValue().compareTo(o2.getValue()) : o2.getValue().compareTo(o1.getValue()) == 0
                ? o2.getKey().compareTo(o1.getKey())
                : o2.getValue().compareTo(o1.getValue()));
        return list.stream().collect(Collectors.toMap(Entry::getKey, Entry::getValue, (a, b) -> b, LinkedHashMap::new));

    }

}
