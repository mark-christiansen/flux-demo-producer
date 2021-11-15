# flux-demo-producer
Flux Demo Java Kafka Producer

### Summary

This is a sample Java Kafka producer application for the demonstration of full
CICD of both Kubernetes and Confluent Cloud resources using [Flux CD](https://fluxcd.io/)
and the [Confluent Cloud Operator](https://github.com/confluentinc/streaming-ops/tree/main/images/ccloud-operator).
Once setup in the [flux-demo-infra](https://github.com/mark-christiansen/flux-demo-infra) repo and all 
prerequisites are setup below, any push to the `main` branch of this repo should cause both Kubernetes and
Counfluent Cloud resources to be automatically deployed.

### Prerequisites

* A personal [Confluent Cloud](https://www.confluent.io/confluent-cloud/tryfree-v1/) account needs to be setup. You can get 650 free credits using the following promo codes: C50INTEG, DEVOPS200, FREETRIAL400.
* [Docker](https://docs.docker.com/get-docker/) should be setup on your local machine. Make sure there is enough memory and cpu in Docker for running Kubernetes.
* A Kubernetes cluster should be installed on your local machine. I would suggest installing [Kind](https://kind.sigs.k8s.io/).
* The [Kubernetes CLI - kubectl](https://kubernetes.io/docs/tasks/tools/) should be installed locally as well.
* The [Flux CD](https://fluxcd.io/) client should be installed locally. See these [instructions](https://fluxcd.io/docs/installation/) for more details.
* The [Confluent Cloud CLI](https://docs.confluent.io/ccloud-cli/current/install.html) should be installed locally.

### Setup Local Kubernetes

1) Create a cluster in Kubernetes: `kind create cluster`
2) Create namespaces
* `kubectl create ns flux-system`

### Setup Flux and flux-demo Repos

1) Fork Git repos
* `https://github.com/mark-christiansen/flux-demo-ccloud-operator.git`
* `https://github.com/mark-christiansen/flux-demo-infra.git`
* `https://github.com/mark-christiansen/flux-demo-producer.git`
2) Clone Git Repos
* `git clone https://github.com/<github-username>/flux-demo-ccloud-operator.git`
* `git clone https://github.com/<github-username>/flux-demo-infra.git`
* `git clone https://github.com/<github-username>/flux-demo-producer.git`
3) Create personal authentication token in your GitHub account and export your username and token as env variables in your terminal
* `export GITHUB_USER=<github-username>`
* `export GITHUB_TOKEN=<github-token>`
4) Install flux components in flux-system namespace
* `flux install -n flux-system`
5) Verify that the flux controllers are running in the `flux-system` namespace
* ```
  % kubectl -n flux-system get pod
  NAME                                       READY   STATUS    RESTARTS   AGE
  helm-controller-59dcbc6dcb-b2g87           1/1     Running   5          7d2h
  kustomize-controller-5b7b8b44f6-9l5nn      1/1     Running   5          7d2h
  notification-controller-77f68bf8f4-rclkk   1/1     Running   6          7d2h
  source-controller-7fbcff87ff-8jn5s         1/1     Running   5          7d2h
  ```
6) Bootstrap flux components in flux-system namespace to the flux-demo-infra repo
* `cd <repo-home>/flux-demo-infra`
* `rm -rf clusters`
* `git commit -am 'deleting to bootstrap'`
* `git push`
* `flux bootstrap github -n flux-system --owner $GITHUB_USER --repository flux-demo-infra --branch main --path ./clusters/my-cluster --token-auth --personal`
* `git pull`
7) Verify there are no errors in the `flux-system` kustomizations
* `flux get kustomizations --watch` (will refresh as kustomizations applied)

### Setup Confluent Cloud Cluster

