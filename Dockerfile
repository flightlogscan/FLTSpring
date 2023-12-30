FROM openjdk:21
 ADD build/libs/FLTSpring-0.0.1-SNAPSHOT.jar FLTSpring.jar
 EXPOSE 80
 EXPOSE 443
ENTRYPOINT ["java","-jar","FLTSpring.jar"]
