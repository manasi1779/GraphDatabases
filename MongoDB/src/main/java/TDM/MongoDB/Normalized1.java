package TDM.MongoDB;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.Mongo;
import com.mongodb.MongoClient;

public class Normalized1 {

	public static void main( String[] args )
    {
        System.out.println( "Hello World!" );
        Normalized1 normalizedDB = new Normalized1();
        normalizedDB.getData();
    }
    
    public void getData(){
    	try {
    		Class.forName("com.mysql.jdbc.Driver");
    	} catch (ClassNotFoundException e1) {
    		e1.printStackTrace();
    	} 
    	@SuppressWarnings("resource")
		MongoClient mongoClient = new MongoClient( "localhost" , 27017 );
    	@SuppressWarnings("deprecation")
		DB db = mongoClient.getDB("imdb");
    	try(Connection con=DriverManager.getConnection(  
    			"jdbc:mysql://localhost:3306/imdb","root","root");){		
    		DatabaseMetaData md = con.getMetaData();
    		ResultSet rs  = md.getTables(null, null, "%", null);
    		ArrayList<String> tables = new ArrayList<String>();
    		DBCollection dbCollection =  db.getCollection("imdbData");
    		while(rs.next()){
    			tables.add(rs.getString(3));
    			ArrayList<String> columns = getTableDefinition(rs.getString(3), md);
    			loadData(rs.getString(3), con.createStatement(), dbCollection, columns);
    		}
    	}catch(SQLException e1){
    		e1.printStackTrace();
    	}
    }
    
    public ArrayList<String> getTableDefinition(String tableName, DatabaseMetaData md){
    	ArrayList<String> tableDefinition = new ArrayList<String>();
    	try {
			ResultSet rs = md.getColumns(null, null, tableName, "%");
			while(rs.next()){
				tableDefinition.add(rs.getString(4));
			}			
		} catch (SQLException e) {
			e.printStackTrace();
		}
    	return tableDefinition;    	
    }

    public void loadData(String tableName, Statement stmt, DBCollection dbCollection, ArrayList<String> tableDefinition){
    	try {
			ResultSet rs = stmt.executeQuery("Select * from "+tableName+";");
			while(rs.next()){
				int j = 1;				
				BasicDBObject documentDetail = new BasicDBObject();
				documentDetail.put("type", tableName);
				for(String column: tableDefinition){	
					if(column.equals("id")){
						column = tableName.substring(0, tableName.length()-1) +"_"+ column;
					}
					String datatype = rs.getMetaData().getColumnTypeName(j);
					try{
					switch(datatype){
						case "INT":{
							documentDetail.put(column, rs.getInt(j));
							break;
						}
						case "CHAR":{
							documentDetail.put(column, rs.getString(j));
							break;
						}
						case "VARCHAR":{
							documentDetail.put(column, rs.getString(j));
							break;
						}
						case "FLOAT":{
							documentDetail.put(column, rs.getFloat(j));
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
				dbCollection.insert(documentDetail);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
    }
	public void loadDataDenorm(Statement stmt, DBCollection dbCollection, int offset){
		String sqlQuery = "select actors.id, actors.first_name, actors.last_name, actors.gender, roles.role, roles.movie_id, movies_genres.genre, movies_directors.director_id, movies.name, movies.year, movies.rank from actors, roles, movies_genres, movies_directors, movies where actors.id = roles.actor_id and roles.movie_id = movies_genres.movie_id and movies_directors.movie_id = movies.id limit 1000 offset "+ offset + ";";
		try {
			ResultSet rs = stmt.executeQuery(sqlQuery);
			int totalColumns = rs.getMetaData().getColumnCount();
			 ArrayList<String> tableDefinition = new ArrayList<String>();
			 for(int i = 1; i <= totalColumns; i++){
				 tableDefinition.add(rs.getMetaData().getColumnName(i));
			 }
			 int i = 0;
			 while(rs.next()){
				int j = 1;				
				BasicDBObject documentDetail = new BasicDBObject();
				for(String column: tableDefinition){				
					String datatype = rs.getMetaData().getColumnTypeName(j);
					try{
					switch(datatype){
						case "INT":{
							documentDetail.put(column, rs.getInt(j));
							break;
						}
						case "CHAR":{
							documentDetail.put(column, rs.getString(j));
							break;
						}
						case "VARCHAR":{
							documentDetail.put(column, rs.getString(j));
							break;
						}
						case "FLOAT":{
							documentDetail.put(column, rs.getFloat(j));
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
				dbCollection.insert(documentDetail);
			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}		
	}

}
