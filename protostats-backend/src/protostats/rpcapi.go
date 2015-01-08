package main

import (
	"container/list"
	"fmt"
	"log"
	"strconv"
	"sync"
	"bytes"
)

type RpcApi struct {
	tokenCount int
	lastEvents *list.List
	logChannel chan string
	mu sync.Mutex
}

func (r *RpcApi) InitTests(networkAndPhoneData string) string {
	if r.lastEvents == nil {
		r.lastEvents = list.New()
		r.logChannel = make(chan string)
		go func() {
			for true {
				str := <-r.logChannel
				r.mu.Lock()				
				r.lastEvents.PushBack(str)
				if r.lastEvents.Len() > 100 {
					r.lastEvents.Remove(r.lastEvents.Front())
				}
				r.mu.Unlock()
				log.Print(str)				
			}
		}()
	}
	token := strconv.Itoa(r.tokenCount)
	r.tokenCount = r.tokenCount + 1
	log.Printf("InitTests called with data %s, return token %s", networkAndPhoneData, token)
	return token
}

func (r *RpcApi) RpcScenarioCall(token string) string {
	log.Printf("RpcScenarioCall called with token %s", token)
	return "Some data, lorem ipsum, random chars gsdfsfarqwadg78se7df89sdyfhgoaidf0ad9fuad0f ua0w, end an end"
}

func (r *RpcApi) TrackScenarioTime(token string, scenarioId string, scenarioStep string, timingMs string) {
	r.logChannel <- fmt.Sprintf("time\t%v\t%v\t%v\t%v", token, scenarioId, scenarioStep, timingMs)
}

func (r *RpcApi) TrackScenarioError(token string, scenarioId string, scenarioStep string, errorMsg string) {
	r.logChannel <- fmt.Sprintf("error\t%v\t%v\t%v\t%v", token, scenarioId, scenarioStep, errorMsg)
}

func (r *RpcApi) GetLatestEvents() string {
	r.mu.Lock()
	defer r.mu.Unlock()
	if r.lastEvents == nil {
		return ""
	} else {
		var buffer bytes.Buffer
		for e := r.lastEvents.Front(); e != nil; e = e.Next() {
			buffer.WriteString(e.Value.(string))
			buffer.WriteString("\n")
		}
		return buffer.String()
	}	
}
