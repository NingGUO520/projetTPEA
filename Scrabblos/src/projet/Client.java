package projet;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.net.UnknownHostException;

import org.json.JSONObject;

public class Client {

	public static void main(String[] args) {



		JSONObject object = new JSONObject();
		object.put("lettre", 'A');
		System.out.print("debut");
		try {
			Socket s = new Socket("127.0.0.1",12345);
			OutputStreamWriter out = new OutputStreamWriter(s.getOutputStream());
//			out.write(object.toString()+"\n");
			out.write("bonjour\n");

		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

}
