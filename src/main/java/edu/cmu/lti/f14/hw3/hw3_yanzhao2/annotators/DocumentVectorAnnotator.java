package edu.cmu.lti.f14.hw3.hw3_yanzhao2.annotators;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.util.*;
import java.util.Map.Entry;

import org.apache.commons.io.FileUtils;
import org.apache.uima.analysis_component.JCasAnnotator_ImplBase;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.ResourceInitializationException;

//import edu.stanford.nlp.objectbank.TokenizerFactory;
import edu.stanford.nlp.process.PTBTokenizer.PTBTokenizerFactory;
import edu.stanford.nlp.process.Tokenizer;
import edu.stanford.nlp.process.TokenizerFactory;
import edu.cmu.lti.f14.hw3.hw3_yanzhao2.typesystems.Document;
import edu.cmu.lti.f14.hw3.hw3_yanzhao2.typesystems.Token;
import edu.cmu.lti.f14.hw3.hw3_yanzhao2.utils.StanfordLemmatizer;
import edu.cmu.lti.f14.hw3.hw3_yanzhao2.utils.Utils;

public class DocumentVectorAnnotator extends JCasAnnotator_ImplBase {

	File file;


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

	List<String> tokenize(String doc) {
	  List<String> res = new ArrayList<String>();
	  TokenizerFactory factory = PTBTokenizerFactory.newTokenizerFactory();  
	  // split based on white-space(one or more)
	  for (String s: doc.split("\\s+")) {
		  Tokenizer tokenizer = factory.getTokenizer(new StringReader(s));
		  Object temp = tokenizer.peek();
		  s = temp.toString();
		  if (!s.matches("['a-zA-Z0-9_]*$") )
			  continue;
//		  System.out.print(s + "  ");
		  res.add(s);
	  }

	  return res;
	}

	/**
	 * 
	 * @param jcas
	 * @param doc
	 */

	private void createTermFreqVector(JCas jcas, Document doc) {

		HashMap<String,Integer> stopWords = new HashMap<String,Integer>();
		file = new File("src/main/resources/stopwords.txt");
		
        BufferedReader reader = null;
            try {
				reader = new BufferedReader(new FileReader(file));
			} catch (FileNotFoundException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
            String temp = null;
            try {
				while ((temp = reader.readLine()) != null) {
//					System.out.print(temp);
					stopWords.put(temp + " ", 1);
				}
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
            try {
				reader.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
//		System.out.println(stopWords.size());
		
		
		String docText = doc.getText();
    HashMap<String, Integer> map = new HashMap<String, Integer>();
    List<String> res =  tokenize(docText);
    for (int i = 0; i < res.size(); i++) {
      String word = res.get(i);
      /* 
       * use Stanford lemmatizer to transform different types of tokens
       */
      word = StanfordLemmatizer.stemText(word);
//      System.out.print(stopWords.isEmpty());
//	  System.out.print(word + " " + word.length() + " ");

      if (stopWords.containsKey(word)) {
 //   	 System.out.println("df");
    	  continue;
      }

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
