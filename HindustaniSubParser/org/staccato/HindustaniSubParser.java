package org.staccato;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jfugue.parser.ParserContext;
import org.jfugue.theory.Note;

public class HindustaniSubParser implements Subparser {
	
	private static HindustaniSubParser instance;
	private Logger logger = Logger.getLogger("org.jfugue");
	
	public static HindustaniSubParser getInstance() {
		if (instance == null) {
			instance = new HindustaniSubParser();
		}
		return instance;
	}
	
	private HindustaniSubParser() {
		logger.setLevel(Level.OFF);
	}
	

	private static Map<String,Integer> sargamList_12 = new HashMap<String,Integer>();
	private int baseSa = 61;//By default Sa mapped to C#
	
	private int parseIndex = 0;
	
	private Pattern notePatern = Pattern.compile("(SA|REK|RE|GAK|GA|MAT|MA|PA|DHK|DH|NIK|NI|KH)");
	
	static {
		sargamList_12.put("SA", 0);
		sargamList_12.put("REK", 1);
		sargamList_12.put("RE", 2);
		sargamList_12.put("GAK", 3);
		sargamList_12.put("GA", 4);
		sargamList_12.put("MA", 5);
		sargamList_12.put("MAT", 6);
		sargamList_12.put("PA", 7);
		sargamList_12.put("DHK", 8);
		sargamList_12.put("DH", 9);
		sargamList_12.put("NIK", 10);
		sargamList_12.put("NI", 11);
	}
	
	@Override
	public boolean matches(String music) {
		music = music.toUpperCase();
		String[] tokens = music.split(" ");
		boolean returnVal = false;
		if (tokens != null && tokens.length>0) {
			StringBuilder s = new StringBuilder(tokens[0].substring(0, 2));
			returnVal =  (sargamList_12.containsKey(s.toString()));
		} 
		if (music.startsWith("KH")) {
			returnVal = true;
		}
		return returnVal;
	}

	@Override
	public int parse(String music, ParserContext context) {
		music = music.toUpperCase();
		int returnindex = music.length();
		parseIndex = 0;
		
		Note note = getParsedNote(music,context);
		if (note != null && context != null) {
			context.fireNoteParsed(note);
		}
		return returnindex;
	}
	
	private Note getParsedNote(String music, ParserContext context) {

		Note note = null;
		
		note = getBaseSwar(music,context);

		if (note != null) {
			//Parse other stuff
			parseSaptak(music,note);
			
			//Parse duration
			parseDuration(music,note);
		}
		logger.info(" " +  note);
		//Cheap Debug
		System.out.print(' ');
		System.out.print(note);
		return note;
	}
	
	/**
	 * Calculate duration.
	 * @param music
	 * @param note
	 */
	private void parseDuration(String music, Note note) {
			
		boolean match = true;
		int durationNumber = 0;
		float totalduraton = 0.0f;
		
		do {
			if (endOfParsing(music)) return;

			char ch = music.charAt(parseIndex);
			
			switch (ch) {
			case 'W' : 
				durationNumber = 1; 
				break;
			case 'H' : 
				durationNumber = 2; 
				break;
			case 'Q' : 
				durationNumber = 4; 
				break;
			case 'I' : 
				durationNumber = 8; 
				break;
			case 'S' : 
				durationNumber = 16; 
				break;
			case 'T' : 
				durationNumber = 32; 
				break;
			case 'X' : 
				durationNumber = 64; 
				break;
			case 'O' : 
				durationNumber = 128; 
				break;
			default:
				durationNumber = 4;//The default duration will be a quarter note.
				match = false;
			}
			totalduraton = totalduraton + (1.0f/(float)durationNumber);
			parseIndex++;
		} while(match && !endOfParsing(music));
		note.setDuration(totalduraton);
	}

	/**
	 * A basic Note creation
	 * @param music
	 * @param context
	 * @param index
	 * @return
	 */
	private Note getBaseSwar(String music, ParserContext context) {
		Note note = null;
		Matcher noteMatcher = notePatern.matcher(music);
		while(noteMatcher.find()) {
			String strNote = music.substring(noteMatcher.start(),noteMatcher.end());
			parseIndex = noteMatcher.end();
			/*
			 * Check for setting the base SA note
			 */
			if (checkBaseSASetting(music)) {
				return null;//No more to do.
			}
			if (checkBlankNote(music)) {
				note = new Note();
				note.setRest(true);
				note.setDuration(0.0f);
			}
			if (sargamList_12.containsKey(strNote)) {
				int offset = sargamList_12.get(strNote);
				note = new Note(offset+baseSa,0.0f);
			}
		}
		return note;
	}

	private boolean checkBlankNote(String music) {
		if (music.startsWith("KH")) {
			return true;
		}
		return false;
	}

	/**
	 * Defined by >(Taar Saptak) or <(Mandra Saptak) after SA NIK etc
	 * Recursively defined for ati (Taar and Mandra)
	 * @param music
	 * @param index
	 * @param note
	 */
	private void parseSaptak(String music, Note note) {
		
		if (endOfParsing(music)) return;
		
		if (music.charAt(parseIndex) == '<' || music.charAt(parseIndex) == '>') {
			byte currNoteVal = note.getValue();
				if (music.charAt(parseIndex) == '<') {
					if (currNoteVal >=12) {//first octave, no more lower
						currNoteVal -=12;
					}
				} else {
					if (currNoteVal <=115) {//last octave, no more higher
						currNoteVal += 12;
					}
				}
				note.setValue(currNoteVal);
				parseIndex++;
				parseSaptak(music, note);
		}
		
	}

	private boolean endOfParsing(String music) {
		return (music.length() == parseIndex);
	}
	
	private int getBaseSA(String music, Integer index) {
		
		return Integer.parseInt(music.substring(index));
	}

	/**
	 * baseSaStr could be a note symbol or note value
	 * @param baseSaStr
	 * @return
	 */
	private boolean checkBaseSASetting(String baseSaStr) {
		
		if (baseSaStr.startsWith("SA=")) {
			String[] tokens = baseSaStr.split("=");
			int baseOffset = baseSa;
			if (tokens.length>1) {
				String newBaseStrg = tokens[1];
				try {
					baseOffset = Integer.parseInt(newBaseStrg);//new Base
				}catch(NumberFormatException nex) {//Try if as string
					Note tempNote = new Note(newBaseStrg);
					baseOffset = tempNote.getValue();
				}
				if (baseOffset >=0 && baseOffset <= 127) {//One can have the extreme values as "Sa", but would not be of much use
					baseSa = baseOffset;
				}
				parseIndex = baseSaStr.length();
				return true;
			}
		}
		return false;
	}

	public void setBaseSa(int note) {
		this.baseSa = note;
	}
	
	public int getBaseSa() {
		return this.baseSa;
	}
	
	/*
	 * Test quick
	 */
	/*public static void main(String[] args) {
		HindustaniSubParser par = new HindustaniSubParser();
		par.parse("ni", null);
		par.parse("ni<",null);
		par.parse("ni>",null);
	}*/

}
