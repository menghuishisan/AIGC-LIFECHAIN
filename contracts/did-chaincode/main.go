package main

import (
	"log"

	"github.com/hyperledger/fabric-contract-api-go/contractapi"
	"github.com/lifechain/did-chaincode/chaincode"
)

func main() {
	cc, err := contractapi.NewChaincode(&chaincode.DidContract{})
	if err != nil {
		log.Panicf("Error creating did_chaincode: %v", err)
	}
	if err = cc.Start(); err != nil {
		log.Panicf("Error starting did_chaincode: %v", err)
	}
}
