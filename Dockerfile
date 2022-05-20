FROM navikt/java:17

COPY docker-init-scripts/import-apigw-key.sh /init-scripts/20-import-apigw-key.sh

COPY build/libs/*.jar ./app.jar