# Giai đoạn 1: Build ứng dụng bằng Maven và Java 21
FROM maven:3.9.9-eclipse-temurin-21 AS build

WORKDIR /app

# Copy cấu hình Maven trước để tận dụng cache
COPY pom.xml .
RUN mvn -B dependency:go-offline

# Copy mã nguồn và đóng gói thành JAR
COPY src ./src
RUN mvn -B clean package -DskipTests

# Giai đoạn 2: Chạy ứng dụng bằng Java 21
FROM eclipse-temurin:21-jre

WORKDIR /app

COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
