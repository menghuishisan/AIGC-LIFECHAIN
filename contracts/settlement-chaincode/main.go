package main

import (
	"log"

	"github.com/hyperledger/fabric-contract-api-go/contractapi"
	"github.com/lifechain/settlement-chaincode/chaincode"
)

func main() {
	cc, err := contractapi.NewChaincode(&chaincode.SettlementContract{})
	if err != nil {
		log.Panicf("Error creating settlement_chaincode: %v", err)
	}
	if err = cc.Start(); err != nil {
		log.Panicf("Error starting settlement_chaincode: %v", err)
	}
}
