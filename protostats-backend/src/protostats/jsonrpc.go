package main

import (
	"encoding/json"
	"errors"
	"fmt"
	"io/ioutil"
	"log"
	"net/http"
	"reflect"
)

type RpcResponse struct {
	Id     int         `json:"id"`
	Result interface{} `json:"result"`
	Error  *string     `json:"error"` // Pointer to allow marshalling to null
}

type RpcRequest struct {
	Id     int      `json:"id"`
	Method string   `json:"method"`
	Params []string `json:"params"`
}

type JsonRpcEndpoint struct {
	Path string
	Api  interface{}
}

func (rpc *JsonRpcEndpoint) Handler(out http.ResponseWriter, in *http.Request) {
	if in.Method == "POST" {
		body, err := ioutil.ReadAll(in.Body)
		log.Printf("Received body %s", body)
		if err == nil {
			var rpcReq RpcRequest
			err = json.Unmarshal(body, &rpcReq)
			if err == nil {
				targetApi := reflect.ValueOf(rpc.Api)
				if targetApi.IsValid() {
					method := targetApi.MethodByName(rpcReq.Method)
					if method.IsValid() {
						args := make([]reflect.Value, len(rpcReq.Params))
						for i := 0; i < len(rpcReq.Params); i++ {
							args[i] = reflect.ValueOf(rpcReq.Params[i])
						}
						defer func() {
							if r := recover(); r != nil {
								log.Printf("An error occured when calling RPC method %s with params %s : %s", rpcReq.Method, rpcReq.Params, r)
								out.WriteHeader(http.StatusInternalServerError)
							}
						}()
						returnValue := method.Call(args)
						rpcRsp := RpcResponse{Id:rpcReq.Id}
						rpcRsp.Result = returnValue[0].Interface()
						rpcRsp.Error = nil
						returnJson, err := json.Marshal(rpcRsp)
						if err == nil {
							out.WriteHeader(http.StatusOK)
							out.Write(returnJson)
							return
						}
					} else {
						err = errors.New(fmt.Sprintf("No method called %s", rpcReq.Method))
					}
				} else {
					err = errors.New(fmt.Sprintf("API type was %s", rpc.Api))
				}

			}
		}
		log.Println(fmt.Sprintf("Error: %s", err))
		out.WriteHeader(http.StatusInternalServerError)
		return
	}
	out.WriteHeader(http.StatusMethodNotAllowed)
}
