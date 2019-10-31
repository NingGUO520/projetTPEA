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
import java.security.Signature;
import java.security.SignatureException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class Politicien implements Runnable{
    
    private Socket socket;
    private DataOutputStream outchan;
	private DataInputStream inchan;
	private static int politicienNumber=0;
	private int id;
	private KeyPair pair;
	private String keyPublic;
	private List<Letter> letters;
	private List<String> allWords;
	private Set<String> guessWords;
	
	public Politicien(String url, int port) throws IOException, NoSuchAlgorithmException {
		socket = new Socket(url, port);
		inchan = new DataInputStream(socket.getInputStream());
		outchan = new DataOutputStream(socket.getOutputStream());
		id = ++politicienNumber;
		letters = new ArrayList<Letter>();
		guessWords = new HashSet<String>();
		allWords = Utils.readFile("dict/dict_100000_1_10.txt");
		KeyPairGenerator kp = KeyPairGenerator.getInstance("DSA");
		pair = kp.generateKeyPair();
		keyPublic = Utils.getHexKey(pair.getPublic());
		System.out.println("POLITICIEN CONNECTED");
        System.out.println("------------------------");
	}
	
	public boolean initialzeLetters() throws IOException {
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
		for(int i = 0; i<array.length();i++){
			letters.add(new Letter(array.get(i).toString().substring(3, array.get(i).toString().length()-1)));
		}
		System.out.println(letters);
		return true;
	}
	
	public boolean initialzeWords() throws IOException {
		JSONObject obj = new JSONObject();
		obj.put("get_full_wordpool",JSONObject.NULL);
		String msg = obj.toString();
		long taille = msg.length();
		outchan.writeLong(taille);
		outchan.write(msg.getBytes("UTF-8"),0,(int)taille);
		String s = read();
		JSONObject object = new JSONObject(s);
		JSONObject full_letterpool  =  (JSONObject) object.get("full_wordpool");
		JSONArray array  =  (JSONArray) full_letterpool.get("words");
		for(int i = 0; i<array.length();i++){
			guessWords.add(array.get(i).toString().substring(3, array.get(i).toString().length()-1));
		}
		System.out.println(guessWords);
		return true;
	}
	
	public boolean listen() throws IOException, ClassNotFoundException, NoSuchAlgorithmException {
		JSONObject obj = new JSONObject();
		obj.put("listen",JSONObject.NULL);
		String msg = obj.toString();
		long taille = msg.length();
		outchan.writeLong(taille);
		outchan.write(msg.getBytes("UTF-8"),0,(int)taille);
		String s = read();
		return true;
	}
	
	public List<String> checkWordIfExist(){
		return allWords.stream().filter(guessWords::contains).filter(word -> check(word)).collect(Collectors.toList());
	}
	
	public boolean check(String word){
		List<String> lettre = letters.stream().map(l -> l.letter).collect(Collectors.toList());
		//todo
		return Arrays.asList(word.split("")).stream().map(c -> lettre.contains(c)) != null;
	}
	
	public boolean injectWord() throws NoSuchAlgorithmException, IOException, InvalidKeyException, JSONException, SignatureException {
		List<String> listOfWords = checkWordIfExist();
		if(!listOfWords.isEmpty()){
			System.out.println("inject word");
			JSONObject injection = new JSONObject();
			injection.put("inject_word", getWord(listOfWords.get(0)));
			String msg = injection.toString();
			long taille = msg.length();
			outchan.writeLong(taille);
			outchan.write(msg.getBytes("UTF-8"),0,(int)taille);
		}
		return true;
	}
	
	public JSONObject getWord(String w) throws NoSuchAlgorithmException, UnsupportedEncodingException, InvalidKeyException, JSONException, SignatureException {
		JSONObject word = new JSONObject();
		word.put("word", getLettreOfWord(w));
		word.put("head", Utils.hash(""));
		word.put("politician", keyPublic);
		String s = Utils.hash(Utils.toBinaryString(getLettreOfWord(w).toString())+Utils.hash("")+keyPublic);
		word.put("signature", signMessage(s));
		return word;
	}
	
	public List<JSONObject> getLettreOfWord(String word){
		List<JSONObject> result = new ArrayList<JSONObject>();
		for(String c : Arrays.asList(word.split(""))){
			Letter lettre = letters.stream().filter(l -> l.letter.equals(c)).collect(Collectors.toList()).get(0);
			result.add(lettreToJson(lettre));
		}
		return result;
	}
	
	public JSONObject lettreToJson(Letter letter){
		JSONObject result = new JSONObject();
		result.put("letter", letter.letter);
		result.put("period", letter.period);
		result.put("head", letter.head);
		result.put("author", letter.author);
		result.put("signature", letter.signature);
		return result;
	}
	
	public String signMessage(String message) throws NoSuchAlgorithmException, InvalidKeyException, SignatureException {
		Signature s = Signature.getInstance("SHA256withDSA");
		s.initSign(pair.getPrivate());
		s.update(message.getBytes());
		String signature = Utils.bytesToHex(s.sign());
		return signature;
	}


	@Override
	public void run() {
		try {
			//while(true) {
				initialzeLetters();
				injectWord();
			//}
		} catch (IOException | InvalidKeyException | NoSuchAlgorithmException | JSONException | SignatureException e) {
			e.printStackTrace();
		}
		
	}
	
	public String read() throws IOException {
		long taille_ans = inchan.readLong();
		byte [] cbuf = new byte[(int)taille_ans];
		inchan.read(cbuf, 0, (int)taille_ans);
		String s = new String(cbuf,"UTF-8");
		System.out.println("Politicien "+id+" recieve "+s);
		return s;
	}

}
