package projet;

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.util.Base64;
import java.util.Base64.Encoder;

public class Utils {
	
	public static String getHexKey(PublicKey pk) {
		byte [] array = pk.getEncoded();
		Encoder encoder = Base64.getEncoder();
		String s = encoder.encodeToString(array);
		byte[] decoded = Base64.getDecoder().decode(s);
		return String.format("%040x", new BigInteger(1, decoded)).substring(0, 64);
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

}
