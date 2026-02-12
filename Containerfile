FROM bellsoft/liberica-openjdk-debian:21
ENV JAVA_HOME=/usr/lib/jvm/jdk-21.0.10-bellsoft-aarch64
ENV PATH="${JAVA_HOME}/bin:${PATH}"
WORKDIR /app
COPY quant-app/target/quant-app-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["/usr/lib/jvm/jdk-21.0.10-bellsoft-aarch64/bin/java", "-jar", "/app/app.jar"]
