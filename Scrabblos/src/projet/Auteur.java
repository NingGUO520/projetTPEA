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
	private int score = 0;
	private boolean work;
	private int periode;
	private JSONArray letterPool;
	
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
		
		//peut injecter à sa création
		work = true;
		
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
		read();
	}
	
	/**
	 * Lecture des messages du serveur
	 * @throws IOException 
	 * @throws JSONException
	 */
	public void read() throws IOException, JSONException {
		long taille_ans = inchan.readLong();
		byte [] cbuf = new byte[(int)taille_ans];
		inchan.read(cbuf, 0, (int)taille_ans);
		String s = new String(cbuf,"UTF-8");
		
		JSONObject msg = new JSONObject(s);
		JSONObject obj;
		
		switch ((String)msg.keys().next()) {
		case "next_turn":
			periode = msg.getInt("next_turn");
			System.out.println("nouvelle periode : "+periode);
			work = true;
			break;
		case "letters_bag":
				letters_bag = msg.getJSONArray("letters_bag");
				System.out.println(letters_bag);
			break;
			
		case "full_letterpool":
			obj = msg.getJSONObject("full_letterpool");
			updateLetterPool(obj);
			break;
			
		case "inject_word":
			System.out.println(msg);
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
		read();
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
		get_full_letterPool();
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
		if(letterPool.length() == 0) {
			head = Utils.hash("");
		}
		else {
			head = Utils.hash(letterPool.get(letterPool.length()-1).toString());
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
	
	public void listen() throws IOException, JSONException {
		JSONObject obj = new JSONObject();
		obj.put("listen",JSONObject.NULL);
		String msg = obj.toString();
		long taille = msg.length();
		outchan.writeLong(taille);
		outchan.write(msg.getBytes("UTF-8"),0,(int)taille);
		System.out.println(keyPublic+" sur écoute");
		
	}
	

	@Override
	public void run() {
		try {
			register();
			listen();
			get_full_letterPool();
			while(true) {
				if(work) {
					inject_letter();
				}
				read();
			}
		} catch (JSONException | IOException | InvalidKeyException | NoSuchAlgorithmException | SignatureException | InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
	}
	
	
	

}
