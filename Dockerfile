FROM openjdk:11-jdk-slim
ARG JAR_FILE=target/*.jar
COPY ${JAR_FILE} /app/app.jar
# unpacked archive is slightly faster on startup than running from an unexploded archive
RUN /bin/bash -c 'cd /app; jar xf app.jar'
# startup time faster when running the app with its "natural" main method instead of the JarLauncher
ENTRYPOINT ["java","-cp","/app/BOOT-INF/classes:/app/BOOT-INF/lib/*","com.machrist.fluxdemo.producer.Application"]