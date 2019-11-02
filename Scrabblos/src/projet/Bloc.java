package projet;

import java.security.NoSuchAlgorithmException;

import org.json.JSONObject;

public class Bloc {
	private String head;
	private Bloc previous=null;
	private JSONObject content=null;
	
	public Bloc(JSONObject content) throws NoSuchAlgorithmException {
		head = Utils.hash("");
		this.content = content;
	}
	
	public Bloc(JSONObject content, Bloc previous) throws NoSuchAlgorithmException {
		this(content);
		this.previous = previous;
	}
	public String getHash() throws NoSuchAlgorithmException {
		return Utils.hash(content.toString());
	}
	
	public JSONObject getContent() {
		return content;
	}
	
	
	//Vérification pas de modification de bloc
	public boolean is_valid() throws NoSuchAlgorithmException {
		if(previous != null) {
			if(previous.getHash().equals(head)) return previous.is_valid();
			else return false;
		}
		else {
			return true;
		}
	}
		

}
