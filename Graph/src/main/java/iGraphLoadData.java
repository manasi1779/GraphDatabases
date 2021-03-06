import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Scanner;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.Transaction;
import org.neo4j.unsafe.batchinsert.BatchInserter;
import org.neo4j.unsafe.batchinsert.BatchInserters;

public class iGraphLoadData {

	String dbpath;
	GraphDatabaseService db;
	Transaction tx;
	BatchInserter insert;

	public static void main(String[] args) {
		iGraphLoadData iGraphData = new iGraphLoadData();
		iGraphData.loadData();
	}
	
	public iGraphLoadData(){
		dbpath = "D:/neo4j-enterprise-3.1.1-windows/neo4j-enterprise-3.1.1/data/databases/graph.db";
	}
	
	public void loadData(){
		File file = new File("C:/Manasi/workspace/TDM/Graph/data/iGraph/target");
		listFilesForFolder(file);
	}

	public void listFilesForFolder(final File folder) {
	    for(final File fileEntry : folder.listFiles()){
	    	loadFile(fileEntry);
	    	//loadQueries(fileEntry);
	    }
	}
	
	public void loadFile(File fileName){
		int dotIndex = fileName.getName().lastIndexOf('.');
		String newDir = fileName.getName().substring(0, dotIndex); 
		newDir = dbpath+"/"+newDir;
		File dir = new File(newDir);		
		dir.mkdir();
		insertiGraphData(fileName, dir);		
	}	
	
	/**
	 * For loading queries
	 * @param fileName
	 */
	public void loadQueries(File fileName){
		int dotIndex = fileName.getName().lastIndexOf('.');
		String newDir = fileName.getName().substring(0, dotIndex); 
		insertiGraphQueries(fileName, newDir);
	}
	
	public void insertiGraphData(File fileName, File dbPath){
		try(Scanner scanner = new Scanner(fileName)){
			insert = BatchInserters.inserter(dbPath);
			scanner.nextLine();
			String values[] ;
			while(true){
				values = scanner.nextLine().split(" ");
				if(values[0].equals("v")){
					Label[] labels = new Label[values.length-2];
					for(int j = 0; j < values.length-2; j++){
						labels[j] = Label.label(values[j+2]);
					}				 
					insert.createNode(Long.parseLong(values[1]), null, labels);			
				}
				else{
					break;
				}
			}			
			createiGraphRelationships(scanner, values);			
			insert.shutdown();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e){
			e.printStackTrace();
		}	
	}
	
	public void insertiGraphQueries(File fileName, String dirName){
		try(Scanner scanner = new Scanner(fileName)){
			String values[] ;
			values = scanner.nextLine().split(" ");
			String newDir = dbpath+"/"+dirName+"_"+values[2];			
			insert = BatchInserters.inserter(new File(newDir));
			while(scanner.hasNextLine()){
				values = scanner.nextLine().split(" ");
				if(values[0].equals("v")){
					Label[] labels = new Label[values.length-2];
					for(int j = 0; j < values.length-2; j++){
						labels[j] = Label.label(values[j+2]);
					}				 
					insert.createNode(Long.parseLong(values[1]), null, labels);			
				}
				else{
					values = createiGraphRelationships(scanner, values);
					if(values == null){
						break;
					}
					newDir = dbpath+"/"+dirName+"_"+values[2];					
					insert.shutdown();
					insert = BatchInserters.inserter(new File(newDir));
				}
			}					
			insert.shutdown();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e){
			e.printStackTrace();
		}	
	}
	
	/**
	 * Create relationship between existing nodes
	 * @param s
	 * @param lastline
	 * @return
	 */
	public String[] createiGraphRelationships(Scanner s, String[] lastline){
		if(insert.nodeExists(Long.parseLong(lastline[1])) && insert.nodeExists(Long.parseLong(lastline[2]))){
			insert.createRelationship(Long.parseLong(lastline[1]), Long.parseLong(lastline[2]), RelationshipType.withName("connected"), null);
		}
		while(s.hasNextLine()){
			String edgeLine = s.nextLine();
			String edgeNodes[] = edgeLine.split(" ");
			if(edgeNodes[0].equals("e")){
				if(insert.nodeExists(Long.parseLong(edgeNodes[1])) && insert.nodeExists(Long.parseLong(edgeNodes[2]))){
					insert.createRelationship(Long.parseLong(edgeNodes[1]), Long.parseLong(edgeNodes[2]), RelationshipType.withName("connected"), null);
				}
			}
			else{
				return edgeNodes;
			}
		}
		return null;
	}

}
