package projet;

import java.security.NoSuchAlgorithmException;

import org.json.JSONObject;

public class Bloc {
	private String head;
	private Bloc previous=null;
	private JSONObject content=null;
	
	public Bloc() throws NoSuchAlgorithmException {
		head = Utils.hash("");
	}
	
	public Bloc(JSONObject content, Bloc previous) throws NoSuchAlgorithmException {
		this();
		this.content = content;
		this.previous = previous;
		if (previous!=null && previous.getContent() !=null)	head = Utils.hash(previous.getContent().toString());
	}
	public String getHash() throws NoSuchAlgorithmException {
		if(content == null) {
			return Utils.hash("");
		}
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
