# CHATOS PROTOCOL 

## Table of Contents
1. Members
2. Summary
3. Design descisions
4. Initial Connection Protocol
5. Clients scope
6. Server responses
7. OPCodes
8. ErrorCodes

## 1. Members
- COSSINET Axel, IINFO2
- GUILLEMAUD Yann, IINFO2
___

## 2. Summary
ChatOS protocol is a student based protocol as part of ESIPE's network projet. 
Protocol aim allows users connecting to a chat server in which no IP adresses are retained. 
This RFC explains the protocol and its types of packets, its working and its client-side way of connection. 
However because of currents few lack of acknoledges, some technical details will come later on. 
Feel free to contact protocol's members in any questions you might have.
___

## 3. Design decisions
- Each exchange start with an OPCode for its meaning, listed in section **# 7. Server Response** below.
- The server does not retains any client informations except his pseudonym.
- Clients pseudonyms must be unique and must not exceed 30 characters. Otherwise transfer initialization will be declined.
- Clients requests and informations must be encoded with UTF8 charset. Otherwise unexpected behaviours might occur.  
- This protocol uses three types of format in order to represent informations: byte for OPCode, int for encoded pseudonym size and long for encoded message size. 
- In order to avoid infinite requests waitings on private connections, a 5 minutes timeout is sent for each request. After this delay the request will be automatically declined.
- On the same way, private connections automatically closes after 5 minutes without message exchange.

___

## 4. Initial Connection Protocol
Server connection consists of sending a first request containing the initialization OPCode alongside to the user pseudonym which has not already been taken.

### Client request
This request must have the following format:
- OPCode : 1 (byte)
- Number of bytes encoding the pseudonym (int)
- Encoded pseudonym


### Server Response
The server will respond to tell to the client if the pseudonym is valid and if it is not already used.
See server response explanations in section **# 6. Server Response** below.

___

## 5. Clients scope
Connected clients have three chatting possibilities.
Theses possibilities and their corresponding request format are explained below.

### _Global message_

#### Client request
A client can send a message to all of connected clients on the server with the following format:
- OPCode : 4 (byte)
- Number of bytes encoding the message (long)
- Encoded message 

#### Server Response
No server response for this request

#### Server Message
The message of the client will be sent from the server to every connected client in a packet containing the sender pseudonym and his message.
It will have the following format :
- OPCode : 5 (byte)
- Number of bytes encoding the sender's pseudonym (int)
- Sender's pseudonym
- Number of bytes encoding the message (long)
- Endoded message 


### _Specific user message_

#### Client request
A client can send a message to a connected client on the server with the following format:
- OPCode : 6 (byte)
- Number of bytes encoding the other client's pseudonym (int)
- Specified client's pseudonym encoded 
- Number of bytes encoding the message (long)
- Endoded message 

#### Server Response
The server will respond to tell to the client if the addressee's pseudonym is linked to an existing connected client.
See server response explanation in section **# 6. Server Response** below.

#### Server Message
The message of the client will be sent to the specified connected client in a packet containing the sender pseudonym and the message.
It will have the following format :
- OPCode : 7 (byte)
- Number of bytes encoding the sender's pseudonyme (long)
- Sender's pseudonyme encoded
- Number of bytes encoding the message (long)
- Endoded message


### _Private TCP Connection_

#### Client request
A client can send a message if he want to establish a private TCP connection.
His initial request will only contain the pseudonym of the other user :

It will have the following format :
- OPCode : 8 (byte)
- Number of bytes encoding the addressee's pseudonyme (int)
- Addressee's pseudonyme encoded 

#### Server Response
The server will respond to tell to the seeker if the addressee's pseudonym is linked to an existing connected client and if he has accepted the private connection request.
See server response explanation in section **# 6. Server Response** below.

#### Server Request
If the addressee's pseudonym is valid and linked to a connected client, the server will send him a request to ask him if he accept the private connection request, with the following format :
- OPCode : 9 (byte)
- Number of bytes encoding the seeker's pseudonyme (int)
- Seeker's pseudonyme encoded

#### Client Response
The addressee can answer to a private connection request.
His response will have the following format :
- OPCode : 10 (byte)
- Number of bytes encoding the seeker's pseudonyme (int)
- Seeker's pseudonyme encoded
- Boolean corresponding to his response (True if he accept the private connection, else False) (boolean)

Each private connection request is linked to a timeout. In the case this delay is exceeded, server will automatically decline the connection with an error code of 7. 

#### Server Message
If the private connection request has been accepted, the server wil send to both clients a message containing a unique connection id that both client will have to use to establish the private connection (the id will be the same for both client but unique for this private connection).

It will have the following format :
- OPCode : 11 (byte)
- Number of bytes in the other client pseudonym endcoded (int)
- Other client's pseudonyme encoded 
- Private connection Id (long)

#### Client Connection

When the client have received a Private connection id, he will have to establish a new TCP connection with the server.

The connection request will have the following format :
- OPCode : 12 (byte)
- Private connection Id (long)

#### Server Response
Once the server have accepted two connection with the same Private connection Id (and this id has been generated by the server), he will send a message to both client to tell them that the private connection has been established.
See server response explanation in section **# 6. Server Response** below.


#### Private connection behavior 
Remplacer le truc en dessous par une traduction de ca imo :
A partir de ce moment, la connexion privée est réputée établie: tous les octets écrits par un client sur l'une des connexions sont relayés par le serveur vers l'autre connexion. Lorsqu'un client ferme sa connexion en écriture, le serveur fait de même sur la connexion correspondante.


According to the main goal of ChatOS protocol, neither of the two clients will know the other IP address they're connected to.
Each of them will connect to a specific socket and the server will transfer data through theses, allowing clients to talk like if there was an unique socket.
Private connections are closed when one of the two clients decide to leave, or if no message has been sent from one of the two client from the last 5 minutes. 
The server will send a message to prevent this closing.

___

### 6. Server Response
Server response can contain two OPCodes, 2 for a successful operation and 3 for failed ones.
The server response will have the following format :
- OPCode : 2 (byte) (SUCCESS)

or
- OPCode : 3 (byte) (FAILURE)
- ErrorCode (int)
(You can check every ErrorCode meaning in the part **#8. ErrorCodes** below)

___

### 7. OPCodes
OPCodes (Operation Codes) describe the operation to be performed.
Each OPCode is represented in a byte. Parenthesis specifies the sender of the code, where S is for Server and C for client.
ChatOS protocol uses the following OPCodes:
1. Initialization (C)
2. Successful operation (S)
3. Failed Operation (S)
4. Global message (C)
5. Global message (S)
6. Specific user message (C)
7. Specific user message (S)
8. Private connection request (C)
9. Private connection request (S)
10. Private connection response (C)
11. Private connection establishment (S)
12. Private connection establishment (C)

___

### 8. ErrorCodes
Here are all the Error Codes that can be used in and 3 OPCode server response in case of a failed resquest from a client.
Each ErrorCode is also represented in byte format.

- 0 : Undefined error
- 1 : Empty Pseudonym
- 2 : Too long pseudonym
- 3 : Already used pseudonym
- 4 : Pseudonym not linked to any connected client
- 5 : Message too long
- 6 : Addressee's client refused the private connection
- 7 : Addressee's client timed out to respond to the private connection
- 8 : Private connection id invalid