
/**
 * Exception for Invalid Canonical Code
 * @author Manasi Bharde & Lakshmi Ravi
 *
 */
public class InvalidCanonicalCode extends Exception{
	
	String message;
	
	public InvalidCanonicalCode(int canonicalCodeSize, int edgeOrderSize){
		message = "\nSize mismatch. \n Size of canonical code is "+canonicalCodeSize+" and size of edge order is"+ edgeOrderSize;
	}
	
	public String toString(){
		return message;
	}

}