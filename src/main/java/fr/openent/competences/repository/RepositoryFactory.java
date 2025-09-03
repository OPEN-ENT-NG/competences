package fr.openent.competences.repository;

import fr.openent.competences.repository.impl.DefaultUserRepository;
import org.entcore.common.neo4j.Neo4j;

public class RepositoryFactory {

    private final Neo4j neo4j;

    private final UserRepository userRepository;

    public RepositoryFactory(Neo4j neo4j) {
        this.neo4j = neo4j;
        this.userRepository = new DefaultUserRepository(this);
    }

    public Neo4j neo4j() {
        return this.neo4j;
    }

    public UserRepository userRepository() {
        return this.userRepository;
    }
}
