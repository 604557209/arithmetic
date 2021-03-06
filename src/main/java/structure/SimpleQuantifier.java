package structure;

import edu.illinois.cs.cogcomp.annotation.AnnotatorException;
import edu.illinois.cs.cogcomp.core.datastructures.textannotation.TextAnnotation;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.semgraph.SemanticGraphCoreAnnotations;
import edu.stanford.nlp.util.CoreMap;
import utils.Params;
import utils.Tools;

public class SimpleQuantifier {
	
	public static String decimalRegex = "(?:\\d+\\,\\d+,\\d+\\.\\d+|\\d+\\,\\d+,\\d+|"
			+ "\\d+\\,\\d+\\.\\d+|\\d+\\,\\d+|\\d*\\.\\d+|\\d+)";
	public static Map<String, Double> numberWords = new HashMap<String, Double>();
	public static Map<String, Double> units = new HashMap<String, Double>();
	public static Map<String, Double> tens = new HashMap<String, Double>();
	 
	public SimpleQuantifier() {
		numberWords.put( "zero",       0.0);
		numberWords.put( "one",		1.0);
		numberWords.put( "two",		2.0);
		numberWords.put( "three",      3.0);
		numberWords.put( "four",       4.0);
		numberWords.put( "five",       5.0);
		numberWords.put( "six",        6.0);
		numberWords.put( "seven",      7.0);
		numberWords.put( "eight",      8.0);
		numberWords.put( "nine",       9.0);
		numberWords.put( "ten",        10.0);
		units.put( "one", 	   1.0);
		units.put( "two", 	   2.0);
		units.put( "three",      3.0);
		units.put( "four",       4.0);
		units.put( "five",       5.0);
		units.put( "six",        6.0);
		units.put( "seven",      7.0);
		units.put( "eight",      8.0);
		units.put( "nine",       9.0);
	    // the teens
		numberWords.put( "eleven",     11.0);
		numberWords.put( "twelve",     12.0);
		numberWords.put( "thirteen",   13.0);
		numberWords.put( "fourteen",   14.0);
		numberWords.put( "fifteen",    15.0);
		numberWords.put( "sixteen",    16.0);
		numberWords.put( "seventeen",  17.0);
		numberWords.put( "eighteen",   18.0);
		numberWords.put( "nineteen",   19.0);
	    // multiples of ten
		numberWords.put( "twenty",     20.0);
		numberWords.put( "thirty",     30.0);
		numberWords.put( "forty",      40.0);
		numberWords.put( "fourty",     40.0);
		numberWords.put( "fifty",      50.0);
		numberWords.put( "sixty",      60.0);
		numberWords.put( "seventy",    70.0);
		numberWords.put( "eighty",     80.0);
		numberWords.put( "ninety",     90.0);
		tens.put( "twenty",     20.0);
		tens.put( "thirty",     30.0);
		tens.put( "forty",      40.0);
		tens.put( "fourty",     40.0);
		tens.put( "fifty",      50.0);
		tens.put( "sixty",      60.0);
		tens.put( "seventy",    70.0);
		tens.put( "eighty",     80.0);
		tens.put( "ninety",     90.0);
		
//		numberWords.put( "twice",      2.0);
//		numberWords.put( "double",     2.0);
//		numberWords.put( "thrice",     3.0);
//		numberWords.put( "triple",     3.0);
//		numberWords.put( "half",       0.5);
		
	}
	
