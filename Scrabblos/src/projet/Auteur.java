package projet;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.SignatureException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


public class Auteur implements Runnable{
	private DataOutputStream outchan;
	private DataInputStream inchan;
	private Socket socket;
	private KeyPair pair;
	private String keyPublic; //clé publique simplifiée
	private JSONArray letters_bag;
	private Random random = new Random();
	private boolean work;
	private int periode;
	private JSONArray letterPool;
	private Map<Integer,JSONArray> map_wordPool;
	private Map<String,Integer> scores_authors;
	private Map<String,Integer> scores_politicians;
	private List<Bloc> blockchain;
	
	public Auteur(Socket s) throws IOException, NoSuchAlgorithmException, NoSuchProviderException {
		//Création connexion
		socket = s;
		inchan = new DataInputStream(socket.getInputStream());
		outchan = new DataOutputStream(s.getOutputStream());
		
		//Création identification (paire clé publique/privée)
		KeyPairGenerator kp = KeyPairGenerator.getInstance("DSA","SUN");
		kp.initialize(1024, SecureRandom.getInstance("SHA1PRNG", "SUN"));
		pair = kp.generateKeyPair();
		keyPublic = Utils.getHexKey(pair.getPublic());
		
		//Création bassin de mots local
		map_wordPool = new HashMap<Integer, JSONArray>();
		letterPool = new JSONArray();
		
		//peut injecter à sa création
		work = true;
		
		//Maps pour stocker localement les scores des auteurs et politiciens
		scores_authors = new HashMap<String, Integer>();
		scores_politicians = new HashMap<String, Integer>();
		
		//blockchain
		blockchain = new ArrayList<Bloc>();
		
	}
	
	/**
	 * Enregistrement sur le jeu de l'auteur
	 * @throws JSONException
	 * @throws IOException
	 */
	public void register() throws JSONException, IOException {
		JSONObject obj = new JSONObject();
		obj.put("register",keyPublic);
		outchan.writeLong(obj.toString().length());
		outchan.write(obj.toString().getBytes(StandardCharsets.UTF_8));
	}
	
	/**
	 * Lecture des messages du serveur
	 * @throws IOException 
	 * @throws JSONException
	 * @throws NoSuchAlgorithmException 
	 */
	public void read() throws IOException, JSONException, NoSuchAlgorithmException {
		long taille_ans = inchan.readLong();
		byte [] cbuf = new byte[(int)taille_ans];
		inchan.read(cbuf, 0, (int)taille_ans);
		String s = new String(cbuf,"UTF-8");
		
		JSONObject msg = new JSONObject(s);
		JSONObject obj;
		
		switch ((String)msg.keys().next()) {
		case "next_turn":
			chooseWord(periode);
			periode = msg.getInt("next_turn");
			System.out.println("nouvelle periode : "+periode);
			work = true;
			break;
		case "letters_bag":
				letters_bag = msg.getJSONArray("letters_bag");
				System.out.println("Sac de lettres recu : "+letters_bag);
			break;
			
		case "full_letterpool":
			obj = msg.getJSONObject("full_letterpool");
			updateLetterPool(obj);
			break;
			
		case "full_wordpool": //TODO
			break;
			
		case "diff_letterpool": //TODO
			break;
		case "diff_wordpool":
			//TODO Election du mot au début de la blockchain ?
					msg.getJSONObject("diff_wordpool").getJSONObject("wordpool");
			break;
			
		case "inject_word":
			addWordToPeriod(periode, msg.getJSONObject("inject_word"));
			break;
		
		default:
			System.out.println(msg);
			break;
		}
	}
	
	/**
	 * Demande le bassin de lettre depuis le début
	 * @throws JSONException
	 * @throws IOException
	 */
	public void get_full_letterPool() throws JSONException, IOException {
		JSONObject obj = new JSONObject();
		obj.put("get_full_letterpool",JSONObject.NULL);
		outchan.writeLong(obj.toString().length());
		outchan.write(obj.toString().getBytes(StandardCharsets.UTF_8));
	}
	
	/**
	 * demander le bassin de lettre à partir d'une certaine periode
	 * @param p periode a partir de laquelle on recupere le bassin de lettre
	 * @throws IOException
	 * @throws JSONException
	 */
	public void getLetterPoolSince(int p) throws IOException, JSONException {
		JSONObject obj = new JSONObject();
		obj.put("get_letterpool_since",p);
		String msg = obj.toString();
		long taille = msg.length();
		outchan.writeLong(taille);
		outchan.write(msg.getBytes("UTF-8"),0,(int)taille);
	}
	
