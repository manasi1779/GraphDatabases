package TDM.Neo4j;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Scanner;
import java.util.Set;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.ResourceIterator;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.graphdb.factory.GraphDatabaseSettings;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Label;


public class SubgraphMatching {
	
	HashMap<String, LabelPattern> labels = new HashMap();
	ArrayList<WhereClause> wheres = new ArrayList();
	ArrayList<Edge> edges = new ArrayList();
	ArrayList<String> queryNodes = new ArrayList();
	GraphDatabaseService db;
	HashMap<String, Set<Node>> entities;
	HashSet<Node> used = new HashSet();
	HashMap<String, Node> searchSpace = new HashMap();
	Set<HashMap> resultSet = new HashSet();
	
	public static void main(String[] args) {
		SubgraphMatching cypher = new SubgraphMatching();
		cypher.parseEdges();		
		cypher.initNeo4j();
		System.out.println(cypher.createCypher());
		/*Result result = cypher.db.execute(cypher.createCypher());
		//result.hasNext()
		System.out.println(result.resultAsString());
		System.out.println(cypher.createCypher());*/
		cypher.queryNeo4j();
	}	
	
	public void initSearchSpace(){
		for(String node: queryNodes){
			searchSpace.put(node, null);
		}
	}
	
	/**
	 * Parses subgraphs having nodes defined with single property followed by edges
	 */
	public void parseEdges(){
		Scanner s = new Scanner(System.in);
		while(s.hasNextLine()){
			String token = s.nextLine();
			String tokens[] = token.split(" ");
			
			//If this is a new alias add it to labels
			if(token.length()>0 && !labels.keySet().contains(tokens[0].trim())){
				LabelPattern label = new LabelPattern(tokens[0].trim(), tokens[1].trim());
				labels.put(tokens[0].trim(), label);		
				queryNodes.add(tokens[0].trim());
				int length = tokens.length;
				String[] newArr = new String[length - 2];
				System.arraycopy(tokens, 2, newArr, 0, length-2);
				if(tokens.length == 2){
					WhereClause where = new WhereClause(label, null, null);
					wheres.add(where);
				}
				for(String property: newArr){
					if(property.contains("<>")){
						String[] clauseValues = property.split("<>");
						WhereClause where = new WhereClause(label, clauseValues[1].trim());
						wheres.add(where);	
					}
					else if(property.contains("=")){
						String[] clauseValues = property.split("=");
						WhereClause where = new WhereClause(label, clauseValues[0].trim(), clauseValues[1].trim());
						wheres.add(where);	
					}						
				}				
			}
			else if(tokens.length == 2){
				edges.add(new Edge(labels.get(tokens[0].trim()), labels.get(tokens[1].trim())));
			}
			else{
				break;
			}
		}
		s.close();
	}
	
	/**
	 * create cypher query using subgraph
	 * @return
	 */
	public String createCypher(){
		String cypherText = "MATCH\t";
		for(Edge edge: edges){
			cypherText+= edge+",";
		}
		cypherText = cypherText.substring(0, cypherText.length()-1)+"\nWHERE ";
		for(WhereClause where: wheres){
			if(where.property == null && where.unequal == null)
				continue;
			cypherText+=where+" AND ";
		}
		cypherText = cypherText.substring(0, cypherText.length()-4)+"\nRETURN ";
		for(String token: labels.keySet()){
			cypherText+=token+",";
		}
		cypherText = cypherText.substring(0, cypherText.length()-1);
		return cypherText;
	}
	
	public void initNeo4j(){
		File dbpath = new File("D:/neo4j-enterprise-3.1.1-windows/neo4j-enterprise-3.1.1/data/databases/graph.db");
		GraphDatabaseSettings.BoltConnector bolt = GraphDatabaseSettings.boltConnector("0");
		GraphDatabaseFactory dbFactory = new GraphDatabaseFactory();
		db = dbFactory.newEmbeddedDatabaseBuilder(dbpath).setConfig(bolt.enabled, "true")
			    .setConfig(bolt.address, "localhost:7687").newGraphDatabase();
	}
	
	
	
	public void queryNeo4j(){				    
		Transaction t = db.beginTx();		
		entities = new HashMap();
		for(WhereClause where: wheres){
			ResourceIterator<Node> nodes;
			if(where.property == null){
				nodes = db.findNodes(getEntity(where.label.name));
			}
			else{
				nodes = db.findNodes(getEntity(where.label.name), where.property, where.value);
			}
			Set<Node> nodesSet = new HashSet();
			while(nodes.hasNext()){
				nodesSet.add(nodes.next());
			}
			if(entities.containsKey(where.label.token)){
				Set<Node> oldSet = entities.get(where.label.token);
				nodesSet.retainAll(oldSet);
			}
			entities.put(where.label.token, nodesSet);
			System.out.println(where.label.token+":"+entities.get(where.label.token).size());
		}		
		initSearchSpace();
		search(0);
		t.close();
		System.out.println("After filtering");
		for(HashMap result: resultSet){
			for(String key: queryNodes){
				System.out.print(key+":"+result.get(key)+" ");
			}
			System.out.println();
		}
		
	}
	
