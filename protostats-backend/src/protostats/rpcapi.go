package main

import (
	"log"
	"strconv"
)

type RpcApi struct {
	tokenCount int
	}

func (r *RpcApi) InitTests(networkAndPhoneData string) string {
	token := strconv.Itoa(r.tokenCount)
	r.tokenCount = r.tokenCount + 1	
	log.Printf("InitTests called with data %s, return token %s", networkAndPhoneData, token)
	return token
}

func (r *RpcApi) RpcScenarioCall(token string) string {
	log.Printf("RpcScenarioCall called with token %s", token)
	return "Some data, lorem ipsum, random chars gsdfsfarqwadg78se7df89sdyfhgoaidf0ad9fuad0f ua0w, end an end"
}