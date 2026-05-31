---
name: deploy-backend-application
description: Deploy the backend application. The application should be built and deployed within the Kubernetes cluster. React to the task to deploy the application.
---

1. Build and push the image: run `./gradlew buildAdapterDockerImage` followed by `./gradlew pushAdapterDockerImage` in the repository root. This builds the Docker image and pushes it to the registry at `192.168.178.29:5000/applications:latest`.
2. Roll out the new image: run `kubectl rollout restart deployment applications`, then wait for it with `kubectl rollout status deployment applications`.
3. Verify: check the pod logs with `kubectl logs deployment/applications --tail=100` for errors and confirm the application started successfully.
