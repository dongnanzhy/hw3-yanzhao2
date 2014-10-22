package edu.cmu.lti.f14.hw3.hw3_yanzhao2.annotators;

import java.util.*;
import java.util.Map.Entry;

import org.apache.uima.analysis_component.JCasAnnotator_ImplBase;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSList;
import org.apache.uima.jcas.cas.IntegerArray;
import org.apache.uima.jcas.cas.StringArray;
import org.apache.uima.jcas.tcas.Annotation;

import edu.cmu.lti.f14.hw3.hw3_yanzhao2.typesystems.Document;
import edu.cmu.lti.f14.hw3.hw3_yanzhao2.typesystems.Token;
import edu.cmu.lti.f14.hw3.hw3_yanzhao2.utils.Utils;

public class DocumentVectorAnnotator extends JCasAnnotator_ImplBase {

	@Override
	public void process(JCas jcas) throws AnalysisEngineProcessException {

		FSIterator<Annotation> iter = jcas.getAnnotationIndex().iterator();
		if (iter.isValid()) {
			iter.moveToNext();
			Document doc = (Document) iter.get();
			createTermFreqVector(jcas, doc);
		}

	}

	/**
   * A basic white-space tokenizer, it deliberately does not split on punctuation!
   *
	 * @param doc input text
	 * @return    a list of tokens.
	 */

	List<String> tokenize0(String doc) {
	  List<String> res = new ArrayList<String>();
	  
	  // split based on white-space(one or more)
	  for (String s: doc.split("\\s+"))
	    res.add(s);
	  return res;
	}

	/**
	 * 
	 * @param jcas
	 * @param doc
	 */

	private void createTermFreqVector(JCas jcas, Document doc) {

		String docText = doc.getText();
    HashMap<String, Integer> map = new HashMap<String, Integer>();
    List<String> res =  tokenize0(docText);
    for (int i = 0; i < res.size(); i++) {
      String word = res.get(i);
      if (map.containsKey(word)) {
        map.put(word, map.get(word) + 1);
      } else
        map.put(word, 1);
    }
    
    Iterator<Entry<String, Integer>> it = map.entrySet().iterator();
    List<Token> tokens = new LinkedList<Token>();
    while (it.hasNext()) {
      Map.Entry<String, Integer> src = it.next();
      Token token = new Token(jcas);
      token.setText(src.getKey());
      token.setFrequency(src.getValue());
      token.addToIndexes();
      tokens.add(token);
      //     System.out.print( src.getKey() +"  ");
    }
    doc.setTokenList(Utils.fromCollectionToFSList(jcas, tokens));

	}

}
