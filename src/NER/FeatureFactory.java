package NER;

import helper.GlobalConstHelper;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

import org.json.JSONException;
import org.json.JSONObject;

public class FeatureFactory {
	private HashSet<String> firstNameSet;
	private HashSet<String> lastnameSet;
	private HashSet<String> personStatusSet;

	/**
	 * Add any necessary initialization steps for your features here. Using this
	 * constructor is optional. Depending on your features, you may not need to
	 * intialize anything.
	 */
	public FeatureFactory() {
		readFirstNamesData();
		System.out.println("First Names Read");
		readLastNamesData();
		System.out.println("Last Names Read");
		fillPersonStatusSet();
		System.out.println("Statuses");
	}
	
	private void fillPersonStatusSet(){
		personStatusSet = new HashSet<String>();
		for(int i = 0; i < GlobalConstHelper.PERSON_STATUSES.length; i++) {
			String status = GlobalConstHelper.PERSON_STATUSES[i];
			System.out.println(status + " " + i);
			personStatusSet.add(status);
			for(int j = 0; j < GlobalConstHelper.STATUS_NOUN_SUFFIXES.length; j++){
				String statusWithSuffix = status;
				statusWithSuffix += GlobalConstHelper.STATUS_NOUN_SUFFIXES[j];
				System.out.println(statusWithSuffix + " " + j);
				personStatusSet.add(statusWithSuffix);
			}
		}
	}
	
