
# STA IDM Connector: Identity Connector for SafeNet Trusted Access over REST APIs

The STA IDM Connector is based on ConnId framework version 1.5.0.0. When used with midPoint (an open-source identity management and identity governance solution), the connector allows provisioning and de-provisioning of users, as well as management of their associated user groups between, SafeNet Trusted Access (STA) and a third-party directory (for example, Active Directory, Azure Active Directory, etc.).
<br><br>
   ![Connector](STA-sync.drawio.png)
<br><br>

## Capabilities and Features ##

| Feature                | Supported     | Notes                                      |
| ---------------------- | ------------- | -------------                              |
| Synchronization        | YES           | For both users and groups                  |
| Live Synchronization   | YES           | Only for users                             |
| Password               | NO            | Passwords are not supported                |
| Filtering changes      | YES           | limited attribute based                    |
| Paging support         | YES           | Simple Page Results                        |


The connector is meant to be compatible with the IDM Services using ConnId framework. The connector has been tested on Evolveum midPoint


## How to use? ##

1. Download this github project and build the java project using maven.
2. Deploy the connector jar file inside your IDM service.
3. Restart the service (if required).
4. Create a new connector resource and configure the basic conigurations.

   ![Basic Configuration](basic-config.png)

5. Select the Schema object that needs to be mapped.


## How Synchronization works? ##

Synchronization of Identities Depends on the scehma mappings configured in the connector

1. Only mapped attributes can be imported/exported to/from STA.
2. If STA already had a user with a similar userID which is being imported, only those attrbutes
   that are changed will be modified.

## How to Build ? ##

This project depends on the connID framework. Based on the IDM service version being used, use the right version into the connector pom.xml file:
    
    <dependency>
     <groupId>net.tirasa.connid</groupId>
     <artifactId>connector-framework</artifactId>
     <version>${connId.version}</version>
    </dependency>

(adjust the polygon version with the version that you use for connector parent)

## Limitations ##

1. Password synchronization is not supported.
3. Group live synchronization is not supported.
