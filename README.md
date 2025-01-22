### Change Workflow
1. Make change locally
1. Test change locally
   * docker compose build
   * docker compose up
   * Make api call to `http:localhost:8080/api/{api}` e.g. `http://localhost:8080/api/ping`
1. Build docker image locally
   * Run `./gradlew build && docker build --platform linux/amd64 -t flightlogscan/flightlogscan:latest .`
1. Push docker image to container registry in docker hub
   * Requires login with `docker login`
   * `docker push flightlogscan/flightlogscan:latest`
     * Overrides the existing latest
   * `docker push flightlogscan/flightlogscan:$(git rev-parse --short HEAD)`
     * Necessary for keeping a history of images since latest gets overridden
1. On server, run deployment script
   * `sudo /opt/flightlogscan/bin/deploy.sh`
1. Test change locally
   * Make api call to `https://api.flightlogtracer.com/api/{api}` e.g. `https://api.flightlogtracer.com/api/ping`

### Server Logs
1. On server go into the docker container: `sudo docker exec -it /flightlogscan /bin/bash`
1. Run `cat /var/log/FLTSpring/application.log`

Alternatively: `sudo docker logs /flightlogscan`

### Local dev setup
1. Create `.env` file in `FLTSpring/`
1. Add contents:
```
API_KEY={insert key here}
SPRING_PROFILES_ACTIVE=dev
```
**NOTE: Do not commit this file to git. `.env` is in `.gitignore`**