	/**
	 * Envoyer une lettre au serveur
	 * @throws InterruptedException
	 * @throws JSONException
	 * @throws IOException
	 * @throws InvalidKeyException
	 * @throws NoSuchAlgorithmException
	 * @throws SignatureException
	 */
	public void inject_letter() throws InterruptedException, JSONException, IOException, InvalidKeyException, NoSuchAlgorithmException, SignatureException {
		int tps_calcul= random.nextInt(5000)+5000;
		Thread.sleep(tps_calcul);
		if(work) {
			JSONObject injection = new JSONObject();
			JSONObject letter = getLetter();
			injection.put("inject_letter", letter);
			String msg = injection.toString();
			long taille = msg.length();
			outchan.writeLong(taille);
			outchan.write(msg.getBytes("UTF-8"),0,(int)taille);
			work = false;
			System.out.println("Lettre injectée : "+injection);
		}
		else {
			System.out.println("Attend prochain tour");
		}
	}
	
	/**
	 * Mettre à jour le bassin de lettres injectées
	 * @param obj bassin de lettre courant
	 * @throws JSONException
	 */
	public void updateLetterPool(JSONObject obj) throws JSONException {
		periode = obj.getInt("current_period");
		letterPool = obj.getJSONArray("letters");
		System.out.println("nouveau pool de lettres : "+letterPool.toString());
		
		//Vérification travail
		for(int i=letterPool.length()-1;i>=0;i--) {
			JSONObject l = letterPool.getJSONArray(i).getJSONObject(1);
			int current_periode = l.getInt("period");
			String author = l.getString("author");
			if(current_periode < periode) {work = true;break;}
			if(current_periode == periode && author.equals(keyPublic)) { work = false;break;}
			if(current_periode > periode) {work = true; periode = current_periode; break;}
		}
		
	}
	
	/**
	 * Créer une lettre au format JSON
	 * @return une lettre aléatoire de l'auteur au format JSON
	 * @throws JSONException 
	 * @throws NoSuchAlgorithmException 
	 * @throws UnsupportedEncodingException 
	 * @throws SignatureException 
	 * @throws InvalidKeyException 
	 */
	public JSONObject getLetter() throws JSONException, NoSuchAlgorithmException, UnsupportedEncodingException, InvalidKeyException, SignatureException {
		int index = random.nextInt(letters_bag.length());
		String l = (String)letters_bag.remove(index);
		JSONObject lettre = new JSONObject();
		lettre.put("letter", l);
		lettre.put("period", periode);
		String head;
		
		if(blockchain.size() == 0) {
			head = Utils.hash("");
		}
		else {
			//head est le hash du mot en tete de la chaine de blocs
			head =blockchain.get(blockchain.size()-1).getHash();
		}
		lettre.put("head",head);
		lettre.put("author", keyPublic);
		String s = Utils.hash(Utils.toBinaryString(l)+Long.toBinaryString(periode)+Utils.hash("")+keyPublic);
		lettre.put("signature", signMessage(s));
		return lettre;
	}
	
	/**
	 * 
	 * @param message le message à signer
	 * @return le hash de la signature du message à envoyer au serveur
	 * @throws NoSuchAlgorithmException
	 * @throws InvalidKeyException
	 * @throws SignatureException
	 */
	public String signMessage(String message) throws NoSuchAlgorithmException, InvalidKeyException, SignatureException {
		Signature s = Signature.getInstance("SHA256withDSA");
		s.initSign(pair.getPrivate());
		s.update(message.getBytes());
		String signature = Utils.bytesToHex(s.sign());
		return signature;
	}
	
	/**
	 * Envoi du message pour écouter les actions
	 * @throws IOException
	 * @throws JSONException
	 */
	public void listen() throws IOException, JSONException {
		JSONObject obj = new JSONObject();
		obj.put("listen",JSONObject.NULL);
		String msg = obj.toString();
		long taille = msg.length();
		outchan.writeLong(taille);
		outchan.write(msg.getBytes("UTF-8"),0,(int)taille);
		System.out.println(keyPublic+" sur écoute");
		
	}
	/**
	 * Arreter l'écoute continue de messages d'injection envoyés par le serveur
	 * @throws JSONException
	 * @throws IOException
	 */
	public void stopListen() throws JSONException, IOException {
		JSONObject obj = new JSONObject();
		obj.put("stop_listen",JSONObject.NULL);
		String msg = obj.toString();
		long taille = msg.length();
		outchan.writeLong(taille);
		outchan.write(msg.getBytes("UTF-8"),0,(int)taille);
		
	}
	