1) Login to your Confluent Cloud account
2) Create a new environment if one doesn't exist.
* You can name the cluster whatever you want, but the name will replace the `default` value in this [file](https://github.com/mark-christiansen/flux-demo-producer/blob/main/kustomize/ccloud-config.yaml).
3) Create a new cluster if one doesn't exist. 
* You can name the cluster whatever you want, but the name will replace the `devops-demo` value in this [file](https://github.com/mark-christiansen/flux-demo-producer/blob/main/kustomize/ccloud-config.yaml).

### Setup Confluent Cloud Operator

1) Create local file containing your Confluent Cloud credentials and deploy to Kubernetes as secret
* ```
  cat > ccloud-secret.yml
  CCLOUD_EMAIL=ccloud-email
  CCLOUD_PASSWORD=ccloud-password
  ```
* `kubectl create secret generic cc.ccloud-secrets -n flux-demo --from-env-file=ccloud-secret.yml`
2) Create files to link `flux-demo-ccloud-operator` repo to `flux-demo-infra` repo
* `cd <repo-home>/flux-demo-infra`
* `flux create source git flux-demo-ccloud-operator -n flux-system --url https://github.com/<github-username>/flux-demo-ccloud-operator --branch main --interval 30s --export > ./clusters/my-cluster/flux-demo-ccloud-operator-source.yaml`
* `flux create kustomization flux-demo-ccloud-operator --target-namespace=flux-demo --source=flux-demo-ccloud-operator --path="./kustomize" --prune=true --interval=5m --export > ./clusters/my-cluster/flux-demo-ccloud-operator-kustomization.yaml`
3) Commit and push changes to `flux-demo-infra` repo
* `git add . && git commit -m "bootstrap flux-demo-ccloud-operator" && git push`
4) Verify `gitrepo` for `flux-demo-ccloud-operator` created in `flux-system` namespace
* ```
  % kubectl get gitrepo -n flux-system
    NAME                        URL                                                              READY   STATUS                                                            AGE
    flux-demo-ccloud-operator   https://github.com/mark-christiansen/flux-demo-ccloud-operator   True    Fetched revision: main/108c1cbfa7534989de7cced40cc320e4ecdae34e   1s
    flux-system                 https://github.com/mark-christiansen/flux-demo-infra.git         True    Fetched revision: main/d59ad0ede4a7aaf05926c71880c18556f44a16bf   47m
  ```
5) Verify `kustomization` for `flux-demo-ccloud-operator` created
* ```
  % flux get kustomizations --watch
    NAME       	                READY	MESSAGE                                                        	REVISION                                     	SUSPENDED
    flux-system	                True 	Applied revision: main/d59ad0ede4a7aaf05926c71880c18556f44a16bf	main/d59ad0ede4a7aaf05926c71880c18556f44a16bf	False
    flux-demo-ccloud-operator	True	Applied revision: main/108c1cbfa7534989de7cced40cc320e4ecdae34e	main/108c1cbfa7534989de7cced40cc320e4ecdae34e	False
  ```
6) Verify `flux-demo-ccloud-operator` pod running
* ```
  % flux-demo-infra % kubectl -n flux-demo get pod
    NAME                               READY   STATUS    RESTARTS   AGE
    ccloud-operator-5b9778c577-x7967   1/1     Running   0          5m39s
  ```

### Build Producer Application and Deploy Docker Image into Kubernetes

1) Run Maven build for `flux-demo-producer` application, create Docker image and deploy into `kind-control-plane`
* `<repo-home>/mvn package`
* `<repo-home>/docker-build.sh` (loads to `kind-control-plane` as well)

### Bootstrap flux-demo-producer repo to flux-demo-infra repo

