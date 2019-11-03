package projet;

import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;

public class LancheAuteur {

	public static void main(String args[]) throws  NoSuchProviderException, UnknownHostException, IOException, NoSuchAlgorithmException {
		Socket s = new Socket("127.0.0.1", 12345);
		Auteur aut = new Auteur(s);
		Thread t = new Thread(aut);
		t.start();
		
		
		Socket s1 = new Socket("127.0.0.1", 12345);
		Auteur aut2 = new Auteur(s1);
		Thread t2 = new Thread(aut2);
		t2.start();
//		
		Socket s3 = new Socket("127.0.0.1", 12345);
		Auteur aut3 = new Auteur(s3);
		Thread t3 = new Thread(aut3);
		t3.start();
		
	}
}