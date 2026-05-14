package main

import (
	"log"

	"github.com/hyperledger/fabric-contract-api-go/contractapi"
	"github.com/lifechain/license-chaincode/chaincode"
)

func main() {
	cc, err := contractapi.NewChaincode(&chaincode.LicenseContract{})
	if err != nil {
		log.Panicf("Error creating license_chaincode: %v", err)
	}
	if err = cc.Start(); err != nil {
		log.Panicf("Error starting license_chaincode: %v", err)
	}
}
