import java.sql.*;  


class Transactions{  
	public static void main(String args[]){
		
	try {
		Class.forName("com.mysql.jdbc.Driver");
	} catch (ClassNotFoundException e1) {
		e1.printStackTrace();
	} 
	try(Connection con=DriverManager.getConnection(  
			"jdbc:mysql://localhost:3306/imdb","root","root");){		
		Statement stmt=con.createStatement();  
		ResultSet rs=stmt.executeQuery("select * from movies");
		rs.last();
		System.out.println("Number of rows before: "+rs.getRow());
		
		PreparedStatement ps = con.prepareStatement("insert into movies values(?,?,?,?)");
		//id, name, year,rank
		
		con.setAutoCommit(false);
		
		ps.setLong(0, 412321);
		ps.setString(1, "Manasi");
		ps.setInt(2, 1990);
		ps.setDouble(2, 5.0);
		ps.addBatch();
		ps.setLong(0, 412322);
		ps.setString(1, "Madhumita");
		ps.setInt(2, 1986);
		ps.setDouble(2, 5.0);
		ps.addBatch();
		
		//Wrong data
		ps.setLong(0, 412321);
		ps.setString(1, "Manasi");
		ps.setInt(2, 1990);
		ps.setDouble(2, 5.0);
		ps.addBatch();
		
		ps.executeBatch();
		con.commit();	
		
		con.close();  
	} catch (SQLException e1) {
		// TODO Auto-generated catch block
		e1.printStackTrace();
	}
	
	//Checking that database remains same after insert update failure
	try(Connection con=DriverManager.getConnection(  
			"jdbc:mysql://localhost:3306/imdb","root","root");){
		Statement stmt=con.createStatement();  
		ResultSet rs=stmt.executeQuery("select * from movies");
		rs.last();
		System.out.println("Number of rows after: "+rs.getRow());
	} catch (SQLException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
	
	}  
} 