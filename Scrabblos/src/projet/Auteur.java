package projet;

import java.awt.image.ConvolveOp;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.net.Socket;
import java.nio.CharBuffer;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;

import javax.xml.bind.annotation.adapters.HexBinaryAdapter;

import org.json.JSONObject;



public class Auteur implements Runnable{
	private DataOutputStream outchan;
	private DataInputStream inchan;
	private Socket socket;
	private KeyPair pair;
	public Auteur(Socket s) throws IOException, NoSuchAlgorithmException{
		socket = s;
		inchan = new DataInputStream(socket.getInputStream());
		outchan = new DataOutputStream(s.getOutputStream());
		KeyPairGenerator kp = KeyPairGenerator.getInstance("DSA");
		pair = kp.generateKeyPair();
	}
	
	long little2big(long i) {
	    return (i&0xff)<<24 | (i&0xff00)<<8 | (i&0xff0000)>>8 | (i>>24)&0xff;
	}
	
	public boolean register() throws IOException, ClassNotFoundException {
		JSONObject obj = new JSONObject();
		String [] key = pair.getPublic().toString().split("\n");
		obj.put("register", "8f7935d9b9aae9bfabed887acf4951b6f32ec59e3baf3718e8eac4961f3efd36");
		String msg = obj.toString();
		long taille = msg.length();
		outchan.writeLong(taille);
		outchan.write(msg.getBytes("UTF-8"),0,(int)taille);
		long taille_ans = inchan.readLong();
		byte [] cbuf = new byte[(int)taille_ans];
		inchan.read(cbuf, 0, (int)taille_ans);
		System.out.println(new String(cbuf,"UTF-8"));
		return true;
	}
	
	public boolean getLetter_pool() throws IOException {
		JSONObject obj = new JSONObject();
		obj.put("listen","null");
		String msg = obj.toString();
		long taille = msg.length();
		outchan.writeLong(taille);
		outchan.writeUTF(msg);
		return true;
		
	}

	@Override
	public void run() {
		try {
			register();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
}
