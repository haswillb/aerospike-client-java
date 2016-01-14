FROM java:latest

ENV REFRESHED_AT 2016-01-14

WORKDIR /usr/lib
RUN wget http://mirror.cc.columbia.edu/pub/software/apache/maven/maven-3/3.0.5/binaries/apache-maven-3.0.5-bin.tar.gz && \
        tar xzf apache-maven-3.0.5-bin.tar.gz && \
        rm apache-maven-3.0.5-bin.tar.gz && \
        mv apache-maven-3.0.5 /usr/lib/mvn
ENV M2_HOME=/usr/lib/mvn
ENV M2=$M2_HOME/bin
ENV PATH $PATH:$$JAVA_HOME:$JAVA:$M2_HOME:$M2

RUN mkdir /app
WORKDIR /app

ADD . .

RUN mvn install -DskipTests

#ADD ./benchmarks/pom.xml /app/benchmarks/pom.xml
RUN cd benchmarks && mvn package -DskipTests

#ADD . .

CMD ["./tests.sh"]