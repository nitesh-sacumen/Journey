# Journey Auth Tree Node

## Journey

## Installation

The Journey-Auth tree nodes are packaged as a jar file and the latest release can be download [here](https://github.com/Enzoic/forgerock/releases/latest).
 Once downloaded, copy the jar file to the ../web-container/webapps/openam/WEB-INF/lib
 
## Steps

1) Configure Maven to be able to access the OpenAM repositories

2) Setup a Journey Maven Project and run mvn package command.

3) The project will generate a .jar file containing our custom nodes. i.e. Journey-ForgeRock-Integration-1.0.jar

5) Copy the Journey-ForgeRock-Integration-1.0.jar file to the WEB-INF/lib/ folder where AM is deployed

6) Restart the AM for the new plug-in to become available.


## Journey Auth Tree Configuration

Below are the nodes that will be available after deploying the jar file:

### Journey Enrollment Look Up
```js
** checking whether username exists in forgerock
** roles of forgerock user are fetched, admin group name is provided—if username is part of that admin group members list then facial biometrics will be preferred enrollment/authentication—but if admin user is not having facial biometrics enrolled but enrolled for mobile app then authentication will be done using mobile app
** if yes then checking whether same username exists at journey
** if yes, fetch enrollments
** if for authentication, required enrollment is there then connect with has enrollments with that method for authentication
** if no/sufficient enrollments then connect with no enrollments
** if user does not exist at journey then also connect with no enrollments path
```

Configuration is:
```js
* Refresh Token

* Time To Live

* Account Id

* Unique Identifier

* Admin Username

* Admin Password

* Group Name

* Retrieve Timeout

* Forgerock Host Url

* Request Timeout

```

<img width="261" alt="Screenshot 2022-07-21 at 1 23 29 AM" src="https://user-images.githubusercontent.com/106667867/180070027-39f7b6c3-b44f-41b3-b2e8-78596dc30db9.png">
<img width="266" alt="Screenshot 2022-07-21 at 1 23 49 AM" src="https://user-images.githubusercontent.com/106667867/180070065-5e4ee732-52ee-42a4-a17d-65ee9514f23b.png">



### Error Message Node
```js
This node will display error to the end user.
```
 
### Journey Pipeline
```js
journey pipeline is initiating authentication/enrollment by calling execution create api, which will give execution id--then retrieve execution api will check for execution response at fixed intervals and for fixed intervals--whether execution was successful/timeout-retry/failed 
```

Configuration is:
```js
* Pipeline Kay : Key for Journey pipeline.

* Dashboard ID : Dashboard ID
```

<img width="267" alt="Screenshot 2022-07-21 at 1 24 03 AM" src="https://user-images.githubusercontent.com/106667867/180070156-4c4e434b-6068-460d-aa4a-63688cfec60d.png">


### Logic To Determine Method
```js
This node will set method choice (FACIAL_BIOMETRIC/MOBILE_APP) for authentication.
```


### Outcome Node
```js
This node will redirect flow basis of customized outcomes(success/failure/timeout).
```



## Configure the trees as follows

### Journey Flow :
<img width="483" alt="Picture 1" src="https://user-images.githubusercontent.com/106667867/180070558-23d97114-24a6-4aa8-94f1-a94ac424018d.png">


## Set Logging Level

* User can set log level in forgerock instance, To set user need to follow this path:
```js
DEPLOYMENT-->SERVERS-->LocalInstance-->Debugging
```

## Configure the trees as follows
```js
* Navigate to **Realm** > **Authentication** > **Trees** > **Create Tree**
```

