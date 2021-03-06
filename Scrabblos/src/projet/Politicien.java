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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import projet.Utils.Bloc;
import projet.Utils.Letter;
import projet.Utils.Points;
import projet.Utils.Utils;
import projet.Utils.Word;

public class Politicien implements Runnable{
    
    private Socket socket;
    private DataOutputStream outchan;
	private DataInputStream inchan;
	private static int politicienNumber=0;
	private int id;
	private int periode;
	private KeyPair pair;
	private String keyPublic;
	private boolean work;
	private Random random = new Random();
	private List<Letter> letters;
	private List<String> dictionary;
	private List<Word> guessWords;
	private List<Word> guessWordsOfLastPeriod;
	private List<Bloc> blockchain;
	private Map<String,Integer> scores_authors;
	private Map<String,Integer> scores_politicians;
	
	public Politicien(String url, int port) throws IOException, NoSuchAlgorithmException {
		socket = new Socket(url, port);
		inchan = new DataInputStream(socket.getInputStream());
		outchan = new DataOutputStream(socket.getOutputStream());
		id = ++politicienNumber;
		work = true;
		letters = new ArrayList<Letter>();
		guessWords = new ArrayList<Word>();
		guessWordsOfLastPeriod = new ArrayList<Word>();
		blockchain = new ArrayList<Bloc>();
		scores_authors = new HashMap<String, Integer>();
		scores_politicians = new HashMap<String, Integer>();
		dictionary = Utils.readFile("dict/dict_100000_1_10.txt");
		KeyPairGenerator kp = KeyPairGenerator.getInstance("DSA");
		pair = kp.generateKeyPair();
		keyPublic = Utils.getHexKey(pair.getPublic());
		System.out.println("POLITICIEN CONNECTED");
        System.out.println("------------------------");
	}
	
	public synchronized boolean initialzeLetters() throws IOException, JSONException, InvalidKeyException, NoSuchAlgorithmException, SignatureException, InterruptedException {
		JSONObject obj = new JSONObject();
		obj.put("get_full_letterpool",JSONObject.NULL);
		String msg = obj.toString();
		long taille = msg.length();
		outchan.writeLong(taille);
		outchan.write(msg.getBytes("UTF-8"),0,(int)taille);
		read();
		return true;
	}
	
	public synchronized boolean initialzeWords() throws IOException, JSONException, InvalidKeyException, NoSuchAlgorithmException, SignatureException, InterruptedException {
		JSONObject obj = new JSONObject();
		obj.put("get_full_wordpool",JSONObject.NULL);
		String msg = obj.toString();
		long taille = msg.length();
		outchan.writeLong(taille);
		outchan.write(msg.getBytes("UTF-8"),0,(int)taille);
		read();
		return true;
	}
	
	/**
	 * verifie si ce mot existe deja dans word pool
	 * @param words
	 * @return
	 */
	public List<String> checkWordIfAlreadyInPool( List<String> words){
		List<String> result = new ArrayList<String>() ;
		for(String w : words) {
			if(!guessWords.stream().map(wo -> wo.wordAsString).collect(Collectors.toList()).contains(w)) {
				result.add(w);
			}
		}
		return result;
	}
	
	
	
	public List<String> checkWordIfExist(){
		List<String> guessWordsAsString = Word.ListOfWordsToListOfString(guessWords);
		return dictionary.stream().filter(word->!guessWordsAsString.contains(word))
				.filter(word -> check(word))
				.filter(word -> oneLettreForEachAuthor(word))
				.collect(Collectors.toList());
	}
	
	public boolean check(String word){
		List<String> lettre = letters.stream().map(l -> l.letter).collect(Collectors.toList());
		return Arrays.asList(word.split("")).stream().filter(c -> lettre.contains(c)).count() == Arrays.asList(word.split("")).size();
	}
	
	/**
	 * On verifie que les mots ne contiennent pas plus d’une lettre soumise par un même auteur
	 * @param word
	 * @return
	 */
	public boolean oneLettreForEachAuthor(String word) {
		return Arrays.asList(word.split("")).stream().map(c -> getAuthor(c)).distinct().count() == Arrays.asList(word.split("")).size();
	}
	
