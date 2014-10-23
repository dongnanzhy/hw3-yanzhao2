package edu.cmu.lti.f14.hw3.hw3_yanzhao2.casconsumers;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.Map.Entry;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASException;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.collection.CasConsumer_ImplBase;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSList;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.ResourceProcessException;
import org.apache.uima.util.ProcessTrace;

import edu.cmu.lti.f14.hw3.hw3_yanzhao2.typesystems.Document;
import edu.cmu.lti.f14.hw3.hw3_yanzhao2.typesystems.Token;
import edu.cmu.lti.f14.hw3.hw3_yanzhao2.utils.Utils;

/** Compute MRR
 * 
 * @author Yan Zhao
 * @version 2.0 October, 2014.
 */
public class RetrievalEvaluator extends CasConsumer_ImplBase {

	/** query id number **/
	public ArrayList<Integer> qIdList;

	/** query and text relevant values **/
	public ArrayList<ArrayList<Integer>> relList;

  /** ranks for each relevant answer in order of the query id **/
  public ArrayList<Integer> rankList;

  /**
   * dictionary of term frequency hashmap
   **/
  public ArrayList<ArrayList<HashMap<String, Integer>>> dic;

  /** store the original text of the relevant answer **/
  public ArrayList<String> relAnsList;

  /** bufferedwriter to write the report.txt **/
  public BufferedWriter writer;

  /** output file name **/
  public String fileName;
  
  /**
   * initialize the parameter.
   */
		
	public void initialize() throws ResourceInitializationException {

		qIdList = new ArrayList<Integer>();

		relList = new ArrayList<ArrayList<Integer>>();
		
    rankList = new ArrayList<Integer>();
		
    dic = new ArrayList<ArrayList<HashMap<String, Integer>>>();

    relAnsList = new ArrayList<String>();
    
    fileName = (String) getConfigParameterValue("output");

    try {
      writer = new BufferedWriter(new FileWriter(new File(fileName), false));
//      writer.write("test");
    } catch (IOException e) {
      e.printStackTrace();
    }

	}

	/**
	 * Construct the global word dictionary. Keep the word
	 * frequency for each sentence
	 * @param aCas
	 */
	@Override
	public void processCas(CAS aCas) throws ResourceProcessException {

		JCas jcas;
		try {
			jcas =aCas.getJCas();
		} catch (CASException e) {
			throw new ResourceProcessException(e);
		}

		FSIterator it = jcas.getAnnotationIndex(Document.type).iterator();
	
		if (it.hasNext()) {
			Document doc = (Document) it.next();

			//Make sure that your previous annotators have populated this in CAS
			FSList fsTokenList = doc.getTokenList();
			ArrayList<Token> tokenList = Utils.fromFSListToCollection(fsTokenList, Token.class);

      if (doc.getRelevanceValue() == 99) {
        qIdList.add(doc.getQueryID());
        ArrayList<HashMap<String, Integer>> queList = new ArrayList<HashMap<String, Integer>>();
        queList.add(tokenList2Map(tokenList));
        dic.add(queList);
        relList.add(new ArrayList<Integer>());
      } else {
        dic.get(dic.size() - 1).add(tokenList2Map(tokenList));
        relList.get(relList.size() - 1).add(doc.getRelevanceValue());
        if (doc.getRelevanceValue() == 1)
          relAnsList.add(doc.getText());
      }

		}
	}

	/**
	 * Compute Cosine Similarity and rank the retrieved sentences 
	 * Compute the MRR metric
	 */
	@Override
	public void collectionProcessComplete(ProcessTrace arg0)
			throws ResourceProcessException, IOException {

		super.collectionProcessComplete(arg0);
		//compute the cosine similarity measure
		//compute the rank of retrieved sentences
    int position;
    for (int i = 0; i < qIdList.size(); i++) {
      ArrayList<Double> similar = new ArrayList<Double>();
      HashMap<String, Integer> queMap = dic.get(i).get(0);
      for (int j = 1; j < dic.get(i).size(); j++) {
        similar.add(computeCosineSimilarity(queMap, dic.get(i).get(j)));
//        similar.add(computeJaccardSimilarity(queMap, dic.get(i).get(j)));
//        similar.add(computeDiceSimilarity(queMap, dic.get(i).get(j)));
      }

      position = getPosition(relList.get(i));
      int rank = getRank(similar, position);
      rankList.add(rank);
      String relAns = relAnsList.get(i);
      writer.write(String.format("cosine=%.4f", similar.get(position)));
      writer.write("\t" + "rank=" + rank + "\tqid=" + qIdList.get(i) + "\t" + "rel=" + 1 + "\t"
              + relAns + "\n");
    }
    //compute the metric:: mean reciprocal rank
    double metric_mrr = compute_mrr();
    writer.write(String.format("MRR=%.4f", metric_mrr));
    writer.close();
		
		System.out.printf(" (MRR) Mean Reciprocal Rank ::" + "MRR=%.4f" + "\n", metric_mrr);
	}

