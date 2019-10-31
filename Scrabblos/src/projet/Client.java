package projet;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.net.UnknownHostException;

import org.json.JSONException;
import org.json.JSONObject;

public class Client {
	
	Socket s ;
	public void connexion() throws JSONException {

		JSONObject object = new JSONObject();
		object.put("lettre", 'A');
		System.out.print("debut");
		try {
			 s = new Socket("127.0.0.1",12345);
			OutputStreamWriter out = new OutputStreamWriter(s.getOutputStream());
//			out.write(object.toString()+"\n");
			out.write("bonjour\n");

		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
	public String getLettreDepart() {
		try {
			InputStreamReader in = new InputStreamReader(s.getInputStream());
			in.read();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		
		return null;
	}

	public static void main(String[] args) throws JSONException {

		Client client = new Client();
		client.connexion();

	}

}
