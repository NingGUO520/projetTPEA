package projet;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;

public class LanchePoliticien {
	public static void main(String args[]) throws NoSuchAlgorithmException, IOException {
		Politicien pol = new Politicien("127.0.0.1", 12345);
		Thread t1 = new Thread(pol);
		t1.start();
		
		Politicien pol1 = new Politicien("127.0.0.1", 12345);
		Thread t2 = new Thread(pol1);
		t2.start();
		
		Politicien pol2 = new Politicien("127.0.0.1", 12345);
		Thread t3 = new Thread(pol2);
		t3.start();
	
	}

}