1) Verify the environment variable settings in [flux-demo-producer/ksutomize/deployment.yaml](https://github.com/mark-christiansen/flux-demo-producer/blob/main/kustomize/deployment.yaml) are the settings you want
* Change `BROKER_URL` to point to your Confluent Cloud cluster. Search for value of `bootstrap.servers` in the CLI tools (https://confluent.cloud/environments/<your-env>/clusters/<your-cluster>/integrations/cli-tools) settings.
* Change the end of the secret name for `BROKER_AUTH` to match your cluster ID.
* Change `ITERATIONS` to the number of messages batches to send to each of the three topics.
* Change `BATCH_SIZE` to the number of messages to send to each of the three topics per iteration.
* Change `FREQUENCY` to how often (in milliseconds) to send a batch of messages to a topic.
* Confluent Cloud will eventually charge you if you were to leave the producer run over a long period of time. Running it for a small amount of messages (< 1000), will not cost you anything. Check [Billing/Payment](https://confluent.cloud/settings/billing/invoice) in Confluent Cloud to see if there are charges being applied.
2) Create flux source for your flux-demo-producer in your flux-demo-infra repo
* `cd <repo-home>/flux-demo-infra`
* `flux create source git flux-demo-producer -n flux-system --url https://github.com/<github-username>/flux-demo-producer --branch main --interval 30s --export > ./clusters/my-cluster/flex-demo-producer-source.yaml`
* `flux create kustomization flux-demo-producer --target-namespace=flux-demo --source=flux-demo-producer --path="./kustomize" --prune=true --interval=5m --export > /clusters/my-cluster/flex-demo-producer-kustomization.yaml`
3) Commit and push changes to `flux-demo-infra` repo
* `git add . && git commit -m "bootstrap flux-demo-producer" && git push`
4) Verify `gitrepo` for `flux-demo-producer` created in `flux-system` namespace
* ```
  % kubectl get gitrepo -n flux-system
    NAME                        URL                                                              READY   STATUS                                                            AGE
    flux-demo-ccloud-operator   https://github.com/mark-christiansen/flux-demo-ccloud-operator   True    Fetched revision: main/418eaaea0f7056fbb428b66f7b5faad3ef34f5bd   27m
    flux-demo-producer          https://github.com/mark-christiansen/flux-demo-producer          True    Fetched revision: main/0ca6156bc8489b69a9206afbb61fb5b5e65e1b65   27s
    flux-system                 https://github.com/mark-christiansen/flux-demo-infra.git         True    Fetched revision: main/e74e798ae5325b6ae1b77140acca130b4dd187ea   74m
  ```
5) Verify `kustomization` for `flux-demo-ccloud-operator` created
* ```
  % flux get kustomizations --watch
    NAME       	                READY	MESSAGE                                                        	REVISION                                     	SUSPENDED
    flux-system	                True 	Applied revision: main/d59ad0ede4a7aaf05926c71880c18556f44a16bf	main/d59ad0ede4a7aaf05926c71880c18556f44a16bf	False
    flux-demo-ccloud-operator	True	Applied revision: main/108c1cbfa7534989de7cced40cc320e4ecdae34e	main/108c1cbfa7534989de7cced40cc320e4ecdae34e	False
    flux-demo-producer	        True	Applied revision: main/0ca6156bc8489b69a9206afbb61fb5b5e65e1b65	main/0ca6156bc8489b69a9206afbb61fb5b5e65e1b65	False
  ```
6) Verify `flux-demo-ccloud-operator` pod running
* ```
  % flux-demo-infra % kubectl -n flux-demo get pod
    NAME                               READY   STATUS    RESTARTS   AGE
    ccloud-operator-5b9778c577-x7967   1/1     Running   0          5m39s
  ```

### Push flux-demo-infra Repo Changes

1) Commit and push changes to `flux-demo-infra` repo
* `git commit -am 'bootstrap flux-demo-producer'`
* `git push`
2) Verify that the flux-demo-producer pod is running in the `flux-demo` namespace
* ```
  % kubectl -n flux-demo get pod
  NAME                                 READY   STATUS    RESTARTS   AGE
  ccloud-operator-5b9778c577-w7w62     1/1     Running   1          7d2h
  flux-demo-producer-5c5d6fb7b-tlkzz   1/1     Running   0          2s
  ```
3) Verify the `flux-demo-producer` is producing messages to its three topics (`source-topic-0`,`source-topic-1`,`source-topic-2`).
4) After you have verified that `flux-demo-producer` is working, make sure to scale down the deployment so the application doesn't keep producing messages to Confluent Cloud which you will eventually be charged for.
* `flux-demo-producer % kubectl -n flux-demo scale --replicas=0 deployment flux-demo-producer`
5) `ccloud-operator` should have created a service account called `flux-demo-producer`. You will not see it in Confluent Cloud unless you look using `ccloud`.
* `ccloud login` (enter you email and password)
* 