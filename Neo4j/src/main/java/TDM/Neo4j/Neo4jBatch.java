package TDM.Neo4j;

import java.io.File;
import java.io.IOException;
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
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.schema.IndexCreator;
import org.neo4j.unsafe.batchinsert.BatchInserter;
import org.neo4j.unsafe.batchinsert.BatchInserters;



public class Neo4jBatch {

	GraphDatabaseService db;
	Transaction tx;
	BatchInserter insert;
	File dbpath;
	static HashMap<String, Integer> offsetID = new HashMap();
	HashMap<String, Long> genreTable = new HashMap();
	
	public static void main(String[] args) {
		Neo4jBatch dbInstance = new Neo4jBatch();
		dbInstance.getData();				
	}
	
	static {
		offsetID.put("movie", 1000000);
		offsetID.put("actor", 2000000);
		offsetID.put("director", 3000000);
	}
	
	public Neo4jBatch(){
		dbpath = new File("D:/neo4j-enterprise-3.1.1-windows/neo4j-enterprise-3.1.1/data/databases/graph.db");
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
	
	public void loadData(Connection con){
		try {
			DatabaseMetaData md = con.getMetaData();
    		ResultSet rs  = md.getTables(null, null, "%", null);
    		ArrayList<String> relationTables = new ArrayList<String>();
    		insert = BatchInserters.inserter(dbpath);
    		while(rs.next()){
    			if(rs.getString(3).contains("_") || rs.getString(3).equals("roles"))
    				relationTables.add(rs.getString(3));
    			else{
	    			ArrayList<String> columns = getTableDefinition(rs.getString(3), md);
	    			loadTableData(rs.getString(3), con.createStatement(), columns);
    			}
    		}
    		addGenreNodes(con.createStatement());
    		createRoleRelationships(con.createStatement());
    		createDirectorRelationships(con.createStatement());    		
    		createMovieGenreRelationship(con.createStatement());
    		createDirectorGenreRelationship(con.createStatement());
    		IndexCreator indexCreator = insert.createDeferredSchemaIndex(getEntity("movies"));
			indexCreator.on("id").create();
			indexCreator = insert.createDeferredSchemaIndex(getEntity("directors"));
			indexCreator.on("id").create();
			indexCreator = insert.createDeferredSchemaIndex(getEntity("actors"));
			indexCreator.on("id").create();
			insert.shutdown();
    		con.close();
		} catch (SQLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} 
	}
	
	private Entities getEntity(String tableName){
		switch(tableName){
		case "actors":
			return Entities.actor;
		case "directors":
			return Entities.director;
		case "movies":
			return Entities.movie;
		default:
			return Entities.genre;
		}
	}	
	
	private void loadTableData(String tableName, Statement createStatement, ArrayList<String> tableDefinition) {
		try (ResultSet rs = createStatement.executeQuery("select * from "+tableName+";");){			
			System.out.println("Adding data to "+tableName);			
			while(rs.next()){				
				HashMap<String, Object> nodeMap = new HashMap();
				long id = 0;
				for(String column: tableDefinition){
					Object value = rs.getObject(column);
					if(column.equals("id")){
						id = convertID((int)value, tableName.substring(0, tableName.length()-1));
					}
					else if(!column.equals("rank")){
						nodeMap.put(column, value);
					}					
				}
				insert.createNode(id, nodeMap, getEntity(tableName));
			}					
			createStatement.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}		
	}
	
	public void addGenreNodes(Statement createStatement){
		try(ResultSet rs = createStatement.executeQuery("select distinct(genre) from movies_genres;");){
			while(rs.next()){
				Map<String, Object> params = new HashMap<>();
				params.put("genre", rs.getString("genre"));
				Long value = insert.createNode(params, getEntity("genre"));
				genreTable.put(rs.getString("genre"), value);
			}
			createStatement.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	public void createRoleRelationships(Statement createStatement){
		try(ResultSet rs = createStatement.executeQuery("select * from roles;");) {
			while(rs.next()){
				Map<String, Object> params = new HashMap<>();
				params.put("role", rs.getString("role"));
				insert.createRelationship(offsetID.get("actor")+rs.getInt("actor_id"), offsetID.get("movie")+rs.getInt("movie_id"), RelationshipIMDB.acted_in, params);
			}
			createStatement.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	public void createDirectorRelationships(Statement createStatement){
		try(ResultSet rs = createStatement.executeQuery("select * from movies_directors;");){
			while(rs.next()){
				long director_id =offsetID.get("director")+rs.getInt("director_id");
				long movie_id = offsetID.get("movie")+rs.getInt("movie_id");
				if(insert.nodeExists(director_id)&&insert.nodeExists(movie_id))
					insert.createRelationship(director_id,movie_id, RelationshipIMDB.directed_by, null);
			}
			createStatement.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	public void createMovieGenreRelationship(Statement createStatement){
		try(ResultSet rs = createStatement.executeQuery("select * from movies_genres;");){
			while(rs.next()){
				long movie_id = offsetID.get("movie")+rs.getInt("movie_id");
				if(insert.nodeExists(movie_id)&& genreTable.containsKey(rs.getString("genre")))
					insert.createRelationship(movie_id, genreTable.get(rs.getString("genre")), RelationshipIMDB.movie_genre, null);
			}
			createStatement.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	public void createDirectorGenreRelationship(Statement createStatement){
		try(ResultSet rs = createStatement.executeQuery("select * from directors_genres;");){
			while(rs.next()){
				long director_id =offsetID.get("director")+rs.getInt("director_id");
				if(insert.nodeExists(director_id) && genreTable.containsKey(rs.getString("genre")))
					insert.createRelationship(director_id, genreTable.get(rs.getString("genre")), RelationshipIMDB.director_genre, null);
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
    
    public long convertID(int id, String type){
    	long offset = offsetID.get(type);
    	return offset+id;
    }
}
