package mpi.ner.taggers;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

import mpi.aida.config.AidaConfig;
import mpi.ner.NER;
import mpi.ner.Name;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.stanford.nlp.ling.CoreAnnotations.CharacterOffsetBeginAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.CharacterOffsetEndAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.NamedEntityTagAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.CoreMap;

public class StanfordNER implements NER {

	private static final Logger logger = LoggerFactory
			.getLogger(StanfordNER.class);

	private StanfordCoreNLP stanfordCoreNLP = null;
	
  // German Models
  private final String GERMAN_NER_HGC = 
      "resources/corenlp/germanmodels/ner/hgc_175m_600.crf.ser.gz";
  private final String GERMAN_POS_HGC = 
      "resources/corenlp/germanmodels/pos/german-hgc.tagger";

	private HashMap<String, String> expectedSuccessdingTags = null;

	public StanfordNER() {
		logger.info("Initilaizing Stanford NER");
		Properties props = new Properties();
		switch(AidaConfig.getLanguage()) {
		   case en:
		     props.put("annotators", "tokenize, ssplit, pos, lemma, ner"); 		     
		     break;
		   case de:
	        props.put("annotators", "tokenize, ssplit, pos, lemma, ner");
	        props.put("pos.model", GERMAN_POS_HGC);
	        props.put("ner.model", GERMAN_NER_HGC);
	        props.put("ner.useSUTime", "false"); //false not for english
	        props.put("ner.applyNumericClassifiers", "false"); //false not for english
		     break;
		   default:
		       break;
		  
		}
		stanfordCoreNLP = new StanfordCoreNLP(props, true);
		
		expectedSuccessdingTags = new HashMap<String, String>();
		expectedSuccessdingTags.put("LOCATION", "LOCATION");
		expectedSuccessdingTags.put("I-LOC", "I-LOC");
		expectedSuccessdingTags.put("B-LOC", "I-LOC");
		expectedSuccessdingTags.put("PERSON", "PERSON");
		expectedSuccessdingTags.put("I-PER", "I-PER");
		expectedSuccessdingTags.put("B-PER", "I-PER");
		expectedSuccessdingTags.put("ORGANIZATION", "ORGANIZATION");
		expectedSuccessdingTags.put("I-ORG", "I-ORG");
		expectedSuccessdingTags.put("B-ORG", "I-ORG");
		expectedSuccessdingTags.put("MISC", "MISC");
		expectedSuccessdingTags.put("I-MISC", "I-MISC");
		expectedSuccessdingTags.put("B-MISC", "I-MISC");
	}

	@Override
	public List<Name> findNames(String docId, String text) {
		if (text.trim().length() == 0) {
			return new LinkedList<Name>();
		}

		List<Name> names = new LinkedList<>();
		Annotation document = new Annotation(text);
		stanfordCoreNLP.annotate(document);
		List<CoreMap> sentences = document.get(SentencesAnnotation.class);
		String previousTag = null;
		int start = 0, end = 0;
		for (CoreMap sentence : sentences) {
			for (CoreLabel token : sentence.get(TokensAnnotation.class)) {

				String currentTokenTag = token
						.get(NamedEntityTagAnnotation.class);

				if (previousTag == null) {
					if(expectedSuccessdingTags.containsKey(currentTokenTag)) {
					previousTag = currentTokenTag;
					start = token.get(CharacterOffsetBeginAnnotation.class);
					end = token.get(CharacterOffsetEndAnnotation.class);
					}
				} else if (expectedSuccessdingTags.get(previousTag).equals(
						currentTokenTag)) {
					end = token.get(CharacterOffsetEndAnnotation.class);
				} else {
					Name name = new Name(text.substring(start, end), start);
					name.setNerAnnotatorId(getId());
					names.add(name);
					previousTag = null;
					if(expectedSuccessdingTags.containsKey(currentTokenTag)) {
						previousTag = currentTokenTag;
						start = token.get(CharacterOffsetBeginAnnotation.class);
						end = token.get(CharacterOffsetEndAnnotation.class);
					}
				}

			}

		}

		return names;
	}

	@Override
	public String getId() {
		return "StanfordNER";
	}

}
