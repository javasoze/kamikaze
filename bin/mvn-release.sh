mvn deploy:deploy-file -Dfile=target/kamikaze-3.0.3.jar -DpomFile=pom.xml -Durl=https://oss.sonatype.org/service/local/staging/deploy/maven2 -DrepositoryId=ossKamikazeRelease
