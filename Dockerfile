FROM docker.digitalfactory.mcb.local/mcb-dtp/openjdk:10.0-jdk-ojdbc8

RUN chmod 644 /ojdbc8.jar

RUN groupadd -g 999 appuser && \
    useradd -r -u 999 -g appuser appuser
USER appuser

ENV SERVER_PORT=8080
EXPOSE 8080

COPY target/FileService-*.jar api.jar

#COPY mcb-store mcb-store

CMD ["java", "-server", "-XX:+UnlockExperimentalVMOptions", "-XX:+UseG1GC", "-XX:MaxGCPauseMillis=100", "-XX:+UseStringDeduplication", "-jar", "api.jar"]

#ENTRYPOINT ["java",\
#"-Djavax.net.ssl.trustStore=/mcb-store",\
#"-Djavax.net.ssl.trustStorePassword=123456",\
#"-Djava.security.egd=file:/dev/./urandom",\
#"-Dloader.main=com.mcb.fileService.MainKt",\
#"-Dloader.path=/ojdbc8.jar",\
#"-classpath","/api.jar",\
#"org.springframework.boot.loader.PropertiesLauncher"]
