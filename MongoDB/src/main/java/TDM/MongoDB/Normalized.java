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

/**
 * Hello world!
 *
 */
public class Normalized 
{
    public static void main( String[] args )
    {
        System.out.println( "Hello World!" );
        Normalized app = new Normalized();
        app.getData();
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
    		DatabaseMetaData md = con.getMetaData();
    		ResultSet rs  = md.getTables(null, null, "%", null);
    		ArrayList<String> tables = new ArrayList<String>();
    		
    		while(rs.next()){
    			tables.add(rs.getString(3));
    			ArrayList<String> columns = getTableDefinition(rs.getString(3), md);
    			createCollection(rs.getString(3), db);
    			loadData(rs.getString(3), con.createStatement(), db, columns);
    		}
    	}catch(SQLException e1){
    		e1.printStackTrace();
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
    	try {
			ResultSet rs = md.getColumns(null, null, tableName, "%");
			while(rs.next()){
				tableDefinition.add(rs.getString(4));
			}			
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	return tableDefinition;    	
    }
    
    public void createCollection(String tableName, DB db){
    	BasicDBObject dbOptions = new BasicDBObject();
    	System.out.println(tableName);
    	db.createCollection(tableName, dbOptions);
    	System.out.println("Created collection "+tableName);
    }
    
    /**
     * Method to load data to MongoDB collection wise
     * @param tableName
     * @param stmt
     * @param db
     * @param tableDefinition
     */
    public void loadData(String tableName, Statement stmt, DB db, ArrayList<String> tableDefinition){
    	try {
			ResultSet rs = stmt.executeQuery("Select * from "+tableName+";");
			DBCollection dbCollection = db.getCollection(tableName);
			while(rs.next()){
				int j = 1;				
				BasicDBObject documentDetail = new BasicDBObject();
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
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }
    

}



