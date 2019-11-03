package projet;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;

public class Word {

	public List<Letter> word;
	public String wordAsString="";
	public JSONObject wordAsObject;
	private String head;
	public String politician;
	private String signature;
	public int point=0;

	public Word(String s) throws JSONException {
		wordAsObject = new JSONObject(s);
		Map<String, Object> map= Utils.jsonToMap(wordAsObject);
		word = new ArrayList<Letter>();
		for(Object l : ((List) map.get("word"))) {
			this.word.add(new Letter(l.toString()));
		}
		this.head = (String) map.get("head");
		this.politician = (String) map.get("politician");
		this.signature = (String) map.get("signature");
		setWordAsString();
		setPoint();
	}
	
	public void setWordAsString() {
		for(Letter l : this.word) {
			wordAsString+=l.letter;
		}
	}
	
	public void setPoint() {
		for(char c : wordAsString.toCharArray()) {
			this.point+= Points.getScore(c);
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

	public int getPoint() {
		return point;
	}

}
