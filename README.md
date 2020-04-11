# Corda java app

## Short description
This project is part of my masters degree application for providing 
cross-chain transactions between **R3 Corda** platform and **Ethereum**, for example, for banking sector.


## Scenario
I try to implement typical for banking sector **KYC** scenario (Know your customer). 
*Bank_1* collects personal info about their customers and then, 
if customer wants to go to another bank (*Bank_2*), 
*Bank_1* just send transaction to *Bank_2* with all KYC data. *Bank_1* lose rights for using this information, *Bank_2* get these rights. 


It works well for banks inside one DLT-platfrom, such as **R3 Corda**, **Hyperledger Fabric** and other.
But the banking sector does not have a single platform and 
all banks are divided into separate groups with their own technology or platform inside.

So, I will try to solve this problem for the two platforms that are actively used in the banking sector - **R3 Corda** and **Ethereum**.

_**todo:** add links to KYC, R3 Corda, Ethereum, Hyperledger Fabric and part with Corda application_


## How to run Corda part

Run gradle task for building nodes:
```
corda-java-app: ./gradlew clean deployNodes
```
Move to /build/nodes and execute .jar file, which will run all 4 nodes (PartyA, PartyB, Crossnode and Notary):
```
corda-java-app/build/nodes: java -jar runnodes.jar
```
It will open 4 XTerm (install it by (for Linux) "_sudo apt install xterm_"), which are Corda terminals. Examples of using it you can found [here](https://docs.corda.net/docs/corda-os/4.4/tutorial-cordapp.html#via-the-interactive-shell-terminal-only)

After this, you can start web server of node, which you want (check list of gradle tasks and add your custom task for running another node):
```
corda-java-app: ./gradlew runPartyAServer
```

It will run server on _http://localhost:10050_. Now you can send HTTP-requests by Postman (or whatever) and use this app.

## How to run crosschain transaction (Corda --> Ethereum):

If the file is already loaded, use method *com.template.webserver.Controller.startCrosschainKYCFlowWithExistingFile* request. Otherwise, use *com.template.webserver.Controller.startCrosschainKYCFlowWithInitFile*;
You should now address of node in Ethereum network (public key); Both methods starts KYCFlow to Corda's CrossNode, and after that init transaction in Ethereum network from Ethereum's CrossNode;