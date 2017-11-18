import java.io.File;
import java.io.IOException;
import java.util.HashMap;

import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.unsafe.batchinsert.BatchInserter;
import org.neo4j.unsafe.batchinsert.BatchInserters;

public class LoadSampleData {

	File dbpath;
	BatchInserter insert;
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		LoadSampleData load = new LoadSampleData();
		load.loadData();
	}

	public LoadSampleData(){
		dbpath = new File("D:/neo4j-enterprise-3.1.1-windows/neo4j-enterprise-3.1.1/data/databases/graph.db");
		
	}
	
	public void loadData(){
		try {
			insert = BatchInserters.inserter(dbpath);
			insert.createNode(0, null, Label.label("A"));
			insert.createNode(1, null, Label.label("B"));
			insert.createNode(2, null, Label.label("C"));
			insert.createNode(3, null, Label.label("D"));
			insert.createNode(4, null, Label.label("E"));
			insert.createNode(5, null, Label.label("F"));
			insert.createRelationship(0, 1, RelationshipType.withName("connected"), null);
			insert.createRelationship(0, 2, RelationshipType.withName("connected"), null);
			insert.createRelationship(1, 3, RelationshipType.withName("connected"), null);
			insert.createRelationship(1, 4, RelationshipType.withName("connected"), null);
			insert.shutdown();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
	}
	
}
