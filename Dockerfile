# Stage 1: Build the bot
FROM gradle:8.5-jdk17 AS build
WORKDIR /app
COPY . .
# Build the jar (skipping tests to save time)
RUN ./gradlew shadowJar --no-daemon

# Stage 2: Run the bot
FROM amazoncorretto:17
WORKDIR /app
# Copy only from the compiled jar from Stage 1
COPY --from=build /app/build/libs/*-all.jar app.jar
# Command to run the bot
CMD ["java", "-Xmx512m", "-jar", "app.jar"]