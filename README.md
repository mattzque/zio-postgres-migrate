# PostgreSQL Migration Utility

Work in progress! My first ZIO & Scala 3 program!

Inspired by: [https://github.com/moneyhub/postgres-migrations](moneyhub/postgres-migrations)




```
% POSTGRES_DB=postgres \
  POSTGRES_USERNAME=postgres \
  POSTGRES_PASSWORD=postgres \
  POSTGRES_HOST=localhost \
  POSTGRES_PORT=5432 \
  POSTGRES_SSL=false \
    java -jar migrate.jar migrations/
```
