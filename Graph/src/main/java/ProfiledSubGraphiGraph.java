import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Scanner;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.ResourceIterator;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.graphdb.factory.GraphDatabaseSettings;

public class ProfiledSubGraphiGraph {
	GraphDatabaseService db;
	String dbpath;
	Transaction tx;
	HashMap<Long, String> queryNodes;
	HashMap<Long, ArrayList<String>> queryEdges;
	HashMap<Long, HashSet<Long>> queryEdgeNodes;
	HashMap<Long, HashSet<Long>> searchSpace;
	ArrayList<Long> queryNodeList;
	HashMap<Long, Long> newSearchSpace;
	HashSet resultSet;
	static HashMap<String, String> querySet = new HashMap();
	
	public static void main(String[] args){
		ProfiledSubGraphiGraph profileMatching = new ProfiledSubGraphiGraph();
	}
	
	public ProfiledSubGraphiGraph(){
		dbpath = "D:/neo4j-enterprise-3.1.1-windows/neo4j-enterprise-3.1.1/data/databases/graph.db/";
		listFilesForFolder(new File("C:/Manasi/workspace/TDM/Graph/data/iGraph/query"));
	}
	public void listFilesForFolder(final File folder) {
	    for(final File fileEntry : folder.listFiles()){
	    	loadFile(fileEntry);
	    }
	}
	
	public void loadFile(File fileName){
		getQueryGraph(fileName);
		int dotIndex = fileName.getName().indexOf('_');
		String newDir = fileName.getName().substring(0, dotIndex); 
		newDir = dbpath+newDir;
		File dir = new File(newDir);
		System.out.println(fileName.getName());
		queryNeo4j(dir);		
	}
	
	public void getQueryGraph(File fileName){
		try {
			Scanner	s = new Scanner(fileName);	
			resultSet = new HashSet();
			queryNodes = new HashMap();
			queryEdges = new HashMap();
			searchSpace = new HashMap();
			queryNodeList = new ArrayList();
			queryEdgeNodes = new HashMap();
			while(s.hasNextLine()){
				String values[] = s.nextLine().split(" ");
				if(values[0].equals("v")){
					long nodeId = Long.parseLong(values[1]);
					queryNodes.put(nodeId, values[2]);
					queryEdges.put(nodeId, new ArrayList());
					queryEdgeNodes.put(nodeId, new HashSet());
					queryNodeList.add(nodeId);
					searchSpace.put(nodeId, new HashSet());
				}
				else{
					long no1 = Long.parseLong(values[1]);
					long no2 = Long.parseLong(values[2]);
					queryEdges.get(no1).add(queryNodes.get(no2));
					queryEdgeNodes.get(no1).add(no2);
					queryEdgeNodes.get(no2).add(no1);
				}								
			}
			s.close();
		} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
		}			
	}

	/**
	 * Performing search space truncation based on label profiles
	 * @param dir
	 */
	public void queryNeo4j(File dir) {
		db = new GraphDatabaseFactory().newEmbeddedDatabaseBuilder(dir).setConfig(GraphDatabaseSettings.pagecache_memory, "512M" ).setConfig(GraphDatabaseSettings.string_block_size, "60" ).setConfig(GraphDatabaseSettings.array_block_size, "300" ).newGraphDatabase();
		tx = db.beginTx();
		System.out.println("S:"+queryNodes.keySet().size());
		for(Long id: queryNodes.keySet()){
			ArrayList<String>  set = queryEdges.get(id);	
			ResourceIterator<Node> nodes = db.findNodes(Label.label(queryNodes.get(id)));
			while(nodes.hasNext()){
				Node node = nodes.next();				
				String[] profileNodes = (String[]) node.getProperty("profile");
				if(Arrays.asList(profileNodes).containsAll(set)){
					searchSpace.get(id).add(node.getId());
				}
			}
			System.out.print(id+" ,"+searchSpace.get(id).size()+";");			
		}
		System.out.println();
		newSearchSpace = new HashMap();
		Long before = System.currentTimeMillis();
		search(0);
		Long after = System.currentTimeMillis();
		System.out.println("Time Taken: "+(after - before));
		tx.close();
		db.shutdown();
	}
	
	public void testPrintSet(List<String> x){
		for(String temp: x){
			System.out.print(temp+" ");
		}
		System.out.println();
	}
	
	public void search(int i){
		Long nodeLabel = queryNodeList.get(i);
		HashSet<Long> nodes = searchSpace.get(nodeLabel);
		for(Long node: nodes){
			if(!newSearchSpace.values().contains(node) && check(node, i)){
				newSearchSpace.put(queryNodeList.get(i), node);
				if(i < queryNodeList.size()-1){					
					search(i+1);	
					if(resultSet.size() < 1000)
						break;
				}else{
					resultSet.add(newSearchSpace);
					printSearchSpace();
					if(resultSet.size() < 1000)
						break;
				}
				newSearchSpace.remove(queryNodeList.get(i));
			}
		}
	}	
	
	public void printSearchSpace(){
		for(Long key: newSearchSpace.keySet()){
			System.out.print(key+":"+newSearchSpace.get(key)+" ");
		}
		System.out.println();
	}
	
	public boolean check(long node, int index){
		for(int i = 0; i < index; i++){
			if(!edgeExists(i, index))
				continue;
			if(!nodeExists(node, index, i, RelationshipType.withName("connected"))){
				return false;
			}
		}
		return true;
	}
	
	//Check if edge exists in query graph
	public boolean edgeExists(int from, int to){
		if(queryEdgeNodes.get(queryNodeList.get(from)).contains(Long.valueOf(to+"")))
			return true;
		else
			return false;
	}		
	
	
	private boolean nodeExists(Long nodeID, int index, int i, RelationshipType relationship) {
		Node node = db.getNodeById(nodeID);
		if(!node.hasRelationship(relationship)){
			return false;
		}
		Long otherNode = newSearchSpace.get(queryNodeList.get(i));		
		Iterator<Relationship> relationships = node.getRelationships(relationship).iterator();		
		while(relationships.hasNext()){
			if(relationships.next().getOtherNode(node).getId() == otherNode)
				return true;
		}
		return false;
	}	

}
