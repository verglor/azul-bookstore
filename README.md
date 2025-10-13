# Azul Bookstore Inventory Management System 

The Bookstore Inventory Management System is a web application (REST API only) that allows bookstore owners to manage their inventory efficiently. It provides features for adding, updating, and searching for books.

## Key Features:

### Book CRUD Operations [REST API]:
- Add new books with details like title, author, genre, and price.
- Update existing book information.
- Delete books from the inventory.

### Search Functionality [REST API]:
- Search for books by title, author, or genre.
- Display search results in a paginated format.

### Authentication and Authorization:
- Implement basic authentication for bookstore staff.
- Differentiate between admin and regular users.
- Admins can perform all CRUD operations, while regular users can only view books.

### Database:
- Set up a database to store book information.
- Define appropriate tables and relationships (e.g., Book, Author, Genre).


## Architecture

### REST application
Main component is Java/Spring Boot app providing CRUD access to basic bookstore model packaged as docker image
It will be deployed to load balanced ECS cluster with seamless autoscaling on Fargate. 

### Data Storage
PostgreSQL RDBMS was choosen for database since model is relational and eventually will be extended to support order transactions requiring strong consistency.
High availability and scalability will be provided by RDS Aurora (once regular RDS will not suffice).

### Authentication
Authentication is delegated to AWS Cognito which simplifies architecture and offloads user management.
Since we only distinguish between authenticated (full access) and anonymous (read-only) users we can simply check credentials at API Gateway level to protect write endpoints without touching the app itself.
Once more fine-grained access control is needed we can process JWT from Cognito with Spring Security.

### Data consistency
Even if project is planned as eshop with orders which will require strong consistency, for presentational data like information about books, authors and genres we can afford showing stale data for short period of time (minute or few) so we can potentialy benefit from introducing CDN (Cloudfront)


## Implementation details

### Layers of abstraction
Since the simple CRUD character of the app there is no actual business logic yet, hence controllers communicate directly with repositories.
Once it becomes more complex, service layer can be easily introduced.

### API first vs code first
Since the API will be consumed by a third party I prefer API-first approach so that it is more stable and development of
server and client can be parallelized once specification is agreed upon.

### Minimalistic domain model
Domain model intentionally contains only what was specified by client. This prevents putting effort into features that might be useless to the client.

### Infrastructure as Code (CDK)
With AWS as infrastructure provider I choose CDK to setup both infrastructure and CI/CD Pipeline consisting of:
- Load balanced ECS with Fargate
- Aurora Serverless with Postgres / RDS
- API Gateway with basic DoS protection
- Cognito + API Gateway integration
- CI/CD with CodePipeline


## Possible enhancements (out of current scope)
- Extend domain model according to client requirements
- Optimistic locking to detect conflicts and prevent unintentional overwriting
- Cursor based pagination for predictable results and better performance
- Fuzzy search (tolerating typos and misspelling) leveraging OpenSearch
- Use testcontainers for Postgres db in tests and dev mode
- Internalization
- Domain entities auditing (created at, updated at, etc.)
- HATEOAS links in API
- Use jlink / GraalVM native image to minimize docker image size/startup/memory usage
- Blue/Green deployment
- Load testing + autoscaling fine-tuning
- Preload big dataset
- Hikari CP fine-tuning
- Backup and disaster recovery (besides regular RDS backups)
- Automated tests of security (Cognito secured endpoints on API gateway)
- Basic client (fronted UI) to test it e2e (including authentication with Cognito)
- DoS protection (besides basic provided by API gateway)
- Cache presentational data in CDN with short TTL
- Checkstyle, Sonarqube
- Monitoring and alerts
- Different deploy environments
- Separate AWS account for production and other environments
- Versioning
- Architecture diagrams
