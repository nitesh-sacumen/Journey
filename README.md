# Journey Auth Tree Node

## Journey

## Installation

The Journey-Auth tree nodes are packaged as a jar file and the latest release can be download [here](https://github.com/nitesh-sacumen/Journey).
Once downloaded, copy the jar file to the ../web-container/webapps/openam/WEB-INF/lib
 
## Steps

1) Configure Maven in machine to setup the OpenAM repositories

2) Clone a Journey Maven Project from github and run mvn package command.

3) The project will generate a .jar file under target folder of repository containing our custom nodes. i.e. Journey-ForgeRock-Integration-1.0.jar.

5) Copy the Journey-ForgeRock-Integration-1.0.jar file to the WEB-INF/lib/ folder where AM is deployed(ex: tomcatDir/webapps/openam/WEB-INF/lib/).

6) Restart the AM for the new plug-in to become available.


## Journey Auth Tree Configuration

Below are the nodes that will be available after deploying the jar file:

### Journey Enrollment Look Up
```js
** checking whether username exists in forgerock
** setting username or email as uniqueId depending upon unique identifier selected
** making journey customer lookup call and storing the result in the shared context key name journeyUser
```

Configuration is:
```js
* API Token

* Account Id

* Unique Identifier

```
<img width="134" alt="j1" src="https://user-images.githubusercontent.com/106667867/182442353-bd3af57e-98b0-4a52-9510-8e728d181a85.png">

 
### Journey Pipeline
```js
journey pipeline is initiating authentication/enrollment by calling execution create api, which will give execution id--then retrieve execution api will check for execution response at fixed intervals and for fixed intervals--whether execution was successful/timeout-retry/failed 
```

Configuration is:
```js
* Pipeline Key : Key for Journey pipeline.

* Dashboard ID : Dashboard ID(Optional)
```

<img width="267" alt="Screenshot 2022-07-21 at 1 24 03 AM" src="https://user-images.githubusercontent.com/106667867/180070156-4c4e434b-6068-460d-aa4a-63688cfec60d.png">

### Scripted Decesion Node:
```js
** Scripted decision node has certain configurable parameters that should configured before running the script which includes forgerockUrlPrefix, forgerockAdminGroupName, forgerockAdminUsername, forgerockAdminPassword, adminUserPriorityArray, otherUserPriorityArray.
** Script file should also make call to the get forgerock session details api and store the session id in the sharedState with the key name forgerockSessionId.
** Script file should also has a field in the sharedState with the key name “type” that can have either of two values “Authentication” or “Enrollment” depending upon has enrollment or no enrollment path.
```


## Configure the trees as follows

### Journey Flow :

<img width="483" alt="j2" src="https://user-images.githubusercontent.com/106667867/182442810-590796a5-c5bd-4307-aec6-6f3645ec31b1.png">


## Set Logging Level

* User can set log level in forgerock instance, To set user need to follow this path:
```js
DEPLOYMENT-->SERVERS-->LocalInstance-->Debugging
```
Logs will be available under userDir/openam/var/debug/debug.out (Example: In mac - home/user/openam/var/debug/debug.out)


## Configure the trees as follows
```js
* Navigate to **Realm** > **Authentication** > **Trees** > **Create Tree**
```

