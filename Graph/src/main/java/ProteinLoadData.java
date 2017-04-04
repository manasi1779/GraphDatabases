import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Scanner;

import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Transaction;
import org.neo4j.unsafe.batchinsert.BatchInserter;
import org.neo4j.unsafe.batchinsert.BatchInserters;

public class ProteinLoadData {
	
	String dbpath;
	Transaction tx;
	BatchInserter insert;

	public static void main(String[] args){
		ProteinLoadData proteinData = new ProteinLoadData();
		proteinData.loadData();		
	}
	
	public ProteinLoadData(){
		dbpath = "D:/neo4j-enterprise-3.1.1-windows/neo4j-enterprise-3.1.1/data/databases/graph.db";
	}
	
	public void loadData(){
		File file = new File("C:/Manasi/workspace/TDM/Graph/data/Proteins/Proteins/target");
		listFilesForFolder(file);
	}

	public void listFilesForFolder(final File folder) {
	    for(final File fileEntry : folder.listFiles()){
	    	loadFile(fileEntry);
	    }	    
	}
	
	public void loadFile(File fileName){
		int dotIndex = fileName.getName().lastIndexOf('.');
		String newDir = fileName.getName().substring(0, dotIndex); 
		newDir = dbpath+"/"+newDir;
		File dir = new File(newDir);		
		dir.mkdir();
		insertProteinData(fileName, dir);
		insert.shutdown();
	}	
	
	public void insertProteinData(File fileName, File dbPath){
		try(Scanner scanner = new Scanner(fileName)){
			int noOfNodes = scanner.nextInt();
			scanner.nextLine();
			insert = BatchInserters.inserter(dbPath);
			for(int i = 0; i < noOfNodes; i++){
				String line = scanner.nextLine();
				String[] values = line.split(" ");
				insert.createNode(Integer.parseInt(values[0]), null, Label.label(values[1]));
			}			
			createProteinRelationships(scanner, noOfNodes);			
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}		
	}
	
	public void createProteinRelationships(Scanner s, int nodes){		
		for(int i = 0; i < nodes; i++){
			int nofOfEdges = Integer.parseInt(s.nextLine());
			for(int j = 0; j < nofOfEdges; j++){
				String edgeLine = s.nextLine();
				String edgeNodes[] = edgeLine.split(" ");
				if(insert.nodeExists(Long.parseLong(edgeNodes[0])) && insert.nodeExists(Long.parseLong(edgeNodes[1]))){
					insert.createRelationship(Long.parseLong(edgeNodes[0]), Long.parseLong(edgeNodes[1]), RelationshipType.withName("connected"), null);
				}
			}
		}
	}
	
}
