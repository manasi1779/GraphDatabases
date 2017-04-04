import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class SimpleSchema {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
	try {
		Class.forName("com.mysql.jdbc.Driver");
	} catch (ClassNotFoundException e1) {
		// TODO Auto-generated catch block
		e1.printStackTrace();
	} 
	
	try(Connection con=DriverManager.getConnection(  
			"jdbc:mysql://localhost:3306/imdb1","root","root");){		
		Statement stmt=con.createStatement();
		String personTable = "create table Person (person_id int primary key,"
				+ "name varchar(10),"
				+ "gender char(1));";
		String directorTable = "create table Director (person_id int primary key,"
				+ "movie_id int,"
				+ "FOREIGN KEY (person_id) REFERENCES Person(person_id),"
				+ "FOREIGN KEY (movie_id) REFERENCES Movie(movie_id));";
		String movieTable = "create table Movie (movie_id int primary key,"
				+ "title varchar(30),"
				+ "release_year int);";
		String actorTable = "create table Actor (person_id int primary key,"
				+ "movie_id int,"
				+ "FOREIGN KEY (person_id) REFERENCES Person(person_id),"
				+ "FOREIGN KEY (movie_id) REFERENCES Movie(movie_id));";
		
		stmt.executeUpdate(personTable);
		stmt.executeUpdate(movieTable);
		stmt.executeUpdate(directorTable);
		stmt.executeUpdate(actorTable);		
		
		String insertPerson = "insert into Person values (";
		String insertMovie = "insert into Movie values (";
		String insertDirector = "insert into Director values (";
		String insertActor = "insert into Actor values (";
		
		for(int i =0; i< 3000; i++){
			stmt.execute(insertPerson+i+",'Test','F');");
		}
		
		for(int i =0; i< 3000; i++){
			stmt.execute(insertMovie+i+",'Title',1990);");
		}
		
		for(int i =0; i< 3000; i++){
			stmt.execute(insertDirector+i+","+i+");");
		}
		for(int i =0; i< 3000; i++){
			stmt.execute(insertActor+i+","+i+");");
		}
		
		//Query to get records where director is actor 
		String getSchema = "select actormovies.movie_id, actormovies.title, actormovies.release_year, directorperson.person_id, directorperson.name, directorperson.gender from (select Actor.person_id, Actor.movie_id, Movie.title, Movie.release_year from Actor join Movie on Actor.movie_id = Movie.movie_id) as actormovies join (select Director.person_id, Director.movie_id, Person.name, Person.gender from Director join Person on Director.person_id = Person.person_id) as directorperson on actormovies.movie_id = directorperson.movie_id;";
		
		ResultSet rs = stmt.executeQuery(getSchema);

		//Populating the inserted data and printing it
		do{
			rs.next();
			System.out.println(rs.getInt(1)+","+rs.getString(2)+","+rs.getInt(3)+","+rs.getInt(4)+","+rs.getString(5)+","+ rs.getString(6));
		}while(!rs.isLast());
		
	}catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