	/**
	 * 
	 * @return Envoi d'un message au serveur pour récuperer le bassin complet de mots injectés 
	 * @throws IOException
	 * @throws JSONException
	 */
	public void getFullWordPool() throws IOException, JSONException {
		JSONObject obj = new JSONObject();
		obj.put("get_full_wordpool",JSONObject.NULL);
		String msg = obj.toString();
		long taille = msg.length();
		outchan.writeLong(taille);
		outchan.write(msg.getBytes("UTF-8"),0,(int)taille);
	}
	
	/**
	 * 
	 * @param p periode depuis laquelle on souhaite récupérer le pool de lettres
	 * @throws IOException
	 * @throws JSONException
	 */
	public void getWordPoolSince(int p) throws IOException, JSONException {
		JSONObject obj = new JSONObject();
		obj.put("get_wordpool_since",p);
		String msg = obj.toString();
		long taille = msg.length();
		outchan.writeLong(taille);
		outchan.write(msg.getBytes("UTF-8"),0,(int)taille);
	}
	/**
	 * Choix du mot qui va être en tête de la chaine (choix parmis les mots injectés dans la précédente période)
	 * @param period periode 
	 * @throws JSONException 
	 * @throws NoSuchAlgorithmException 
	 */
	public void chooseWord(int period) throws JSONException, NoSuchAlgorithmException {
		if(map_wordPool.containsKey(period)) {
			int taille =map_wordPool.get(period).length();
			int score_max =0;
			JSONObject winner = null;
			for(int i=0;i<taille;i++) {
				JSONObject o =map_wordPool.get(period).getJSONObject(i);
				JSONArray letters = o.getJSONArray("word");
				int score = Points.score_mot(letters);
				if(score > score_max) { score_max = score; winner = o;}					
			}
			if(winner !=null) {
				if(blockchain.isEmpty()) { blockchain.add(new Bloc(winner));}
				else { 
					//TODO vérifier que le head de winner correspond au hash du bloc precedent
					blockchain.add(new Bloc(winner,blockchain.get(blockchain.size()-1)));}				
				System.out.println("mot élu ("+score_max+" points) :"+winner );
				
				//calcul score des joueurs
				updateScore_politician(winner.getString("politician"), score_max);
				//TODO calcul score auteurs

			}
		}
		
	}
	
	/**
	 * Mettre à jour le score d'un politicien
	 * @param politician clé publique du politicien
	 * @param additionalPoints points supplémentaires du politicien
	 */
	public void updateScore_politician(String politician, int additionalPoints) {
		if(!scores_politicians.containsKey(politician)) { scores_politicians.put(politician, additionalPoints);}
		else {scores_politicians.replace(politician, additionalPoints+scores_politicians.get(politician)) ;}
	}
	
	/**
	 * Ajout local d'un mot qui vient d'être injecté (quand pas de listen de l'auteur?) //TODO
	 * @param p période à laquelle le mot a été injecté
	 * @param word mot injecté
	 * @throws JSONException 
	 */
	public void addWordToPeriod(int p,JSONObject word) throws JSONException {
		//Vérification validité du mot (pas plusieurs lettres du même auteur)
		boolean is_valid =true;
		JSONArray letters = word.getJSONArray("word");
		List<String> authors = new ArrayList<String>();
		for(int i=0;i<letters.length();i++) {
			JSONObject l = letters.getJSONObject(i);
			String a = l.getString("author");
			if(!authors.contains(a)) { authors.add(a);}
			else { is_valid=false; break;}
		}
		
		//Ajout si le mot est valide
		if(is_valid) {
			if(!map_wordPool.containsKey(p)) { map_wordPool.put(p, new JSONArray()); }
			map_wordPool.get(p).put(word);
		}
	}
	
	//TODO AddLetterToPeriod (voir ci-dessus)
	

	@Override
	public void run() {
		try {
			register();
			read();
			listen();
			while(true) {
				inject_letter();
				read();
			}
		} catch (JSONException | IOException | InvalidKeyException | NoSuchAlgorithmException | SignatureException | InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
	}
	
	
	

}
