FROM eclipse-temurin:21

RUN apt-get update && apt-get install -y --no-install-recommends \
    tesseract-ocr \
    tesseract-ocr-deu \
    tesseract-ocr-eng \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app

RUN mkdir -p /app/camt/done
RUN mkdir -p /app/import/done
RUN mkdir -p /app/export
RUN mkdir -p /app/google

COPY boot/build/libs/backend.jar /app/backend.jar

ENTRYPOINT ["java", "-jar", "backend.jar"]
