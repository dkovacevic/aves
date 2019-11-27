FROM dejankovacevic/bots.runtime:2.10.3

COPY target/aves.jar     /opt/aves/
COPY swisscom.jks        /opt/aves/
COPY aves.yaml           /etc/aves/

WORKDIR /opt/aves

EXPOSE  8090 8091 8082

CMD ["sh", "-c", "/usr/bin/java -jar aves.jar server /etc/aves/aves.yaml"]