	public String getAuthor(String c) {
		return letters.stream().filter(l -> l.letter.equals(c)).map(l -> l.author).findAny().get();
	}
	
	public synchronized void injectWord() throws NoSuchAlgorithmException, IOException, InvalidKeyException, JSONException, SignatureException, InterruptedException {
		Thread.sleep(5000+random.nextInt(2000));
		if(work) {
			getPeriodeWord(periode-1);
			getPeriodeWord(periode);
			setDictionary();
			List<String> listOfWords = checkWordIfExist();
			if(!listOfWords.isEmpty()){
				System.out.println("not exicte on "+guessWords);
				System.out.println("Politicien "+id+" inject word "+listOfWords.stream().findAny().get());
				JSONObject injection = new JSONObject();
				injection.put("inject_word", getWord(listOfWords.get(0)));
				String msg = injection.toString();
				long taille = msg.length();
				outchan.writeLong(taille);
				outchan.write(msg.getBytes("UTF-8"),0,(int)taille);
			}
			work = false;
		}
		else {
			System.out.println("Politicien "+id+" attend prochain tour");
	
		}
	}
	
	public JSONObject getWord(String w) throws NoSuchAlgorithmException, UnsupportedEncodingException, InvalidKeyException, JSONException, SignatureException {
		JSONObject word = new JSONObject();
		word.put("word", getLettreOfWord(w));
		if(blockchain.isEmpty()) word.put("head", Utils.hash(""));
		else word.put("head", blockchain.get(blockchain.size()-1).getHash());
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
	
	public void addBlockchaine(int periode, JSONObject content) throws NoSuchAlgorithmException {
		if(blockchain.isEmpty()) blockchain.add(new Bloc(content));
		else blockchain.add(new Bloc(content,blockchain.get(blockchain.size()-1)));
	}
	
	public void getPeriodeWord(int p) throws JSONException, IOException, InvalidKeyException, NoSuchAlgorithmException, SignatureException, InterruptedException {
		JSONObject obj = new JSONObject();
		obj.put("get_wordpool_since", p);
		String msg = obj.toString();
		long taille = msg.length();
		outchan.writeLong(taille);
		outchan.write(msg.getBytes("UTF-8"),0,(int)taille);
		read();
	}
	
	public Word bestWordOfLastPeriod() {
		return this.guessWordsOfLastPeriod.stream().max(Comparator.comparingInt(Word::getPoint)).get();
	}
	
	public void score(Word word) {
		scorePolitician(word);
		scoreAuthor(word.word);
	}
	
	public void scorePolitician(Word word) {
		if(!scores_politicians.containsKey(word.politician)) scores_politicians.put(word.politician, word.point);
		else scores_politicians.replace(word.politician, word.point+scores_politicians.get(word.politician));
	}
	
	public void scoreAuthor(List<Letter> lettres) {
		for(Letter l : lettres) {
			if(!scores_authors.containsKey(l.author)) scores_authors.put(l.author, Points.getScore(l.letter.charAt(0)));
			else scores_authors.replace(l.author, Points.getScore(l.letter.charAt(0))+scores_authors.get(l.author));
		}
	}
	
	public void setDictionary() throws IOException {
		if(5<letters.size() && letters.size() <= 15) {
			dictionary.addAll(Utils.readFile("dict/dict_100000_5_15.txt"));
		}
		else if(25<letters.size() && letters.size() <= 50) {
			dictionary.addAll(Utils.readFile("dict/dict_100000_25_75.txt"));
		}
		else if(50<letters.size() && letters.size() <= 200) {
			dictionary.addAll(Utils.readFile("dict/dict_100000_50_200.txt"));
		}
	}
	
	public void printWinner() {
		Map<String, Integer> politicien = Utils.sortByValue(scores_politicians, false);
		Map<String, Integer> author = Utils.sortByValue(scores_authors, false);
		System.out.println("Politician's score "+id);
		for(String key: politicien.keySet()) {
			System.out.println("Politician "+key.substring(0,5)+" :" +politicien.get(key));
		}
		System.out.println("Author's score "+id);
		for(String key: author.keySet()) {
			System.out.println("Author "+key.substring(0,5)+" :" +author.get(key));
		}
	}


	@Override
	public void run() {
		try {
			while(true) {
				initialzeLetters();
				initialzeWords();
				read();
				injectWord();
			}
		} catch (IOException | InterruptedException | JSONException | InvalidKeyException | NoSuchAlgorithmException | SignatureException e) {
			printWinner();
		}
		
	}
	
	public void read() throws IOException, InvalidKeyException, NoSuchAlgorithmException, SignatureException, JSONException, InterruptedException {
		long taille_ans = inchan.readLong();
		byte [] cbuf = new byte[(int)taille_ans];
		inchan.read(cbuf, 0, (int)taille_ans);
		String s = new String(cbuf,"UTF-8");
//		System.out.println("Politicien "+id+" recieve "+s);
		JSONObject msg = new JSONObject(s);
		
		switch ((String)msg.keys().next()) {
		case "next_turn":
			periode = msg.getInt("next_turn");
			System.out.println("Politicien "+id+" est en nouvelle periode : "+periode);
			work = true;
			break;
			
		case "full_letterpool":
			JSONObject full_letterpool  =  (JSONObject) msg.get("full_letterpool");
			JSONArray array  =  (JSONArray) full_letterpool.get("letters");
			letters.removeAll(letters);
			for(int i = 0; i<array.length();i++){
				letters.add(new Letter(array.get(i).toString().substring(array.get(i).toString().indexOf("{"), array.get(i).toString().length()-1)));
			}
			System.out.println("Politicien "+id+" recoit : letter pool "+letters);
			break;
			
		case "full_wordpool":
			JSONObject full_wordpool  =  (JSONObject) msg.get("full_wordpool");
			JSONArray array1  =  (JSONArray) full_wordpool.get("words");
			guessWords.removeAll(guessWords);
			for(int i = 0; i<array1.length();i++){
				guessWords.add(new Word(array1.get(i).toString().substring(array1.get(i).toString().indexOf("{"), array1.get(i).toString().length()-1)));
			}
			System.out.println("Politicien "+id+" recoit : word pool "+guessWords);
			break;
			
		case "diff_wordpool":
			JSONObject diff_wordpool  =  (JSONObject)msg.getJSONObject("diff_wordpool");
			JSONObject wordpool  =  (JSONObject)msg.getJSONObject("diff_wordpool").get("wordpool");
			JSONArray array2  =  (JSONArray) wordpool.get("words");
			String StringPeriod = periode+"";
			String StringPeriodSubOne = periode-1+"";
			if((diff_wordpool.get("since").toString()).equals(StringPeriodSubOne)){
				guessWordsOfLastPeriod.removeAll(guessWordsOfLastPeriod);
				for(int i = 0; i<array2.length();i++){
					guessWordsOfLastPeriod.add(new Word(array2.get(i).toString().substring(array2.get(i).toString().indexOf("{"), array2.get(i).toString().length()-1)));
				}
				System.out.println("Politicien "+id+" recoit : Les mots injectes de la période précédente "+guessWordsOfLastPeriod);
				if(!guessWordsOfLastPeriod.isEmpty()) {
					Word bestWord = bestWordOfLastPeriod();
					addBlockchaine(periode, bestWord.wordAsObject);
					score(bestWord);
				}
			}
			else {
				List<Word> word = new ArrayList<Word>();
				for(int i = 0; i<array2.length();i++){
					word.add(new Word(array2.get(i).toString().substring(array2.get(i).toString().indexOf("{"), array2.get(i).toString().length()-1)));
				}
				guessWords.addAll(word);
				System.out.println("Politicien "+id+" recoit : Les mots injectes de cette période "+word);
			}
			break;

		
		default:
			break;
		}
	}

}
