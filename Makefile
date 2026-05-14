JAVA     = /mnt/c/Program\ Files/Java/jdk-23/bin/java.exe
JAVAFX   = lib/openjfx-21.0.11_windows-x64_bin-sdk/javafx-sdk-21.0.11/lib
M2       = C:/Users/samou/.m2/repository
MAIN     = TaskManager.client.ClientMain

CP = target/classes;$(JAVAFX)/javafx.base.jar;$(JAVAFX)/javafx.controls.jar;$(JAVAFX)/javafx.fxml.jar;$(JAVAFX)/javafx.graphics.jar;$(JAVAFX)/javafx.media.jar;$(JAVAFX)/javafx.swing.jar;$(JAVAFX)/javafx.web.jar;$(JAVAFX)/javafx-swt.jar;$(M2)/com/fasterxml/jackson/core/jackson-databind/2.18.3/jackson-databind-2.18.3.jar;$(M2)/com/fasterxml/jackson/core/jackson-core/2.18.3/jackson-core-2.18.3.jar;$(M2)/com/fasterxml/jackson/core/jackson-annotations/2.18.3/jackson-annotations-2.18.3.jar;$(M2)/org/springframework/spring-websocket/6.2.6/spring-websocket-6.2.6.jar;$(M2)/org/springframework/spring-web/6.2.6/spring-web-6.2.6.jar;$(M2)/org/springframework/spring-context/6.2.6/spring-context-6.2.6.jar;$(M2)/org/springframework/spring-beans/6.2.6/spring-beans-6.2.6.jar;$(M2)/org/springframework/spring-core/6.2.6/spring-core-6.2.6.jar;$(M2)/org/springframework/spring-jcl/6.2.6/spring-jcl-6.2.6.jar;$(M2)/com/oracle/database/jdbc/ojdbc11/23.3.0.23.09/ojdbc11-23.3.0.23.09.jar

run-client:
	$(JAVA) \
		--module-path "$(JAVAFX)" \
		--add-modules javafx.controls,javafx.fxml \
		-cp "$(CP)" \
		$(MAIN)

run-server:
	mvn spring-boot:run

compile:
	mvn compile