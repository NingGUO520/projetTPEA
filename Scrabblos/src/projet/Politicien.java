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
import java.util.Comparator;
import java.util.List;
import java.util.Map;
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
	private int periode;
	private KeyPair pair;
	private String keyPublic;
	private List<Letter> letters;
	private List<String> allWords;
	private List<Word> guessWords;
	private List<Word> guessWordsOfLastPeriod;
	private List<Bloc> blockchain;
	
	public Politicien(String url, int port) throws IOException, NoSuchAlgorithmException {
		socket = new Socket(url, port);
		inchan = new DataInputStream(socket.getInputStream());
		outchan = new DataOutputStream(socket.getOutputStream());
		id = ++politicienNumber;
		letters = new ArrayList<Letter>();
		guessWords = new ArrayList<Word>();
		guessWordsOfLastPeriod = new ArrayList<Word>();
		blockchain = new ArrayList<Bloc>();
		allWords = Utils.readFile("dict/dict_100000_1_10.txt");
		KeyPairGenerator kp = KeyPairGenerator.getInstance("DSA");
		pair = kp.generateKeyPair();
		keyPublic = Utils.getHexKey(pair.getPublic());
		System.out.println("POLITICIEN CONNECTED");
        System.out.println("------------------------");
	}
	
	public synchronized boolean initialzeLetters() throws IOException, JSONException, InvalidKeyException, NoSuchAlgorithmException, SignatureException {
		JSONObject obj = new JSONObject();
		obj.put("get_full_letterpool",JSONObject.NULL);
		String msg = obj.toString();
		long taille = msg.length();
		outchan.writeLong(taille);
		outchan.write(msg.getBytes("UTF-8"),0,(int)taille);
		read();
		return true;
	}
	
	public synchronized boolean initialzeWords() throws IOException, JSONException, InvalidKeyException, NoSuchAlgorithmException, SignatureException {
		JSONObject obj = new JSONObject();
		obj.put("get_full_wordpool",JSONObject.NULL);
		String msg = obj.toString();
		long taille = msg.length();
		outchan.writeLong(taille);
		outchan.write(msg.getBytes("UTF-8"),0,(int)taille);
		read();
		return true;
	}
	
	
	public List<String> checkWordIfExist(){
		List<String> guessWordsAsString = Word.ListOfWordsToListOfString(guessWords);
		return allWords.stream().filter(word->!guessWordsAsString.contains(word))
				.filter(word -> check(word))
				.filter(word -> oneLettreForEachAuthor(word))
				.collect(Collectors.toList());
	}
	
	public boolean check(String word){
		List<String> lettre = letters.stream().map(l -> l.letter).collect(Collectors.toList());
		return Arrays.asList(word.split("")).stream().filter(c -> lettre.contains(c)).count() == Arrays.asList(word.split("")).size();
	}
	
	public boolean oneLettreForEachAuthor(String word) {
		return Arrays.asList(word.split("")).stream().map(c -> getAuthor(c)).distinct().count() == Arrays.asList(word.split("")).size();
	}
	
	public String getAuthor(String c) {
		return letters.stream().filter(l -> l.letter.equals(c)).map(l -> l.author).findAny().get();
	}
	
	public synchronized boolean injectWord() throws NoSuchAlgorithmException, IOException, InvalidKeyException, JSONException, SignatureException {
		List<String> listOfWords = checkWordIfExist();
		if(!listOfWords.isEmpty()){
			System.out.println("Politicien "+id+" inject word "+listOfWords);
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
	
	public List<JSONObject> getLettreOfWord(String word) throws JSONException{
		List<JSONObject> result = new ArrayList<JSONObject>();
		for(String c : Arrays.asList(word.split(""))){
			Letter lettre = letters.stream().filter(l -> l.letter.equals(c)).collect(Collectors.toList()).get(0);
			result.add(lettreToJson(lettre));
		}
		return result;
	}
	
	public JSONObject lettreToJson(Letter letter) throws JSONException{
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
	
	public void addBlockchaine(int periode, String content) {
		
	}
	
	public void getLastPeriodeWord() throws JSONException, IOException, InvalidKeyException, NoSuchAlgorithmException, SignatureException {
		JSONObject obj = new JSONObject();
		obj.put("get_wordpool_since",periode-1);
		String msg = obj.toString();
		long taille = msg.length();
		outchan.writeLong(taille);
		outchan.write(msg.getBytes("UTF-8"),0,(int)taille);
		read();
	}
	
	public String bestWordOfLastPeriod() {
		return this.guessWordsOfLastPeriod.stream().max(Comparator.comparingInt(Word::getPoint)).get().wordAsString;
	}


	@Override
	public void run() {
		try {
			while(true) {
				initialzeLetters();
				initialzeWords();
				read();
			}
		} catch (IOException | JSONException e) {
			e.printStackTrace();
		} catch (InvalidKeyException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SignatureException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	public void read() throws IOException, InvalidKeyException, NoSuchAlgorithmException, SignatureException, JSONException {
		long taille_ans = inchan.readLong();
		byte [] cbuf = new byte[(int)taille_ans];
		inchan.read(cbuf, 0, (int)taille_ans);
		String s = new String(cbuf,"UTF-8");
		System.out.println("Politicien "+id+" recieve "+s);
		JSONObject msg = new JSONObject(s);
		
		switch ((String)msg.keys().next()) {
		case "next_turn":
			periode = msg.getInt("next_turn");
			System.out.println("Politicien "+id+" recoit : nouvelle periode : "+periode);
			getLastPeriodeWord();
			injectWord();
			break;
			
		case "full_letterpool":
			JSONObject full_letterpool  =  (JSONObject) msg.get("full_letterpool");
			JSONArray array  =  (JSONArray) full_letterpool.get("letters");
			letters.removeAll(letters);
			for(int i = 0; i<array.length();i++){
				letters.add(new Letter(array.get(i).toString().substring(array.get(i).toString().indexOf("{"), array.get(i).toString().length()-1)));
			}
			System.out.println("Politicien "+id+" recoit : Les lettres injectees "+letters);
			break;
			
		case "full_wordpool":
			JSONObject full_wordpool  =  (JSONObject) msg.get("full_wordpool");
			JSONArray array1  =  (JSONArray) full_wordpool.get("words");
			guessWords.removeAll(guessWords);
			for(int i = 0; i<array1.length();i++){
				guessWords.add(new Word(array1.get(i).toString().substring(array1.get(i).toString().indexOf("{"), array1.get(i).toString().length()-1)));
			}
			System.out.println("Politicien "+id+" recoit : Les mots injectes "+guessWords);
			break;
			
		case "diff_wordpool":
			JSONObject diff_wordpool  =  (JSONObject)msg.getJSONObject("diff_wordpool").get("wordpool");;
			JSONArray array2  =  (JSONArray) diff_wordpool.get("words");
			guessWordsOfLastPeriod.removeAll(guessWordsOfLastPeriod);
			for(int i = 0; i<array2.length();i++){
				guessWordsOfLastPeriod.add(new Word(array2.get(i).toString().substring(array2.get(i).toString().indexOf("{"), array2.get(i).toString().length()-1)));
			}
			System.out.println("Politicien "+id+" recoit : Les mots injectes de la période précédente "+guessWordsOfLastPeriod);
			if(!guessWordsOfLastPeriod.isEmpty())  System.out.println("Best word "+bestWordOfLastPeriod());
			break;

		
		default:
			break;
		}
	}

}
