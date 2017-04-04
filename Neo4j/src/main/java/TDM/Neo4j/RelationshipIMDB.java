package TDM.Neo4j;

import org.neo4j.graphdb.RelationshipType;

public enum RelationshipIMDB implements RelationshipType {
	movie_genre, director_genre, directed_by, acted_in
}
