package main

import (
	"fmt"
	"strconv"
)

type RpcApi struct {}

type TestRunDto struct {
	Token string	
}

func (r *RpcApi) InitTests(in string) TestRunDto {
	fmt.Printf("InitTests called %s\n", in)
	return TestRunDto{Token:strconv.Itoa(1234)}
}