	public void search(int i){
		String nodeLabel = queryNodes.get(i);
		Set<Node> nodes = entities.get(nodeLabel);
		for(Node node: nodes){
			if(!searchSpace.values().contains(node) && check(node, i)){
				searchSpace.put(queryNodes.get(i), node);
				if(i < queryNodes.size()-1){					
					search(i+1);					
				}else{
					resultSet.add(searchSpace);
					printSearchSpace();
				}
				searchSpace.put(queryNodes.get(i), null);
			}
		}
	}	
	
	public void printSearchSpace(){
		for(String key: searchSpace.keySet()){
			System.out.print(key+":"+searchSpace.get(key)+" ");
		}
		System.out.println();
	}
	
	public boolean check(Node node, int index){
		for(int i = 0; i < index; i++){
			if(!isRelationship(labels.get(queryNodes.get(index)).token, labels.get(queryNodes.get(i)).token))
				continue;
			RelationshipIMDB relationship = getRelationship(labels.get(queryNodes.get(index)).name, labels.get(queryNodes.get(i)).name);
			if(!nodeExists(node, index, i, relationship)){
				return false;
			}
		}
		return true;
	}
	
	private boolean nodeExists(Node node, int index, int i, RelationshipIMDB relationship) {		
		if(!node.hasRelationship(relationship)){
			return false;
		}
		Node otherNode = searchSpace.get(queryNodes.get(i));		
		Iterator<Relationship> relationships = node.getRelationships(relationship).iterator();
		
		while(relationships.hasNext()){
			if(relationships.next().getOtherNode(node).getId() == otherNode.getId())
				return true;
		}
		return false;
	}

	
	private Entities getEntity(String tableName){
		switch(tableName.trim()){
		case "actor":
			return Entities.actor;
		case "director":
			return Entities.director;
		case "movie":
			return Entities.movie;
		case "genre":
			return Entities.genre;
		default:
			return null;
		}
	}	
	
	
	public RelationshipIMDB getRelationship(String label1, String label2){
		switch(label1){
			case "movie":{
				if(label2.equals("director"))
					return RelationshipIMDB.directed_by;
				else if(label2.equals("actor"))
					return RelationshipIMDB.acted_in;
				else if(label2.equals("genre"))
					return RelationshipIMDB.movie_genre;
				break;
			}
			case "actor":{
				return RelationshipIMDB.acted_in;
			}
			case "director":{
				if(label2.equals("movie"))
					return RelationshipIMDB.directed_by;
				else if(label2.equals("director"))
					return RelationshipIMDB.director_genre;		
				break;
			}
			case "genre":{
				if(label2.equals("movie"))
					return RelationshipIMDB.movie_genre;
				else if(label2.equals("director"))
					return RelationshipIMDB.director_genre;
				break;
			}			
		}
		return null;
	}
	
	public boolean isRelationship(String label1, String label2){
		for(Edge edge: edges){
			if(edge.from.token.equals(label1)&&edge.to.token.equals(label2))
				return true;
			else if(edge.from.token.equals(label2)&&edge.to.token.equals(label1))
				return true;
		}
		return false;
	}
	
	public void scrap(){
		
		for(Edge edge: edges){
			Iterator<Node> nodesFrom = entities.get(edge.from.token).iterator();
			Iterator<Node> nodesTo = entities.get(edge.to.token).iterator();
			Label label1 = nodesFrom.next().getLabels().iterator().next(),
					label2 = nodesTo.next().getLabels().iterator().next();
			RelationshipIMDB relationship = getRelationship(label1.toString(), label2.toString());
			System.out.println(edge.from.token+" "+edge.to.token);
			System.out.println(relationship);
			while(nodesFrom.hasNext()){
				Node from = nodesFrom.next();
				if(!from.hasRelationship(relationship)){
					
					nodesFrom.remove();
				}				
			}
		
			while(nodesTo.hasNext()){
				Node to = nodesTo.next();
				if(!to.hasRelationship(relationship)){
					nodesTo.remove();
				}				
			}				
		}
		
		System.out.println("After filtering");
		for(String key: entities.keySet()){
			System.out.println(key+":"+entities.get(key).size());
		}

	}
}

class Edge{
	LabelPattern from, to;
	
	public Edge(LabelPattern from, LabelPattern to){
		this.from = from;
		this.to = to;
	}
	
	public String toString(){
		return "("+from.token+":"+from.name+")"+"--"+"("+to.token+":"+to.name+")";
	}
}

class LabelPattern{
	String token, name;
	
	public LabelPattern(String token, String name){
		this.token = token;
		this.name = name;
	}
	
	public String toString(){
		return token+":"+name;
	}
}


class WhereClause{
	LabelPattern label;
	String property;
	String value;
	String unequal;
	
	public WhereClause(LabelPattern label,	String property, String value){
		this.label = label;
		this.property = property;
		this.value = value;
	}
	
	public WhereClause(LabelPattern label,	String unequal){
		this.label = label;
		this.unequal = unequal;
	}
	
	public String toString(){
		if(property == null && unequal == null)
			return label.token;
		else if(unequal != null)
			return label.token+"<>"+unequal;
		else
			return label.token+"."+property+"='"+value+"'"; 
	}
}