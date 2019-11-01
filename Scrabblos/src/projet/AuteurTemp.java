package projet;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.SignatureException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;



public class AuteurTemp implements Runnable{
	private DataOutputStream outchan;
	private DataInputStream inchan;
	private Socket socket;
	private KeyPair pair;
	private ArrayList<String> letters;
	private long periode;
	private String keyPublic;
	private static int authorNumber=0;
	private int id;
	private Bloc bloc;

	
	public AuteurTemp(Socket s) throws IOException, NoSuchAlgorithmException, NoSuchProviderException{
		periode = 0;
		letters = new ArrayList<String>();
		socket = s;
		inchan = new DataInputStream(socket.getInputStream());
		outchan = new DataOutputStream(s.getOutputStream());
		KeyPairGenerator kp = KeyPairGenerator.getInstance("DSA","SUN");
		kp.initialize(1024, SecureRandom.getInstance("SHA1PRNG", "SUN"));
		pair = kp.generateKeyPair();
		keyPublic = Utils.getHexKey(pair.getPublic());
		bloc = new Bloc();
		
		id = ++authorNumber;
		System.out.println("AUTEUR "+id+" CONNECTED");
        System.out.println("------------------------");
	}
	
	public String read() throws IOException, JSONException {
		long taille_ans = inchan.readLong();
		byte [] cbuf = new byte[(int)taille_ans];
		inchan.read(cbuf, 0, (int)taille_ans);
		String s = new String(cbuf,"UTF-8");
//		System.out.println("Author "+id+" receive "+s);
		return s;
	}
	
	
	public boolean register() throws IOException, ClassNotFoundException, NoSuchAlgorithmException, JSONException {
		JSONObject obj = new JSONObject();
		obj.put("register",keyPublic);
		String msg = obj.toString();
		long taille = msg.length();
		outchan.writeLong(taille);
		outchan.write(msg.getBytes("UTF-8"),0,(int)taille);
		long taille_ans = inchan.readLong();
		byte [] cbuf = new byte[(int)taille_ans];
		inchan.read(cbuf, 0, (int)taille_ans);
		String s = new String(cbuf,"UTF-8");
		System.out.println("Author "+authorNumber+" receive "+s);
		
		JSONObject object = new JSONObject(s);
		JSONArray array  =  (JSONArray) object.get("letters_bag");
		for(int i=0;i<array.length();i++) {
			String x = (String)array.get(i);
			letters.add(x);
		}
		return true;
	}
	
	public boolean injectLetter() throws InvalidKeyException, JSONException, NoSuchAlgorithmException, SignatureException, IOException {
		JSONObject injection = new JSONObject();
		JSONObject letter = getLetter();
		injection.put("inject_letter", letter);
		String msg = injection.toString();
		long taille = msg.length();
		outchan.writeLong(taille);
		outchan.write(msg.getBytes("UTF-8"),0,(int)taille);
		
		bloc = new Bloc(letter, bloc);
		

		return true;
	}
	
	public JSONObject getLetter() throws NoSuchAlgorithmException, InvalidKeyException, JSONException, SignatureException, UnsupportedEncodingException {
		Random rd = new Random();
		int index = rd.nextInt(letters.size());
		String l = letters.remove(index);
		JSONObject lettre = new JSONObject();
		lettre.put("letter", l);
		lettre.put("period", periode);
		lettre.put("head", bloc.getHash());
		lettre.put("author", keyPublic);
		String s = Utils.hash(Utils.toBinaryString(l)+Long.toBinaryString(periode)+Utils.hash("")+keyPublic);
		lettre.put("signature", signMessage(s));
		System.out.println("Author "+id+" inject letter "+ l );

		return lettre;
	}
	
	public String signMessage(String message) throws NoSuchAlgorithmException, InvalidKeyException, SignatureException, UnsupportedEncodingException {
		Signature s = Signature.getInstance("SHA256withDSA");
		s.initSign(pair.getPrivate());
		s.update(message.getBytes());
		String signature = Utils.bytesToHex(s.sign());
		return signature;
	}
	
	public boolean ecouteContinue() throws IOException, JSONException {
		JSONObject obj = new JSONObject();
		obj.put("listen",JSONObject.NULL);
		String msg = obj.toString();
		long taille = msg.length();
		outchan.writeLong(taille);
		outchan.write(msg.getBytes("UTF-8"),0,(int)taille);
		return true;
		
	}
	
	
	public boolean stopListen() throws JSONException, IOException {
		JSONObject obj = new JSONObject();
		obj.put("stop_listen",JSONObject.NULL);
		String msg = obj.toString();
		long taille = msg.length();
		outchan.writeLong(taille);
		outchan.write(msg.getBytes("UTF-8"),0,(int)taille);
		return true;
		
	}
	
	/**
	 * Pour obtenir l'ensemble des lettres injectées depuis le début de la partie
	 * @return
	 * @throws IOException
	 * @throws JSONException
	 */
	public boolean getFullLetterPool() throws IOException, JSONException {
		JSONObject obj = new JSONObject();
		obj.put("get_full_letterpool",JSONObject.NULL);
		String msg = obj.toString();
		long taille = msg.length();
		outchan.writeLong(taille);
		outchan.write(msg.getBytes("UTF-8"),0,(int)taille);
	
		String s = read();
		JSONObject object = new JSONObject(s);
		JSONObject full_letterpool  =  (JSONObject) object.get("full_letterpool");
		JSONArray array  =  (JSONArray) full_letterpool.get("letters");
		
		ArrayList<Letter> letterPool = new ArrayList<Letter>();
		for(int i = 0; i<array.length();i++){
			letterPool.add(new Letter(array.get(i).toString().substring(3, array.get(i).toString().length()-1)));
		}
		System.out.print("Author "+id+" receive letter pool : ");
		for(Letter l: letterPool) {
			System.out.print(l.letter+", ");
		}
		System.out.print("\n");
		return true;
	}
	
	
	public boolean getLetterPoolSince(int p) throws IOException, JSONException {
		JSONObject obj = new JSONObject();
		obj.put("get_letterpool_since",p);
		String msg = obj.toString();
		long taille = msg.length();
		outchan.writeLong(taille);
		outchan.write(msg.getBytes("UTF-8"),0,(int)taille);
//		read();

		return true;
	}
	
	/**
	 *  obtenir les mots injectés
	 */
	public boolean getFullWordPool() throws IOException, JSONException {
		JSONObject obj = new JSONObject();
		obj.put("get_full_wordpool",JSONObject.NULL);
		String msg = obj.toString();
		long taille = msg.length();
		outchan.writeLong(taille);
		outchan.write(msg.getBytes("UTF-8"),0,(int)taille);
		
		read();
		return true;
	}
	
	
	public boolean getWordPoolSince(int p) throws IOException, JSONException {
		JSONObject obj = new JSONObject();
		obj.put("get_wordpool_since",p);
		String msg = obj.toString();
		long taille = msg.length();
		outchan.writeLong(taille);
		outchan.write(msg.getBytes("UTF-8"),0,(int)taille);
		read();

		return true;
	}
	
	
	public void next_turn(JSONObject o) throws InvalidKeyException, JSONException, NoSuchAlgorithmException, SignatureException, IOException {
		periode = o.getInt("period");
		injectLetter();
	}
	
	

	@Override
	public void run() {
		try {
				
			register();
			ecouteContinue();
			while(true) {
				injectLetter();
				Thread.sleep(1000);
				getFullLetterPool();
//				getFullWordPool();

			}
			
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InvalidKeyException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SignatureException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
		
	}
}
