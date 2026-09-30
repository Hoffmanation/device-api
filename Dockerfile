FROM eclipse-temurin:25-jdk AS build
WORKDIR /workspace

RUN apt-get update \
    && apt-get install -y --no-install-recommends maven \
    && rm -rf /var/lib/apt/lists/*

COPY pom.xml ./

RUN mvn dependency:go-offline -B

COPY src ./src

RUN mvn package -DskipTests -B

FROM eclipse-temurin:25-jre
WORKDIR /app

RUN addgroup --system appgroup \
    && adduser --system appuser --ingroup appgroup

USER appuser

COPY --from=build /workspace/target/devices-api-*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
