package org.staccato;

import org.jfugue.parser.ParserContext;

/**
 * Disjoin notes that are to be played in the same beat.
 * They are added with the ',' symbol. 
 * So "sa,re" shall play sa of half of default duration and re for the rest half of the default duration
 * As of now expects 
 * @author niranjan
 *
 */
public class DisjoinSwarsPreprocessor  implements Preprocessor {
	
	private static DisjoinSwarsPreprocessor instance;
	
	public static DisjoinSwarsPreprocessor getInstance() {
		if (instance == null) {
			instance = new DisjoinSwarsPreprocessor();
		}
		return instance;
	}

	@Override
	public String preprocess(String musicString, ParserContext context) {
		StringBuilder sBuff = new StringBuilder();
		String[] tokens = musicString.split(" ");
		//Each music string
		for(int i=0; i< tokens.length; i++) {
			String token = tokens[i];
			if (token.contains(",")) {
				String[] s = token.split(",");
				int durationNumber1 = s.length; 
				for(int j=0; s.length > 1 && j<s.length; j++) {
					sBuff.append(s[j]);
					sBuff.append(durationLiteral(durationNumber1));
					sBuff.append(' ');
				}
			} else {
				sBuff.append(token);
				sBuff.append(' ');
			}
		}
		//none found
		if (tokens.length == 1) {
			return musicString;
		}
		return sBuff.toString().trim();
	}
	
	private char durationLiteral(int duration) {
		switch (duration) {
		case 1:
			return 'Q';
		case 2:
			return 'I';
		case 3:
			return 'S';
		case 4:
			return 'T';
		case 5:
			return 'X';
		case 6:
			return 'O';
		default:
			return 'Q';
		}
	}
}
