package TDM.MongoDB;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.Mongo;
import com.mongodb.MongoClient;

public class DeNormalized {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		DeNormalized denorm = new DeNormalized();
		denorm.getData();
	}
	
	public void getData(){
		try {
    		Class.forName("com.mysql.jdbc.Driver");
    	} catch (ClassNotFoundException e1) {
    		e1.printStackTrace();
    	} 
    	MongoClient mongoClient = new MongoClient( "localhost" , 27017 );
    	DB db = mongoClient.getDB("imdb");
    	try(Connection con=DriverManager.getConnection(  
    			"jdbc:mysql://localhost:3306/imdb","root","root");){		
    		for(int i = 0; i < 3700; i++){
    			loadDataDeNormalized(con, db.getCollection("imdbDeNorm"));
    		}
    	}catch(SQLException e1){
    		e1.printStackTrace();
    	}

	}
		 
	/**
	 * Method to load data to MongoDB in required embedded document format 
	 * @param conn
	 * @param dbCollection
	 */
	public void loadDataDeNormalized(Connection conn, DBCollection dbCollection){
		String actorQuery = "select id, first_name, last_name, gender from actors;";
		try {
			Statement stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery(actorQuery);
			int totalColumns = rs.getMetaData().getColumnCount();
			 ArrayList<String> tableDefinition = new ArrayList<String>();
			 for(int i = 1; i <= totalColumns; i++){
				 tableDefinition.add(rs.getMetaData().getColumnName(i));
			 }
			 while(rs.next()){
				 BasicDBObject documentDetail = createDocument(tableDefinition, rs);
				 String rolesQuery = "select movie_id, role from roles where actor_id ="+ rs.getString(1)+";";
				 Statement roleStmt = conn.createStatement();
				 ResultSet rolesrs = roleStmt.executeQuery(rolesQuery);
					int rolesColumns = rolesrs.getMetaData().getColumnCount();
					 ArrayList<String> rolesTableDefinition = new ArrayList<String>();
					 for(int i = 1; i <= rolesColumns; i++){
						 rolesTableDefinition.add(rolesrs.getMetaData().getColumnName(i));
					 }
					 List<BasicDBObject> movies = new ArrayList<>();
					 while(rolesrs.next()){
						 BasicDBObject movieDetail = createDocument(rolesTableDefinition, rolesrs);
						 String movieId = rolesrs.getString(1);
						 String movieQuery = "select name, year, rank, genre from movies join movies_genres on movies.id = movies_genres.movie_id where movie_id ="+ movieId +";";
						 Statement movieStmt = conn.createStatement();
						 ResultSet moviesrs = movieStmt.executeQuery(movieQuery);
						 int moviesColumns = moviesrs.getMetaData().getColumnCount();
						 ArrayList<String> moviesTableDefinition = new ArrayList<String>();
						 for(int i = 1; i <= moviesColumns; i++){
							 moviesTableDefinition.add(moviesrs.getMetaData().getColumnName(i));
						 }
						 
						 while(moviesrs.next()){
							 BasicDBObject movieData = createDocument(moviesTableDefinition, moviesrs);
							 movieDetail.put("MovieDetail", movieData);					 
						 }
						 String directorMovieQuery = "select director_id from movies_directors where movie_id = "+ movieId+";";
						 Statement dirmovieStmt = conn.createStatement();
						 ResultSet directorsmoviers = dirmovieStmt.executeQuery(directorMovieQuery);
						 List<BasicDBObject> directors = new ArrayList<>();
						 while(directorsmoviers.next()){
							 String directorsQuery = "select id, first_name, last_name from directors where id = "+ directorsmoviers.getString(1)+";";
							 Statement directorStmt = conn.createStatement();
							 ResultSet directorsrs = directorStmt.executeQuery(directorsQuery);
							 int directorColumns = directorsrs.getMetaData().getColumnCount();
							 ArrayList<String> directorsTableDefinition = new ArrayList<String>();
							 for(int i = 1; i <= directorColumns; i++){
								 directorsTableDefinition.add(directorsrs.getMetaData().getColumnName(i));
							 }
							 while(directorsrs.next()){
								 BasicDBObject directorDetail = createDocument(directorsTableDefinition, directorsrs);
								 directors.add(directorDetail);
							 }
						 }
						 movieDetail.put("directors", directors);
						 movies.add(movieDetail);
					 }					 
					 documentDetail.put("movies", movies);
					 dbCollection.insert(documentDetail);
			 }
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}		
	}
	
	/***
	 * Method to create document with single row of result set
	 * @param tableDefinition
	 * @param rs
	 * @return
	 */
	public BasicDBObject createDocument(ArrayList<String> tableDefinition, ResultSet rs){
		int j = 1;				
		BasicDBObject documentDetail = new BasicDBObject();
		try{
			for(String column: tableDefinition){				
			String datatype = rs.getMetaData().getColumnTypeName(j);
			
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
			}}
			catch(SQLException e){
				e.printStackTrace();						
			}
			return documentDetail;
		}
}
