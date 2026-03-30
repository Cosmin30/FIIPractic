CREATE SCHEMA IF NOT EXISTS stocks;
CREATE USER stocksuser WITH PASSWORD 'stockspassword';
GRANT ALL PRIVILEGES ON SCHEMA stocks TO stocksuser;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA stocks TO stocksuser;
ALTER USER stocksuser SET search_path = stocks;
-- Keycloak schema
CREATE SCHEMA IF NOT EXISTS keycloak;
CREATE USER keycloakuser WITH PASSWORD 'keycloakpassword';
GRANT ALL PRIVILEGES ON SCHEMA keycloak TO keycloakuser;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA keycloak TO keycloakuser;
ALTER USER keycloakuser SET search_path = keycloak;
