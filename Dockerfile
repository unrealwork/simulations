FROM openjdk:8
ENV LANG=en_US.UTF-8
COPY target/rio-simulations*jar-with-dependencies.jar /opt/rio-simulations.jar
COPY entrypoint.sh /opt/entrypoint.sh
RUN ["chmod", "+x", "/opt/entrypoint.sh"]
ENTRYPOINT ["/opt/entrypoint.sh"]
