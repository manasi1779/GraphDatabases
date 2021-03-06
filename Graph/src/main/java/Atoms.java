import org.neo4j.graphdb.Label;

public enum Atoms implements Label{
	C, H, O, N, S, B;
	
	public static Atoms getAtomLabel(String name){
		switch(name){
			case "C":{
				return C;
			}
			case "H":{
				return H;
			}
			case "O":{
				return O;
			}
			case "N":{
				return N;
			}
			case "S":{
				return S;
			}		
			case "B":{
				return B;
			}			
		}
		return null;
	}
}