	public List<QuantSpan> getSpans(String text) {
		Matcher matcher = Pattern.compile(decimalRegex).matcher(text);
		List<QuantSpan> qsList = new ArrayList<QuantSpan>();
		while(matcher.find()) {
			QuantSpan qs = new QuantSpan(Double.parseDouble(matcher.group().replace(",", "")),
					matcher.start(), matcher.end());
			qsList.add(qs);
		}
		if(Params.useIllinoisTools && Params.runDemo) {
			TextAnnotation ta = null;
			try {
				ta = Tools.pipeline.createBasicTextAnnotation("", "", text);
			} catch (AnnotatorException e) {
				e.printStackTrace();
			}
			for (int i = 0; i < ta.size(); ++i) {
				if (i < ta.size() - 1 && tens.containsKey(ta.getToken(i).toLowerCase()) &&
						units.containsKey(ta.getToken(i + 1).toLowerCase())) {
					QuantSpan qs = new QuantSpan(1.0 * tens.get(ta.getToken(i).toLowerCase()) +
							units.get(ta.getToken(i + 1).toLowerCase()),
							ta.getTokenCharacterOffset(i).getFirst(),
							ta.getTokenCharacterOffset(i + 1).getSecond());
					qsList.add(qs);
					i++;
					continue;
				}
				if (numberWords.containsKey(ta.getToken(i).toLowerCase())) {
					QuantSpan qs = new QuantSpan(
							1.0 * numberWords.get(ta.getToken(i).toLowerCase()),
							ta.getTokenCharacterOffset(i).getFirst(),
							ta.getTokenCharacterOffset(i).getSecond());
					qsList.add(qs);
				}
			}
		}
		if(Params.useStanfordTools && Params.runDemo) {
			List<CoreMap> sentences = Tools.annotateWithStanfordCoreNLP(text);
			List<List<CoreLabel>> tokens = new ArrayList<>();
			for(CoreMap sentence: sentences) {
				tokens.add(sentence.get(CoreAnnotations.TokensAnnotation.class));
			}
			for(int i=0; i<tokens.size(); ++i) {
				List<CoreLabel> ta = tokens.get(i);
				for(int j=0; j<ta.size(); ++j) {
					if (j < ta.size() - 1 && tens.containsKey(ta.get(j).word().toLowerCase()) &&
							units.containsKey(ta.get(j + 1).word().toLowerCase())) {
						QuantSpan qs = new QuantSpan(1.0 * tens.get(ta.get(j).word().toLowerCase()) +
								units.get(ta.get(j + 1).word().toLowerCase()),
								ta.get(j).beginPosition(),
								ta.get(j + 1).endPosition());
						qsList.add(qs);
						i++;
						continue;
					}
					if (numberWords.containsKey(ta.get(j).word().toLowerCase())) {
						QuantSpan qs = new QuantSpan(
								1.0 * numberWords.get(ta.get(j).word().toLowerCase()),
								ta.get(j).beginPosition(),
								ta.get(i).endPosition());
						qsList.add(qs);
					}
				}
			}
		}
		Collections.sort(qsList, new Comparator<QuantSpan>() {
			@Override
			public int compare(QuantSpan o1, QuantSpan o2) {
				return (int)Math.signum(o1.start-o2.start);
			}
		});
		List<QuantSpan> quantities = new ArrayList<>();
		for(QuantSpan span:qsList){
			boolean containsDigit = false;
			for(int i=span.start; i<span.end; ++i) {
				if(Character.isDigit(text.charAt(i))) {
					containsDigit = true;
					break;
				}
			}
			if(containsDigit){
				if(Character.isLowerCase(text.charAt(span.end-1)) ||
						Character.isUpperCase(text.charAt(span.end-1))) continue;
				if(span.start>0 && (Character.isLowerCase(text.charAt(span.start-1))
						|| Character.isUpperCase(text.charAt(span.start-1)))) continue;
				quantities.add(span);
			} else {
				quantities.add(span);
			}
		}
		for(QuantSpan qs : quantities) {
			if(text.substring(Math.min(qs.end, text.length()-1),
					Math.min(qs.end+2, text.length())).contains("%")) {
				qs.val *= 0.01;
			}
		}
		return quantities;
	}
	
	public static void main(String args[]) {
		SimpleQuantifier sq = new SimpleQuantifier();
		for(QuantSpan qs : sq.getSpans("I have twenty six eggs and 7 oranges.")) {
			System.out.println(qs);
		}
	}
	
}
