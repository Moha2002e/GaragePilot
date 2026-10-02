# GaragePilot

Monolithe SaaS multi-garages pour ateliers automobiles indépendants belges.

## Démarrage local

1. Copier `.env.example` vers `.env` et remplacer les secrets.
2. Pour PostgreSQL local : `docker compose up postgres`.
3. Backend : installer un JDK 17 puis lancer `backend\\mvnw.cmd spring-boot:run`.
4. Frontend : `cd GaregePilotFront && npm start`.

## Connexion Supabase

GaragePilot utilise le pooler Supabase en mode session (port 5432) avec SSL obligatoire. Dans `.env`, renseigner :

```text
SPRING_PROFILES_ACTIVE=supabase
SUPABASE_DB_HOST=<host du Session pooler>
SUPABASE_DB_PORT=5432
SUPABASE_DB_NAME=postgres
SUPABASE_DB_USER=postgres.<project-ref>
SUPABASE_DB_PASSWORD=<mot de passe de la base>
JWT_SECRET=<secret aléatoire d'au moins 32 octets>
GARAGEPILOT_PLATFORM_ADMIN_EMAIL=<email exact du compte administrateur global>
```

Puis démarrer avec `docker compose up --build backend`. Liquibase crée et met à jour le schéma. Le fichier `.env` est ignoré par Git.

## Formatage

- Backend : `backend\\mvnw.cmd spotless:apply`
- Frontend : `npm run format` depuis `GaregePilotFront`

L'API écoute sur `http://localhost:8080`, Swagger sur `/swagger-ui/index.html`, et Angular sur `http://localhost:4200`.

## État actuel

Les fondations, l'authentification JWT, les garages, clients, véhicules,
factures/PDF, dashboard, planning, prestations et l'administration globale des
sociétés sont opérationnels. Les accès métier sont contraints au garage présent
dans le JWT. L'administration globale exige le rôle `SUPER_ADMIN`, attribué au
démarrage uniquement à l'adresse explicitement configurée par
`GARAGEPILOT_PLATFORM_ADMIN_EMAIL`.
