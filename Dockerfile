# handles compilation and packages
FROM maven:3.8.6-openjdk-11-slim AS build

# setting the working directory to app
WORKDIR /app

# downloading all the dependencies
COPY pom.xml .
RUN mvn dependency:go-offline -B

# building the application without the test cases, also creates the jar file in the target directory
COPY src ./src
RUN mvn clean package -DskipTests
RUN ls -la /app/target/

#only using jre not the full sdk , which reduces the size of the image
FROM openjdk:11-jre-slim


RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*


WORKDIR /app
RUN mkdir -p /app/data

# copies the jar file from the build stage (Multi stage docker biggest advantage)
COPY --from=build /app/target/*.jar ./redis-clone.jar


EXPOSE 6379


HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
    CMD curl -f http://localhost:6379 || exit 1


ENV JAVA_OPTS="-Xmx512m -Xms256m -XX:+UseG1GC -XX:+UseContainerSupport"


CMD ["sh", "-c", "java $JAVA_OPTS -jar redis-clone.jar --port 6379"]

