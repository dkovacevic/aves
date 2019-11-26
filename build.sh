#!/usr/bin/env bash
docker login -u $DOCKER_USERNAME -p $DOCKER_PASSWORD
mvn package -DskipTests=true -Dmaven.javadoc.skip=true
docker build -t $DOCKER_USERNAME/aves:latest .
docker push $DOCKER_USERNAME/aves
kubectl delete pod -l name=aves -n prod
kubectl get pods -l name=aves -n prod
