FROM eclipse-temurin:17-jdk-alpine AS build
WORKDIR /app
COPY gradle gradle
COPY gradlew build.gradle settings.gradle ./
RUN chmod +x gradlew && ./gradlew dependencies --no-daemon 2>/dev/null || true
COPY src src
RUN ./gradlew build -x test --no-daemon

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

# Descargar e importar certificado TLS de Amazon DocumentDB al truststore del JRE
RUN apk add --no-cache wget && \
    wget -q https://truststore.pki.rds.amazonaws.com/global/global-bundle.pem -O /tmp/global-bundle.pem && \
    csplit -f /tmp/cert- -b '%02d.pem' -z /tmp/global-bundle.pem '/-----BEGIN CERTIFICATE-----/' '{*}' 2>/dev/null && \
    TRUSTSTORE=$(find /opt -name cacerts -type f | head -1) && \
    for cert in /tmp/cert-*.pem; do \
      [ -s "$cert" ] && keytool -importcert -trustcacerts -keystore "$TRUSTSTORE" \
        -storepass changeit -noprompt -alias "aws-docdb-$(basename $cert)" -file "$cert" 2>/dev/null || true; \
    done && \
    rm -f /tmp/cert-*.pem /tmp/global-bundle.pem && \
    apk del wget

COPY --from=build /app/build/libs/*.jar app.jar
USER appuser
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
