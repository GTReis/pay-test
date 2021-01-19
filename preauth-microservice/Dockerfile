FROM openjdk:11

ENV VERTICLE_FILE preauth-microservice-fat.jar

ENV VERTICLE_HOME /opt/verticles

RUN mkdir $VERTICLE_HOME

EXPOSE 8082

COPY target/$VERTICLE_FILE $VERTICLE_HOME
COPY src/config/auth.json $VERTICLE_HOME

WORKDIR $VERTICLE_HOME

ENTRYPOINT ["sh", "-c"]

CMD ["exec java -jar $VERTICLE_FILE -conf auth.json"]