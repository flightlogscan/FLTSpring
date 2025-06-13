### Change Workflow
1. Make change locally
1. Test change locally
   * docker compose build
   * docker compose up
   * Make api call to `http:localhost:8080/api/{api}` e.g. `http://localhost:8080/api/ping`
1. Push to git
1. Let the magic of CI/CD in .github/workflows/service.yml awaken something in you
1. Make sure change built successfully: https://github.com/flightlogscan/FLTSpring/actions
1. Test real server api call
   * Make api call to `https://api.flightlogscan.com/api/{api}` e.g. `https://api.flightlogscan.com/api/ping`

### Server Logs
1. On server go into the docker container: `sudo docker exec -it /flightlogscan /bin/bash`
1. Run `cat /var/log/FLTSpring/application.log`

Alternatively: `sudo docker logs /flightlogscan`

TODO: Need to configure centralized logging in infra repo :yuh_panic:

### Local dev setup
1. Create `.env` file in `FLTSpring/`
1. Add contents:
```
API_KEY={insert key here}
SPRING_PROFILES_ACTIVE=dev
```
**NOTE: Do not commit this file to git. `.env` is in `.gitignore`**

