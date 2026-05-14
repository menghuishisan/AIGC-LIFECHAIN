package main

import (
	"log"

	"github.com/hyperledger/fabric-contract-api-go/contractapi"
	"github.com/lifechain/claim-chaincode/chaincode"
)

func main() {
	cc, err := contractapi.NewChaincode(&chaincode.ClaimContract{})
	if err != nil {
		log.Panicf("Error creating claim_chaincode: %v", err)
	}
	if err = cc.Start(); err != nil {
		log.Panicf("Error starting claim_chaincode: %v", err)
	}
}
