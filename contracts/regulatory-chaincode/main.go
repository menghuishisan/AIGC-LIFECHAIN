package main

import (
	"log"

	"github.com/hyperledger/fabric-contract-api-go/contractapi"
	"github.com/lifechain/regulatory-chaincode/chaincode"
)

func main() {
	cc, err := contractapi.NewChaincode(&chaincode.RegulatoryContract{})
	if err != nil {
		log.Panicf("Error creating regulatory_chaincode: %v", err)
	}
	if err = cc.Start(); err != nil {
		log.Panicf("Error starting regulatory_chaincode: %v", err)
	}
}
