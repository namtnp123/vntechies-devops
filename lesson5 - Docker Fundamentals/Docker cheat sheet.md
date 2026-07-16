# Docker quick reference cheat sheet
## Docker Build Commands:
```shell
docker build -t <image_name> .: Build a Docker image from a Dockerfile in the current directory and tag it with a name.

docker build --no-cache -t <image_name> .: Build a Docker image without using the cache.

docker build -f <dockerfile_name> -t <image_name> .: Build a Docker image using a specified Dockerfile.
```

## Docker Clean Up Commands:
```shell
docker system prune: Remove all unused Docker resources, including containers, images, networks, and volumes.

docker container prune: Remove all stopped containers.

docker image prune: Remove unused images.

docker volume prune: Remove unused volumes.

docker network prune: Remove unused networks.
```
## Container Interaction Commands:
```shell
docker run <image_name>: Run a Docker image as a container.

docker start <container_id>: Start a stopped container.

docker stop <container_id>: Stop a running container.

docker restart <container_id>: Restart a running container.

docker exec -it <container_id> <command>: Execute a command inside a running container interactively.
```
## Container Inspection Commands:
```shell
docker ps: List running containers.

docker ps -a: List all containers, including stopped ones.

docker logs <container_id>: Fetch the logs of a specific container.

docker inspect <container_id>: Inspect detailed information about a container.
```
## Image Commands:
```shell
docker images: List available Docker images.

docker pull <image_name>: Pull a Docker image from a Docker registry.

docker push <image_name>: Push a Docker image to a Docker registry.

docker rmi <image_id>: Remove a Docker image.
```