	/**
	 * @param queryVector 
	 * @param docVector
	 * @return cosine_similarity
	 */
	private double computeCosineSimilarity(Map<String, Integer> queryVector,
			Map<String, Integer> docVector) {
		double cosine_similarity=0.0;
    double doc = 0.0;
    double que = 0.0;
    double dotProduct = 0.0;
    
    Iterator<Entry<String, Integer>> it = queryVector.entrySet().iterator();
    while (it.hasNext()) {
      Map.Entry<String, Integer> queMap = it.next();
      if (docVector.containsKey(queMap.getKey()))
        dotProduct += queMap.getValue() * docVector.get(queMap.getKey());
      que += queMap.getValue() * queMap.getValue();

    }
    it = docVector.entrySet().iterator();
    while (it.hasNext()) {
      Map.Entry<String, Integer> docMap = it.next();

      doc += docMap.getValue() * docMap.getValue();
    }

    cosine_similarity = dotProduct / (Math.sqrt(doc) * Math.sqrt(que));

		return cosine_similarity;
	}

	 /**
   * @param queryVector 
   * @param docVector
   * @return jaccard_similarity
   */
  private double computeJaccardSimilarity(Map<String, Integer> queryVector,
      Map<String, Integer> docVector) {
    double jaccard_similarity=0.0;
    double doc = 0.0;
    double que = 0.0;
    double aAndB = 0.0;
    double aOrB= 0.0;  
    
    Iterator<Entry<String, Integer>> it = queryVector.entrySet().iterator();
    while (it.hasNext()) {
      Map.Entry<String, Integer> queMap = it.next();
      if (docVector.containsKey(queMap.getKey()))
        aAndB += queMap.getValue() * docVector.get(queMap.getKey());
      que += queMap.getValue() * queMap.getValue();

    }
    it = docVector.entrySet().iterator();
    while (it.hasNext()) {
      Map.Entry<String, Integer> docMap = it.next();

      doc += docMap.getValue() * docMap.getValue();
    }
    
    aOrB = que + doc - aAndB;
    jaccard_similarity = aAndB / aOrB;
    return jaccard_similarity;
  }
  
  /**
  * @param queryVector 
  * @param docVector
  * @return dice_similarity
  */
 private double computeDiceSimilarity(Map<String, Integer> queryVector,
     Map<String, Integer> docVector) {
   double dice_similarity=0.0;
   double doc = 0.0;
   double que = 0.0;
   double aAndB = 0.0; 
   
   Iterator<Entry<String, Integer>> it = queryVector.entrySet().iterator();
   while (it.hasNext()) {
     Map.Entry<String, Integer> queMap = it.next();
     if (docVector.containsKey(queMap.getKey()))
       aAndB += queMap.getValue() * docVector.get(queMap.getKey());
     que += queMap.getValue() * queMap.getValue();

   }
   it = docVector.entrySet().iterator();
   while (it.hasNext()) {
     Map.Entry<String, Integer> docMap = it.next();

     doc += docMap.getValue() * docMap.getValue();
   }
   
   dice_similarity = 2 * aAndB / (que + doc);
   return dice_similarity;
 }
 
	/**
	 * 
	 * @return mrr
	 */
	private double compute_mrr() {
		double metric_mrr=0.0;

    for (int i : rankList) {
      metric_mrr += (double) 1.0 / i;
    }
    metric_mrr = metric_mrr / rankList.size();
		
		return metric_mrr;
	}

  /**
   * change to token list to a term frequency map
   * 
   * @param list
   * @return map
   */
  private HashMap<String, Integer> tokenList2Map(ArrayList<Token> list) {
    HashMap<String, Integer> map = new HashMap<String, Integer>();
    for (Token token : list) {
      map.put(token.getText(), token.getFrequency());
    }
    return map;
  }
  
  /**
   * get the position of the relevant answer in the list
   * 
   * @param list
   * @return position
   */
  private int getPosition(ArrayList<Integer> list) {
    int pos = 0;
    for (int i = 0; i < list.size(); i++) {
      if (list.get(i) == 1)
        pos = i;
    }
    return pos;
  }

  /**
   * Get the rank of the relevant answer according to the similarity list
   * 
   * @param similarities
   * @param position
   * @return rank
   */
  private int getRank(ArrayList<Double> list, int position) {
    double temp = list.get(position);
    int rank = 1;
    for (double i : list)
      if (i > temp)
        rank++;
    return rank;
  }
  
}
