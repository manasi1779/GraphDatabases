import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.Scanner;

public class CreateIgraphQueries {
	
	String fileEntry = "C:/Manasi/workspace/TDM/Graph/data/iGraph/query";

	public static void main(String[] args) {
		CreateIgraphQueries createModule = new CreateIgraphQueries();
		createModule.createFile("human_q10.igraph");
		createModule.createFile("yeast_q10.igraph");
	}
	
	public void createFile(String filename){
		File file = new File(fileEntry+"/"+filename);
		int dotIndex = filename.indexOf('.');
		String newName = filename.substring(0, dotIndex);
		try {
			Scanner s = new Scanner(file);
			s.nextLine();
			for(int i = 0; i < 10; i++){
				File queryFile = new File(fileEntry+"/"+newName+"_"+i+".igraph");
				PrintWriter pw = new PrintWriter(queryFile);				
				while(s.hasNextLine()){
					String line = s.nextLine();
					if(line.contains("#"))
						break;
					else{
						pw.println(line);
					}
				}
				pw.close();
			}
			s.close();
			
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
