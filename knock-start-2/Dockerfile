# ---- build ----
FROM gradle:8.14.3-jdk17 AS build
WORKDIR /src
COPY build.gradle settings.gradle ./
RUN gradle dependencies --no-daemon -q > /dev/null 2>&1 || true
COPY src ./src
RUN gradle bootJar --no-daemon -q -x test

# ---- run ----
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /src/build/libs/app.jar app.jar
# 무료 인스턴스(512MB) 메모리에 맞춤. 기본은 데모 모드로 실행
ENV SPRING_PROFILES_ACTIVE=demo \
    JAVA_OPTS="-Xmx320m -XX:MaxMetaspaceSize=128m -XX:+UseSerialGC -Xss512k"
EXPOSE 8080
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
