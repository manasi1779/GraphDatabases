package TDM.Neo4j;

import java.io.File;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.graphdb.factory.GraphDatabaseSettings;



public class Neo4j {

	GraphDatabaseService db;
	Transaction tx;
	
	public static void main(String[] args) {
		Neo4j dbInstance = new Neo4j();
		dbInstance.getData();		
	}
	
	public Neo4j(){
		File dbpath = new File("D:/neo4j-enterprise-3.1.1-windows/neo4j-enterprise-3.1.1/data/databases/graph.db");
		GraphDatabaseSettings.BoltConnector bolt = GraphDatabaseSettings.boltConnector("0");
		GraphDatabaseFactory dbFactory = new GraphDatabaseFactory();
		db = dbFactory.newEmbeddedDatabaseBuilder(dbpath).setConfig(bolt.enabled, "true")
			    .setConfig(bolt.address, "localhost:7687")
			    .newGraphDatabase();
	}
	

	public void getData(){
		try {
    		Class.forName("com.mysql.jdbc.Driver");
    	} catch (ClassNotFoundException e1) {
    		e1.printStackTrace();
    	}
		try(Connection con=DriverManager.getConnection(  
    			"jdbc:mysql://localhost:3306/imdb","root","root");){
			loadData(con);
    	}catch(SQLException e1){
    		e1.printStackTrace();
    	}		
	}
	
	public void connectNeo4j(){
		/*Driver driver = GraphDatabase.driver( "bolt://localhost:7474", AuthTokens.basic( "neo4j", "graph" ) );
		GraphDatabase
		tx = driver.session().beginTransaction();*/
	}

	public void loadData(Connection con){
		try {
			DatabaseMetaData md = con.getMetaData();
    		ResultSet rs  = md.getTables(null, null, "%", null);
    		ArrayList<String> relationTables = new ArrayList<String>();
    		
    		/*while(rs.next()){
    			if(rs.getString(3).contains("_") || rs.getString(3).equals("roles"))
    				relationTables.add(rs.getString(3));
    			else{
	    			ArrayList<String> columns = getTableDefinition(rs.getString(3), md);
	    			loadTableData(rs.getString(3), con.createStatement(), columns);
    			}
    		}*/
    	//	addIndexes();
    	//	createRoleRelationships(con.createStatement());
    	//	createDirectorRelationships(con.createStatement());
    		addGenreInformation(con.createStatement());
    		con.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	private Entities getEntity(String tableName){
		switch(tableName){
		case "actors":
			return Entities.actor;
		case "directors":
			return Entities.director;
		}
		return Entities.movie;
	}
	
	private void loadTableData(String tableName, Statement createStatement, ArrayList<String> tableDefinition) {
		try (ResultSet rs = createStatement.executeQuery("select * from "+tableName+";");){			
			System.out.println("Adding data to "+tableName);
			while(rs.next()){
				tx = db.beginTx();
				Node javaNode = db.createNode(getEntity(tableName));
				int j = 1;				
				for(String column: tableDefinition){		
					if(column.equals("id")){
						column = tableName.substring(0, tableName.length()-1) +"_"+ column;
					}
					String datatype = rs.getMetaData().getColumnTypeName(j);
					try{
					switch(datatype){
						case "INT":{
							javaNode.setProperty(column, rs.getInt(j));
							break;
						}
						case "CHAR":{
							javaNode.setProperty(column, rs.getString(j));
							break;
						}
						case "VARCHAR":{
							javaNode.setProperty(column, rs.getString(j));
							break;
						}
						case "FLOAT":{
							javaNode.setProperty(column, rs.getFloat(j));
							break;
						}	
						default:{
							System.out.println("Add this type: "+datatype);
						}
					}
					j++;
					}
					catch(SQLException e){
						System.out.println(datatype);						
					}
				}
				tx.success();
				tx.close();
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
	}
	
	public void addIndexes(){
		String movieIndexing = "create index on :movie(movie_id);", 
				actorIndexing = "create index on :actor(actor_id);", 
				directorIndexing = "create index on :director(director_id);";
		tx = db.beginTx();
		db.execute(actorIndexing);
		db.execute(movieIndexing);
		db.execute(directorIndexing);
		tx.success();
		tx.close();
	}	
	
	public void createRoleRelationships(Statement createStatement){
		try(ResultSet rs = createStatement.executeQuery("select * from roles;");) {
			while(rs.next()){
				String cypherText = "MATCH (a:actor {actor_id:$actor_id}), (m:movie {movie_id:$movie_id}) CREATE (a)-[:acted_in {role:$role}]->(m)";
				Map<String, Object> params = new HashMap<>();
				params.put("actor_id", rs.getInt("actor_id"));
				params.put("movie_id", rs.getInt("movie_id"));
				params.put("role", rs.getString("role"));
				tx = db.beginTx();
				db.execute( cypherText, params );
				tx.success();
				tx.close();
			}
			createStatement.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	public void createDirectorRelationships(Statement createStatement){
		try(ResultSet rs = createStatement.executeQuery("select * from movies_directors;");){
			while(rs.next()){
				String cypherText = "MATCH (d:director {director_id:$director_id}), (m:movie {movie_id:$movie_id}) CREATE (d)-[:directed]->(m)";
				Map<String, Object> params = new HashMap<>();
				params.put("director_id", rs.getInt("director_id"));
				params.put("movie_id", rs.getInt("movie_id"));
				tx = db.beginTx();
				db.execute( cypherText, params );
				tx.success();
				tx.close();
			}
			createStatement.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	public void addGenreInformation(Statement createStatement){
		try(ResultSet rs = createStatement.executeQuery("select * from movies_genres;");){
			while(rs.next()){
				String cypherText = "MATCH (movie { movie_id: $movie_id }) SET movie += { genre: $genre}";
				Map<String, Object> params = new HashMap<>();
				params.put("movie_id", rs.getInt("movie_id"));
				params.put("genre", rs.getString("genre"));
				tx = db.beginTx();
				db.execute( cypherText, params);
				tx.success();
				tx.close();
			}
			createStatement.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}


	/**
     * Method to get table definition
     * @param tableName
     * @param md
     * @return
     */
    public ArrayList<String> getTableDefinition(String tableName, DatabaseMetaData md){
    	ArrayList<String> tableDefinition = new ArrayList<String>();
    	try(ResultSet rs = md.getColumns(null, null, tableName, "%");){
			while(rs.next()){
				tableDefinition.add(rs.getString(4));
			}			
		} catch (SQLException e) {
			e.printStackTrace();
		}
    	return tableDefinition;    	
    }
}
