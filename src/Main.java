import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;


public class Main {
	static String trainDir = "/Users/gkiko/Documents/NLP/assign3/pa3-sentiment/data/imdb1";
	static String testDir = "/Users/gkiko/Documents/NLP/assign3/pa3-sentiment/data/imdb1/pos";
	
	public static void main(String[] args) {
		SentimentAnal anal = new SentimentAnal();

		Main m = new Main();
		List<DirFiles> dirList = m.getDirWithFiles(trainDir);
		for(DirFiles dr : dirList){
			for(File file : dr){
				List<String> fileContent = m.getFileContent(file);
				anal.addInfoToClassifier(dr.getKlass(), fileContent);
			}
		}
		
		List<File> fileList = m.getFileList(testDir);
		String klass = fileList.get(0).getParentFile().getName();
		String res;
		double accuracy = 0;
		for(File f : fileList){
			res = anal.classifyInput(m.getFileContent(f));
			if(res.equals(klass)){
				accuracy++;
			}
		}
		System.out.println(accuracy/fileList.size());
		
	}
	
	private List<DirFiles> getDirWithFiles(String dirPath){
		List<DirFiles> ls = new ArrayList<Main.DirFiles>();
		for(File f : getFileList(dirPath)){
			if(!f.getName().startsWith(".") && f.isDirectory()){
				
				DirFiles dirFiles = new DirFiles(f.getName());
				List<File> fileList = Arrays.asList(f.listFiles());
				dirFiles.addFileList(fileList);
				ls.add(dirFiles);
			}
		}
		
		return ls;
	}
	
	public List<File> getFileList(String dirPath){
		File dir = new File(dirPath);
		return Arrays.asList(dir.listFiles());
	}
	
	public List<String> getFileContent(File f){
		try (BufferedReader input = new BufferedReader(new FileReader(f));){
	  		StringBuilder contents = new StringBuilder();

	  		for(String line = input.readLine(); line != null; line = input.readLine()) {
	  			contents.append(line);
	  			contents.append("\n");
	  		}
	  		input.close();

	  		return segmentWords(contents.toString());

	  	} catch(IOException e) {
	  		e.printStackTrace();
	  		System.exit(1);
	  		return null;
	  	} 
	}
	
	private List<String> segmentWords(String fileContent){
		List<String> ret = new ArrayList<String>();

		StringTokenizer tk = new StringTokenizer(fileContent);
		String word;
	  	while(tk.hasMoreTokens()){
	  		word = tk.nextToken();
	  		if(word.length() > 0) {
	  			ret.add(word);
	  		}
	  	}
	  	return ret;
	}
	
	private class DirFiles implements Iterable<File>{
		
		private List<File> files;
		
		private String klass;
		
		public DirFiles(String klass) {
			this.klass = klass;
			files = new ArrayList<File>();
		}
		
		public void addFileList(List<File> files){
			if(files == null){
				return;
			}
			this.files.addAll(files);
		}
		
		private String getKlass(){
			return klass;
		}

		@Override
		public Iterator<File> iterator() {
			return files.iterator();
		}
	}
}