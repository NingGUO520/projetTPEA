package projet;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;

public class LanchePoliticien {
	public static void main(String args[]) throws NoSuchAlgorithmException, IOException {
		Politicien pol = new Politicien("127.0.0.1", 12345);
		Thread t1 = new Thread(pol);
		t1.start();
		
		Politicien pol1 = new Politicien("127.0.0.1", 12345);
		Thread t4 = new Thread(pol1);
		t4.start();
	
	}

}
