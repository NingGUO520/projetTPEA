package projet;

import java.awt.image.ConvolveOp;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.net.Socket;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.adapters.HexBinaryAdapter;

import org.json.JSONArray;
import org.json.JSONObject;



public class Auteur implements Runnable{
	private DataOutputStream outchan;
	private DataInputStream inchan;
	private Socket socket;
	private KeyPair pair;
	private ArrayList<String> letters;
	private int periode;
	private String keyPublic;
	
	
	public Auteur(Socket s) throws IOException, NoSuchAlgorithmException{
		periode = 0;
		letters = new ArrayList<String>();
		socket = s;
		inchan = new DataInputStream(socket.getInputStream());
		outchan = new DataOutputStream(s.getOutputStream());
		KeyPairGenerator kp = KeyPairGenerator.getInstance("DSA");
		pair = kp.generateKeyPair();
	}
	
	long little2big(long i) {
	    return (i&0xff)<<24 | (i&0xff00)<<8 | (i&0xff0000)>>8 | (i>>24)&0xff;
	}
	
	public void read() throws IOException {
		long taille_ans = inchan.readLong();
		byte [] cbuf = new byte[(int)taille_ans];
		inchan.read(cbuf, 0, (int)taille_ans);
		String s = new String(cbuf,"UTF-8");
		System.out.println(s);
		
	}
	public boolean register() throws IOException, ClassNotFoundException, NoSuchAlgorithmException {
		JSONObject obj = new JSONObject();
		String [] key = pair.getPublic().toString().split("\n");

//		keyPublic = "8f7935d9b9aae9bfabed887acf4951b6f32ec59e3baf3718e8eac4961f3efd36";
		keyPublic = sha256("auteur1");
//		System.out.println("keyPublic = "+keyPublic);
		obj.put("register","b7b597e0d64accdb6d8271328c75ad301c29829619f4865d31cc0c550046a08f" );
//		obj.put("register",keyPublic);
		String msg = obj.toString();
		long taille = msg.length();
		outchan.writeLong(taille);
		outchan.write(msg.getBytes("UTF-8"),0,(int)taille);
		long taille_ans = inchan.readLong();
		byte [] cbuf = new byte[(int)taille_ans];
		inchan.read(cbuf, 0, (int)taille_ans);
		String s = new String(cbuf,"UTF-8");
		System.out.println(s);
		
		JSONObject object = new JSONObject(s);
		JSONArray array  =  (JSONArray) object.get("letters_bag");
		List<Object> l = array.toList();
		for(Object a: l ) {
			String x = (String)a;
			letters.add(x);
		}
		return true;
	}
	
	public boolean injectLettre() throws IOException, NoSuchAlgorithmException {
		JSONObject letter = new JSONObject();
//		String a = letters.get(3);
		String a = "a";
		String head = sha256("");
		
		String auteur = "b7b597e0d64accdb6d8271328c75ad301c29829619f4865d31cc0c550046a08f";
//		System.out.println("head = " +head);
		String signature = "8b6547447108e11c0092c95e460d70f367bc137d5f89c626642e1e5f2ceb6108043d4a080223b467bb810c52b5975960eea96a2203a877f32bbd6c4dac16ec07";
	
//		String signature = sha512(a+periode+head+keyPublic);
//		System.out.println("signature = " +signature);
//		System.out.println("periode = " +periode);

		letter.put("letter", a);
		letter.put("period", periode);
		letter.put("head", head);
		letter.put("author", auteur);
		letter.put("signature", signature);
		
		
		JSONObject obj = new JSONObject();
		obj.put("inject_letter", letter);
		
//		obj.put("inject_letter", "{ \"letter\":\"a\", \"period\":0,\n" + 
//				"     \"head\":\"e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855\",\n" + 
//				"     \"author\":\"b7b597e0d64accdb6d8271328c75ad301c29829619f4865d31cc0c550046a08f\",\n" + 
//				"     \"signature\":\"8b6547447108e11c0092c95e460d70f367bc137d5f89c626642e1e5f2ceb6108043d4a080223b467bb810c52b5975960eea96a2203a877f32bbd6c4dac16ec07\"\n" + 
//				"   }\n" + 
//				"");

		
		String msg = obj.toString();
		long taille = msg.length();
		outchan.writeLong(taille);
		outchan.write(msg.getBytes("UTF-8"),0,(int)taille);
		
		periode++;
		return true;
	}
	
	
	public String bytesToHex(byte[] bytes) {
		StringBuilder sb = new StringBuilder();
		for(Byte b : bytes) {
			sb.append(String.format("%02x", b));
			
		}
		return sb.toString();
	}
	
	public String hash256(String s) throws NoSuchAlgorithmException {
		MessageDigest md = MessageDigest.getInstance("SHA-256");
		byte[] resultat = md.digest(s.getBytes());
		
		return bytesToHex(resultat);
	}
	
	
	public String hash512(String s) throws NoSuchAlgorithmException {
		MessageDigest md = MessageDigest.getInstance("SHA-512");
		byte[] resultat = md.digest(s.getBytes());
		
		return bytesToHex(resultat);
	}
	
	
	public  String sha256(String base) throws NoSuchAlgorithmException {
	   return hash256(base);
	}
	
	public  String sha512(String base) throws NoSuchAlgorithmException {
		   return hash512(base);
		}
	
	public boolean ecouteContinue() throws IOException {
		JSONObject obj = new JSONObject();
		obj.put("listen",JSONObject.NULL);
		String msg = obj.toString();
		long taille = msg.length();
		outchan.writeLong(taille);
		outchan.write(msg.getBytes("UTF-8"),0,(int)taille);
		return true;
		
	}
	
	public boolean getFullLetterPool() throws IOException {
		JSONObject obj = new JSONObject();
		obj.put("get_full_letterpool",JSONObject.NULL);
		String msg = obj.toString();
		long taille = msg.length();
		outchan.writeLong(taille);
		outchan.write(msg.getBytes("UTF-8"),0,(int)taille);
		
		read();
		return true;
	}
	
	
	public boolean getLetterPoolSince(int p) throws IOException {
		JSONObject obj = new JSONObject();
		obj.put("get_letterpool_since",p);
		String msg = obj.toString();
		long taille = msg.length();
		outchan.writeLong(taille);
		outchan.write(msg.getBytes("UTF-8"),0,(int)taille);
		read();

		return true;
	}
	
	

	@Override
	public void run() {
		try {
				
				register();
				System.out.println("fin de registration");
				injectLettre();
				System.out.println("fin de envoyer de lettre");
//				ecouteContinue();
//
				getFullLetterPool();
				getLetterPoolSince(0);
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
		
	}
}
