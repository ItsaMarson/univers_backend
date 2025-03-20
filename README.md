# UniVERS - BackEnd


# UniVERS 🎓📅

**UniVERS** is a centralized, automated reservation system with an integrated approval workflow. It enables seamless venue and equipment reservations with real-time availability tracking, approval hierarchies, and automated notifications.


📌 Prerequisites
Java 17
Spring Boot
MySQL
Maven
# Steps

Clone Repository
```bash
git clone https://github.com/ItsaMarson/univers_backend
```
MySQL Schema 
```bash
CREATE DATABASE universe CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

Navigate to the backend directory: 

```bash
cd universe_backend
```

Copy Paste to the application.properties
```bash
spring.application.name=univers_backend
server.port=8080
spring.datasource.url = jdbc:mysql://localhost:3306/univers
spring.datasource.username = root
spring.datasource.password = (your MySQL root password)
spring.jpa.database-platform=org.hibernate.dialect.MySQLDialect
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MySQLDialect
server.error.include-stacktrace=never
```

#🚀 Happy Coding! 🎉





