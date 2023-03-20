build-native:
		@./mvnw package -Dpackaging=native-image; \
    chmod +x target/attini; \
    target/attini --version
