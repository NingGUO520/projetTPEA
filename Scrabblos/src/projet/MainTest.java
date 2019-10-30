package projet;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.NoSuchAlgorithmException;

public class MainTest {
	public static void main (String [] args) throws IOException, NoSuchAlgorithmException, InterruptedException, ClassNotFoundException {
//
		Socket s = new Socket("127.0.0.1", 12345);
		Auteur aut = new Auteur(s);
		
		Thread t = new Thread(aut);
		t.start();
//		
	
//		
//		Politicien pol = new Politicien("127.0.0.1", 12345);
//		Thread t1 = new Thread(pol);
//		t1.start();
	}
}