	private void readLastNamesData (){
		lastnameSet = new HashSet<String>();
		try {
			BufferedReader rd = new BufferedReader(new InputStreamReader(new FileInputStream("data/Gvarebi.txt"),  "UTF-8"));
			String line = rd.readLine();
			while(line != null){
				if(!lastnameSet.contains(line)){
					lastnameSet.add(line);
				}
				line = rd.readLine();
			}
			rd.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private void readFirstNamesData (){
		firstNameSet = new HashSet<String>();
		try {
			BufferedReader rd = new BufferedReader(new InputStreamReader(new FileInputStream("data/Saxelebi.txt"),  "UTF-8"));
			String line = rd.readLine();
			while(line != null){
				if(!firstNameSet.contains(line)){
					firstNameSet.add(line);
				}
				line = rd.readLine();
			}
			rd.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Words is a list of the words in the entire corpus, previousLabel is the
	 * label for position-1 (or O if it's the start of a new sentence), and
	 * position is the word you are adding features for. PreviousLabel must be
	 * the only label that is visible to this method.
	 */
	private List<String> computeFeatures(List<String> words,
			String previousLabel, int position) {

		List<String> features = new ArrayList<String>();

		String currentWord = words.get(position);

		// Baseline Features
		features.add("word=" + currentWord);
		features.add("prevLabel=" + previousLabel);
		features.add("word=" + currentWord + ", prevLabel=" + previousLabel);
		/**
		 * Warning: If you encounter "line search failure" error when running
		 * the program, considering putting the baseline features back. It
		 * occurs when the features are too sparse. Once you have added enough
		 * features, take out the features that you don't need.
		 */

		// TODO: Add your features here
		
		String previousWord = getPreviousWord(words, position);

		checkPersonStatus(features, previousWord);
		normalPersonNameChecks(features, currentWord, previousWord, previousLabel);
		personNamesStemCheck(features, currentWord, previousWord, previousLabel);
		
		return features;
	}
	
	private void checkPersonStatus(List<String> features, String previousWord){
		if(previousWord != null && personStatusSet.contains(previousWord))
			features.add("personStatus=" + previousWord);
	}
	
	private void normalPersonNameChecks(List<String> features, String currentWord, String previousWord, String previousLabel){
		if(firstNameSet.contains(currentWord) || lastnameSet.contains(currentWord))
			features.add("personNames");
		if(previousLabel.equals("PERSON") && previousWord != null && (firstNameSet.contains(previousWord + " " + currentWord)
												|| lastnameSet.contains(previousWord + " " + currentWord)))
			features.add("personNames");
	}
	
	private void personNamesStemCheck(List<String> features, String currentWord, String previousWord, String previousLabel){
		for(int i = 0; i < GlobalConstHelper.NAME_SUFFIXES.length; i++){
			String suffix = GlobalConstHelper.NAME_SUFFIXES[i];
			if(currentWord.endsWith(suffix)){
				String withoutSuffix = currentWord.substring(0, currentWord.length() - suffix.length());
				if(firstNameSet.contains(withoutSuffix) || lastnameSet.contains(withoutSuffix))
					features.add("personNames");
			}	
			if(previousWord != null && previousLabel.equals("PERSON") && !previousWord.endsWith(suffix) && currentWord.endsWith(suffix))
				features.add("withSuffix");
		}
	}
	
	private String getPreviousWord(List<String> words, int position){
		String previousWord = null;
		if(position > 0)
			previousWord = words.get(position - 1);
		return previousWord;
	}

	/** Do not modify this method **/
	public List<Datum> readData(String filename) throws IOException {
		List<Datum> data = new ArrayList<Datum>();
		BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(filename), "UTF-8"));
		for (String line = in.readLine(); line != null; line = in.readLine()) {
			if (line.trim().length() == 0) {
				continue;
			}
			String[] bits = line.split("\\s+");
			String word = bits[0];
			String label = bits[1];

			Datum datum = new Datum(word, label);
			data.add(datum);
		}

		return data;
	}

	/** Do not modify this method **/
	public List<Datum> readTestData(String ch_aux) throws IOException {

		List<Datum> data = new ArrayList<Datum>();

		for (String line : ch_aux.split("\n")) {
			if (line.trim().length() == 0) {
				continue;
			}
			String[] bits = line.split("\\s+");
			String word = bits[0];
			String label = bits[1];

			Datum datum = new Datum(word, label);
			data.add(datum);
		}

		return data;
	}

	/** Do not modify this method **/
	public List<Datum> setFeaturesTrain(List<Datum> data) {
		// this is so that the feature factory code doesn't accidentally use the
		// true label info
		List<Datum> newData = new ArrayList<Datum>();
		List<String> words = new ArrayList<String>();

		for (Datum datum : data) {
			words.add(datum.word);
		}

		String previousLabel = "O";
		for (int i = 0; i < data.size(); i++) {
			Datum datum = data.get(i);

			Datum newDatum = new Datum(datum.word, datum.label);
			newDatum.features = computeFeatures(words, previousLabel, i);
			newDatum.previousLabel = previousLabel;
			newData.add(newDatum);

			previousLabel = datum.label;
		}

		return newData;
	}

	/** Do not modify this method **/
	public List<Datum> setFeaturesTest(List<Datum> data) {
		// this is so that the feature factory code doesn't accidentally use the
		// true label info
		List<Datum> newData = new ArrayList<Datum>();
		List<String> words = new ArrayList<String>();
		List<String> labels = new ArrayList<String>();
		Map<String, Integer> labelIndex = new HashMap<String, Integer>();

		for (Datum datum : data) {
			words.add(datum.word);
			if (labelIndex.containsKey(datum.label) == false) {
				labelIndex.put(datum.label, labels.size());
				labels.add(datum.label);
			}
		}

		// compute features for all possible previous labels in advance for
		// Viterbi algorithm
		for (int i = 0; i < data.size(); i++) {
			Datum datum = data.get(i);

			if (i == 0) {
				String previousLabel = "O";
				datum.features = computeFeatures(words, previousLabel, i);

				Datum newDatum = new Datum(datum.word, datum.label);
				newDatum.features = computeFeatures(words, previousLabel, i);
				newDatum.previousLabel = previousLabel;
				newData.add(newDatum);

			} else {
				for (String previousLabel : labels) {
					datum.features = computeFeatures(words, previousLabel, i);

					Datum newDatum = new Datum(datum.word, datum.label);
					newDatum.features = computeFeatures(words, previousLabel, i);
					newDatum.previousLabel = previousLabel;
					newData.add(newDatum);
				}
			}

		}

		return newData;
	}

	/** Do not modify this method **/
	public void writeData(List<Datum> data, String filename) throws IOException {

		FileWriter file = new FileWriter(filename + ".json", false);

		for (int i = 0; i < data.size(); i++) {
			try {
				JSONObject obj = new JSONObject();
				Datum datum = data.get(i);
				obj.put("_label", datum.label);
				obj.put("_word", base64encode(datum.word));
				obj.put("_prevLabel", datum.previousLabel);

				JSONObject featureObj = new JSONObject();

				List<String> features = datum.features;
				for (int j = 0; j < features.size(); j++) {
					String feature = features.get(j).toString();
					featureObj.put("_" + feature, feature);
				}
				obj.put("_features", featureObj);
				obj.write(file);
				file.append("\n");
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		file.close();
	}

	/** Do not modify this method **/
	private String base64encode(String str) {
		Base64 base = new Base64();
		byte[] strBytes = str.getBytes();
		byte[] encBytes = base.encode(strBytes);
		String encoded = new String(encBytes);
		return encoded;
	}

}
