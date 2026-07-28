# syntax=docker/dockerfile:1

FROM eclipse-temurin:21-jdk AS build
WORKDIR /build
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN ./mvnw -B -q dependency:go-offline
COPY src ./src
RUN ./mvnw -B -q clean package -DskipTests

# Fetch the model here, never at runtime. A cold container must not need
# network access to serve its first request.
FROM eclipse-temurin:21-jdk AS model
# ARG is scoped per stage, so these must be declared inside this stage.
# Defaults are pinned so the build needs no arguments or env vars.
ARG ONNX_URL="https://huggingface.co/sentence-transformers/all-MiniLM-L6-v2/resolve/1110a243fdf4706b3f48f1d95db1a4f5529b4d41/onnx/model.onnx"
ARG TOKENIZER_URL="https://huggingface.co/sentence-transformers/all-MiniLM-L6-v2/resolve/1110a243fdf4706b3f48f1d95db1a4f5529b4d41/tokenizer.json"
RUN apt-get update && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*
WORKDIR /opt/model
RUN curl -fL -o model.onnx     "$ONNX_URL" \
 && curl -fL -o tokenizer.json "$TOKENIZER_URL"

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=model /opt/model /opt/model
COPY --from=build /build/target/*.jar app.jar
ENV SPRING_AI_EMBEDDING_TRANSFORMER_ONNX_MODEL_URI=file:/opt/model/model.onnx
ENV SPRING_AI_EMBEDDING_TRANSFORMER_TOKENIZER_URI=file:/opt/model/tokenizer.json
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
