mvn deploy:deploy-file -Dfile=dist/kamikaze-2.0.0.jar -DpomFile=pom.xml -Durl=http://oss.sonatype.org/service/local/staging/deploy/maven2 -DrepositoryId=oss
