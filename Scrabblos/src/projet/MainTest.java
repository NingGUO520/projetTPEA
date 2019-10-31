package projet;

import java.io.IOException;
import java.net.Socket;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;

import org.json.JSONException;

public class MainTest {
	public static void main (String [] args) throws IOException, NoSuchAlgorithmException, InterruptedException, ClassNotFoundException, InvalidKeyException, JSONException, SignatureException {

		
		
		
		
		Socket s = new Socket("127.0.0.1", 12345);
		Auteur aut = new Auteur(s);
		Thread t = new Thread(aut);
		t.start();
		
		
		Socket s1 = new Socket("127.0.0.1", 12345);
		Auteur aut2 = new Auteur(s1);
		Thread t2 = new Thread(aut2);
		t2.start();
		
		Thread.sleep(1000);
		
		aut.injectLetter();
		Thread.sleep(1000);
		aut2.injectLetter();
		
		Thread.sleep(1000);
		
		Politicien pol = new Politicien("127.0.0.1", 12345);
		Thread t1 = new Thread(pol);
		t1.start();
	

	}
}
