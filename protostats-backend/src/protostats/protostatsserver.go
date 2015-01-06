package main

import (	
	"log"	
	"net/http"	
)

func main() {
	rpcEndpoint := JsonRpcEndpoint{Path:"/rpc", Api: new(RpcApi)}	
	http.HandleFunc(rpcEndpoint.Path, rpcEndpoint.Handler)
	log.Println("Starting server")
	http.ListenAndServe(":8080", nil)
}
