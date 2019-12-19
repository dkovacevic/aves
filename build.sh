#!/usr/bin/env bash
#docker login -u $DOCKER_USERNAME -p $DOCKER_PASSWORD
#mvn package -DskipTests=true -Dmaven.javadoc.skip=true
docker build -t $DOCKER_USERNAME/aves:0.1.0 .
docker push $DOCKER_USERNAME/aves
kubectl delete pod -l name=aves -n staging
kubectl get pods -l name=aves -n staging
