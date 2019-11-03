package projet.Utils;

import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;

public class Letter {
	public String letter;
	public int period;
	public String head;
	public String author;
	public String signature;
	
	public Letter(String s) throws JSONException {
		JSONObject object = new JSONObject(s);
		Map<String, Object> map= Utils.jsonToMap(object);
		this.letter = (String) map.get("letter");
		this.period = (int) map.get("period");
		this.head = (String) map.get("head");
		this.author = (String) map.get("author");
		this.signature = (String) map.get("signature");
	}
	
	@Override
	public String toString() {
		return this.letter;
	}

}
