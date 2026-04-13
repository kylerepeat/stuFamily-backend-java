FROM maven:3.9.9-eclipse-temurin-17 AS builder
WORKDIR /workspace

COPY pom.xml ./
COPY stufamily-core/pom.xml stufamily-core/pom.xml
COPY stufamily-admin-api/pom.xml stufamily-admin-api/pom.xml
COPY stufamily-weixin-api/pom.xml stufamily-weixin-api/pom.xml
COPY stufamily-boot/pom.xml stufamily-boot/pom.xml

COPY stufamily-core/src stufamily-core/src
COPY stufamily-admin-api/src stufamily-admin-api/src
COPY stufamily-weixin-api/src stufamily-weixin-api/src
COPY stufamily-boot/src stufamily-boot/src

RUN mvn -B -DskipTests -pl stufamily-boot -am package

FROM eclipse-temurin:17-jre
WORKDIR /app

ENV TZ=Asia/Shanghai
ENV JAVA_OPTS="-Xms256m -Xmx512m"

COPY --from=builder /workspace/stufamily-boot/target/stufamily-boot-*.jar /app/app.jar

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
