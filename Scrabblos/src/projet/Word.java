package projet;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;

public class Word {

	private List<Letter> word;
	public String wordAsString="";
	private String head;
	private String politician;
	private String signature;

	public Word(String s) throws JSONException {
		JSONObject object = new JSONObject(s);
		Map<String, Object> map= Utils.jsonToMap(object);
		word = new ArrayList<Letter>();
		for(Object l : ((List) map.get("word"))) {
			this.word.add(new Letter(l.toString()));
		}
		this.head = (String) map.get("head");
		this.politician = (String) map.get("politician");
		this.signature = (String) map.get("signature");
		setWordAsString();
	}
	
	public void setWordAsString() {
		for(Letter l : this.word) {
			wordAsString+=l.letter;
		}
	}
	
	public static List<String> ListOfWordsToListOfString(List<Word> words){
		List<String> result = new ArrayList<String>();
		for(Word w: words) {
			result.add(w.wordAsString);
		}
		return result;
	}
	
	@Override
	public String toString() {
		return this.wordAsString;
	}
}
