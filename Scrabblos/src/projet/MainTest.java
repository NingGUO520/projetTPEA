package projet;

import java.io.IOException;
import java.net.Socket;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SignatureException;

import org.json.JSONException;

public class MainTest {
	public static void main (String [] args) throws IOException, NoSuchAlgorithmException, InterruptedException, ClassNotFoundException, InvalidKeyException, JSONException, SignatureException, NoSuchProviderException {

		Socket s = new Socket("127.0.0.1", 12345);
		Auteur aut = new Auteur(s);
		Thread t = new Thread(aut);
		t.start();
		
		
//		Socket s1 = new Socket("127.0.0.1", 12345);
//		Auteur aut2 = new Auteur(s1);
//		Thread t2 = new Thread(aut2);
//		t2.start();
//		
//		Socket s3 = new Socket("127.0.0.1", 12345);
//		Auteur aut3 = new Auteur(s3);
//		Thread t3 = new Thread(aut3);
//		t3.start();
		
		Politicien pol = new Politicien("127.0.0.1", 12345);
		Thread t1 = new Thread(pol);
		t1.start();
	
//		
//		while(true) {
//			Thread.sleep(1000);
//			aut.injectLetter();
//			Thread.sleep(100);
//			aut2.injectLetter();
//			Thread.sleep(100);
//			aut3.injectLetter();
//		}

	}
}
