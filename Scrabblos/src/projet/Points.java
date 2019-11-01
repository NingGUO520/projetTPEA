package projet;

import java.util.Map;

public class Points {
	
	public static int getScore(char lettre) {
		switch(lettre) {
		case 'a': return 1;
		case 'e': return 1;
		case 'i': return 1;
		case 'n': return 1;
		case 'o': return 1;
		case 'r': return 1;
		case 's': return 1;
		case 't': return 1;
		case 'u': return 1;
		case 'l': return 1;
		
		case 'd': return 2;
		case 'm': return 2;
		case 'g': return 2;
		
		case 'b': return 3;
		case 'c': return 3;
		case 'p': return 3;
	
		case 'f': return 4;
		case 'h': return 4;
		case 'v': return 4;
		
		case 'j': return 8;
		case 'q': return 8;
		
		case 'k': return 10;
		case 'w': return 10;
		case 'x': return 10;
		case 'y': return 10;
		case 'z': return 10;
		
		
		default:return 0;
		}
	}
	
	public static int score_mot(String mot) {
		int score  = 0;
		for(int i=0;i<mot.length();i++) {
			score+=getScore(mot.charAt(i));
		}
		return score;
	}
	

}
